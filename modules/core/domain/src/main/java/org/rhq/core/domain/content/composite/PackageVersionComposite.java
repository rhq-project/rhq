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

import org.rhq.core.domain.content.PackageCategory;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.configuration.Configuration;

import java.io.Serializable;

public class PackageVersionComposite implements Serializable {

    private static final long serialVersionUID = 1L;

    private final PackageVersion packageVersion;
    private final Configuration extraProperties;
    private final String packageTypeName;
    private final PackageCategory packageCategory;
    private final String packageName;
    private final String architectureName;
    private final String packageClassification;
    private final Number packageBitsId; // will be not null if package version is locally loaded
    private final Number isPackageBitsInDatabase; // will be not null and greater than 0 if locally loaded in DB

    public PackageVersionComposite(PackageVersion packageVersion, String packageTypeName,
        PackageCategory packageCategory, String packageName, String architectureName, String packageClassification) {
        this(packageVersion, packageTypeName, packageCategory, packageName, architectureName, packageClassification,
            0, 0);
    }

    public PackageVersionComposite(PackageVersion packageVersion, String packageTypeName,
        PackageCategory packageCategory, String packageName, String architectureName, String packageClassification,
        Number packageBitsId, Number isPackageBitsInDatabase) {
        this(packageVersion, null, packageTypeName, packageCategory, packageName, architectureName,
            packageClassification, packageBitsId, isPackageBitsInDatabase);
    }

    public PackageVersionComposite(PackageVersion packageVersion, Configuration extraProperties, String packageTypeName,
        PackageCategory packageCategory, String packageName, String architectureName, String packageClassification,
        Number packageBitsId, Number isPackageBitsInDatabase) {
        this.packageVersion = packageVersion;
        this.extraProperties = extraProperties;
        this.packageTypeName = packageTypeName;
        this.packageCategory = packageCategory;
        this.packageName = packageName;
        this.architectureName = architectureName;
        this.packageClassification = packageClassification;
        this.packageBitsId = packageBitsId;
        this.isPackageBitsInDatabase = isPackageBitsInDatabase;
    }

    public PackageVersion getPackageVersion() {
        return packageVersion;
    }

    public Configuration getExtraProperties() {
        return extraProperties;
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

    public String getArchitectureName() {
        return architectureName;
    }

    public String getPackageClassification() {
        return packageClassification;
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