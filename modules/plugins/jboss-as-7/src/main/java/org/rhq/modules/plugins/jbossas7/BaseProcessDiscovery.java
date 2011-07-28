/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.modules.plugins.jbossas7;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.event.log.LogFileEventResourceComponentHelper;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.system.ProcessInfo;

/**
 * Discovery class
 */
public class BaseProcessDiscovery extends AbstractBaseDiscovery implements ResourceDiscoveryComponent

{

    static final String DJBOSS_HOME_DIR = "-Djboss.home.dir=";
    static final String DORG_JBOSS_BOOT_LOG_FILE = "-Dorg.jboss.boot.log.file=";
    static final String DLOGGING_CONFIGURATION = "-Dlogging.configuration=";
    static final String DASH_DASH_SERVER_CONFIG = "--server-config=";
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
            String description = discoveryContext.getResourceType().getDescription();
            String version = null;

            if (psName.equals("HostController")) {
                readStandaloneOrHostXml(psr.getProcessInfo(), true);
                HostPort hp = getDomainControllerFromHostXml();
                if (hp.isLocal) {
                    serverName = "DomainController";
                    serverNameFull = "DomainController";
                    description = "Domain controller for an AS7 domain";
                } else {
                    serverName = "HostController";
                    serverNameFull = "HostController";
                }
                String homeDir = getHomeDirFromCommandLine(psr.getProcessInfo().getCommandLine());
                config.put(new PropertySimple("baseDir", homeDir));
                version = homeDir.substring(homeDir.lastIndexOf("-") + 1);
                config.put(new PropertySimple("startScript", "bin/domain.sh"));
                String host = findHost(psr.getProcessInfo(), true);
                config.put(new PropertySimple("domainHost", host));

                fillUserPassFromFile(config, "domain", homeDir);

                // TODO provide running config

            } else { // Standalone server
                serverNameFull = getHomeDirFromCommandLine(commandLine);
                readStandaloneOrHostXml(psr.getProcessInfo(), false);
                if (serverNameFull == null || serverNameFull.isEmpty()) {
                    // Try to obtain the server name
                    //  -Dorg.jboss.boot.log.file=domain/servers/server-one/log/boot.log
                    // This is a hack until I know a better way to do so.
                    String tmp = getLogFileFromCommandLine(commandLine);
                    int i = tmp.indexOf("servers/");
                    tmp = tmp.substring(i + 8);
                    tmp = tmp.substring(0, tmp.indexOf("/"));
                    serverNameFull = tmp;

                }
                String host = findHost(psr.getProcessInfo(), true);
                config.put(new PropertySimple("domainHost", host));

                config.put(new PropertySimple("baseDir", serverNameFull));

                serverName = findHostName();
                if (serverName.isEmpty())
                    serverName = serverNameFull;

                version = serverName.substring(serverName.lastIndexOf("-") + 1);

                String serverConfig = getServerConfigFromCommandLine(commandLine);
                config.put(new PropertySimple("config", serverConfig));
                config.put(new PropertySimple("startScript", "bin/standalone.sh"));

                fillUserPassFromFile(config, "standalone", serverNameFull);

            }

            //            String logFile = getLogFileFromCommandLine(commandLine);
            //monitor the server.log instead of the boot.log
            String bootLogFile = getLogFileFromCommandLine(commandLine);
            String logFile = bootLogFile.substring(0, bootLogFile.lastIndexOf("/")) + File.separator + "server.log";
            initLogEventSourcesConfigProp(logFile, config);

            HostPort managmentPort = getManagementPortFromHostXml();
            config.put(new PropertySimple("hostname", managmentPort.host));
            config.put(new PropertySimple("port", managmentPort.port));
            //            String javaClazz = psr.getProcessInfo().getName();

            /*
             * We'll connect to the discovered VM on the local host, so set the jmx connection
             * properties accordingly. This may only work on JDK6+, but then JDK5 is deprecated
             * anyway.
             */
            //                config.put(new PropertySimple(JMXDiscoveryComponent.COMMAND_LINE_CONFIG_PROPERTY,
            //                        javaClazz));
            //                config.put(new PropertySimple(JMXDiscoveryComponent.CONNECTION_TYPE,
            //                        LocalVMTypeDescriptor.class.getName()));
            //
            //                // TODO vmid will change when the detected server is bounced - how do we follow this?
            //                config.put(new PropertySimple(JMXDiscoveryComponent.VMID_CONFIG_PROPERTY,psr.getProcessInfo().getPid()));

            DiscoveredResourceDetails detail = new DiscoveredResourceDetails(discoveryContext.getResourceType(), // ResourceType
                serverNameFull, // key TODO distinguish per domain?
                serverName, // Name
                version, // TODO get via API ?�
                description, // Description
                config, psr.getProcessInfo());

            // Add to return values
            discoveredResources.add(detail);
            log.info("Discovered new ...  " + discoveryContext.getResourceType() + ", " + serverNameFull);
        }

        return discoveredResources;

    }

    private void fillUserPassFromFile(Configuration config, String mode, String baseDir) {

        String configDir = baseDir + File.separator + mode + File.separator + "configuration";

        File file = new File(configDir, "mgmt-users.properties");
        if (!file.exists() || !file.canRead()) {
            if (log.isDebugEnabled())
                log.debug("No console user properties file found at [" + file.getAbsolutePath()
                    + "] or file is not readable");
            return;
        }
        BufferedReader br = null;
        try {
            FileReader fileReader = new FileReader(file);
            br = new BufferedReader(fileReader);
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("#"))
                    continue;
                if (line.isEmpty())
                    continue;
                if (!line.contains("="))
                    continue;
                // found a candidate
                String user = line.substring(0, line.indexOf("="));
                String pass = line.substring(line.indexOf("=") + 1);
                config.put(new PropertySimple("user", user));
                config.put(new PropertySimple("password", pass));

            }
        } catch (IOException e) {
            e.printStackTrace(); // TODO: Customise this generated block
        } finally {
            if (br != null)
                try {
                    br.close();
                } catch (IOException e) {
                    // empty
                }
        }

    }

    private String findHost(ProcessInfo processInfo, boolean isDomain) {
        String hostXmlFile = getHostXmlFileLocation(processInfo, isDomain);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        String hostName = null;
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputStream is = new FileInputStream(hostXmlFile);
            Document document = builder.parse(is); // TODO keep this around
            hostName = document.getDocumentElement().getAttribute("name");
            is.close();
        } catch (Exception e) {
            e.printStackTrace(); // TODO: Customise this generated block
        }
        if (hostName == null)
            hostName = "local"; // Fallback to the installation default
        return hostName;
    }

    String getServerConfigFromCommandLine(String[] commandLine) {
        for (String line : commandLine) {
            if (line.startsWith(DASH_DASH_SERVER_CONFIG))
                return line.substring(DASH_DASH_SERVER_CONFIG.length());
        }
        return "standalone.xml";
    }

    //-Dorg.jboss.boot.log.file=/devel/jbas7/jboss-as/build/target/jboss-7.0.0.Alpha2/domain/log/server-manager/boot.log
    //-Dlogging.configuration=file:/devel/jbas7/jboss-as/build/target/jboss-7.0.0.Alpha2/domain/configuration/logging.properties

    String getLogFileFromCommandLine(String[] commandLine) {

        for (String line : commandLine) {
            if (line.startsWith(DORG_JBOSS_BOOT_LOG_FILE))
                return line.substring(DORG_JBOSS_BOOT_LOG_FILE.length());
        }
        return "";
    }

    private void initLogEventSourcesConfigProp(String fileName, Configuration pluginConfiguration) {

        PropertyList logEventSources = pluginConfiguration
            .getList(LogFileEventResourceComponentHelper.LOG_EVENT_SOURCES_CONFIG_PROP);

        if (logEventSources == null)
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
