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
package org.rhq.server.storage.maintenance;

import java.util.ArrayList;
import java.util.List;

import org.rhq.core.domain.storage.MaintenanceJob;
import org.rhq.core.domain.storage.MaintenanceStep;
import org.rhq.server.storage.maintenance.step.MaintenanceStepFacade;
import org.rhq.server.storage.maintenance.step.ShutdownStorageClient;
import org.rhq.server.storage.maintenance.step.StartStorageClient;
import org.rhq.server.storage.maintenance.step.UpdateStorageNodeEndpoints;
import org.rhq.server.storage.maintenance.step.UpdateStorageNodeEntity;

/**
 * @author Stefan Negrea
 *
 */
public class StorageMaintenanceJobFactory {

    private StorageMaintenanceJobFactory() {
    }

    public MaintenanceJob createJob(String operation, String[] existingStorageNodes, String[] affectedNodes, String args) {
        MaintenanceJob job = new MaintenanceJob();

        job.setType(1);
        job.setName(operation);

        if (operation.equals("NodeChangeAddress")) {
            job.setSteps(createNodeChangeAddressSteps(job, existingStorageNodes, affectedNodes));
        }

        return job;
    }

    public List<MaintenanceStep> createNodeChangeAddressSteps(MaintenanceJob job, String[] existingNodes,
        String[] affectedNodes) {

        List<MaintenanceStep> steps = new ArrayList<MaintenanceStep>();

        if (existingNodes.length == 1) {
            int stepCount = 0;

            MaintenanceStepFacade stepBuilder = new UpdateStorageNodeEntity();
            steps.add(stepBuilder.build(job, stepCount++, existingNodes, affectedNodes[0]));

            stepBuilder = new ShutdownStorageClient();
            steps.add(stepBuilder.build(job, stepCount++, existingNodes, affectedNodes[0]));

            stepBuilder = new UpdateStorageNodeEndpoints();
            steps.add(stepBuilder.build(job, stepCount++, existingNodes, affectedNodes[0]));

            stepBuilder = new StartStorageClient();
            steps.add(stepBuilder.build(job, stepCount++, existingNodes, affectedNodes[0]));
        }

        return steps;
    }

}
