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
package org.rhq.plugins.platform;

import java.util.HashSet;
import java.util.Set;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.system.NetworkAdapterInfo;
import org.rhq.core.system.SystemInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class NetworkAdapterDiscoveryComponent implements ResourceDiscoveryComponent<PlatformComponent> {
    private final Log log = LogFactory.getLog(this.getClass());

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<PlatformComponent> discoveryContext)
            throws Exception {
        Set<DiscoveredResourceDetails> results = new HashSet<DiscoveredResourceDetails>();

        SystemInfo sysInfo = discoveryContext.getSystemInformation();
        if (!sysInfo.isNative()) {
            log.debug("Skipping " + discoveryContext.getResourceType().getName() +
                    " discovery, since native system info is not available.");
            return results;
        }

        for (NetworkAdapterInfo info : sysInfo.getAllNetworkAdapters()) {
            Configuration configuration = discoveryContext.getDefaultPluginConfiguration();
            configuration.put(new PropertySimple("macAddress", info.getMacAddressString()));
            DiscoveredResourceDetails found = new DiscoveredResourceDetails(discoveryContext.getResourceType(),
                    info.getName(), info.getDisplayName(), null, info.getMacAddressString(), configuration, null);
            results.add(found);
        }

        return results;
    }
}