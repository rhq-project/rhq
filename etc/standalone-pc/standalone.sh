#!/bin/sh

_DOLLARZERO=`readlink "$0" || echo "$0"`
RHQ_AGENT_BIN_DIR_PATH=`dirname "$_DOLLARZERO"`
RHQ_AGENT_MAINCLASS=org.rhq.core.pc.StandaloneContainer
export RHQ_AGENT_MAINCLASS
$RHQ_AGENT_BIN_DIR_PATH/rhq-agent.sh $*