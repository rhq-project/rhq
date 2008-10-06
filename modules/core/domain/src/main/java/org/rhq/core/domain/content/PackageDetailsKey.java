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
package org.rhq.core.domain.content;

import java.io.Serializable;

/**
 * Contains the data necessary to describe a specific version of a package.
 *
 * @author Jason Dobies
 */
public class PackageDetailsKey implements Serializable {
    // Constants  --------------------------------------------

    private static final long serialVersionUID = 1L;

    // Package Identification Attributes  --------------------------------------------

    /**
     * Programmatic name of the package. This will be used to identify a package in the scope of a particular content
     * source. This may not be unique, but the combination of name, major version, and minor version should be unique in
     * the list of packages in a content source.
     */
    private final String name;

    /**
     * The version of the package. The format of this attribute will vary based on the package type. Splitting the
     * version into two strings will allow us to query based on the severity (for lack of a better word) of the version
     * differences.
     */
    private final String version;

    /**
     * Fully qualified name of the package type this package;
     */
    private final String packageTypeName;

    /**
     * Architecture of the package.
     */
    private final String architectureName;

    // Constructors  --------------------------------------------

    public PackageDetailsKey(String name, String version, String packageTypeName, String architectureName) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("name cannot be null or zero length");
        }

        if (version == null || version.length() == 0) {
            throw new IllegalArgumentException("version cannot be null or zero length");
        }

        if (packageTypeName == null || packageTypeName.length() == 0) {
            throw new IllegalArgumentException("packageTypeName cannot be null or zero length");
        }

        if (architectureName == null || architectureName.length() == 0) {
            throw new IllegalArgumentException("architectureName cannot be null or zero length");
        }

        this.name = name;
        this.version = version;
        this.packageTypeName = packageTypeName;
        this.architectureName = architectureName;
    }

    // Public  --------------------------------------------

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getPackageTypeName() {
        return packageTypeName;
    }

    public String getArchitectureName() {
        return architectureName;
    }

    @Override
    public String toString() {
        return "PackageDetailsKey[Name=" + name + ", Version=" + version + " Arch=" + architectureName + " Type="
            + packageTypeName + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + name.hashCode();
        result = (prime * result) + version.hashCode();
        result = (prime * result) + packageTypeName.hashCode();
        result = (prime * result) + architectureName.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (!(obj instanceof PackageDetailsKey)) {
            return false;
        }

        final PackageDetailsKey other = (PackageDetailsKey) obj;

        if (!name.equals(other.name)) {
            return false;
        }

        if (!version.equals(other.version)) {
            return false;
        }

        if (!packageTypeName.equals(other.packageTypeName)) {
            return false;
        }

        if (!architectureName.equals(other.architectureName)) {
            return false;
        }

        return true;
    }
}