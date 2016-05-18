package it.unimib.disco.summarization.dataset;

import it.unimib.disco.summarization.export.Events;
import it.unimib.disco.summarization.ontology.ConceptExtractor;
import it.unimib.disco.summarization.ontology.Concepts;
import it.unimib.disco.summarization.ontology.OntologyDomainRangeExtractor;
import it.unimib.disco.summarization.ontology.OntologySubclassOfExtractor;
import it.unimib.disco.summarization.ontology.Properties;
import it.unimib.disco.summarization.ontology.PropertyExtractor;
import it.unimib.disco.summarization.ontology.TypeGraph;

import scala.Tuple2;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.vocabulary.OWL;

import org.apache.spark.*;
import org.apache.spark.api.java.*;
import org.apache.spark.api.java.function.*;

public class MinimalTypesCalculation implements Processing {

	private static TypeGraph graph;
	private Concepts concepts;
	private List<String> subclassRelations;
	private File targetDirectory;
	private SparkConf conf;
	private JavaSparkContext sc;

	public MinimalTypesCalculation(OntModel ontology, File targetDirectory) throws Exception {
		Concepts concepts = extractConcepts(ontology);

		this.targetDirectory = targetDirectory;
		this.concepts = concepts;
		MinimalTypesCalculation.graph = new TypeGraph(concepts, subclassRelations);

		conf = new SparkConf().setAppName("Summarization").setMaster("local[4]")
				.set("spark.driver.allowMultipleContexts", "true");
	}

	@Override
	public void endProcessing() throws Exception {
	}

	@Override
	public void process(InputFile types) throws Exception {
		HashMap<String, Integer> conceptCounts = buildConceptCountsFrom(concepts);
		List<String> externalConcepts = new ArrayList<String>();
		Map<String, HashSet<String>> minimalTypes = new HashMap<String, HashSet<String>>();

		sc = new JavaSparkContext(conf);
		sc.setLocalProperty("spark.driver.allowMultipleContexts", "true");
		HDFS hdfs = new HDFS(types);
		try {
			String path = types.name();

			if (types.hasNextLine()) {
				//String path = hdfs.createHadoopCopy();
				JavaRDD<String> file = sc.textFile(path);

				trackConcept(file, conceptCounts, externalConcepts);
				minimalTypes = trackMinimalType(file);

				//hdfs.deleteHadoopCopy();
			}
		} catch (Exception e) {
			Events.summarization().error("error processing " + types.name(), e);
		}

		sc.close();
		String prefix = new Files().prefixOf(types);
		writeConceptCounts(conceptCounts, targetDirectory, prefix);
		writeExternalConcepts(externalConcepts, targetDirectory, prefix);
		writeMinimalTypes(minimalTypes, targetDirectory, prefix);

	}

	private static Map<String, HashSet<String>> trackMinimalType(JavaRDD<String> file) {
		/*
		 * if (!minimalTypes.containsKey(entity)) minimalTypes.put(entity, new
		 * HashSet<String>()); for (String minimalType : new
		 * HashSet<String>(minimalTypes.get(entity))) { if
		 * (!graph.pathsBetween(minimalType, concept).isEmpty()) { return; } if
		 * (!graph.pathsBetween(concept, minimalType).isEmpty()) {
		 * minimalTypes.get(entity).remove(minimalType); } }
		 * minimalTypes.get(entity).add(concept);
		 */

		JavaPairRDD<String, HashSet<String>> minimalTypes = file
				.mapToPair(new PairFunction<String, String, HashSet<String>>() {
					/**
					 * 
					 */
					private static final long serialVersionUID = 1L;

					public Tuple2<String, HashSet<String>> call(String line) {
						String[] resources = line.split("##");

						String entity = resources[0];
						String concept = resources[2];

						if (!concept.equals(OWL.Thing.getURI())) {
							HashSet<String> set = new HashSet<String>();
							set.add(concept);

							return new Tuple2<String, HashSet<String>>(entity, set);
						}
						return new Tuple2<String, HashSet<String>>("", new HashSet<String>());
					}
				});

		minimalTypes = minimalTypes.filter(new Function<Tuple2<String, HashSet<String>>, Boolean>() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public Boolean call(Tuple2<String, HashSet<String>> t) {
				return !(t._1.equals(""));
			}

		});

		minimalTypes = minimalTypes.reduceByKey(new Function2<HashSet<String>, HashSet<String>, HashSet<String>>() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public HashSet<String> call(HashSet<String> a, HashSet<String> b) {
				boolean add;

				for (String minimalType_b : b) {
					add = true;
					HashSet<String> c = new HashSet<String>(a);
					for (String minimalType_a : c) {
						if (!empty_path(minimalType_a, minimalType_b)) {
							add = false;
							break;
						}
						if (!empty_path(minimalType_b, minimalType_a)) {
							a.remove(minimalType_a);
						}
					}
					if (add)
						a.add(minimalType_b);
				}

				return a;
			}
		});

		return minimalTypes.collectAsMap();

	}

	private static boolean empty_path(String leaf, String root) {
		return graph.pathsBetween(leaf, root).isEmpty();
	}

	private static void trackConcept(JavaRDD<String> file, HashMap<String, Integer> counts,
			List<String> externalConcepts) {
		/*
		 * if(counts.containsKey(concept)) { counts.put(concept,
		 * counts.get(concept) + 1); }else{ externalConcepts.add(entity + "##" +
		 * concept); }
		 */
		JavaPairRDD<String, Integer> pairs = file.mapToPair(new PairFunction<String, String, Integer>() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public Tuple2<String, Integer> call(String s) {
				return new Tuple2<String, Integer>(s, 1);
			}
		});

		JavaPairRDD<String, Integer> counts_pairs = pairs.reduceByKey(new Function2<Integer, Integer, Integer>() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public Integer call(Integer a, Integer b) {
				return a + b;
			}
		});

		Map<String, Integer> tmp = counts_pairs.collectAsMap();

		for (String line : tmp.keySet()) {
			String[] resources = line.split("##");

			String entity = resources[0];
			String concept = resources[2];

			if (!concept.equals(OWL.Thing.getURI())) {
				if (counts.containsKey(concept)) {
					counts.put(concept, tmp.get(line));
				} else {
					externalConcepts.add(entity + "##" + concept);
				}
			}
		}

	}

	private void writeExternalConcepts(List<String> externalConcepts, File directory, String prefix) throws Exception {
		BulkTextOutput externalConceptFile = connectorTo(directory, prefix, "uknHierConcept");
		for (String line : externalConcepts) {
			externalConceptFile.writeLine(line);
		}
		externalConceptFile.close();
	}

	private void writeConceptCounts(HashMap<String, Integer> conceptCounts, File directory, String prefix)
			throws Exception {
		BulkTextOutput countConceptFile = connectorTo(directory, prefix, "countConcepts");
		for (Entry<String, Integer> concept : conceptCounts.entrySet()) {
			countConceptFile.writeLine(concept.getKey() + "##" + concept.getValue());
		}
		countConceptFile.close();
	}

	private void writeMinimalTypes(Map<String, HashSet<String>> minimalTypes, File directory, String prefix)
			throws Exception {
		BulkTextOutput connector = connectorTo(directory, prefix, "minType");
		for (Entry<String, HashSet<String>> entityTypes : minimalTypes.entrySet()) {
			ArrayList<String> types = new ArrayList<String>(entityTypes.getValue());
			Collections.sort(types);
			connector.writeLine(types.size() + "##" + entityTypes.getKey() + "##" + StringUtils.join(types, "#-#"));
		}
		connector.close();
	}

	private HashMap<String, Integer> buildConceptCountsFrom(Concepts concepts) throws Exception {
		HashMap<String, Integer> conceptCounts = new HashMap<String, Integer>();
		for (String concept : concepts.getConcepts().keySet()) {
			conceptCounts.put(concept, 0);
		}
		return conceptCounts;
	}

	private BulkTextOutput connectorTo(File directory, String prefix, String name) {
		return new BulkTextOutput(new FileSystemConnector(new File(directory, prefix + "_" + name + ".txt")), 1000);
	}

	private Concepts extractConcepts(OntModel ontology) {
		PropertyExtractor pExtract = new PropertyExtractor();
		pExtract.setProperty(ontology);

		Properties properties = new Properties();
		properties.setProperty(pExtract.getProperty());
		properties.setExtractedProperty(pExtract.getExtractedProperty());
		properties.setCounter(pExtract.getCounter());

		ConceptExtractor cExtract = new ConceptExtractor();
		cExtract.setConcepts(ontology);

		Concepts concepts = new Concepts();
		concepts.setConcepts(cExtract.getConcepts());
		concepts.setExtractedConcepts(cExtract.getExtractedConcepts());
		concepts.setObtainedBy(cExtract.getObtainedBy());

		OntologySubclassOfExtractor extractor = new OntologySubclassOfExtractor();
		extractor.setConceptsSubclassOf(concepts, ontology);

		this.subclassRelations = new ArrayList<String>();
		for (List<OntClass> subClasses : extractor.getConceptsSubclassOf().getConceptsSubclassOf()) {
			this.subclassRelations.add(subClasses.get(0) + "##" + subClasses.get(1));
		}

		OntologyDomainRangeExtractor DRExtractor = new OntologyDomainRangeExtractor();
		DRExtractor.setConceptsDomainRange(concepts, properties);
		return concepts;
	}
}
