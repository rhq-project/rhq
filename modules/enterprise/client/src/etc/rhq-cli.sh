#!/bin/sh

# =============================================================================
# RHQ CLI client UNIX Startup Script
#
# This file is used to execute the RHQ Agent on a Windows platform.
# Run this script with the --help option for the runtime options.
#
# This script is customizable by setting certain environment variables, which
# are described in comments in rhq-client-env.sh. The variables can also be
# set via rhq-client-env.sh, which is sourced by this script.
# =============================================================================

# ----------------------------------------------------------------------
# Subroutine that simply dumps a message if debug mode is enabled
# ----------------------------------------------------------------------
debug_msg ()
{
   if [ -n "$RHQ_CLI_DEBUG" ]; then
      echo $1
   fi
}

# ----------------------------------------------------------------------
# Determine what specific platform we are running on.
# Set some platform-specific variables.
# ----------------------------------------------------------------------
case "`uname`" in
   CYGWIN*) _CYGWIN=true
            ;;
   Darwin*) _DARWIN=true
            ;;
esac

# ----------------------------------------------------------------------
# Change directory so the current directory is the CLI home.
# Here we assume this script is a child directory of the agent home
# ----------------------------------------------------------------------
RHQ_CLI_BIN_DIR_PATH=`dirname $0`

if [ -z "$RHQ_CLI_HOME" ]; then
   cd ${RHQ_CLI_BIN_DIR_PATH}/..
else
   cd ${RHQ_CLI_HOME} || {
      echo Cannot go to the RHQ_CLI_HOME directory: ${RHQ_CLI_HOME}
      exit 1
      }
fi

RHQ_CLI_HOME=`pwd`

debug_msg "RHQ_CLI_HOME: $RHQ_CLI_HOME"

# ----------------------------------------------------------------------
# If we are on a Mac and JAVA_HOME is not set, then set it to /usr
# as this is the default location.
# ----------------------------------------------------------------------
if [ -z "$JAVA_HOME" ]; then
   if [ -n "$_DARWIN" ]; then
     debug_msg "Running on Mac OS X, setting JAVA_HOME to /usr"
     JAVA_HOME=/usr
   fi
fi

# ----------------------------------------------------------------------
# Find the Java executable and verify we have a VM available
# ----------------------------------------------------------------------

if [ -z "$RHQ_CLI_JAVA_EXE_FILE_PATH" ]; then
   if [ -z "$RHQ_CLI_JAVA_HOME" ]; then
      RHQ_CLI_JAVA_HOME=${RHQ_CLI_HOME}/jre
      debug_msg "Using the embedded JRE"
      if [ ! -d $RHQ_CLI_JAVA_HOME ]; then
         debug_msg "No embedded JRE found - will try to use JAVA_HOME: $JAVA_HOME"
         RHQ_CLI_JAVA_HOME=$JAVA_HOME
      fi
   fi
   debug_msg "RHQ_CLI_JAVA_HOME: $RHQ_CLI_JAVA_HOME"
   RHQ_CLI_JAVA_EXE_FILE_PATH=${RHQ_CLI_JAVA_HOME}/bin/java
fi
debug_msg "RHQ_CLI_JAVA_EXE_FILE_PATH: $RHQ_CLI_JAVA_EXE_FILE_PATH"

if [ ! -f "$RHQ_CLI_JAVA_EXE_FILE_PATH" ]; then
   echo There is no JVM available.
   echo Please set RHQ_CLI_JAVA_HOME or RHQ_CLI_JAVA_EXE_FILE_PATH appropriately.
   exit 1
fi

# ----------------------------------------------------------------------
# Prepare the classpath
# ----------------------------------------------------------------------

CLASSPATH=${RHQ_CLI_HOME}/conf
_JAR_FILES=`ls -1 ${RHQ_CLI_HOME}/lib/*.jar`
for _JAR in $_JAR_FILES ; do
   if [ -z "$CLASSPATH" ]; then
      CLASSPATH="${_JAR}"
   else
      CLASSPATH="${CLASSPATH}:${_JAR}"
   fi
   debug_msg "CLASSPATH entry: $_JAR"
done
_JAR="${RHQ_CLI_HOME}/jbossws-native-dist/deploy/lib/jbossws-client.jar"
CLASSPATH="${CLASSPATH}:${_JAR}"
debug_msg "CLASSPATH entry: $_JAR"

# ----------------------------------------------------------------------
# Prepare the VM command line options to be passed in
# ----------------------------------------------------------------------

if [ -z "$RHQ_CLI_JAVA_OPTS" ]; then
   RHQ_CLI_JAVA_OPTS="-Xms64m -Xmx128m -Djava.net.preferIPv4Stack=true"
fi

if [ "$RHQ_CLI_JAVA_ENDORSED_DIRS" = "none" ]; then
   debug_msg "Not explicitly setting java.endorsed.dirs"
else
   if [ "x$RHQ_CLI_JAVA_ENDORSED_DIRS" = "x" ]; then
      RHQ_CLI_JAVA_ENDORSED_DIRS="${RHQ_CLI_HOME}/lib/endorsed"
   fi

   # convert the path if on Windows
   if [ "x$_CYGWIN" != "x" ]; then
      RHQ_CLI_JAVA_ENDORSED_DIRS=`cygpath --windows --path "$RHQ_CLI_JAVA_ENDORSED_DIRS"`
   fi
   debug_msg "RHQ_CLI_JAVA_ENDORSED_DIRS: $RHQ_CLI_JAVA_ENDORSED_DIRS"
   _JAVA_ENDORSED_DIRS_OPT="\"-Djava.endorsed.dirs=${RHQ_CLI_JAVA_ENDORSED_DIRS}\""
fi

RHQ_CLI_JAVA_OPTS="${_JAVA_ENDORSED_DIRS_OPT} ${RHQ_CLI_JAVA_OPTS} -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=9787"

debug_msg "RHQ_CLI_JAVA_OPTS: $RHQ_CLI_JAVA_OPTS"
debug_msg "RHQ_CLI_ADDITIONAL_JAVA_OPTS: $RHQ_CLI_ADDITIONAL_JAVA_OPTS"

# ----------------------------------------------------------------------
# Prepare the command line arguments passed to the RHQ Agent
# ----------------------------------------------------------------------
if [ -z "$RHQ_CLI_CMDLINE_OPTS" ]; then
   RHQ_CLI_CMDLINE_OPTS=$*
fi
debug_msg "RHQ_CLI_CMDLINE_OPTS: $RHQ_CLI_CMDLINE_OPTS"

# ----------------------------------------------------------------------
# Execute the VM which starts the agent
# ----------------------------------------------------------------------

if [ -n "$RHQ_CLI_DEBUG" ]; then
   _LOG_CONFIG=-Dlog4j.configuration="log4j-debug.xml -Di18nlog.dump-stack-traces=true"
else
   _LOG_CONFIG=-Dlog4j.configuration="log4j.xml"
fi

# log4j 1.2.8 does not create the directory for us (later versions do)
if [ ! -d "${RHQ_CLI_HOME}/logs" ]; then
   mkdir ${RHQ_CLI_HOME}/logs
fi

# convert some of the paths if we are on Windows
if [ -n "$_CYGWIN" ]; then
   CLASSPATH=`cygpath --windows --path "$CLASSPATH"`
fi

# Build the command line that starts the VM
CMD="${RHQ_CLI_JAVA_EXE_FILE_PATH} ${RHQ_CLI_JAVA_OPTS} ${RHQ_CLI_ADDITIONAL_JAVA_OPTS} ${_LOG_CONFIG} -cp ${CLASSPATH} org.rhq.enterprise.client.ClientMain ${RHQ_CLI_CMDLINE_OPTS}"

debug_msg "Executing the agent with this command line:"
debug_msg "$CMD"

debug_msg echo $0 done.
