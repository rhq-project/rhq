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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

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
 * Discovery class for managed AS 7 instances.
 *
 * @author Heiko W. Rupp
 */
public class ManagedASDiscovery implements ResourceDiscoveryComponent

{

    static final String DJBOSS_SERVER_BASE_DIR = "-Djboss.server.base.dir=";
    static final String DORG_JBOSS_BOOT_LOG_FILE = "-Dorg.jboss.boot.log.file=";
    static final String DLOGGING_CONFIGURATION = "-Dlogging.configuration=";
    static final int DEFAULT_MGMT_PORT = 9990;
    private final Log log = LogFactory.getLog(this.getClass());
    private Document hostXml;
    private static final String DJBOSS_SERVER_HOME_DIR = "-Djboss.home.dir";

    /**
     * Run the auto-discovery
     */
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext discoveryContext) throws Exception {
        Set<DiscoveredResourceDetails> discoveredResources = new HashSet<DiscoveredResourceDetails>();


        List<ProcessScanResult> scans = discoveryContext.getAutoDiscoveredProcesses();

        for (ProcessScanResult psr : scans) {

            // get the HostController, as this is an indicator for managed AS
            String psName = psr.getProcessScan().getName();
            if (!psName.equals("HostController"))
                continue;

            // Now we have the host controller, lets get the host.xml file
            // and obtain the servers from there.
            ProcessInfo processInfo = psr.getProcessInfo();
            readHostXml(processInfo);
            String hostName = findHostName();
            int port = getManagementPortFromHostXml();


            List<ServerInfo> serverNames = getServersFromHostXml();
            for (ServerInfo serverInfo : serverNames) {

                Configuration config = discoveryContext.getDefaultPluginConfiguration();
                config.put(new PropertySimple("domainHost",hostName));
                config.put(new PropertySimple("group",serverInfo.group));
                config.put(new PropertySimple("port",port));

                // TODO this fails for the downed servers.
                // get from the domain or other place as soon as the domain provides it.
                initLogFile(scans, serverInfo.name, config, getHomeDirFromCommandLine(processInfo.getCommandLine()));

                DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
                    discoveryContext.getResourceType(), // ResourceType
                    hostName + "/" + serverInfo.name, // key
                    serverInfo.name,  // Name
                    null,  // TODO real version - get from Domain as soon as it is provided
                    "Managed AS 7 instance", // Description
                    config,
                    null
                );

                // Add to return values
                discoveredResources.add(detail);
                log.info("Discovered new ...  " + discoveryContext.getResourceType() + ", " + serverInfo);
            }
        }
        return discoveredResources;
    }

    /**
     * Loop through the Process scans for ManagedAS and if found extract the logfile path.
     * @param scans process scan results
     * @param name server name to look for
     * @param config config to put the info in
     * @param basePath
     */
    private void initLogFile(List<ProcessScanResult> scans, String name, Configuration config, String basePath) {

        for (ProcessScanResult psr : scans) {
            if (!psr.getProcessScan().getName().equals("ManagedAS"))
                continue;

            String[] commandLine = psr.getProcessInfo().getCommandLine();

            String logFile = basePath + File.separator +  getLogFileFromCommandLine(commandLine);

            if (logFile.contains(name))
                initLogEventSourcesConfigProp(logFile,config);
        }
    }

    private int getManagementPortFromHostXml() {
        Element host =  hostXml.getDocumentElement();
        NodeList interfaceParent = host.getElementsByTagName("management-interfaces");
        if (interfaceParent ==null || interfaceParent.getLength()==0) {
            log.warn("No <management-interfaces> found in host.xml");
            return DEFAULT_MGMT_PORT;
        }
        NodeList mgmtInterfaces = interfaceParent.item(0).getChildNodes();
        if (mgmtInterfaces==null || mgmtInterfaces.getLength()==0) {
            log.warn("No <*-interface> found in host.xml");
            return DEFAULT_MGMT_PORT;
        }
        for (int i = 0 ; i < mgmtInterfaces.getLength(); i++) {
            if (!(mgmtInterfaces.item(i) instanceof Element))
                continue;

            Element mgmtInterface = (Element) mgmtInterfaces.item(i);
            if (mgmtInterface.getNodeName().equals("http-interface")) {
                String tmp = mgmtInterface.getAttribute("port");
                int port = Integer.valueOf(tmp);
                return port;
            }
        }
        return DEFAULT_MGMT_PORT;
    }

    private List<ServerInfo> getServersFromHostXml() {

        Element host = hostXml.getDocumentElement();
        NodeList serversElement = host.getElementsByTagName("servers");
        if (serversElement ==null || serversElement.getLength()==0) {
            log.warn("No <servers> found in host.xml");
            return Collections.emptyList();
        }
        NodeList servers = serversElement.item(0).getChildNodes();
        if (servers==null || servers.getLength()==0) {
            log.warn("No <server> found in host.xml");
            return Collections.emptyList();
        }
        List<ServerInfo> result = new ArrayList<ServerInfo>();
        for (int i = 0 ; i < servers.getLength(); i++) {
            if (!(servers.item(i) instanceof Element))
                continue;

            ServerInfo info = new ServerInfo();
            Element server = (Element) servers.item(i);
            info.name = server.getAttribute("name");
            info.group = server.getAttribute("group");
            String autoStart = server.getAttribute("autoStart");
            if (autoStart==null || autoStart.isEmpty())
                autoStart = "false";
            info.autoStart = Boolean.getBoolean(autoStart);

            result.add(info);
        }

        return result;
    }


    private String findHostName() {
        String hostName = hostXml.getDocumentElement().getAttribute("name");
        return hostName;
    }

    private void readHostXml(ProcessInfo processInfo) {
        String hostXmlFile = getHostXmlFileLocation(processInfo);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputStream is = new FileInputStream(hostXmlFile);
            hostXml = builder.parse(is);
            is.close();
        } catch (Exception e) {
            e.printStackTrace();  // TODO: Customise this generated block
        }
    }

    /**
     * Get the location of the host definition file (host.xml in domain mode, standalone.xml
     * in standalone mode.
     *
     * @param processInfo ProcessInfo structure containing the ENV variables
     * @return The path to the definition file.
     */
    private String getHostXmlFileLocation(ProcessInfo processInfo) {

        String home = getHomeDirFromCommandLine(processInfo.getCommandLine());
        StringBuilder builder = new StringBuilder(home);
        builder.append("/domain");
        builder.append("/configuration");
        builder.append("/host.xml");
        return builder.toString();

    }



    String getHomeDirFromCommandLine(String[] commandLine) {
            for (String line: commandLine) {
                if (line.startsWith(DJBOSS_SERVER_HOME_DIR))
                    return line.substring(DJBOSS_SERVER_HOME_DIR.length()+1);
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

    private class ServerInfo {
        String name;
        String group;
        boolean autoStart;


    }

}
