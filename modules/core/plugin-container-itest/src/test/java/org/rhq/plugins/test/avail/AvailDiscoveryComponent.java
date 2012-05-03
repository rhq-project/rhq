/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

package org.rhq.plugins.test.avail;

import java.util.HashSet;
import java.util.Set;

import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

public class AvailDiscoveryComponent implements ResourceDiscoveryComponent<ResourceComponent<?>> {

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<ResourceComponent<?>> context)
        throws InvalidPluginConfigurationException, Exception {

        ResourceType rt = context.getResourceType();
        int count = Integer.valueOf(rt.getDescription()).intValue(); // type descriptor is a number - the # of res. to discover

        HashSet<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>(count);
        for (int i = 1; i <= count; i++) {
            String key = rt.getName() + "_" + i;
            String name = rt.getName() + "_" + i;
            String version = "1";
            DiscoveredResourceDetails resource = new DiscoveredResourceDetails(rt, key, name, version, null,
                context.getDefaultPluginConfiguration(), null);
            details.add(resource);
        }

        return details;
    }

}
