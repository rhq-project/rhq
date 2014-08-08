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

import java.util.HashMap;
import java.util.Map;

import javax.ejb.EJB;

import org.rhq.core.domain.storage.MaintenanceJob;
import org.rhq.core.domain.storage.MaintenanceStep;
import org.rhq.enterprise.server.storage.maintenance.step.MaintenanceStepRunner;
import org.rhq.enterprise.server.storage.maintenance.step.ShutdownStorageClient;
import org.rhq.enterprise.server.storage.maintenance.step.StartStorageClient;
import org.rhq.enterprise.server.storage.maintenance.step.UpdateStorageNodeEndpoints;
import org.rhq.enterprise.server.storage.maintenance.step.UpdateStorageNodeEntity;

public class MaintenanceJobRunner {

    @EJB
    StartStorageClient startStorageClient;

    @EJB
    ShutdownStorageClient shutdownStorageClient;

    @EJB
    UpdateStorageNodeEndpoints updateStorageNodeEndpoints;

    @EJB
    UpdateStorageNodeEntity updateStorageNodeEntity;


    public void runJob(MaintenanceJob job) {
        Map<String, MaintenanceStepRunner> runners = new HashMap<String, MaintenanceStepRunner>();
        runners.put(startStorageClient.getClass().getSimpleName(), startStorageClient);
        runners.put(shutdownStorageClient.getClass().getSimpleName(), shutdownStorageClient);
        runners.put(updateStorageNodeEndpoints.getClass().getSimpleName(), updateStorageNodeEndpoints);
        runners.put(updateStorageNodeEntity.getClass().getSimpleName(), updateStorageNodeEntity);


        for (MaintenanceStep step : job.getMaintenanceSteps()) {
            try {
                runners.get(step.getName()).execute(step);
            } catch (Exception e) {
                //do nothing for now ... exception handling to be decided a later
            }
        }
    }
}
