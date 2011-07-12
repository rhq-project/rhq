#!/bin/sh

#
# This script silently installs an RHQ Server under a specified parent 
# directory. The script will make sure the name of the RHQ Server install 
# directory is "rhq-server", so as to keep its location consistent across 
# upgrades and make it easier to manage via scripts. 
# 
# NOTE: If you are upgrading or replacing an existing set of RHQ Servers, you 
# must make sure all of those Servers are stopped before running this script. 
#
# The script is configured via the following environment variables. For 
# convenience, these variables can be defined in a file named 
# install-rhq-server-env.sh located in the same directory as this script.
#
# RHQ_SERVER_DIST_URL - the URL or path of the RHQ Server distribution zipfile
# RHQ_SERVER_PROPERTIES_URL - the URL or path of the rhq-server.properties file
#                             containing the configuration the Server should use
# RHQ_SERVER_INSTALL_PARENT_DIR - the path of the directory under which the 
#                                 Server install directory (rhq-server) will be
#                                 created (e.g. $HOME/Applications)
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

# Process command line args.
EXE=`basename $0`
if [ "$#" -ne 0 ]; then
   usage "This script does not take any arguments. It is configured via environment variables."
fi  

if [ -z "$RHQ_SERVER_DIST_URL" ]; then
   abort "RHQ_SERVER_DIST_URL environment variable is not defined."
fi

if [ -z "$RHQ_SERVER_PROPERTIES_URL" ]; then
   abort "RHQ_SERVER_PROPERTIES_URL environment variable is not defined."
fi

if [ -z "$RHQ_SERVER_INSTALL_PARENT_DIR" ]; then
   abort "RHQ_SERVER_INSTALL_PARENT_DIR environment variable is not defined."
fi

echo "*** NOTE *** If you are upgrading or replacing existing RHQ Servers, make sure *all* of those Servers are stopped, then hit Enter to continue."
read

# Download the Server zipfile.
if [ -f "$RHQ_SERVER_DIST_URL" ]; then
   RHQ_SERVER_DIST_FILE="$RHQ_SERVER_DIST_URL"
else
   mkdir -p "$HOME/Downloads/tmp"
   cd "$HOME/Downloads/tmp"
   rm -rf *
   wget --content-disposition --no-check-certificate --timestamping "$RHQ_SERVER_DIST_URL"
   RHQ_SERVER_DIST_FILE_NAME=`echo *`
   mv $RHQ_SERVER_DIST_FILE_NAME ..
   RHQ_SERVER_DIST_FILE="$HOME/Downloads/$RHQ_SERVER_DIST_FILE_NAME"
fi

# Download the Server properties file.
if [ -f "$RHQ_SERVER_PROPERTIES_URL" ]; then
   RHQ_SERVER_PROPERTIES_FILE="$RHQ_SERVER_PROPERTIES_URL"
else
   mkdir -p "$HOME/Downloads/tmp"
   cd "$HOME/Downloads/tmp"
   rm -rf *
   wget --content-disposition --no-check-certificate --timestamping "$RHQ_SERVER_PROPERTIES_URL"
   RHQ_SERVER_PROPERTIES_FILE_NAME=`echo *`
   RHQ_SERVER_PROPERTIES_FILE="$HOME/Downloads/tmp/$RHQ_SERVER_PROPERTIES_FILE_NAME"
fi

# Unzip the Server zipfile.
mkdir "$RHQ_SERVER_INSTALL_PARENT_DIR/tmp"
cd "$RHQ_SERVER_INSTALL_PARENT_DIR/tmp"
unzip -q "$RHQ_SERVER_DIST_FILE"
RHQ_SERVER_HOME_BASE_NAME=`echo *`

RHQ_SERVER_HOME="$RHQ_SERVER_INSTALL_PARENT_DIR/rhq-server"
if [ -f "$RHQ_SERVER_HOME" ]; then
   # Backup existing Server installation.
   rm -rf "$RHQ_SERVER_HOME.bak"
   mv "$RHQ_SERVER_HOME" "$RHQ_SERVER_HOME.bak"
fi
# In the dist zipfile, the name of the RHQ install dir includes the version
# (e.g. "rhq-server-4.1.0-SNAPSHOT"). Rename it to "rhq-server" to keep its 
# location consistent across upgrades and make it easier to manage via scripts.
mv "$RHQ_SERVER_HOME_BASE_NAME" "$RHQ_SERVER_HOME"

# Install the Server properties file.
mv -f "$RHQ_SERVER_PROPERTIES_FILE" "$RHQ_SERVER_HOME/bin/rhq-server.properties"
sed -e "s/@@HOSTNAME@@/`hostname`/g" -e "s/@@SHORT_HOSTNAME@@/`hostname -s`/g" "$RHQ_SERVER_HOME/bin/rhq-server.properties"

# Start the Server
$RHQ_SERVER_HOME/bin/rhq-server.sh start

