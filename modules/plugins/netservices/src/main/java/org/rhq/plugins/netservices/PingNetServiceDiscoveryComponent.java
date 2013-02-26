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
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.rhq.plugins.netservices;

import java.net.InetAddress;
import java.util.Collections;
import java.util.Set;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ManualAddFacet;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * @author Greg Hinkle
 */
public class PingNetServiceDiscoveryComponent implements ResourceDiscoveryComponent, ManualAddFacet {

    private static final String RESOURCE_NAME_PREFIX = "Ping ";

    @Override
    public Set discoverResources(ResourceDiscoveryContext resourceDiscoveryContext)
        throws InvalidPluginConfigurationException, Exception {
        // We don't support auto-discovery.
        return Collections.emptySet();
    }

    @Override
    public DiscoveredResourceDetails discoverResource(Configuration config,
        ResourceDiscoveryContext resourceDiscoveryContext) throws InvalidPluginConfigurationException {
        InetAddress address = PingNetServiceComponent.createComponentConfiguration(config);
        DiscoveredResourceDetails details = new DiscoveredResourceDetails(resourceDiscoveryContext.getResourceType(),
            address.getHostAddress(), RESOURCE_NAME_PREFIX + address.getHostAddress(), null, null, config, null);
        return details;
    }
}