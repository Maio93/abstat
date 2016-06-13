package it.unimib.disco.summarization.dataset;

import it.unimib.disco.summarization.export.Events;

import org.apache.commons.io.IOUtils;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.VoidFunction;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;

public class NTripleFile {

	private static NTripleAnalysis[] analyzers;
	private JavaSparkContext sc;
	//private SparkConf conf;

	public NTripleFile(NTripleAnalysis... analyzers) {

		NTripleFile.analyzers = analyzers;
		
		//conf = new SparkConf().set("spark.driver.allowMultipleContexts", "true");
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
			//HDFS hdfs = new HDFS(file);
			sc = new JavaSparkContext(new SparkConf());
			try {

				String path = file.name();

				//String path = hdfs.createHadoopCopy();
				
				JavaRDD<String> lines = sc.textFile(path);

				JavaRDD<Statement> statements = calculate_statements(lines);
				analysis_statements(statements);

				//hdfs.deleteHadoopCopy();
			} catch (Exception e) {
				Events.summarization().error("error processing a line from " + file.name(), e);
			}
			sc.close();
		}
	}

	private static JavaRDD<Statement> calculate_statements(JavaRDD<String> lines) throws Exception {
		JavaRDD<Statement> statements = lines.map(new Function<String, Statement>() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public Statement call(String line) {
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

				return extract_statement(refined_line);
			}
		});

		return statements;
	}

	private static void analysis_statements(JavaRDD<Statement> statements) throws Exception {
		statements.foreach(new VoidFunction<Statement>() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public void call(Statement statement) {
				NTriple triple = new NTriple(statement);
				for (NTripleAnalysis analysis : analyzers) {
					analysis.track(triple);
				}
			}
		});
	}

	private static Statement extract_statement(String line) {
		Model model = ModelFactory.createDefaultModel();
		model.read(IOUtils.toInputStream(line), null, "N-TRIPLES");
		Statement statement = model.listStatements().next();
		return statement;
	}
}
