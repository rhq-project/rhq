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
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.system.FileSystemInfo;
import org.hyperic.sigar.FileSystem;

/**
 * @author Greg Hinkle
 */
public class FileSystemDiscoveryComponent implements ResourceDiscoveryComponent<PlatformComponent> {
    public Set<DiscoveredResourceDetails> discoverResources(
        ResourceDiscoveryContext<PlatformComponent> resourceDiscoveryContext)
        throws InvalidPluginConfigurationException, Exception {
        Set<DiscoveredResourceDetails> results = new HashSet<DiscoveredResourceDetails>();
        for (FileSystemInfo fs : resourceDiscoveryContext.getSystemInformation().getFileSystems()) {
            int fsType = fs.getFileSystem().getType();
            if (fsType != FileSystem.TYPE_LOCAL_DISK && fsType != FileSystem.TYPE_NETWORK)
            {
                continue;
            }
            String hostname = resourceDiscoveryContext.getSystemInformation().getHostname();
            String name = ((hostname == null) ? "" : (hostname + " ")) + " File System ("
                + fs.getFileSystem().getTypeName() + ") " + fs.getMountPoint();
            DiscoveredResourceDetails details = new DiscoveredResourceDetails(resourceDiscoveryContext
                .getResourceType(), fs.getMountPoint(), name, null, fs.getFileSystem().getDevName() + ": "
                + fs.getFileSystem().getDirName(), resourceDiscoveryContext.getDefaultPluginConfiguration(), null);
            results.add(details);
        }
        return results;
    }
}