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

/**
 * Represents the major supported categories for packages.
 *
 * @author Jason Dobies
 */
public enum PackageCategory {
    // Enumerated Values  --------------------------------------------

    /**
     * Executable scripts that may be edited prior to execution.
     */
    EXECUTABLE_SCRIPT("Executable Script"),

    /**
     * Executables that are able to natively run on a platform
     */
    EXECUTABLE_BINARY("Executable Binary"),

    /**
     * Represents a deployable to the parent resource that can be pushed as arbitrary content
     */
    DEPLOYABLE("Deployable"),

    /**
     * Represents configuration data for a managed resource
     */
    CONFIGURATION("Configuration"),

    /**
     * A log file
     */
    LOG("Log");

    // Attributes  --------------------------------------------

    private final String displayName;

    // Constructors  --------------------------------------------

    PackageCategory(String displayName) {
        this.displayName = displayName;
    }

    // Public  --------------------------------------------

    public String getName() {
        return name();
    }

    // Object Overridden Methods  --------------------------------------------

    public String toString() {
        return this.displayName;
    }
}