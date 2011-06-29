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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.event.log.LogFileEventResourceComponentHelper;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.system.ProcessInfo;
import org.rhq.modules.plugins.jbossas7.json.ComplexResult;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.PROPERTY_VALUE;
import org.rhq.modules.plugins.jbossas7.json.ReadResource;

/**
 * Discovery class for managed AS 7 instances.
 *
 * @author Heiko W. Rupp
 */
public class ManagedASDiscovery extends AbstractBaseDiscovery

{

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
            readHostXml(processInfo,true);
            String hostName = findHostName();
            HostPort managementHostPort = getManagementPortFromHostXml();


            List<ServerInfo> serverNames = getServersFromHostXml();
            for (ServerInfo serverInfo : serverNames) {

                Configuration config = discoveryContext.getDefaultPluginConfiguration();
                config.put(new PropertySimple("domainHost",hostName));
                config.put(new PropertySimple("group",serverInfo.group));
                config.put(new PropertySimple("port",managementHostPort.port));
                config.put(new PropertySimple("hostname",managementHostPort.host));
                if (serverInfo.bindingGroup!=null) {
                    config.put(new PropertySimple("socket-binding-group",serverInfo.bindingGroup));
                    config.put(new PropertySimple("socket-binding-port-offset",serverInfo.portOffset));
                }
                else {
                    HostPort dcHP = getDomainControllerFromHostXml();
                    if (dcHP.port == 9999)
                        dcHP.port = 9990;  // TODO Hack until JBAS-9306 is solved

                    ServerInfo dcInfo = getBindingsFromDC(dcHP, serverInfo.group);
                    config.put(new PropertySimple("socket-binding-group", dcInfo.bindingGroup));
                    config.put(new PropertySimple("socket-binding-port-offset",dcInfo.portOffset));
                }
                config.put(new PropertySimple("socket-binding-port-offset",serverInfo.portOffset));

                String path = "host=" + hostName + ",server-config=" + serverInfo.name;
                config.put(new PropertySimple("path",path));



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

    private ServerInfo getBindingsFromDC(HostPort domainController, String serverGroup) {
        ASConnection dcConnection = new ASConnection(domainController.host,domainController.port);
        List<PROPERTY_VALUE> address = new ArrayList<PROPERTY_VALUE>();
        address.add(new PROPERTY_VALUE("server-group",serverGroup));
        Operation op = new ReadResource(address);
        ComplexResult res = (ComplexResult) dcConnection.execute(op, true);
        if (res.isSuccess()) {
            if (res.getResult().containsKey("socket-binding-group")) {
                String sbg = (String) res.getResult().get("socket-binding-group");

                ServerInfo serverInfo = new ServerInfo();
                serverInfo.bindingGroup = sbg;
                return serverInfo;
            }
        }

        return new ServerInfo();
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

            // Look for  <socket-binding-group ref="standard-sockets" port-offset="250"/>
            NodeList sbgs = server.getChildNodes();
            if (sbgs!=null) {
                for (int j = 0 ; j < sbgs.getLength(); j++) {
                    if (!(sbgs.item(j) instanceof Element))
                        continue;

                    Element sbg = (Element) sbgs.item(j);
                    if (!sbg.getNodeName().equals("socket-binding-group"))
                        continue;

                    info.bindingGroup = sbg.getAttribute("ref");
                    String portOffset = sbg.getAttribute("port-offset");
                    if (portOffset!=null && !portOffset.isEmpty())
                        info.portOffset = Integer.parseInt(portOffset);

                }
            }
            result.add(info);
        }

        return result;
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

    private static class ServerInfo {
        String name;
        String group;
        boolean autoStart;
        int portOffset;
        String bindingGroup;

        @Override
        public String toString() {
            return "ServerInfo{" +
                "name='" + name + '\'' +
                ", group='" + group + '\'' +
                '}';
        }
    }

}
