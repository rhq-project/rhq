#!/bin/sh

# set this to the number of instances you want to start
INSTANCES=20
# the JBoss configuration name
CONFIG_NAME=all

#export JAVA_HOME=${HOME}/jdk1.5.0_15
export JBOSS_HOME=${HOME}/jboss-4.0.5.GA

for ((  i = 1 ;  i <= ${INSTANCES};  i++  )); do
  if [ ! -d ${JBOSS_HOME}/server/config${i} ]; then
    echo "Creating new config dir '${JBOSS_HOME}/server/config${i}'..."
    cp -pr ${JBOSS_HOME}/server/${CONFIG_NAME} ${JBOSS_HOME}/server/config${i}
  fi
  echo "Starting instance #${i} bound to 127.0.0.${i}..."
  nohup ./run.sh -c config${i} -b 127.0.0.${i} >config${i}.log 2>&1 &
done
