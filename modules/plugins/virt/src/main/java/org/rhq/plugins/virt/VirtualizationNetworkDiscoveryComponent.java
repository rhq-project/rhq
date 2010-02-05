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
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.libvirt.LibvirtException;

import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.virt.LibVirtConnection.NetworkInfo;

/**
 * @author Greg Hinkle
 */
public class VirtualizationNetworkDiscoveryComponent implements ResourceDiscoveryComponent {

    private Log log = LogFactory.getLog(getClass());

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext resourceDiscoveryContext)
        throws InvalidPluginConfigurationException, Exception {

        Set<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>();

        String uri = resourceDiscoveryContext.getParentResourceContext().getPluginConfiguration().getSimpleValue(
            "connectionURI", "");
        LibVirtConnection virt = null;

        try {
            virt = new LibVirtConnection(uri);
        } catch (Throwable t) {
            log.warn("Can not load native library for libvirt: " + t.getMessage(), t);
            virt.close();
            return details;
        }

        List<String> definedNetworks = null;
        List<String> activeNetworks = null;
        try {
            definedNetworks = virt.getDefinedNetworks();
            activeNetworks = virt.getNetworks();
        } catch (Exception e) {
            log.info("Failure obtaining domains from libvirt: " + e.getMessage());
            virt.close();
            return details;
        }

        for (String net : definedNetworks) {
            details.add(createResource(resourceDiscoveryContext.getResourceType(), virt.getNetwork(net)));

        }
        for (String net : activeNetworks) {
            details.add(createResource(resourceDiscoveryContext.getResourceType(), virt.getNetwork(net)));
        }

        virt.close();

        return details;
    }

    public DiscoveredResourceDetails createResource(ResourceType resourceType, NetworkInfo net) throws LibvirtException {
        String name = net.name;
        DiscoveredResourceDetails detail = new DiscoveredResourceDetails(resourceType, name, name, null,
            "Virtual Network Named " + name, null, null);
        return detail;
    }
}