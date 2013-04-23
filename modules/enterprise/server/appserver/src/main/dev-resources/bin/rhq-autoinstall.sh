if [ -z ${RHQ_SERVER_HOME} ]; then
   echo RHQ_SERVER_HOME not defined
   exit 1
fi

cd "$RHQ_SERVER_HOME"
RHQ_SERVER_HOME=`pwd`

_INSTALLER_SCRIPT="${RHQ_SERVER_HOME}/bin/rhq-installer.sh"
_CCM_SCRIPT="${RHQ_SERVER_HOME}/bin/rhq-ccm.sh"
_JBOSS_CLI_SCRIPT="${RHQ_SERVER_HOME}/jbossas/bin/jboss-cli.sh"

# we are normally executed just before the server starts, so give it time to initialize
sleep 5

echo Installing embedded Cassandra cluster...
eval ${_CCM_SCRIPT} deploy

if [ ! -e "${RHQ_SERVER_HOME}/bin/.propSet" ]; then
   echo Updating the rhq.cassandra.seeds property...
   #jboss-cli needs to have clear JAVA_OPTS
   JAVA_OPTS_BACKUP=$JAVA_OPTS
   export JAVA_OPTS=""
   SEEDS=`cat ${RHQ_SERVER_HOME}/bin/rhq-server.properties | sed -ne 's/,/;/g' -e 's/rhq.cassandra.seeds=//gp'`
   ${_JBOSS_CLI_SCRIPT} --connect controller=localhost:6999 /system-property=rhq.cassandra.seeds/:add\(value="$SEEDS"\)
   export JAVA_OPTS=$JAVA_OPTS_BACKUP
   touch "${RHQ_SERVER_HOME}/bin/.propSet"
fi

_TRIES="1 2"
for _TRY in $_TRIES
do
   echo Running Installer Now...
   eval ${_INSTALLER_SCRIPT}
   if [ "$?" -eq "0" ]; then
      echo Installer finished
      # most times, the installation was already done, but see if changes to rhq-server.properties requires a reconfiguration
      eval ${_INSTALLER_SCRIPT} --reconfig
      break;
   elif [ "$?" -eq "1" ]; then
      echo The installer has been disabled - please fix rhq-server.properties
      break;
   else
      echo Installer exited with failure code "$?" - will try again in a few seconds
      sleep 5
   fi
done
