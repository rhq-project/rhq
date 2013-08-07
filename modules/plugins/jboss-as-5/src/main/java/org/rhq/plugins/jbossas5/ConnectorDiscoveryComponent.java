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

package org.rhq.plugins.jbossas5;

import static org.rhq.plugins.jbossas5.ConnectorComponent.ADDRESS_PROPERTY;
import static org.rhq.plugins.jbossas5.ConnectorComponent.HOST_PROPERTY;
import static org.rhq.plugins.jbossas5.ConnectorComponent.PORT_PROPERTY;
import static org.rhq.plugins.jbossas5.ConnectorComponent.PROTOCOL_PROPERTY;
import static org.rhq.plugins.jbossas5.ManagedComponentComponent.Config.COMPONENT_NAME;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.managed.api.ComponentType;
import org.jboss.managed.api.ManagedComponent;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.jbossas5.helper.MoreKnownComponentTypes;
import org.rhq.plugins.jbossas5.util.ManagedComponentUtils;
import org.rhq.plugins.jbossas5.util.RegularExpressionNameMatcher;

/**
 * A component for discovering JBoss Web connectors.
 *
 * @author Ian Springer
 */
public class ConnectorDiscoveryComponent
        implements ResourceDiscoveryComponent<JBossWebComponent>
{
    private static final Log LOG = LogFactory.getLog(ConnectorDiscoveryComponent.class);

    // A regex for the names of all MBean:WebRequestProcessor components,
    // e.g. "jboss.web:name=http-127.0.0.1-8080,type=GlobalRequestProcessor"
    private static final String WEB_REQUEST_PROCESSOR_COMPONENT_NAMES_REGEX =
            "jboss.web:name=([^\\-]+)-(.+)-([0-9]+),type=GlobalRequestProcessor";

    private static final Pattern WEB_REQUEST_PROCESSOR_COMPONENT_NAMES_PATTERN = Pattern
        .compile(WEB_REQUEST_PROCESSOR_COMPONENT_NAMES_REGEX);
    
    private static final Pattern HOSTADRESS_PATTERN = Pattern
        .compile("(.*)(%2F)(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})");

    public Set<DiscoveredResourceDetails> discoverResources(
            ResourceDiscoveryContext<JBossWebComponent> discoveryContext) throws Exception
    {
        ResourceType resourceType = discoveryContext.getResourceType();
        LOG.trace("Discovering " + resourceType.getName() + " Resources...");

        JBossWebComponent jbossWebComponent = discoveryContext.getParentResourceComponent();
        ManagementView managementView = jbossWebComponent.getConnection().getManagementView();

        // TODO (ips): Only refresh the ManagementView *once* per runtime discovery scan, rather than every time this
        //             method is called. Do this by providing a runtime scan id in the ResourceDiscoveryContext.
        managementView.load();

        Set<ManagedComponent> webRequestProcessorComponents = getWebRequestProcessorComponents(managementView);
        Set<DiscoveredResourceDetails> discoveredResources = new LinkedHashSet(webRequestProcessorComponents.size());
        for (ManagedComponent webRequestProcessorComponent : webRequestProcessorComponents)
        {
            // Parse the component name, e.g. "jboss.web:name=http-127.0.0.1-8080,type=GlobalRequestProcessor", to
            // figure out the protocol, address, and port.
            Matcher matcher = WEB_REQUEST_PROCESSOR_COMPONENT_NAMES_PATTERN.matcher(webRequestProcessorComponent
                .getName());
            if (!matcher.matches())
            {
                LOG.error("Component name '" + webRequestProcessorComponent.getName() + "' does not match regex '"
                        + WEB_REQUEST_PROCESSOR_COMPONENT_NAMES_PATTERN + "'.");
                continue;
            }
            String protocol = matcher.group(1);
            String host = null;
            String address = null;
            String hostAddress = matcher.group(2);
            Matcher hostAddressMatcher = HOSTADRESS_PATTERN.matcher(hostAddress);
            if (hostAddressMatcher.matches()) {
                // We have a composed host address string: my-server.com%2F127.0.0.98
                host = hostAddressMatcher.group(1);
                address = hostAddressMatcher.group(3);
            } else {
                // We only have an IP address
                address = hostAddress;
            }

            int port = Integer.valueOf(matcher.group(3));

            String resourceKey = protocol + "://" + address + ":" + port;
            String resourceName = protocol + "://" + address + ":" + port;
            String resourceDescription = resourceType.getDescription();
            String resourceVersion = null;

            Configuration pluginConfig = discoveryContext.getDefaultPluginConfiguration();
            pluginConfig.put(new PropertySimple(COMPONENT_NAME, webRequestProcessorComponent.getName()));
            pluginConfig.put(new PropertySimple(PROTOCOL_PROPERTY, protocol));
            pluginConfig.put(new PropertySimple(HOST_PROPERTY, host));
            pluginConfig.put(new PropertySimple(ADDRESS_PROPERTY, address));
            pluginConfig.put(new PropertySimple(PORT_PROPERTY, port));

            DiscoveredResourceDetails resource =
                    new DiscoveredResourceDetails(resourceType,
                            resourceKey,
                            resourceName,
                            resourceVersion,
                            resourceDescription,
                            pluginConfig,
                            null);

            discoveredResources.add(resource);
        }

        LOG.trace("Discovered " + discoveredResources.size() + " " + resourceType.getName() + " Resources.");
        return discoveredResources;
    }

    private static Set<ManagedComponent> getWebRequestProcessorComponents(ManagementView managementView)
            throws Exception
    {
        ComponentType webRequestProcessorComponentType = MoreKnownComponentTypes.MBean.WebRequestProcessor.getType();
        return ManagedComponentUtils.getManagedComponents(managementView, webRequestProcessorComponentType,
                WEB_REQUEST_PROCESSOR_COMPONENT_NAMES_REGEX, new RegularExpressionNameMatcher());
    }
}
