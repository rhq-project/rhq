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
package org.rhq.enterprise.gui.content;

import org.rhq.core.domain.content.Package;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;

/**
 * Utilities used by the UI for dealing with content related pages.
 *
 * @author Jason Dobies
 */
public class ContentUtils {

    /**
     * Translates the domain object package version, which will typically be returned for use in the UI, into the
     * transfer objects that will be send to the SLSB layer calls, such as deploy package and translate steps.
     * 
     * @param packageVersion populated domain entity for a package version
     * @return transfer object populated with the necessary data to request package deployment related activities
     */
    public static ResourcePackageDetails toResourcePackageDetails(PackageVersion packageVersion) {
        Package pkg = packageVersion.getGeneralPackage();
        PackageType pkgType = pkg.getPackageType();

        PackageDetailsKey key = new PackageDetailsKey(pkg.getName(), packageVersion.getVersion(), pkgType.getName(),
            packageVersion.getArchitecture().getName());

        ResourcePackageDetails details = new ResourcePackageDetails(key);
        details.setClassification(pkg.getClassification());
        details.setDisplayName(packageVersion.getDisplayName());
        details.setDisplayVersion(packageVersion.getDisplayVersion());
        //        details.setExtraProperties(packageVersion.getExtraProperties());
        details.setFileCreatedDate(packageVersion.getFileCreatedDate());
        details.setFileName(packageVersion.getFileName());
        details.setFileSize(packageVersion.getFileSize());
        details.setLicenseName(packageVersion.getLicenseName());
        details.setLicenseVersion(packageVersion.getLicenseVersion());
        details.setLongDescription(packageVersion.getLongDescription());
        details.setMD5(packageVersion.getMD5());
        details.setMetadata(packageVersion.getMetadata());
        details.setSHA256(packageVersion.getSHA256());
        details.setShortDescription(packageVersion.getShortDescription());

        return details;
    }
}
