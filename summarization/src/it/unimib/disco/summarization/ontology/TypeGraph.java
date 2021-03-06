package it.unimib.disco.summarization.ontology;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.jgraph.graph.DefaultEdge;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph;

public class TypeGraph implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private DirectedAcyclicGraph<String, DefaultEdge> graph;

	public TypeGraph(Concepts concepts, List<String> subClasses) throws Exception{
		this.graph = subTypeGraphFrom(concepts, subClasses);
	}
	
	public List<List<String>> pathsBetween(String leaf, String root){
		ArrayList<List<String>> paths = new ArrayList<List<String>>();
		if(graph.containsVertex(leaf) && graph.containsVertex(root)){
			inOrderTraversal(leaf, root, new ArrayList<String>(), paths);
		}
		return paths;
	}
	
	private void inOrderTraversal(String leaf, String root, List<String> currentPath, List<List<String>> paths){
		ArrayList<String> path = new ArrayList<String>(currentPath);
		path.add(leaf);
		if(leaf.equals(root)){
			paths.add(path);
		}
		for(DefaultEdge edgeToSuperType : graph.outgoingEdgesOf(leaf)){
			String superType = graph.getEdgeTarget(edgeToSuperType);
			inOrderTraversal(superType, root, path, paths);
		}
	}
	
	private static DirectedAcyclicGraph<String, DefaultEdge> subTypeGraphFrom(Concepts concepts, List<String> subclassRelations) throws Exception {
		DirectedAcyclicGraph<String, DefaultEdge> typeGraph = new DirectedAcyclicGraph<String, DefaultEdge>(DefaultEdge.class);
		
		Concepts tmp_concepts = new Concepts();
		tmp_concepts = concepts;
		for(String concept : tmp_concepts.getConcepts().keySet()){
			typeGraph.addVertex(concept);
		}
		
		for(String line : subclassRelations){
			String[] relation = line.split("##");
			String subtype = relation[0];
			String supertype = relation[1];
			
			if(!typeGraph.containsVertex(subtype)) typeGraph.addVertex(subtype);
			if(!typeGraph.containsVertex(supertype)) typeGraph.addVertex(supertype);
			
			typeGraph.addEdge(subtype, supertype);
		}
		
		return typeGraph;
	}
	
	public void Stampa() {
		System.out.println("prova");
	}
}
