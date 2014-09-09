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
   if [ -n "$RHQ_AGENT_DEBUG" ] && [ "$RHQ_AGENT_DEBUG" != "false" ]; then
      echo "rhq-agent.sh: $1"
   fi
}

# ----------------------------------------------------------------------
# Try to determine the fallback JAVA_HOME if not already set
# ----------------------------------------------------------------------

set_java_home ()
{
   if [ -z "$JAVA_HOME" ]; then
      _WHICH_JAVA=`which java 2>/dev/null`
      if [ -n "$_WHICH_JAVA" ]; then
         _WHICH_JAVA_BIN_DIR=`dirname "$_WHICH_JAVA"`
         JAVA_HOME=`dirname "$_WHICH_JAVA_BIN_DIR"`
         debug_msg "JAVA_HOME determined by which: ${JAVA_HOME}"
      elif [ -n "$_DARWIN" ]; then
         JAVA_HOME=/usr
         debug_msg "Running on Mac OS X, setting JAVA_HOME to ${JAVA_HOME}"
      elif [ -n "$_LINUX" ]; then
         JAVA_HOME=/usr/lib/jvm/jre
         debug_msg "Running on Linux, setting JAVA_HOME to ${JAVA_HOME}"
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
   Linux*)  _LINUX=true
            ;;
   Darwin*) _DARWIN=true
            ;;
   SunOS*) _SOLARIS=true
            ;;
   AIX*)   _AIX=true
            ;;
esac

case "`uname -m`" in
   x86_64) _X86_64=true
           ;;
esac

# ----------------------------------------------------------------------
# Change directory so the current directory is the agent home.
# Here we assume this script is a child directory of the agent home.
# We also assume our custom environment script is located in the same
# place as this script.
# ----------------------------------------------------------------------

type readlink >/dev/null 2>&1
if [ $? -ne 0 ]; then
    echo >&2 'WARNING: The readlink command is not available on this platform.'
    echo >&2 '         If this script was launched from a symbolic link, it may '
    echo >&2 '         fail to properly resolve its home directory.'
fi

if [ -n "${_LINUX}${_SOLARIS}${_CYGWIN}" ]; then
   # only certain platforms support the -e argument for readlink
   _READLINK_ARG="-e"
fi

_DOLLARZERO=`readlink $_READLINK_ARG "$0" 2>/dev/null || echo "$0"`
RHQ_AGENT_BIN_DIR_PATH=`dirname "$_DOLLARZERO"`

if [ -f "${RHQ_AGENT_BIN_DIR_PATH}/rhq-agent-env.sh" ]; then
   debug_msg "Loading environment script: ${RHQ_AGENT_BIN_DIR_PATH}/rhq-agent-env.sh"
   . "${RHQ_AGENT_BIN_DIR_PATH}/rhq-agent-env.sh" $*
else
   debug_msg "No environment script found at: ${RHQ_AGENT_BIN_DIR_PATH}/rhq-agent-env.sh"
fi

if [ -z "$RHQ_AGENT_HOME" ]; then
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
# Find the Java executable and verify we have a VM available
# ----------------------------------------------------------------------
if [ -z "$RHQ_JAVA_EXE_FILE_PATH" ]; then
   if [ ! -z "$RHQ_AGENT_JAVA_EXE_FILE_PATH" ]; then
      RHQ_JAVA_EXE_FILE_PATH="$RHQ_AGENT_JAVA_EXE_FILE_PATH"
   fi
fi
if [ -z "$RHQ_JAVA_HOME" ]; then
   if [ ! -z "$RHQ_AGENT_JAVA_HOME" ]; then
      RHQ_JAVA_HOME="$RHQ_AGENT_JAVA_HOME"
   fi
fi

if [ -z "$RHQ_JAVA_EXE_FILE_PATH" ]; then
   if [ -z "$RHQ_JAVA_HOME" ]; then
      set_java_home
      debug_msg "No RHQ JAVA property set, defaulting to JAVA_HOME: $JAVA_HOME"
      RHQ_JAVA_HOME="$JAVA_HOME"
   fi
   debug_msg "RHQ_JAVA_HOME: $RHQ_JAVA_HOME"
   RHQ_JAVA_EXE_FILE_PATH="${RHQ_JAVA_HOME}/bin/java"
else
   # Infer RHQ_JAVA_HOME from RHQ_JAVA_EXE_FILE_PATH
   # RHQ_JAVA_HOME will be used later to find JDK's tools.jar if available
   _RHQ_JAVA_BIN_DIR=`dirname "$RHQ_JAVA_EXE_FILE_PATH"`
   RHQ_JAVA_HOME=`dirname "$_RHQ_JAVA_BIN_DIR"`
   debug_msg "RHQ_JAVA_HOME (inferred from RHQ_JAVA_EXE_FILE_PATH): $RHQ_JAVA_HOME"
fi
debug_msg "RHQ_JAVA_EXE_FILE_PATH: $RHQ_JAVA_EXE_FILE_PATH"

if [ ! -f "$RHQ_JAVA_EXE_FILE_PATH" ]; then
   echo There is no JVM available.
   echo Please set RHQ_JAVA_HOME or RHQ_JAVA_EXE_FILE_PATH appropriately.
   exit 1
fi

# ----------------------------------------------------------------------
# Prepare the classpath (take into account possible spaces in dir names)
# ----------------------------------------------------------------------

CLASSPATH="${RHQ_AGENT_HOME}/conf"
if [ ! -d "${RHQ_AGENT_HOME}/lib" ]; then
   echo "lib subdirectory does not exist under RHQ_AGENT_HOME directory: ${RHQ_AGENT_HOME}"
   exit 1
fi
_JAR_FILES=`cd "${RHQ_AGENT_HOME}/lib";ls -1 *.jar`
for _JAR in $_JAR_FILES ; do
   _JAR="${RHQ_AGENT_HOME}/lib/${_JAR}"
   if [ -z "$CLASSPATH" ]; then
      CLASSPATH="${_JAR}"
   else
      CLASSPATH="${CLASSPATH}:${_JAR}"
   fi
   debug_msg "CLASSPATH entry: $_JAR"
done
for _TOOLS_JAR in "${RHQ_JAVA_HOME}/lib/tools.jar" "${RHQ_JAVA_HOME}/../lib/tools.jar" "${RHQ_JAVA_HOME}/Classes/classes.jar" "${RHQ_JAVA_HOME}/../Classes/classes.jar"; do
   if [ -f "${_TOOLS_JAR}" ]; then
      debug_msg "CLASSPATH entry: ${_TOOLS_JAR}"
      CLASSPATH="${CLASSPATH}:${_TOOLS_JAR}"
      break
   fi
done

# ----------------------------------------------------------------------
# Prepare the VM command line options to be passed in
# ----------------------------------------------------------------------

if [ -z "$RHQ_AGENT_JAVA_OPTS" ]; then
   RHQ_AGENT_JAVA_OPTS='-Xms64m -Xmx128m -Djava.net.preferIPv4Stack=true "-Drhq.preferences.file=${RHQ_AGENT_HOME}/conf/agent-prefs.properties"'
fi
debug_msg "RHQ_AGENT_JAVA_OPTS: $RHQ_AGENT_JAVA_OPTS"

if [ "$RHQ_AGENT_JAVA_ENDORSED_DIRS" = "none" ]; then
   debug_msg "Not explicitly setting java.endorsed.dirs"
else
   if [ -z "$RHQ_AGENT_JAVA_ENDORSED_DIRS" ]; then
      RHQ_AGENT_JAVA_ENDORSED_DIRS="${RHQ_AGENT_HOME}/lib/endorsed"
   fi

   # convert the path if on Windows
   if [ -n "$_CYGWIN" ]; then
      RHQ_AGENT_JAVA_ENDORSED_DIRS=`cygpath --windows --path "$RHQ_AGENT_JAVA_ENDORSED_DIRS"`
   fi
   debug_msg "RHQ_AGENT_JAVA_ENDORSED_DIRS: $RHQ_AGENT_JAVA_ENDORSED_DIRS"
   _JAVA_ENDORSED_DIRS_OPT="\"-Djava.endorsed.dirs=${RHQ_AGENT_JAVA_ENDORSED_DIRS}\""
fi

if [ "$RHQ_AGENT_JAVA_LIBRARY_PATH" = "none" ]; then
   debug_msg "Not explicitly setting java.library.path"
else
   if [ -z "$RHQ_AGENT_JAVA_LIBRARY_PATH" ]; then
      RHQ_AGENT_JAVA_LIBRARY_PATH="${RHQ_AGENT_HOME}/lib"
   fi

   # convert the path if on Windows
   if [ -n "$_CYGWIN" ]; then
      RHQ_AGENT_JAVA_LIBRARY_PATH=`cygpath --windows --path "$RHQ_AGENT_JAVA_LIBRARY_PATH"`
   fi
   debug_msg "RHQ_AGENT_JAVA_LIBRARY_PATH: $RHQ_AGENT_JAVA_LIBRARY_PATH"
   _JAVA_LIBRARY_PATH_OPT="\"-Djava.library.path=${RHQ_AGENT_JAVA_LIBRARY_PATH}\""
fi

debug_msg "RHQ_AGENT_ADDITIONAL_JAVA_OPTS: $RHQ_AGENT_ADDITIONAL_JAVA_OPTS"

# ----------------------------------------------------------------------
# Ensure the agent uses our custom JavaPreferences implementation
# ----------------------------------------------------------------------
_JAVA_PREFERENCES_FACTORY_OPT="\"-Djava.util.prefs.PreferencesFactory=org.rhq.core.util.preferences.FilePreferencesFactory\""
# add umask so user preferences are not world readable
if [ -z "${RHQ_AGENT_UMASK}" ]; then
   RHQ_AGENT_UMASK=007
fi

umask ${RHQ_AGENT_UMASK} >/dev/null
if [ $? -ne 0 ]; then
   echo >&2 "RHQ_AGENT_UMASK contains an invalid umask value of [${RHQ_AGENT_UMASK}]"
fi

# ----------------------------------------------------------------------
# Prepare the command line arguments passed to the RHQ Agent
# ----------------------------------------------------------------------
if [ -z "$RHQ_AGENT_CMDLINE_OPTS" ]; then
   RHQ_AGENT_CMDLINE_OPTS=$*
fi
debug_msg "RHQ_AGENT_CMDLINE_OPTS: $RHQ_AGENT_CMDLINE_OPTS"

# ----------------------------------------------------------------------
# Prepare LD_LIBRARY_PATH to include libraries shipped with the agent and
# prepare jna.platform.library.path for JNA to be able to load augeas from our
# custom location.
# ----------------------------------------------------------------------

if [ -n "$_LINUX" ]; then
   if [ -z "$LD_LIBRARY_PATH" ]; then
      if [ -n "$_X86_64" ]; then
         LD_LIBRARY_PATH="${RHQ_AGENT_HOME}/lib/augeas/lib64"
      else
         LD_LIBRARY_PATH="${RHQ_AGENT_HOME}/lib/augeas/lib"
      fi
   else
      if [ -n "$_X86_64" ]; then
         LD_LIBRARY_PATH="${RHQ_AGENT_HOME}/lib/augeas/lib64:${LD_LIBRARY_PATH}"
      else
         LD_LIBRARY_PATH="${RHQ_AGENT_HOME}/lib/augeas/lib:${LD_LIBRARY_PATH}"
      fi
   fi
   export LD_LIBRARY_PATH

   # We need to force our custom library path as the "system" look up path to
   # JNA. Without this, the lookup of .so.x.y versions wouldn't work.
   # We also need to keep the LD_LIBRARY_PATH in place so that the default
   # system lookup works for libfa, which libaugeas depends on.
   _JNA_LIBRARY_PATH="\"-Djna.platform.library.path=${LD_LIBRARY_PATH}\""

   debug_msg "LD_LIBRARY_PATH: $LD_LIBRARY_PATH"
fi

# ----------------------------------------------------------------------
# Execute the VM which starts the agent
# ----------------------------------------------------------------------

_LOG_CONFIG=-Dlog4j.configuration=log4j.xml

# if debug is enabled, the log configuration is different
if [ -n "$RHQ_AGENT_DEBUG" ] && [ "$RHQ_AGENT_DEBUG" != "false" ]; then
   _LOG_CONFIG="-Dlog4j.configuration=log4j-debug.xml -Di18nlog.dump-stack-traces=true"
fi

# if sigar debug is enabled, the log configuration is different - sigar debugging is noisy, so it has its own debug var
if [ -n "$RHQ_AGENT_SIGAR_DEBUG" ] && [ "$RHQ_AGENT_SIGAR_DEBUG" != "false" ]; then
   _LOG_CONFIG="$_LOG_CONFIG -Dsigar.nativeLogging=true"
fi

# create the logs directory
if [ ! -d "${RHQ_AGENT_HOME}/logs" ]; then
   mkdir "${RHQ_AGENT_HOME}/logs"
fi

# convert some of the paths if we are on Windows
if [ -n "$_CYGWIN" ]; then
   CLASSPATH=`cygpath --windows --path "$CLASSPATH"`
fi

# to support other agents/plugin containers, allow the caller to override the main classname
if [ -z "$RHQ_AGENT_MAINCLASS" ]; then
   RHQ_AGENT_MAINCLASS="org.rhq.enterprise.agent.AgentMain"
fi

# Build the command line that starts the VM
# note - currently not using custom Java Prefs as the default, use commented command line to activate
# CMD="\"${RHQ_JAVA_EXE_FILE_PATH}\" ${_JAVA_ENDORSED_DIRS_OPT} ${_JAVA_LIBRARY_PATH_OPT} ${_JAVA_PREFERENCES_FACTORY_OPT} ${_JNA_LIBRARY_PATH} ${RHQ_AGENT_JAVA_OPTS} ${RHQ_AGENT_ADDITIONAL_JAVA_OPTS} ${_LOG_CONFIG} -cp \"${CLASSPATH}\" ${RHQ_AGENT_MAINCLASS} ${RHQ_AGENT_CMDLINE_OPTS}"
CMD="\"${RHQ_JAVA_EXE_FILE_PATH}\" ${_JAVA_ENDORSED_DIRS_OPT} ${_JAVA_LIBRARY_PATH_OPT} ${_JNA_LIBRARY_PATH} ${RHQ_AGENT_JAVA_OPTS} ${RHQ_AGENT_ADDITIONAL_JAVA_OPTS} ${_LOG_CONFIG} -cp \"${CLASSPATH}\" ${RHQ_AGENT_MAINCLASS} ${RHQ_AGENT_CMDLINE_OPTS}"

debug_msg "Executing the agent with this command line:"
debug_msg "$CMD"

# Run the VM - put it in background if the caller wants it to be
if [ -z "$RHQ_AGENT_IN_BACKGROUND" ]; then
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
