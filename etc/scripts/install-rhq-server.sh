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
   abort "$@" "Usage:   $EXE RHQ_SERVER_DIST_URL RHQ_SERVER_PROPERTIES_URL RHQ_SERVER_INSTALL_PARENT_DIR" "Example: $EXE http://sourceforge.net/projects/rhq/files/rhq/rhq-4.0.1/rhq-server-4.0.1.zip/download rhq-server.properties /home/bob/Applications"
}

# Process command line args.
EXE=`basename $0`
if [ "$#" -ne 3 ]; then
   usage "Invalid number of arguments."
fi  
RHQ_SERVER_DIST_URL="$1"
RHQ_SERVER_PROPERTIES_URL="$2"
RHQ_SERVER_INSTALL_PARENT_DIR="$3"

echo "If you are upgrading or replacing existing RHQ Servers, make sure all of those Servers are stopped, then hit Enter to continue."
read

# Download the Server zipfile.
mkdir -p "$HOME/Downloads/tmp"
cd "$HOME/Downloads/tmp"
rm -rf *
wget --content-disposition --no-check-certificate --timestamping "$RHQ_SERVER_DOWNLOAD_URL"
RHQ_SERVER_DIST_FILE_NAME=`echo *`
mv $RHQ_SERVER_DIST_FILE_NAME ..

# Download the Server properties file.
if [ -f "$RHQ_SERVER_PROPERTIES_URL" ]; then
   RHQ_SERVER_PROPERTIES_FILE="$RHQ_SERVER_PROPERTIES_URL"
else
   mkdir -p "$HOME/Downloads/tmp"
   cd "$HOME/Downloads/tmp"
   rm -rf *
   wget --content-disposition --no-check-certificate --timestamping "$RHQ_SERVER_PROPERTIES_URL"
   RHQ_SERVER_PROPERTIES_FILE_NAME=`echo *`
   RHQ_SERVER_PROPERTIES_FILE="/tmp/$RHQ_SERVER_PROPERTIES_FILE_NAME"
fi

# Unzip the Server zipfile.
mkdir "$RHQ_SERVER_INSTALL_PARENT_DIR/tmp"
cd "$RHQ_SERVER_INSTALL_PARENT_DIR/tmp"
unzip -q "$HOME/Downloads/$RHQ_SERVER_DIST_FILE_NAME"
RHQ_SERVER_HOME_BASE_NAME=`echo *`
RHQ_SERVER_HOME="$RHQ_SERVER_INSTALL_PARENT_DIR/$RHQ_SERVER_HOME_BASE_NAME"
if [ -f "$RHQ_SERVER_HOME" ]; then
   # Backup existing Server installation.
   rm -rf "$RHQ_SERVER_HOME.bak"
   mv "$RHQ_SERVER_HOME" "$RHQ_SERVER_HOME.bak"
fi
mv $RHQ_SERVER_HOME_BASE_NAME ..

# Install the Server properties file.
mv -f "$RHQ_SERVER_PROPERTIES_FILE" "$RHQ_SERVER_HOME/bin/rhq-server.properties"
sed -e "s/@@HOSTNAME@@/`hostname`/g" -e "s/@@SHORT_HOSTNAME@@/`hostname -s`/g" "$RHQ_SERVER_HOME/bin/rhq-server.properties"

# Start the Server
$RHQ_SERVER_HOME/bin/rhq-server.sh start

