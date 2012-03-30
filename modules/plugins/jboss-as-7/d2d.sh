#!/bin/sh 

RHQ_DEV_HOME=${HOME}/im/rhq
M2_REPO=${HOME}/.m2/repository
RHQ_VERSION='4.4.0-SNAPSHOT'
OPTS=""
#OPTS="-agentlib:jdwp=transport=dt_socket,address=8790,server=y,suspend=y"

java $OPTS -cp target/rhq-jboss-as-7-plugin-${RHQ_VERSION}.jar:${M2_REPO}/commons-logging/commons-logging-api/1.1/commons-logging-api-1.1.jar:${M2_REPO}/org/codehaus/jackson/jackson-core-asl/1.7.4/jackson-core-asl-1.7.4.jar:${M2_REPO}/org/codehaus/jackson/jackson-mapper-asl/1.7.4/jackson-mapper-asl-1.7.4.jar:${M2_REPO}/org/rhq/rhq-core-plugin-api/${RHQ_VERSION}/rhq-core-plugin-api-${RHQ_VERSION}.jar:${M2_REPO}/org/rhq/rhq-core-util/${RHQ_VERSION}/rhq-core-util-${RHQ_VERSION}.jar org.rhq.modules.plugins.jbossas7.Domain2Descriptor $*
