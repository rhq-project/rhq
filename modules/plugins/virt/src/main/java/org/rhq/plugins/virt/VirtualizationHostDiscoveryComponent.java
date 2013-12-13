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
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ManualAddFacet;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.virt.LibVirtConnection.HVInfo;

/**
 * Discovers Host and Guest information using
 */
public class VirtualizationHostDiscoveryComponent implements ResourceDiscoveryComponent, ManualAddFacet {

    private Log log = LogFactory.getLog(getClass());

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext resourceDiscoveryContext)
        throws InvalidPluginConfigurationException, Exception {

        Set<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>();

        DiscoveredResourceDetails detail = getResource(resourceDiscoveryContext.getResourceType(),
            resourceDiscoveryContext.getDefaultPluginConfiguration());
        if (detail != null) {
            details.add(detail);
        }
        return details;
    }

    DiscoveredResourceDetails getResource(ResourceType type, Configuration config) {
        //Libvirt is smart enough to use the default when passing in an empty string
        DiscoveredResourceDetails res = null;
        LibVirtConnection virt;
        try {
            String uri = config.getSimpleValue("connectionURI", "");
            //System.out.println(uri);
            virt = new LibVirtConnection(uri);
            HVInfo hi = virt.getHVInfo();
            res = new DiscoveredResourceDetails(type, "VirtHost." + hi.hostname, hi.hvType + " Hypervisor", ""
                + hi.version, String.format("Libvirt Connection to a %s hypervisor", hi.hvType), null, null);
            res.getPluginConfiguration().put(new PropertySimple("connectionURI", virt.getConnectionURI()));
            virt.close();
        } catch (Throwable t) {
            String message = t.getMessage();
            if (t.getCause()!=null) {
                message += ": " + t.getCause().getMessage();
            }
            log.warn("Can not load libvirt: " + message);

        }

        return res;
    }

    public DiscoveredResourceDetails discoverResource(Configuration pluginConfiguration,
        ResourceDiscoveryContext context) throws InvalidPluginConfigurationException {
        return getResource(context.getResourceType(), pluginConfiguration);
    }
}