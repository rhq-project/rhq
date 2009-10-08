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
package org.rhq.plugins.hosts;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * @author Jason Dobies
 */
public class HostsAugeasDiscoveryComponent implements ResourceDiscoveryComponent {

    public Set discoverResources(ResourceDiscoveryContext discoveryContext)
            throws InvalidPluginConfigurationException, Exception {

        Set<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>();

        File hostsFile = new File("/etc/hosts");
        if (hostsFile.exists()) {
            DiscoveredResourceDetails resource =
                new DiscoveredResourceDetails(discoveryContext.getResourceType(), hostsFile.getPath(),
                        "Hosts File", null, "Hosts File", discoveryContext.getDefaultPluginConfiguration(), null);

            details.add(resource);
        }

        return details;
    }
}
