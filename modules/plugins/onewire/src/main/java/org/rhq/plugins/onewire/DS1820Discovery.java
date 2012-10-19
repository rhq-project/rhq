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
package org.rhq.plugins.onewire;

import java.util.HashSet;
import java.util.Set;

import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.container.OneWireContainer;
import com.dalsemi.onewire.container.OneWireContainer10;

import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * Discover a DS18S20 or compatible thermometer chip on the bus. Those belong
 * to family 10 and are thus instances of {@link OneWireContainer10}.
 * @author Heiko W. Rupp
 */
public class DS1820Discovery implements ResourceDiscoveryComponent<OneWireAdapterComponent> {

    /* (non-Javadoc)
     * @see org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent#discoverResources(org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext)
     */
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<OneWireAdapterComponent> context)
        throws InvalidPluginConfigurationException, Exception {

        DSPortAdapter adapter = context.getParentResourceComponent().getAdapter();
        Set<DiscoveredResourceDetails> results = new HashSet<DiscoveredResourceDetails>();

        OneWireContainer cont = adapter.getFirstDeviceContainer();
        while (cont != null) {
            if (cont instanceof OneWireContainer10) {

                String descr = cont.getDescription();
                if (descr.length() > 50) { // Shorten. The device can have a really long description
                    int pos = descr.indexOf(".");
                    if (pos > 0) {
                        descr = descr.substring(0, pos);
                        descr += "...";
                    }
                }

                DiscoveredResourceDetails detail = new DiscoveredResourceDetails(context.getResourceType(), // Resource type 
                    cont.getAddressAsString(), // unique address from device
                    cont.getName(), // the typ of device
                    "", // version
                    descr, // description
                    context.getDefaultPluginConfiguration(), // plugin config
                    null); // no physical info

                results.add(detail);
            }
            cont = adapter.getNextDeviceContainer();
        }
        return results;
    }
}
