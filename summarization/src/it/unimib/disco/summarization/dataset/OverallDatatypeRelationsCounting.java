package it.unimib.disco.summarization.dataset;

import java.io.File;
import java.util.Vector;

import org.apache.spark.api.java.JavaSparkContext;

public class OverallDatatypeRelationsCounting implements Processing {

	private Vector<NTripleAnalysis> datatypesCount;
	private Vector<NTripleAnalysis> propertiesCount;
	private Vector<NTripleAnalysis> akpCounts;
	private File datatypeCountsResults;
	private File propertyFile;
	private File akps;
	private File minimalTypes;
	private JavaSparkContext sc;

	public OverallDatatypeRelationsCounting(File datatypeFile, File propertyFile, File akps, File minimalTypes) {
		this.datatypesCount = new Vector<NTripleAnalysis>();
		this.propertiesCount = new Vector<NTripleAnalysis>();
		this.akpCounts = new Vector<NTripleAnalysis>();
		this.datatypeCountsResults = datatypeFile;
		this.propertyFile = propertyFile;
		this.akps = akps;
		this.minimalTypes = minimalTypes;
	}

	@Override
	public void process(InputFile file) throws Exception {
		DatatypeCount analysis = new DatatypeCount();
		PropertyCount propertyCount = new PropertyCount();

		String prefix = new Files().prefixOf(file);
		File minimalTypesFile = new File(minimalTypes, prefix + "_minType.txt");
		AKPDatatypeCount akpCount = new AKPDatatypeCount(new TextInput(new FileSystemConnector(minimalTypesFile)));

		new NTripleFile(sc, analysis, propertyCount, akpCount).process(file);

		datatypesCount.add(analysis);
		propertiesCount.add(propertyCount);
		akpCounts.add(akpCount);
	}
	
	public void setSC (JavaSparkContext sc){
		this.sc = sc;
	}

	public void endProcessing() throws Exception {
		new AggregatedCount(datatypesCount).writeTo(datatypeCountsResults);
		new AggregatedCount(propertiesCount).writeTo(propertyFile);
		new AggregatedCount(akpCounts).writeTo(akps);
	}
}