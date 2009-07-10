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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.system.ProcessInfo;
import org.rhq.core.system.SystemInfo;

/**
 * Discovers generic processes to manage based on process queries or pidfiles.
 *  
 * @author Greg Hinkle
 */
public class ProcessDiscoveryComponent implements ResourceDiscoveryComponent {

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext resourceDiscoveryContext)
        throws InvalidPluginConfigurationException, Exception {

        SystemInfo systemInformation = resourceDiscoveryContext.getSystemInformation();

        Set<DiscoveredResourceDetails> results = new HashSet<DiscoveredResourceDetails>();

        // the configs for the manually added processes to discover
        List<Configuration> configs = resourceDiscoveryContext.getPluginConfigurations();
        for (Configuration config : configs) {

            ProcessInfo processInfo = ProcessComponent.getProcessForConfiguration(config, systemInformation);

            String type = config.getSimpleValue("type", "pidFile");
            String resourceKey = config.getSimpleValue(type, null);
            if (resourceKey == null || resourceKey.length() == 0) {
                throw new InvalidPluginConfigurationException("Invalid type [" + type + "] value [" + resourceKey + "]");
            }

            ResourceType resourceType = resourceDiscoveryContext.getResourceType();
            String resourceName = processInfo.getBaseName();
            String resourceVersion = null;
            String resourceDescription = null;

            DiscoveredResourceDetails detail = new DiscoveredResourceDetails(resourceType, resourceKey, resourceName,
                resourceVersion, resourceDescription, config, processInfo);

            results.add(detail);
        }

        return results;
    }
}
