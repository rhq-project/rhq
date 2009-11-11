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
package org.rhq.enterprise.server.plugin.pc.content;

/**
 * @author jmatthews
 */
public class DistributionFileDetails
{
    private String relativeFilename;
    private long lastModified;
    private String md5sum;
    private long fileSize;

    public long getFileSize()
    {
        return fileSize;
    }

    public void setFileSize(long fileSizeIn)
    {
        this.fileSize = fileSizeIn;
    }

    public DistributionFileDetails(String filenameIn, long lastModifiedIn, String md5sumIn) {
        relativeFilename = filenameIn;
        lastModified = lastModifiedIn;
        md5sum = md5sumIn;
        this.fileSize = 0;

    }

    public String getRelativeFilename()
    {
        return relativeFilename;
    }

    public void setRelativeFilename(String relativeFilenameIn)
    {
        this.relativeFilename = relativeFilenameIn;
    }

    public long getLastModified()
    {
        return lastModified;
    }

    public void setLastModified(long lastModifiedIn)
    {
        this.lastModified = lastModifiedIn;
    }

    public String getMd5sum()
    {
        return md5sum;
    }

    public void setMd5sum(String md5sumIn)
    {
        this.md5sum = md5sumIn;
    }
}
