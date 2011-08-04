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
package org.rhq.plugins.perftest;

import java.util.Collections;
import java.util.Set;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.perftest.configuration.ConfigurationFactory;
import org.rhq.plugins.perftest.resource.ResourceFactory;

/**
 * RHQ discovery component for discovering resources defined in the performance test scenario.
 *
 * @author Jason Dobies
 */
public class PerfTestDiscoveryComponent implements ResourceDiscoveryComponent {
    // ResourceDiscoveryComponent Implementation  --------------------------------------------

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext context)
        throws InvalidPluginConfigurationException, Exception {
        ResourceType resourceType = context.getResourceType();

        ScenarioManager manager = ScenarioManager.getInstance();
        Set<DiscoveredResourceDetails> resourceDetails = null;
        if (manager.isEnabled()) {
            ResourceFactory resourceFactory = manager.getResourceFactory(resourceType.getName());
            if (resourceFactory==null)
                return Collections.emptySet();
            
            resourceDetails = resourceFactory.discoverResources(context);

            // If there is a plugin configuration factory defined, run it on each resource
            ConfigurationFactory configurationFactory = manager.getPluginConfigurationFactory(resourceType.getName());
            if (configurationFactory != null) {
                for (DiscoveredResourceDetails details : resourceDetails) {
                    Configuration configuration = configurationFactory.generateConfiguration(resourceType
                        .getPluginConfigurationDefinition());
                    details.setPluginConfiguration(configuration);
                }
            }
        }

        return resourceDetails;
    }
}