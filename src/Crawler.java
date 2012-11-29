import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;
import java.io.IOException;

public class Crawler {
  public static final String OUTPUT_DIR = "data";
  
  public static final String TEST_OUTPUT = Classifier.test;
  public static final String TRAIN_OUTPUT = Classifier.train;
  
  public static final int NUM_PAGES = 10;

  public static void main (String [] args) {
    new Crawler("http://kenzas.se/author/kenza/feed/?paged=%page");
  }

  public Crawler(String url) {
    // Parameterized url (contains variables)?
    if (url.contains("%page")) {
      for (int page = 1; page < NUM_PAGES; ++page)
        crawlWordpress(url.replace("%page", "" + page));
    }
  }

  protected boolean crawlWordpress(String url) {
    try {
      Document doc = Jsoup.connect(url).get();

      Elements items = doc.getElementsByTag("item");
      for (Element item : items) {
        System.out.println(item.getElementsByTag("title").first().text());
      }

      return true;
    } catch(IOException e) {}
    return false;
  }

  protected boolean saveData(String id, String data) {
    return true;
  }
}
