import weka.core.*;
import weka.core.converters.*;
import weka.filters.*;
import weka.filters.unsupervised.attribute.*;

import java.io.*;

public class ConvertDirectoryToArff {
  /**
   * Converts a specified directory into ARFF format and outputs it to STDOUT.
   * Any additional parameters are sent as options to the StringToWordVector instance.
   */
  public static void main(String[] args) throws Exception {
    // Convert the directory into a dataset
    TextDirectoryLoader loader = new TextDirectoryLoader();
    loader.setDirectory(new File(args[0]));
    Instances instances = loader.getDataSet();

    // Apply the StringToWordVector
    StringToWordVector filter = new StringToWordVector();
    filter.setInputFormat(instances);

    String[] options;
    if (args.length > 1) { // Custom filter options provided in args
      options = new String[args.length - 1];
      System.arraycopy(args, 1, options, 0, options.length);
    } else { // Default filter options
      options = new String[] { "-stopwords", "stopwords.txt", "-L" };
    }
    filter.setOptions(options);

    instances = Filter.useFilter(instances, filter);

    // Output data set in ARFF format
    System.out.println(instances);
  }
}
