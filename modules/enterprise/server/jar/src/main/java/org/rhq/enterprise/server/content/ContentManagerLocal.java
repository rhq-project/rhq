/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.enterprise.server.content;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;

import org.rhq.core.clientapi.server.content.ContentDiscoveryReport;
import org.rhq.core.clientapi.server.content.ContentServiceResponse;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.Architecture;
import org.rhq.core.domain.content.ContentServiceRequest;
import org.rhq.core.domain.content.InstalledPackage;
import org.rhq.core.domain.content.Package;
import org.rhq.core.domain.content.PackageBits;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.transfer.DeployPackageStep;
import org.rhq.core.domain.content.transfer.DeployPackagesResponse;
import org.rhq.core.domain.content.transfer.RemovePackagesResponse;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.resource.Resource;

/**
 * EJB interface to the server content subsystem.
 *
 * @author Jason Dobies
 */
@Local
public interface ContentManagerLocal extends ContentManagerRemote {

    // Use case logic  --------------------------------------------

    /**
     * This is currently ignored as the file size is computed
     * upon persist.
     */
    public static final String UPLOAD_FILE_SIZE = "fileSize";

    public static final String UPLOAD_FILE_INSTALL_DATE = "fileInstallDate";

    /**
     * This doesn't seem to serve any purpose.
     */
    public static final String UPLOAD_OWNER = "owner";

    public static final String UPLOAD_FILE_NAME = "fileName";

    public static final String UPLOAD_MD5 = "md5";

    public static final String UPLOAD_DISPLAY_VERSION = "displayVersion";

    /**
     * This is currently ignored as the SHA is computed upon
     * persist.
     */
    public static final String UPLOAD_SHA256 = "sha256";

    /**
     * Deploys a package on the specified resource. Each installed package entry should be populated with the <code>
     * PackageVersion</code> being installed, along with the deployment configuration values if any. This method will
     * take care of populating the rest of the values in each installed package object.
     *
     * @param user         the user who is requesting the creation
     * @param resourceId   identifies the resource against which the package will be deployed
     * @param packages     packages (with their deployment time configuration values) to deploy
     * @param requestNotes user-specified notes on what is contained in this request
     */
    void deployPackages(Subject user, int resourceId, Set<ResourcePackageDetails> packages, String requestNotes);

    /**
     * Deletes the specified package from the resource.
     *
     * @param user                the user who is requesting the delete
     * @param resourceIds         identifies the resources from which the packages should be deleted
     * @param installedPackageIds identifies all of the packages to be deleted
     */
    void deletePackages(Subject user, int[] resourceIds, int[] installedPackageIds);

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
     * {@link org.rhq.core.clientapi.server.content.ContentServerService#mergeDiscoveredPackages(org.rhq.core.clientapi.server.content.ContentDiscoveryReport)}
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
     * @param  resourceId   resource against which the package request was executed
     * @param  username     user who made the request
     * @param  packages     packages being deployed in the request
     * @param  requestNotes user-specified notes on what the request entails
     *
     * @return request entity after being persisted to the database (it's ID will be populated)
     */
    ContentServiceRequest createDeployRequest(int resourceId, String username, Set<ResourcePackageDetails> packages,
        String requestNotes);

    /**
     * For internal use only - Adds a request entry to the database to track the deleting of currently installed
     * packages from the resource. This will be performed in a new transaction.
     *
     * @param  resourceId          resource against which the package request was executed
     * @param  username            user who made the request
     * @param  installedPackageIds identifies the installed packages that are to be deleted; ids in this list must be of
     *                             valid <code>InstalledPackage</code> objects on the resource
     * @param  requestNotes        user-specified notes on what the request entails
     *
     * @return request entity after being persisted to the database (it's ID will be populated)
     */
    ContentServiceRequest createRemoveRequest(int resourceId, String username, int[] installedPackageIds,
        String requestNotes);

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
     * @param subject         the user requesting the package creation
     * @param  packageName    parent package name; uniquely identifies the package under which this version goes
     * @param  packageTypeId  identifies the type of package in case the general package needs to be created
     * @param  version        identifies the version to be create
     * @param displayVersion  package display version
     * @param  architectureId architecture of the newly created package version
     *
     * @return newly created package version if one did not exist; existing package version that matches these data if
     *         one was found
     */
    PackageVersion createPackageVersionWithDisplayVersion(Subject subject, String packageName, int packageTypeId,
        String version, String displayVersion, int architectureId, InputStream packageBitStream);

    /**
     * This method is essentially the same as {@link #createPackageVersion(Subject, String, int, String, int, InputStream)}
     * but will update the package bits if a package version with the provided identification already exists.
     *
     * @param subject the current user
     * @param packageName the name of the package (the general package will be created if none exists)
     * @param packageTypeId the id of the package type. This is ignored if the <code>newResourceTypeId</code> is not null
     * @param version the version of the package version being created
     * @param architectureId the architecture of the package version
     * @param packageBitStream the input stream with the package bits
     * @param packageUploadDetails additional details about the package. See the constants defined in this interface
     * @param repoId an optional id of the repo to insert the package version in
     * @return the newly create package version
     */
    PackageVersion getUploadedPackageVersion(Subject subject, String packageName, int packageTypeId, String version,
        int architectureId, InputStream packageBitStream, Map<String, String> packageUploadDetails, Integer repoId);

    /**
     * Very simple method that persists the given package version within its own transaction.
     *
     * <p>This method is here to support {@link #persistOrMergePackageVersionSafely(PackageVersion)},
     * it is not meant for general consumption.</p>
     *
     * @param pv the package version to persist
     *
     * @return the newly persisted package version
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
     * @return the newly persisted package
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

    /**
     * Returns the entity associated with no architecture.
     *
     * @return no architecture entity
     */
    Architecture getNoArchitecture();

    /**
     * Returns list of version strings for installed packages on the resource.
     * @param subject
     * @param resourceId
     * @return List of InstalledPackage versions
     */
    List<String> findInstalledPackageVersions(Subject subject, int resourceId);

    /**
     * Returns the package type that backs resources of the specified type.
     *
     * @param resourceTypeId identifies the resource type.
     * @return backing package type if one exists; <code>null</code> otherwise
     */
    PackageType getResourceCreationPackageType(int resourceTypeId);

    /**
     * This method is used to persist new package types that are defined on the server-side
     * by some kind of plugin.
     * <p>
     * Server-side package types are used to identify data stored in the content subsystem
     * which don't have any agent-side counter-part. Such package types are required to have
     * the {@link PackageType#getResourceType() resource type} set to null.
     *
     * @param packageType the package type to persist
     * @return the persisted package type
     * @throws IllegalArgumentException if the supplied package type has non-null resource type
     */
    PackageType persistServersidePackageType(PackageType packageType);

    void writeBlobOutToStream(OutputStream stream, PackageBits bits, boolean closeStreams);

    void updateBlobStream(InputStream stream, PackageBits bits, Map<String, String> contentDetails);

    /**
     * Get the file denoted by this <code>temporaryContentHandle</code>.
     *
     * @see org.rhq.enterprise.server.content.ContentManagerRemote#createTemporaryContentHandle()
     * @param temporaryContentHandle
     * @return the file denoted by this <code>temporaryContentHandle</code>
     */
    File getTemporaryContentFile(String temporaryContentHandle);

    // used solely for Tx demarcation
    void handleDiscoveredPackage(Resource resource, ResourcePackageDetails discoveredPackage,
        Set<InstalledPackage> doomedPackages, long timestamp);

    // used solely for Tx demarcation
    void removeInstalledPackages(Resource resource, Set<InstalledPackage> doomedPackages, long timestamp);

    void removeHistoryDeploymentsBits();
}
