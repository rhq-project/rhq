#!/bin/sh

# ----------------------------------------------------------------------
# Determine what specific platform we are running on.
# Set some platform-specific variables.
# ----------------------------------------------------------------------
case "`uname`" in
   CYGWIN*) _CYGWIN=true
            ;;
esac

# ----------------------------------------------------------------------
# Prepare the classpath (take into account possible spaces in dir names)
# ----------------------------------------------------------------------

RHQ_PLUGINDOC_BIN_DIR=`dirname $0`
cd ${RHQ_PLUGINDOC_BIN_DIR}/..
RHQ_PLUGINDOC_HOME=`pwd`

CLASSPATH="${RHQ_PLUGINDOC_HOME}/conf"
_JAR_FILES=`cd "${RHQ_PLUGINDOC_HOME}/lib";ls -1 *.jar`
for _JAR in $_JAR_FILES ; do
   _JAR="${RHQ_PLUGINDOC_HOME}/lib/${_JAR}"
   CLASSPATH="${CLASSPATH}:${_JAR}"
done

RHQ_PLUGINDOC_CMDLINE_OPTS=$*
RHQ_PLUGINDOC_MAINCLASS=org.rhq.core.tool.plugindoc.PluginDocGenerator

# convert some of the paths if we are on Windows
if [ "x$_CYGWIN" != "x" ]; then
   CLASSPATH=`cygpath --windows --path "$CLASSPATH"`
fi

CMD="java -cp \"${CLASSPATH}\" ${RHQ_PLUGINDOC_MAINCLASS} ${RHQ_PLUGINDOC_CMDLINE_OPTS}"
echo $CMD > cmd.log
eval "$CMD"
