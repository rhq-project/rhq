/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.modules.plugins.jbossas7;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
import org.rhq.modules.plugins.jbossas7.json.ExpressionResolver;
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
public class ManagedASDiscovery implements ResourceDiscoveryComponent<HostControllerComponent<?>> {
    private static final Log LOG = LogFactory.getLog(ManagedASDiscovery.class);

    private HostControllerComponent parentComponent;

    /**
     * Run the auto-discovery
     */
    public Set<DiscoveredResourceDetails> discoverResources(
            ResourceDiscoveryContext<HostControllerComponent<?>> discoveryContext) throws Exception {
        Set<DiscoveredResourceDetails> discoveredResources = new HashSet<DiscoveredResourceDetails>();

        parentComponent = discoveryContext.getParentResourceComponent();
        Configuration hcConfig = discoveryContext.getParentResourceContext().getPluginConfiguration();
        String hostName = parentComponent.getASHostName();

        // TODO verify we don't need this anymore
        if (hostName==null) {
            hostName = getHostName(discoveryContext.getParentResourceComponent().getASConnection());
        }
        String productTypeString = hcConfig.getSimpleValue("productType", null);
        JBossProductType productType = JBossProductType.valueOf(productTypeString);

        HostInfo hostInfo = getHostInfo(hostName);
        if (hostInfo == null)
            return discoveredResources;

        try {
            // get the HostController, as this is an indicator for managed AS

            // Now we have the host controller, let's get the host.xml file
            // and obtain the servers from there.

            List<ServerInfo> serverNames = getManagedServers(hostName);
            for (ServerInfo serverInfo : serverNames) {
                Configuration pluginConfig = discoveryContext.getDefaultPluginConfiguration();
                pluginConfig.put(new PropertySimple("domainHost", hostName));
                pluginConfig.put(new PropertySimple("group", serverInfo.group));
                if (serverInfo.bindingGroup != null) {
                    pluginConfig.put(new PropertySimple("socket-binding-group", serverInfo.bindingGroup));
                } else {
                    String group = resolveSocketBindingGroup(serverInfo.group);
                    pluginConfig.put(new PropertySimple("socket-binding-group", group));

                }
                pluginConfig.put(new PropertySimple("socket-binding-port-offset", serverInfo.portOffset));

                String path = "host=" + hostName + ",server-config=" + serverInfo.name;
                pluginConfig.put(new PropertySimple("path", path));


                String tmpDir = System.getProperty("java.io.tmpdir");
                String serverLog = hcConfig.getSimpleValue("baseDir", tmpDir) + File.separator + "servers" + File.separator
                    + serverInfo.name + File.separator + "log" + File.separator + "server.log";
                initLogEventSourcesConfigProp(serverLog, pluginConfig);

                String version;
                if (productType == JBossProductType.AS) {
                    version = hostInfo.releaseVersion;
                } else {
                    version = productType.SHORT_NAME + " " + hostInfo.productVersion;
                }
                String resourceName = productType.SHORT_NAME + " " + serverInfo.name;
                String resourceDescription = "Managed " + productType.FULL_NAME + " server";

                String resourceKey = createResourceKey(hostName, serverInfo.name);
                // TODO: Try to find the process corresponding to the managed server, so we can include the ProcessInfo
                //       in the details.
                DiscoveredResourceDetails detail = new DiscoveredResourceDetails(discoveryContext.getResourceType(),
                    resourceKey, resourceName, version, resourceDescription, pluginConfig, null);

                // Add to return values
                discoveredResources.add(detail);

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Discovered new " + discoveryContext.getResourceType().getName() + " Resource with key ["
                        + detail.getResourceKey() + "].");
                }
            }
        } catch (Exception e) {
            LOG.warn("Discovery for a " + discoveryContext.getResourceType() + " failed for process " + " :"
                    + e.getMessage());
        }
        return discoveredResources;
    }

    public static String createResourceKey(String hostControllerName, String managedServerName) {
        return hostControllerName + "/" + managedServerName;
    }

    /**
     * Let us try to determine the hostname of the parent's controller via api access.
     * If that does not work, use host name resolution.
     *
     * @param asConnection ASConnection to the parent
     * @return Host name
     */
    private String getHostName(ASConnection asConnection) {

        Operation op = new ReadAttribute(new Address(),"local-host-name");
        String hostname;
        Result res = asConnection.execute(op);
        if (res.isSuccess()) {
            hostname = (String) res.getResult();
            return hostname;
        }
        // Above failed. Now try falling back to host name resolution
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            hostname = "master"; // Very last resort
        }

        return hostname;
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
        ExpressionResolver resolver = parentComponent.getExpressionResolver();
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
            info.autoStart = resolver.getBoolean(map.get("auto-start"));
            Integer offset = resolver.getInteger(map.get("socket-binding-port-offset"));
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

        if (!serverLogFile.isDirectory()) {
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
