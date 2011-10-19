#!/bin/sh

java $JAVA_OPTS -cp 'target/dependency/*':target/perftest-support-4.1.0-SNAPSHOT.jar org.rhq.helpers.perftest.support.Main $@
