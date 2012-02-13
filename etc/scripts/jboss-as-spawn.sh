#!/bin/sh

######### begin config ##########

# the number of instances to start
INSTANCES=5

# the JBoss AS home directory
JBOSS_HOME=${HOME}/Applications/jboss-eap-5.1.2/jboss-as

# the name of the config to be cloned
JBOSS_CONFIG=production

# options to pass to the JBoss AS JVM
JAVA_OPTS="-Xms200M -Xmx400M -XX:MaxPermSize=150M -Dorg.jboss.resolver.warning=true -Dsun.rmi.dgc.client.gcInterval=3600000 -Dsun.rmi.dgc.server.gcInterval=3600000 -Dsun.lang.ClassLoader.allowArraySyntax=true"

########## end config ###########

echo "JBOSS_HOME=${JBOSS_HOME}"
echo "JAVA_OPTS=${JAVA_OPTS}"

# export these, since they are used by the forked run.sh processes.
export JBOSS_HOME JAVA_OPTS

cd "${JBOSS_HOME}"
for ((  i = 1 ;  i <= ${INSTANCES};  i++  )); do
  JBOSS_CONFIG_COPY="${JBOSS_CONFIG}${i}"
  if [ ! -d "server/${JBOSS_CONFIG_COPY}" ]; then
    echo "Creating new config dir '${JBOSS_HOME}/server/${JBOSS_CONFIG_COPY}'..."
    cp -pr server/${JBOSS_CONFIG} server/${JBOSS_CONFIG_COPY}
    ( cd server/${JBOSS_CONFIG_COPY} ; rm -rf data log tmp work )
  fi
  BIND_ADDRESS="127.0.0.$(($i + 1))"
  echo "Starting instance with config '${JBOSS_CONFIG_COPY}' and bind address '${BIND_ADDRESS}'..."
  nohup $JBOSS_HOME/bin/run.sh -c ${JBOSS_CONFIG_COPY} -b ${BIND_ADDRESS} >$JBOSS_HOME/bin/${JBOSS_CONFIG_COPY}.out 2>&1 &
done

