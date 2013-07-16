@echo off
setlocal

if /i "%4" == "" (
   echo Usage: rhq48-storage-patch.bat ^<rhq-server-dir^> ^<storage-ip-address^> ^<cql-port^> ^<jmx-port^>
   exit /B 1
)

set RHQ_SERVER_DIR=%1
set CQL_HOSTNAME=%2
set CQL_PORT=%3
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

set RHQ_STORAGE_BIN=%RHQ_SERVER_DIR%\rhq-storage\bin\
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
