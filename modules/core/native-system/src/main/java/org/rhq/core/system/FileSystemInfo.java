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
package org.rhq.core.system;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.FileSystemUsage;
import org.hyperic.sigar.Sigar;

/**
 * Information about a mounted file system.
 *
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public class FileSystemInfo {
    private final Log log = LogFactory.getLog(FileSystemInfo.class);

    private final String mountPoint;
    private FileSystem fs;
    private FileSystemUsage fsUsage;

    public FileSystemInfo(String mountPoint) {
        this.mountPoint = mountPoint;
        refresh();
    }

    public void refresh() {
        Sigar sigar = new Sigar();

        try {
            try {
                // this only needs to be loaded once - it will never change during the lifetime of the file system
                if (this.fs == null) {
                    this.fs = sigar.getFileSystemMap().getFileSystem(this.mountPoint);
                }
            } catch (Exception e) {
                throw new SystemInfoException("Cannot refresh file system mounted at [" + this.mountPoint + "]", e);
            }

            try {
                // this is the usage data and therefore should be refreshed
                this.fsUsage = sigar.getMountedFileSystemUsage(this.mountPoint);
            } catch (Exception e) {
                // this happens when the file system is not available (like if its a CD-ROM without a CD loaded in it)
                // we can ignore this and set the usage data to null
                this.fsUsage = null;
                log.debug("Cannot refresh the usage data for file system mounted at [" + this.mountPoint + "]", e);
            }
        } finally {
            sigar.close();
        }
    }

    public String getMountPoint() {
        return this.mountPoint;
    }

    /**
     * Provides with static information about the file system.
     *
     * @return static file system information
     */
    public FileSystem getFileSystem() {
        return this.fs;
    }

    /**
     * This returns the usage information on the file system. This may return <code>null</code> if the file system is
     * not available (e.g when a "device not ready" error for CD-ROM drives occurs).
     *
     * @return file system usage data
     */
    public FileSystemUsage getFileSystemUsage() {
        return this.fsUsage;
    }
}