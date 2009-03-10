#!/bin/sh

# =============================================================================
# RHQ Agent UNIX Startup Script
#
# This file is used to execute the RHQ Agent on a UNIX platform.
# Run this script with the --help option for the runtime options.
# Note that this script can also be used to run the agent on a Windows
# platform if this script is run within a Cygwin environment.
#
# This script is customizable by setting certain environment variables, which
# are described in comments in rhq-agent-env.sh found in the same directory
# as this script. The variables can also be set via that rhq-agent-env.sh file,
# which is sourced by this script.
#
# If the embedded JRE is to be used but is not available, the fallback
# JRE to be used will be determined by the JAVA_HOME environment variable.
# =============================================================================

# ----------------------------------------------------------------------
# Subroutine that simply dumps a message iff debug mode is enabled
# ----------------------------------------------------------------------

debug_msg ()
{
   # if debug variable is set, it is assumed to be on, unless its value is false
   if [ "x$RHQ_AGENT_DEBUG" != "x" ]; then
      if [ "$RHQ_AGENT_DEBUG" != "false" ]; then
         echo "rhq-agent.sh: $1"
      fi
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
# Here we assume this script is a child directory of the agent home.
# We also assume our custom environment script is located in the same
# place as this script.
# ----------------------------------------------------------------------
_DOLLARZERO=`readlink "$0" || echo "$0"`
RHQ_AGENT_BIN_DIR_PATH=`dirname "$_DOLLARZERO"`

if [ -f "${RHQ_AGENT_BIN_DIR_PATH}/rhq-agent-env.sh" ]; then
   debug_msg "Loading environment script: ${RHQ_AGENT_BIN_DIR_PATH}/rhq-agent-env.sh"
   . "${RHQ_AGENT_BIN_DIR_PATH}/rhq-agent-env.sh" $*
else
   debug_msg "No environment script found at: ${RHQ_AGENT_BIN_DIR_PATH}/rhq-agent-env.sh"
fi

if [ "x$RHQ_AGENT_HOME" = "x" ]; then
   cd "${RHQ_AGENT_BIN_DIR_PATH}/.."
else
   cd "${RHQ_AGENT_HOME}" || {
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
if [ "x$JAVA_HOME" = "x" ]; then
   if [ "x$_DARWIN" != "x" ]; then
     debug_msg "Running on Mac OS X, setting JAVA_HOME to /usr"
     JAVA_HOME=/usr
   fi
fi

# ----------------------------------------------------------------------
# Find the Java executable and verify we have a VM available
# ----------------------------------------------------------------------

if [ "x$RHQ_AGENT_JAVA_EXE_FILE_PATH" = "x" ]; then
   if [ "x$RHQ_AGENT_JAVA_HOME" = "x" ]; then
      RHQ_AGENT_JAVA_HOME="${RHQ_AGENT_HOME}/jre"
      debug_msg "Using the embedded JRE"
      if [ ! -d "$RHQ_AGENT_JAVA_HOME" ]; then
         debug_msg "No embedded JRE found - will try to use JAVA_HOME: $JAVA_HOME"
         RHQ_AGENT_JAVA_HOME="$JAVA_HOME"
      fi
   fi
   debug_msg "RHQ_AGENT_JAVA_HOME: $RHQ_AGENT_JAVA_HOME"
   RHQ_AGENT_JAVA_EXE_FILE_PATH="${RHQ_AGENT_JAVA_HOME}/bin/java"
fi
debug_msg "RHQ_AGENT_JAVA_EXE_FILE_PATH: $RHQ_AGENT_JAVA_EXE_FILE_PATH"

if [ ! -f "$RHQ_AGENT_JAVA_EXE_FILE_PATH" ]; then
   echo There is no JVM available.
   echo Please set RHQ_AGENT_JAVA_HOME or RHQ_AGENT_JAVA_EXE_FILE_PATH appropriately.
   exit 1
fi

# ----------------------------------------------------------------------
# Prepare the classpath (take into account possible spaces in dir names)
# ----------------------------------------------------------------------

CLASSPATH="${RHQ_AGENT_HOME}/conf"
_JAR_FILES=`cd "${RHQ_AGENT_HOME}/lib";ls -1 *.jar`
for _JAR in $_JAR_FILES ; do
   _JAR="${RHQ_AGENT_HOME}/lib/${_JAR}"
   if [ "x$CLASSPATH" = "x" ]; then
      CLASSPATH="${_JAR}"
   else
      CLASSPATH="${CLASSPATH}:${_JAR}"
   fi
   debug_msg "CLASSPATH entry: $_JAR"
done

# ----------------------------------------------------------------------
# Prepare the VM command line options to be passed in
# ----------------------------------------------------------------------

if [ "x$RHQ_AGENT_JAVA_OPTS" = "x" ]; then
   RHQ_AGENT_JAVA_OPTS="-Xms64m -Xmx128m -Djava.net.preferIPv4Stack=true"
fi
debug_msg "RHQ_AGENT_JAVA_OPTS: $RHQ_AGENT_JAVA_OPTS"

if [ "$RHQ_AGENT_JAVA_ENDORSED_DIRS" = "none" ]; then
   debug_msg "Not explicitly setting java.endorsed.dirs"
else
   if [ "x$RHQ_AGENT_JAVA_ENDORSED_DIRS" = "x" ]; then
      RHQ_AGENT_JAVA_ENDORSED_DIRS="${RHQ_AGENT_HOME}/lib/endorsed"
   fi

   # convert the path if on Windows
   if [ "x$_CYGWIN" != "x" ]; then
      RHQ_AGENT_JAVA_ENDORSED_DIRS=`cygpath --windows --path "$RHQ_AGENT_JAVA_ENDORSED_DIRS"`
   fi
   debug_msg "RHQ_AGENT_JAVA_ENDORSED_DIRS: $RHQ_AGENT_JAVA_ENDORSED_DIRS"
   _JAVA_ENDORSED_DIRS_OPT="\"-Djava.endorsed.dirs=${RHQ_AGENT_JAVA_ENDORSED_DIRS}\""
fi

if [ "$RHQ_AGENT_JAVA_LIBRARY_PATH" = "none" ]; then
   debug_msg "Not explicitly setting java.library.path"
else
   if [ "x$RHQ_AGENT_JAVA_LIBRARY_PATH" = "x" ]; then
      RHQ_AGENT_JAVA_LIBRARY_PATH="${RHQ_AGENT_HOME}/lib"
   fi

   # convert the path if on Windows
   if [ "x$_CYGWIN" != "x" ]; then
      RHQ_AGENT_JAVA_LIBRARY_PATH=`cygpath --windows --path "$RHQ_AGENT_JAVA_LIBRARY_PATH"`
   fi
   debug_msg "RHQ_AGENT_JAVA_LIBRARY_PATH: $RHQ_AGENT_JAVA_LIBRARY_PATH"
   _JAVA_LIBRARY_PATH_OPT="\"-Djava.library.path=${RHQ_AGENT_JAVA_LIBRARY_PATH}\""
fi

debug_msg "RHQ_AGENT_ADDITIONAL_JAVA_OPTS: $RHQ_AGENT_ADDITIONAL_JAVA_OPTS"

# ----------------------------------------------------------------------
# Prepare the command line arguments passed to the RHQ Agent
# ----------------------------------------------------------------------
if [ "x$RHQ_AGENT_CMDLINE_OPTS" = "x" ]; then
   RHQ_AGENT_CMDLINE_OPTS=$*
fi
debug_msg "RHQ_AGENT_CMDLINE_OPTS: $RHQ_AGENT_CMDLINE_OPTS"

# ----------------------------------------------------------------------
# Execute the VM which starts the agent
# ----------------------------------------------------------------------

_LOG_CONFIG=-Dlog4j.configuration=log4j.xml

# if debug is enabled, the log configuration is different
if [ "x$RHQ_AGENT_DEBUG" != "x" ]; then
   if [ "$RHQ_AGENT_DEBUG" != "false" ]; then
      _LOG_CONFIG="-Dlog4j.configuration=log4j-debug.xml -Di18nlog.dump-stack-traces=true"
   fi
fi

# if sigar debug is enabled, the log configuration is different - sigar debugging is noisy, so its got its own debug var
if [ "x$RHQ_AGENT_SIGAR_DEBUG" != "x" ]; then
   if [ "$RHQ_AGENT_SIGAR_DEBUG" != "false" ]; then
      _LOG_CONFIG="$_LOG_CONFIG -Dsigar.nativeLogging=true"
   fi
fi

# create the logs directory
if [ ! -d "${RHQ_AGENT_HOME}/logs" ]; then
   mkdir "${RHQ_AGENT_HOME}/logs"
fi

# convert some of the paths if we are on Windows
if [ "x$_CYGWIN" != "x" ]; then
   CLASSPATH=`cygpath --windows --path "$CLASSPATH"`
fi

# Build the command line that starts the VM
# This is ony used if we need to emit debug message - but make sure
# this is kept in sync with the real command executed below.
CMD="\"${RHQ_AGENT_JAVA_EXE_FILE_PATH}\" ${_JAVA_ENDORSED_DIRS_OPT} ${_JAVA_LIBRARY_PATH_OPT} ${RHQ_AGENT_JAVA_OPTS} ${RHQ_AGENT_ADDITIONAL_JAVA_OPTS} ${_LOG_CONFIG} -cp \"${CLASSPATH}\" org.rhq.enterprise.agent.AgentMain ${RHQ_AGENT_CMDLINE_OPTS}"

debug_msg "Executing the agent with this command line:"
debug_msg "$CMD"

# Run the VM - put it in background if the caller wants it to be
if [ "x$RHQ_AGENT_IN_BACKGROUND" = "x" ]; then
   eval "$CMD"
else
   eval "$CMD &"
   RHQ_AGENT_BACKGROUND_PID=$!
   export RHQ_AGENT_BACKGROUND_PID
   if [ "$RHQ_AGENT_IN_BACKGROUND" != "nofile" ]; then
      echo $RHQ_AGENT_BACKGROUND_PID > "$RHQ_AGENT_IN_BACKGROUND"
   fi
fi

debug_msg "$0 done."
