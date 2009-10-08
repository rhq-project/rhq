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
package org.rhq.plugins.virt;

import java.util.HashSet;
import java.util.Set;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * @author Greg Hinkle
 */
public class VirtualizationBlockDeviceDiscoveryComponent implements ResourceDiscoveryComponent<VirtualizationComponent> {
    public Set<DiscoveredResourceDetails> discoverResources(
        ResourceDiscoveryContext<VirtualizationComponent> discoveryContext) throws InvalidPluginConfigurationException,
        Exception {
        Set<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>();

        Configuration config = discoveryContext.getParentResourceComponent().loadResourceConfiguration();

        if (config != null) {
            PropertyList list = config.getList("disks");
            for (Property p : list.getList()) {
                PropertyMap intf = (PropertyMap) p;

                String device = intf.getSimple("targetDevice").getStringValue();

                DiscoveredResourceDetails detail = new DiscoveredResourceDetails(discoveryContext.getResourceType(),
                    device, device + " virtual device", null, "Virtual block device", null, null);
                details.add(detail);
            }
        }
        return details;
    }
}