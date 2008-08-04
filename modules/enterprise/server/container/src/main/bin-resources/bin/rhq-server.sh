#!/bin/sh

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
# RHQ_SERVER_HOME so this script knows where to find the server installation.
#
#    RHQ_SERVER_DEBUG - If this is defined (with any value), the script
#                       will emit debug messages.
#
#    RHQ_SERVER_HOME - Defines where the server's home install directory is.
#                      If not defined, it will be assumed to be the parent
#                      directory of the directory where this script lives.
#
#    RHQ_SERVER_JAVA_HOME - The location of the JRE that the server will
#                           use. This will be ignored if
#                           RHQ_SERVER_JAVA_EXE_FILE_PATH is set.
#                           If this and RHQ_SERVER_JAVA_EXE_FILE_PATH are
#                           not set, the server's embedded JRE will be used.
#
#    RHQ_SERVER_JAVA_EXE_FILE_PATH - Defines the full path to the Java
#                                    executable to use. If this is set,
#                                    RHQ_SERVER_JAVA_HOME is ignored.
#                                    If this is not set, then
#                                    $RHQ_SERVER_JAVA_HOME/bin/java
#                                    is used. If this and
#                                    RHQ_SERVER_JAVA_HOME are not set, the
#                                    server's embedded JRE will be used.
#
#    RHQ_SERVER_JAVA_OPTS - Java VM command line options to be
#                           passed into the server's VM. If this is not defined
#                           this script will pass in a default set of options.
#                           If this is set, it completely overrides the
#                           server's defaults. If you only want to add options
#                           to the server's defaults, then you will want to
#                           use RHQ_SERVER_ADDITIONAL_JAVA_OPTS instead.
#
#    RHQ_SERVER_ADDITIONAL_JAVA_OPTS - additional Java VM command line options
#                                      to be passed into the server's VM. This
#                                      is added to RHQ_SERVER_JAVA_OPTS; it
#                                      is mainly used to augment the server's
#                                      default set of options. This can be
#                                      left unset if it is not needed.
#
#    RHQ_SERVER_CMDLINE_OPTS - If this is defined, these are the command line
#                              arguments that will be passed to the RHQ Server.
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
# RHQ_SERVER_CMDLINE_OPTS=additional run.sh options

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
   if [ -n "$RHQ_SERVER_DEBUG" ]; then
      echo $1
   fi
}

# ----------------------------------------------------------------------
# Sets STATUS, RUNNING and PID based on the status of the RHQ Server
# ----------------------------------------------------------------------

check_status ()
{
    if [ -f "$PIDFILE" ]; then
        PID=`cat $PIDFILE`
        if [ -n "$PID" ] && kill -0 $PID 2>/dev/null ; then
            STATUS="RHQ Server (pid $PID) is running"
            RUNNING=1
        else
            STATUS="RHQ Server (pid $PID) is NOT running"
            RUNNING=0
        fi
    else
        STATUS="RHQ Server (no pid file) is NOT running"
        RUNNING=0
    fi
}

# ----------------------------------------------------------------------
# Ensures that the PID file no longer exists
# ----------------------------------------------------------------------

remove_pid_file ()
{
   if [ -f "$PIDFILE" ]; then
      rm $PIDFILE
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
esac

# ----------------------------------------------------------------------
# Determine the RHQ Server installation directory.
# If RHQ_SERVER_HOME is not defined, we will assume we are running
# directly from the server installation's bin directory.
# ----------------------------------------------------------------------

if [ -z "$RHQ_SERVER_HOME" ]; then
   RHQ_SERVER_HOME=`dirname $0`/..
else
   if [ ! -d "$RHQ_SERVER_HOME" ]; then
      echo "ERROR! RHQ_SERVER_HOME is not pointing to a valid directory"
      echo "RHQ_SERVER_HOME: $RHQ_SERVER_HOME"
      exit 1
   fi
fi

cd $RHQ_SERVER_HOME
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
      RHQ_SERVER_JAVA_HOME=${RHQ_SERVER_HOME}/jre
      debug_msg "Using the embedded JRE"
      if [ ! -d "$RHQ_SERVER_JAVA_HOME" ]; then
         debug_msg "No embedded JRE found - will try to use JAVA_HOME: $JAVA_HOME"
         RHQ_SERVER_JAVA_HOME=$JAVA_HOME
      fi
   fi
   debug_msg "RHQ_SERVER_JAVA_HOME: $RHQ_SERVER_JAVA_HOME"
   RHQ_SERVER_JAVA_EXE_FILE_PATH=${RHQ_SERVER_JAVA_HOME}/bin/java
fi
debug_msg "RHQ_SERVER_JAVA_EXE_FILE_PATH: $RHQ_SERVER_JAVA_EXE_FILE_PATH"

if [ ! -f "$RHQ_SERVER_JAVA_EXE_FILE_PATH" ]; then
   echo There is no JVM available.
   echo Please set RHQ_SERVER_JAVA_HOME or RHQ_SERVER_JAVA_EXE_FILE_PATH appropriately.
   exit 1
fi

# run.sh will use JAVA as the full java command
JAVA=$RHQ_SERVER_JAVA_EXE_FILE_PATH
export JAVA

# ----------------------------------------------------------------------
# Prepare the VM command line options to be passed in
# ----------------------------------------------------------------------

if [ -z "$RHQ_SERVER_JAVA_OPTS" ]; then
   _LOG_DIR_PATH=${RHQ_SERVER_HOME}/logs
   if [ -n "$_CYGWIN" ]; then
      _LOG_DIR_PATH=`cygpath --windows --path "$_LOG_DIR_PATH"`
   fi
   RHQ_SERVER_JAVA_OPTS="-Xms256M -Xmx1024M -XX:PermSize=128M -XX:MaxPermSize=256M -Djava.net.preferIPv4Stack=true -Djboss.server.log.dir=${_LOG_DIR_PATH}"
fi
RHQ_SERVER_JAVA_OPTS="-Dapp.name=rhq-server $RHQ_SERVER_JAVA_OPTS -Djava.awt.headless=true -Djboss.platform.mbeanserver"
debug_msg "RHQ_SERVER_JAVA_OPTS: $RHQ_SERVER_JAVA_OPTS"
debug_msg "RHQ_SERVER_ADDITIONAL_JAVA_OPTS: $RHQ_SERVER_ADDITIONAL_JAVA_OPTS"

# run.sh wants the options to be in the JAVA_OPTS variable
JAVA_OPTS="$RHQ_SERVER_JAVA_OPTS $RHQ_SERVER_ADDITIONAL_JAVA_OPTS"
export JAVA_OPTS

# ----------------------------------------------------------------------
# Prepare the command line arguments passed to the RHQ Server
# ----------------------------------------------------------------------

if [ -z "$RHQ_SERVER_CMDLINE_OPTS" ]; then
   
   _PROPS_FILE_PATH=${RHQ_SERVER_HOME}/bin/rhq-server.properties
   
   # convert paths if we are on Windows
   if [ -n "$_CYGWIN" ]; then
      _PROPS_FILE_PATH=`cygpath --windows --path "$_PROPS_FILE_PATH"`
   fi

   RHQ_SERVER_CMDLINE_OPTS="-P ${_PROPS_FILE_PATH}"
fi
debug_msg "RHQ_SERVER_CMDLINE_OPTS: $RHQ_SERVER_CMDLINE_OPTS"

# ----------------------------------------------------------------------
# Now find the JBoss run.sh script
# ----------------------------------------------------------------------

_JBOSS_RUN_SCRIPT=${RHQ_SERVER_HOME}/jbossas/bin/run.sh

if [ ! -f "$_JBOSS_RUN_SCRIPT" ]; then
   echo "ERROR! Cannot find the JBossAS run script"
   echo "Not found: $_JBOSS_RUN_SCRIPT"
   exit 1
fi
debug_msg "_JBOSS_RUN_SCRIPT: $_JBOSS_RUN_SCRIPT"

PIDFILE=${RHQ_SERVER_HOME}/bin/.rhq-server.pid

# ----------------------------------------------------------------------
# Execute the command that the user wants us to do
# ----------------------------------------------------------------------

check_status

case "$1" in
'console')
        if [ "$RUNNING" = "1" ]; then
           echo $STATUS
           exit 0
        fi

        echo Starting RHQ Server in console...

        echo "$$" > $PIDFILE
        
        # start the server, making sure its working directory is the JBossAS bin directory 
        cd ${RHQ_SERVER_HOME}/jbossas/bin
        $_JBOSS_RUN_SCRIPT $RHQ_SERVER_CMDLINE_OPTS
        
        JBOSS_STATUS=$?
        
        rm $PIDFILE
        
        exit $JBOSS_STATUS
        ;;

'start')
        if [ "$RUNNING" = "1" ]; then
           echo $STATUS
           exit 0
        fi

        echo Starting RHQ Server...

        LAUNCH_JBOSS_IN_BACKGROUND=true
        export LAUNCH_JBOSS_IN_BACKGROUND
        
        # start the server, making sure its working directory is the JBossAS bin directory 
        cd ${RHQ_SERVER_HOME}/jbossas/bin
        if [ -z "$RHQ_SERVER_DEBUG" ]; then
           $_JBOSS_RUN_SCRIPT $RHQ_SERVER_CMDLINE_OPTS > /dev/null 2>&1 &
        else
           $_JBOSS_RUN_SCRIPT $RHQ_SERVER_CMDLINE_OPTS &
        fi

        echo "$!" > $PIDFILE
        
        sleep 5
        check_status
        echo $STATUS

        if [ "$RUNNING" = "1" ]; then
           exit 0
        else
           echo Failed to start - make sure the RHQ Server is fully configured properly
           exit 1
        fi
        ;;

'stop')
        if [ "$RUNNING" = "0" ]; then
           echo $STATUS
           remove_pid_file
           exit 0
        fi

        echo Stopping RHQ Server...
        
        if [ -n "$_SOLARIS" ]; then
        	kill -TERM `cat ${RHQ_SERVER_HOME}/jbossas/.jboss_pid`
        	sleep 3
        fi

        echo "RHQ Server (pid=${PID}) is stopping..."

        while [ "$RUNNING" = "1"  ]; do
           kill -TERM $PID
           sleep 2
           if [ -n "$_SOLARIS" ]; then
               kill -9 $PID
               sleep 2
           fi
           check_status
        done

        remove_pid_file
        echo "RHQ Server has stopped."
        exit 0
        ;;

'status')
        echo $STATUS
        exit 0
        ;;

*)
        echo "Usage: $0 { start | stop | status | console }"
        exit 1
        ;;
esac
