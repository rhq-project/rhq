/*
 * RHQ Management Platform
 * Copyright (C) 2005-2015 Red Hat, Inc.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;

import org.rhq.core.clientapi.agent.bundle.BundleScheduleRequest;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.bundle.BundleDeploymentStatus;
import org.rhq.core.domain.bundle.BundleResourceDeployment;
import org.rhq.core.domain.bundle.BundleResourceDeploymentHistory;
import org.rhq.core.domain.bundle.BundleType;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.bundle.composite.BundleGroupAssignmentComposite;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.resource.ResourceType;

/**
 * Local interface to the manager responsible for creating and managing bundles.
 *
 * @author John Mazzitelli
 * @author Jay Shaughnessy
 */
@Local
public interface BundleManagerLocal extends BundleManagerRemote {

    // Methods in the Local are not exposed to the remote API.  This is is typically due to:
    // - used strictly for internal processing (like generating history records)
    // - transactional reasons (like needing REQUIRES_NEW)
    // - security reasons
    // - used for testing only
    // - legacy reasons

    /**
     * Internal use only
     * </p>
     * Called internally to add history when action is taken against a deployment. This executes
     * in a New Transaction and supports deployBundle and Agent requests.
     * </p>
     * This method performs NO AUTHZ!
     * </p>
     * @param subject
     * @param resourceDeploymentId id of the deployment appending the history record
     * @param history
     * @return the persisted history
     */
    BundleResourceDeploymentHistory addBundleResourceDeploymentHistoryInNewTrans(Subject subject,
        int resourceDeploymentId, BundleResourceDeploymentHistory history) throws Exception;

    /**
     * Used by the UI. Performs security checks.
     *
     * @param subject the caller
     * @param resourceDeploymentId id of the deployment
     *
     * @return the list of audit entities, or an empty list if the caller is not authorized to view this deployment.
     */
    List<BundleResourceDeploymentHistory> getBundleResourceDeploymentHistories(Subject subject, int resourceDeploymentId);

    /**
     * Internal use only, and test entry point.
     * <pre>
     * Required Permissions (same as createInitialBundleVersionXxx): Either:
     * - Global.CREATE_BUNDLES and Global.VIEW_BUNDLES
     * - Global.CREATE_BUNDLES and BundleGroup.VIEW_BUNDLES_IN_GROUP for bundle group BG
     * - BundleGroup.CREATE_BUNDLES_IN_GROUP for bundle group BG
     * </pre>
     * @param subject user that must have proper permissions
     * @param name not null or empty
     * @param description optional long description of the bundle
     * @param bundleTypeId valid bundleType
     * @param bundleGroupIds existing bundle groups for initial bundle assignment, null or 0 length for unassigned
     * @return the persisted Bundle (id is assigned)
     */
    Bundle createBundle(Subject subject, String name, String description, int bundleTypeId, int[] bundleGroupIds)
        throws Exception;

    /**
     * Internal use only and test entry point.
     *
     * Convenience method that combines {@link #createBundle(Subject, String, int)} and {@link #createBundleVersion(Subject, int, String, String, String)}.
     * This will first check to see if a bundle with the given type/name exists - if it doesn't, it will be created. If it does, it will be reused.
     * This will then create the bundle version that will be associated with the bundle that was created or found.
     * <pre>
     * Required Permissions (same as createInitialBundleVersionXxx): Either:
     * - Global.CREATE_BUNDLES and Global.VIEW_BUNDLES
     * - Global.CREATE_BUNDLES and BundleGroup.VIEW_BUNDLES_IN_GROUP for bundle group BG
     * - BundleGroup.CREATE_BUNDLES_IN_GROUP for bundle group BG
     * </pre>
     * @param subject user that must have proper permissions
     * @param bundleName name of the bundle to use (if not found, it will be created)
     * @param bundleDescription optional long description of the bundle
     * @param bundleTypeId the bundle type for the new bundle (if it is created) for which this will be the first version
     * @param bundleGroupIds the bundle groups for the new bundle (if it is created) for which this will be the first version. null to leave unassigned.
     * @param bundleVersionName name of the bundle version
     * @param bundleVersionDescription optional long description of the bundle version
     * @param version optional. If not supplied set to 1.0 for first version, or incremented (as best as possible) for subsequent version
     * @return the persisted BundleVersion (id is assigned)
     * @deprecated since 4.13 this is only used in tests - no need to have this in the API
     */
    @Deprecated
    BundleVersion createBundleAndBundleVersion(Subject subject, String bundleName, String bundleDescription,
        int bundleTypeId, int[] bundleGroupIds, String bundleVersionName, String bundleVersionDescription,
        String version, String recipe) throws Exception;

    /**
     * Internal use only, test entry point
     * <pre>
     * Required Permissions: Either:
     * - Global.CREATE_BUNDLES and Global.VIEW_BUNDLES
     * - Global.CREATE_BUNDLES and BundleGroup.VIEW_BUNDLES_IN_GROUP for bundle group BG and the relevant bundle is assigned to BG
     * - BundleGroup.CREATE_BUNDLES_IN_GROUP for bundle group BG and the relevant bundle is assigned to BG
     * </pre>
     * @param subject user that must have proper permissions
     * @param bundleId the bundle for which this will be the next version
     * @param name not null or empty
     * @param description optional long description of the bundle version
     * @param version optional. If not supplied set to 1.0 for first version, or incremented (as best as possible) for subsequent version
     * @return the persisted BundleVersion (id is assigned)
     *
     * @deprecated since 4.13, use one of the create(Initial)BundleVersionVia* methods
     */
    @Deprecated
    BundleVersion createBundleVersion(Subject subject, int bundleId, String name, String description, String version,
        String recipe) throws Exception;

    /**
     * Used internally for transaction demarcation purposes.
     *
     * @param bundle the bundle to create version of
     * @param name the name of the bundle version
     * @param version the version of the bundle version
     * @param description the description of the bundle version
     * @param recipe the recipe of the bundle version
     * @param configurationDefinition the configuration definition of the deployment properties
     * @return the created bundle version
     * @throws Exception on error
     * @since 4.13
     */
    BundleVersion createBundleVersionInternal(Bundle bundle, String name, String version, String description,
        String recipe, ConfigurationDefinition configurationDefinition) throws Exception;

    /**
     * Not generally called. For use by Server Side Plugins when registering a Bundle Plugin.
     * </p>
     * Required Permissions:
     * - Global.CREATE_BUNDLES
     * </p>
     * @param subject
     * @param name not null or empty
     * @param resourceTypeId id of the ResourceType that handles this BundleType
     * @return the persisted BundleType (id is assigned)
     */
    BundleType createBundleType(Subject subject, String name, int resourceTypeId) throws Exception;

    /**
     * This is typically not called directly, typically scheduleBundleResourceDeployment() is called externally.
     * This executes in a New Transaction and supports scheduleBundleResourceDeployment.
     * </p>
     * This method performs NO AUTHZ!
     * </p>
     */
    BundleResourceDeployment createBundleResourceDeploymentInNewTrans(Subject subject, int bundleDeploymentId,
        int resourceId) throws Exception;

    /**
     * Similar to {@link BundleManagerRemote#createBundleDeployment(Subject, int, int, String, Configuration)} but
     * supplies the internally generated deploymentName and has different transaction semantics. Useful when an
     * slsb method needs to both create a deployment and schedules it prior to returning to an external caller.
     * </p>
     * This method performs NO AUTHZ!
     * </p>
     */
    public BundleDeployment createBundleDeploymentInNewTrans(Subject subject, int bundleVersionId,
        int bundleDestinationId, String name, String description, Configuration configuration) throws Exception;

    /**
     * Used by GUI
     * </p>
     * Required Permissions: Either:
     * - Global.CREATE_BUNDLES and Global.VIEW_BUNDLES
     * - Global.CREATE_BUNDLES and BundleGroup.VIEW_BUNDLES_IN_GROUP for bundle group BG and the relevant bundle is assigned to BG
     * - BundleGroup.CREATE_BUNDLES_IN_GROUP for bundle group BG and the relevant bundle is assigned to BG
     * </p>
     *
     * @param subject
     * @param bundleVersionId
     * @return Map, filename to foundInBundleVersion
     * @throws Exception
     */
    HashMap<String, Boolean> getAllBundleVersionFilenames(Subject subject, int bundleVersionId) throws Exception;

    /**
     * Used by GUI. Needed by the Bundle Deploy and Revert wizards GUI to generate a deployment name for display.
     * <pre>
     * Required Permissions: Either:
     * - Global.DEPLOY_BUNDLES and a view of the relevant bundle and a view of the relevant resource group (may involve multiple roles)
     * - Resource.DEPLOY_BUNDLES_TO_GROUP and a view of the relevant bundle and a view of the relevant resource group (may involve multiple roles)
     * </pre>
     *
     * @param subject
     * @param bundleDestinationId required
     * @param bundleVersionId required for progressive deployment, -1 for revert
     * @param prevDeploymentId required for revert deployment, -1 for progressive
     * @return
     */
    public String getBundleDeploymentName(Subject subject, int bundleDestinationId, int bundleVersionId,
        int prevDeploymentId);

    /**
     * Internal use only. A special case method to build the pojo that can be sent to the agent to
     * schedule the deployment request. Uses NOT_SUPPORTED transaction attribute to avoid having the cleaned pojo
     * affect the persistence context.
     * </p>
     * This method performs NO AUTHZ!
     * </p>
     *
     * @throws Exception
     */
    public BundleScheduleRequest getScheduleRequest(Subject subject, int resourceDeploymentId,
        boolean isCleanDeployment, boolean isRevert) throws Exception;

    /**
     * Used by GUI. The deployment must be PENDING or in a completed state.
     * <pre>
     * Required Permissions: Either:
     * - Global.DEPLOY_BUNDLES and a view of the relevant bundle and a view of the relevant resource group (may involve multiple roles)
     * - Resource.DEPLOY_BUNDLES_TO_GROUP and a view of the relevant bundle and a view of the relevant resource group (may involve multiple roles)
     * </pre>
     *
     * @param subject
     * @param bundleDeploymentId
     * @throws Exception if any part of the removal fails.
     */
    void deleteBundleDeployment(Subject subject, int bundleDeploymentId) throws Exception;

    /**
     * Internal use only. Called internally to set deployment status. Typically to a completion status when deployment
     * ends.  Exists for transaction boundary reasons only.
     * </p>
     * This method performs NO AUTHZ!
     * </p>
     *
     * @param subject
     * @param resourceDeploymentId id of the resource deployment appending the history record
     * @param status
     * @return the updated {@link BundleResourceDeployment}
     */
    BundleResourceDeployment setBundleResourceDeploymentStatusInNewTransaction(Subject subject,
        int resourceDeploymentId, BundleDeploymentStatus status) throws Exception;

    /**
     * Internal use only
     * </p>
     * When {@link #purgeBundleDestination(Subject, int)} is done, it
     * calls this so the purge can be finalized. This is required because this method is called with
     * a transactional context, as opposed to the main purge method.
     * </p>
     * This method performs NO AUTHZ!
     * </p>
     *
     * @param subject
     * @param bundleDeployment
     * @param failedToPurge
     * @throws Exception
     */
    void _finalizePurge(Subject subject, BundleDeployment bundleDeployment,
        Map<BundleResourceDeployment, String> failedToPurge) throws Exception;

    /**
     * Like {@link #createBundleVersionViaFile(Subject, File)} with one additional feature.
     * This method exists solely to support the GUI's wizard workflow which always first tries to create a bundle
     * version for an existing bundle, because it does not know whether this is an initial bundle version (only
     * the server can figure that out after it cracks open the bundle distribution, parses the recipe, and
     * looks for the bundle).  If this is an initialBundleVersion this method does two things. First, it stores the
     * distribution file as a temp file, this is done to avoid having to upload the file a second time. Second, it
     * throws IllegalStateException with special message text, a token that can be sent back to
     * {@link #createInitialBundleVersionViaToken(Subject, int[], String)}.
     */
    BundleVersion createOrStoreBundleVersionViaFile(Subject subject, File distributionFile) throws Exception;

    /**
     * Like {@link #createBundleVersionViaURL(Subject, String, String, String)}  with one additional feature.
     * This method exists solely to support the GUI's wizard workflow which always first tries to create a bundle
     * version for an existing bundle, because it does not know whether this is an initial bundle version (only
     * the server can figure that out after it cracks open the bundle distribution, parses the recipe, and
     * looks for the bundle).  If this is an initialBundleVersion this method does two things. First, it stores the
     * distribution file as a temp file, this is done to avoid having to upload the file a second time. Second, it
     * throws IllegalStateException with special message text, a token that can be sent back to
     * {@link #createInitialBundleVersionViaToken(Subject, int[], String)}.
     */
    BundleVersion createOrStoreBundleVersionViaURL(Subject subject, String distributionFileUrl, String username,
        String password) throws Exception;

    /**
     * This method exists solely to support the GUI's wizard workflow which always first tries to create a bundle
     * version for an existing bundle, because it does not know whether this is an initial bundle version (only
     * the server can figure that out after it cracks open the bundle distribution, parses the recipe, and
     * looks for the bundle).  It works in conjunction with {@link #createOrStoreBundleVersionViaFile(Subject, File)} or
     * {@link #createOrStoreBundleVersionViaURL(Subject, String, String, String)}.
     * <p/>
     * This method will use the supplied token to access the distribution file. It assumes this is a new bundle and
     * is responsible for creating the bundle as well as the bundle version. The caller can indicate which bundle
     * groups the new bundle should be assigned to. If bundleGroupId is null, then the new bundle will not be
     * associated with any bundle group - this is only allowed if the caller has the permission Global.VIEW_BUNDLES.
     * <pre>
     * Required Permissions: Either:
     * - Global.CREATE_BUNDLES and Global.VIEW_BUNDLES
     * - Global.CREATE_BUNDLES and BundleGroup.VIEW_BUNDLES_IN_GROUP for bundle group BG
     * - BundleGroup.CREATE_BUNDLES_IN_GROUP for bundle group BG
     * </pre>
     * @param subject user that must have proper permissions
     * @param bundleGroupIds identifies the bundle groups that the new bundle will be associated with; null or zero
     * length to leave unassigned.
     * @param token the token used to identify the distribution file stashed as a temp file.
     * @return the persisted BundleVersion with a lot of the internal relationships filled in to help the caller
     *         understand all that this method did.
     */
    BundleVersion createInitialBundleVersionViaToken(Subject subject, int[] bundleGroupIds, String token)
        throws Exception;

    /**
     * For the calling subject determines which bundle groups to which the user can assign the bundle. The composite
     * includes a <code>Map<BundleGroup,Boolean></code> indicating the assignable BundleGroups and which are
     * currently assigned.   It also indicates whether the bundle can be left unassigned.  When querying for
     * new bundles the bundleId should be set to 0.
     *
     * @param subject, the calling subject
     * @param assigningSubject, the subject relevant to the bundle group assignment
     * @param bundleId, the bundle relevant to the bundle group assignment, or 0 for a new bundle
     * @return
     * @throws Exception
     */
    BundleGroupAssignmentComposite getAssignableBundleGroups(Subject subject, Subject assigningSubject, int bundleId)
        throws Exception;

    /**
     * Determines (and updates) the deployment status of the provided bundle deployment based on the deployment statuses
     * of the underlying resource deployments.
     *
     * @param bundleDeploymentId the id of the bundle deployment to check
     * @return the determined bundle deployment status
     *
     * @since 4.10
     */
    BundleDeploymentStatus determineBundleDeploymentStatus(int bundleDeploymentId);

    /**
     * Internal use only. Exists for transaction boundary reasons only.
     * </p>
     * This method performs NO AUTHZ!
     * </p>
     */
    BundleDeployment scheduleBundleDeploymentInNewTransaction(Subject subject, int bundleDeploymentId,
        boolean isCleanDeployment, boolean isRevert, Integer revertedDeploymentReplacedDeployment) throws Exception;

    /**
     * Deletes all Bundles, Bundle types connected to given resource type
     * @param subject
     * @param resourceType
     * @throws Exception
     */
    void deleteMetadata(Subject subject, ResourceType resourceType) throws Exception;

}
