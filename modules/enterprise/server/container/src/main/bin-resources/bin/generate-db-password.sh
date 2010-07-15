#!/bin/sh

# =============================================================================
# RHQ Server UNIX Generate db password script
#
# This file is used to execute the generate a new encrypted db password.
#
# This script is customizable by setting the following environment variables:
#
#
#    RHQ_SERVER_HOME - Defines where the server's home install directory is.
#                      If not defined, it will be assumed to be the parent
#                      directory of the directory where this script lives.
#
#    RHQ_SERVER_JAVA_HOME - The location of the JRE that the server will
#                           use. This will be ignored if
#                           RHQ_SERVER_JAVA_EXE_FILE_PATH is set.
#                           If this and RHQ_SERVER_JAVA_EXE_FILE_PATH are
#                           not set, the Server's embedded JRE will be used.
#
#    RHQ_SERVER_JAVA_EXE_FILE_PATH - Defines the full path to the Java
#                                    executable to use. If this is set,
#                                    RHQ_SERVER_JAVA_HOME is ignored.
#                                    If this is not set, then
#                                    $RHQ_SERVER_JAVA_HOME/bin/java
#                                    is used. If this and
#                                    RHQ_SERVER_JAVA_HOME are not set, the
#                                    Server's embedded JRE will be used.
#
# If the embedded JRE is to be used but is not available, the fallback
# JRE to be used will be determined by the JAVA_HOME environment variable.
#
# =============================================================================

# ----------------------------------------------------------------------
# Environment variables you can set to customize the launch of the RHQ Server.
# ----------------------------------------------------------------------

# RHQ_SERVER_HOME=/path/to/server/home
# RHQ_SERVER_DEBUG=true
# JAVA_HOME=/path/to/java/installation
# RHQ_SERVER_JAVA_EXE_FILE_PATH=/path/directly/to/java/executable
# ----------------------------------------------------------------------
# Make sure we unset any lingering JBossAS environment variables that
# were set in the user's environment.  This might happen if the user
# has an external JBossAS configured.
# ----------------------------------------------------------------------

unset JBOSS_HOME
unset RUN_CONF
unset JAVAC_JAR
unset JBOSS_CLASSPATH

# ----------------------------------------------------------------------
# Dumps a message iff debug mode is enabled
# ----------------------------------------------------------------------

debug_msg ()
{
   # if debug variable is set, it is assumed to be on, unless its value is false
   if [ "x$RHQ_SERVER_DEBUG" != "x" ]; then
      if [ "$RHQ_SERVER_DEBUG" != "false" ]; then
         echo $1
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
   SunOS*) _SOLARIS=true
            ;;
   AIX*) _AIX=true
            ;;
esac

# ----------------------------------------------------------------------
# Determine the RHQ Server installation directory.
# If RHQ_SERVER_HOME is not defined, we will assume we are running
# directly from the server installation's bin directory.
# ----------------------------------------------------------------------

if [ -z "$RHQ_SERVER_HOME" ]; then
   _DOLLARZERO=`readlink "$0" 2>/dev/null || echo "$0"`
   RHQ_SERVER_HOME=`dirname "$_DOLLARZERO"`/..
else
   if [ ! -d "$RHQ_SERVER_HOME" ]; then
      echo "ERROR! RHQ_SERVER_HOME is not pointing to a valid directory"
      echo "RHQ_SERVER_HOME: $RHQ_SERVER_HOME"
      exit 1
   fi
fi

cd "$RHQ_SERVER_HOME"
RHQ_SERVER_HOME=`pwd`

debug_msg "RHQ_SERVER_HOME: $RHQ_SERVER_HOME"

if [ ! -f "${RHQ_SERVER_HOME}/jbossas/bin/run.jar" ]; then
   echo "ERROR! RHQ_SERVER_HOME is not pointing to a valid RHQ Server"
   echo "Missing ${RHQ_SERVER_HOME}/jbossas/bin/run.jar"
   exit 1
fi

# ----------------------------------------------------------------------
# if we are on a Mac and JAVA_HOME is not set, then set it to /usr
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

if [ -z "$RHQ_SERVER_JAVA_EXE_FILE_PATH" ]; then
   if [ -z "$RHQ_SERVER_JAVA_HOME" ]; then
      RHQ_SERVER_JAVA_HOME="${RHQ_SERVER_HOME}/jre"
      debug_msg "Using the embedded JRE"
      if [ ! -d "$RHQ_SERVER_JAVA_HOME" ]; then
         debug_msg "No embedded JRE found - will try to use JAVA_HOME: $JAVA_HOME"
         RHQ_SERVER_JAVA_HOME="$JAVA_HOME"
      fi
   fi
   debug_msg "RHQ_SERVER_JAVA_HOME: $RHQ_SERVER_JAVA_HOME"
   RHQ_SERVER_JAVA_EXE_FILE_PATH="${RHQ_SERVER_JAVA_HOME}/bin/java"
fi
debug_msg "RHQ_SERVER_JAVA_EXE_FILE_PATH: $RHQ_SERVER_JAVA_EXE_FILE_PATH"

if [ ! -f "$RHQ_SERVER_JAVA_EXE_FILE_PATH" ]; then
   echo "There is no JVM available."
   echo "Please set RHQ_SERVER_JAVA_HOME or RHQ_SERVER_JAVA_EXE_FILE_PATH appropriately."
   exit 1
fi

# run.sh will use JAVA as the full java command
JAVA="$RHQ_SERVER_JAVA_EXE_FILE_PATH"
export JAVA

if [ $# -eq 0 ]
then
    echo "Usage generate-db-password <password>"
    exit 1
fi

_JB_DIR=${RHQ_SERVER_HOME}/jbossas
$JAVA -cp $_JB_DIR/lib/jboss-common.jar:$_JB_DIR/lib/jboss-jmx.jar:$_JB_DIR/server/default/lib/jbosssx.jar:$_JB_DIR/server/default/lib/jboss-jca.jar org.jboss.resource.security.SecureIdentityLoginModule $*
