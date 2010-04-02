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

import java.util.Map;

import javax.ejb.Local;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.bundle.BundleDeploymentHistory;
import org.rhq.core.domain.bundle.BundleDeploymentStatus;
import org.rhq.core.domain.bundle.BundleGroupDeployment;
import org.rhq.core.domain.bundle.BundleType;

/**
 * Local interface to the manager responsible for creating and managing bundles.
 *  
 * @author John Mazzitelli
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
     * Called internally to add history when action is taken against a deployment. This executes
     * in a New Transaction and supports deployBundle and Agent requests.
     * 
     * @param subject
     * @param bundleDeploymentId id of the deployment appending the history record
     * @param history
     * @return the persisted history
     */
    BundleDeploymentHistory addBundleDeploymentHistory(Subject subject, int bundleDeploymentId,
        BundleDeploymentHistory history) throws Exception;

    /**
     * Not generally called. For use by Server Side Plugins when registering a Bundle Plugin.
     *  
     * @param subject must be InventoryManager
     * @param name not null or empty
     * @param resourceTypeId id of the ResourceType that handles this BundleType   
     * @return the persisted BundleType (id is assigned)
     */
    BundleType createBundleType(Subject subject, String name, int resourceTypeId) throws Exception;

    /**
     * This is typically not called directly, typically scheduleBundleDeployment() is called externally. This executes
     * in a New Transaction and supports scheduleBundleDeployment. 
     */
    BundleDeployment createBundleDeployment(Subject subject, int bundleDeployDefinitionId, int resourceId,
        int bundleGroupDeploymentId) throws Exception;

    /**
     * This is typically not called directly, typically scheduleBundleGroupDeployment() is called externally.
     * This executes in a New Transaction and supports scheduleBundleGroupDeployment. 
     */
    BundleGroupDeployment createBundleGroupDeployment(BundleGroupDeployment bundleGroupDeployment) throws Exception;

    // added here because the same method in @Remote was commented out to bypass a WSProvide issue
    Map<String, Boolean> getAllBundleVersionFilenames(Subject subject, int bundleVersionId) throws Exception;

    /**
     * Called internally to set deployment status. Typically to a completion status when deployment ends.
     * 
     * @param subject
     * @param bundleDeploymentId id of the deployment appending the history record
     * @param status
     * @return the updated BundleDeployment  
     */
    BundleDeployment setBundleDeploymentStatus(Subject subject, int bundleDeploymentId, BundleDeploymentStatus status)
        throws Exception;

}
