#!/bin/sh

# chkconfig: 2345 92 26
# description: Starts and stops the RHQ Server
#
# processname: java

# =============================================================================
# RHQ Server UNIX Startup Script
#
# This file is used to execute the RHQ Server on a UNIX platform as part of
# the platform's bootup sequence or as a foreground console process.
# Run this script without any command line options for the syntax help.
#
# This script is customizable by setting the following environment variables:
#
# Note that if this script is to be used as an init.d script, you must set
# RHQ_SERVER_HOME so this script knows where to find the Server installation.
#
#    RHQ_SERVER_DEBUG - If this is defined, the script will emit debug
#                       messages. If this is not defined or set to "false"
#                       debug messages are not emitted.
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
#    RHQ_SERVER_JAVA_OPTS - Java VM command line options to be
#                           passed into the Server's VM. If this is not defined
#                           this script will pass in a default set of options.
#                           If this is set, it completely overrides the
#                           Server's defaults. If you only want to add options
#                           to the Server's defaults, then you will want to
#                           use RHQ_SERVER_ADDITIONAL_JAVA_OPTS instead.
#
#    RHQ_SERVER_ADDITIONAL_JAVA_OPTS - additional Java VM command line options
#                                      to be passed into the Server's VM. This
#                                      is added to RHQ_SERVER_JAVA_OPTS; it
#                                      is mainly used to augment the Server's
#                                      default set of options. This can be
#                                      left unset if it is not needed.
#RHQ_SERVER_ADDITIONAL_JAVA_OPTS=-agentlib:jdwp=transport=dt_socket,address=8787,server=y,suspend=n
#
#    RHQ_SERVER_CMDLINE_OPTS - If this is defined, these are the command line
#                              arguments that will be passed to the RHQ Server
#                              JBossAS run.sh. If you only want to add options
#                              to the Server's defaults, then you will want to
#                              use RHQ_SERVER_ADDITIONAL_CMDLINE_OPTS instead.
#
#    RHQ_SERVER_ADDITIONAL_CMDLINE_OPTS - additional command line arguments to
#                                         be passed to the RHQ Server JBossAS 
#                                         run.sh. This is added to 
#                                         RHQ_SERVER_CMDLINE_OPTS; it is mainly
#                                         used to augment the Server's default
#                                         set of options. This can be left unset
#                                         if it is not needed.
#
#    RHQ_SERVER_PIDFILE_DIR - a full path to a writable directory where this
#                             script can write its pidfile to.
#                             If not defined, this defaults to the Server's
#                             bin directory.
#
# If the embedded JRE is to be used but is not available, the fallback
# JRE to be used will be determined by the JAVA_HOME environment variable.
#
# This script calls run.sh when starting the underlying JBossAS server.
# =============================================================================

# ----------------------------------------------------------------------
# Environment variables you can set to customize the launch of the RHQ Server.
# ----------------------------------------------------------------------

# RHQ_SERVER_HOME=/path/to/server/home
# RHQ_SERVER_DEBUG=true
# JAVA_HOME=/path/to/java/installation
# RHQ_SERVER_JAVA_EXE_FILE_PATH=/path/directly/to/java/executable
# RHQ_SERVER_JAVA_OPTS=VM options
# RHQ_SERVER_ADDITIONAL_JAVA_OPTS=additional VM options
# RHQ_SERVER_CMDLINE_OPTS=run.sh options
# RHQ_SERVER_ADDITIONAL_CMDLINE_OPTS=additional run.sh options

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
# Sets _SERVER_STATUS, _SERVER_RUNNING and _SERVER_PID based on the
# status of the RHQ Server VM start script (run.sh).
# Also sets _JVM_STATUS, _JVM_RUNNING and _JVM_PID based on the
# status of the JBossAS Java Virtual Machine.
# ----------------------------------------------------------------------

check_status ()
{
    if [ -f "$_SERVER_PIDFILE" ]; then
        _SERVER_PID=`cat "$_SERVER_PIDFILE"`
        if [ -n "$_SERVER_PID" ] && kill -0 $_SERVER_PID 2>/dev/null ; then
            _SERVER_STATUS="RHQ Server (pid $_SERVER_PID) is $1"
            _SERVER_RUNNING=1
        else
            _SERVER_STATUS="RHQ Server (pid $_SERVER_PID) is NOT running"
            _SERVER_RUNNING=0
        fi
    else
        _SERVER_STATUS="RHQ Server (no pid file) is NOT running"
        _SERVER_RUNNING=0
    fi

    if [ -f "$_JVM_PIDFILE" ]; then
        _JVM_PID=`cat "$_JVM_PIDFILE"`
        if [ -n "$_JVM_PID" ] && kill -0 $_JVM_PID 2>/dev/null ; then
            _JVM_STATUS="JBossAS Java VM child process (pid $_JVM_PID) is $1"
            _JVM_RUNNING=1
        else
            _JVM_STATUS="JBossAS Java VM child process (pid $_JVM_PID) is NOT running"
            _JVM_RUNNING=0
        fi
    else
        _JVM_STATUS="JBossAS Java VM child process (no pid file) is NOT running"
        _JVM_RUNNING=0
    fi
}

# ----------------------------------------------------------------------
# Ensures that the PID files no longer exist
# ----------------------------------------------------------------------

remove_pid_files ()
{
   if [ -f "$_SERVER_PIDFILE" ]; then
      rm -f "$_SERVER_PIDFILE"
   fi
   if [ -f "$_JVM_PIDFILE" ]; then
      rm -f "$_JVM_PIDFILE"
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

# ----------------------------------------------------------------------
# Determine the RHQ Server installation directory.
# If RHQ_SERVER_HOME is not defined, we will assume we are running
# directly from the server installation's bin directory.
# ----------------------------------------------------------------------

if [ -z "$RHQ_SERVER_HOME" ]; then
   if [ "x${_LINUX}${_SOLARIS}${_CYGWIN}" != "x" ]; then
      # only certain platforms support the -e argument for readlink
      _READLINK_ARG="-e"
   fi
   _DOLLARZERO=`readlink $_READLINK_ARG "$0" 2>/dev/null || echo "$0"`
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

# create the logs directory
_LOG_DIR_PATH="${RHQ_SERVER_HOME}/logs"
if [ -n "$_CYGWIN" ]; then
   _LOG_DIR_PATH=`cygpath --windows --path "$_LOG_DIR_PATH"`
fi
if [ ! -d "${_LOG_DIR_PATH}" ]; then
   mkdir "${_LOG_DIR_PATH}"
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

# If we're using IBM Java, we need to copy xalan.jar into the AS lib/endorsed dir.
if $JAVA -version 2>&1 | grep "IBM J9" >/dev/null 2>&1; then
   XALAN_JAR="${RHQ_SERVER_HOME}/etc/ibm/xalan.jar"
   INSTALLED_XALAN_JAR="${RHQ_SERVER_HOME}/jbossas/lib/endorsed/xalan.jar"
   if [ -f "$XALAN_JAR" ] && [ ! -f "$INSTALLED_XALAN_JAR" ]; then
      echo "Detected IBM Java - installing xalan.jar..."
      cp "$XALAN_JAR" "$INSTALLED_XALAN_JAR"
   fi
fi

# ----------------------------------------------------------------------
# Prepare the VM command line options to be passed in
# ----------------------------------------------------------------------

if [ -z "$RHQ_SERVER_JAVA_OPTS" ]; then
   RHQ_SERVER_JAVA_OPTS="-Xms1024M -Xmx1024M -XX:PermSize=256M -XX:MaxPermSize=256M -Djava.net.preferIPv4Stack=true -Djboss.server.log.dir=${_LOG_DIR_PATH}"
fi
# Add the JVM opts that we always want to specify, whether or not the user set RHQ_SERVER_JAVA_OPTS.
# Note that the double equals for the policy file specification IS INTENTIONAL
RHQ_SERVER_JAVA_OPTS="-Dapp.name=rhq-server $RHQ_SERVER_JAVA_OPTS -Djava.awt.headless=true -Djboss.platform.mbeanserver -Dsun.lang.ClassLoader.allowArraySyntax=true -Djava.util.logging.config.file=$RHQ_SERVER_HOME/jbossas/server/default/conf/logging.properties -Djava.security.manager -Djava.security.policy==$RHQ_SERVER_HOME/bin/rhq-server.security-policy"

debug_msg "RHQ_SERVER_JAVA_OPTS: $RHQ_SERVER_JAVA_OPTS"
debug_msg "RHQ_SERVER_ADDITIONAL_JAVA_OPTS: $RHQ_SERVER_ADDITIONAL_JAVA_OPTS"

# run.sh wants the options to be in the JAVA_OPTS variable
JAVA_OPTS="$RHQ_SERVER_JAVA_OPTS $RHQ_SERVER_ADDITIONAL_JAVA_OPTS"
export JAVA_OPTS

# ----------------------------------------------------------------------
# Prepare the command line arguments passed to the RHQ Server JBossAS
# run.sh script
# ----------------------------------------------------------------------

if [ -z "$RHQ_SERVER_CMDLINE_OPTS" ]; then
   _PROPS_FILE_PATH="${RHQ_SERVER_HOME}/bin/rhq-server.properties"

   # convert paths if we are on Windows
   if [ -n "$_CYGWIN" ]; then
      _PROPS_FILE_PATH=`cygpath --windows --path "$_PROPS_FILE_PATH"`
   fi

   RHQ_SERVER_CMDLINE_OPTS="-P ${_PROPS_FILE_PATH}"   
fi

debug_msg "RHQ_SERVER_CMDLINE_OPTS: $RHQ_SERVER_CMDLINE_OPTS"
debug_msg "RHQ_SERVER_ADDITIONAL_CMDLINE_OPTS: $RHQ_SERVER_ADDITIONAL_CMDLINE_OPTS"

# org.jboss.Main parses its command line args such that later options of a 
# certain type, override earlier options of that same type, so make sure the
# additional opts are added after the base opts, since we want the additional
# opts to take precedence
_CMDLINE_OPTS="$RHQ_SERVER_CMDLINE_OPTS $RHQ_SERVER_ADDITIONAL_CMDLINE_OPTS"

# ----------------------------------------------------------------------
# Now find the JBossAS run.sh script
# ----------------------------------------------------------------------

_JBOSS_RUN_SCRIPT="${RHQ_SERVER_HOME}/jbossas/bin/run.sh"

if [ ! -f "$_JBOSS_RUN_SCRIPT" ]; then
   echo "ERROR! Cannot find the JBossAS run script"
   echo "Not found: $_JBOSS_RUN_SCRIPT"
   exit 1
fi
debug_msg "_JBOSS_RUN_SCRIPT: $_JBOSS_RUN_SCRIPT"

# ----------------------------------------------------------------------
# Define where we want to write the pidfiles - let user override the dir
# Note that we actually have two pidfiles - one is for the script
# that starts the JBossAS Java virtual machine and the second is the
# actual server's Java virtual machine process itself.
# ----------------------------------------------------------------------

if [ -z "$RHQ_SERVER_PIDFILE_DIR" ]; then
   RHQ_SERVER_PIDFILE_DIR="${RHQ_SERVER_HOME}/bin"
fi
mkdir -p "$RHQ_SERVER_PIDFILE_DIR"

_SERVER_PIDFILE="${RHQ_SERVER_PIDFILE_DIR}/rhq-server.pid"
_JVM_PIDFILE="${RHQ_SERVER_PIDFILE_DIR}/rhq-jvm.pid"

# ----------------------------------------------------------------------
# Execute the command that the user wants us to do
# ----------------------------------------------------------------------

check_status "running"

case "$1" in
'console')
        if [ "$_SERVER_RUNNING" = "1" ]; then
           echo "$_SERVER_STATUS"
           exit 0
        fi

        echo "Starting RHQ Server in console..."

        # we are running in foreground, make both pids show the same process
        echo "$$" > "$_SERVER_PIDFILE"
        echo "$$" > "$_JVM_PIDFILE"

        # start the server, making sure its working directory is the JBossAS bin directory
        cd "${RHQ_SERVER_HOME}/jbossas/bin"
        "$_JBOSS_RUN_SCRIPT" $_CMDLINE_OPTS

        _JBOSS_STATUS=$?

        rm "$_SERVER_PIDFILE"
        rm "$_JVM_PIDFILE"

        exit $_JBOSS_STATUS
        ;;

'start')
        if [ "$_SERVER_RUNNING" = "1" ]; then
           echo "$_SERVER_STATUS"
           exit 0
        fi

        echo "Trying to start the RHQ Server..."

        LAUNCH_JBOSS_IN_BACKGROUND="$_JVM_PIDFILE"
        export LAUNCH_JBOSS_IN_BACKGROUND

        # start the server, making sure its working directory is the JBossAS bin directory
        cd "${RHQ_SERVER_HOME}/jbossas/bin"
        if [ -n "$RHQ_SERVER_DEBUG" ] && [ "$RHQ_SERVER_DEBUG" != "false" ]; then
           "$_JBOSS_RUN_SCRIPT" $_CMDLINE_OPTS &
        else
           "$_JBOSS_RUN_SCRIPT" $_CMDLINE_OPTS > /dev/null 2>&1 &
        fi

        echo "$!" > "$_SERVER_PIDFILE"

        sleep 5
        check_status "starting"
        echo "$_SERVER_STATUS"

        if [ "$_SERVER_RUNNING" = "1" ]; then
           exit 0
        else
           echo "Failed to start - make sure the RHQ Server is fully configured properly"
           exit 1
        fi
        ;;

'stop')
        if [ "$_SERVER_RUNNING" = "0" ]; then
           echo "$_SERVER_STATUS"
           remove_pid_files
           exit 0
        fi

        echo "Trying to stop the RHQ Server..."

        echo "RHQ Server (pid=${_SERVER_PID}) is stopping..."

        while [ "$_SERVER_RUNNING" = "1"  ]; do
           kill -TERM $_SERVER_PID
           sleep 2
           check_status "stopping..."
        done

        remove_pid_files
        echo "RHQ Server has stopped."
        exit 0
        ;;

'kill')
        if [ "$_SERVER_RUNNING" = "0" ]; then
           echo "$_SERVER_STATUS"
        fi
        if [ "$_JVM_RUNNING" = "0" ]; then
           echo "$_JVM_STATUS"
           remove_pid_files
           exit 0
        fi

        echo "Trying to kill the RHQ Server..."

        echo "RHQ Server parent process (pid=${_SERVER_PID}) is being killed..."
        while [ "$_SERVER_RUNNING" = "1"  ]; do
           kill -9 $_SERVER_PID
           sleep 2
           check_status "killing..."
        done

        echo "Java Virtual Machine child process (pid=${_JVM_PID}) is being killed..."
        while [ "$_JVM_RUNNING" = "1"  ]; do
           kill -9 $_JVM_PID
           sleep 2
           check_status "killing..."
        done

        remove_pid_files
        echo "RHQ Server has been killed."
        exit 0
        ;;

'status')
        echo "$_SERVER_STATUS"
        echo "$_JVM_STATUS"
        exit 0
        ;;

*)
        echo "Usage: $0 { start | stop | kill | status | console }"
        exit 1
        ;;
esac
