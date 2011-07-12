#!/bin/sh

#
# This script silently installs an RHQ Agent. It requires that the RHQ Server
# that the Agent will register with be up and running. It is configured via the
# following environment variables. For convenience, these variables can be 
# defined in a file named install-rhq-agent-env.sh located in the same directory
# as this script.
#
# RHQ_AGENT_ENV_URL - the URL or path of the rhq-agent-env.sh to be used by the 
#                     Agent                     
# RHQ_AGENT_CONFIGURATION_URL - the URL or path of the agent-configuration.xml 
#                               to be used by the Agent for its initial 
#                               configuration
# RHQ_AGENT_INSTALL_PARENT_DIR - the path of the directory under which the Agent
#                                install directory (rhq-agent) will be created
#                                (e.g. $HOME/Applications)
# RHQ_AGENT_NAME - the Agent name
# RHQ_AGENT_SERVER_BIND_ADDRESS - the Server bind address
#

# Define functions.
abort()
{
   echo >&2
   for ARG in "$@"; do
      echo "$ARG" >&2
   done
   exit 1
}

usage() 
{   
   abort "$@" "Usage: $EXE"
}

SCRIPT_PATH=`readlink -e "$0" 2>/dev/null || readlink "$0" 2>/dev/null || echo "$0"`
SCRIPT_PARENT_DIR=`dirname "$SCRIPT_PATH"`

SCRIPT_ENV_PATH="${SCRIPT_PARENT_DIR}/install-rhq-agent-env.sh"
if [ -f "$SCRIPT_ENV_PATH" ]; then
   . "$SCRIPT_ENV_PATH" $*
else
   echo "No environment script found at: $SCRIPT_ENV_PATH"
fi

# Process command line args.
EXE=`basename $0`
if [ "$#" -ne 0 ]; then
   usage "This script does not take any arguments. It is configured via environment variables."
fi  

if [ -z "$RHQ_AGENT_ENV_URL" ]; then
   abort "RHQ_AGENT_ENV_URL environment variable is not defined."
fi

if [ -z "$RHQ_AGENT_CONFIGURATION_URL" ]; then
   abort "RHQ_AGENT_CONFIGURATION_URL environment variable is not defined."
fi

if [ -z "$RHQ_AGENT_INSTALL_PARENT_DIR" ]; then
   abort "RHQ_AGENT_INSTALL_PARENT_DIR environment variable is not defined."
fi

if [ -z "$RHQ_AGENT_NAME" ]; then
   RHQ_AGENT_NAME=`hostname`
   echo "RHQ_AGENT_NAME environment variable is not defined - defaulting to $RHQ_AGENT_NAME"
fi

if [ -z "$RHQ_AGENT_SERVER_BIND_ADDRESS" ]; then
   RHQ_AGENT_SERVER_BIND_ADDRESS="127.0.0.1"
   echo "RHQ_AGENT_SERVER_BIND_ADDRESS environment variable is not defined - defaulting to $RHQ_AGENT_SERVER_BIND_ADDRESS"
fi

RHQ_SERVER_URL="http://$RHQ_AGENT_SERVER_BIND_ADDRESS:7080/"

# Download the Agent installer jarfile.
mkdir -p "$HOME/Downloads/tmp"
cd "$HOME/Downloads/tmp"
rm -rf *
wget --content-disposition --timestamping "$RHQ_SERVER_URL/agentupdate/download"
RHQ_AGENT_DIST_FILE_NAME=`echo *`
mv $RHQ_AGENT_DIST_FILE_NAME ..

# Download the Agent env file.
if [ -f "$RHQ_AGENT_ENV_URL" ]; then
   RHQ_AGENT_ENV_FILE="$RHQ_AGENT_ENV_URL"
else
   mkdir -p "$HOME/Downloads/tmp"
   cd "$HOME/Downloads/tmp"
   rm -rf *
   wget --content-disposition --no-check-certificate --timestamping "$RHQ_AGENT_ENV_URL"
   RHQ_AGENT_ENV_FILE_NAME=`echo *`
   RHQ_AGENT_ENV_FILE="$HOME/Downloads/tmp/$RHQ_AGENT_ENV_FILE_NAME"
fi

# Download the Agent config file.
if [ -f "$RHQ_AGENT_CONFIGURATION_URL" ]; then
   RHQ_AGENT_CONFIGURATION_FILE="$RHQ_AGENT_CONFIGURATION_URL"
else
   mkdir -p "$HOME/Downloads/tmp"
   cd "$HOME/Downloads/tmp"
   rm -rf *
   wget --content-disposition --no-check-certificate --timestamping "$RHQ_AGENT_CONFIGURATION_URL"
   RHQ_AGENT_CONFIGURATION_FILE_NAME=`echo *`
   RHQ_AGENT_CONFIGURATION_FILE="$HOME/Downloads/tmp/$RHQ_AGENT_CONFIGURATION_FILE_NAME"
fi

# Install the Agent.
cd "$RHQ_AGENT_INSTALL_PARENT_DIR"
# The Agent installer installs the Agent to ./rhq-agent.
RHQ_AGENT_HOME="$RHQ_SERVER_INSTALL_PARENT_DIR/rhq-agent"
if [ -f "$RHQ_AGENT_HOME" ]; then
   # Backup existing Agent installation.
   rm -rf "$RHQ_AGENT_HOME.bak"
   mv "$RHQ_AGENT_HOME" "$RHQ_AGENT_HOME.bak"
fi
java -jar "$HOME/Downloads/$RHQ_AGENT_DIST_FILE_NAME" --install

# Install the Agent env file.
mv -f "$RHQ_AGENT_ENV_FILE" "$RHQ_AGENT_HOME/bin/rhq-agent-env.sh"
sed -e "s/@@HOSTNAME@@/`hostname`/g" -e "s/@@SHORT_HOSTNAME@@/`hostname -s`/g" "$RHQ_AGENT_HOME/bin/rhq-agent-env.sh"

# Install the Agent configuration file.
mv -f "$RHQ_AGENT_CONFIGURATION_FILE" "$RHQ_AGENT_HOME/conf/agent-configuration.xml"
sed -e "s/@@HOSTNAME@@/`hostname`/g" -e "s/@@SHORT_HOSTNAME@@/`hostname -s`/g" "$RHQ_AGENT_HOME/conf/agent-configuration.xml"

# Start the Agent
$RHQ_AGENT_HOME/bin/rhq-agent-wrapper.sh start

