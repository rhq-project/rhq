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
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.Architecture;
import org.rhq.core.domain.content.InstalledPackage;
import org.rhq.core.domain.content.Package;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.composite.PackageAndLatestVersionComposite;
import org.rhq.core.domain.criteria.InstalledPackageCriteria;
import org.rhq.core.domain.criteria.PackageCriteria;
import org.rhq.core.domain.criteria.PackageVersionCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.resource.ResourceTypeNotFoundException;
import org.rhq.enterprise.server.system.ServerVersion;

/**
 * @author Jay Shaughnessy
 */
@SOAPBinding(style = SOAPBinding.Style.DOCUMENT)
@WebService(targetNamespace = ServerVersion.namespace)
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
    @WebMethod
    PackageVersion createPackageVersion( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "packageName") String packageName, //
        @WebParam(name = "packageTypeId") int packageTypeId, //
        @WebParam(name = "version") String version, //
        @WebParam(name = "architectureId") Integer architectureId, //
        @WebParam(name = "packageBytes") byte[] packageBytes);

    /**
     * Deletes the specified package from the resource.
     *
     * @param subject             The logged in subject
     * @param resourceId          identifies the resource from which the packages should be deleted
     * @param installedPackageIds identifies all of the packages to be deleted
     */
    @WebMethod
    void deletePackages( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceId") int resourceId, //
        @WebParam(name = "installedPackages") int[] installedPackageIds, //
        @WebParam(name = "requestNotes") String requestNotes);

    /**
     * Deletes the specified PackageVersion from the system.  The PackageVersion must be an orphan to be
     * deleted. If it is referenced by a content source, repo or installed package it must be removed via the
     * higher level construct and this call will have no effect.
     *
     * @param subject             The logged in subject
     * @param packageVersionId    The PackageVersion to delete.
     */
    @WebMethod
    public void deletePackageVersion(//
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceId") int resourceId);

    /**
     * Deploys packages on the specified resources. Each installed package entry should be populated with the <code>
     * PackageVersion</code> being installed, along with the deployment configuration values if any. This method will
     * take care of populating the rest of the values in each installed package object.
     *
     * @param subject           The logged in subject
     * @param resourceIds       identifies the resources against which the package will be deployed
     * @param packageVersionIds packageVersions we want to install
     */
    @WebMethod
    void deployPackages( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceIds") int[] resourceIds, //
        @WebParam(name = "packageVersionIds") int[] packageVersionIds);

    /**
     * Returns all architectures known to the system.
     *
     * @param  subject The logged in subject
     * @return list of all architectures in the database
     */
    @WebMethod
    List<Architecture> findArchitectures( //
        @WebParam(name = "subject") Subject subject);

    /**
     * This gets the package types that can be deployed to the given resource. It is a function of the resource
     * type of the resource.
     *
     * @param subject          The logged in subject
     * @param resourceTypeName The resource type in question
     *
     * @return The requested list of package types. Can be empty.
     */
    @WebMethod
    List<PackageType> findPackageTypes( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceTypeName") String resourceTypeName, //
        @WebParam(name = "pluginName") String pluginName) throws ResourceTypeNotFoundException;

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
    @WebMethod
    PackageType findPackageType(
        @WebParam(name = "subject") Subject subject,
        @WebParam(name = "resourceTypeId") Integer resourceTypeId,
        @WebParam(name = "packageTypeName") String packageTypeName
        );
    
    /**
     * @param subject
     * @param criteria {@link InstalledPackageCriteria}
     * @return InstalledPackages for the criteria
     */
    @WebMethod
    PageList<InstalledPackage> findInstalledPackagesByCriteria( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "criteria") InstalledPackageCriteria criteria);

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
    @WebMethod
    PageList<PackageVersion> findPackageVersionsByCriteria( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "criteria") PackageVersionCriteria criteria);

    /**
     * If the criteria object filters on repo id, the subject needs to be able to 
     * access that repo. If there is no filter on repos, the subject needs to have 
     * MANAGE_REPOSITORIES permission.
     * 
     * @param subject
     * @param criteria
     * @return
     */
    @WebMethod
    PageList<Package> findPackagesByCriteria(
        @WebParam(name = "subject") Subject subject,
        @WebParam(name = "criteria") PackageCriteria criteria);
    
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
    @WebMethod    
    PageList<PackageAndLatestVersionComposite> findPackagesWithLatestVersion(
        @WebParam(name = "subject") Subject subject, 
        @WebParam(name = "criteria") PackageCriteria criteria);
    
    /**
     * For a resource that is content-backed (aka package-backed), this call will return InstalledPackage information
     * for the backing content (package).
     *
     * @param resourceId a valid resource
     * @return The InstalledPackage object for the content-packed resource. Or null for non-existent or non-package backed resource.
     */
    @WebMethod
    InstalledPackage getBackingPackageForResource( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceId") int resourceId);

    /**
     * This can be a dangerous call for large packages as the entire package will attempt to be loaded.
     * @param user
     * @param resourceId
     * @param installedPackageId
     * @return the package bytes
     */
    @WebMethod
    byte[] getPackageBytes(@WebParam(name = "subject") Subject user, @WebParam(name = "resourceId") int resourceId,
        @WebParam(name = "installedPackageId") int installedPackageId);

}