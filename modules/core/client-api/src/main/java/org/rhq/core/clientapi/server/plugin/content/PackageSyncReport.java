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
package org.rhq.core.clientapi.server.plugin.content;

import java.util.HashSet;
import java.util.Set;

/**
 * Indicates the results of performing a syncronize with a content source. This object carries the diff information
 * necessary to update the server's current knowledge of the source's packages.
 *
 * @author Jason Dobies
 */
public class PackageSyncReport {
    // Attributes  --------------------------------------------

    /**
     * Set of packages that have been added to the external package source that are not yet known to the server. A new
     * version of a package constitutes an entry in this list.
     */
    private final Set<ContentSourcePackageDetails> newPackages = new HashSet<ContentSourcePackageDetails>();

    /**
     * Set of packages that are already known to the server and whose metadata has changed in the external package
     * source. The data in each of these packages will be merged with the server's existing knowledge of the package.
     * Packages should only be included in this list if the name and version are the same as a package in the server;
     * new versions of a package belong in the <code>newPackages</code> list.
     */
    private final Set<ContentSourcePackageDetails> updatedPackages = new HashSet<ContentSourcePackageDetails>();

    /**
     * Set of packages that are known to the server but no longer in the external source.
     */
    private final Set<ContentSourcePackageDetails> deletedPackages = new HashSet<ContentSourcePackageDetails>();

    /**
     * The synchronization summary. Free form textual report.
     */
    private String summary;

    // Public  --------------------------------------------

    public Set<ContentSourcePackageDetails> getNewPackages() {
        return newPackages;
    }

    /**
     * Add to the set of packages that have been added to the external package source that are not yet known to the
     * server. A new version of a package constitutes an entry in this list.
     */
    public void addNewPackage(ContentSourcePackageDetails newPackage) {
        this.newPackages.add(newPackage);
    }

    public Set<ContentSourcePackageDetails> getUpdatedPackages() {
        return updatedPackages;
    }

    /**
     * Add to the set of packages that are already known to the server and whose metadata has changed in the external
     * package source. The data in this package will be merged with the server's existing knowledge of the package.
     * Packages should only be added to this updated list if the name and version are the same as a package in the
     * server; new versions of a package belong in the
     * {@link #addNewPackage(ContentSourcePackageDetails) new packages list}.
     */
    public void addUpdatedPackage(ContentSourcePackageDetails updatedPackage) {
        this.updatedPackages.add(updatedPackage);
    }

    public Set<ContentSourcePackageDetails> getDeletedPackages() {
        return deletedPackages;
    }

    /**
     * Add to the set of packages that are known to the server but no longer in the external source.
     */
    public void addDeletePackage(ContentSourcePackageDetails deletedPackage) {
        this.deletedPackages.add(deletedPackage);
    }

    public String getSummary() {
        return summary;
    }

    /**
     * The synchronization summary. Free form textual report that will be stored with the content source sync report.
     */
    public void setSummary(String summary) {
        this.summary = summary;
    }

    @Override
    public String toString() {
        return "PackageSyncReport: new=[" + newPackages.size() + "], updated=[" + updatedPackages.size()
            + "], deleted=[" + deletedPackages.size() + "]";
    }
}