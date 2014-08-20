/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
package org.rhq.enterprise.server.storage.maintenance.job;

import java.util.Set;

import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.storage.MaintenanceStep;
import org.rhq.enterprise.server.storage.maintenance.JobProperties;
import org.rhq.enterprise.server.storage.maintenance.StorageMaintenanceJob;
import org.rhq.enterprise.server.storage.maintenance.step.DecommissionStorageNode;
import org.rhq.enterprise.server.storage.maintenance.step.DeleteStorageNode;
import org.rhq.enterprise.server.storage.maintenance.step.RemoveMaintenance;
import org.rhq.enterprise.server.storage.maintenance.step.UnannounceStorageNode;
import org.rhq.enterprise.server.storage.maintenance.step.UninstallStorageNode;
import org.rhq.enterprise.server.storage.maintenance.step.UpdateStorageNodeStatus;

/**
 * @author John Sanda
 */
public class UndeployNode extends DeployNode {

    @Override
    public StorageMaintenanceJob calculateSteps(StorageMaintenanceJob job) {
        Set<String> clusterSnapshot = job.getClusterSnapshot();
        String nodeAddress = job.getTarget();

        job.addStep(new MaintenanceStep()
            .setName(UpdateStorageNodeStatus.class.getName())
            .setDescription("Update operation mode of " + nodeAddress + " to " +
                StorageNode.OperationMode.DECOMMISSION)
            .setConfiguration(new Configuration.Builder()
                .addSimple(JobProperties.TARGET, nodeAddress)
                .addSimple(JobProperties.OPERATION_MODE, StorageNode.OperationMode.DECOMMISSION.toString())
                .build()));

        job.addStep(new MaintenanceStep()
            .setName(DecommissionStorageNode.class.getName())
            .setDescription("Decommission " + nodeAddress)
            .setConfiguration(new Configuration.Builder()
                .addSimple(JobProperties.TARGET, nodeAddress)
                .build()));

        SchemaChanges schemaChanges = determineSchemaChanges(clusterSnapshot.size(), clusterSnapshot.size() - 1);
        applySchemaChanges(job, schemaChanges);

        job.addStep(new MaintenanceStep()
            .setName(UpdateStorageNodeStatus.class.getName())
            .setDescription("Update operation mode of " + nodeAddress + " to " +
                StorageNode.OperationMode.REMOVE_MAINTENANCE)
            .setConfiguration(new Configuration.Builder()
                .addSimple(JobProperties.TARGET, nodeAddress)
                .addSimple(JobProperties.OPERATION_MODE, StorageNode.OperationMode.REMOVE_MAINTENANCE.toString())
                .build()));

        for (String address : clusterSnapshot) {
            job.addStep(new MaintenanceStep()
                .setName(RemoveMaintenance.class.getName())
                .setDescription("Run cluster maintenance on " + address)
                .setConfiguration(new Configuration.Builder()
                    .addSimple(JobProperties.TARGET, address)
                    .openMap(JobProperties.PARAMETERS)
                        .addSimple("removedNodeAddress", nodeAddress)
                        .addSimple("runRepair", schemaChanges.replicationFactor != null)
                        .addSimple("updateSeedsList", true)
                        .openList("seedsList", "seedsList")
                            .addSimples(job.getClusterSnapshot().toArray(new String[job.getClusterSnapshot().size()]))
                        .closeList()
                    .closeMap()
                    .build()));
        }

        job.addStep(new MaintenanceStep()
            .setName(UpdateStorageNodeStatus.class.getName())
            .setDescription("Update operation mode of " + nodeAddress + " to " +
                StorageNode.OperationMode.UNANNOUNCE)
            .setConfiguration(new Configuration.Builder()
                .addSimple(JobProperties.TARGET, nodeAddress)
                .addSimple(JobProperties.OPERATION_MODE, StorageNode.OperationMode.UNANNOUNCE.toString())
                .build()));

        for (String address : clusterSnapshot) {
            job.addStep(new MaintenanceStep()
                .setName(UnannounceStorageNode.class.getName())
                .setDescription("Unannouncing " + nodeAddress + " to " + address)
                .setConfiguration(new Configuration.Builder()
                    .addSimple(JobProperties.TARGET, address)
                    .openMap(JobProperties.PARAMETERS)
                        .addSimple("address", nodeAddress)
                    .closeMap()
                    .build()));
        }

        job.addStep(new MaintenanceStep()
            .setName(UpdateStorageNodeStatus.class.getName())
            .setDescription("Update operation mode of " + nodeAddress + " to " +
                StorageNode.OperationMode.UNINSTALL)
            .setConfiguration(new Configuration.Builder()
                .addSimple(JobProperties.TARGET, nodeAddress)
                .addSimple(JobProperties.OPERATION_MODE, StorageNode.OperationMode.UNINSTALL.toString())
                .build()));

        job.addStep(new MaintenanceStep()
            .setName(UninstallStorageNode.class.getName())
            .setDescription("Uninstalling " + nodeAddress)
            .setConfiguration(new Configuration.Builder()
                .addSimple(JobProperties.TARGET, nodeAddress)
                .build()));

        job.addStep(new MaintenanceStep()
            .setName(DeleteStorageNode.class.getName())
            .setDescription("Deleting " + nodeAddress + " from inventory")
            .setConfiguration(new Configuration.Builder()
                .addSimple(JobProperties.TARGET, nodeAddress)
                .build()));

        return job;
    }

    @Override
    public void updateSteps(StorageMaintenanceJob job, MaintenanceStep failedStep) {
    }

}
