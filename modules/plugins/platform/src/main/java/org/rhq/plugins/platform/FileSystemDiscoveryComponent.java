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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.sigar.FileSystem;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.system.FileSystemInfo;
import org.rhq.core.system.SystemInfo;

/**
 * @author Greg Hinkle
 */
public class FileSystemDiscoveryComponent implements ResourceDiscoveryComponent<PlatformComponent> {
    private final Log log = LogFactory.getLog(this.getClass());

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<PlatformComponent> discoveryContext)
        throws Exception {

        Set<DiscoveredResourceDetails> results = new HashSet<DiscoveredResourceDetails>();

        SystemInfo sysInfo = discoveryContext.getSystemInformation();
        if (!sysInfo.isNative()) {
            log.debug("Skipping " + discoveryContext.getResourceType().getName()
                + " discovery, since native system info is not available.");
            return results;
        }

        String hostname = discoveryContext.getSystemInformation().getHostname();

        for (FileSystemInfo fs : sysInfo.getFileSystems()) {
            Configuration defaultPluginConfiguration = discoveryContext.getDefaultPluginConfiguration();
            
            int fsType = fs.getFileSystem().getType();
            if (fsType != FileSystem.TYPE_LOCAL_DISK && fsType != FileSystem.TYPE_NETWORK) {
                continue;
            }
            try {
                String name = ((hostname == null) ? "" : (hostname + " ")) + " File System ("
                    + fs.getFileSystem().getTypeName() + ") " + fs.getMountPoint();
                DiscoveredResourceDetails details = new DiscoveredResourceDetails(discoveryContext.getResourceType(),
                    fs.getMountPoint(), name, null, fs.getFileSystem().getDevName() + ": "
                        + fs.getFileSystem().getDirName(), defaultPluginConfiguration, null);
                results.add(details);
            } catch (Exception e) {
                log.error("File system discovery failed : " + e.getMessage() + ", skipping.");
            }
        }
        return results;
    }
}