package it.unimib.disco.summarization.tests;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;

public class UnitTest{
	
	public TemporaryFolder temporary;
	
	@Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();
	
	@Before
	public void eraseTemporaryFolder(){
		temporary = new TemporaryFolder();
		temporary.create();
	}
	
	@After
	public void deleteTemporaryFolders(){
		temporary.delete();
	}
}