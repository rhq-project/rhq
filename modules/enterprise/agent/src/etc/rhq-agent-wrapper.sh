#!/bin/sh

# chkconfig: 2345 93 25
# description: Starts and stops the RHQ agent
#
# processname: java
# pidfile: /var/run/rhq-agent.pid

# =============================================================================
# RHQ Agent UNIX Boot Script
#
# This file is used to execute the RHQ Agent on a UNIX platform as part of
# the platform's bootup sequence.
# Run this script without any command line options for the syntax help.
#
# This script is customizable by setting certain environment variables, which
# are described in comments in rhq-agent-env.sh found in the same directory
# as this script. The variables can also be set via that rhq-agent-env.sh file,
# which is sourced by this script.
#
# Note that if this script is to be used as an init.d script, you must ensure
# this script has the RHQ_AGENT_HOME set so it knows where to find the
# agent installation.
# =============================================================================

# ----------------------------------------------------------------------
# Function that simply dumps a message iff debug mode is enabled
# ----------------------------------------------------------------------

debug_wrapper_msg ()
{
   # if debug variable is set, it is assumed to be on, unless its value is false
   if [ -n "$RHQ_AGENT_DEBUG" ] && [ "$RHQ_AGENT_DEBUG" != "false" ]; then
      echo "rhq-agent-wrapper.sh: $1"
   fi
}

# ----------------------------------------------------------------------
# Function that sets _STATUS, _RUNNING and _PID based on the status of
# the RHQ Agent process as found in the pidfile
# ----------------------------------------------------------------------

check_status ()
{
    if [ -f "${_PIDFILE}" ]; then
        _PID=`cat "${_PIDFILE}"`
        check_status_of_pid $_PID
    else
        _STATUS=`printf "%-30s (no pid file) is down" "RHQ Agent"`
        _RUNNING=0
        add_colors
    fi
}

# ----------------------------------------------------------------------
# Function that sets _STATUS and _RUNNING based on the status of
# the given pid number
# ----------------------------------------------------------------------

check_status_of_pid ()
{
    if [ -n "$1" ] && kill -0 $1 2>/dev/null ; then
        _STATUS=`printf "%-30s (pid %-7s) is running %s" "RHQ Agent" $1`
        _RUNNING=1
    else
        _STATUS=`printf "%-30s (pid %-7s) is down" "RHQ Agent" $1`
        _RUNNING=0
    fi
    add_colors
}

# ----------------------------------------------------------------------
# Function that colors _STATUS if terminal supports it
# ----------------------------------------------------------------------
# helper functions
red () { sed "/$1/s//`printf "\033[31m$2$1\033[0m"`/"; }
green () { sed "/$1/s//`printf "\033[32m$2$1\033[0m"`/"; }

add_colors () {
    # find out if terminal support colors
    _COLORS_NUM=`tput colors 2> /dev/null`
    if [ $? = 0 ] && [ $_COLORS_NUM -gt 2 ]; then
        _COLOR=true
        (sh --version | grep bash) 1> /dev/null 2>&1
        _IS_BASH=$?
        _UP_SYMBOL=`[ $_IS_BASH = 0 ] && echo ✔`
        _DOWN_SYMBOL=`[ $_IS_BASH = 0 ] && echo ✘`
        _STATUS=`echo "${_STATUS}" | green running "${_UP_SYMBOL}"`
        _STATUS=`echo "${_STATUS}" | red down "${_DOWN_SYMBOL}"`
    fi
}

# ----------------------------------------------------------------------
# Function that ensures that the pidfile no longer exists
# ----------------------------------------------------------------------

remove_pid_file ()
{
   if [ -f "${_PIDFILE}" ]; then
      debug_wrapper_msg "Removing existing pidfile"
      rm "${_PIDFILE}"
   fi
}

# ----------------------------------------------------------------------
# Function that prepares the location where the pidfile will live
# ----------------------------------------------------------------------

prepare_pid_dir ()
{
   mkdir -p "$RHQ_AGENT_PIDFILE_DIR"
   if [ "$?" != "0" ]; then
      echo "Cannot create the directory where the pidfile will go: ${RHQ_AGENT_PIDFILE_DIR}"
      exit 1
   fi
}

# ----------------------------------------------------------------------
# Determine what specific platform we are running on.
# Set some platform-specific variables.

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

# -------------------------------
# Get the location of this script.
# Make sure we take into account the possibility $0
# is a symlink to the real agent installation script.
# Only certain platforms support the -e option of readlink

command -v readlink >/dev/null 2>&1
if [ $? -ne 0 ]; then
    echo >&2 'WARNING: The readlink command is not available on this platform.'
    echo >&2 '         If this script was launched from a symbolic link, errors may occur.'
    echo >&2 '         Consider installing readlink on this platform.'
    _DOLLARZERO="$0"
else
    # only certain platforms support the -e argument for readlink
    if [ -n "${_LINUX}${_SOLARIS}${_CYGWIN}" ]; then
       _READLINK_ARG="-e"
    fi
    _DOLLARZERO=`readlink $_READLINK_ARG "$0" 2>/dev/null || echo "$0"`
fi


# Define and export RHQ_AGENT_HOME, we will assume we are running
# directly from the agent installation's bin directory
# Note: later sourced scripts can and may override this
if [ -z "$RHQ_AGENT_HOME" ]; then
   RHQ_AGENT_HOME=`dirname $_DOLLARZERO`
   RHQ_AGENT_HOME=`dirname $RHQ_AGENT_HOME`
fi

export RHQ_AGENT_HOME


# -------------------------------
# Read in the rhq-agent-env.sh file so we get the configured agent environment

RHQ_AGENT_WRAPPER_BIN_DIR_PATH=`dirname "$_DOLLARZERO"`
debug_wrapper_msg "RHQ_AGENT_WRAPPER_BIN_DIR_PATH=$RHQ_AGENT_WRAPPER_BIN_DIR_PATH"

if [ -f "${RHQ_AGENT_WRAPPER_BIN_DIR_PATH}/rhq-agent-env.sh" ]; then
   debug_wrapper_msg "Loading environment script: ${RHQ_AGENT_WRAPPER_BIN_DIR_PATH}/rhq-agent-env.sh"
   . "${RHQ_AGENT_WRAPPER_BIN_DIR_PATH}/rhq-agent-env.sh" $*

   # add umask so generated content is not world readable
   if [ -z "${RHQ_AGENT_UMASK}" ]; then
      RHQ_AGENT_UMASK=007
   fi

   umask ${RHQ_AGENT_UMASK} >/dev/null
   if [ $? -ne 0 ]; then
      echo >&2 "RHQ_AGENT_UMASK contains an invalid umask value of [${RHQ_AGENT_UMASK}]"
   fi
fi

# -------------------------------
# The --daemon argument is required

found_daemon_option=0
for opt in $RHQ_AGENT_CMDLINE_OPTS; do
   if [ "$opt" = "-d" ] || [ "$opt" = "--daemon" ]; then
      found_daemon_option=1
      break
   fi
done
if [ "$found_daemon_option" = "0" ]; then
   RHQ_AGENT_CMDLINE_OPTS="--daemon $RHQ_AGENT_CMDLINE_OPTS"
fi
export RHQ_AGENT_CMDLINE_OPTS

# -------------------------------
# Determine where this script is, and change to its directory

cd "$RHQ_AGENT_WRAPPER_BIN_DIR_PATH"
_THIS_SCRIPT_DIR=`pwd`
_THIS_SCRIPT="${_THIS_SCRIPT_DIR}"/`basename "$_DOLLARZERO"`

# -------------------------------
# Figure out where the RHQ Agent's home directory is and cd to it.
cd "${RHQ_AGENT_HOME}" || {
   echo "Cannot go to the RHQ_AGENT_HOME directory: ${RHQ_AGENT_HOME}"
   exit 1
}


# -------------------------------
# create the logs directory

if [ ! -d "${RHQ_AGENT_HOME}/logs" ]; then
   mkdir "${RHQ_AGENT_HOME}/logs"
fi

# -------------------------------
# Find out where to put the pidfile and prepare its directory

if [ -z "$RHQ_AGENT_PIDFILE_DIR" ]; then
   RHQ_AGENT_PIDFILE_DIR="${RHQ_AGENT_HOME}/bin"
fi
_PIDFILE="${RHQ_AGENT_PIDFILE_DIR}/rhq-agent.pid"
debug_wrapper_msg "pidfile will be located at ${_PIDFILE}"

# -------------------------------
# Main processing starts here

check_status

case "$1" in
'start')
        prepare_pid_dir

        if [ "$_RUNNING" = "1" ]; then
           echo $_STATUS
           exit 0
        fi

        echo "Starting RHQ Agent..."

        RHQ_AGENT_IN_BACKGROUND="${_PIDFILE}"
        export RHQ_AGENT_IN_BACKGROUND

        # Determine the command to execute when starting the agent
        if [ -z "$RHQ_AGENT_START_COMMAND" ]; then
           # Find out where the agent start script is located
           _START_SCRIPT="${RHQ_AGENT_HOME}/bin/rhq-agent.sh"

           if [ ! -f "$_START_SCRIPT" ]; then
              echo "ERROR! Cannot find the RHQ Agent start script"
              echo "Not found: $_START_SCRIPT"
              exit 1
           fi
           debug_wrapper_msg "Start script found here: $_START_SCRIPT"

           RHQ_AGENT_START_COMMAND="'${_START_SCRIPT}'"
        fi

        # If a password prompt needs to be shown, do it now
        if [ -n "$RHQ_AGENT_PASSWORD_PROMPT" ]; then
           if [ "$RHQ_AGENT_PASSWORD_PROMPT" = "true" ]; then
              RHQ_AGENT_PASSWORD_PROMPT="Enter password to start the agent"
           fi
           echo $RHQ_AGENT_PASSWORD_PROMPT
        fi

        # start the agent now!
        if [ -n "$RHQ_AGENT_DEBUG" ] && [ "$RHQ_AGENT_DEBUG" != "false" ]; then
           debug_wrapper_msg "Executing agent with command: ${RHQ_AGENT_START_COMMAND}"
           eval "$RHQ_AGENT_START_COMMAND > \"${RHQ_AGENT_HOME}/logs/rhq-agent-wrapper.log\" 2>&1"
        else
           # since the start command may run as non-root user, ensure the old
           # wrapper (which may be owned by root) is removed
           rm -f $RHQ_AGENT_HOME/logs/rhq-agent-wrapper.log
           eval "$RHQ_AGENT_START_COMMAND > \"${RHQ_AGENT_HOME}/logs/rhq-agent-wrapper.log\" 2>&1"
        fi

        sleep 5
        check_status
        echo $_STATUS

        if [ "$_RUNNING" = "1" ]; then
           exit 0
        else
           echo "Failed to start - make sure the RHQ Agent is fully configured properly"
           exit 1
        fi
        ;;

'config')
        prepare_pid_dir

        if [ "$_RUNNING" = "1" ]; then
           echo "Cannot run config - please stop the agent before running config"
           echo $_STATUS
           exit 0
        fi

        echo "Configure RHQ Agent..."

        # Determine the command to execute when starting the agent
        if [ -z "$RHQ_AGENT_START_COMMAND" ]; then
           # Find out where the agent start script is located
           _START_SCRIPT="${RHQ_AGENT_HOME}/bin/rhq-agent.sh"

           if [ ! -f "$_START_SCRIPT" ]; then
              echo "ERROR! Cannot find the RHQ Agent start script"
              echo "Not found: $_START_SCRIPT"
              exit 1
           fi
           debug_wrapper_msg "Start script found here: $_START_SCRIPT"

           RHQ_AGENT_START_COMMAND="${_START_SCRIPT}"
        fi


        if [ -z "$2" ]; then
           RHQ_AGENT_CMDLINE_OPTS="--cleanconfig --nostart --daemon --setup --advanced"
           export RHQ_AGENT_CMDLINE_OPTS
        else
           if [ ! -f "$2" ]; then
              echo "ERROR! Cannot find config file"
              echo "Not found: $2"
              exit 1
           fi

           RHQ_AGENT_CMDLINE_OPTS="--cleanconfig --nostart --daemon --config=\"$2\""
           export RHQ_AGENT_CMDLINE_OPTS
        fi

        # start the agent now!
        if [ -n "$RHQ_AGENT_DEBUG" ] && [ "$RHQ_AGENT_DEBUG" != "false" ]; then
           debug_wrapper_msg "Executing agent with command: ${RHQ_AGENT_START_COMMAND} ${RHQ_AGENT_CMDLINE_OPTS}"
        fi

        eval "$RHQ_AGENT_START_COMMAND"

        ;;

'cleanconfig')
        prepare_pid_dir

        if [ "$_RUNNING" = "1" ]; then
           echo "Cannot run config - please stop the agent before running config"
           echo $_STATUS
           exit 0
        fi

        echo "Configure RHQ Agent..."

        # Determine the command to execute when starting the agent
        if [ -z "$RHQ_AGENT_START_COMMAND" ]; then
           # Find out where the agent start script is located
           _START_SCRIPT="${RHQ_AGENT_HOME}/bin/rhq-agent.sh"

           if [ ! -f "$_START_SCRIPT" ]; then
              echo "ERROR! Cannot find the RHQ Agent start script"
              echo "Not found: $_START_SCRIPT"
              exit 1
           fi
           debug_wrapper_msg "Start script found here: $_START_SCRIPT"

           RHQ_AGENT_START_COMMAND="${_START_SCRIPT}"
        fi

        RHQ_AGENT_CMDLINE_OPTS="--cleanconfig --nostart --daemon"
        export RHQ_AGENT_CMDLINE_OPTS

        # start the agent now!
        if [ -n "$RHQ_AGENT_DEBUG" ] && [ "$RHQ_AGENT_DEBUG" != "false" ]; then
           debug_wrapper_msg "Executing agent with command: ${RHQ_AGENT_START_COMMAND} ${RHQ_AGENT_CMDLINE_OPTS}"
        fi

        eval "$RHQ_AGENT_START_COMMAND"

        ;;

'stop')
        prepare_pid_dir

        if [ "$_RUNNING" = "0" ]; then
           echo $_STATUS
           remove_pid_file
           exit 0
        fi

        echo "Stopping RHQ Agent..."

        _PID_TO_KILL=$_PID;

        echo "RHQ Agent (pid=${_PID_TO_KILL}) is stopping..."
        kill -TERM $_PID_TO_KILL

        sleep 5
        check_status_of_pid $_PID_TO_KILL
        if [ "$_RUNNING" = "1" ]; then
           debug_wrapper_msg "Agent did not die yet, trying to kill it again"
           kill -TERM $_PID_TO_KILL
        fi

        count=0
        while [ "$_RUNNING" = "1" ]; do
           sleep 2
           check_status_of_pid $_PID_TO_KILL
           count=`expr $count + 1`
           if [ $count -gt 150 ]; then
               echo "RHQ Agent could not be cleanly shutdown"
               kill -9 $_PID_TO_KILL
               break
           fi
        done

        remove_pid_file
        echo "RHQ Agent has stopped."
        exit 0
        ;;

'kill')
        prepare_pid_dir

        if [ "$_RUNNING" = "0" ]; then
           echo $_STATUS
           remove_pid_file
           exit 0
        fi

        echo "Killing RHQ Agent..."

        _PID_TO_KILL=$_PID;

        # do not try to gracefully kill, use a hard -KILL/-9
        echo "RHQ Agent (pid=${_PID_TO_KILL}) is being killed..."
        kill -9 $_PID_TO_KILL

        while [ "$_RUNNING" = "1"  ]; do
           sleep 2
           check_status_of_pid $_PID_TO_KILL
        done

        remove_pid_file
        echo "RHQ Agent has been killed."
        exit 0
        ;;

'status')
        echo "$_STATUS"
        if [ "$_RUNNING" = "0" ]; then
           exit 1
        fi
        exit 0
        ;;

'restart')
        "${_THIS_SCRIPT}" stop
        "${_THIS_SCRIPT}" start
        exit $?
        ;;

'quiet-restart')
        "${_THIS_SCRIPT}" stop >> /dev/null
        "${_THIS_SCRIPT}" start >> /dev/null
        exit $?
        ;;
*)
        echo "Usage: $0 { start | stop | kill | restart | status | config | cleanconfig | quiet-restart}"
        exit 1
        ;;
esac
