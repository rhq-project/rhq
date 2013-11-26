#!/bin/sh

# =============================================================================
# RHQ CLI client UNIX Startup Script
#
# This file is used to execute the RHQ CLI on a UNIX platform.
# Run this script with the --help option for the runtime options.
#
# This script is customizable by setting certain environment variables, which
# are described in comments in rhq-client-env.sh. The variables can also be
# set via rhq-client-env.sh, which is sourced by this script.
# =============================================================================

# ----------------------------------------------------------------------
# Subroutine that simply dumps a message iff debug mode is enabled
# ----------------------------------------------------------------------
debug_msg ()
{
   # if debug variable is set, it is assumed to be on, unless its value is false
   if [ -n "$RHQ_CLI_DEBUG" ] && [ "$RHQ_CLI_DEBUG" != "false" ]; then
      echo "rhq-cli.sh: $1"
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
# Here we assume this script is a child directory of the CLI home
# We also assume our custom environment script is located in the same
# place as this script.
# ----------------------------------------------------------------------
_DOLLARZERO=`readlink "$0" 2>/dev/null || echo "$0"`
RHQ_CLI_BIN_DIR_PATH=`dirname "$_DOLLARZERO"`

if [ -f "${RHQ_CLI_BIN_DIR_PATH}/rhq-cli-env.sh" ]; then
   debug_msg "Loading environment script: ${RHQ_CLI_BIN_DIR_PATH}/rhq-cli-env.sh"
   . "${RHQ_CLI_BIN_DIR_PATH}/rhq-cli-env.sh" $*
else
   debug_msg "No environment script found at: ${RHQ_CLI_BIN_DIR_PATH}/rhq-cli-env.sh"
fi

if [ -z "$RHQ_CLI_HOME" ]; then
   cd "${RHQ_CLI_BIN_DIR_PATH}/.."
else
   cd "${RHQ_CLI_HOME}" || {
      echo "Cannot go to the RHQ_CLI_HOME directory: ${RHQ_CLI_HOME}"
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
      RHQ_CLI_JAVA_HOME="${RHQ_CLI_HOME}/jre"
      if [ -d "$RHQ_CLI_JAVA_HOME" ]; then
         debug_msg "Using the embedded JRE"
      else
         debug_msg "No embedded JRE found - will try to use JAVA_HOME: $JAVA_HOME"
         RHQ_CLI_JAVA_HOME="$JAVA_HOME"
      fi
   fi
   debug_msg "RHQ_CLI_JAVA_HOME: $RHQ_CLI_JAVA_HOME"
   RHQ_CLI_JAVA_EXE_FILE_PATH=${RHQ_CLI_JAVA_HOME}/bin/java
fi
debug_msg "RHQ_CLI_JAVA_EXE_FILE_PATH: $RHQ_CLI_JAVA_EXE_FILE_PATH"

if [ ! -f "$RHQ_CLI_JAVA_EXE_FILE_PATH" ]; then
   echo "There is no JVM available."
   echo "Please set RHQ_CLI_JAVA_HOME or RHQ_CLI_JAVA_EXE_FILE_PATH appropriately."
   exit 1
fi

# ----------------------------------------------------------------------
# Prepare the classpath (take into account possible spaces in dir names)
# ----------------------------------------------------------------------

CLASSPATH="${RHQ_CLI_HOME}/conf"
_JAR_FILES=`cd "${RHQ_CLI_HOME}/lib";ls -1 *.jar`
for _JAR in $_JAR_FILES ; do
   _JAR="${RHQ_CLI_HOME}/lib/${_JAR}"
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
debug_msg "RHQ_CLI_JAVA_OPTS: $RHQ_CLI_JAVA_OPTS"

if [ "$RHQ_CLI_JAVA_ENDORSED_DIRS" = "none" ]; then
   debug_msg "Not explicitly setting java.endorsed.dirs"
else
   if [ -z "$RHQ_CLI_JAVA_ENDORSED_DIRS" ]; then
      RHQ_CLI_JAVA_ENDORSED_DIRS="${RHQ_CLI_HOME}/lib/endorsed"
   fi

   # convert the path if on Windows
   if [ -n "$_CYGWIN" ]; then
      RHQ_CLI_JAVA_ENDORSED_DIRS=`cygpath --windows --path "$RHQ_CLI_JAVA_ENDORSED_DIRS"`
   fi
   debug_msg "RHQ_CLI_JAVA_ENDORSED_DIRS: $RHQ_CLI_JAVA_ENDORSED_DIRS"
   _JAVA_ENDORSED_DIRS_OPT="-Djava.endorsed.dirs=\"${RHQ_CLI_JAVA_ENDORSED_DIRS}\""
fi

if [ "$RHQ_CLI_JAVA_LIBRARY_PATH" = "none" ]; then
   debug_msg "Not explicitly setting java.library.path"
else
   if [ -z "$RHQ_CLI_JAVA_LIBRARY_PATH" ]; then
      RHQ_CLI_JAVA_LIBRARY_PATH="${RHQ_CLI_HOME}/lib"
   fi

   # convert the path if on Windows
   if [ -n "$_CYGWIN" ]; then
      RHQ_CLI_JAVA_LIBRARY_PATH=`cygpath --windows --path "$RHQ_CLI_JAVA_LIBRARY_PATH"`
   fi
   debug_msg "RHQ_CLI_JAVA_LIBRARY_PATH: $RHQ_CLI_JAVA_LIBRARY_PATH"
   _JAVA_LIBRARY_PATH_OPT="-Djava.library.path=\"${RHQ_CLI_JAVA_LIBRARY_PATH}\""
fi

debug_msg "RHQ_CLI_ADDITIONAL_JAVA_OPTS: $RHQ_CLI_ADDITIONAL_JAVA_OPTS"

# ----------------------------------------------------------------------
# Execute the VM which starts the CLI
# ----------------------------------------------------------------------

_LOG_CONFIG=-Dlog4j.configuration=log4j.xml

# if debug is enabled, the log configuration is different
if [ -n "$RHQ_CLI_DEBUG" ]; then
   if [ "$RHQ_CLI_DEBUG" != "false" ]; then
      _LOG_CONFIG="-Dlog4j.configuration=log4j-debug.xml"
   fi
fi

# create the logs directory
if [ ! -d "${RHQ_CLI_HOME}/logs" ]; then
   mkdir "${RHQ_CLI_HOME}/logs"
fi

# convert some of the paths if we are on Windows
if [ -n "$_CYGWIN" ]; then
   CLASSPATH=`cygpath --windows --path "$CLASSPATH"`
fi

# Build the command line that starts the VM
# We're using an if on existence of externally passed RHQ_CLI_CMDLINE_OPTS
# variable rather than assigning RHQ_CLI_CMDLINE_OPTS to "$@" if nothing passed
# because doing that would expand the quoted variables at the assignment time.
# If we then passed that variable to the actual command, the quoted arguments
# would be already expanded and we'd therefore loose the ability to pass in
# quoted args.
debug_msg "Executing the CLI with this command line:"
exit_code=0
if [ -z "$RHQ_CLI_CMDLINE_OPTS" ]; then
    debug_msg "${RHQ_CLI_JAVA_EXE_FILE_PATH} ${_JAVA_ENDORSED_DIRS_OPT} ${_JAVA_LIBRARY_PATH_OPT} ${RHQ_CLI_JAVA_OPTS} ${RHQ_CLI_ADDITIONAL_JAVA_OPTS} ${_LOG_CONFIG} -cp ${CLASSPATH} org.rhq.enterprise.client.ClientMain $@"
    "${RHQ_CLI_JAVA_EXE_FILE_PATH}" ${_JAVA_ENDORSED_DIRS_OPT} ${_JAVA_LIBRARY_PATH_OPT} ${RHQ_CLI_JAVA_OPTS} ${RHQ_CLI_ADDITIONAL_JAVA_OPTS} ${_LOG_CONFIG} -cp "${CLASSPATH}" org.rhq.enterprise.client.ClientMain "$@"
    exit_code=$?

else
    debug_msg "${RHQ_CLI_JAVA_EXE_FILE_PATH} ${_JAVA_ENDORSED_DIRS_OPT} ${_JAVA_LIBRARY_PATH_OPT} ${RHQ_CLI_JAVA_OPTS} ${RHQ_CLI_ADDITIONAL_JAVA_OPTS} ${_LOG_CONFIG} -cp ${CLASSPATH} org.rhq.enterprise.client.ClientMain ${RHQ_CLI_CMDLINE_OPTS}"
    "${RHQ_CLI_JAVA_EXE_FILE_PATH}" ${_JAVA_ENDORSED_DIRS_OPT} ${_JAVA_LIBRARY_PATH_OPT} ${RHQ_CLI_JAVA_OPTS} ${RHQ_CLI_ADDITIONAL_JAVA_OPTS} ${_LOG_CONFIG} -cp "${CLASSPATH}" org.rhq.enterprise.client.ClientMain ${RHQ_CLI_CMDLINE_OPTS}
    exit_code=$?
fi

debug_msg "$0 done."
exit ${exit_code}
