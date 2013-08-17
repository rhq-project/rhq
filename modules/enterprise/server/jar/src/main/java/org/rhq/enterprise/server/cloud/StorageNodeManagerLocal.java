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
package org.rhq.enterprise.server.cloud;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.cloud.StorageNodeConfigurationComposite;
import org.rhq.core.domain.cloud.StorageNodeLoadComposite;
import org.rhq.core.domain.criteria.StorageNodeCriteria;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.operation.bean.ResourceOperationSchedule;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageList;

@Local
public interface StorageNodeManagerLocal {

    // The following have package visibility to make accessible to StorageNodeManagerBeanTest
    String STORAGE_NODE_GROUP_NAME = "RHQ Storage Nodes";
    String STORAGE_NODE_RESOURCE_TYPE_NAME = "RHQ Storage Node";
    String STORAGE_NODE_PLUGIN_NAME = "RHQStorage";

    List<StorageNode> getStorageNodes();
    
    PageList<StorageNodeLoadComposite> getStorageNodeComposites();

    void linkResource(Resource resource);

    /**
     * <p>Returns the summary of load of the storage node.</p>
     *
     * <p>the subject needs to have <code>MANAGE_SETTINGS</code> permissions.</p>
     *
     * @param subject   user that must have proper permissions
     * @param node      storage node entity (it can be a new object, but the id should be set properly)
     * @param beginTime the start time
     * @param endTime   the end time
     * @return instance of {@link StorageNodeLoadComposite} with the aggregate measurement data of selected metrics
     */
    StorageNodeLoadComposite getLoad(Subject subject, StorageNode node, long beginTime, long endTime);

    StorageNodeConfigurationComposite retrieveConfiguration(Subject subject, StorageNode storageNode);

    boolean updateConfiguration(Subject subject, StorageNodeConfigurationComposite storageNodeConfiguration);
    
    void updateConfigurationAsync(Subject subject, StorageNodeConfigurationComposite storageNodeConfiguration);

    /**
     * Fetches the list of StorageNode entities based on provided criteria.
     *
     * the subject needs to have MANAGE_SETTINGS permissions.
     *
     * @param subject caller
     * @param criteria the criteria
     * @return list of nodes
     */
    PageList<StorageNode> findStorageNodesByCriteria(Subject subject, StorageNodeCriteria criteria);

    /**
     * Fetches the list of Storage Node related alerts that have not yet been acknowledged.
     *
     * @param subject subject
     * @return storage nodes alerts not acknowledged
     */
    PageList<Alert> findNotAcknowledgedStorageNodeAlerts(Subject subject);

    /**
     * Fetches the list of Storage Node related alerts that have not yet been acknowledged for the
     * specified storage node.
     *
     * @param subject subject
     * @return storage nodes alerts not acknowledged
     */
    PageList<Alert> findNotAcknowledgedStorageNodeAlerts(Subject subject, StorageNode storageNode);

    /**
     * Fetches all the Storage Node related alerts.
     *
     * @param subject subject
     * @return all storage nodes alerts
     */
    PageList<Alert> findAllStorageNodeAlerts(Subject subject);

    /**
     * Fetches all the Storage Node related alerts for the specified storage node.
     *
     * @param subject subject
     * @return all storage nodes alerts
     */
    PageList<Alert> findAllStorageNodeAlerts(Subject subject, StorageNode storageNode);

    StorageNode findStorageNodeByAddress(InetAddress address);


    /**
     * Find ids for all resources and sub-resources of Storage Nodes that
     * have alert definitions. This can be used by the resource criteria queries to find
     * all alerts triggered for storage nodes resources.
     *
     * @return resource ids
     */
    Integer[] findResourcesWithAlertDefinitions();

    /**
     * Find ids for all resources and sub-resources, of the specified storage node, that
     * have alert definitions. This can be used by the resource criteria queries to find
     * all alerts triggered for storage nodes resources.
     *
     * If storage node is null it find ids for all resources and sub-resources of Storage Nodes that
     * have alert definitions. Please see {@link #findResourcesWithAlertDefinitions()} for more details.
     *
     * @param storageNode storage node
     *
     * @return resource ids
     */
    Integer[] findResourcesWithAlertDefinitions(StorageNode storageNode);


    /**
     * <p>Prepares the node for subsequent upgrade.</p>
     * <p> CAUTION: this method will set the RHQ server to maintenance mode, RHQ storage flushes all the data to disk
     * and backup of all the keyspaces is created</p>
     * <p>the subject needs to have <code>MANAGE_SETTINGS</code> and <code>MANAGE_INVENTORY</code> permissions.</p>
     *
     * @param subject caller
     * @param storageNode storage node on which the prepareForUpgrade operation should be run
     */
    void prepareNodeForUpgrade(Subject subject, StorageNode storageNode);

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
     */
    void runReadRepair();

    void scheduleOperationInNewTransaction(Subject subject, ResourceOperationSchedule schedule);

    Map<String, List<MeasurementDataNumericHighLowComposite>> findStorageNodeLoadDataForLast(Subject subject, StorageNode node, long beginTime, long endTime, int numPoints);

    StorageNode createStorageNode(Resource resource);

    void deployStorageNode(Subject subject, StorageNode storageNode);

    void undeployStorageNode(Subject subject, StorageNode storageNode);
}
