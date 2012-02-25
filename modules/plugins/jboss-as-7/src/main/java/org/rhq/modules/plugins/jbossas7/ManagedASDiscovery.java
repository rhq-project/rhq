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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.event.log.LogFileEventResourceComponentHelper;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.ComplexResult;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.ReadAttribute;
import org.rhq.modules.plugins.jbossas7.json.ReadChildrenNames;
import org.rhq.modules.plugins.jbossas7.json.ReadResource;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * Discovery class for managed AS 7 instances.
 *
 * @author Heiko W. Rupp
 */
public class ManagedASDiscovery extends AbstractBaseDiscovery implements ResourceDiscoveryComponent

{

    private HostControllerComponent parentComponent;

    /**
     * Run the auto-discovery
     */
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext discoveryContext) throws Exception {
        Set<DiscoveredResourceDetails> discoveredResources = new HashSet<DiscoveredResourceDetails>();

        parentComponent = (HostControllerComponent) discoveryContext.getParentResourceComponent();
        Configuration hcConfig = parentComponent.getHCConfig();
        String hostName = hcConfig.getSimpleValue("domainHost", "master"); // TODO good default?

        HostInfo hostInfo = getHostInfo(hostName);
        if (hostInfo == null)
            return discoveredResources;

        try {
            // get the HostController, as this is an indicator for managed AS

            // Now we have the host controller, lets get the host.xml file
            // and obtain the servers from there.

            List<ServerInfo> serverNames;
            serverNames = getManagedServers(hostName);
            for (ServerInfo serverInfo : serverNames) {

                Configuration config = discoveryContext.getDefaultPluginConfiguration();
                config.put(new PropertySimple("domainHost", hostName));
                config.put(new PropertySimple("group", serverInfo.group));
                if (serverInfo.bindingGroup != null) {
                    config.put(new PropertySimple("socket-binding-group", serverInfo.bindingGroup));
                } else {
                    String group = resolveSocketBindingGroup(serverInfo.group);
                    config.put(new PropertySimple("socket-binding-group", group));

                }
                config.put(new PropertySimple("socket-binding-port-offset", serverInfo.portOffset));

                String path = "host=" + hostName + ",server-config=" + serverInfo.name;
                config.put(new PropertySimple("path", path));

                // get from the domain or other place as soon as the domain provides it.
                //XXX hardcoded separators?
                String serverLog = hcConfig.getSimpleValue("baseDir", "/tmp") + File.separator + "domain/servers/"
                    + serverInfo.name + "/log/server.log";
                initLogEventSourcesConfigProp(serverLog, config);

                String version;
                String resourceDescription;

                String resourceName = serverInfo.name;

                if (hostInfo.productName.equalsIgnoreCase("eap")) {
                    version = "EAP " + hostInfo.productVersion;
                    resourceDescription = "Managed JBoss Enterprise Application Platform 6 server";
                    resourceName = "EAP " + resourceName;
                } else if (hostInfo.productName.equalsIgnoreCase("EDG")) {
                    version = "EDG " + hostInfo.productVersion;
                    resourceDescription = "Managed JBoss Enterprise Data Grid 6 server";
                } else {
                    resourceDescription = "Managed AS7 server";
                    version = hostInfo.releaseVersion;
                }

                DiscoveredResourceDetails detail = new DiscoveredResourceDetails(discoveryContext.getResourceType(), // ResourceType
                    hostName + "/" + serverInfo.name, // key
                    resourceName, // Name
                    version, // TODO  get from Domain as soon as it is provided
                    resourceDescription, // Description
                    config, null);

                // Add to return values
                discoveredResources.add(detail);
                log.info("Discovered new ...  " + discoveryContext.getResourceType() + ", " + serverInfo);
            }
        } catch (Exception e) {
            log.warn("Discovery for a " + discoveryContext.getResourceType() + " failed for process " + " :"
                + e.getMessage());
        }
        return discoveredResources;
    }

    private String resolveSocketBindingGroup(String serverGroup) {
        Address address = new Address("server-group", serverGroup);
        Operation operation = new ReadAttribute(address, "socket-binding-group");
        Result result = parentComponent.getASConnection().execute(operation);
        return (String) result.getResult();
    }

    private List<ServerInfo> getManagedServers(String domainHost) {
        Address address = new Address("host", domainHost);
        Operation operation = new ReadChildrenNames(address, "server-config");
        ASConnection connection = parentComponent.getASConnection();
        Result res = connection.execute(operation);
        List<String> servers = (List<String>) res.getResult();
        List<ServerInfo> ret = new ArrayList<ServerInfo>(servers.size());
        for (String server : servers) {
            ServerInfo info = new ServerInfo();
            info.name = server;
            ret.add(info);

            address = new Address("host", domainHost);
            address.add("server-config", server);
            operation = new ReadResource(address);
            ComplexResult cres = connection.executeComplex(operation);
            Map<String, Object> map = cres.getResult();
            info.group = (String) map.get("group");
            info.autoStart = (Boolean) map.get("auto-start");
            Integer offset = (Integer) map.get("socket-binding-port-offset");
            if (offset != null)
                info.portOffset = offset;
            info.bindingGroup = (String) map.get("socket-binding-group");
        }

        return ret;
    }

    private HostInfo getHostInfo(String domainHost) {
        Address address = new Address("host", domainHost);
        Operation operation = new ReadResource(address);
        HostInfo info = new HostInfo();

        ComplexResult cres = parentComponent.getASConnection().executeComplex(operation);
        if (!cres.isSuccess())
            return null;

        Map<String, Object> map = cres.getResult();
        info.releaseCodeName = (String) map.get("release-codename");
        info.releaseVersion = (String) map.get("release-version");
        info.productName = (String) map.get("product-name");
        info.productVersion = (String) map.get("product-version");

        return info;
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

    private static class ServerInfo {
        String name;
        String group;
        boolean autoStart;
        int portOffset;
        String bindingGroup;

        @Override
        public String toString() {
            return "ServerInfo{" + "name='" + name + '\'' + ", group='" + group + '\'' + '}';
        }
    }

    private static class HostInfo {
        String name;
        String productVersion;
        String releaseVersion;
        String productName;
        String releaseCodeName;
    }
}
