#!/bin/bash

function as_absolute(){
	echo `cd $1; pwd`
}

function export_rdf(){
	command=$1	
	from=$2
	to=$3
	dataset=$4
	domain=$5
	type=$6

	echo "Exporting $from as $to"
	java -Xms256m -Xmx16g -cp .:'summarization.jar' it.unimib.disco.summarization.export.$command $from $to $dataset $domain $type
	echo "Done"
}

set -e
relative_path=`dirname $0`
root=`cd $relative_path;pwd`
project=$root/../summarization

output=$root/../summarization-output
mkdir -p $output

dataset=$root/../data/summaries/$1
input_directory=$(as_absolute $dataset)/patterns
output_directory=$(as_absolute $output)
graph=http://ld-summaries.org/$1
domain=$2

cd $project
export_rdf WriteConceptToRDF $input_directory/count-concepts.txt $output_directory/count-concepts.nt $graph $domain
export_rdf WriteDatatypeToRDF $input_directory/count-datatype.txt $output_directory/count-datatype.nt $graph $domain
export_rdf WriteObjectPropertyToRDF $input_directory/count-object-properties.txt $output_directory/count-properties.nt $graph $domain
export_rdf WriteDatatypePropertyToRDF $input_directory/count-datatype-properties.txt $output_directory/count-datatype-properties.nt $graph $domain
export_rdf WriteAKPToRDF $input_directory/object-akp.txt $output_directory/relation-count.nt $graph $domain object
export_rdf WriteAKPToRDF $input_directory/datatype-akp.txt $output_directory/relation-datatype-count.nt $graph $domain datatype
export_rdf WriteConceptGraphToRDF "$input_directory/../reports/tmp-data-for-computation/SubclassOf.txt" $output_directory/concept-graph.nt $graph
 
cd $root

echo "Deleting graph ${graph}"
./isql.sh "SPARQL CLEAR GRAPH <$graph>;"

echo "Indexing the result of the analysis in ${output_directory} into graph ${graph}"
./isql.sh "delete from DB.DBA.load_list;ld_dir ('${output_directory}', '*.nt', '${graph}');rdf_loader_run();checkpoint;"
echo "Done"

rm -r $output_directory
