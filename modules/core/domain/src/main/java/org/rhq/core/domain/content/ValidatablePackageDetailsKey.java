/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.core.domain.content;

/**
 * This is a mutable version of the {@link PackageDetailsKey} used
 * for validation purposes.
 *
 * @author Lukas Krejci
 */
public class ValidatablePackageDetailsKey {
    private String name;
    private String version;
    private String packageTypeName;
    private String architectureName;
    
    public ValidatablePackageDetailsKey() {
        
    }

    public ValidatablePackageDetailsKey(String name, String version, String packageTypeName, String architectureName) {
        this.name = name;
        this.version = version;
        this.packageTypeName = packageTypeName;
        this.architectureName = architectureName;
    }
    
    public ValidatablePackageDetailsKey(PackageDetailsKey key) {
        this.name = key.getName();
        this.version = key.getVersion();
        this.packageTypeName = key.getPackageTypeName();
        this.architectureName = key.getArchitectureName();
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * @param version the version to set
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * @return the packageTypeName
     */
    public String getPackageTypeName() {
        return packageTypeName;
    }

    /**
     * @param packageTypeName the packageTypeName to set
     */
    public void setPackageTypeName(String packageTypeName) {
        this.packageTypeName = packageTypeName;
    }

    /**
     * @return the architectureName
     */
    public String getArchitectureName() {
        return architectureName;
    }

    /**
     * @param architectureName the architectureName to set
     */
    public void setArchitectureName(String architectureName) {
        this.architectureName = architectureName;
    }
}
