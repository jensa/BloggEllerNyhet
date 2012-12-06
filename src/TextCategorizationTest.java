import weka.core.*;
import weka.core.converters.*;
import weka.classifiers.trees.*;
import weka.filters.*;
import weka.filters.unsupervised.attribute.*;

import java.io.*;

/**
 * Example class that converts HTML files stored in a directory structure into 
 * and ARFF file using the TextDirectoryLoader converter. It then applies the
 * StringToWordVector to the data and feeds a J48 classifier with it.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 */
public class TextCategorizationTest {

  /**
   * Expects the first parameter to point to the directory with the text files.
   * In that directory, each sub-directory represents a class and the text
   * files in these sub-directories will be labeled as such.
   *
   * @param args        the commandline arguments
   * @throws Exception  if something goes wrong
   */
  public static void main(String[] args) throws Exception {
    // Convert the directory into a dataset
    TextDirectoryLoader loader = new TextDirectoryLoader();
    loader.setDirectory(new File(args[0]));
    Instances dataRaw = loader.getDataSet();
    //System.out.println("\n\nImported data:\n\n" + dataRaw);

    // Apply the StringToWordVector
    StringToWordVector filter = new StringToWordVector();
    filter.setInputFormat(dataRaw);
    filter.setOptions(new String[] { "-stopwords", "stopwords.txt" });
    Instances dataFiltered = Filter.useFilter(dataRaw, filter);
    //System.out.println("\n\nFiltered data:\n\n" + dataFiltered);

    // Train classifier and output model
    J48 classifier = new J48();
    classifier.buildClassifier(dataFiltered);
    System.out.println("\n\nClassifier model:\n\n" + classifier);
  }
}
