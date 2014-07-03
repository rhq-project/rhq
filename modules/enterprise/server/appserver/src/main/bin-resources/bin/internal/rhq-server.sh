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
# This script is customized by the settings in rhq-server-env.sh.  The options
# set there will be applied to this script.  It is not recommended to edit this
# script directly.
#
# NOTE: If this script is to be used as an init.d script, you must set
# RHQ_SERVER_HOME so this script knows where to find the Server installation.
#
# This script calls standalone.sh when starting the underlying JBossAS server.
# =============================================================================

# ----------------------------------------------------------------------
# Dumps a message iff debug mode is enabled
# ----------------------------------------------------------------------

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
# Environment variables that can customize the launch of the RHQ Server.
# ----------------------------------------------------------------------

# RHQ_SERVER_HOME=/path/to/server/home
# RHQ_SERVER_DEBUG=true
# RHQ_JAVA_HOME=/path/to/java/installation
# RHQ_JAVA_EXE_FILE_PATH=/path/directly/to/java/executable
# RHQ_SERVER_JAVA_OPTS=VM options
# RHQ_SERVER_ADDITIONAL_JAVA_OPTS=additional VM options
# RHQ_SERVER_CMDLINE_OPTS=standalone.sh options
# RHQ_SERVER_ADDITIONAL_CMDLINE_OPTS=additional standalone.sh options
# RHQ_SERVER_STOP_DELAY=5
# RHQ_SERVER_KILL_AFTER_STOP_DELAY=false

# ----------------------------------------------------------------------
# Environment variables to set in order to enable remote debugging.
# ----------------------------------------------------------------------

#RHQ_SERVER_ADDITIONAL_JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,address=8787,server=y,suspend=n"

# Enable JProfiler
#JPROFILER_HOME="/opt/jprofiler6"
#RHQ_SERVER_ADDITIONAL_JAVA_OPTS="$RHQ_SERVER_ADDITIONAL_JAVA_OPTS -agentlib:jprofilerti=port=8849 -Xbootclasspath/a:$JPROFILER_HOME/bin/agent.jar"
#export PATH="$PATH:$JPROFILER_HOME/bin"
#export LD_LIBRARY_PATH="$JPROFILER_HOME/bin/linux-x64"

# ----------------------------------------------------------------------
# Sets _SERVER_STATUS, _SERVER_RUNNING and _SERVER_PID based on the
# status of the RHQ Server VM start script (standalone.sh).
# Also sets _JVM_STATUS, _JVM_RUNNING and _JVM_PID based on the
# status of the JBossAS Java Virtual Machine.
# ----------------------------------------------------------------------

check_status ()
{
    if [ -f "$_SERVER_PIDFILE" ]; then
        _SERVER_PID=`cat "$_SERVER_PIDFILE"`
        if [ -n "$_SERVER_PID" ] && kill -0 $_SERVER_PID 2>/dev/null ; then
            _SERVER_STATUS=`printf "%-30s (pid %-7s) is %s" "RHQ Server" $_SERVER_PID $1`
            _SERVER_RUNNING=1
        else
            _SERVER_STATUS=`printf "%-30s (pid %-7s) is down" "RHQ Server" $_SERVER_PID`
            _SERVER_RUNNING=0
        fi
    else
        _SERVER_STATUS=`printf "%-30s (no pid file) is down" "RHQ Server"`
        _SERVER_RUNNING=0
    fi

    if [ -f "$_JVM_PIDFILE" ]; then
        _JVM_PID=`cat "$_JVM_PIDFILE"`
        if [ -n "$_JVM_PID" ] && kill -0 $_JVM_PID 2>/dev/null ; then
            _JVM_STATUS=`printf "%-30s (pid %-7s) is %s" "JBossAS Java VM child process" $_JVM_PID $1`
            _JVM_RUNNING=1
        else
            _JVM_STATUS=`printf "%-30s (pid %-7s) is down" "JBossAS Java VM child process" $_JVM_PID`
            _JVM_RUNNING=0
        fi
    else
        _JVM_STATUS=`printf "%-30s (no pid file) is down" "JBossAS Java VM child process"`
        _JVM_RUNNING=0
    fi
    add_colors
}

# ----------------------------------------------------------------------
# Function that colors _SERVER_STATUS and _JVM_STATUS if terminal
# supports colors
# ----------------------------------------------------------------------
# helper functions
red () { sed "/$1/s//`printf "\033[31m$2$1\033[0m"`/"; }
green () { sed "/$1/s//`printf "\033[32m$2$1\033[0m"`/"; }
orange () { sed "/$1/s//`printf "\033[33m$1\033[0m"`/"; }

add_colors () {
    # find out if terminal support colors
    _COLORS_NUM=`tput colors 2> /dev/null`
    if [ $? = 0 ] && [ $_COLORS_NUM -gt 2 ]; then
        (sh --version | grep bash) 1> /dev/null 2>&1
        _IS_BASH=$?
        _UP_SYMBOL=`[ $_IS_BASH = 0 ] && echo ✔`
        _DOWN_SYMBOL=`[ $_IS_BASH = 0 ] && echo ✘`
        _SERVER_STATUS=`echo "${_SERVER_STATUS}" | green running "${_UP_SYMBOL}"`
        _SERVER_STATUS=`echo "${_SERVER_STATUS}" | red down "${_DOWN_SYMBOL}"`
        _SERVER_STATUS=`echo "${_SERVER_STATUS}" | orange starting`
        _SERVER_STATUS=`echo "${_SERVER_STATUS}" | orange killing...`
        _JVM_STATUS=`echo "${_JVM_STATUS}" | green running "${_UP_SYMBOL}"`
        _JVM_STATUS=`echo "${_JVM_STATUS}" | red down "${_DOWN_SYMBOL}"`
        _JVM_STATUS=`echo "${_JVM_STATUS}" | orange starting`
        _JVM_STATUS=`echo "${_JVM_STATUS}" | orange killing...`
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
# Unset any lingering JBossAS environment variables that were set in
# the user's environment. This might happen if the user has an external
# JBossAS installed and configured but doesn't want RHQ to use it.
# ----------------------------------------------------------------------
unset_jboss_as_env ()
{
   unset JBOSS_HOME
   unset RUN_CONF
   unset JAVAC_JAR
   unset JBOSS_CLASSPATH
   unset JBOSS_BASE_DIR
   unset JBOSS_LOG_DIR
   unset JBOSS_CONFIG_DIR
}

# ----------------------------------------------------------------------
# Kill RHQ Server
# ----------------------------------------------------------------------

kill_rhq_server ()
{
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
      # Unset any lingering JBossAS environment variables that we don't want.
      # These could be in the user's environment for an external AS install.
      unset_jboss_as_env
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
   unset_jboss_as_env
fi

cd "$RHQ_SERVER_JBOSS_HOME"
RHQ_SERVER_JBOSS_HOME=`pwd`

debug_msg "RHQ_SERVER_JBOSS_HOME: $RHQ_SERVER_JBOSS_HOME"


if [ ! -f "${RHQ_SERVER_JBOSS_HOME}/jboss-modules.jar" ]; then
   echo "ERROR! RHQ_SERVER_JBOSS_HOME is not pointing to a valid RHQ Server"
   echo "Missing ${RHQ_SERVER_JBOSS_HOME}/jboss-modules.jar"
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
# create the logs directory
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

# standalone.sh will use JAVA as the full java command
JAVA="$RHQ_JAVA_EXE_FILE_PATH"
export JAVA

# ----------------------------------------------------------------------
# Prepare the VM command line options to be passed in
# ----------------------------------------------------------------------

if [ -z "$RHQ_SERVER_JAVA_OPTS" ]; then
   RHQ_SERVER_JAVA_OPTS="-Xms1024M -Xmx1024M -XX:PermSize=256M -XX:MaxPermSize=256M -Djava.net.preferIPv4Stack=true -Dorg.jboss.resolver.warning=true -Dsun.rmi.dgc.client.gcInterval=3600000 -Dsun.rmi.dgc.server.gcInterval=3600000"
fi

if [ -n "$RHQ_SERVER_DEBUG" ] && [ "$RHQ_SERVER_DEBUG" != "false" ]; then
   _JBOSS_DEBUG_LOGGING="-Djboss.boot.server.log.level=DEBUG -Djboss.boot.server.log.console.level=DEBUG"
else
   _JBOSS_DEBUG_LOGGING=
fi

# Add the JVM opts that we always want to specify, whether or not the user set RHQ_SERVER_JAVA_OPTS.
# Note that the double equals for the policy file specification IS INTENTIONAL
_HTTP_COMPRESSION="-Dorg.apache.coyote.http11.Http11Protocol.COMPRESSION=on -Dorg.apache.coyote.http11.Http11Protocol.COMPRESSION_MIME_TYPES=text/javascript,text/css,text/html"
RHQ_SERVER_JAVA_OPTS="-Dapp.name=rhq-server ${RHQ_SERVER_JAVA_OPTS} -Drhq.server.home=${RHQ_SERVER_HOME} -Djboss.server.log.dir=${_LOG_DIR_PATH} -Djava.awt.headless=true -Dsun.lang.ClassLoader.allowArraySyntax=true -Djboss.server.default.config=standalone-full.xml -Djboss.modules.system.pkgs=org.jboss.byteman -DXXXjava.security.manager -Djava.security.policy==${RHQ_SERVER_HOME}/bin/internal/rhq-server.security-policy ${_HTTP_COMPRESSION} ${_JBOSS_DEBUG_LOGGING}"

debug_msg "RHQ_SERVER_JAVA_OPTS: $RHQ_SERVER_JAVA_OPTS"
debug_msg "RHQ_SERVER_ADDITIONAL_JAVA_OPTS: $RHQ_SERVER_ADDITIONAL_JAVA_OPTS"

# standalone.sh wants the options to be in the JAVA_OPTS variable
JAVA_OPTS="$RHQ_SERVER_JAVA_OPTS $RHQ_SERVER_ADDITIONAL_JAVA_OPTS"
export JAVA_OPTS

# ----------------------------------------------------------------------
# Prepare the command line arguments passed to the RHQ Server JBossAS
# standalone.sh script
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

# JBoss may parse its command line args such that later options of a
# certain type may override earlier options of that same type, so make sure the
# additional opts are added after the base opts, since we want the additional
# opts to take precedence
_CMDLINE_OPTS="$RHQ_SERVER_CMDLINE_OPTS $RHQ_SERVER_ADDITIONAL_CMDLINE_OPTS"

# ----------------------------------------------------------------------
# We need to add our own modules to the core set of JBossAS modules.
# JBOSS_MODULEPATH tells standalone.sh what to use.
# ----------------------------------------------------------------------
_RHQ_MODULES_PATH="${RHQ_SERVER_HOME}/modules"
_INTERNAL_MODULES_PATH="${RHQ_SERVER_JBOSS_HOME}/modules"
if [ -n "$_CYGWIN" ]; then
   _RHQ_MODULES_PATH=`cygpath --windows --path "$_RHQ_MODULES_PATH"`
   _INTERNAL_MODULES_PATH=`cygpath --windows --path "$_INTERNAL_MODULES_PATH"`
fi
JBOSS_MODULEPATH="${_RHQ_MODULES_PATH}:${_INTERNAL_MODULES_PATH}"
export JBOSS_MODULEPATH
debug_msg "JBOSS_MODULEPATH: $JBOSS_MODULEPATH"

# ----------------------------------------------------------------------
# Now find the JBossAS standalone.sh script
# ----------------------------------------------------------------------

_JBOSS_RUN_SCRIPT="${RHQ_SERVER_JBOSS_HOME}/bin/standalone.sh"

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
   RHQ_SERVER_PIDFILE_DIR="${RHQ_SERVER_HOME}/bin/internal"
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

        # START SERVER
        # first, make sure its working directory is the JBossAS bin directory
        cd "${RHQ_SERVER_JBOSS_HOME}/bin"
        "$_JBOSS_RUN_SCRIPT" $_CMDLINE_OPTS

        _JBOSS_STATUS=$?

        remove_pid_files

        exit $_JBOSS_STATUS
        ;;

'start')
        if [ "$_SERVER_RUNNING" = "1" ]; then
           echo "$_SERVER_STATUS"
           exit 0
        fi

        echo "Trying to start the RHQ Server..."

        LAUNCH_JBOSS_IN_BACKGROUND=true
        JBOSS_PIDFILE="$_JVM_PIDFILE"
        export LAUNCH_JBOSS_IN_BACKGROUND
        export JBOSS_PIDFILE

        # START SERVER
        # first, make sure its working directory is the JBossAS bin directory
        cd "${RHQ_SERVER_JBOSS_HOME}/bin"
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

        kill -TERM $_SERVER_PID

        if [ -z "$RHQ_SERVER_STOP_DELAY" ]; then
            # RHQ_SERVER_STOP_DELAY is not set
            RHQ_SERVER_STOP_DELAY=5
        fi
        case $RHQ_SERVER_STOP_DELAY in
        ''|*[!0-9]*)
            echo "RHQ_SERVER_STOP_DELAY is not a number (value=${RHQ_SERVER_STOP_DELAY})"
            echo "Applying default value (5 minutes)"
            RHQ_SERVER_STOP_DELAY=5
            ;;
        *) ;;
        esac
        if [ $RHQ_SERVER_STOP_DELAY -le 0 ]; then
            echo "RHQ_SERVER_STOP_DELAY is less than or equal to zero (value=${RHQ_SERVER_STOP_DELAY})"
            echo "Applying default value (5 minutes)"
            RHQ_SERVER_STOP_DELAY=5
        fi
        waited_seconds=0
        max_wait_seconds=`expr $RHQ_SERVER_STOP_DELAY \* 60`
        while [ "$_SERVER_RUNNING" -eq "1"  ] && [ $waited_seconds -lt $max_wait_seconds ]; do
            sleep 2
            waited_seconds=`expr $waited_seconds + 2`
            check_status "stopping..."
        done

        if [ "$_SERVER_RUNNING" = "0" ]; then
            remove_pid_files
            echo "RHQ Server has stopped."
            exit 0
        fi

        debug_msg "RHQ Server did not stop within $RHQ_SERVER_STOP_DELAY minutes."
        echo "Timed out waiting for RHQ Server to stop."
        kill -QUIT $_SERVER_PID # Generate thread dump for later investigation

        if [ -n "$RHQ_SERVER_KILL_AFTER_STOP_DELAY" ] && [ "$RHQ_SERVER_KILL_AFTER_STOP_DELAY" != "false" ]; then
            kill_rhq_server
            exit 0
        else
            echo "Failed to stop RHQ Server"
            exit 127
        fi
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

        kill_rhq_server
        exit 0
        ;;

'clean')
        if [ "$_SERVER_RUNNING" = "1" ]; then
           echo "$_SERVER_STATUS"
           echo "Please shutdown the server before cleaning."
           exit 1
        fi

        echo "Cleaning data, tmp and log directories..."
        rm -rf "${RHQ_SERVER_JBOSS_HOME}/standalone/data"
        rm -rf "${RHQ_SERVER_JBOSS_HOME}/standalone/tmp"
        rm -rf "${RHQ_SERVER_HOME}/logs"
        ;;

'status')
        echo "$_SERVER_STATUS"
        echo "$_JVM_STATUS"
	if [ "$_SERVER_RUNNING" = "0" ] || [ "$_JVM_RUNNING" = 0 ]; then
	    exit 3
	fi
        exit 0
        ;;

*)
        echo "Usage: $0 { start | stop | kill | status | console | clean }"
        exit 1
        ;;
esac
