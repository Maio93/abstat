#!/bin/bash

set -e
relative_path=`dirname $0`
root=`cd $relative_path;pwd`
project=$root/../summarization

echo "Running the system tests"
cd $project
java -Xms256m -Xmx1g -cp .:'summarization.jar' org.junit.runner.JUnitCore it.unimib.disco.summarization.test.system.SystemTests
cd $root

