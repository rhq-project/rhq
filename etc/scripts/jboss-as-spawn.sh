#!/bin/sh

######### begin config ##########

# the JBoss AS home directory
#JBOSS_HOME=${HOME}/Applications/jboss-as-7.1.1.Final

# the name of the config to be cloned 
# (e.g. for AS7, "standalone" or "domain"; for other AS versions, "default" or "all")
#JBOSS_CONFIG=standalone

# the number of instances to start
#JBOSS_INSTANCES=1

# options to pass to the JBoss AS JVM
#JAVA_OPTS="-Xms200M -Xmx400M -XX:MaxPermSize=150M -Dorg.jboss.resolver.warning=true -Dsun.rmi.dgc.client.gcInterval=3600000 -Dsun.rmi.dgc.server.gcInterval=3600000 -Dsun.lang.ClassLoader.allowArraySyntax=true -Djava.net.preferIPv4Stack=true"

########## end config ###########

if [ -z "${JBOSS_HOME}" ]; then
    echo "The 'JBOSS_HOME' variable must be set to the JBoss install dir you want to spawn from." >&2
    exit 1
fi

if [ -z "${JBOSS_CONFIG}" ]; then
    echo "The 'JBOSS_CONFIG' variable must be set to the name of the JBoss config you want to spawn from." >&2
    exit 1
fi

if [ -z "${JBOSS_INSTANCES}" ]; then
    echo "The 'JBOSS_INSTANCES' variable must be set to the number of JBoss instances you want to spawn." >&2
    exit 1
fi

if [ -z "${JAVA_OPTS}" ]; then
    JAVA_OPTS="-Xms200M -Xmx400M -XX:MaxPermSize=150M -Dorg.jboss.resolver.warning=true -Dsun.rmi.dgc.client.gcInterval=3600000 -Dsun.rmi.dgc.server.gcInterval=3600000 -Dsun.lang.ClassLoader.allowArraySyntax=true -Djava.net.preferIPv4Stack=true"
fi

echo "JBOSS_HOME=${JBOSS_HOME}"
echo "JAVA_OPTS=${JAVA_OPTS}"

# export these, since they are used by the forked run.sh processes.
export JBOSS_HOME JAVA_OPTS

if [ -f "${JBOSS_HOME}/jboss-modules.jar" ]; then
   AS7=1
   SERVER_BASE_DIR="${JBOSS_HOME}"
else 
   AS7=
   SERVER_BASE_DIR="${JBOSS_HOME}/server"
   OCTET=1
fi

JBOSS_CONFIG_DIR="${SERVER_BASE_DIR}/${JBOSS_CONFIG}"
if [ ! -d "${JBOSS_CONFIG_DIR}" ]; then
   echo "Invalid config name '${JBOSS_CONFIG}' - ${JBOSS_CONFIG_DIR} does not exist." >&2
   exit 1
fi

if [ -n "${AS7}" ]; then
 if [ -f "${JBOSS_CONFIG_DIR}/configuration/standalone.xml" ]; then
   AS7_STANDALONE=1
   CONFIG_FILE="${JBOSS_CONFIG_DIR}/configuration/standalone.xml"
   OCTET=2
 elif [ -f "${JBOSS_CONFIG_DIR}/configuration/host.xml" ]; then
   AS7_STANDALONE=
   CONFIG_FILE="${JBOSS_CONFIG_DIR}/configuration/host.xml"
   OCTET=3
 else
   echo "AS7 config file not found." >&2
   exit 1
 fi
fi

for ((  i = 1 ;  i <= ${INSTANCES};  i++  )); do
  CONFIG_DIR_NAME="${JBOSS_CONFIG}${i}"
  CONFIG_DIR="${SERVER_BASE_DIR}/${CONFIG_DIR_NAME}"
  if [ ! -d "${CONFIG_DIR}" ]; then
    echo "Creating new config dir '${CONFIG_DIR}'..."
    cp -pr "${SERVER_BASE_DIR}/${JBOSS_CONFIG}" "${CONFIG_DIR}"
    ( cd "${CONFIG_DIR}" ; rm -rf data log tmp work )
    if [ -n "${AS7}" ]; then
        sed -in '{s/<inet-address /<loopback-address /}' "${CONFIG_FILE}"
    fi 
  fi
  BIND_ADDRESS="127.${OCTET}.0.${i}"
    
  if wget -q -P /tmp http://${BIND_ADDRESS}:8080/; then
      echo "Not starting AS instance with config '${CONFIG_DIR_NAME}' and bind address '${BIND_ADDRESS}', since an AS instance is already bound to ${BIND_ADDRESS}."
      continue
  fi

  if [ -n "${AS7}" ]; then
      if [ -n "${AS7_STANDALONE}" ]; then
          echo "Starting AS7 standalone instance with config '${CONFIG_DIR_NAME}' and bind address '${BIND_ADDRESS}'..."
          nohup $JBOSS_HOME/bin/standalone.sh -Djboss.server.base.dir=${CONFIG_DIR} -b=${BIND_ADDRESS} -bmanagement=${BIND_ADDRESS} -bunsecure=${BIND_ADDRESS} >${JBOSS_HOME}/bin/${CONFIG_DIR_NAME}.out 2>&1 &
      else 
          echo "Starting AS7 host controller instance with config '${CONFIG_DIR_NAME}' and bind address '${BIND_ADDRESS}'..."
          nohup $JBOSS_HOME/bin/domain.sh -Djboss.domain.base.dir=${CONFIG_DIR} -b=${BIND_ADDRESS} -bmanagement=${BIND_ADDRESS} -bunsecure=${BIND_ADDRESS} --pc-address=${BIND_ADDRESS} >${JBOSS_HOME}/bin/${CONFIG_DIR_NAME}.out 2>&1 &
      fi               
  else
      echo "Starting AS instance with config '${CONFIG_DIR_NAME}' and bind address '${BIND_ADDRESS}'..."
      nohup $JBOSS_HOME/bin/run.sh -c ${CONFIG_DIR} -b ${BIND_ADDRESS} >$JBOSS_HOME/bin/${CONFIG_DIR_NAME}.out 2>&1 &
  fi
done

