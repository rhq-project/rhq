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

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import com.dalsemi.onewire.OneWireException;
import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.adapter.OneWireIOException;
import com.dalsemi.onewire.adapter.PDKAdapterUSB;
import com.dalsemi.onewire.container.OneWireContainer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * @author Heiko W. Rupp
 *
 */
public class OneWireAdapterDiscovery implements ResourceDiscoveryComponent<OneWireAdapterComponent> {

    private static final Log log = LogFactory.getLog(OneWireAdapterDiscovery.class);

    /* (non-Javadoc)
     * @see org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent#discoverResources(org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext)
     */
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<OneWireAdapterComponent> context)
        throws InvalidPluginConfigurationException, Exception {

        Configuration pluginConfig = context.getDefaultPluginConfiguration();
        String port = pluginConfig.getSimple("port").getStringValue();

        DSPortAdapter adapter = new PDKAdapterUSB();
        adapter.selectPort(port);

        boolean found = false;
        adapter.beginExclusive(true);
        found = adapter.adapterDetected();
        adapter.endExclusive();

        Set<DiscoveredResourceDetails> ret = new HashSet<DiscoveredResourceDetails>();

        if (found) {
            String key = getIdForAdapter(adapter);
            String name = adapter.getAdapterName() + " on " + adapter.getPortName();
            log.info("Found " + name + "@[" + key + "]");
            String descr = "OneWire adapter (" + name + ") with key (" + key + ")";
            DiscoveredResourceDetails detail = new DiscoveredResourceDetails(context.getResourceType(), key, name, "",
                descr, pluginConfig, null);
            ret.add(detail);
            adapter.freePort();
        }

        return ret;
    }

    /**
     * Get the specific address of the USB to OneWire adapter. This is a special
     * DS1990 or compatible chip in the adapter itself. It belongs to special family 0x81
     * @param adapter Our USB port adapter
     * @return the unique ID of the adapter
     * @throws OneWireIOException
     * @throws OneWireException
     */
    @SuppressWarnings("unchecked")
    private String getIdForAdapter(DSPortAdapter adapter) throws OneWireIOException, OneWireException {

        Enumeration<OneWireContainer> devices = adapter.getAllDeviceContainers();
        while (devices.hasMoreElements()) {
            OneWireContainer cont = devices.nextElement();
            String name = cont.getName();
            if ("DS1990A".equals(name) || "DS2401".equals(name) || "DS2411".equals(name)) {
                // TODO check for family code of 0x81 in case we have more then one of those devices on the bus
                return cont.getAddressAsString();
            }
        }
        log.warn("Was not able to get a unique adapter name");
        return adapter.getAdapterName();

    }
}
