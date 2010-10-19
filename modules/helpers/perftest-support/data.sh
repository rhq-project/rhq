#!/bin/sh

java -cp 'target/dependency/*':target/perftest-support-4.0.0-SNAPSHOT.jar org.rhq.helpers.perftest.support.Main $@
