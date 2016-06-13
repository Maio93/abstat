#!/bin/bash

set -e
relative_path=`dirname $0`
root=`cd $relative_path;pwd`
project=$root/../summarization
master=local[4]
name="summarization"

echo "Unit testing the java summarization module"
cd $project
../spark/bin/spark-submit --jars summarization.jar --master $master --name $name --class org.junit.runner.JUnitCore summarization.jar it.unimib.disco.summarization.test.unit.AggregateConceptCountsTest
../spark/bin/spark-submit --jars summarization.jar --master $master --name $name --class org.junit.runner.JUnitCore summarization.jar it.unimib.disco.summarization.test.unit.AKPDatatypeCountTest
../spark/bin/spark-submit --jars summarization.jar --master $master --name $name --class org.junit.runner.JUnitCore summarization.jar it.unimib.disco.summarization.test.unit.AKPObjectCountTest
../spark/bin/spark-submit --jars summarization.jar --master $master --name $name --class org.junit.runner.JUnitCore summarization.jar it.unimib.disco.summarization.test.unit.AllMinimalTypesTest
../spark/bin/spark-submit --jars summarization.jar --master $master --name $name --class org.junit.runner.JUnitCore summarization.jar it.unimib.disco.summarization.test.unit.CalculateMinimalTypesTest
../spark/bin/spark-submit --jars summarization.jar --master $master --name $name --class org.junit.runner.JUnitCore summarization.jar it.unimib.disco.summarization.test.unit.ConceptCountTest
../spark/bin/spark-submit --jars summarization.jar --master $master --name $name --class org.junit.runner.JUnitCore summarization.jar it.unimib.disco.summarization.test.unit.ConceptExtractorTest
../spark/bin/spark-submit --jars summarization.jar --master $master --name $name --class org.junit.runner.JUnitCore summarization.jar it.unimib.disco.summarization.test.unit.DatatypeCountTest
../spark/bin/spark-submit --jars summarization.jar --master $master --name $name --class org.junit.runner.JUnitCore summarization.jar it.unimib.disco.summarization.test.unit.FilesTest
../spark/bin/spark-submit --jars summarization.jar --master $master --name $name --class org.junit.runner.JUnitCore summarization.jar it.unimib.disco.summarization.test.unit.LDSummariesVocabularyTest
../spark/bin/spark-submit --jars summarization.jar --master $master --name $name --class org.junit.runner.JUnitCore summarization.jar it.unimib.disco.summarization.test.unit.MinimalTypesCalculationTest
../spark/bin/spark-submit --jars summarization.jar --master $master --name $name --class org.junit.runner.JUnitCore summarization.jar it.unimib.disco.summarization.test.unit.NTripleFileTest
../spark/bin/spark-submit --jars summarization.jar --master $master --name $name --class org.junit.runner.JUnitCore summarization.jar it.unimib.disco.summarization.test.unit.OntologyDomainRangeExtractorTest
../spark/bin/spark-submit --jars summarization.jar --master $master --name $name --class org.junit.runner.JUnitCore summarization.jar it.unimib.disco.summarization.test.unit.OntologySubclassOfExtractorTest
../spark/bin/spark-submit --jars summarization.jar --master $master --name $name --class org.junit.runner.JUnitCore summarization.jar it.unimib.disco.summarization.test.unit.PartitionedMinimalTypeTest
../spark/bin/spark-submit --jars summarization.jar --master $master --name $name --class org.junit.runner.JUnitCore summarization.jar it.unimib.disco.summarization.test.unit.ProcessDatatypeRelationAssertionsTest
../spark/bin/spark-submit --jars summarization.jar --master $master --name $name --class org.junit.runner.JUnitCore summarization.jar it.unimib.disco.summarization.test.unit.ProcessObjectRelationAssertionsTest
../spark/bin/spark-submit --jars summarization.jar --master $master --name $name --class org.junit.runner.JUnitCore summarization.jar it.unimib.disco.summarization.test.unit.ProcessOntologyTest
../spark/bin/spark-submit --jars summarization.jar --master $master --name $name --class org.junit.runner.JUnitCore summarization.jar it.unimib.disco.summarization.test.unit.PropertyCountTest
../spark/bin/spark-submit --jars summarization.jar --master $master --name $name --class org.junit.runner.JUnitCore summarization.jar it.unimib.disco.summarization.test.unit.RDFResourceTest
../spark/bin/spark-submit --jars summarization.jar --master $master --name $name --class org.junit.runner.JUnitCore summarization.jar it.unimib.disco.summarization.test.unit.RDFTypeOfTest
../spark/bin/spark-submit --jars summarization.jar --master $master --name $name --class org.junit.runner.JUnitCore summarization.jar it.unimib.disco.summarization.test.unit.TextInputTest
../spark/bin/spark-submit --jars summarization.jar --master $master --name $name --class org.junit.runner.JUnitCore summarization.jar it.unimib.disco.summarization.test.unit.TextOutputTest
../spark/bin/spark-submit --jars summarization.jar --master $master --name $name --class org.junit.runner.JUnitCore summarization.jar it.unimib.disco.summarization.test.unit.ToyOntologyTest
../spark/bin/spark-submit --jars summarization.jar --master $master --name $name --class org.junit.runner.JUnitCore summarization.jar it.unimib.disco.summarization.test.unit.TrivialTest
../spark/bin/spark-submit --jars summarization.jar --master $master --name $name --class org.junit.runner.JUnitCore summarization.jar it.unimib.disco.summarization.test.unit.WriteAKPToRDFTest
../spark/bin/spark-submit --jars summarization.jar --master $master --name $name --class org.junit.runner.JUnitCore summarization.jar it.unimib.disco.summarization.test.unit.WriteConceptGraphToRDFTest
../spark/bin/spark-submit --jars summarization.jar --master $master --name $name --class org.junit.runner.JUnitCore summarization.jar it.unimib.disco.summarization.test.unit.WriteConceptToRDFTest
../spark/bin/spark-submit --jars summarization.jar --master $master --name $name --class org.junit.runner.JUnitCore summarization.jar it.unimib.disco.summarization.test.unit.WritePropertiesToRDFTest
cd $root

