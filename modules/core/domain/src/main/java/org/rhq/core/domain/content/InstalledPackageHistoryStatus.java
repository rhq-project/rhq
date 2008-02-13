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
package org.rhq.core.domain.content;

/**
 * Indicates if an {@link InstalledPackage} is in use on the resource or is a historical entry.
 *
 * @author Jason Dobies
 */
public enum InstalledPackageHistoryStatus {
    /**
     * The package installation represented by the installed package object encountered an error.
     */
    FAILED("Installation Failed", "An attempt to install the package failed."),

    /**
     * Indicates the installed package instance is the currently installed version.
     */
    INSTALLED("Currently Installed", "The package is currently installed on the resource."),

    /**
     * The package is in the process of being deployed out to a resource.
     */
    BEING_INSTALLED("Installation In Progress",
        "The package is currently in the process of being installed on the resource."),

    /**
     * Indicates the package was explicitly deleted by the user.
     */
    DELETED("Deleted", "The package was explicitly deleted from the resource by a user."),

    /**
     * The package is in the process of being deployed out to a resource.
     */
    BEING_DELETED("Removal In Progress", "The package is currently in the process of being removed from the resource."),

    /**
     * A request to retrieve the bits for a package was successful.
     */
    RETRIEVED("Retrieved Content", "The package bits were successfully retrieved from the agent."),

    /**
     * A request to retrieve the bits for a package is in progress.
     */
    BEING_RETRIEVED("Bits Retrieval In Progress", "The package bits are being retrieved from the agent.");

    // Attributes  --------------------------------------------

    private String displayName;
    private String description;

    // Constructors  --------------------------------------------

    InstalledPackageHistoryStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    // Public  --------------------------------------------

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}