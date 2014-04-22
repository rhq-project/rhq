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
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.server.cloud;

import javax.ejb.Remote;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.cloud.StorageNodeConfigurationComposite;
import org.rhq.core.domain.cloud.StorageNodeLoadComposite;
import org.rhq.core.domain.criteria.StorageNodeCriteria;
import org.rhq.core.domain.util.PageList;

/**
 * Remote interface to the manager responsible for creating and managing storage nodes.
 *
 * @author Jirka Kremser
 */
@Remote
public interface StorageNodeManagerRemote {

    /**
     * <p>Returns the summary of load of the storage node.</p>
     *
     * <p>The subject needs to have <code>MANAGE_SETTINGS</code> permissions.</p>
     *
     * @param subject   user that must have proper permissions
     * @param node      storage node entity (it can be a new object, but the id should be set properly)
     * @param beginTime the start time
     * @param endTime   the end time
     * @return          instance of {@link StorageNodeLoadComposite} with the aggregate measurement data of selected metrics
     */
    StorageNodeLoadComposite getLoad(Subject subject, StorageNode node, long beginTime, long endTime);

    /**
     * <p>Returns the current configuration of the storage node.</p>
     * <p>For updating the configuration see {@link #retrieveConfiguration(Subject,StorageNode)}.</p>
     * <p>The subject needs to have <code>MANAGE_SETTINGS</code> permissions.</p>
     *
     * @param subject           user that must have proper permissions
     * @param storageNode       the storage node for which we want to get the configuration
     * @return                  instance of {@link StorageNodeConfigurationComposite} with the configuration properties
     *
     * @since 4.9
     */
    StorageNodeConfigurationComposite retrieveConfiguration(Subject subject, StorageNode storageNode);

    /**
     * <p>Updates the current configuration of the storage node.</p>
     * <p>For retrieving the configuration see {@link #updateConfiguration(Subject,StorageNodeConfigurationComposite)}.</p>
     * <p>The subject needs to have <code>MANAGE_SETTINGS</code> permissions.</p>
     *
     * @param subject                   user that must have proper permissions
     * @param storageNodeConfiguration  instance of {@link StorageNodeConfigurationComposite} with the configuration properties
     * @return                          true if the update was successful
     *
     * @since 4.9
     */
    boolean updateConfiguration(Subject subject, StorageNodeConfigurationComposite storageNodeConfiguration);

    /**
     * <p>Fetches the list of {@link StorageNode} entities based on provided criteria.</p>
     *
     * <p>The subject needs to have <code>MANAGE_SETTINGS</code> permissions.</p>
     *
     * @param subject   user that must have proper permissions
     * @param criteria  the criteria
     * @return          list of nodes
     */
    PageList<StorageNode> findStorageNodesByCriteria(Subject subject, StorageNodeCriteria criteria);

    /**
     * <p>Fetches the list of Storage Node related alerts that have not yet been acknowledged.</p>
     *
     * <p>The subject needs to have <code>MANAGE_SETTINGS</code> permissions.</p>
     *
     * @param subject   user that must have proper permissions
     * @return          storage nodes alerts not acknowledged
     *
     * @since 4.9
     */
    PageList<Alert> findNotAcknowledgedStorageNodeAlerts(Subject subject);

    /**
     * <p>Fetches the list of Storage Node related alerts that have not yet been acknowledged for the
     * specified storage node.</p>
     *
     * <p>The subject needs to have <code>MANAGE_SETTINGS</code> permissions.</p>
     *
     * @param subject     user that must have proper permissions
     * @param storageNode the storage node
     * @return            storage nodes alerts not acknowledged
     *
     * @since 4.9
     */
    PageList<Alert> findNotAcknowledgedStorageNodeAlerts(Subject subject, StorageNode storageNode);

    /**
     * <p>Fetches all the Storage Node related alerts.</p>
     *
     * <p>The subject needs to have <code>MANAGE_SETTINGS</code> permissions.</p>
     *
     * @param subject   user that must have proper permissions
     * @return          all storage nodes alerts
     *
     * @since 4.9
     */
    PageList<Alert> findAllStorageNodeAlerts(Subject subject);

    /**
     * <p>Fetches all the Storage Node related alerts for the specified storage node.</p>
     *
     * <p>The subject needs to have <code>MANAGE_SETTINGS</code> permissions.</p>
     *
     * @param subject     user that must have proper permissions
     * @param storageNode the storage node
     * @return            all storage nodes alerts
     *
     * @since 4.9
     */
    PageList<Alert> findAllStorageNodeAlerts(Subject subject, StorageNode storageNode);

    /**
     * <p>Runs the deploy operations on the given storage node. This operation should ensure the node will be part of the RHQ storage cluster.</p>
     * <p>This will move the storage node from operation mode <code>INSTALLED</code> to the mode <code>NORMAL</code> going through following phases:</p>
     * <ol>
     *  <li><code>ANNOUNCE</code></li>
     *  <li><code>BOOTSTRAP</code></li>
     *  <li><code>ADD_MAINTENANCE</code></li>
     * </ol>
     * <p>This can be run also on a storage node that is in any intermediate modes mentioned above, because of some failure during the deployment process.</p>
     *
     * <p>The subject needs to have <code>MANAGE_SETTINGS</code> permissions.</p>
     *
     * @see                     <a href="https://docs.jboss.org/author/display/RHQ/Deploying+Storage+Nodes">https://docs.jboss.org/author/display/RHQ/Deploying+Storage+Nodes</a>
     * @param sbubject          user that must have proper permissions
     * @param storageNode       storage node to be deployed to the cluster
     *
     * @since 4.9
     */
    void deployStorageNode(Subject sbubject, StorageNode storageNode);

    /**
     * <p>Runs the undeploy operations on the given storage node. This operation should ensure the node will removed from the RHQ storage cluster.</p>
     * <p>This will move the storage node from operation mode <code>NORMAL</code> or any other mode to the mode <code>UNINSTALL</code>. At the end of the day,
     *  the node is removed also from the relational database. Currently there is no way to add it back.</p>
     * <p>WARNING: Run this operation only if you know what you are doing.</p>
     *
     * <p>The subject needs to have <code>MANAGE_SETTINGS</code> permissions.</p>
     *
     * @see                     <a href="https://docs.jboss.org/author/display/RHQ/Deploying+Storage+Nodes">https://docs.jboss.org/author/display/RHQ/Deploying+Storage+Nodes</a>
     * @param subject          user that must have proper permissions
     * @param storageNode       storage node to be deployed to the cluster
     *
     * @since 4.9
     */
    void undeployStorageNode(Subject subject, StorageNode storageNode);

    /**
     * <p>
     * Schedules read repair to run on the storage cluster. The repair operation is executed one node at a time. This
     * method is invoked from {@link org.rhq.enterprise.server.scheduler.jobs.StorageClusterReadRepairJob StorageClusterReadRepairJob}
     * as part of regularly scheduled maintenance.
     * </p>
     * <p>
     * <strong>NOTE:</strong> Repair is one of the most resource-intensive operations that a storage node performs. Make
     * sure you know what you are doing if you invoke this method outside of the regularly scheduled maintenance window.
     * </p>
     *
     * <p>the subject needs to have <code>MANAGE_SETTINGS</code> permissions.</p>
     *
     * @param subject   user that must have proper permissions
     *
     * @since 4.9
     */
    void runClusterMaintenance(Subject subject);
}
