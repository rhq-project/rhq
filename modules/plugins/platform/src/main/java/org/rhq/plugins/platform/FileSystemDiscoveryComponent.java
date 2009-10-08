 /*
  * RHQ Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
  * All rights reserved.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License, version 2, as
  * published by the Free Software Foundation, and/or the GNU Lesser
  * General Public License, version 2.1, also as published by the Free
  * Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License and the GNU Lesser General Public License
  * for more details.
  *
  * You should have received a copy of the GNU General Public License
  * and the GNU Lesser General Public License along with this program;
  * if not, write to the Free Software Foundation, Inc.,
  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
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
            int fsType = fs.getFileSystem().getType();
            if (fsType != FileSystem.TYPE_LOCAL_DISK && fsType != FileSystem.TYPE_NETWORK) {
                continue;
            }
            Configuration pluginConfig = discoveryContext.getDefaultPluginConfiguration();
            try {
                String name = ((hostname == null) ? "" : (hostname + " ")) + " File System ("
                    + fs.getFileSystem().getTypeName() + ") " + fs.getMountPoint();
                DiscoveredResourceDetails details = new DiscoveredResourceDetails(discoveryContext.getResourceType(),
                    fs.getMountPoint(), name, null, fs.getFileSystem().getDevName() + ": "
                        + fs.getFileSystem().getDirName(), pluginConfig, null);
                results.add(details);
            } catch (Exception e) {
                log.error("File system discovery failed: " + e + ", skipping.");
            }
        }
        return results;
    }
}