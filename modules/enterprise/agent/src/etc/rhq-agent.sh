#!/bin/sh

# =============================================================================
# RHQ Agent UNIX Startup Script
#
# This file is used to execute the RHQ Agent on a UNIX platform.
# Run this script with the --help option for the runtime options.
# Note that this script can also be used to run the agent on a Windows
# platform if this script is run within a Cygwin environment.
#
# This script is customizable by setting the following environment variables:
#
# This script is customizable by setting certain environment variables, which
# are described in comments in rhq-agent-env.sh. The variables can also be
# set via rhq-agent-env.sh, which is sourced by this script.
#
# If the embedded JRE is to be used but is not available, the fallback
# JRE to be used will be determined by the JAVA_HOME environment variable.
# =============================================================================

if [ -f "rhq-agent-env.sh" ]; then
   echo "Loading script environment from rhq-agent-env.sh..."
   . rhq-agent-env.sh
fi

# ----------------------------------------------------------------------
# Subroutine that simply dumps a message iff debug mode is enabled
# ----------------------------------------------------------------------
debug_msg ()
{
   if [ -n "$RHQ_AGENT_DEBUG" ]; then
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
# Change directory so the current directory is the agent home.
# Here we assume this script is a child directory of the agent home
# ----------------------------------------------------------------------
RHQ_AGENT_BIN_DIR_PATH=`dirname $0`

if [ -z "$RHQ_AGENT_HOME" ]; then
   cd ${RHQ_AGENT_BIN_DIR_PATH}/..
else
   cd ${RHQ_AGENT_HOME} || {
      echo Cannot go to the RHQ_AGENT_HOME directory: ${RHQ_AGENT_HOME}
      exit 1
      }
fi

RHQ_AGENT_HOME=`pwd`

debug_msg "RHQ_AGENT_HOME: $RHQ_AGENT_HOME"

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

if [ -z "$RHQ_AGENT_JAVA_EXE_FILE_PATH" ]; then
   if [ -z "$RHQ_AGENT_JAVA_HOME" ]; then
      RHQ_AGENT_JAVA_HOME=${RHQ_AGENT_HOME}/jre
      debug_msg "Using the embedded JRE"
      if [ ! -d $RHQ_AGENT_JAVA_HOME ]; then
         debug_msg "No embedded JRE found - will try to use JAVA_HOME: $JAVA_HOME"
         RHQ_AGENT_JAVA_HOME=$JAVA_HOME
      fi
   fi
   debug_msg "RHQ_AGENT_JAVA_HOME: $RHQ_AGENT_JAVA_HOME"
   RHQ_AGENT_JAVA_EXE_FILE_PATH=${RHQ_AGENT_JAVA_HOME}/bin/java
fi
debug_msg "RHQ_AGENT_JAVA_EXE_FILE_PATH: $RHQ_AGENT_JAVA_EXE_FILE_PATH"

if [ ! -f "$RHQ_AGENT_JAVA_EXE_FILE_PATH" ]; then
   echo There is no JVM available.
   echo Please set RHQ_AGENT_JAVA_HOME or RHQ_AGENT_JAVA_EXE_FILE_PATH appropriately.
   exit 1
fi

# ----------------------------------------------------------------------
# Prepare the classpath
# ----------------------------------------------------------------------

CLASSPATH=${RHQ_AGENT_HOME}/conf
_JAR_FILES=`ls -1 ${RHQ_AGENT_HOME}/lib/*.jar`
for _JAR in $_JAR_FILES ; do
   if [ -z "$CLASSPATH" ]; then
      CLASSPATH="${_JAR}"
   else
      CLASSPATH="${CLASSPATH}:${_JAR}"
   fi
   debug_msg "CLASSPATH entry: $_JAR"
done

# ----------------------------------------------------------------------
# Prepare the VM command line options to be passed in
# ----------------------------------------------------------------------

if [ -z "$RHQ_AGENT_JAVA_OPTS" ]; then
   RHQ_AGENT_JAVA_OPTS="-Xms64m -Xmx128m -Djava.net.preferIPv4Stack=true"
fi

RHQ_AGENT_JAVA_OPTS="-Djava.endorsed.dirs=${RHQ_AGENT_HOME}/lib/endorsed ${RHQ_AGENT_JAVA_OPTS}"

# The RHQ Agent has a JNI library that it needs to find in order to
# do things like execute PIQL queries and access low-level operating
# system data. Here we add the java.library.path system property
# to point to the JNI libraries.  If you deploy a custom plugin
# that also requires JNI libraries, you must add to the library path
# here, you must not remove the RHQ Agent library path that it needs.

_JNI_PATH="${RHQ_AGENT_HOME}/lib"

# convert the path if on Windows
if [ -n "$_CYGWIN" ]; then
   _JNI_PATH=`cygpath --windows --path "$_JNI_PATH"`
fi

RHQ_AGENT_JAVA_OPTS="-Djava.library.path=${_JNI_PATH} ${RHQ_AGENT_JAVA_OPTS}"

debug_msg "RHQ_AGENT_JAVA_OPTS: $RHQ_AGENT_JAVA_OPTS"
debug_msg "RHQ_AGENT_ADDITIONAL_JAVA_OPTS: $RHQ_AGENT_ADDITIONAL_JAVA_OPTS"

# ----------------------------------------------------------------------
# Prepare the command line arguments passed to the RHQ Agent
# ----------------------------------------------------------------------
if [ -z "$RHQ_AGENT_CMDLINE_OPTS" ]; then
   RHQ_AGENT_CMDLINE_OPTS=$*
fi
debug_msg "RHQ_AGENT_CMDLINE_OPTS: $RHQ_AGENT_CMDLINE_OPTS"

# ----------------------------------------------------------------------
# Execute the VM which starts the agent
# ----------------------------------------------------------------------

if [ -n "$RHQ_AGENT_DEBUG" ]; then
   _LOG_CONFIG=-Dlog4j.configuration="log4j-debug.xml -Dsigar.nativeLogging=true -Di18nlog.dump-stack-traces=true"
else
   _LOG_CONFIG=-Dlog4j.configuration="log4j.xml"
fi

# log4j 1.2.8 does not create the directory for us (later versions do)
if [ ! -d "${RHQ_AGENT_HOME}/logs" ]; then
   mkdir ${RHQ_AGENT_HOME}/logs
fi

# convert some of the paths if we are on Windows
if [ -n "$_CYGWIN" ]; then
   CLASSPATH=`cygpath --windows --path "$CLASSPATH"`
fi

# Build the command line that starts the VM
CMD="${RHQ_AGENT_JAVA_EXE_FILE_PATH} ${RHQ_AGENT_JAVA_OPTS} ${RHQ_AGENT_ADDITIONAL_JAVA_OPTS} ${_LOG_CONFIG} -cp ${CLASSPATH} org.rhq.enterprise.agent.AgentMain ${RHQ_AGENT_CMDLINE_OPTS}"

debug_msg "Executing the agent with this command line:"
debug_msg "$CMD"

# Run the VM - put it in background if the caller wants it to be
if [ -z "$RHQ_AGENT_IN_BACKGROUND" ]; then
   $CMD
else
   $CMD &
   RHQ_AGENT_BACKGROUND_PID=$!
   export RHQ_AGENT_BACKGROUND_PID
   if [ "$RHQ_AGENT_IN_BACKGROUND" != "nofile" ]; then
      echo $RHQ_AGENT_BACKGROUND_PID > $RHQ_AGENT_IN_BACKGROUND
   fi
fi

debug_msg echo $0 done.
