#!/bin/bash

function install() {
    #echo "install $@"
    echo "Installing RHQ Storage Node"
    #exec ./rhq-storage-installer.sh   
    sh ./rhq-storage-installer.sh --commitlog=../storage/commit_log --data=../storage/data --saved-caches=../storage/saved_caches

    echo "Installing RHQ Server"
    sh ./rhq-server.sh start
    wait_for_server_to_init
    sh ./rhq-installer.sh
}

function wait_for_server_to_init() {
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

function start() {
    echo "start $@"
}

function stop() {
    echo stop $@
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
