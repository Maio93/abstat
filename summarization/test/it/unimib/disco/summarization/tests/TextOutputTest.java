package it.unimib.disco.summarization.tests;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import it.unimib.disco.summarization.utility.FileSystemConnector;
import it.unimib.disco.summarization.utility.TextOutput;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class TextOutputTest extends TestWithTemporaryData{

	@Test
	public void shouldWriteStrings() throws Exception {
		
		File temporaryFile = temporary.newFile();
		
		TextOutput textFile = new TextOutput(new FileSystemConnector(temporaryFile));
		
		textFile.writeLine("the line").close();
		
		assertThat(FileUtils.readLines(temporaryFile), hasSize(1));
	}
}
