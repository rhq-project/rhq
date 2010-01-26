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

import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * @author Greg Hinkle
 */
public class VirtualizationDomainDiscoveryComponent implements ResourceDiscoveryComponent {

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
            log.warn(String.format("Can not load native library for libvirt with uri '%s'. Message is '%s'.: ", uri, t
                .getMessage()), t);
            return details;
        }
        int[] ids;
        List<String> guests = null;
        try {
            ids = virt.getDomainIds();
            guests = virt.getDomainNames();
        } catch (Exception e) {
            log.info("Failure obtaining domains from libvirt: " + e.getMessage());
            return details;
        }

        // Ids represent running guests, Names are not running. We need to populate both.
        for (int id : ids) {
            LibVirtConnection.DomainInfo domainInfo = virt.getDomainInfo(id);
            details.add(createResource(resourceDiscoveryContext.getResourceType(), domainInfo));

        }
        for (String guestName : guests) {
            if (guestName != null) {
                LibVirtConnection.DomainInfo domainInfo = virt.getDomainInfo(guestName);
                details.add(createResource(resourceDiscoveryContext.getResourceType(), domainInfo));
            }
        }

        virt.close();

        return details;
    }

    public DiscoveredResourceDetails createResource(ResourceType resourceType, LibVirtConnection.DomainInfo domainInfo) {
        DiscoveredResourceDetails detail = new DiscoveredResourceDetails(resourceType, domainInfo.name,
            domainInfo.name, null, // TODO - Change to domain id?
            "Guest Named " + domainInfo.name, null, null);
        return detail;
    }
}