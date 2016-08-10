package it.unimib.disco.summarization.export;

import it.unimib.disco.summarization.dataset.ConceptCount;
import it.unimib.disco.summarization.dataset.Files;

import java.io.File;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;

public class AggregateConceptCounts {

	public static void main(String[] args) throws Exception {
		
		Events.summarization();
		
		File sourceDirectory = new File(args[0]);
		File targetFile = new File(args[1], "count-concepts.txt");
		
		SparkConf conf = new SparkConf();
		JavaSparkContext sc = new JavaSparkContext(conf.setAppName("summarization"));
		ConceptCount counts = new ConceptCount(sc);
		for(File file : new Files().get(sourceDirectory, "_countConcepts.txt")){
			try{
				counts.process(file);
			}catch(Exception e){
				Events.summarization().error("processing " + file, e);
			}
		}
		
		counts.writeResultsTo(targetFile);
		sc.stop();
	}
}
