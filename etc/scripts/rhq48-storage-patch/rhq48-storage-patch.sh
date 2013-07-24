#!/bin/bash
#
# BACKGROUND:
# This patch script needs to be run against RHQ 4.8.0 installations prior to
# script. You do not need to run this script if upgrading from a version 
# earlier than 4.8.0.
#
# PREREQUISITES:
# 1) Shut down the RHQ storage node and server.
# 
# 2) Edit <rhq-install-dir>/rhq-storage/conf/cassandra.yaml and set the 
#    following property,
#
#      start_rpc: true
#
# 3) Note the value of rpc_port in cassandra.yaml. By default it is 9160 which
#    is fine.
#
# RUNNING THE PATCH:
# 1) cd <patch-dir>
#
# 2) ./rhq48-storage-patch.sh <rhq-480-server-dir> <storage-ip-address> <thrift-port> <jmx-port>
#
# 3) Carefully reivew the script output for any errors.
#
# 4) Edit cassandra.yaml against and reset start_rpc: false
#
# ADDITIONAL NOTES:
# The <jmx-port> defaults to 7299. If you are uncertain of what value to use,
# you can find it in the UI. Log into RHQ and go to Administration --> Storage Nodes.
#
# If you are uncertain of the value to use for the storage node IP address, you
# find the correct valu in the storage nodes admin UI as well.
# 
# EXAMPLE:
# ./rhq48-storage-patch.sh /opt/rhq-4.8.0 127.0.0.1 9160 7299
# Usage: ./rhq48-storage-patch.sh <rhq-480-server-dir> <storage-ip-address> <thrift-port> <jmx-port>

function usage() {
  echo "Usage: $0 <rhq-480-server-dir> <storage-ip-address> <thrift-port> <jmx-port>"
}

if [ $# -ne 4 ]; then
  usage
  exit 1
fi

if [[ "x$1" = "x"  ]] || [[ "x$2" = "x" ]] || [[ "x$3" = "x" ]] || [[ "x$4" = "x" ]]; then
  usage
  exit 1
fi

RHQ_SERVER_DIR=$1
CQL_HOSTNAME=$2
THRIFT_PORT=$3
JMX_PORT=$4

export CQLSH_HOST=$2
export CQLSH_PORT=$3

PATCH="apache-cassandra-1.2.4-patch-1.jar"

# swap out the Cassandra jar file with the patched version
echo "Copying patch file to $RHQ_SERVER_DIR/rhq-storage/lib"
mv $RHQ_SERVER_DIR/rhq-storage/lib/apache-cassandra-1.2.4.jar .
cp $PATCH $RHQ_SERVER_DIR/rhq-storage/lib

# restart the storage node
echo "Starting RHQ Storage node"
$RHQ_SERVER_DIR/bin/rhqctl start --storage

# sleep for a few seconds while Cassandra starts up
echo "Waiting for RHQ Storage Node to start up..."
sleep 3

# run the CQL script
echo "Running CQL script to disable table compression"
$RHQ_SERVER_DIR/rhq-storage/bin/cqlsh -u rhqadmin -p rhqadmin -f ./disable_compression.cql

# rewrite all sstables
echo "Rebuilding data files for system keyspace"
$RHQ_SERVER_DIR/rhq-storage/bin/nodetool -u rhqadmin -pw rhqadmin -p $JMX_PORT upgradesstables --include-all-sstables system

echo "Rebuilding data files for system_traces keyspace"
$RHQ_SERVER_DIR/rhq-storage/bin/nodetool -u rhqadmin -pw rhqadmin -p $JMX_PORT upgradesstables --include-all-sstables system_traces

echo "Rebuilding data files for system_auth keyspace"
$RHQ_SERVER_DIR/rhq-storage/bin/nodetool -u rhqadmin -pw rhqadmin -p $JMX_PORT upgradesstables --include-all-sstables system_auth

echo "Rebuilding data files for rhq keyspace"
$RHQ_SERVER_DIR/rhq-storage/bin/nodetool -u rhqadmin -pw rhqadmin -p $JMX_PORT upgradesstables --include-all-sstables rhq

# flush memtables and commit log to ensure no data loss prior to upgrade
$RHQ_SERVER_DIR/rhq-storage/bin/nodetool -u rhqadmin -pw rhqadmin -p $JMX_PORT drain

echo "Shutting down the RHQ Storage node"
$RHQ_SERVER_DIR/bin/rhqctl stop

echo "Removing patch file"
rm $RHQ_SERVER_DIR/rhq-storage/lib/$PATCH
mv ./apache-cassandra-1.2.4.jar $RHQ_SERVER_DIR/rhq-storage/lib

echo "Table compression has been disabled for all keyspaces. You are now ready to upgrade your RHQ installation."
