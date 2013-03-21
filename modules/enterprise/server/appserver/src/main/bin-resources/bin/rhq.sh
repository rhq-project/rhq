#!/bin/bash
#
# Note this script assumes GNU sed is installed. On Mac OS X a different
# implementation of sed is installed and will not work with this script. If you
# are on Mac OS X you can install GNU sed with the home brew package manager by
# running,
#
#    $ brew install gnu-sed
#    $ ln -s /usr/local/bin/gsed /usr/local/bin/sed
#    $ exec bash  # this reloads the shell for changes to take effect

function install() {
    #echo "install $@"
    echo "Installing RHQ Storage Node"
    #exec ./rhq-storage-installer.sh   
    sh ./rhq-storage-installer.sh --commitlog=../storage/commit_log --data=../storage/data --saved-caches=../storage/saved_caches

    echo "Installing RHQ Server"
    sh ./rhq-server.sh start
    wait_until_server_is_ready_to_run_installer
    sh ./rhq-installer.sh

    wait_until_rhq_is_initialized
    echo "Installing RHQ agent"
    install_agent
}

function wait_until_server_is_ready_to_run_installer() {
     echo "Testing to seeing if RHQ server is initialized"
    ./rhq-installer.sh --test
    while [ $? -ne 0 ]
    do
        echo "Server is not ready for installer to run"
        echo "Testing again to see if RHQ server is intialized"
        ./rhq-installer.sh --test
    done
    echo "RHQ server is initialized and ready for installer to run"
}

function wait_until_rhq_is_initialized() {
    delay=15
    server_log=../logs/server.log

    finished=$(grep -c "StartupBean.*Server started." $server_log)
    while [ $finished -ne 1 ]
    do
        echo "Waiting for RHQ server to initialize..."
        sleep $delay
        finished=$(grep -c "StartupBean.*Server started." $server_log)
    done
    echo "RHQ server is up"
}

function install_agent() {
    agent_installer_dir="../modules/org/rhq/rhq-enterprise-server-startup-subsystem/main/deployments/rhq.ear/rhq-downloads/rhq-agent"
    agent_installer="rhq-enterprise-agent-4.7.0-SNAPSHOT.jar"
    rhq_host=`hostname`

    cp "$agent_installer_dir/$agent_installer" "$RHQ_SERVER_HOME"/rhq-agent.jar
    cd $RHQ_SERVER_HOME
    java -jar rhq-agent.jar --install
 
    sed_cmd="s;<entry key=\"rhq\.agent\.server\.bind-port.*$;\0\n<entry key=\"rhq.agent.server.bind-address\" value=\"$rhq_host\" />;g"
    sed -i "$sed_cmd" rhq-agent/conf/agent-configuration.xml

    rm "$RHQ_SERVER_HOME/rhq-agent-update.log"
    rm "$RHQ_SERVER_HOME/rhq-agent.jar"

    cd $RHQ_SERVER_HOME/rhq-agent/bin
    ./rhq-agent-wrapper.sh start
}

function start() {
    echo "Starting RHQ storage node"
    cd $RHQ_SERVER_HOME/storage/bin
    ./cassandra -p cassandra.pid 2>/dev/null

    echo "Starting RHQ server"
    cd $RHQ_SERVER_HOME/bin
    ./rhq-server.sh start

    echo "Starting RHQ agent"
    cd $RHQ_SERVER_HOME/rhq-agent/bin
    ./rhq-agent-wrapper.sh start
}

function stop() {
    echo "Shutting down RHQ storage node"
    storage_pid=`cat $RHQ_SERVER_HOME/storage/bin/cassandra.pid`
    echo "RHQ storage node (pid=$storage_pid) is stopping..."
    kill $storage_pid

    echo "Shutting down RHQ server"
    $RHQ_SERVER_HOME/bin/rhq-server.sh stop

    echo "Shutting down RHQ agent"
    $RHQ_SERVER_HOME/rhq-agent/bin/rhq-agent-wrapper.sh stop
}

function usage() {
    echo "Usage: rhq.sh { install | start | stop }"
}

if [ -z "$RHQ_SERVER_HOME" ]; then
   _DOLLARZERO=`readlink $_READLINK_ARG "$0" 2>/dev/null || echo "$0"`
   RHQ_SERVER_HOME=`dirname "$_DOLLARZERO"`/..
else
   if [ ! -d "$RHQ_SERVER_HOME" ]; then
      echo "ERROR! RHQ_SERVER_HOME is not pointing to a valid directory"
      echo "RHQ_SERVER_HOME: $RHQ_SERVER_HOME"
      exit 1
   fi
fi

cd $RHQ_SERVER_HOME
RHQ_SERVER_HOME=`pwd`
cd bin

case $1 in
"install") shift
           install ${*:2}
           ;;

"start") shift
         start ${*:2}
         ;;

"stop") shift
        stop ${*:2}
        ;;

*) usage
   exit 1
esac
