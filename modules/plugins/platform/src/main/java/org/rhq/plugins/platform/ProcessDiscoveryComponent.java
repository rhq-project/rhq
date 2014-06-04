/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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

package org.rhq.plugins.platform;

import static org.rhq.plugins.platform.ProcessComponent.findProcess;
import static org.rhq.plugins.platform.ProcessComponentConfig.createProcessComponentConfig;

import java.util.Collections;
import java.util.Set;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ManualAddFacet;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.system.ProcessInfo;

/**
 * Discovers generic processes to manage based on process queries or pidfiles.
 *  
 * @author Greg Hinkle
 */
public class ProcessDiscoveryComponent implements ResourceDiscoveryComponent, ManualAddFacet {

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext resourceDiscoveryContext)
        throws InvalidPluginConfigurationException, Exception {
        // We don't support auto-discovery.
        return Collections.emptySet();
    }

    @Override
    public DiscoveredResourceDetails discoverResource(Configuration pluginConfig, ResourceDiscoveryContext context)
        throws InvalidPluginConfigurationException {

        ProcessComponentConfig processComponentConfig = createProcessComponentConfig(pluginConfig);

        ProcessInfo processInfo;
        try {
            processInfo = findProcess(processComponentConfig, context.getSystemInformation());
        } catch (Exception e) {
            throw new RuntimeException("Failed to manually add process Resource based on plugin config: "
                + pluginConfig.toString(true), e);
        }

        String resourceKey, resourceDescription;
        switch (processComponentConfig.getType()) {
        case pidFile:
            resourceKey = processComponentConfig.getPidFile();
            resourceDescription = processInfo.getBaseName() + " process with PID file [" + resourceKey + "]";
            break;
        case piql:
            resourceKey = processComponentConfig.getPiql();
            resourceDescription = processInfo.getBaseName() + " process with PIQL expression [" + resourceKey + "]";
            break;
        default:
            throw new InvalidPluginConfigurationException("Unknown type: " + processComponentConfig.getType());
        }

        return new DiscoveredResourceDetails(context.getResourceType(), resourceKey, processInfo.getBaseName(),
            null /*version*/, resourceDescription, pluginConfig, processInfo);
    }

}
