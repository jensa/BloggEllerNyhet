import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.lang.Thread;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Vector;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.safety.*;
import org.jsoup.select.*;

public class Crawler implements Runnable {
  public static final String DIR_SEP = System.getProperty("file.separator");
  public static final String BASE_OUTPUT_DIR = "data";
  public static final String DEFAULT_SOURCE_FILE = "sources.txt";
  
  //public static final String TEST_OUTPUT = Classifier.test;
  //public static final String TRAIN_OUTPUT = Classifier.train;
  
  /** Number of pages to crawl for source URLs containing the %page variable */
  public static final int NUM_PAGES = 10;

  /** Format of dates read from RSS pubDate tags */
  public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
  /** Format of output files (uses the parsed pubDate date) */
  public static final SimpleDateFormat ID_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

  public static void main(String [] args) throws Exception {
    LineNumberReader lnr = null;
    
    // Crawl URLs listed in sources file
    try {
      lnr = new LineNumberReader(new FileReader(DEFAULT_SOURCE_FILE));
      String line;

      // Parse and process each line
      while ((line = lnr.readLine()) != null) {
        try {
          String[] params = parseCSVLine(line).toArray(new String[5]);
          new Thread(new Crawler(params)).start();
        } catch (Exception e) {
          System.err.println("Error on line " + lnr.getLineNumber() + ": " +
              e.getMessage());
        }
      }
    } catch(IOException e) {
      System.err.println("Could not open source file for reading: " + DEFAULT_SOURCE_FILE);
      System.exit(1);
    } finally {
      try {
        if (lnr != null) lnr.close();
      } catch(IOException e) {}
    }
  }

  /** Target URL to crawl */
  public String url;
  /** Classification of this URL */
  public String cls;
  /** Name for this crawling instance (used when generating file names) */
  public String outputName;
  /** Base directory for data crawled in this instance */
  public String outputDir;
  /** CSS selector for element containing the body text for each item */
  protected String contentTag = "content, content|encoded";
  /** CSS selector for the body text if present in an external HTML document */
  protected String externalSelector = null;

  /**
   * Required arguments:
   *   name: Unique name for crawled site
   *   class: Classification of content
   *   url: URL to feed
   * Optional arguments:
   *   contentTag: CSS selector / tag name of element containing the body text
   *   externalSelector: Will consider contentTag text as an URL, fetch that URL
   *     and use the text in the element matching a CSS selector (this string)
   *     as the body text
   */
  public Crawler(String... args) throws Exception {
    if (args.length < 3)
      throw new IllegalArgumentException("name, class and url are required, only " +
          args.length + " arguments provided.");
    
    this.outputName = args[0];
    this.cls = args[1];
    this.url = args[2];
    if (args.length >= 3 && args[3] != null)
      this.contentTag = args[3];
    if (args.length >= 4 && args[4] != null)
      this.externalSelector = args[4];

    this.outputDir = BASE_OUTPUT_DIR + DIR_SEP + cls;
    (new File(this.outputDir)).mkdirs();
  }

  public void run() {
    System.out.printf("Fetching %s/%s:\n", cls, this.outputName);

    // Parameterized url (contains variables)?
    if (url.contains("%page")) {
      for (int page = 1; page <= NUM_PAGES; ++page)
        crawlFeed(url.replace("%page", "" + page));
    } else {
      crawlFeed(url);
    }
  }

  /**
   * Crawls and saves the RSS feed located in a specified URL.
   * Handles links to external pages if configured.
   */
  protected boolean crawlFeed(String url) {
    try {
      System.out.println("Crawling " + url);
      Document doc = fetchDocument(url);

      Elements items = doc.getElementsByTag("item");
      for (Element item : items) {
        String id = idFromPubDate(item.getElementsByTag("pubDate").first().text());
        Elements contents = item.select(this.contentTag);

        if (contents.size() > 0) {
          String data = contents.first().text();

          // Is this a external link which we need to crawl?
          if (this.externalSelector != null) {
            doc = fetchDocument(data);
            contents = doc.select(this.externalSelector);
            if (contents.size() > 0) {
              data = contents.first().text();
            } else {
              printError(data + " - no element matching external selector");
              data = null;
            }
          }

          if (data != null)
            saveData(id, data);
        } else {
          printError(id + " - missing content tag");
        }
      }

      return true;
    } catch(IOException e) {
      printError("Error during crawling: " + e.getMessage());
    }
    return false;
  }

  /**
   * Saves the crawled body text to the outputDir using the id as the basis for
   * the file name.
   */
  protected void saveData(String id, String text) throws IOException {
    String path = this.outputDir + DIR_SEP + this.outputName + "_" + id + ".txt";
    System.out.println("> " + path);

    BufferedWriter bw = new BufferedWriter(new FileWriter(path));
    bw.write(cleanString(text));
    bw.close();
  }

  /**
   * Cleans a unsafe string, removing HTML tags and unescaping any entities present.
   */
  protected String cleanString(String unsafe) {
    unsafe = Jsoup.clean(unsafe, Whitelist.none());
    unsafe = unsafe.replaceAll("&nbsp;", " ");
    unsafe = unsafe.replaceAll("[\\x94\\x92\\x091\\x85]", " ");
    return StringEscapeUtils.unescapeHtml4(unsafe);
  }

  /**
   * Generates a ID used for file names based on the date data present in the string.
   */
  protected String idFromPubDate(String string) {
    try {
      return ID_FORMAT.format(DATE_FORMAT.parse(string));
    } catch(Exception e) {
      return "unknown";
    }
  }

  /**
   * Parses a line as a comma-separated CSV string and returns the column data.
   */
  protected static List<String> parseCSVLine(String line) throws Exception {
    if (line==null)
      return null;

    Vector<String> store = new Vector<String>();
    StringBuffer curVal = new StringBuffer();
    boolean inquotes = false;

    for (int i=0; i<line.length(); i++) {
      char ch = line.charAt(i);
      if (inquotes) {
        if (ch=='\"')
          inquotes = false;
        else
          curVal.append(ch);
      } else {
        if (ch=='\"') {
          inquotes = true;
          if (curVal.length()>0)
            curVal.append('\"');
        } else if (ch==',') {
          store.add(curVal.toString());
          curVal = new StringBuffer();
        } else {
          curVal.append(ch);
        }
      }
    }

    store.add(curVal.toString());
    return store;
  }

  /**
   * Uses Jsoup to retrieve a HTML/XML document from the specified URL.
   */
  protected Document fetchDocument(String url) throws IOException {
    // Ignore content type in case the server decides to return something odd
    return Jsoup.connect(url).ignoreContentType(true).get();
  }

  protected void printError(String msg) {
    System.err.println(cls+"/"+outputName+": "+msg);
  }
}
