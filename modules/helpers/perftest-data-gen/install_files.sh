#!/bin/sh

set -x

pwd

if [ ! -d ../../enterprise/server/jar/src/test/resources/perftest/ ]
then
	mkdir -p ../../enterprise/server/jar/src/test/resources/perftest/
fi
cp target/*.csv ../../enterprise/server/jar/src/test/resources/perftest/ 
