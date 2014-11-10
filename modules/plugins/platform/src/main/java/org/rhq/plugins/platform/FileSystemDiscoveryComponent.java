/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

import java.util.LinkedHashSet;
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
 * @author Ian Springer
 */
public class FileSystemDiscoveryComponent implements ResourceDiscoveryComponent<PlatformComponent> {

    private static final String SYS_TYPE_NAME_LOFS = "lofs";
    private static final String SYS_TYPE_NAME_TMPFS = "tmpfs";
    private static final String SYS_TYPE_NAME_BTRFS = "btrfs";
    private static final String SYS_TYPE_NAME_ZFS = "zfs";
    private static final String SYS_TYPE_NAME_F2FS = "f2fs";

    private final Log log = LogFactory.getLog(this.getClass());

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<PlatformComponent> discoveryContext)
        throws Exception {

        Set<DiscoveredResourceDetails> results = new LinkedHashSet<DiscoveredResourceDetails>();

        SystemInfo sysInfo = discoveryContext.getSystemInformation();
        if (!sysInfo.isNative()) {
            log.debug("Skipping " + discoveryContext.getResourceType().getName()
                + " discovery, since native system info is not available.");
            return results;
        }

        for (FileSystemInfo fsInfo : sysInfo.getFileSystems()) {
            FileSystem fs = fsInfo.getFileSystem();
            int fsType = fs.getType();
            // We only support local, network (nfs), lofs, or tmpfs filesystems - skip any other types.
            switch (fsType) {
            case FileSystem.TYPE_LOCAL_DISK:
            case FileSystem.TYPE_NETWORK:
                break;
            default:
                String sysTypeName = fs.getSysTypeName();
                if (!(SYS_TYPE_NAME_LOFS.equals(sysTypeName) || SYS_TYPE_NAME_TMPFS.equals(sysTypeName)
                || SYS_TYPE_NAME_BTRFS.equals(sysTypeName) || SYS_TYPE_NAME_F2FS.equals(sysTypeName)
                || SYS_TYPE_NAME_ZFS.equals(sysTypeName))) {
                    continue;
                }
            }

            try {
                String key = fsInfo.getMountPoint();
                String name = fsInfo.getMountPoint();
                String description = fsInfo.getFileSystem().getDevName() + ": " + fsInfo.getFileSystem().getDirName();
                Configuration pluginConfig = discoveryContext.getDefaultPluginConfiguration();
                DiscoveredResourceDetails details = new DiscoveredResourceDetails(discoveryContext.getResourceType(),
                    key, name, null, description, pluginConfig, null);
                results.add(details);
            } catch (Exception e) {
                log.error("File system discovery failed for [" + fsInfo + "].", e);
            }
        }

        return results;
    }

}
