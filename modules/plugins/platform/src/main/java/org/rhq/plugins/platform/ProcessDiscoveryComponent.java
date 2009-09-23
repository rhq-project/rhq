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
package org.rhq.plugins.platform;

import java.util.Set;
import java.util.Collections;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.pluginapi.inventory.ManualAddFacet;
import org.rhq.core.system.ProcessInfo;

/**
 * Discovers generic processes to manage based on process queries or pidfiles.
 *  
 * @author Greg Hinkle
 */
public class ProcessDiscoveryComponent implements ResourceDiscoveryComponent, ManualAddFacet {

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext resourceDiscoveryContext)
        throws InvalidPluginConfigurationException, Exception {
        // We don't support auto-discovery.
        return Collections.emptySet();
    }

    public DiscoveredResourceDetails discoverResource(Configuration pluginConfig,
                                                      ResourceDiscoveryContext resourceDiscoveryContext)
            throws InvalidPluginConfigurationException {
        ProcessInfo processInfo;
        try {
            processInfo = ProcessComponent.getProcessForConfiguration(pluginConfig,
                    resourceDiscoveryContext.getSystemInformation());
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to create process based on plugin config: " + pluginConfig);
        }

        String type = pluginConfig.getSimpleValue("type", "pidFile");
        String resourceKey = pluginConfig.getSimpleValue(type, null);
        if (resourceKey == null || resourceKey.length() == 0) {
            throw new InvalidPluginConfigurationException("Invalid type [" + type + "] value [" + resourceKey + "]");
        }

        ResourceType resourceType = resourceDiscoveryContext.getResourceType();
        String resourceName = processInfo.getBaseName();
        String resourceVersion = null;
        String resourceDescription = null;

        DiscoveredResourceDetails detail = new DiscoveredResourceDetails(resourceType, resourceKey, resourceName,
            resourceVersion, resourceDescription, pluginConfig, processInfo);
        return detail;
    }
}
