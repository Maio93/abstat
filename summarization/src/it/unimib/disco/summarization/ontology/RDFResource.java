package it.unimib.disco.summarization.ontology;

import org.apache.commons.lang3.StringUtils;

public class RDFResource {
	
	private String uri;

	public RDFResource(String uri){
		this.uri = uri;
	}
	
	public String localName() {
		String[] splitted = StringUtils.split(uri, "/#");
		return splitted[splitted.length - 1];
	}
	
	public String uri(){
		return this.uri;
	}

	public String namespace() {
		return uri.replace(localName(), "");
	}
	
	@Override
	public String toString() {
		return uri();
	}
}