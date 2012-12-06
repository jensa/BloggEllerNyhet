import weka.core.*;
import weka.core.converters.ConverterUtils.DataSource;
import weka.classifiers.trees.*;

public class TreeClassifier {
  public static void main(String[] args) throws Exception {
    DataSource source = new DataSource(args[0]);
    Instances data = source.getDataSet();
    if (data.classIndex() == -1)
      data.setClassIndex(0);

    // Train classifier and output model
    J48 classifier = new J48();
    classifier.buildClassifier(data);
    System.out.println("\nClassifier model:\n\n" + classifier);
  }
}
