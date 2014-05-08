#!/bin/sh

# =============================================================================
# RHQ Server UNIX Install Script
#
# This file is used to complete the installation of the RHQ Server on a
# UNIX platform.
#
# This script is customized by the settings in rhq-server-env.sh.  The options
# set there will be applied to this script.  It is not recommended to edit this
# script directly.
# =============================================================================

debug_msg ()
{
   # if debug variable is set, it is assumed to be on, unless its value is false
   if [ -n "$RHQ_SERVER_DEBUG" ] && [ "$RHQ_SERVER_DEBUG" != "false" ]; then
      echo $1
   fi
}

if [ -f "../rhq-server-env.sh" ]; then
   . "../rhq-server-env.sh" $*
else
   debug_msg "Failed to find rhq-server-env.sh. Continuing with current environment..."
fi

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

# only certain platforms support the -e argument for readlink
if [ -n "${_LINUX}${_SOLARIS}${_CYGWIN}" ]; then
   _READLINK_ARG="-e"
fi

# ----------------------------------------------------------------------
# Determine the RHQ Server installation directory.
# If RHQ_SERVER_HOME is not defined, we will assume we are running
# directly from the server installation's bin/internal directory.
# ----------------------------------------------------------------------

if [ -z "$RHQ_SERVER_HOME" ]; then
   _DOLLARZERO=`readlink $_READLINK_ARG "$0" 2>/dev/null || echo "$0"`
   RHQ_SERVER_HOME=`dirname "$_DOLLARZERO"`/../..
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

# ----------------------------------------------------------------------
# Determine what JBossAS instance to use.
# If RHQ_SERVER_JBOSS_HOME and JBOSS_HOME are both not defined, we will
# assume we are to run the embedded AS instance from the RHQ
# installation directory - RHQ_SERVER_HOME/jbossas
# ----------------------------------------------------------------------

if [ -z "$RHQ_SERVER_JBOSS_HOME" ]; then
   if [ -z "$JBOSS_HOME" ]; then
      RHQ_SERVER_JBOSS_HOME="${RHQ_SERVER_HOME}/jbossas"
   else
      if [ ! -d "$JBOSS_HOME" ]; then
         echo "ERROR! JBOSS_HOME is not pointing to a valid AS directory"
         echo "JBOSS_HOME: $JBOSS_HOME"
         exit 1
      fi
      RHQ_SERVER_JBOSS_HOME="${JBOSS_HOME}"
   fi
else
   if [ ! -d "$RHQ_SERVER_JBOSS_HOME" ]; then
      echo "ERROR! RHQ_SERVER_JBOSS_HOME is not pointing to a valid AS directory"
      echo "RHQ_SERVER_JBOSS_HOME: $RHQ_SERVER_JBOSS_HOME"
      exit 1
   fi
fi

cd "$RHQ_SERVER_JBOSS_HOME"
RHQ_SERVER_JBOSS_HOME=`pwd`

debug_msg "RHQ_SERVER_JBOSS_HOME: $RHQ_SERVER_JBOSS_HOME"

if [ ! -f "${RHQ_SERVER_JBOSS_HOME}/jboss-modules.jar" ]; then
   echo "ERROR! RHQ_SERVER_JBOSS_HOME is not pointing to a valid AS instance"
   echo "Missing ${RHQ_SERVER_JBOSS_HOME}/jboss-modules.jar"
   exit 1
fi

# we want the rest of this script to be able to assume cwd is the RHQ install dir
cd "$RHQ_SERVER_HOME"

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
# Create the logs directory
# ----------------------------------------------------------------------

_LOG_DIR_PATH="${RHQ_SERVER_HOME}/logs"
if [ -n "$_CYGWIN" ]; then
   _LOG_DIR_PATH=`cygpath --windows --path "$_LOG_DIR_PATH"`
fi
if [ ! -d "${_LOG_DIR_PATH}" ]; then
   mkdir "${_LOG_DIR_PATH}"
fi

# ----------------------------------------------------------------------
# Find the Java executable and verify we have a VM available
# Note that RHQ_SERVER_JAVA_* props are still handled for back compat
# ----------------------------------------------------------------------
if [ -z "$RHQ_JAVA_EXE_FILE_PATH" ]; then
   if [ ! -z "$RHQ_SERVER_JAVA_EXE_FILE_PATH" ]; then
      RHQ_JAVA_EXE_FILE_PATH="$RHQ_SERVER_JAVA_EXE_FILE_PATH"
   fi
fi
if [ -z "$RHQ_JAVA_HOME" ]; then
   if [ ! -z "$RHQ_SERVER_JAVA_HOME" ]; then
      RHQ_JAVA_HOME="$RHQ_SERVER_JAVA_HOME"
   fi
fi

if [ -z "$RHQ_JAVA_EXE_FILE_PATH" ]; then
   if [ -z "$RHQ_JAVA_HOME" ]; then
      debug_msg "No RHQ JAVA property set, defaulting to JAVA_HOME: $JAVA_HOME"
      RHQ_JAVA_HOME="$JAVA_HOME"
   fi
   debug_msg "RHQ_JAVA_HOME: $RHQ_JAVA_HOME"
   RHQ_JAVA_EXE_FILE_PATH="${RHQ_JAVA_HOME}/bin/java"
fi

debug_msg "RHQ_JAVA_EXE_FILE_PATH: $RHQ_JAVA_EXE_FILE_PATH"

if [ ! -f "$RHQ_JAVA_EXE_FILE_PATH" ]; then
   echo "There is no JVM available."
   echo "Please set RHQ_JAVA_HOME or RHQ_JAVA_EXE_FILE_PATH appropriately."
   exit 1
fi

# ----------------------------------------------------------------------
# Prepare the VM command line options to be passed in
# ----------------------------------------------------------------------

if [ -z "$RHQ_SERVER_INSTALLER_JAVA_OPTS" ]; then
   RHQ_SERVER_INSTALLER_JAVA_OPTS="-Xms512M -Xmx512M -XX:PermSize=128M -XX:MaxPermSize=128M -Djava.net.preferIPv4Stack=true -Dorg.jboss.resolver.warning=true"
fi

# Add the JVM opts that we always want to specify, whether or not the user set RHQ_SERVER_INSTALLER_JAVA_OPTS.
if [ -n "$RHQ_SERVER_DEBUG" ] && [ "$RHQ_SERVER_DEBUG" != "false" ]; then
   _RHQ_LOGLEVEL="DEBUG"
else
   _RHQ_LOGLEVEL="INFO"
fi

RHQ_SERVER_INSTALLER_JAVA_OPTS="${RHQ_SERVER_INSTALLER_JAVA_OPTS} -Djava.awt.headless=true -Di18nlog.logger-type=commons -Drhq.server.properties-file=${RHQ_SERVER_HOME}/bin/rhq-server.properties -Drhq.server.installer.logdir=${RHQ_SERVER_HOME}/logs -Drhq.server.installer.loglevel=${_RHQ_LOGLEVEL} -Drhq.server.basedir=${RHQ_SERVER_HOME}"

# Sample JPDA settings for remote socket debugging
#RHQ_SERVER_INSTALLER_JAVA_OPTS="${RHQ_SERVER_INSTALLER_JAVA_OPTS} -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=y"

debug_msg "RHQ_SERVER_INSTALLER_JAVA_OPTS: $RHQ_SERVER_INSTALLER_JAVA_OPTS"
debug_msg "RHQ_SERVER_INSTALLER_ADDITIONAL_JAVA_OPTS: $RHQ_SERVER_INSTALLER_ADDITIONAL_JAVA_OPTS"

# ----------------------------------------------------------------------
# We need to add our own modules to the core set of JBossAS modules.
# ----------------------------------------------------------------------
_RHQ_MODULES_PATH="${RHQ_SERVER_HOME}/modules"
_INTERNAL_MODULES_PATH="${RHQ_SERVER_JBOSS_HOME}/modules"
if [ -n "$_CYGWIN" ]; then
   _RHQ_MODULES_PATH=`cygpath --windows --path "$_RHQ_MODULES_PATH"`
   _INTERNAL_MODULES_PATH=`cygpath --windows --path "$_INTERNAL_MODULES_PATH"`
fi
_JBOSS_MODULEPATH="${_RHQ_MODULES_PATH}:${_INTERNAL_MODULES_PATH}"
debug_msg "_JBOSS_MODULEPATH: $_JBOSS_MODULEPATH"

# start the AS instance with our main installer module
"$RHQ_JAVA_EXE_FILE_PATH" ${RHQ_SERVER_INSTALLER_JAVA_OPTS} ${RHQ_SERVER_INSTALLER_ADDITIONAL_JAVA_OPTS} -jar "${RHQ_SERVER_JBOSS_HOME}/jboss-modules.jar" -mp "$_JBOSS_MODULEPATH" org.rhq.rhq-installer-util "$@"

_EXIT_STATUS=$?
exit $_EXIT_STATUS
