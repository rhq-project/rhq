@echo off

rem ===========================================================================
rem RHQ Storage Node (Cassandra) Windows 4.8.0 upgrade patch script
rem
rem WHO NEEDS TO RUN THE PATCH?
rem
rem   Run this patch script if you are:
rem     - Running RHQ 4.8.0 on Windows
rem     - Planning to upgrade and maintain your Storage node data
rem
rem PREQUISITES:
rem
rem   This patch requires the installation of Python.  Download Python 2.7.5 from:
rem
rem     http://www.python.org/download/releases/2.7.5/
rem
rem   Install as directed.  Note that Python will need to be on your PATH to run this patch. Also,
rem   this patch will not work with Python3.
rem
rem   Edit <rhq-install-dir>\rhq-storage\conf\cassandra.yaml to ensure the following is true:
rem
rem     start_rpc: true
rem
rem   Note the setting of rpc_port.  By default it is 9160, which is fine.
rem
rem RUNNING THE PATCH:
rem
rem   > cd <patch-dir>
rem   > rhq48-storage-patch.bat <rhq-480-server-dir> <storage-node-ip-address> <thrift-rpc-port> <jmx-port>
rem
rem   For example:
rem   > rhq48-storage-patch.bat c:\rhq-server-4.8.0 127.0.0.1 9160 7299
rem
rem   Review the output carefully. There should be no errors (be careful, the script may still have completed).
rem   If errors are encountered fix the issue and rerun the patch.
rem
rem   When done, you can again edit cassandra.yaml and reset start_rpc: false
rem
rem ===========================================================================

setlocal

if /i "%4" == "" (
   echo Usage: rhq48-storage-patch.bat ^<rhq-server-dir^> ^<storage-ip-address^> ^<thrift-rpc-port^> ^<jmx-port^>
   exit /B 1
)

set RHQ_SERVER_DIR=%1
set CQLSH_HOST=%2
set CQLSH_PORT=%3
set JMX_PORT=%4
set USERNAME="rhqadmin"
set PASSWORD="rhqadmin"

set PATCH="apache-cassandra-1.2.4-patch-1.jar"

rem swap out the Cassandra jar file with the patched version
echo Copying patch file to %RHQ_SERVER_DIR%\rhq-storage\lib
move %RHQ_SERVER_DIR%\rhq-storage\lib\apache-cassandra-1.2.4.jar .
copy %PATCH% %RHQ_SERVER_DIR%\rhq-storage\lib

rem restart the storage node
echo Starting RHQ Storage node
call %RHQ_SERVER_DIR%\bin\rhqctl.bat start --storage

rem sleep for a few seconds while Cassandra starts up
echo Waiting for RHQ Storage Node to start up..
rem Sleep is not implemented in all Windows prompts, this one won't work in Vista
choice /n /c y /d y /t 3

set RHQ_STORAGE_BIN=%RHQ_SERVER_DIR%\rhq-storage\bin
set CQLSH_PATH=%RHQ_STORAGE_BIN%\cqlsh
set NODETOOL_PATH=%RHQ_STORAGE_BIN%\nodetool.bat

rem run the CQL script
echo Running CQL script to disable table compression
python %CQLSH_PATH% -u %USERNAME% -p %PASSWORD% -f ./disable_compression.cql

rem rewrite all sstables
echo Rebuilding data files for system keyspace
call %NODETOOL_PATH% -u rhqadmin -pw rhqadmin -p %JMX_PORT% upgradesstables --include-all-sstables system

echo Rebuilding data files for system_traces keyspace
call %NODETOOL_PATH% -u rhqadmin -pw rhqadmin -p %JMX_PORT% upgradesstables --include-all-sstables system_traces

echo Rebuilding data files for system_auth keyspace
call %NODETOOL_PATH% -u rhqadmin -pw rhqadmin -p %JMX_PORT% upgradesstables --include-all-sstables system_auth

echo Rebuilding data files for rhq keyspace
call %NODETOOL_PATH% -u rhqadmin -pw rhqadmin -p %JMX_PORT% upgradesstables --include-all-sstables rhq

rem flush memtables and commit log to ensure no data loss prior to upgrade
call %NODETOOL_PATH% -u rhqadmin -pw rhqadmin -p %JMX_PORT% drain

echo Shutting down the RHQ Storage node
call %RHQ_SERVER_DIR%\bin\rhqctl.bat stop --storage

echo Removing patch file
del %RHQ_SERVER_DIR%\rhq-storage\lib\%PATCH%
move apache-cassandra-1.2.4.jar %RHQ_SERVER_DIR%\rhq-storage\lib

echo Table compression has been disabled for all keyspaces. You are now ready to upgrade your RHQ installation.
endlocal
