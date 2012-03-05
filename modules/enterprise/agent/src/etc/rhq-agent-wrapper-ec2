#!/bin/sh

# chkconfig: 2345 93 25
# description: Starts and stops the JBoss ON agent
#
# processname: java
# pidfile: /var/run/rhq-agent.pid

# =============================================================================
# RHQ Agent UNIX/EC2 Boot Script
#
# Grab the user data for the EC2 instance and parse out the JON server URL.
# Calls rhq-agent-wrapper.sh for the rest of the startup/shutdown handling.

# source function library
. /etc/init.d/functions

RHQ_AGENT_HOME=/usr/share/rhq-agent-<VERSION>
export RHQ_AGENT_HOME

RHQ_AGENT_JAVA_EXE_FILE_PATH=/usr/bin/java
export RHQ_AGENT_JAVA_EXE_FILE_PATH

RHQ_AGENT_PIDFILE_DIR=/var/run
export RHQ_AGENT_PIDFILE_DIR

RHQ_AGENT_ADDITIONAL_JAVA_OPTS="-Drhq.agent.data-directory=/var/lib/jon-agent/data -Djava.util.prefs.userRoot=/var/lib/jon-agent/prefs"
export RHQ_AGENT_ADDITIONAL_JAVA_OPTS

function set_server_params() {
    local USER_DATA SERVER_URL SERVER_PORT INSTANCE_ID
    USER_DATA="$(curl http://169.254.169.254/1.0/user-data/ 2>/dev/null)"
    INSTANCE_ID="$(curl 169.254.169.254/1.0/meta-data/instance-id 2>/dev/null)"
    if [ $? != 0 ]; then
	echo -n $"Unable to retrieve EC2 user data for JON Agent"
	echo_failure
        echo
	exit 1
    fi
    for token in $USER_DATA; do
	if strstr "$token" "jon.server.url"; then
	    # remove prefix
	    SERVER_URL="${token##*jon.server.url=}"
	    # remove any trailing comma-separated values
	    SERVER_URL="${SERVER_URL%%,*}"
	fi
	if strstr "$token" "jon.server.port"; then
	    SERVER_PORT="${token##*jon.server.port=}"
	    SERVER_PORT="${SERVER_PORT%%,*}"
	fi
    done
    if [ -z "$SERVER_URL" ]; then
	echo -n $"No jon.server.url present in EC2 user data"
	echo_failure
	echo
	exit 1
    fi
    RHQ_AGENT_ADDITIONAL_JAVA_OPTS="$RHQ_AGENT_ADDITIONAL_JAVA_OPTS -Drhq.agent.name=$INSTANCE_ID"
    RHQ_AGENT_ADDITIONAL_JAVA_OPTS="$RHQ_AGENT_ADDITIONAL_JAVA_OPTS -Drhq.agent.server.bind-address=$SERVER_URL"
    if [ -n "$SERVER_PORT" ]; then
	RHQ_AGENT_ADDITIONAL_JAVA_OPTS="$RHQ_AGENT_ADDITIONAL_JAVA_OPTS -Drhq.agent.server.bind-port=$SERVER_PORT"
    fi
    export RHQ_AGENT_ADDITIONAL_JAVA_OPTS
}

if [ "$1" = "start" ]; then
    set_server_params
fi

exec $RHQ_AGENT_HOME/bin/rhq-agent-wrapper.sh "$@"
