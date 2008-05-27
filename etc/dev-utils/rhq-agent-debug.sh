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
#    RHQ_AGENT_DEBUG - If this is defined (with any value), the script
#                      will emit debug messages. It will also enable debug
#                      messages to be emitted from the agent itself.
#
#    RHQ_AGENT_HOME - Defines where the agent's home install directory is.
#                     If not defined, it will be assumed to be the parent
#                     directory of the directory where this script lives.
#
#    RHQ_AGENT_JAVA_HOME - The location of the JRE that the agent will
#                          use. This will be ignored if
#                          RHQ_AGENT_JAVA_EXE_FILE_PATH is set.
#                          If this and RHQ_AGENT_JAVA_EXE_FILE_PATH are
#                          not set, the agent's embedded JRE will be used.
#
#    RHQ_AGENT_JAVA_EXE_FILE_PATH - Defines the full path to the Java
#                                   executable to use. If this is set,
#                                   RHQ_AGENT_JAVA_HOME is ignored.
#                                   If this is not set, then
#                                   $RHQ_AGENT_JAVA_HOME/bin/java
#                                   is used. If this and
#                                   RHQ_AGENT_JAVA_HOME are not set, the
#                                   agent's embedded JRE will be used.
#
#    RHQ_AGENT_JAVA_OPTS - Java VM command line options to be
#                          passed into the agent's VM. If this is not defined
#                          this script will pass in a default set of options.
#                          If this is set, it completely overrides the
#                          agent's defaults. If you only want to add options
#                          to the agent's defaults, then you will want to
#                          use RHQ_AGENT_ADDITIONAL_JAVA_OPTS instead.
#
#    RHQ_AGENT_ADDITIONAL_JAVA_OPTS - additional Java VM command line options
#                                     to be passed into the agent's VM. This
#                                     is added to RHQ_AGENT_JAVA_OPTS; it
#                                     is mainly used to augment the agent's
#                                     default set of options. This can be
#                                     left unset if it is not needed.
#
#    RHQ_AGENT_CMDLINE_OPTS - If this is defined, these are the command line
#                             arguments that will be passed to the RHQ Agent.
#                             If this is not defined, the command line
#                             arguments given to this script are passed
#                             through to the RHQ Agent.
#
#    RHQ_AGENT_IN_BACKGROUND - If this is defined, the RHQ Agent JVM will
#                              be launched in the background (thus causing this
#                              script to exit immediately).  If the value is
#                              something other than "nofile", it will be assumed
#                              to be a full file path which this script will
#                              create and will contain the agent VM's process
#                              pid value. If this is not defined, the VM is
#                              launched in foreground and this script blocks
#                              until the VM exits, at which time this
#                              script will also exit.
#
# If the embedded JRE is to be used but is not available, the fallback
# JRE to be used will be determined by the JAVA_HOME environment variable.
# =============================================================================

RHQ_AGENT_DEBUG=1
RHQ_AGENT_ADDITIONAL_JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,address=9797,server=y,suspend=n"

# ----------------------------------------------------------------------
# Subroutine that simply dumps a message iff debug mode is enabled
# ----------------------------------------------------------------------

debug_msg ()
{
   if [ "x$RHQ_AGENT_DEBUG" != "x" ]; then
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

if [ "x$RHQ_AGENT_HOME" = "x" ]; then
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

if [ ! -f $RHQ_AGENT_JAVA_EXE_FILE_PATH ]; then
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

# The RHQ Agent has a JNI library that it needs to find in order to
# do things like execute PIQL queries and access low-level operating
# system data. Here we add the java.library.path system property
# to point to the JNI libraries.  If you deploy a custom plugin
# that also requires JNI libraries, you must add to the library path
# here, you must not remove the RHQ Agent library path that it needs.

_JNI_PATH="${RHQ_AGENT_HOME}/lib"

# convert the path if on Windows
if [ "x$_CYGWIN" != "x" ]; then
   _JNI_PATH=`cygpath --windows --path "$_JNI_PATH"`
fi

RHQ_AGENT_JAVA_OPTS="-Djava.library.path=${_JNI_PATH} ${RHQ_AGENT_JAVA_OPTS}"

debug_msg "RHQ_AGENT_JAVA_OPTS: $RHQ_AGENT_JAVA_OPTS"
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

if [ "x$RHQ_AGENT_DEBUG" != "x" ]; then
   _LOG_CONFIG=-Dlog4j.configuration="log4j-debug.xml -Dsigar.nativeLogging=true -Di18nlog.dump-stack-traces=true"
else
   _LOG_CONFIG=-Dlog4j.configuration="log4j.xml"
fi

# log4j 1.2.8 does not create the directory for us (later versions do)
if [ ! -d "${RHQ_AGENT_HOME}/logs" ]; then
   mkdir ${RHQ_AGENT_HOME}/logs
fi

# convert some of the paths if we are on Windows
if [ "x$_CYGWIN" != "x" ]; then
   CLASSPATH=`cygpath --windows --path "$CLASSPATH"`
fi

# Build the command line that starts the VM
CMD="${RHQ_AGENT_JAVA_EXE_FILE_PATH} ${RHQ_AGENT_JAVA_OPTS} ${RHQ_AGENT_ADDITIONAL_JAVA_OPTS} ${_LOG_CONFIG} -cp ${CLASSPATH} org.rhq.enterprise.agent.AgentMain ${RHQ_AGENT_CMDLINE_OPTS}"

debug_msg "Executing the agent with this command line:"
debug_msg "$CMD"

# Run the VM - put it in background if the caller wants it to be
if [ "x$RHQ_AGENT_IN_BACKGROUND" = "x" ]; then
   $CMD
else
   $CMD &
   RHQ_AGENT_BACKGROUND_PID=$!
   export RHQ_AGENT_BACKGROUND_PID
   if [ "x$RHQ_AGENT_IN_BACKGROUND" != "nofile" ]; then
      echo $RHQ_AGENT_BACKGROUND_PID > $RHQ_AGENT_IN_BACKGROUND
   fi
fi

debug_msg echo $0 done.
