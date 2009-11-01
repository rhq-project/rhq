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
package org.rhq.core.clientapi.server.plugin.content;

import java.util.HashSet;
import java.util.Set;

/**
 * Indicates the results of performing a syncronize with a package source. This object carries the diff information
 * necessary to update the server's current knowledge of the source's packages.
 *
 * @author Jason Dobies
 */
public class PackageSyncReport {

    /**
     * Set of packages that have been added to the external package source that are not yet known to the server. A new
     * version of a package constitutes an entry in this list.
     */
    private final Set<ContentProviderPackageDetails> newPackages = new HashSet<ContentProviderPackageDetails>();

    /**
     * Set of packages that are already known to the server and whose metadata has changed in the external package
     * source. The data in each of these packages will be merged with the server's existing knowledge of the package.
     * Packages should only be included in this list if the name and version are the same as a package in the server;
     * new versions of a package belong in the <code>newPackages</code> list.
     */
    private final Set<ContentProviderPackageDetails> updatedPackages = new HashSet<ContentProviderPackageDetails>();

    /**
     * Set of packages that are known to the server but no longer in the external source.
     */
    private final Set<ContentProviderPackageDetails> deletedPackages = new HashSet<ContentProviderPackageDetails>();

    /**
     * The synchronization summary. Free form textual report.
     */
    private String summary;

    public Set<ContentProviderPackageDetails> getNewPackages() {
        return newPackages;
    }

    /**
     * Add to the set of packages that have been added to the external package source that are not yet known to the
     * server. A new version of a package constitutes an entry in this list.
     *
     * @param newPackage contains the details of the new package to be added; should not be <code>null</code>
     */
    public void addNewPackage(ContentProviderPackageDetails newPackage) {
        this.newPackages.add(newPackage);
    }

    public Set<ContentProviderPackageDetails> getUpdatedPackages() {
        return updatedPackages;
    }

    /**
     * Add to the set of packages that are already known to the server and whose metadata has changed in the external
     * package source. The data in this package will be merged with the server's existing knowledge of the package.
     * Packages should only be added to this updated list if the name and version are the same as a package in the
     * server; new versions of a package belong in the
     * {@link #addNewPackage(ContentProviderPackageDetails) new packages list}.
     *
     * @param updatedPackage contains the new information of a package that was updated; should not be
     *                       <code>null</code>
     */
    public void addUpdatedPackage(ContentProviderPackageDetails updatedPackage) {
        this.updatedPackages.add(updatedPackage);
    }

    public Set<ContentProviderPackageDetails> getDeletedPackages() {
        return deletedPackages;
    }

    /**
     * Add to the set of packages that are known to the server but no longer in the external source.
     *
     * @param deletedPackage details object identifying the package that was removed; should not be <code>null</code>
     */
    public void addDeletePackage(ContentProviderPackageDetails deletedPackage) {
        this.deletedPackages.add(deletedPackage);
    }

    public String getSummary() {
        return summary;
    }

    /**
     * The synchronization summary. Free form textual report that will be stored with the content source sync report.
     *
     * @param summary should not be <code>null</code>
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