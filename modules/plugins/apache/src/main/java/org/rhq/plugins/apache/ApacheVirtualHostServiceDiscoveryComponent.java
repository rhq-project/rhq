/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.plugins.apache;

import java.io.File;
import java.net.URI;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.augeas.AugeasException;

import org.rhq.augeas.node.AugeasNode;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.apache.util.HttpdAddressUtility;
import org.rhq.plugins.apache.util.HttpdAddressUtility.Address;

/**
 * Discovers VirtualHosts under the Apache server by reading them out from Augeas tree constructed
 * in the parent component.
 * 
 * @author Lukas Krejci
 */
public class ApacheVirtualHostServiceDiscoveryComponent implements ResourceDiscoveryComponent<ApacheServerComponent> {

    public static final String LOGS_DIRECTORY_NAME = "logs";

    private static final String RT_LOG_FILE_NAME_SUFFIX = "_rt.log";

    private final Log log = LogFactory.getLog(getClass());

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<ApacheServerComponent> context)
        throws InvalidPluginConfigurationException, Exception {

        Set<DiscoveredResourceDetails> discoveredResources = new LinkedHashSet<DiscoveredResourceDetails>();

        //first define the root server as one virtual host
        ResourceType resourceType = context.getResourceType();

        Configuration mainServerPluginConfig = context.getDefaultPluginConfiguration();

        File configPath = context.getParentResourceComponent().getServerRoot();
        File logsDir = new File(configPath, LOGS_DIRECTORY_NAME);        
        
        String mainServerUrl = context.getParentResourceContext().getPluginConfiguration().getSimple(
            ApacheServerComponent.PLUGIN_CONFIG_PROP_URL).getStringValue();
        if (mainServerUrl != null && !"null".equals(mainServerUrl)) {
            PropertySimple mainServerUrlProp = new PropertySimple(ApacheVirtualHostServiceComponent.URL_CONFIG_PROP,
                mainServerUrl);

            mainServerPluginConfig.put(mainServerUrlProp);

            URI mainServerUri = new URI(mainServerUrl);
            String host = mainServerUri.getHost();
            int port = mainServerUri.getPort();
            if (port == -1) {
                port = 80;
            }
            
            File rtLogFile = new File(logsDir, host + port
                + RT_LOG_FILE_NAME_SUFFIX);
            
            PropertySimple rtLogProp = new PropertySimple(
                ApacheVirtualHostServiceComponent.RESPONSE_TIME_LOG_FILE_CONFIG_PROP, rtLogFile.toString());
            mainServerPluginConfig.put(rtLogProp);
        }

        DiscoveredResourceDetails mainServer = new DiscoveredResourceDetails(resourceType,
            ApacheVirtualHostServiceComponent.MAIN_SERVER_RESOURCE_KEY, "Main Server", null, null,
            mainServerPluginConfig, null);
        discoveredResources.add(mainServer);

        //read the virtual hosts from augeas
        AugeasTree ag = null;

        try {
            ag = context.getParentResourceComponent().getAugeasTree();
        } catch (AugeasException e) {
            log.warn("Could not obtain the Augeas tree, giving up virtual host discovery.", e);
            return discoveredResources;
        }

        List<AugeasNode> virtualHosts = ag.matchRelative(ag.getRootNode(), "<VirtualHost");

        for (AugeasNode node : virtualHosts) {
            List<AugeasNode> hosts = ag.matchRelative(node, "param");
            String firstAddress = hosts.get(0).getValue();

            List<AugeasNode> serverNames = ag.matchRelative(node, "ServerName/param");
            String serverName = null;
            if (serverNames.size() > 0) {
                serverName = serverNames.get(0).getValue();
            }

            StringBuilder keyBuilder = new StringBuilder();
            if (serverName != null) {
                keyBuilder.append(serverName).append("|");
            }
            keyBuilder.append(firstAddress);

            Iterator<AugeasNode> it = hosts.iterator();
            it.next();

            while (it.hasNext()) {
                keyBuilder.append(" ").append(it.next().getValue());
            }

            String resourceKey = keyBuilder.toString();

            Configuration pluginConfiguration = context.getDefaultPluginConfiguration();

            Address address = HttpdAddressUtility.getVirtualHostSampleAddress(ag, firstAddress, serverName);
            if (address != null) {
                String url = "http://" + address.host + ":" + address.port + "/";

                PropertySimple urlProp = new PropertySimple(ApacheVirtualHostServiceComponent.URL_CONFIG_PROP, url);
                pluginConfiguration.put(urlProp);
            }

            File rtLogFile = new File(logsDir, address.host + address.port
                + RT_LOG_FILE_NAME_SUFFIX);
            
            PropertySimple rtLogProp = new PropertySimple(
                ApacheVirtualHostServiceComponent.RESPONSE_TIME_LOG_FILE_CONFIG_PROP, rtLogFile.toString());
            pluginConfiguration.put(rtLogProp);

            String resourceName = "Virtual Host ";
            if (serverName != null) {
                resourceName += address.host + ":" + address.port;
            } else {
                resourceName += resourceKey;
            }

            discoveredResources.add(new DiscoveredResourceDetails(resourceType, resourceKey, resourceName, null, null,
                pluginConfiguration, null));
        }

        return discoveredResources;
    }
}