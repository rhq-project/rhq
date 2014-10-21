#!/bin/sh

command -v readlink >/dev/null 2>&1
if [ $? -ne 0 ]; then
    echo >&2 'WARNING: The readlink command is not available on this platform.'
    echo >&2 '         If this script was launched from a symbolic link, errors may occur.'
    echo >&2 '         Consider installing readlink on this platform.'
    _DOLLARZERO="$0"
else
    # only certain platforms support the -e argument for readlink
     if [ -n "${_LINUX}${_SOLARIS}${_CYGWIN}" ]; then
       _READLINK_ARG="-e"
       _DOLLARZERO=`readlink $_READLINK_ARG "$0" 2>/dev/null || echo "$0"`
    elif  [ -n "${_DARWIN}" ]; then
       _DOLLARZERO=$(cd `dirname "${BASH_SOURCE[0]}"` && pwd)/`basename "${BASH_SOURCE[0]}"`
    else
       _DOLLARZERO=`readlink "$0" 2>/dev/null || echo "$0"`
    fi
fi

RHQ_AGENT_BIN_DIR_PATH=`dirname "$_DOLLARZERO"`
RHQ_AGENT_MAINCLASS=org.rhq.core.pc.StandaloneContainer
export RHQ_AGENT_MAINCLASS

# uncomment below if you want to enable JPDA debugging
#RHQ_AGENT_ADDITIONAL_JAVA_OPTS="-Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,address=9797,server=y,suspend=n"
#export RHQ_AGENT_ADDITIONAL_JAVA_OPTS

$RHQ_AGENT_BIN_DIR_PATH/rhq-agent.sh $*
