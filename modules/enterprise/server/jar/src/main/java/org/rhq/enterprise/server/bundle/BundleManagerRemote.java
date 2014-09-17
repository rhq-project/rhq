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
package org.rhq.enterprise.server.bundle;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

import javax.ejb.Remote;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.bundle.BundleDestination;
import org.rhq.core.domain.bundle.BundleFile;
import org.rhq.core.domain.bundle.BundleGroup;
import org.rhq.core.domain.bundle.BundleResourceDeployment;
import org.rhq.core.domain.bundle.BundleType;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.bundle.ResourceTypeBundleConfiguration;
import org.rhq.core.domain.bundle.composite.BundleWithLatestVersionComposite;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.content.Architecture;
import org.rhq.core.domain.criteria.BundleCriteria;
import org.rhq.core.domain.criteria.BundleDeploymentCriteria;
import org.rhq.core.domain.criteria.BundleDestinationCriteria;
import org.rhq.core.domain.criteria.BundleFileCriteria;
import org.rhq.core.domain.criteria.BundleGroupCriteria;
import org.rhq.core.domain.criteria.BundleResourceDeploymentCriteria;
import org.rhq.core.domain.criteria.BundleVersionCriteria;
import org.rhq.core.domain.util.PageList;

/**
 * Remote interface to the manager responsible for creating and managing bundles.
 *
 * @author John Mazzitelli
 * @author Jay Shaughnessy
 */
@Remote
public interface BundleManagerRemote {

    /**
     * Given the ID for a compatible group, this will return the bundle configuration metadata for that group's resource type.
     * User interfaces will need to use this method in order to find out if a) the group can be a target for a bundle deployment
     * and/or b) what different destination base locations are supported by the group.
     *
     * @param subject the user making the request
     * @param compatGroupId the ID for a compatible group whose type's bundle config is to be returned
     * @return the bundle configuration for the group's resource type
     * @throws Exception
     */
    ResourceTypeBundleConfiguration getResourceTypeBundleConfiguration(Subject subject, int compatGroupId)
        throws Exception;

    /**
     * Adds a BundleFile to the BundleVersion and implicitly creates the backing PackageVersion. If the PackageVersion
     * already exists use {@link #addBundleFileViaPackageVersion(Subject, int, String, int)}
     * <pre>
     * Required Permissions: Either:
     * - Global.CREATE_BUNDLES and Global.VIEW_BUNDLES
     * - Global.CREATE_BUNDLES and BundleGroup.VIEW_BUNDLES_IN_GROUP for bundle group BG and the relevant bundle is assigned to BG
     * - BundleGroup.CREATE_BUNDLES_IN_GROUP for bundle group BG and the relevant bundle is assigned to BG
     * </pre>
     * @param subject user that must have proper permissions
     * @param bundleVersionId id of the BundleVersion incorporating this BundleFile
     * @param name name of the BundleFile (and the resulting Package)
     * @param version version of the backing package
     * @param architecture architecture appropriate for the backing package.  Defaults to noarch (i.e. any architecture).
     * @param fileStream the file bits
     * @return the new BundleFile
     * @throws Exception
     */
    BundleFile addBundleFile(Subject subject, int bundleVersionId, String name, String version,
        Architecture architecture, InputStream fileStream) throws Exception;

    /**
     * A convenience method taking a byte array as opposed to a stream for the file bits.
     * WARNING: obviously, this requires the entire bundle file to have been loaded fully in memory.
     * For very large files, this could cause OutOfMemoryErrors.
     * <pre>
     * Required Permissions: Either:
     * - Global.CREATE_BUNDLES and Global.VIEW_BUNDLES
     * - Global.CREATE_BUNDLES and BundleGroup.VIEW_BUNDLES_IN_GROUP for bundle group BG and the relevant bundle is assigned to BG
     * - BundleGroup.CREATE_BUNDLES_IN_GROUP for bundle group BG and the relevant bundle is assigned to BG
     * </pre>
     * @param subject user that must have proper permissions
     * @param bundleVersionId id of the BundleVersion incorporating this BundleFile
     * @param name name of the BundleFile (and the resulting Package)
     * @param version version of the backing package
     * @param architecture architecture appropriate for the backing package.  Defaults to noarch (i.e. any architecture).
     * @param fileBytes
     * @return the new BundleFile
     * @throws Exception
     * @see {@link #addBundleFile(Subject, int, String, String, Architecture, InputStream)}
     */
    BundleFile addBundleFileViaByteArray(Subject subject, int bundleVersionId, String name, String version,
        Architecture architecture, byte[] fileBytes) throws Exception;

    /**
     * A convenience method taking a URL String whose content will be streamed to the server and used for the file bits.
     * <pre>
     * Required Permissions: Either:
     * - Global.CREATE_BUNDLES and Global.VIEW_BUNDLES
     * - Global.CREATE_BUNDLES and BundleGroup.VIEW_BUNDLES_IN_GROUP for bundle group BG and the relevant bundle is assigned to BG
     * - BundleGroup.CREATE_BUNDLES_IN_GROUP for bundle group BG and the relevant bundle is assigned to BG
     * </pre>
     * @param subject user that must have proper permissions
     * @param bundleVersionId id of the BundleVersion incorporating this BundleFile
     * @param name name of the BundleFile (and the resulting Package)
     * @param version version of the backing package
     * @param architecture architecture appropriate for the backing package.  Defaults to noarch (i.e. any architecture).
     * @param bundleFileUrl
     * @return the new BundleFile
     * @throws Exception
     * @see #addBundleFile(Subject, int, String, String, Architecture, InputStream)
     *
     * @since 4.8
     */
    BundleFile addBundleFileViaURL(Subject subject, int bundleVersionId, String name, String version,
        Architecture architecture, String bundleFileUrl) throws Exception;

    /**
     * A variant of {@link #addBundleFileViaURL(Subject, int, String, String, Architecture, String)} supporting the
     * HTTP basic authentication.
     * <pre>
     * Required Permissions: Either:
     * - Global.CREATE_BUNDLES and Global.VIEW_BUNDLES
     * - Global.CREATE_BUNDLES and BundleGroup.VIEW_BUNDLES_IN_GROUP for bundle group BG and the relevant bundle is assigned to BG
     * - BundleGroup.CREATE_BUNDLES_IN_GROUP for bundle group BG and the relevant bundle is assigned to BG
     * </pre>
     * @param subject user that must have proper permissions
     * @param bundleVersionId id of the BundleVersion incorporating this BundleFile
     * @param name name of the BundleFile (and the resulting Package)
     * @param version version of the backing package
     * @param architecture architecture appropriate for the backing package.  Defaults to noarch (i.e. any architecture).
     * @param bundleFileUrl
     * @param userName
     * @param password
     * @return the new BundleFile
     * @throws Exception
     * @see #addBundleFileViaURL(Subject, int, String, String, Architecture, String)
     */
    BundleFile addBundleFileViaURL(Subject subject, int bundleVersionId, String name, String version,
        Architecture architecture, String bundleFileUrl, String userName, String password) throws Exception;

    /**
     * A convenience method taking an existing PackageVersion as opposed to a stream for the file bits.
     * <pre>
     * Required Permissions: Either:
     * - Global.CREATE_BUNDLES and Global.VIEW_BUNDLES
     * - Global.CREATE_BUNDLES and BundleGroup.VIEW_BUNDLES_IN_GROUP for bundle group BG and the relevant bundle is assigned to BG
     * - BundleGroup.CREATE_BUNDLES_IN_GROUP for bundle group BG and the relevant bundle is assigned to BG
     * </pre>
     * @param subject user that must have proper permissions
     * @param bundleVersionId id of the BundleVersion incorporating this BundleFile
     * @param name name of the BundleFile (and the resulting Package)
     * @param packageVersionId
     * @return the new BundleFile
     * @throws Exception
     * @see {@link #addBundleFile(Subject, int, String, String, Architecture, InputStream)}
     */
    BundleFile addBundleFileViaPackageVersion(Subject subject, int bundleVersionId, String name, int packageVersionId)
        throws Exception;

    /**
     * Assign the specified bundles to the specified bundle groups. This adds bundles that were not previously
     * assigned.  Others are ignored.
     * <pre>
     * Requires VIEW permission for the relevant bundle and one of:
     * - Global.MANAGE_BUNDLE_GROUPS
     * - Global.CREATE_BUNDLE
     * - BundleGroup.ASSIGN_BUNDLES_TO_GROUP for the relevant bundle group
     * - BundleGroup.CREATE_BUNDLES_IN_GROUP for the relevant bundle group
     * </pre>
     * @param subject
     * @param bundleGroupIds
     * @param bundleIds
     *
     * @since 4.9
     */
    void assignBundlesToBundleGroups(Subject subject, int[] bundleGroupIds, int[] bundleIds);

    /**
     * Create a new bundle deployment. Note that bundle deployment names are generated by this
     * call.  This provides useful, uniform naming for display. An optional, custom description
     * can be added.  This call defines a deployment.  The defined deployment can then be
     * scheduled in a separate call.
     * <pre>
     * Required Permissions: Either:
     * - Global.DEPLOY_BUNDLES and a view of the relevant bundle and a view of the relevant resource group (may involve multiple roles)
     * - Resource.DEPLOY_BUNDLES_TO_GROUP and a view of the relevant bundle and a view of the relevant resource group (may involve multiple roles)
     * </pre>
     * @param subject user that must have proper permissions
     * @param bundleVersionId the BundleVersion being deployed by this deployment
     * @param bundleDestinationId the BundleDestination for the deployment
     * @param description an optional longer description describing this deployment
     * @param configuration a Configuration (pojo) to be associated with this deployment.
     *                      This is validated against the configuration definition provided by the
     *                      bundle version.
     * @return the persisted deployment
     * @throws Exception
     */
    BundleDeployment createBundleDeployment(Subject subject, int bundleVersionId, int bundleDestinationId,
        String description, Configuration configuration) throws Exception;

    /**
     * Creates a bundle destination that describes a target for the bundle deployments.
     * <pre>
     * Required Permissions: Either:
     * - Global.DEPLOY_BUNDLES and a view of the relevant bundle and a view of the relevant resource group (may involve multiple roles)
     * - Resource.DEPLOY_BUNDLES_TO_GROUP and a view of the relevant bundle and a view of the relevant resource group (may involve multiple roles)
     * </pre>
     * @param subject user must have MANAGE_INVENTORY permission
     * @param bundleId the Bundle to be deployed to this Destination
     * @param name a name for this destination. not null or empty
     * @param description an optional longer description describing this destination
     * @param destinationSpecification The name of the destination location where the bundle will be deployed.
     *                        <code>deployDir</code> is relative to the directory that this name refers to.
     *                        This name isn't the directory itself, it refers to the named location as
     *                        defined in the agent plugin's descriptor for the resource's type.
     * @param deployDir the root dir for deployments to this destination or null if the type of the bundle does not
     *                  require it
     * @param groupId the target platforms for deployments to this destination
     * @return the persisted destination
     * @throws Exception
     */
    BundleDestination createBundleDestination(Subject subject, int bundleId, String name, String description,
        String destinationSpecification, String deployDir, Integer groupId) throws Exception;

    /**
     * Create a new bundle group. Bundles, if specified will be set as the initial bundles in the group.
     * <pre>
     * Require Permissions:
     * - Global.MANAGE_BUNDLE_GROUPS
     * </pre>
     * @param subject user that must have proper permissions
     * @param bundleGroup the new bundle group name the unique bundle group name
     * @return the persisted BundleGroup
     * @throws Exception
     *
     * @since 4.9
     */
    BundleGroup createBundleGroup(Subject subject, BundleGroup bundleGroup) throws Exception;

    /**
     * Creates a bundle version based on single recipe string. The recipe specifies the bundle name,
     * version, version name and version description. If this is the initial version for the named
     * bundle the bundle will be implicitly created.  The bundle type is discovered by the bundle server
     * plugin that can parse the recipe.
     * </p>
     * If this bundle version is the initial version of a new bundle that needs to be created, the subject must
     * have Global.VIEW_BUNDLES because the new bundle will not be associated with any bundle group.
     * <pre>
     * Required Permissions: Either:
     * - Global.CREATE_BUNDLES and Global.VIEW_BUNDLES
     * - Global.CREATE_BUNDLES and BundleGroup.VIEW_BUNDLES_IN_GROUP for bundle group BG and the relevant bundle is assigned to BG
     * - BundleGroup.CREATE_BUNDLES_IN_GROUP for bundle group BG and the relevant bundle is assigned to BG
     * </pre>
     * @param subject user that must have proper permissions
     * @param recipe the recipe that defines the bundle version to be created
     * @return the persisted BundleVersion with alot of the internal relationships filled in to help the caller
     *         understand all that this method did.
     * @throws Exception
     */
    BundleVersion createBundleVersionViaRecipe(Subject subject, String recipe) throws Exception;

    /**
     * Like #createBundleVersionViaRecipe except this method will assume this is a new bundle and is responsible
     * for creating the bundle as well as the bundle version. The caller can indicate which bundle groups the new bundle
     * should be assigned to.
     * If bundleGroupIds is null, then the new bundle will not be associated with any bundle group - this is only
     * allowed if the caller has the permission Global.VIEW_BUNDLES.
     * <pre>
     * Required Permissions: Either:
     * - Global.CREATE_BUNDLES and Global.VIEW_BUNDLES
     * - Global.CREATE_BUNDLES and BundleGroup.VIEW_BUNDLES_IN_GROUP for bundle group BG
     * - BundleGroup.CREATE_BUNDLES_IN_GROUP for bundle group BG
     * </pre>
     * @param subject user that must have proper permissions
     * @param bundleGroupIds identifies the bundle groups that the new bundle will be associated with; null or zero
     * length to leave unassigned.
     * @param recipe the recipe that defines the bundle version to be created
     * @return the persisted BundleVersion with alot of the internal relationships filled in to help the caller
     *         understand all that this method did.
     * @throws Exception
     *
     * @since 4.9
     */
    BundleVersion createInitialBundleVersionViaRecipe(Subject subject, int[] bundleGroupIds, String recipe)
        throws Exception;

    /**
     * Creates a bundle version based on a Bundle Distribution file. Typically a zip file, the bundle distribution
     * contains the recipe for a supported bundle type, along with 0, 1 or more bundle files that will be associated
     * with the bundle version.  The recipe specifies the bundle name, version, version name and version description.
     * If this is the initial version for the named bundle the bundle will be implicitly created.  The bundle type
     * is discovered by inspecting the distribution file.
     * </p>
     * If this bundle version is the initial version of a new bundle that needs to be created, the subject must
     * have Global.VIEW_BUNDLES because the new bundle will not be associated with any bundle group.
     * <pre>
     * Required Permissions: Either:
     * - Global.CREATE_BUNDLES and Global.VIEW_BUNDLES
     * - Global.CREATE_BUNDLES and BundleGroup.VIEW_BUNDLES_IN_GROUP for bundle group BG and the relevant bundle is assigned to BG
     * - BundleGroup.CREATE_BUNDLES_IN_GROUP for bundle group BG and the relevant bundle is assigned to BG
     * </pre>
     * @param subject user that must have proper permissions
     * @param distributionFile a local Bundle Distribution file. It must be read accessible by the RHQ server process.
     * @return the persisted BundleVersion with a lot of the internal relationships filled in to help the caller
     *         understand all that this method did. Bundle files specifically are returned.
     * @throws Exception
     */
    BundleVersion createBundleVersionViaFile(Subject subject, File distributionFile) throws Exception;

    /**
     * Like {@link #createBundleVersionViaFile(org.rhq.core.domain.auth.Subject, java.io.File)} except that this method
     * takes a <code>temporaryContentHandle</code> as parameter instead of a file.
     * @param subject user that must have proper permissions
     * @param temporaryContentHandle
     * @return the persisted BundleVersion with a lot of the internal relationships filled in to help the caller
     *         understand all that this method did. Bundle files specifically are returned.
     * @throws Exception
     *
     * @see org.rhq.enterprise.server.content.ContentManagerRemote#createTemporaryContentHandle()
     * @see org.rhq.enterprise.server.content.ContentManagerRemote#uploadContentFragment(String, byte[], int, int)
     * @see #createBundleVersionViaFile(org.rhq.core.domain.auth.Subject, java.io.File)
     *
     * @since 4.10
     */
    BundleVersion createBundleVersionViaContentHandle(Subject subject, String temporaryContentHandle) throws Exception;

    /**
     * Like #createBundleVersionViaFile except this method will assume this is a new bundle and is responsible
     * for creating the bundle as well as the bundle version. The caller can indicate which bundle groups the new bundle
     * should be assigned to.
     * If bundleGroupIds is null, then the new bundle will not be associated with any bundle group - this is only
     * allowed if the caller has the permission Global.VIEW_BUNDLES.
     * <pre>
     * Required Permissions: Either:
     * - Global.CREATE_BUNDLES and Global.VIEW_BUNDLES
     * - Global.CREATE_BUNDLES and BundleGroup.VIEW_BUNDLES_IN_GROUP for bundle group BG
     * - BundleGroup.CREATE_BUNDLES_IN_GROUP for bundle group BG
     * </pre>
     * @param subject user that must have proper permissions
     * @param bundleGroupIds identifies the bundle groups that the new bundle will be associated with; null or zero
     * length to leave unassigned.
     * @param distributionFile a local Bundle Distribution file. It must be read accessible by the RHQ server process.
     * @return the persisted BundleVersion with a lot of the internal relationships filled in to help the caller
     *         understand all that this method did. Bundle files specifically are returned.
     * @throws Exception
     *
     * @since 4.9
     */
    BundleVersion createInitialBundleVersionViaFile(Subject subject, int[] bundleGroupIds, File distributionFile)
            throws Exception;

    /**
     * Like {@link #createInitialBundleVersionViaFile(org.rhq.core.domain.auth.Subject, int[], java.io.File)}, except
     * that this method takes a <code>temporaryContentHandle</code> as parameter instead of a file.
     *
     * @param subject user that must have proper permissions
     * @param bundleGroupIds identifies the bundle groups that the new bundle will be associated with; null or zero
     * length to leave unassigned.
     * @param temporaryContentHandle
     * @return the persisted BundleVersion with a lot of the internal relationships filled in to help the caller
     *         understand all that this method did. Bundle files specifically are returned.
     * @throws Exception
     *
     * @see org.rhq.enterprise.server.content.ContentManagerRemote#createTemporaryContentHandle()
     * @see org.rhq.enterprise.server.content.ContentManagerRemote#uploadContentFragment(String, byte[], int, int)
     * @see #createInitialBundleVersionViaFile(org.rhq.core.domain.auth.Subject, int[], java.io.File)
     *
     * @since 4.10
     */
    BundleVersion createInitialBundleVersionViaContentHandle(Subject subject, int[] bundleGroupIds,
        String temporaryContentHandle) throws Exception;

    /**
     * Creates a bundle version based on the actual bytes of a Bundle Distribution file. This is essentially
     * the same as {@link #createBundleVersionViaFile(Subject, File)} but the caller is providing the actual
     * bytes of the file as opposed to the file itself.
     * WARNING: obviously, this requires the entire distribution file to have been loaded fully in memory.
     * For very large distribution files, this could cause OutOfMemoryErrors.
     * </p>
     * If this bundle version is the initial version of a new bundle that needs to be created, the subject must
     * have Global.VIEW_BUNDLES because the new bundle will not be associated with any bundle group.
     * <pre>
     * Required Permissions: Either:
     * - Global.CREATE_BUNDLES and Global.VIEW_BUNDLES
     * - Global.CREATE_BUNDLES and BundleGroup.VIEW_BUNDLES_IN_GROUP for bundle group BG and the relevant bundle is assigned to BG
     * - BundleGroup.CREATE_BUNDLES_IN_GROUP for bundle group BG and the relevant bundle is assigned to BG
     * </pre>
     * @param subject user that must have proper permissions
     * @param fileBytes the file bits that make up the entire bundle distribution file
     * @return the persisted BundleVersion with a lot of the internal relationships filled in to help the caller
     *         understand all that this method did. Bundle files specifically are returned.
     * @throws Exception
     */
    BundleVersion createBundleVersionViaByteArray(Subject subject, byte[] fileBytes) throws Exception;

    /**
     * Like #createBundleVersionViaByteArray except this method will assume this is a new bundle and is responsible
     * for creating the bundle as well as the bundle version. The caller can indicate which bundle groups the new bundle
     * should be assigned to.
     * If bundleGroupIds is null, then the new bundle will not be associated with any bundle group - this is only
     * allowed if the caller has the permission Global.VIEW_BUNDLES.
     * <pre>
     * Required Permissions: Either:
     * - Global.CREATE_BUNDLES and Global.VIEW_BUNDLES
     * - Global.CREATE_BUNDLES and BundleGroup.VIEW_BUNDLES_IN_GROUP for bundle group BG
     * - BundleGroup.CREATE_BUNDLES_IN_GROUP for bundle group BG
     * </pre>
     * @param subject user that must have proper permissions
     * @param bundleGroupIds identifies the bundle groups that the new bundle will be associated with; null or zero
     * length to leave unassigned.
     * @param fileBytes the file bits that make up the entire bundle distribution file
     * @return the persisted BundleVersion with a lot of the internal relationships filled in to help the caller
     *         understand all that this method did. Bundle files specifically are returned.
     * @throws Exception
     *
     * @since 4.9
     */
    BundleVersion createInitialBundleVersionViaByteArray(Subject subject, int[] bundleGroupIds, byte[] fileBytes)
        throws Exception;

    /**
     * Creates a bundle version based on a Bundle Distribution file. Typically a zip file, the bundle distribution
     * contains the recipe for a supported bundle type, along with 0, 1 or more bundle files that will be associated
     * with the bundle version.  The recipe specifies the bundle name, version, version name and version description.
     * If this is the initial version for the named bundle the bundle will be implicitly created.  The bundle type
     * is discovered by inspecting the distribution file.
     * <br/></br>
     * Note, if the file is local it is more efficient to use {@link #createBundleVersionViaFile(Subject,File)}.
     * </p>
     * If this bundle version is the initial version of a new bundle that needs to be created, the subject must
     * have Global.VIEW_BUNDLES because the new bundle will not be associated with any bundle group.
     * <pre>
     * Required Permissions: Either:
     * - Global.CREATE_BUNDLES and Global.VIEW_BUNDLES
     * - Global.CREATE_BUNDLES and BundleGroup.VIEW_BUNDLES_IN_GROUP for bundle group BG and the relevant bundle is assigned to BG
     * - BundleGroup.CREATE_BUNDLES_IN_GROUP for bundle group BG and the relevant bundle is assigned to BG
     * </pre>
     * @param subject user that must have proper permissions
     * @param distributionFileUrl a URL String to the Bundle Distribution file. It must be live, resolvable and read accessible
     * by the RHQ server process.
     *
     * @return the persisted BundleVersion with a lot of the internal relationships filled in to help the caller
     *         understand all that this method did. Bundle files specifically are returned.
     * @throws Exception
     */
    BundleVersion createBundleVersionViaURL(Subject subject, String distributionFileUrl) throws Exception;

    /**
     * Like #createBundleVersionViaURL except this method will assume this is a new bundle and is responsible
     * for creating the bundle as well as the bundle version. The caller can indicate which bundle groups the new bundle
     * should be assigned to.
     * If bundleGroupIds is null, then the new bundle will not be associated with any bundle group - this is only
     * allowed if the caller has the permission Global.VIEW_BUNDLES.
     * <pre>
     * Required Permissions: Either:
     * - Global.CREATE_BUNDLES and Global.VIEW_BUNDLES
     * - Global.CREATE_BUNDLES and BundleGroup.VIEW_BUNDLES_IN_GROUP for bundle group BG
     * - BundleGroup.CREATE_BUNDLES_IN_GROUP for bundle group BG
     * </pre>
     * @param subject user that must have proper permissions
     * @param bundleGroupIds identifies the bundle groups that the new bundle will be associated with; null or zero
     * length to leave unassigned.
     * @param distributionFileUrl a URL String to the Bundle Distribution file. It must be live, resolvable and read accessible
     *                            by the RHQ server process.
     * @return the persisted BundleVersion with a lot of the internal relationships filled in to help the caller
     *         understand all that this method did. Bundle files specifically are returned.
     * @throws Exception
     *
     * @since 4.9
     */
    BundleVersion createInitialBundleVersionViaURL(Subject subject, int[] bundleGroupIds, String distributionFileUrl)
        throws Exception;

    /**
     * A version of the {@link #createBundleVersionViaURL(org.rhq.core.domain.auth.Subject, String)} that accepts a
     * username and password for basic authentication on the HTTP URLs.
     * </p>
     * If this bundle version is the initial version of a new bundle that needs to be created, the subject must
     * have Global.VIEW_BUNDLES because the new bundle will not be associated with any bundle group.
     * <pre>
     * Required Permissions: Either:
     * - Global.CREATE_BUNDLES and Global.VIEW_BUNDLES
     * - Global.CREATE_BUNDLES and BundleGroup.VIEW_BUNDLES_IN_GROUP for bundle group BG and the relevant bundle is assigned to BG
     * - BundleGroup.CREATE_BUNDLES_IN_GROUP for bundle group BG and the relevant bundle is assigned to BG
     * </pre>
     * @param subject user that must have proper permissions
     * @param distributionFileUrl
     * @param username
     * @param password
     * @return the persisted BundleVersion with a lot of the internal relationships filled in to help the caller
     *         understand all that this method did. Bundle files specifically are returned.
     * @throws Exception
     * @see #createBundleVersionViaURL(org.rhq.core.domain.auth.Subject, String)
     *
     * @since 4.8
     */
    BundleVersion createBundleVersionViaURL(Subject subject, String distributionFileUrl, String username,
        String password) throws Exception;

    /**
     * Like #createBundleVersionViaURL except this method will assume this is a new bundle and is responsible
     * for creating the bundle as well as the bundle version. The caller can indicate which bundle groups the new bundle
     * should be assigned to.
     * If bundleGroupIds is null, then the new bundle will not be associated with any bundle group - this is only
     * allowed if the caller has the permission Global.VIEW_BUNDLES.
     * <pre>
     * Required Permissions: Either:
     * - Global.CREATE_BUNDLES and Global.VIEW_BUNDLES
     * - Global.CREATE_BUNDLES and BundleGroup.VIEW_BUNDLES_IN_GROUP for bundle group BG
     * - BundleGroup.CREATE_BUNDLES_IN_GROUP for bundle group BG
     * </pre>
     * @param subject user that must have proper permissions
     * @param bundleGroupIds identifies the bundle groups that the new bundle will be associated with; null or zero
     * length to leave unassigned.
     * @param distributionFileUrl
     * @param username
     * @param password
     * @return the persisted BundleVersion with a lot of the internal relationships filled in to help the caller
     *         understand all that this method did. Bundle files specifically are returned.
     * @throws Exception
     * @see #createBundleVersionViaURL(org.rhq.core.domain.auth.Subject, String)
     *
     * @since 4.9
     */
    BundleVersion createInitialBundleVersionViaURL(Subject subject, int[] bundleGroupIds, String distributionFileUrl,
        String username, String password) throws Exception;

    /**
     * Remove everything associated with the Bundles with the exception of files laid down by related deployments.
     * Deployed files are left as is on the deployment platforms but the bundle mechanism will no longer track
     * the deployment of all bundles that have been deleted.
     * The bundles that are deleted will be removed from all bundle groups that it was a member of.
     * <pre>
     * Required Permissions: Either:
     * - Global.DELETE_BUNDLES and Global.VIEW_BUNDLES
     * - Global.DELETE_BUNDLES and BundleGroup.VIEW_BUNDLES_IN_GROUP for bundle group BG and the relevant bundle is assigned to BG
     * - BundleGroup.DELETE_BUNDLES_FROM_GROUP for bundle group BG and the relevant bundle is assigned to BG
     * </pre>
     * @param subject user that must have proper permissions
     * @param bundleIds IDs of all bundles to be deleted
     * @throws Exception if any part of the removal fails.
     */
    void deleteBundles(Subject subject, int[] bundleIds) throws Exception;

    /**
     * Remove everything associated with the Bundle with the exception of files laid down by related deployments.
     * Deployed files are left as is on the deployment platforms but the bundle mechanism will no longer track
     * the deployment.
     * The bundles that are deleted will be removed from all bundle groups that it was a member of.
     * <pre>
     * Required Permissions: Either:
     * - Global.DELETE_BUNDLES and Global.VIEW_BUNDLES
     * - Global.DELETE_BUNDLES and BundleGroup.VIEW_BUNDLES_IN_GROUP for bundle group BG and the relevant bundle is assigned to BG
     * - BundleGroup.DELETE_BUNDLES_FROM_GROUP for bundle group BG and the relevant bundle is assigned to BG
     * </pre>
     * @param subject user that must have proper permissions
     * @param bundleId the id of the bundle to remove
     * @throws Exception if any part of the removal fails.
     */
    void deleteBundle(Subject subject, int bundleId) throws Exception;

    /**
     * Delete a bundle group. Any currently assigned bundles will be removed but are not deleted.
     * <pre>
     * Required Permissions:
     * - Global.MANAGE_BUNDLE_GROUPS
     * </pre>
     * @param subject user that must have proper permissions
     * @param ids the bundle group id
     * @throws Exception
     *
     * @since 4.9
     */
    void deleteBundleGroups(Subject subject, int[] ids) throws Exception;

    /**
     * Remove everything associated with the BundleVersion with the exception of files laid down by related deployments.
     * Deployed files are left as is on the deployment platforms but the bundle mechanism will no longer track
     * the deployment.
     * The deleted bundle version will no longer exist in any bundle group.
     * <pre>
     * Required Permissions: Either:
     * - Global.DELETE_BUNDLES and Global.VIEW_BUNDLES
     * - Global.DELETE_BUNDLES and BundleGroup.VIEW_BUNDLES_IN_GROUP for bundle group BG and the relevant bundle is assigned to BG
     * - BundleGroup.DELETE_BUNDLES_FROM_GROUP for bundle group BG and the relevant bundle is assigned to BG
     * </pre>
     * @param subject user that must have proper permissions
     * @param bundleVersionId the id of the bundle version to remove
     * @param deleteBundleIfEmpty if <code>true</code> and if this method deletes the last bundle version for its
     *                            bundle, then that bundle entity itself will be completely purged
     * @throws Exception if any part of the removal fails.
     */
    void deleteBundleVersion(Subject subject, int bundleVersionId, boolean deleteBundleIfEmpty) throws Exception;

    /**
     * Return the <code>Bundles</code> narrowed by the supplied Criteria. The results are implicitly
     * narrowed to those bundles viewable by the <code>subject</code>.
     * @param subject
     * @param criteria
     * @return not null
     */
    PageList<Bundle> findBundlesByCriteria(Subject subject, BundleCriteria criteria);

    /**
     * Return the <code>BundleGroups</code> narrowed by the supplied Criteria. The results are implicitly
     * narrowed to those bundle groups viewable by the <code>subject</code>.
     * @param subject
     * @param criteria
     * @return not null
     *
     * @since 4.9
     */
    PageList<BundleGroup> findBundleGroupsByCriteria(Subject subject, BundleGroupCriteria criteria);

    /**
     * Return the <code>BundleDeployments</code> narrowed by the supplied Criteria. The results are implicitly
     * narrowed to those for bundles and destination groups viewable by the <code>subject</code>.
     * @param subject
     * @param criteria
     * @return not null
     */
    PageList<BundleDeployment> findBundleDeploymentsByCriteria(Subject subject, BundleDeploymentCriteria criteria);

    /**
     * Return the <code>BundleDestinations</code> narrowed by the supplied Criteria. The results are implicitly
     * narrowed to those with destination resource groups viewable by the <code>subject</code>.
     * @param subject
     * @param criteria
     * @return not null
     */
    PageList<BundleDestination> findBundleDestinationsByCriteria(Subject subject, BundleDestinationCriteria criteria);

    /**
     * Note that this can involves permissions on bundles and resources.  Results will always be narrowed to the bundles
     * viewable by <code>subject</code>.  If optionally requesting the relevant Resources
     * via <code>BundleResourceDeploymentCriteria.fetchResources(true)</code> the results will be further narrowed to
     * the viewable resources.
     *
     * @param subject
     * @param criteria
     * @return not null
     */
    PageList<BundleResourceDeployment> findBundleResourceDeploymentsByCriteria(Subject subject,
        BundleResourceDeploymentCriteria criteria);

    /**
     * @param subject
     * @param criteria
     * @return not null
     */
    PageList<BundleFile> findBundleFilesByCriteria(Subject subject, BundleFileCriteria criteria);

    /**
     * @param subject
     * @param criteria
     * @return not null
     */
    PageList<BundleVersion> findBundleVersionsByCriteria(Subject subject, BundleVersionCriteria criteria);

    /**
     * @param subject
     * @param criteria
     * @return not null
     */
    PageList<BundleWithLatestVersionComposite> findBundlesWithLatestVersionCompositesByCriteria(Subject subject,
        BundleCriteria criteria);

    /**
     * @param subject
     * @return not null
     */
    List<BundleType> getAllBundleTypes(Subject subject);

    /**
     * @param subject
     * @param bundleTypeName must exist
     * @return the bundle type
     */
    BundleType getBundleType(Subject subject, String bundleTypeName);

    /**
     * Determine the files required for a BundleVersion and return all of the filenames or optionally, just those
     * that lack BundleFiles for the BundleVersion.  The recipe may be parsed as part of this call.
     * This is needed as part of the bundle creation workflow, hence why creation permissions are needed.
     * <pre>
     * Required Permissions: Either:
     * - Global.CREATE_BUNDLES and Global.VIEW_BUNDLES
     * - Global.CREATE_BUNDLES and BundleGroup.VIEW_BUNDLES_IN_GROUP for bundle group BG and the relevant bundle is assigned to BG
     * - BundleGroup.CREATE_BUNDLES_IN_GROUP for bundle group BG and the relevant bundle is assigned to BG
     * </pre>
     * @param subject user that must have proper permissions
     * @param bundleVersionId the BundleVersion being queried
     * @param withoutBundleFileOnly if true omit any filenames that already have a corresponding BundleFile for
     *        the BundleVersion.
     * @return The List of filenames.
     * @throws Exception
     */
    Set<String> getBundleVersionFilenames(Subject subject, int bundleVersionId, boolean withoutBundleFileOnly)
        throws Exception;

    /**
     * Purges the destination's live deployment content from the remote platforms.
     * <pre>
     * Required Permissions: Either:
     * - Global.DEPLOY_BUNDLES and a view of the relevant bundle and a view of the relevant resource group (may involve multiple roles)
     * - Resource.DEPLOY_BUNDLES_TO_GROUP and a view of the relevant bundle and a view of the relevant resource group (may involve multiple roles)
     * </pre>
     * @param subject user that must have proper permissions
     * @param bundleDestinationId the ID of the destination that is to be purged of bundle content
     * @throws Exception
     */
    void purgeBundleDestination(Subject subject, int bundleDestinationId) throws Exception;

    /**
     * Deploy the bundle to the destination, as described in the provided deployment.
     * Deployment is asynchronous so return of this method does not indicate individual resource deployments are
     * complete. The returned BundleDeployment can be used to track the history of the individual deployments.
     * <br/><br/>
     * TODO: Add the scheduling capability, currently it's Immediate.
     * <pre>
     * Required Permissions: Either:
     * - Global.DEPLOY_BUNDLES and a view of the relevant bundle and a view of the relevant resource group (may involve multiple roles)
     * - Resource.DEPLOY_BUNDLES_TO_GROUP and a view of the relevant bundle and a view of the relevant resource group (may involve multiple roles)
     * </pre>
     * @param subject user that must have proper permissions
     * @param bundleDeploymentId the BundleDeployment being used to guide the deployments
     * @param isCleanDeployment if true perform a wipe of the deploy directory prior to the deployment. If false
     *                        perform as an upgrade to the existing deployment, if any.
     * @return the BundleDeployment record, updated with status and (resource) deployments.
     * @throws Exception
     */
    BundleDeployment scheduleBundleDeployment(Subject subject, int bundleDeploymentId, boolean isCleanDeployment)
        throws Exception;

    /**
     * For the specified destination, revert from the current live deployment to the deployment it had replaced.
     * A revert first rolls back to the previous deployment (bundle version and configuration) and then rolls forward
     * by replacing changed files that had been backed up during the most recent (live) deployment.
     * The returned BundleDeployment represents the new live deployment and can be used to track the history of the
     * individual revert deployments. Note that bundle deployment names are generated by this
     * call.  This provides useful, uniform naming for display. An optional, custom description can be added.
     * <br/><br/>
     * TODO: Add the scheduling capability, currently it's Immediate.
     * </pre>
     * Required Permissions: Either:
     * - Global.DEPLOY_BUNDLES and a view of the relevant bundle and a view of the relevant resource group (may involve multiple roles)
     * - Resource.DEPLOY_BUNDLES_TO_GROUP and a view of the relevant bundle and a view of the relevant resource group (may involve multiple roles)
     *
     * @param subject user that must have proper permissions
     * @param bundleDestinationId the destination on which the revert should be applied
     * @param deploymentDescription an optional longer description describing this deployment. If null defaults
     *        to the description of the previous deployment.
     * @param isCleanDeployment if true perform a wipe of the deploy directory prior to the revert deployment. Backed up
     *                        files will still be applied. If false perform as an upgrade to the existing deployment.
     * @return the BundleDeployment record, updated with status and (resource) deployments.
     * @throws Exception
     */
    BundleDeployment scheduleRevertBundleDeployment(Subject subject, int bundleDestinationId,
        String deploymentDescription, boolean isCleanDeployment) throws Exception;

    /**
     * Unassign the specified bundles from the specified bundle groups. This removes bundles that were previously
     * assigned. Others are ignored.
     * <pre>
     * Requires VIEW permission for the relevant bundles and one of:
     * - Global.MANAGE_BUNDLE_GROUPS
     * - Global.DELETE_BUNDLE
     * - BundleGroup.UNASSIGN_BUNDLES_FROM_GROUP for the relevant bundle group
     * - BundleGroup.DELETE_BUNDLES_FROM_GROUP for the relevant bundle group
     * </pre>
     * @param subject
     * @param bundleGroupIds
     * @param bundleIds
     *
     * @since 4.9
     */
    void unassignBundlesFromBundleGroups(Subject subject, int[] bundleGroupIds, int[] bundleIds);

    /**
     * Updates an existing bundle group. The set of bundles will  be updated if non-null.
     * <pre>
     * Require Permissions:
     * - Global.MANAGE_BUNDLE_GROUPS
     * </pre>
     * @param subject user that must have proper permissions
     * @param bundleGroup the updated bundle group
     * @return the updated BundleGroup
     * @throws Exception
     *
     * @since 4.9
     */
    BundleGroup updateBundleGroup(Subject subject, BundleGroup bundleGroup) throws Exception;

}
