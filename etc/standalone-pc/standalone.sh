#!/bin/sh

_DOLLARZERO=`readlink "$0" || echo "$0"`
RHQ_AGENT_BIN_DIR_PATH=`dirname "$_DOLLARZERO"`
RHQ_AGENT_MAINCLASS=org.rhq.core.pc.StandaloneContainer
export RHQ_AGENT_MAINCLASS

# uncomment below if you want to enable JPDA debugging
#RHQ_AGENT_ADDITIONAL_JAVA_OPTS="-Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,address=9797,server=y,suspend=n"
#export RHQ_AGENT_ADDITIONAL_JAVA_OPTS

$RHQ_AGENT_BIN_DIR_PATH/rhq-agent.sh $*
