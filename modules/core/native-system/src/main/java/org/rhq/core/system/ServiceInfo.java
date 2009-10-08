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
package org.rhq.core.system;

import java.util.Arrays;

/**
 * Encapsulates information about a known service.
 *
 * @author John Mazzitelli
 */
public class ServiceInfo {
    private final String name;
    private final String displayName;
    private final String description;
    private final String binaryPath;
    private final String[] dependencies;

    public ServiceInfo(String name, String displayName, String description, String binaryPath, String[] dependencies) {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.binaryPath = binaryPath;
        this.dependencies = (dependencies != null) ? dependencies : new String[0];
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getBinaryPath() {
        return binaryPath;
    }

    public String[] getDependencies() {
        return dependencies;
    }

    /**
     * A service's name makes it unique.
     *
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return getName().hashCode();
    }

    /**
     * Two {@link ServiceInfo} objects are equal if their {@link #getName() names} are the same.
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || !(obj instanceof ServiceInfo)) {
            return false;
        }

        return this.getName().equals(((ServiceInfo) obj).getName());
    }

    public String toString() {
        StringBuffer s = new StringBuffer("service: ");

        s.append("name=[");
        s.append(getName());
        s.append("], display-name=[");
        s.append(getDisplayName());
        s.append("], description=[");
        s.append(getDescription());
        s.append("], binary-path=[");
        s.append(getBinaryPath());
        s.append("], dependencies=");
        s.append(Arrays.asList(getDependencies()));
        s.append("]");

        return s.toString();
    }
}