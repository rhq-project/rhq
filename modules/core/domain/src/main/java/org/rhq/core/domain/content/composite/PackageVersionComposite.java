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
package org.rhq.core.domain.content.composite;

import java.io.Serializable;
import java.util.Date;
import org.rhq.core.domain.content.PackageCategory;

public class PackageVersionComposite implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int id;
    private final String packageTypeName;
    private final PackageCategory packageCategory;
    private final String packageName;
    private final String packageClassification;
    private final String displayName;
    private final String displayVersion;
    private final String architectureName;
    private final String shortDescription;
    private final String longDescription;
    private final String fileName;
    private final Number fileSize;
    private final String md5;
    private final String sha256;
    private final Date fileCreatedDate;
    private final String licenseName;
    private final String licenseVersion;
    private final Number packageBitsId; // will be not null if package version is locally loaded
    private final Number isPackageBitsInDatabase; // will be not null and greater than 0 if locally loaded in DB

    public PackageVersionComposite(int id, String packageTypeName, PackageCategory packageCategory, String packageName,
        String packageClassification, String displayName, String displayVersion, String architectureName,
        String shortDescription, String longDescription, String fileName, Number fileSize, String md5, String sha256,
        Date fileCreatedDate, String licenseName, String licenseVersion, Number packageBitsId,
        Number isPackageBitsInDatabase) {
        this.id = id;
        this.packageTypeName = packageTypeName;
        this.packageCategory = packageCategory;
        this.packageName = packageName;
        this.packageClassification = packageClassification;
        this.displayName = displayName;
        this.displayVersion = displayVersion;
        this.architectureName = architectureName;
        this.shortDescription = shortDescription;
        this.longDescription = longDescription;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.md5 = md5;
        this.sha256 = sha256;
        this.fileCreatedDate = fileCreatedDate;
        this.licenseName = licenseName;
        this.licenseVersion = licenseVersion;
        this.packageBitsId = packageBitsId;
        this.isPackageBitsInDatabase = isPackageBitsInDatabase;
    }

    public int getId() {
        return id;
    }

    public String getPackageTypeName() {
        return packageTypeName;
    }

    public PackageCategory getPackageCategory() {
        return packageCategory;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getPackageClassification() {
        return packageClassification;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDisplayVersion() {
        return displayVersion;
    }

    public String getArchitectureName() {
        return architectureName;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public String getLongDescription() {
        return longDescription;
    }

    public String getFileName() {
        return fileName;
    }

    public Number getFileSize() {
        return fileSize;
    }

    public String getMd5() {
        return md5;
    }

    public String getSha256() {
        return sha256;
    }

    public Date getFileCreatedDate() {
        return fileCreatedDate;
    }

    public String getLicenseName() {
        return this.licenseName;
    }

    public String getLicenseVersion() {
        return this.licenseVersion;
    }

    public Number getPackageBitsId() {
        return this.packageBitsId;
    }

    public boolean isPackageBitsAvailable() {
        return this.packageBitsId != null;
    }

    public boolean isPackageBitsInDatabase() {
        return (isPackageBitsInDatabase != null) && (isPackageBitsInDatabase.intValue() > 0);
    }
}