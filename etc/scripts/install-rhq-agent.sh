#!/bin/sh

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
   abort "$@" "Usage:   $EXE RHQ_SERVER_URL RHQ_AGENT_ENV_URL RHQ_AGENT_CONFIGURATION_URL RHQ_AGENT_INSTALL_PARENT_DIR" "Example: $EXE http://localhost:7080/ rhq-agent-env.sh agent-configuration.xml ~/Applications"
}

# Process command line args.
EXE=`basename $0`
if [ "$#" -ne 4 ]; then
   usage "Invalid number of arguments."
fi  
RHQ_SERVER_URL="$1"
RHQ_AGENT_ENV_URL="$2"
RHQ_AGENT_CONFIGURATION_URL="$3"
RHQ_AGENT_INSTALL_PARENT_DIR="$4"

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

