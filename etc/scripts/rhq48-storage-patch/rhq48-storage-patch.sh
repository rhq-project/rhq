#!/bin/bash

function usage() {
  echo "Usage: $0 <rhq-server-dir> <storage-ip-address> <cql-port> <jmx-port>"
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
CQL_PORT=$3
JMX_PORT=$4

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
export CQLSH_HOST=$CQLSH_HOST
export CQL_PORT=$CQL_PORT
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
