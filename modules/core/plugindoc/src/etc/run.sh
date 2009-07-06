#!/bin/sh

# ----------------------------------------------------------------------
# Prepare the classpath (take into account possible spaces in dir names)
# ----------------------------------------------------------------------

RHQ_PLUGINDOC_HOME=`pwd`

CLASSPATH="${RHQ_PLUGINDOC_HOME}/conf"
_JAR_FILES=`cd "${RHQ_PLUGINDOC_HOME}/lib";ls -1 *.jar`
for _JAR in $_JAR_FILES ; do
   _JAR="${RHQ_PLUGINDOC_HOME}/lib/${_JAR}"
   CLASSPATH="${CLASSPATH}:${_JAR}"
done

RHQ_PLUGINDOC_CMDLINE_OPTS=$*
RHQ_PLUGINDOC_MAINCLASS=org.rhq.core.tool.plugindoc.PluginDocGenerator

CMD="java -cp \"${CLASSPATH}\" ${RHQ_PLUGINDOC_MAINCLASS} ${RHQ_PLUGINDOC_CMDLINE_OPTS}"
echo $CMD > cmd.log
eval "$CMD"
