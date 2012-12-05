crawl:
	java -cp bin:libs/commons-lang3-3.1.jar:libs/jsoup-1.7.1.jar Crawler

arff:
	java -cp libs/weka.jar \
		-Dfile.encoding=utf-8 \
		weka.core.converters.TextDirectoryLoader \
		-dir data > data.arff

test:
	java -cp bin:libs/weka.jar TextCategorizationTest data
