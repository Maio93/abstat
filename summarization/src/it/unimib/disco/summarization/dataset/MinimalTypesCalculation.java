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

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;

public class MinimalTypesCalculation implements Processing {

	private static TypeGraph graph;
	private static Concepts concepts;
	private static List<String> subclassRelations;
	private File targetDirectory;
	private JavaSparkContext sc;

	public MinimalTypesCalculation(OntModel ontology, File targetDirectory, JavaSparkContext sc) throws Exception {
		Concepts concepts = extractConcepts(ontology);

		this.targetDirectory = targetDirectory;
		MinimalTypesCalculation.concepts = concepts;
		MinimalTypesCalculation.graph = new TypeGraph(concepts, subclassRelations);
		this.sc = sc;
	}

	@Override
	public void endProcessing() throws Exception {
	}

	@Override
	public void process(InputFile types) throws Exception {
		HashMap<String, Integer> conceptCounts = buildConceptCountsFrom(concepts);
		List<String> externalConcepts = new ArrayList<String>();
		Map<String, HashSet<String>> minimalTypes = new HashMap<String, HashSet<String>>();

		try {
			String path = types.name();
			if (sc.master().equals("yarn-cluster"))
				path = "hdfs://master:54310" + path;

			if (types.hasNextLine()) {
				JavaRDD<String> file = sc.textFile(path);
				trackConcept(file, conceptCounts, externalConcepts);
				minimalTypes = trackMinimalType(file);
			}
		} catch (Exception e) {
			Events.summarization().error("error processing " + types.name(), e);
		}

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
			private TypeGraph graph = MinimalTypesCalculation.graph;

			@Override
			public HashSet<String> call(HashSet<String> arg0, HashSet<String> arg1) throws Exception {

				for (String argument1 : arg1) {
					boolean add = true;
					List<String> toDelete = new ArrayList<String>();
					for (String argument0 : arg0) {
						if (!graph.pathsBetween(argument0, argument1).isEmpty()) {
							add = false;
							break;
						}
						if (!graph.pathsBetween(argument1, argument0).isEmpty())
							toDelete.add(argument0);
					}
					if (!toDelete.isEmpty()) {
						for (String arg_del : toDelete) {
							arg0.remove(arg_del);
						}
					}
					if (add)
						arg0.add(argument1);
				}
				return arg0;
			}

		});
		// JavaPairRDD<String,Iterable <HashSet<String>>> tmp =
		// minimalTypes.groupByKey();
		// return tmp.collectAsMap();

		return minimalTypes.collectAsMap();

	}

	private static void trackConcept(JavaRDD<String> file, HashMap<String, Integer> counts,
			List<String> externalConcepts) {
		/*
		 * if(counts.containsKey(concept)) { counts.put(concept,
		 * counts.get(concept) + 1); }else{ externalConcepts.add(entity + "##" +
		 * concept); }
		 */
		JavaPairRDD<String, ArrayList<String>> pairs = file
				.mapToPair(new PairFunction<String, String, ArrayList<String>>() {
					/**
					 * 
					 */
					private static final long serialVersionUID = 1L;

					public Tuple2<String, ArrayList<String>> call(String s) {
						String concept = s.split("##")[2];
						if (!concept.equals(OWL.Thing.getURI())) {
							ArrayList<String> entity = new ArrayList<String>();
							entity.add(s.split("##")[0]);
							return new Tuple2<String, ArrayList<String>>(concept, entity);
						}
						return new Tuple2<String, ArrayList<String>>("", new ArrayList<String>());
					}
				});

		pairs = pairs.filter(new Function<Tuple2<String, ArrayList<String>>, Boolean>() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public Boolean call(Tuple2<String, ArrayList<String>> t) {
				return !(t._1.equals(""));
			}

		});

		JavaPairRDD<String, ArrayList<String>> counts_pairs = pairs
				.reduceByKey(new Function2<ArrayList<String>, ArrayList<String>, ArrayList<String>>() {
					/**
					 * 
					 */
					private static final long serialVersionUID = 1L;

					public ArrayList<String> call(ArrayList<String> a, ArrayList<String> b) {
						a.addAll(b);
						return a;
					}
				});

		Map<String, ArrayList<String>> tmp = counts_pairs.collectAsMap();

		for (String concept : tmp.keySet()) {

			if (counts.containsKey(concept)) {
				counts.put(concept, counts.get(concept) + tmp.get(concept).size());
			} else {
				for (String entity : tmp.get(concept))
					externalConcepts.add(entity + "##" + concept);
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

		MinimalTypesCalculation.subclassRelations = new ArrayList<String>();
		for (List<OntClass> subClasses : extractor.getConceptsSubclassOf().getConceptsSubclassOf()) {
			MinimalTypesCalculation.subclassRelations.add(subClasses.get(0) + "##" + subClasses.get(1));
		}

		OntologyDomainRangeExtractor DRExtractor = new OntologyDomainRangeExtractor();
		DRExtractor.setConceptsDomainRange(concepts, properties);
		return concepts;
	}
}
