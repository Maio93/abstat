package it.unimib.disco.summarization.export;
import it.unimib.disco.summarization.dataset.OverallDatatypeRelationsCounting;
import it.unimib.disco.summarization.dataset.ParallelProcessing;

import java.io.File;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;

public class ProcessDatatypeRelationAssertions {

	public static void main(String[] args) throws Exception {
		
		Events.summarization();
		
		File sourceDirectory = new File(args[0]);
		File minimalTypesDirectory = new File(args[1]);
		File datatypes = new File(new File(args[2]), "count-datatype.txt");
		File properties = new File(new File(args[2]), "count-datatype-properties.txt");
		File akps = new File(new File(args[2]), "datatype-akp.txt");
		
		SparkConf conf = new SparkConf();
		JavaSparkContext sc = new JavaSparkContext(conf.setAppName("summarization"));
		OverallDatatypeRelationsCounting counts = new OverallDatatypeRelationsCounting(datatypes, properties, akps, minimalTypesDirectory);
		counts.setSC(sc);
		
		new ParallelProcessing(sourceDirectory, "_dt_properties.nt").process(counts);
	    
	    counts.endProcessing();
	    sc.stop();
	}	
}
