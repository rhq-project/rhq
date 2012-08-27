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
package org.rhq.enterprise.server.content;

import java.util.List;

import javax.ejb.Remote;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.Architecture;
import org.rhq.core.domain.content.InstalledPackage;
import org.rhq.core.domain.content.Package;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.composite.PackageAndLatestVersionComposite;
import org.rhq.core.domain.content.composite.PackageTypeAndVersionFormatComposite;
import org.rhq.core.domain.criteria.InstalledPackageCriteria;
import org.rhq.core.domain.criteria.PackageCriteria;
import org.rhq.core.domain.criteria.PackageVersionCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.resource.ResourceTypeNotFoundException;

/**
 * @author Jay Shaughnessy
 */

@Remote
public interface ContentManagerRemote {

    /**
     * Creates a new package version in the system. If the parent package (identified by the packageName parameter) does
     * not exist, it will be created. If a package version exists with the specified version ID, a new one will not be
     * created and the existing package version instance will be returned.
     *
     * @param subject        The logged in subject
     * @param packageName    parent package name; uniquely identifies the package under which this version goes
     * @param packageTypeId  identifies the type of package in case the general package needs to be created
     * @param version        identifies the version to be create
     * @param architectureId architecture of the newly created package version. If null then no architecture restriction.
     *
     * @return newly created package version if one did not exist; existing package version that matches these data if
     *         one was found
     */
    PackageVersion createPackageVersion(Subject subject, String packageName, int packageTypeId, String version,
        Integer architectureId, byte[] packageBytes);

    /**
     * Creates a new package version in the system. If the parent package (identified by the packageName parameter) does
     * not exist, it will be created. If a package version exists with the specified version ID, a new one will not be
     * created and the existing package version instance will be returned.
     *
     * @param subject        The logged in subject
     * @param packageName    parent package name; uniquely identifies the package under which this version goes
     * @param packageTypeId  identifies the type of package in case the general package needs to be created
     * @param version        identifies the version to be create
     * @param architectureId architecture of the newly created package version. If null then no architecture restriction.
     *
     * @return newly created package version if one did not exist; existing package version that matches these data if
     *         one was found
     */
    PackageVersion createPackageVersionWithDisplayVersion(Subject subject, String packageName, int packageTypeId,
        String version, String displayVersion, Integer architectureId, byte[] packageBytes);

    /**
     * Deletes the specified package from the resource.
     *
     * @param subject             The logged in subject
     * @param resourceId          identifies the resource from which the packages should be deleted
     * @param installedPackageIds identifies all of the packages to be deleted
     */
    void deletePackages(Subject subject, int resourceId, int[] installedPackageIds, String requestNotes);

    /**
     * Deletes the specified PackageVersion from the system.  The PackageVersion must be an orphan to be
     * deleted. If it is referenced by a content source, repo or installed package it must be removed via the
     * higher level construct and this call will have no effect.
     *
     * @param subject             The logged in subject
     * @param packageVersionId    The PackageVersion to delete.
     */
    public void deletePackageVersion(//
        Subject subject, int resourceId);

    /**
     * Deploys packages on the specified resources. Each installed package entry should be populated with the <code>
     * PackageVersion</code> being installed, along with the deployment configuration values if any. This method will
     * take care of populating the rest of the values in each installed package object.
     *
     * @param subject           The logged in subject
     * @param resourceIds       identifies the resources against which the package will be deployed
     * @param packageVersionIds packageVersions we want to install
     */
    @Deprecated
    void deployPackages(Subject subject, int[] resourceIds, int[] packageVersionIds);

    /**
     * Deploys packages on the specified resources. Each installed package entry should be populated with the <code>
     * PackageVersion</code> being installed, along with the deployment configuration values if any. This method will
     * take care of populating the rest of the values in each installed package object.
     *
     * @param subject           The logged in subject
     * @param resourceIds       identifies the resources against which the package will be deployed
     * @param packageVersionIds packageVersions we want to install
     * @param requestNotes      request notes
     */
    void deployPackagesWithNote(Subject subject, int[] resourceIds, int[] packageVersionIds, String requestNotes);

    /**
     * Returns all architectures known to the system.
     *
     * @param  subject The logged in subject
     * @return list of all architectures in the database
     */
    List<Architecture> findArchitectures(Subject subject);

    /**
     * This gets the package types that can be deployed to the given resource. It is a function of the resource
     * type of the resource.
     *
     * @param subject          The logged in subject
     * @param resourceTypeName The resource type in question
     *
     * @return The requested list of package types. Can be empty.
     */
    List<PackageType> findPackageTypes(Subject subject, String resourceTypeName, String pluginName)
        throws ResourceTypeNotFoundException;

    /**
     * This re tries to find a package type of given name defined by the resource type
     * provided. 
     * <p>
     * The resource type id can be null, in which case only the serverside defined package types
     * are searched for.
     * 
     * @param subject the authenticated user
     * @param resourceTypeId the id of the resource type associated with the package type or null  if only server-side package types should be searched for 
     * @param packageTypeName the name of the package type to find
     * @return
     */
    PackageType findPackageType(Subject subject, Integer resourceTypeId, String packageTypeName);

    /**
     * Similar to {@link #findPackageType(Subject, Integer, String)} but
     * returns the package type along with the version format specification.
     * 
     * @param subject
     * @param resourceTypeId
     * @param packageTypeName
     * @return
     */
    PackageTypeAndVersionFormatComposite findPackageTypeWithVersionFormat(Subject subject, Integer resourceTypeId,
        String packageTypeName);

    /**
     * @param subject
     * @param criteria {@link InstalledPackageCriteria}
     * @return InstalledPackages for the criteria
     */
    PageList<InstalledPackage> findInstalledPackagesByCriteria(Subject subject, InstalledPackageCriteria criteria);

    /**
     * If a resourceId filter is not set via {@link PackageVersionCriteria.addFilterResourceId()} then
     * this method requires InventoryManager permissions. When set the user must have permission to view
     * the resource.
     * 
     * @param subject
     * @param criteria
     * @return Installed PackageVersions for the resource
     * @throws IllegalArgumentException for invalid resourceId filter
     */
    PageList<PackageVersion> findPackageVersionsByCriteria(Subject subject, PackageVersionCriteria criteria);

    /**
     * If the criteria object filters on repo id, the subject needs to be able to 
     * access that repo. If there is no filter on repos, the subject needs to have 
     * MANAGE_REPOSITORIES permission.
     * 
     * @param subject
     * @param criteria
     * @return
     */
    PageList<Package> findPackagesByCriteria(Subject subject, PackageCriteria criteria);

    /**
     * Akin to {@link #findPackagesByCriteria(Subject, PackageCriteria)} but also
     * determines the latest version of the returned packages.
     * <p>
     * The provided criteria has to be limited to a specific repo using {@link PackageCriteria#addFilterRepoId(Integer)}.
     * 
     * @param subject
     * @param criteria
     * @return
     * @throws IllegalArgumentException if the criteria doesn't define a repo filter
     */
    PageList<PackageAndLatestVersionComposite> findPackagesWithLatestVersion(Subject subject, PackageCriteria criteria);

    /**
     * For a resource that is content-backed (aka package-backed), this call will return InstalledPackage information
     * for the backing content (package).
     *
     * @param resourceId a valid resource
     * @return The InstalledPackage object for the content-packed resource. Or null for non-existent or non-package backed resource.
     */
    InstalledPackage getBackingPackageForResource(Subject subject, int resourceId);

    /**
     * This can be a dangerous call for large packages as the entire package will attempt to be loaded.
     * @param subject
     * @param resourceId
     * @param installedPackageId
     * @return the package bytes
     */
    byte[] getPackageBytes(Subject subject, int resourceId, int installedPackageId);
}