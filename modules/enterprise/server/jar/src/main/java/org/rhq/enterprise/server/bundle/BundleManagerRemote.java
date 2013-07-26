/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
     *   
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
     * 
     * @see {@link #addBundleFile(Subject, int, String, String, Architecture, InputStream)}
     */
    BundleFile addBundleFileViaByteArray(Subject subject, int bundleVersionId, String name, String version,
        Architecture architecture, byte[] fileBytes) throws Exception;

    /**
     * A convenience method taking a URL String whose content will be streamed to the server and used for the file bits.
     * 
     * @see #addBundleFile(Subject, int, String, String, Architecture, InputStream)
     */
    BundleFile addBundleFileViaURL(Subject subject, int bundleVersionId, String name, String version,
        Architecture architecture, String bundleFileUrl) throws Exception;

    /**
     * A variant of {@link #addBundleFileViaURL(Subject, int, String, String, Architecture, String)} supporting the
     * HTTP basic authentication.
     *
     * @see #addBundleFileViaURL(Subject, int, String, String, Architecture, String)
     */
    BundleFile addBundleFileViaURL(Subject subject, int bundleVersionId, String name, String version,
        Architecture architecture, String bundleFileUrl, String userName, String password) throws Exception;

    /**
     * A convenience method taking an existing PackageVersion as opposed to a stream for the file bits.
     * 
     * @see {@link #addBundleFile(Subject, int, String, String, Architecture, InputStream)}
     */
    BundleFile addBundleFileViaPackageVersion(Subject subject, int bundleVersionId, String name, int packageVersionId)
        throws Exception;

    //void assignBundlesToBundleGroup
    
    /**
     * Create a new bundle deployment. Note that bundle deployment names are generated by this
     * call.  This provides useful, uniform naming for display. An optional, custom description
     * can be added.  This call defines a deployment.  The defined deployment can then be
     * scheduled in a separate call.  
     * @param subject user that must have proper permissions
     * @param bundleVersionId the BundleVersion being deployed by this deployment
     * @param bundleDestinationId the BundleDestination for the deployment
     * @param description an optional longer description describing this deployment 
     * @param configuration a Configuration (pojo) to be associated with this deployment. Although
     *        it is not enforceable must be that of the associated BundleVersion.
     * @return the persisted deployment
     * @throws Exception
     */
    BundleDeployment createBundleDeployment(Subject subject, int bundleVersionId, int bundleDestinationId,
        String description, Configuration configuration) throws Exception;

    /**
     * Creates a bundle destination that describes a target for the bundle deployments.
     * 
     * @param subject user must have MANAGE_INVENTORY permission
     * @param bundleId the Bundle to be deployed to this Destination
     * @param name a name for this destination. not null or empty
     * @param description an optional longer description describing this destination 
     * @param destBaseDirName The name of the base directory location where the bundle will be deployed.
     *                        <code>deployDir</code> is relative to the directory that this name refers to.
     *                        This name isn't the directory itself, it refers to the named location as
     *                        defined in the agent plugin's descriptor for the resource's type
     * @param deployDir the root dir for deployments to this destination
     * @param groupId the target platforms for deployments to this destination 
     * @return the persisted destination
     * @throws Exception
     */
    BundleDestination createBundleDestination(Subject subject, int bundleId, String name, String description,
        String destBaseDirName, String deployDir, Integer groupId) throws Exception;

    /**
     * Create a new bundle group.
     * <p/>
     * Requires Global.MANAGE_BUNDLE_GROUP permission.
     * 
     * @param subject user that must have proper permissions
     * @param name the unique bundle group name
     * @param description an optional description
     * @return the persisted BundleGroup
     * @throws Exception
     */
    BundleGroup createBundleGroup(Subject subject, String name, String description) throws Exception;

    /**
     * Creates a bundle version based on single recipe string. The recipe specifies the bundle name,
     * version, version name and version description. If this is the initial version for the named
     * bundle the bundle will be implicitly created.  The bundle type is discovered by the bundle server
     * plugin that can parse the recipe.   
     * 
     * @param subject user that must have proper permissions
     * @param recipe the recipe that defines the bundle version to be created
     * @return the persisted BundleVersion with alot of the internal relationships filled in to help the caller
     *         understand all that this method did.
     */
    BundleVersion createBundleVersionViaRecipe(Subject subject, String recipe) throws Exception;

    /**
     * Creates a bundle version based on a Bundle Distribution file. Typically a zip file, the bundle distribution
     * contains the recipe for a supported bundle type, along with 0, 1 or more bundle files that will be associated
     * with the bundle version.  The recipe specifies the bundle name, version, version name and version description.
     * If this is the initial version for the named bundle the bundle will be implicitly created.  The bundle type
     * is discovered by inspecting the distribution file.   
     * 
     * @param subject user that must have proper permissions
     * @param distributionFile a local Bundle Distribution file. It must be read accessible by the RHQ server process.
     * @return the persisted BundleVersion with alot of the internal relationships filled in to help the caller
     *         understand all that this method did. Bundle files specifically are returned.
     */
    BundleVersion createBundleVersionViaFile(Subject subject, File distributionFile) throws Exception;

    /**
     * Creates a bundle version based on the actual bytes of a Bundle Distribution file. This is essentially
     * the same as {@link #createBundleVersionViaFile(Subject, File)} but the caller is providing the actual
     * bytes of the file as opposed to the file itself.
     * WARNING: obviously, this requires the entire distribution file to have been loaded fully in memory.
     * For very large distribution files, this could cause OutOfMemoryErrors.
     *
     * @param subject user that must have proper permissions
     * @param fileBytes the file bits that make up the entire bundle distribution file
     * @return the persisted BundleVersion with a lot of the internal relationships filled in to help the caller
     *         understand all that this method did. Bundle files specifically are returned.
     */
    BundleVersion createBundleVersionViaByteArray(Subject subject, byte[] fileBytes) throws Exception;

    /**
     * Creates a bundle version based on a Bundle Distribution file. Typically a zip file, the bundle distribution
     * contains the recipe for a supported bundle type, along with 0, 1 or more bundle files that will be associated
     * with the bundle version.  The recipe specifies the bundle name, version, version name and version description.
     * If this is the initial version for the named bundle the bundle will be implicitly created.  The bundle type
     * is discovered by inspecting the distribution file.
     * <br/></br>
     * Note, if the file is local it is more efficient to use {@link #createBundleVersionViaFile(Subject,File)}.
     * 
     * @param subject user that must have proper permissions
     * @param distributionFileUrl a URL String to the Bundle Distribution file. It must be live, resolvable and read accessible
     * by the RHQ server process. 
     * 
     * @return the persisted BundleVersion with a lot of the internal relationships filled in to help the caller
     *         understand all that this method did. Bundle files specifically are returned.
     */
    BundleVersion createBundleVersionViaURL(Subject subject, String distributionFileUrl) throws Exception;

    /**
     * A version of the {@link #createBundleVersionViaURL(org.rhq.core.domain.auth.Subject, String)} that accepts a
     * username and password for basic authentication on the HTTP URLs.
     *
     * @see #createBundleVersionViaURL(org.rhq.core.domain.auth.Subject, String)
     */
    BundleVersion createBundleVersionViaURL(Subject subject, String distributionFileUrl, String username,
        String password) throws Exception;

    /**
     * Remove everything associated with the Bundles with the exception of files laid down by related deployments.
     * Deployed files are left as is on the deployment platforms but the bundle mechanism will no longer track
     * the deployment of all bundles that have been deleted.
     *    
     * @param subject user that must have proper permissions
     * @param bundleIds IDs of all bundles to be deleted
     * @throws Exception if any part of the removal fails. 
     */
    void deleteBundles(Subject subject, int[] bundleIds) throws Exception;

    /**
     * Remove everything associated with the Bundle with the exception of files laid down by related deployments.
     * Deployed files are left as is on the deployment platforms but the bundle mechanism will no longer track
     * the deployment.
     *    
     * @param subject user that must have proper permissions
     * @param bundleId the id of the bundle to remove
     * @throws Exception if any part of the removal fails. 
     */
    void deleteBundle(Subject subject, int bundleId) throws Exception;

    /**
     * Delete a bundle group. Any currently assigned bundles will be removed but are not deleted.
     * <p/>
     * Requires Global.MANAGE_BUNDLE_GROUP permission.
     * 
     * @param subject user that must have proper permissions
     * @param id the bundle group id
     * @throws Exception
     */
    void deleteBundleGroups(Subject subject, int... id) throws Exception;

    /**
     * Remove everything associated with the BundleVersion with the exception of files laid down by related deployments.
     * Deployed files are left as is on the deployment platforms but the bundle mechanism will no longer track
     * the deployment.
     *    
     * @param subject user that must have proper permissions
     * @param bundleVersionId the id of the bundle version to remove
     * @param deleteBundleIfEmpty if <code>true</code> and if this method deletes the last bundle version for its
     *                            bundle, then that bundle entity itself will be completely purged
     * @throws Exception if any part of the removal fails. 
     */
    void deleteBundleVersion(Subject subject, int bundleVersionId, boolean deleteBundleIfEmpty) throws Exception;

    PageList<Bundle> findBundlesByCriteria(Subject subject, BundleCriteria criteria);

    PageList<BundleGroup> findBundleGroupsByCriteria(Subject subject, BundleGroupCriteria criteria);

    PageList<BundleDeployment> findBundleDeploymentsByCriteria(Subject subject, BundleDeploymentCriteria criteria);

    PageList<BundleDestination> findBundleDestinationsByCriteria(Subject subject, BundleDestinationCriteria criteria);

    PageList<BundleResourceDeployment> findBundleResourceDeploymentsByCriteria(Subject subject,
        BundleResourceDeploymentCriteria criteria);

    PageList<BundleFile> findBundleFilesByCriteria(Subject subject, BundleFileCriteria criteria);

    PageList<BundleVersion> findBundleVersionsByCriteria(Subject subject, BundleVersionCriteria criteria);

    PageList<BundleWithLatestVersionComposite> findBundlesWithLatestVersionCompositesByCriteria(Subject subject,
        BundleCriteria criteria);

    List<BundleType> getAllBundleTypes(Subject subject);

    BundleType getBundleType(Subject subject, String bundleTypeName);

    /**
     * Determine the files required for a BundleVersion and return all of the filenames or optionally, just those
     * that lack BundleFiles for the BundleVersion.  The recipe may be parsed as part of this call.
     *   
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
     * Similar to {@link #getBundleVersionFilenames(Subject, int, boolean)}, this will determine the files required for a BundleVersion and return
     * all of the filenames, with the values of the map being true if they already exist or false if they lack BundleFile representation
     * in the BundleVersion.
     *   
     * @param subject user that must have proper permissions
     * @param bundleVersionId the BundleVersion being queried
     * @return map keyed on filenames whose value indicates if a bundle file exists for the file or not
     * @throws Exception
     */
    /* comment back in when someone writes an adapter to support Map un/marshalling
    Map<String, Boolean> getAllBundleVersionFilenames(
         Subject subject,
         int bundleVersionId) throws Exception;
     */

    /**
     * Purges the destination's live deployment content from the remote platforms.
     *
     * @param subject user that must have proper permissions
     * @param bundleDestinationId the ID of the destination that is to be purged of bundle content
     */
    void purgeBundleDestination(Subject subject, int bundleDestinationId) throws Exception;

    /**
     * Deploy the bundle to the destination, as described in the provided deployment.
     * Deployment is asynchronous so return of this method does not indicate individual resource deployments are
     * complete. The returned BundleDeployment can be used to track the history of the individual deployments.
     * <br/><br/>
     * TODO: Add the scheduling capability, currently it's Immediate.
     * <br/> 
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
     * <br/>
     * @param subject user that must have proper permissions
     * @param deploymentDescription an optional longer description describing this deployment. If null defaults
     *        to the description of the previous deployment. 
     * @param isCleanDeployment if true perform a wipe of the deploy directory prior to the revert deployment. Backed up
     *                        files will still be applied. If false perform as an upgrade to the existing deployment.
     * @return the BundleDeployment record, updated with status and (resource) deployments. 
     * @throws Exception
     */
    BundleDeployment scheduleRevertBundleDeployment(Subject subject, int bundleDestinationId,
        String deploymentDescription, boolean isCleanDeployment) throws Exception;

}
