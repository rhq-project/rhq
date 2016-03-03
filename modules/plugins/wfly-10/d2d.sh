#!/bin/sh 

M2_REPO=${HOME}/.m2/repository
RHQ_VERSION='4.14.0-SNAPSHOT'
OPTS=""
#OPTS="-agentlib:jdwp=transport=dt_socket,address=8790,server=y,suspend=y"

java $OPTS -cp "target/classes/lib/*:${M2_REPO}/commons-logging/commons-logging-api/1.1/commons-logging-api-1.1.jar:target/rhq-jboss-as-7-plugin-${RHQ_VERSION}.jar:${M2_REPO}/org/rhq/rhq-core-plugin-api/${RHQ_VERSION}/rhq-core-plugin-api-${RHQ_VERSION}.jar:${M2_REPO}/org/rhq/rhq-core-util/${RHQ_VERSION}/rhq-core-util-${RHQ_VERSION}.jar" org.rhq.modules.plugins.wildfly10.Domain2Descriptor $*
