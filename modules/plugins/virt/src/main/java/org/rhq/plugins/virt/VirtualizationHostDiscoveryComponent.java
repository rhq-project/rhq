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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.virt.LibVirtConnection.HVInfo;

/**
 * Discovers Host and Guest information using 
 */
public class VirtualizationHostDiscoveryComponent implements ResourceDiscoveryComponent {

    private Log log = LogFactory.getLog(getClass());

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext resourceDiscoveryContext)
        throws InvalidPluginConfigurationException, Exception {

        Set<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>();

        LibVirtConnection virt;
        try {
            //Libvirt is smart enough to use the default when passing in an empty string
            String uri = resourceDiscoveryContext.getDefaultPluginConfiguration().getSimpleValue("connectionURI", "");
            virt = new LibVirtConnection(uri);
            HVInfo hi = virt.getHVInfo();
            DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
                resourceDiscoveryContext.getResourceType(), "VirtHost", hi.hvType + " Hypervisor", "" + hi.version,
                String.format("Libvirt Connection to a %s hypervisor", hi.hvType), null, null);
            detail.getPluginConfiguration().put(new PropertySimple("ConnectionURI", virt.getConnectionURI()));
            details.add(detail);

        } catch (Throwable t) {
            log.warn("Can not load libvirt: " + t.getMessage(), t);
            return details;
        }

        return details;
    }

    public static void populateConfigurationForHV(Configuration config, HVInfo info) {
        config.put(new PropertySimple("hypervisorType", info.hvType));
        config.put(new PropertySimple("hostName", info.hostname));
        config.put(new PropertySimple("libvirtVersion", info.libvirtVersion));
    }
}