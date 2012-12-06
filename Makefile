crawl:
	java -cp bin:libs/commons-lang3-3.1.jar:libs/jsoup-1.7.1.jar Crawler data/train

arff:
	java -cp bin:libs/weka.jar -Dfile.encoding=utf-8 \
		ConvertDirectoryToArff data/train -stopwords stopwords.txt -L > data/train.arff

tree:
	java -cp bin:libs/weka.jar TreeClassifier data/train.arff

standardize:
	java -cp bin:libs/weka.jar weka.filters.unsupervised.attribute.Standardize \
		-b -i data/train.arff -o data/train_std.arff -r data/test.arff -s data/test_std.arff
	
