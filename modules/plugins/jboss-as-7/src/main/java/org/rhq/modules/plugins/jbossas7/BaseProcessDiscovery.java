package org.rhq.modules.plugins.jbossas7;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.support.metadata.LocalVMTypeDescriptor;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.event.log.LogFileEventResourceComponentHelper;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.jmx.JMXDiscoveryComponent;


/**
 * Discovery class
 */
public class BaseProcessDiscovery implements ResourceDiscoveryComponent

{

    static final String DJBOSS_SERVER_BASE_DIR = "-Djboss.server.base.dir=";
    static final String DORG_JBOSS_BOOT_LOG_FILE = "-Dorg.jboss.boot.log.file=";
    private final Log log = LogFactory.getLog(this.getClass());

    /**
     * Run the auto-discovery
     */
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext discoveryContext) throws Exception {
        Set<DiscoveredResourceDetails> discoveredResources = new HashSet<DiscoveredResourceDetails>();


        List<ProcessScanResult> scans = discoveryContext.getAutoDiscoveredProcesses();

        for (ProcessScanResult psr : scans) {

            Configuration config = discoveryContext.getDefaultPluginConfiguration();
            // IF SE, then look at domain/configuration/host.xml <management interface="default" port="9990
            // for management port
            String[] commandLine = psr.getProcessInfo().getCommandLine();
            String serverNameFull;
            String serverName;
            String psName = psr.getProcessScan().getName();
            if (psName.equals("ProcessManager")) {
                serverNameFull = "ProcessManager";
                serverName = "ProcessManager";
            } else if (psName.equals("ASManager")) {
                serverName = "ASManager";
                serverNameFull = "ASManager";
            } else if (psName.equals("HostController")) {
                serverName = "HostController";
                serverNameFull = "HostController";
            } else {
                serverNameFull = getBaseDirFromCommandLine(commandLine);
                if (serverNameFull==null || serverNameFull.isEmpty()) {
                    // Try to obtain the server name
                    //  -Dorg.jboss.boot.log.file=domain/servers/server-one/log/boot.log
                    // This is a hack until I know a better way to do so.
                    String tmp = getLogFileFromCommandLine(commandLine);
                    int i = tmp.indexOf("servers/");
                    tmp = tmp.substring( i + 8);
                    tmp = tmp.substring(0,tmp.indexOf("/"));
                    serverNameFull = tmp;
                }
                serverName = serverNameFull.substring(serverNameFull.lastIndexOf("/")+1);
                if (serverName.isEmpty())
                    serverName = serverNameFull;

            }
            String logFile = getLogFileFromCommandLine(commandLine);
            initLogEventSourcesConfigProp(logFile,config);
            String javaClazz = psr.getProcessInfo().getName();


                /*
                 * We'll connect to the discovered VM on the local host, so set the jmx connection
                 * properties accordingly. This may only work on JDK6+, but then JDK5 is deprecated
                 * anyway.
                 */
//                config.put(new PropertySimple(JMXDiscoveryComponent.COMMAND_LINE_CONFIG_PROPERTY,
//                        javaClazz));
                config.put(new PropertySimple(JMXDiscoveryComponent.CONNECTION_TYPE,
                        LocalVMTypeDescriptor.class.getName()));

                // TODO vmid will change when the detected server is bounced - how do we follow this?
                config.put(new PropertySimple(JMXDiscoveryComponent.VMID_CONFIG_PROPERTY,psr.getProcessInfo().getPid()));


            DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
                discoveryContext.getResourceType(), // ResourceType
                serverNameFull, // key TODO distinguish per domain?
                serverName,  // Name
                null,  // TODO real version ?
                "TODO", // Description
                config,
                psr.getProcessInfo()
            );


            // Add to return values
            discoveredResources.add(detail);
            log.info("Discovered new ...  " + discoveryContext.getResourceType() + ", " + serverNameFull);
        }

        return discoveredResources;

        }

        String getBaseDirFromCommandLine(String[] commandLine) {
            for (String line: commandLine) {
                if (line.startsWith(DJBOSS_SERVER_BASE_DIR))
                    return line.substring(DJBOSS_SERVER_BASE_DIR.length());
            }
            return "";
        }

//-Dorg.jboss.boot.log.file=/devel/jbas7/jboss-as/build/target/jboss-7.0.0.Alpha2/domain/log/server-manager/boot.log
//-Dlogging.configuration=file:/devel/jbas7/jboss-as/build/target/jboss-7.0.0.Alpha2/domain/configuration/logging.properties

    String getLogFileFromCommandLine(String[] commandLine) {

        for (String line: commandLine) {
            if (line.startsWith(DORG_JBOSS_BOOT_LOG_FILE))
                return line.substring(DORG_JBOSS_BOOT_LOG_FILE.length());
        }
        return "";
    }

    private void initLogEventSourcesConfigProp(String fileName, Configuration pluginConfiguration) {

        PropertyList logEventSources = pluginConfiguration
            .getList(LogFileEventResourceComponentHelper.LOG_EVENT_SOURCES_CONFIG_PROP);

        if (logEventSources==null)
            return;

        File serverLogFile = new File(fileName);

        if (serverLogFile.exists() && !serverLogFile.isDirectory()) {
            PropertyMap serverLogEventSource = new PropertyMap("logEventSource");
            serverLogEventSource.put(new PropertySimple(
                LogFileEventResourceComponentHelper.LogEventSourcePropertyNames.LOG_FILE_PATH, serverLogFile));
            serverLogEventSource.put(new PropertySimple(
                LogFileEventResourceComponentHelper.LogEventSourcePropertyNames.ENABLED, Boolean.FALSE));
            logEventSources.add(serverLogEventSource);
        }
    }

}
