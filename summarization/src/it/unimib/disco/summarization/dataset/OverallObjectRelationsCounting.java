package it.unimib.disco.summarization.dataset;

import java.io.File;
import java.util.Vector;

import org.apache.spark.api.java.JavaSparkContext;

public class OverallObjectRelationsCounting implements Processing {

	private Vector<NTripleAnalysis> propertiesCount;
	private Vector<NTripleAnalysis> akpCounts;
	private File propertyFile;
	private File akps;
	private MinimalTypes minimalTypesOracle;
	private JavaSparkContext sc;

	public OverallObjectRelationsCounting(File propertyFile, File akps, File types) throws Exception {
		this.propertiesCount = new Vector<NTripleAnalysis>();
		this.akpCounts = new Vector<NTripleAnalysis>();
		this.propertyFile = propertyFile;
		this.akps = akps;
		this.minimalTypesOracle = new AllMinimalTypes(types);
	}

	@Override
	public void process(InputFile file) throws Exception {
		PropertyCount propertyCount = new PropertyCount();
		AKPObjectCount akpCount = new AKPObjectCount(minimalTypesOracle);

		new NTripleFile(sc, propertyCount, akpCount).process(file);

		propertiesCount.add(propertyCount);
		akpCounts.add(akpCount);
	}

	public void setSc(JavaSparkContext sc) {
		this.sc = sc;
	}

	public void endProcessing() throws Exception {
		new AggregatedCount(propertiesCount).writeTo(propertyFile);
		new AggregatedCount(akpCounts).writeTo(akps);

	}

}
