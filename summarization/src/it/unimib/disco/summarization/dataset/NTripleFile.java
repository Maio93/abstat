package it.unimib.disco.summarization.dataset;

import it.unimib.disco.summarization.export.Events;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;

public class NTripleFile {

	private static NTripleAnalysis[] analyzers;
	private JavaSparkContext sc;
	// private SparkConf conf;

	public NTripleFile(JavaSparkContext sc, NTripleAnalysis... analyzers) {

		NTripleFile.analyzers = analyzers;
		this.sc = sc;
		// conf = new SparkConf().set("spark.driver.allowMultipleContexts",
		// "true");
	}

	public void process(InputFile file) throws Exception {
		/*
		 * while(file.hasNextLine()){ String line = file.nextLine(); try{
		 * String[] splitted = line.split("##"); String subject = splitted[0];
		 * String property = splitted[1]; String object = splitted[2]; String
		 * datatype = "";
		 * 
		 * if(splitted.length > 3){ datatype = "^^<" + splitted[3] + ">"; }
		 * 
		 * if(!object.startsWith("\"")){ object = "<" + object + ">"; }
		 * 
		 * line = "<" + subject + "> <" + property + "> " + object + datatype +
		 * " .";
		 * 
		 * Model model = ModelFactory.createDefaultModel();
		 * model.read(IOUtils.toInputStream(line) ,null, "N-TRIPLES"); Statement
		 * statement = model.listStatements().next();
		 * 
		 * 
		 * NTriple triple = new NTriple(statement); for(NTripleAnalysis analysis
		 * : analyzers){ analysis.track(triple); } }catch(Exception e){
		 * Events.summarization().error("error processing " + line + " from " +
		 * file.name(), e); } }
		 */
		if (file.hasNextLine()) {
			try {

				String path = file.name();

				if (sc.master().equals("yarn-cluster"))
					path = "hdfs://master:54310" + path;

				JavaRDD<String> lines = sc.textFile(path);

				JavaRDD<String> statements = calculate_statements(lines);
				List<String> refined_lines = new ArrayList<String>();
				refined_lines = statements.collect();

				for (String refined_line : refined_lines) {
					Statement statement = extract_statement(refined_line);
					NTriple triple = new NTriple(statement);
					for (NTripleAnalysis analysis : analyzers) {
						analysis.track(triple);
					}
				}

			} catch (Exception e) {
				Events.summarization().error("error processing a line from " + file.name(), e);
			}
		}
	}

	private static JavaRDD<String> calculate_statements(JavaRDD<String> lines) throws Exception {
		JavaRDD<String> statements = lines.map(new Function<String, String>() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public String call(String line) {
				String[] splitted = line.split("##");
				String subject = splitted[0];
				String property = splitted[1];
				String object = splitted[2];
				String datatype = "";

				if (splitted.length > 3) {
					datatype = "^^<" + splitted[3] + ">";
				}

				if (!object.startsWith("\"")) {
					object = "<" + object + ">";
				}

				String refined_line = "<" + subject + "> <" + property + "> " + object + datatype + " .";

				return refined_line;
			}
		});

		return statements;
	}

	private static Statement extract_statement(String line) {
		Model model = ModelFactory.createDefaultModel();
		model.read(IOUtils.toInputStream(line), null, "N-TRIPLES");
		Statement statement = model.listStatements().next();
		return statement;
	}
}
