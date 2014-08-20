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

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.storage.MaintenanceStep;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.storage.StorageClusterSettingsManagerLocal;
import org.rhq.enterprise.server.storage.maintenance.StorageMaintenanceJob;
import org.rhq.enterprise.server.storage.maintenance.step.ShutdownStorageClient;
import org.rhq.enterprise.server.storage.maintenance.step.StartStorageClient;
import org.rhq.enterprise.server.storage.maintenance.step.UpdateStorageNodeEndpoints;
import org.rhq.enterprise.server.storage.maintenance.step.UpdateStorageNodeEntity;

public class ChangeAddressJob implements StepCalculator {

    private StorageClusterSettingsManagerLocal clusterSettingsManager;

    public void setClusterSettingsManager(StorageClusterSettingsManagerLocal clusterSettingsManager) {
        this.clusterSettingsManager = clusterSettingsManager;
    }

    private SubjectManagerLocal subjectManager;

    public void setSubjectManager(SubjectManagerLocal subjectManager) {
        this.subjectManager = subjectManager;
    }

    @Override
    public StorageMaintenanceJob calculateSteps(StorageMaintenanceJob job) {
        Set<String> clusterSnapshot = job.getClusterSnapshot();
        PropertyMap parametersMap = job.getJobParameters();
        String newNodeAddress = parametersMap.getSimple("newNodeAddress").getStringValue();
        String currentNodeAddress = parametersMap.getSimple("currentNodeAddress").getStringValue();

        if(clusterSnapshot.size() == 1) {
            MaintenanceStep step = new MaintenanceStep()
                .setName(ShutdownStorageClient.class.getName())
                .setDescription("Shutting down storage cluster communication before updating storage node address.");
            job.addStep(step);

            step = new MaintenanceStep()
                .setName(UpdateStorageNodeEntity.class.getName())
                .setDescription("Update storage node entity with the new address.")
                .setConfiguration(new Configuration.Builder()
                    .addSimple("currentNodeAddress", currentNodeAddress)
                    .addSimple("newNodeAddress", newNodeAddress)
                    .build());
            job.addStep(step);

            step = new MaintenanceStep()
                .setName(UpdateStorageNodeEndpoints.class.getName())
                .setDescription("Update storage node entity with the new address.")
                .setConfiguration(new Configuration.Builder()
                    .addSimple("currentNodeAddress", currentNodeAddress)
                    .addSimple("newNodeAddress", newNodeAddress).build());
            job.addStep(step);

            step = new MaintenanceStep()
                .setName(StartStorageClient.class.getName())
                .setDescription("Restarting storage cluster communication.");
            job.addStep(step);
        }

        return job;
    }

    @Override
    public void updateSteps(StorageMaintenanceJob originalJob, MaintenanceStep failedStep) {
    }
}
