#!/bin/bash

function run_experiment(){
	echo "*************** Running experiment $@ ***************"
	echo
	java -Xms256m -Xmx16g -cp .:'ontology_summarization.jar' it.unimib.disco.summarization.experiments.$@
	echo "*************** done ***************"
	echo
}

set -e
relative_path=`dirname $0`
root=`cd $relative_path;pwd`
project=$root/../summarization

cd $root
./build-java-summarization-module.sh
cd $project

run_experiment UnderspecifiedProperties music-ontology/mo.owl linked-brainz
run_experiment DomainRangeViolations music-ontology/mo.owl linked-brainz
run_experiment UnderspecifiedProperties dbpedia/dbpedia_2014.owl dbpedia2014
run_experiment DomainRangeViolations dbpedia/dbpedia_2014.owl dbpedia2014

