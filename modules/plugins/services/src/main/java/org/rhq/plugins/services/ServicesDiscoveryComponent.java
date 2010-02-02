/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.plugins.services;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ManualAddFacet;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * The ResourceDiscoveryComponent for the "Services File" ResourceType.
 *
 * @author Partha Aji
 */
public class ServicesDiscoveryComponent<T extends ResourceComponent> implements ResourceDiscoveryComponent<T>,
    ManualAddFacet<T> {

    private final Log log = LogFactory.getLog(this.getClass());

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<T> discoveryContext)
        throws InvalidPluginConfigurationException, Exception {
        Set<DiscoveredResourceDetails> discoveredResources = new HashSet<DiscoveredResourceDetails>(1);

        Configuration pluginConfig = discoveryContext.getDefaultPluginConfiguration();

        DiscoveredResourceDetails resource = createResourceDetails(discoveryContext, pluginConfig);
        discoveredResources.add(resource);
        log.debug("Discovered " + discoveryContext.getResourceType().getName() + " Resource with key ["
            + resource.getResourceKey() + "].");

        return discoveredResources;
    }

    @Override
    public DiscoveredResourceDetails discoverResource(Configuration pluginConfiguration,
        ResourceDiscoveryContext<T> discoveryContext) throws InvalidPluginConfigurationException {

        DiscoveredResourceDetails resource = createResourceDetails(discoveryContext, pluginConfiguration);
        return resource;
    }

    private DiscoveredResourceDetails createResourceDetails(ResourceDiscoveryContext<T> discoveryContext,
        Configuration pluginConfiguration) {
        ResourceType resourceType = discoveryContext.getResourceType();
        String resourceKey = "services";
        DiscoveredResourceDetails resource = new DiscoveredResourceDetails(resourceType, resourceKey, resourceType
            .getName(), null, resourceType.getDescription(), pluginConfiguration, null);
        return resource;
    }
}
