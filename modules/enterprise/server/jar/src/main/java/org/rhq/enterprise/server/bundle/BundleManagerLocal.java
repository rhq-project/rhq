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

import java.util.HashMap;
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
import org.rhq.core.domain.configuration.Configuration;

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
     * @param bundleGroupId bundle group, existing bundle group for bundle assignment, or 0 for unassigned
     * @return the persisted Bundle (id is assigned)
     */
    Bundle createBundle(Subject subject, String name, String description, int bundleTypeId, int bundleGroupId)
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
     * @param bundleGroupId the bundle group for the new bundle (if it is created) for which this will be the first version. 0 to leave unassigned. 
     * @param bundleVersionName name of the bundle version
     * @param bundleVersionDescription optional long description of the bundle version
     * @param version optional. If not supplied set to 1.0 for first version, or incremented (as best as possible) for subsequent version
     * @return the persisted BundleVersion (id is assigned)
     */
    BundleVersion createBundleAndBundleVersion(Subject subject, String bundleName, String bundleDescription,
        int bundleTypeId, int bundleGroupId, String bundleVersionName, String bundleVersionDescription, String version,
        String recipe) throws Exception;

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
     */
    BundleVersion createBundleVersion(Subject subject, int bundleId, String name, String description, String version,
        String recipe) throws Exception;

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
     * Used by GUI. This is a simple attempt at delete, typically used for removing a poorly defined destination. It will
     * fail if any actual deployments are referring to the destination.
     * <pre>
     * Required Permissions: Either:
     * - Global.DEPLOY_BUNDLES and a view of the relevant bundle and a view of the relevant resource group (may involve multiple roles)
     * - Resource.DEPLOY_BUNDLES_TO_GROUP and a view of the relevant bundle and a view of the relevant resource group (may involve multiple roles)
     * </pre>
     *
     * @param subject
     * @param bundleDestinationId
     * @throws Exception if any part of the removal fails.
     */
    void deleteBundleDestination(Subject subject, int bundleDestinationId) throws Exception;

    /**
     * Internal use only. Called internally to set deployment status. Typically to a completion status when deployment 
     * ends.
     * </p>
     * This method performs NO AUTHZ!
     * </p> 
     *
     * @param subject
     * @param resourceDeploymentId id of the resource deployment appending the history record
     * @param status
     * @return the updated {@link BundleResourceDeployment}
     */
    BundleResourceDeployment setBundleResourceDeploymentStatus(Subject subject, int resourceDeploymentId,
        BundleDeploymentStatus status) throws Exception;

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

}
