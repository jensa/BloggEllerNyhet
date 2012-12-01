import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.lang.IllegalArgumentException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.safety.*;
import org.jsoup.select.*;

public class Crawler {
  public static final String DIR_SEP = System.getProperty("file.separator");
  public static final String BASE_OUTPUT_DIR = "data";
  public static final String DEFAULT_SOURCE_FILE = "sources.txt";
  
  //public static final String TEST_OUTPUT = Classifier.test;
  //public static final String TRAIN_OUTPUT = Classifier.train;
  
  public static final int NUM_PAGES = 10;

  public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
  public static final SimpleDateFormat ID_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

  public static void main (String [] args) throws Exception {
    LineNumberReader lnr = null;
    
    // Crawl URLs listed in sources file
    try {
      lnr = new LineNumberReader(new FileReader(DEFAULT_SOURCE_FILE));
      String line;

      // Parse and process each line
      while ((line = lnr.readLine()) != null) {
        try {
          List<String> params = parseCSVLine(line);
          if (params.size() < 3)
            throw new IllegalArgumentException("Invalid number of arguments (should be 3).");

          new Crawler(params.get(0), params.get(1), params.get(2));
        } catch(IllegalArgumentException e) {
          System.err.println("Could not parse line " + lnr.getLineNumber() +
              ": " + e.getMessage());
        } catch (Exception e) {
          System.err.println("Error on line" + lnr.getLineNumber() + ": " +
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

  public String outputDir;

  public Crawler(String name, String cls, String url) throws Exception {
    this.outputDir = BASE_OUTPUT_DIR + DIR_SEP + cls + DIR_SEP + name;
    (new File(this.outputDir)).mkdirs();

    // Parameterized url (contains variables)?
    if (url.contains("%page")) {
      for (int page = 1; page <= NUM_PAGES; ++page)
        crawlFeed(url.replace("%page", "" + page));
    } else {
      crawlFeed(url);
    }
  }

  protected boolean crawlFeed(String url) {
    try {
      System.out.println("Crawling " + url);
      Document doc = Jsoup.connect(url).get();

      Elements items = doc.getElementsByTag("item");
      for (Element item : items) {
        String id = idFromPubDate(item.getElementsByTag("pubDate").first().text());
        Elements contents = item.select("content|encoded, content");

        if (contents.size() > 0) {
          String data = cleanString(contents.first().text());
          saveData(id, data);
        } else {
          System.err.println(id + ": No content tag found, skipping");
        }
      }

      return true;
    } catch(IOException e) {}
    return false;
  }

  protected void saveData(String id, String data) throws IOException {
    String path = this.outputDir + DIR_SEP + id + ".txt";
    System.out.println(path);

    BufferedWriter bw = new BufferedWriter(new FileWriter(path));
    bw.write(data);
    bw.close();
  }

  protected String cleanString(String unsafe) {
    unsafe = Jsoup.clean(unsafe, Whitelist.none());
    unsafe = unsafe.replaceAll("&nbsp;", " ");
    return StringEscapeUtils.unescapeHtml4(unsafe);
  }

  protected String idFromPubDate(String string) {
    try {
      return ID_FORMAT.format(DATE_FORMAT.parse(string));
    } catch(ParseException e) {
      return "unknown-id";
    }
  }

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
}
