/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

package org.rhq.core.util.updater;

/**
 * Provides information about the estimated disk usage needed for a deployment.
 * 
 * @author John Mazzitelli
 */
public class DeploymentDiskUsage {
    private long maxDiskUsable = 0L;
    private long diskUsage = 0L;
    private int fileCount = 0;

    /**
     * Returns the amount of bytes that are estimated to be usable on disk.
     * In other words, consider this the maximum amount of space available.
     * If you compare this with the {@link #getDiskUsage() deployment disk usage}
     * you can esimtate if the disk can fit the deployment content.
     * 
     * @return maximum amount of disk that is estimated to be available
     */
    public long getMaxDiskUsable() {
        return maxDiskUsable;
    }

    public void setMaxDiskUsable(long maxDiskUsable) {
        this.maxDiskUsable = maxDiskUsable;
    }

    public long getDiskUsage() {
        return diskUsage;
    }

    public void setDiskUsage(long diskUsage) {
        this.diskUsage = diskUsage;
    }

    public void increaseDiskUsage(long additionalDiskUsage) {
        if (additionalDiskUsage > 0) {
            this.diskUsage += additionalDiskUsage;
        }
    }

    public int getFileCount() {
        return fileCount;
    }

    public void setFileCount(int fileCount) {
        this.fileCount = fileCount;
    }

    public void incrementFileCount() {
        this.fileCount++;
    }

    @Override
    public String toString() {
        return "Disk Usage: max-avail=[" + this.maxDiskUsable + "], usage=[" + this.diskUsage + "], file-count=["
            + this.fileCount + "])";
    }
}
