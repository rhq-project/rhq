/*
 * RHQ Management Platform
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ManualAddFacet;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

import java.util.Collections;
import java.util.Set;

/**
 * @author Thomas Segismont
 */
public class PortNetServiceDiscoveryComponent implements ResourceDiscoveryComponent, ManualAddFacet {

    private static final String RESOURCE_PREFIX = "Port ";

    private static final String SEPARATOR = ":";

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext context)
        throws InvalidPluginConfigurationException, Exception {
        // No automatic discovery
        return Collections.EMPTY_SET;
    }

    @Override
    public DiscoveredResourceDetails discoverResource(Configuration pluginConfiguration,
        ResourceDiscoveryContext context) throws InvalidPluginConfigurationException {
        PortNetServiceComponentConfiguration componentConfiguration = PortNetServiceComponent
            .createComponentConfiguration(pluginConfiguration);
        String resourceKey = createResourceKey(componentConfiguration);
        return new DiscoveredResourceDetails(context.getResourceType(), resourceKey, resourceKey, null, null,
            pluginConfiguration, null);
    }

    private String createResourceKey(PortNetServiceComponentConfiguration componentConfiguration) {
        return RESOURCE_PREFIX + componentConfiguration.getAddress().getHostAddress() + SEPARATOR
            + componentConfiguration.getPort();
    }
}
