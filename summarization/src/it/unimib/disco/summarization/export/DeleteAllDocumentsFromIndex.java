package it.unimib.disco.summarization.export;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;

public class DeleteAllDocumentsFromIndex
{
	public static void main (String[] args) throws SolrServerException, IOException
	{
		String host = args[0];
		String port = args[1];
		String datasetInput = args[2];
		
		String serverUrl = "http://"+host+":"+port+"/solr/indexing";
		HttpSolrServer client = new HttpSolrServer(serverUrl);
		
		client.deleteByQuery("dataset:" + datasetInput);
		
		client.commit();
	}
}