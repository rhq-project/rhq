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

package org.rhq.plugins.database;

import java.util.Collections;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ManualAddFacet;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.system.ProcessInfo;

public class H2DatabaseDiscovery implements ResourceDiscoveryComponent<ResourceComponent<?>>, ManualAddFacet<ResourceComponent<?>> {

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(
            ResourceDiscoveryContext<ResourceComponent<?>> context) {
        return Collections.emptySet();
    }

    @Override
    public DiscoveredResourceDetails discoverResource(Configuration pluginConfiguration,
            ResourceDiscoveryContext<ResourceComponent<?>> context)
            throws InvalidPluginConfigurationException {

        String version = "";
        DiscoveredResourceDetails details = createResourceDetails(context, pluginConfiguration,
            version, null);
        return details;

    }

    private static DiscoveredResourceDetails createResourceDetails(ResourceDiscoveryContext discoveryContext,
            Configuration pluginConfig, String version, @Nullable
            ProcessInfo processInfo) {
            String key = pluginConfig.getSimpleValue("url", "");
            String name = key;
            String description = "Database " + version + " (" + key + ")";
            return new DiscoveredResourceDetails(discoveryContext.getResourceType(), key, name, version, description,
                pluginConfig, processInfo);
        }

}
