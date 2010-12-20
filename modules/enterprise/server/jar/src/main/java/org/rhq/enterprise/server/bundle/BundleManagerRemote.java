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
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.bundle.BundleDestination;
import org.rhq.core.domain.bundle.BundleFile;
import org.rhq.core.domain.bundle.BundleResourceDeployment;
import org.rhq.core.domain.bundle.BundleType;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.bundle.composite.BundleWithLatestVersionComposite;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.content.Architecture;
import org.rhq.core.domain.criteria.BundleCriteria;
import org.rhq.core.domain.criteria.BundleDeploymentCriteria;
import org.rhq.core.domain.criteria.BundleDestinationCriteria;
import org.rhq.core.domain.criteria.BundleFileCriteria;
import org.rhq.core.domain.criteria.BundleResourceDeploymentCriteria;
import org.rhq.core.domain.criteria.BundleVersionCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.system.ServerVersion;

/**
 * Remote interface to the manager responsible for creating and managing bundles.
 *  
 * @author John Mazzitelli
 * @author Jay Shaughnessy
 */
@SOAPBinding(style = SOAPBinding.Style.DOCUMENT)
@WebService(targetNamespace = ServerVersion.namespace)
@Remote
public interface BundleManagerRemote {

    /**
     * Adds a BundleFile to the BundleVersion and implicitly creates the backing PackageVersion. If the PackageVersion
     * already exists use {@link addBundleFile(Subject, int, String, int, boolean)} 
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
    @WebMethod
    BundleFile addBundleFile( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "bundleVersionid") int bundleVersionId, //
        @WebParam(name = "name") String name, //
        @WebParam(name = "version") String version, //
        @WebParam(name = "architecture") Architecture architecture, //
        @WebParam(name = "fileStream") InputStream fileStream) throws Exception;

    /**
     * A convenience method taking a byte array as opposed to a stream for the file bits.
     * 
     * @see {@link addBundleFile(Subject, int, String, String, Architecture, InputStream, boolean)}     
     */
    BundleFile addBundleFileViaByteArray( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "bundleVersionid") int bundleVersionId, //
        @WebParam(name = "name") String name, //
        @WebParam(name = "version") String version, //
        @WebParam(name = "architecture") Architecture architecture, //
        @WebParam(name = "fileBytes") byte[] fileBytes) throws Exception;

    /**
     * A convenience method taking a URL String whose content will be streamed to the server and used for the file bits.
     * 
     * @see {@link addBundleFile(Subject, int, String, String, Architecture, InputStream, boolean)}     
     */
    BundleFile addBundleFileViaURL( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "bundleVersionid") int bundleVersionId, //
        @WebParam(name = "name") String name, //
        @WebParam(name = "version") String version, //
        @WebParam(name = "architecture") Architecture architecture, //
        @WebParam(name = "bundleFileUrl") String bundleFileUrl) throws Exception;

    /**
     * A convenience method taking an existing PackageVersion as opposed to a stream for the file bits.
     * 
     * @see {@link addBundleFile(Subject, int, String, String, Architecture, InputStream, boolean)}     
     */
    BundleFile addBundleFileViaPackageVersion( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "bundleVersionid") int bundleVersionId, //
        @WebParam(name = "name") String name, //
        @WebParam(name = "packageVersionId") int packageVersionId) throws Exception;

    /**
     * Create a new bundle deployment. Note that bundle deployment names are generated by this
     * call.  This provides useful, uniform naming for display. An optional, custom description
     * can be added.  This call defines a deployment.  The defined deployment can then be
     * scheduled in a separate call.  
     * @param subject user that must have proper permissions
     * @param BundleVersionId the BundleVersion being deployed by this deployment
     * @param BundleDestinationId the BundleDestination for the deployment
     * @param description an optional longer description describing this deployment 
     * @param configuration a Configuration (pojo) to be associated with this deployment. Although
     *        it is not enforceable must be that of the associated BundleVersion.
     * @return the persisted deployment
     * @throws Exception
     */
    BundleDeployment createBundleDeployment( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "bundleVersionId") int bundleVersionId, //
        @WebParam(name = "bundleDestinationId") int bundleDestinationId, //        
        @WebParam(name = "description") String description, //        
        @WebParam(name = "configuration") Configuration configuration) throws Exception;

    /**
     * @param subject user must have MANAGE_INVENTORY permission
     * @param BundleId the Bundle to be deployed to this Destination
     * @param name a name for this destination. not null or empty
     * @param description an optional longer description describing this destination 
     * @param deployDir the root dir for deployments to this destination
     * @param groupIf the target platforms for deployments to this destination 
     * @return the persisted destination
     * @throws Exception
     */
    BundleDestination createBundleDestination( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "bundleId") int bundleId, //
        @WebParam(name = "name") String name, //
        @WebParam(name = "description") String description, //
        @WebParam(name = "deployDir") String deployDir, //        
        @WebParam(name = "groupId") Integer groupId) throws Exception;

    /**
     * Creates a bundle version based on single recipe string. The recipe specifies the bundle name,
     * version, version name and version description. If this is the initial version for the named
     * bundle the bundle will be implicitly created.  The bundle type is discovered by the bundle server
     * plugin that can parse the recipe.   
     * 
     * @param subject
     * @param recipe the recipe that defines the bundle version to be created
     * @return the persisted BundleVersion with alot of the internal relationships filled in to help the caller
     *         understand all that this method did.
     */
    BundleVersion createBundleVersionViaRecipe( //
        @WebParam(name = "subject") Subject subject, //        
        @WebParam(name = "recipe") String recipe) throws Exception;

    /**
     * Creates a bundle version based on a Bundle Distribution file. Typically a zip file, the bundle distribution
     * contains the recipe for a supported bundle type, along with 0, 1 or more bundle files that will be associated
     * with the bundle version.  The recipe specifies the bundle name, version, version name and version description.
     * If this is the initial version for the named bundle the bundle will be implicitly created.  The bundle type
     * is discovered by inspecting the distribution file.   
     * 
     * @param subject
     * @param distributionFile a local Bundle Distribution file. It must be read accessible by the RHQ server process.
     * @return the persisted BundleVersion with alot of the internal relationships filled in to help the caller
     *         understand all that this method did. Bundle files specifically are returned.
     */
    BundleVersion createBundleVersionViaFile( //
        @WebParam(name = "subject") Subject subject, //        
        @WebParam(name = "distributionFile") File distributionFile) throws Exception;

    /**
     * Creates a bundle version based on a Bundle Distribution file. Typically a zip file, the bundle distribution
     * contains the recipe for a supported bundle type, along with 0, 1 or more bundle files that will be associated
     * with the bundle version.  The recipe specifies the bundle name, version, version name and version description.
     * If this is the initial version for the named bundle the bundle will be implicitly created.  The bundle type
     * is discovered by inspecting the distribution file.
     * <br/></br>
     * Note, if the file is local it is more efficient to use {@link createBundleVersionViaFile(Subject,File)}.  
     * 
     * @param subject
     * @param distributionFileUrl a URL String to the Bundle Distribution file. It must be live, resolvable and read accessible
     * by the RHQ server process. 
     * 
     * @return the persisted BundleVersion with alot of the internal relationships filled in to help the caller
     *         understand all that this method did. Bundle files specifically are returned.
     */
    BundleVersion createBundleVersionViaURL( //
        @WebParam(name = "subject") Subject subject, //        
        @WebParam(name = "distributionFileUrl") String distributionFileUrl) throws Exception;

    /**
     * Remove everything associated with the Bundles with the exception of files laid down by related deployments.
     * Deployed files are left as is on the deployment platforms but the bundle mechanism will no longer track
     * the deployment of all bundles that have been deleted.
     *    
     * @param subject
     * @param bundleIds IDs of all bundles to be deleted
     * @throws Exception if any part of the removal fails. 
     */
    @WebMethod
    void deleteBundles( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "bundleIds") int[] bundleIds) throws Exception;

    /**
     * Remove everything associated with the Bundle with the exception of files laid down by related deployments.
     * Deployed files are left as is on the deployment platforms but the bundle mechanism will no longer track
     * the deployment.
     *    
     * @param subject
     * @param bundleId
     * @throws Exception if any part of the removal fails. 
     */
    @WebMethod
    void deleteBundle( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "bundleId") int bundleId) throws Exception;

    /**
     * Remove everything associated with the BundleVersion with the exception of files laid down by related deployments.
     * Deployed files are left as is on the deployment platforms but the bundle mechanism will no longer track
     * the deployment.
     *    
     * @param subject
     * @param bundleVersionId
     * @param deleteBundleIfEmpty if <code>true</code> and if this method deletes the last bundle version for its
     *                            bundle, then that bundle entity itself will be completely purged
     * @throws Exception if any part of the removal fails. 
     */
    @WebMethod
    void deleteBundleVersion( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "bundleVersionId") int bundleVersionId, //
        @WebParam(name = "deleteBundleIfEmpty") boolean deleteBundleIfEmpty) throws Exception;

    @WebMethod
    PageList<Bundle> findBundlesByCriteria( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "criteria") BundleCriteria criteria);

    @WebMethod
    PageList<BundleDeployment> findBundleDeploymentsByCriteria( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "criteria") BundleDeploymentCriteria criteria);

    @WebMethod
    PageList<BundleDestination> findBundleDestinationsByCriteria(@WebParam(name = "subject") Subject subject,
        @WebParam(name = "criteria") BundleDestinationCriteria criteria);

    @WebMethod
    PageList<BundleResourceDeployment> findBundleResourceDeploymentsByCriteria( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "BundleResourceDeploymentCriteria") BundleResourceDeploymentCriteria criteria);

    @WebMethod
    PageList<BundleFile> findBundleFilesByCriteria( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "criteria") BundleFileCriteria criteria);

    @WebMethod
    PageList<BundleVersion> findBundleVersionsByCriteria( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "criteria") BundleVersionCriteria criteria);

    @WebMethod
    PageList<BundleWithLatestVersionComposite> findBundlesWithLatestVersionCompositesByCriteria( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "criteria") BundleCriteria criteria);

    @WebMethod
    List<BundleType> getAllBundleTypes( //
        @WebParam(name = "subject") Subject subject);

    @WebMethod
    BundleType getBundleType( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "bundleTypeName") String bundleTypeName);

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
    Set<String> getBundleVersionFilenames( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "bundleVersionId") int bundleVersionId, //
        @WebParam(name = "withoutBundleFileOnly") boolean withoutBundleFileOnly) throws Exception;

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
    Map<String, Boolean> getAllBundleVersionFilenames( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "bundleVersionId") int bundleVersionId) throws Exception;
     */

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
    @WebMethod
    BundleDeployment scheduleBundleDeployment(
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "bundleDeploymentId") int bundleDeploymentId,
        @WebParam(name = "isCleanDeployment") boolean isCleanDeployment) throws Exception;

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
    @WebMethod
    BundleDeployment scheduleRevertBundleDeployment( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "bundleDestinationId") int bundleDestinationId, //
        @WebParam(name = "deploymentDescription") String deploymentDescription, //        
        @WebParam(name = "isCleanDeployment") boolean isCleanDeployment) throws Exception;

}
