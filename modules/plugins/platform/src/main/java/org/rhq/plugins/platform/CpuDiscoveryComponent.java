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
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.system.CpuInformation;
import org.rhq.core.system.SystemInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Discovers CPUs found on this platform.
 *
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public class CpuDiscoveryComponent implements ResourceDiscoveryComponent<PlatformComponent> {
    private final Log log = LogFactory.getLog(this.getClass());

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<PlatformComponent> discoveryContext) {
        Set<DiscoveredResourceDetails> results = new HashSet<DiscoveredResourceDetails>();

        SystemInfo sysInfo = discoveryContext.getSystemInformation();
        if (!sysInfo.isNative()) {
            log.debug("Skipping " + discoveryContext.getResourceType().getName() +
                    " discovery, since native system info is not available.");
            return results;
        }
        
        int numberOfCpus = sysInfo.getNumberOfCpus();
        for (int i = 0; i < numberOfCpus; i++) {
            CpuInformation cpuInfo = sysInfo.getCpu(i);
            DiscoveredResourceDetails details = new DiscoveredResourceDetails(discoveryContext.getResourceType(), // resourceType
                String.valueOf(i), // resourceKey
                "CPU " + i, // resourceName
                cpuInfo.getCpuInfo().getModel(), // resourceVersion
                cpuInfo.getCpuInfo().getVendor() + ' ' + cpuInfo.getCpuInfo().getModel(), // resourceDescription
                null, null);

            results.add(details);
        }
        return results;
    }
}