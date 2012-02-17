#!/bin/sh

RHQ_DEV_HOME=${HOME}/dev/jboss/rhq
M2_REPO=${HOME}/.m2/repository

java -cp target/rhq-jboss-as-7-plugin-4.3.0-SNAPSHOT.jar:${RHQ_DEV_HOME}/modules/enterprise/agent/target/rhq-agent/lib/commons-logging-1.1.0.jboss.jar:${M2_REPO}/org/codehaus/jackson/jackson-core-asl/1.7.4/jackson-core-asl-1.7.4.jar:${M2_REPO}/org/codehaus/jackson/jackson-mapper-asl/1.7.4/jackson-mapper-asl-1.7.4.jar org.rhq.modules.plugins.jbossas7.Domain2Descriptor $*
