import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.safety.*;
import org.jsoup.select.*;

public class Crawler {
  public static final String DIR_SEP = System.getProperty("File.separator");
  public static final String BASE_OUTPUT_DIR = "data";
  
  //public static final String TEST_OUTPUT = Classifier.test;
  //public static final String TRAIN_OUTPUT = Classifier.train;
  
  public static final int NUM_PAGES = 10;
  public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
  public static final SimpleDateFormat ID_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

  public static void main (String [] args) throws Exception {
      new Crawler("kenza", "blogg", "http://kenzas.se/author/kenza/feed/?paged=%page");
  }

  public String outputDir;

  public Crawler(String name, String cls, String url) throws Exception {
    this.outputDir = BASE_OUTPUT_DIR + DIR_SEP + cls + DIR_SEP + name;
    (new File(this.outputDir)).mkdirs();

    // Parameterized url (contains variables)?
    if (url.contains("%page")) {
      for (int page = 1; page <= NUM_PAGES; ++page)
        crawlFeed(url.replace("%page", "" + page));
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
}
