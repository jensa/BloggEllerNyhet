import java.io.File;
import java.io.IOException;

import weka.classifiers.functions.VotedPerceptron;
import weka.classifiers.trees.J48;
import weka.core.Instances;
import weka.core.converters.TextDirectoryLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.StringToWordVector;


public class Classifier {
	
	public static final String sep = System.getProperty ("File.separator");
	
	public static final String test = "test";
	public static final String train = "train";
	
	private static int BLOG = 0;
	private static int NEWS = 1;
	
	public static void main (String[] args){
		try {
			new Classifier ().runClassifier ();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void runClassifier () throws Exception{
		TextDirectoryLoader loader = new TextDirectoryLoader();
	    StringToWordVector filter = new StringToWordVector();
	    
	    loader.setDirectory(new File(train));
	    Instances dataRaw = loader.getDataSet();
	    filter.setInputFormat(dataRaw);
	    Instances dataFiltered = Filter.useFilter(dataRaw, filter);
	    
	    loader.setDirectory (new File (test));
	    Instances dataRawTest = loader.getDataSet ();
	    filter.setInputFormat (dataRawTest);
	    Instances testData = Filter.useFilter (dataRawTest, filter);
	    
	    VotedPerceptron classifier = new VotedPerceptron ();
	    classifier.buildClassifier(dataFiltered);
	    
	    classifyInstance (testData, classifier);
	    
	}
	
	private void classifyInstance (Instances data, VotedPerceptron c) throws Exception{
		int blog = 0;
		int news = 0;
		for (int i=0;i<data.numInstances ();i++){
			double result = c.classifyInstance (data.instance (i));
			if (result == BLOG)
				blog++;
			if (result == NEWS)
				news++;
		}
		System.out.println ("Texts classed as news: "+news);
		System.out.println ("Texts classed as blogs: "+blog);
	}
	
	

}
