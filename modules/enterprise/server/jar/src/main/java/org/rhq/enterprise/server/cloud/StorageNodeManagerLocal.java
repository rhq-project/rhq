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

import java.util.List;
import java.util.Map;

import javax.ejb.Local;

import com.google.common.util.concurrent.ListenableFuture;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.cloud.StorageClusterSettings;
import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.cloud.StorageNodeConfigurationComposite;
import org.rhq.core.domain.cloud.StorageNodeLoadComposite;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.operation.bean.ResourceOperationSchedule;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageList;

@Local
public interface StorageNodeManagerLocal extends StorageNodeManagerRemote {

    // The following have package visibility to make accessible to StorageNodeManagerBeanTest
    String STORAGE_NODE_GROUP_NAME = "RHQ Storage Nodes";
    String STORAGE_NODE_RESOURCE_TYPE_NAME = "RHQ Storage Node";
    String STORAGE_NODE_PLUGIN_NAME = "RHQStorage";

    /**
     * @return All storage nodes, including those that are not part of the cluster.
     */
    List<StorageNode> getStorageNodes();

    /**
     * @return All storage nodes that are part of the cluster.
     */
    List<StorageNode> getClusterNodes();

    PageList<StorageNodeLoadComposite> getStorageNodeComposites(Subject subject);

    void linkResource(Resource resource);

    StorageNode linkExistingStorageNodeToResource(StorageNode storageNode);

    ListenableFuture<List<StorageNodeLoadComposite>> getLoadAsync(Subject subject, StorageNode node, long beginTime,
        long endTime);

    void updateConfigurationAsync(Subject subject, StorageNodeConfigurationComposite storageNodeConfiguration);

    StorageNode findStorageNodeByAddress(String address);

    StorageNode updateStorageNode(StorageNode node);

    /**
     * Find ids for all resources and sub-resources of Storage Nodes that
     * have alert definitions. This can be used by the resource criteria queries to find
     * all alerts triggered for storage nodes resources.
     *
     * @return map with resource (with defined alert) id as a key and storage node id as a value
     */
    Map<Integer, Integer> findResourcesWithAlertDefinitions();

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

    void scheduleOperationInNewTransaction(Subject subject, ResourceOperationSchedule schedule);

    Map<String, List<MeasurementDataNumericHighLowComposite>> findStorageNodeLoadDataForLast(Subject subject, StorageNode node, long beginTime, long endTime, int numPoints);

    StorageNode createStorageNode(Resource resource, StorageClusterSettings clusterSettings);

    /**
     * Resets all StorageNode errorMessage and failedOperation values to null.
     * Done in new trans to ensure ensuing logic sees the reset and avoids locking.
     */
    void resetInNewTransaction();
}
