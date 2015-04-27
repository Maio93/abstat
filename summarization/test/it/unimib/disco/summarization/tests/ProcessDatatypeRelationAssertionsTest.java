package it.unimib.disco.summarization.tests;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;
import it.unimib.disco.summarization.output.ProcessDatatypeRelationAssertions;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import com.hp.hpl.jena.vocabulary.RDFS;

public class ProcessDatatypeRelationAssertionsTest extends TestWithTemporaryData{

	@Test
	public void shouldCountAndAggregateDatatypeOccurrences() throws Exception {
		
		temporary.namedFile("entity##http://property1##\"35\"##type", "a_dt_properties.nt");
		temporary.namedFile("entity##http://property1##\"82\"##type", "b_dt_properties.nt");
		temporary.namedFile("entity##http://property2##\"35\"", "c_dt_properties.nt");
		temporary.namedFile("entity##http://property2##\"a string\"", "d_dt_properties.nt");
		
		ProcessDatatypeRelationAssertions.main(new String[]{
			temporary.directory().getAbsolutePath(),
			temporary.directory().getAbsolutePath(),
		});
		
		List<String> datatypes = linesOf("count-datatype.txt");
		assertThat(datatypes, hasItem("type##2"));
		assertThat(datatypes, hasItem(RDFS.Literal.getURI() + "##2"));
		
		List<String> properties = linesOf("count-datatype-properties.txt");
		assertThat(properties, hasItem("http://property1##2"));
		assertThat(properties, hasItem("http://property2##2"));
	}
	
	private List<String> linesOf(String file) throws Exception {
		return FileUtils.readLines(new File(temporary.directory(), file));
	}
}
