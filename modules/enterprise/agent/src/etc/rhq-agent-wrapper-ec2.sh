#!/bin/bash

# =============================================================================
# RHQ AGENT LINUX/EC2 Boot Script
#
# This script is a wrapper script for rhq-agent-wrapper.sh which is used to
# start/stop the agent on LINUX/UNIX platforms. This script provides some
# additional functionality for automatically configuring the agent in an EC2
# environment. 
#
# When the agent is started with this script, it will check to  see if server 
# endpoint URL has been configured. If it finds that the agent has not already
# been configured the script fetches paramerters specified as part of the AMIs
# user-defined data, parses the parameters, looking for the jon.server.url 
# parameter. The rhq.agent.server.bind-address setting is then configured with 
# the parameter's value. If the jon.server.url parameter is not found, an error
# message is logged and the agent will not be started.
#
# To force initialization or to re-initialize the agent run,
#
#     $ rhq-agent-wrapper-ec2 init  
# =============================================================================

RHQ_AGENT_HOME=/usr/share/rhq-agent-<VERSION>
SCRIPT_NAME=rhq-wrapper-agent-ec2

function load_user_data
{
    user_data_url="http://169.254.169.254/1.0/user-data/"
    user_data_file="/tmp/user_data"

    #curl $user_data_url > $user_data_file

    user_data=`cat $user_data_file`
}

function parse_jon_server_url
{
    ORIG_IFS=$IFS
    IFS=$','

    read -rda params <<< $user_data

    IFS=$ORIG_IFS   

    for param in $params
    do
        idx=`expr index $param =`
        key=${param:0:idx - 1}

        if [ $key = jon.server.url ]; then
            jon_server_url=${param:idx}
            return 0
        fi
    done
}

function init
{
    force_init=$1
    default_prefs=~/.java/.userPrefs/rhq-agent/default/prefs.xml

    if [ -s $default_prefs ] && [ "force" != $force_init ]; then
        echo "Default configuration found at $default_prefs. Skipping " \
             "configuration. Run $SCRIPT_NAME <init> to force configuration " \
             "of server endpoint URL."
    fi

    load_user_data
    parse_jon_server_url

    if [ -z $jon_server_url ]; then
        echo "Warning: Failed find jon.server.url parameter. You need to " \
             "manually set the rhq.agent.server.bind-address property in " \
             "<RHQ_AGENT>/conf/agent-configuration.xml. Cannot start agent."
        return 1
    fi

    export RHQ_AGENT_CMDLINE_OPTS="--daemon $RHQ_AGENT_CMDLINE_OPTS" 
    export RHQ_AGENT_CMDLINE_OPTS="-Drhq.agent.server.bind-address=$jon_server_url $RHQ_AGENT_CMDLINE_OPTS" 

    return 0
}


cd $RHQ_AGENT_HOME/bin

case "$1" in
'init')
    init "force"
    ;;
'start')
    if init
    then
        ./rhq-agent-wrapper.sh start
    else
        echo "Failed: Cannot start the agent do to previous initialization errors"
    fi
    ;;
'stop')
    ./rhq-agent-wrapper.sh stop
    ;;
'kill')
    ./rhq-agent-wrapper.sh kill
    ;;
'status')
    ./rhq-agent-wrapper.sh status
    ;;
'restart')
    ./rhq-agent-wrapper.sh restart
    ;;
'quiet-restart')
    ./rhq-agent-wrapper.sh quiet-restart
    ;;
'echo')
    echo "script: $0"
    ;;
*)
    echo "Usage: $0 { init | start | stop | kill | restart | status }"
    exit 1
    ;;
esac
