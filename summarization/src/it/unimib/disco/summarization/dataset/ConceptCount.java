package it.unimib.disco.summarization.dataset;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.PairFunction;

import scala.Tuple2;

public class ConceptCount {

	private static Map<String, Long> conceptCounts = new HashMap<String, Long>();
	private JavaSparkContext sc;
	
	
	public ConceptCount(JavaSparkContext sc) {
		this.sc = sc;
	}
	public ConceptCount process(File file) throws Exception {
		InputFile counts = new TextInput(new FileSystemConnector(file));
		
		if(counts.hasNextLine()){
			
			String path = counts.name();
			if(sc.master().equals("yarn-cluster"))
				path = "hdfs://master:54310" + path;
			
			JavaRDD<String> lines = sc.textFile(path);
			
			Map<String, Long> tmp = new HashMap<String, Long>();
			
			tmp = count_concepts(lines);
			add_to_conceptCounts(tmp);
		}
		return this;
	}
	
	private static Map<String, Long> count_concepts(JavaRDD<String> lines){
		JavaPairRDD<String, Long> conceptCounts = lines.mapToPair(new PairFunction<String, String, Long>() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;
			
			public Tuple2<String, Long> call(String line) throws Exception {
				String[] splitted = line.split("##");
				String concept = splitted[0];
				Long count = Long.parseLong(splitted[1]);
				return new Tuple2<String, Long>(concept, count);
			}
			
		});	
		
		return conceptCounts.collectAsMap();
	}
	
	private void add_to_conceptCounts(Map<String, Long> tmp){
		for(Entry<String, Long> concept : tmp.entrySet()){
			String key = concept.getKey();
			Long value = concept.getValue();
			if(!conceptCounts.containsKey(key)) conceptCounts.put(key, 0l);
			conceptCounts.put(key, conceptCounts.get(key) + value);
		}
	}

	public ConceptCount writeResultsTo(File results) throws Exception {
		BulkTextOutput output = new BulkTextOutput(new FileSystemConnector(results), 10000);
		for(Entry<String, Long> count : conceptCounts.entrySet()){
			if(count.getValue() > 0) output.writeLine(count.getKey() + "##" + count.getValue());
		}
		output.close();
		return this;
	}
}
