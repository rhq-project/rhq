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

import java.io.InputStream;
import java.util.Set;
import java.util.List;

import javax.ejb.Local;

import org.rhq.core.clientapi.server.content.ContentServiceResponse;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.ContentServiceRequest;
import org.rhq.core.domain.content.Package;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.transfer.ContentDiscoveryReport;
import org.rhq.core.domain.content.transfer.DeployPackagesResponse;
import org.rhq.core.domain.content.transfer.RemovePackagesResponse;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.content.transfer.DeployPackageStep;

/**
 * EJB interface to the server content subsystem.
 *
 * @author Jason Dobies
 */
@Local
public interface ContentManagerLocal {

    // Use case logic  --------------------------------------------

    /**
     * Deploys a package on the specified resource. Each installed package entry should be populated with the <code>
     * PackageVersion</code> being installed, along with the deployment configuration values if any. This method will
     * take care of populating the rest of the values in each installed package object.
     *
     * @param user       the user who is requesting the creation
     * @param resourceId identifies the resource against which the package will be deployed
     * @param packages   packages (with their deployment time configuration values) to deploy
     */
    void deployPackages(Subject user, int resourceId, Set<ResourcePackageDetails> packages);

    /**
     * Deletes the specified package from the resource.
     *
     * @param user                the user who is requesting the delete
     * @param resourceId          identifies the resource from which the packages should be deleted
     * @param installedPackageIds identifies all of the packages to be deleted
     */
    void deletePackages(Subject user, int resourceId, Set<Integer> installedPackageIds);

    /**
     * Deletes the specified package from the resource.
     *
     * @param user                the user who is requesting the delete
     * @param resourceIds         identifies the resources from which the packages should be deleted
     * @param installedPackageIds identifies all of the packages to be deleted
     */
    void deletePackages(Subject user, Set<Integer> resourceIds, Set<Integer> installedPackageIds);

    /**
     * Deploys packages on the specified resources. Each installed package entry should be populated with the <code>
     * PackageVersion</code> being installed, along with the deployment configuration values if any. This method will
     * take care of populating the rest of the values in each installed package object.
     *
     * @param user              the user who is requesting the creation
     * @param resourceIds       identifies the resources against which the package will be deployed
     * @param packageVersionIds packageVersions we want to install
     */
    void deployPackages(Subject user, Set<Integer> resourceIds, Set<Integer> packageVersionIds);

    /**
     * Requests the plugin load and send the actual bits for the specified package.
     *
     * @param user               the user who is requesting the update
     * @param resourceId         identifies the resource against which the package exists
     * @param installedPackageId id of the installed package to retrieve bits
     */
    void retrieveBitsFromResource(Subject user, int resourceId, int installedPackageId);

    /**
     * Requests the plugin translate the installation steps of the specified package.
     *
     * @param resourceId     resource against which the package is being installed
     * @param packageDetails package being installed 
     * @return list of deployment steps if the plugin specified them; <code>null</code> if they cannot be determined
     *         for this package
     * @throws Exception if there is an error either contacting the agent or in the plugin's generation of the steps 
     */
    List<DeployPackageStep> translateInstallationSteps(int resourceId, ResourcePackageDetails packageDetails)
        throws Exception;

    // The methods below should not be exposed to remote clients

    // Calls from the agent  --------------------------------------------

    /**
     * For documentation, see
     * {@link org.rhq.core.clientapi.server.content.ContentServerService#mergeDiscoveredPackages(org.rhq.core.domain.content.transfer.ContentDiscoveryReport)}
     * .
     */
    void mergeDiscoveredPackages(ContentDiscoveryReport report);

    /**
     * For documentation, see
     * {@link org.rhq.core.clientapi.server.content.ContentServerService#completeDeployPackageRequest(org.rhq.core.domain.content.transfer.DeployPackagesResponse)}
     * .
     */
    void completeDeployPackageRequest(DeployPackagesResponse response);

    /**
     * For documentation, see
     * {@link org.rhq.core.clientapi.server.content.ContentServerService#completeDeletePackageRequest(org.rhq.core.domain.content.transfer.RemovePackagesResponse)}
     * .
     */
    void completeDeletePackageRequest(RemovePackagesResponse response);

    /**
     * For documentation, see
     * {@link org.rhq.core.clientapi.server.content.ContentServerService#completeRetrievePackageBitsRequest(org.rhq.core.clientapi.server.content.ContentServiceResponse, java.io.InputStream)}
     * )}.
     */
    void completeRetrievePackageBitsRequest(ContentServiceResponse response, InputStream bitStream);

    /**
     * For documentation, see
     * {@link org.rhq.core.clientapi.server.content.ContentServerService#loadDependencies(int, java.util.Set)}
     */
    Set<ResourcePackageDetails> loadDependencies(int requestId, Set<PackageDetailsKey> keys);

    // Internal Utilities  --------------------------------------------

    /**
     * For internal use only - Adds a request entry to the database to track the deployment of a group of packages. This
     * will be performed in a new transaction.
     *
     * @param  resourceId resource against which the package request was executed
     * @param  username   user who made the request
     * @param  packages   packages being deployed in the request
     *
     * @return request entity after being persisted to the database (it's ID will be populated)
     */
    ContentServiceRequest createDeployRequest(int resourceId, String username, Set<ResourcePackageDetails> packages);

    /**
     * For internal use only - Adds a request entry to the database to track the deleting of currently installed
     * packages from the resource. This will be performed in a new transaction.
     *
     * @param  resourceId          resource against which the package request was executed
     * @param  username            user who made the request
     * @param  installedPackageIds identifies the installed packages that are to be deleted; ids in this list must be of
     *                             valid <code>InstalledPackage</code> objects on the resource
     *
     * @return request entity after being persisted to the database (it's ID will be populated)
     */
    ContentServiceRequest createRemoveRequest(int resourceId, String username, Set<Integer> installedPackageIds);

    /**
     * For internal use only - Adds a request entry to the database to track the request for a package's bits. This will
     * be performed in a new transaction.
     *
     * @param  resourceId         resource against which the package request was executed
     * @param  username           user who made the request
     * @param  installedPackageId package whose bits are being retrieved by the request; this must be the ID of a valid
     *                            <code>InstalledPackage</code> on the resource.
     *
     * @return request entity after being persisted to the database (it's ID will be populated)
     */
    ContentServiceRequest createRetrieveBitsRequest(int resourceId, String username, int installedPackageId);

    /**
     * For internal use only - Updates a persisted <code>ContentServiceRequest</code> in the case a failure is
     * encountered during one of the use case methods (i.e. create, delete).
     *
     * @param requestId identifies the previously persisted request
     * @param error     error encountered to cause the failure
     */
    void failRequest(int requestId, Throwable error);

    /**
     * For internal use only - Will check to see if any in progress content request jobs are taking too long to finish
     * and if so marks them as failed. This method will be periodically called by the Server.
     *
     * @param subject only the overlord may execute this system operation
     */
    void checkForTimedOutRequests(Subject subject);

    /**
     * Creates a new package version in the system. If the parent package (identified by the packageName parameter) does
     * not exist, it will be created. If a package version exists with the specified version ID, a new one will not be
     * created and the existing package version instance will be returned.
     *
     * @param  packageName    parent package name; uniquely identifies the package under which this version goes
     * @param  packageTypeId  identifies the type of package in case the general package needs to be created
     * @param  version        identifies the version to be create
     * @param  architectureId architecture of the newly created package version
     *
     * @return newly created package version if one did not exist; existing package version that matches these data if
     *         one was found
     */
    PackageVersion createPackageVersion(String packageName, int packageTypeId, String version, int architectureId,
        InputStream packageBitStream);

    /**
     * Very simple method that pesists the given package version within its own transaction.
     *
     * <p>This method is here to support {@link #persistOrMergePackageVersionSafely(PackageVersion)},
     * it is not meant for general consumption.</p>
     *
     * @param pv the package version to persist
     * 
     * @return the newly persisted package version, <code>null</code> if the package version already exists
     */
    PackageVersion persistPackageVersion(PackageVersion pv);

    /**
     * Finds, and if it doesn't exist, persists the package version.
     * If it already exists, it will return the merge the given PV with the object it found and return
     * the merged PV.
     * This performs its tasks safely; that is, it makes sure that no contraint violations occur
     * if the package version already exists.
     *
     * <p>This method is for a very specific use case - that is, when creating a package version
     * in a place where, concurrently, someone else might try to create the same package version.
     * It is not for general persisting/merging of package versions.</p>
     *
     * @param pv the package version to find and possibly persist to the database
     *
     * @return the package version that was found/persisted
     */
    PackageVersion persistOrMergePackageVersionSafely(PackageVersion pv);

    /**
     * Very simple method that pesists the given package within its own transaction.
     *
     * <p>This method is here to support {@link #persistOrMergePackageSafely(Package)},
     * it is not meant for general consumption.</p>
     * 
     * @param pkg the package to persist
     * 
     * @return the newly persisted package, <code>null</code> if the package already exists
     */
    Package persistPackage(Package pkg);

    /**
     * Finds, and if it doesn't exist, persists the package.
     * If it already exists, it will return the merge the given package with the object it found and return
     * the merged package.
     * This performs its tasks safely; that is, it makes sure that no contraint violations occur
     * if the package already exists.
     *
     * <p>This method is for a very specific use case - that is, when creating a package
     * in a place where, concurrently, someone else might try to create the same package.
     * It is not for general persisting/merging of packages.</p>
     * 
     * @param pkg the package to find and possibly persist to the database
     *
     * @return the package that was found/persisted
     */
    Package persistOrMergePackageSafely(Package pkg);
}