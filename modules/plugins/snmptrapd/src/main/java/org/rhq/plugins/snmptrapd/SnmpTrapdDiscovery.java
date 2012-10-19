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

package org.rhq.plugins.snmptrapd;

import java.util.HashSet;
import java.util.Set;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ManualAddFacet;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * Discovery component for an SNMP trapd
 * @author Heiko W. Rupp
 *
 */
public class SnmpTrapdDiscovery implements ResourceDiscoveryComponent<SnmpTrapdComponent>, ManualAddFacet<SnmpTrapdComponent> {

    private static final String PORT_PROPERTY = "port";

    /*
     * Autodiscovery is not supported.
     */
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext context)
        throws InvalidPluginConfigurationException, Exception {

        return new HashSet<DiscoveredResourceDetails>();

    }

    /*
     * Manual discovery
     */
    public DiscoveredResourceDetails discoverResource(Configuration pluginConfiguration,
                                                      ResourceDiscoveryContext<SnmpTrapdComponent> ctx) throws InvalidPluginConfigurationException {

        String port = pluginConfiguration.getSimpleValue(PORT_PROPERTY, null);
        String key = "Trapd " + port;
        String name = key;
        String description = "SNMP Trap receiver on port " + port;
        ResourceType resourceType = ctx.getResourceType();
        DiscoveredResourceDetails details = new DiscoveredResourceDetails(resourceType, key, name, null, description,
            pluginConfiguration, null);

        return details;
    }
}
