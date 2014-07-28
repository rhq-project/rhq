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
package org.rhq.server.storage.maintenance.step;

import javax.ejb.EJB;

import org.rhq.core.domain.storage.MaintenanceJob;
import org.rhq.core.domain.storage.MaintenanceStep;
import org.rhq.core.domain.storage.MaintenanceStep.Type;
import org.rhq.enterprise.server.storage.StorageClientManager;

/**
 * @author Stefan Negrea
 *
 */
public class StartStorageClient implements MaintenanceStepFacade {

    @EJB
    private StorageClientManager storageClientManager;

    @Override
    public void execute(MaintenanceStep maintenanceStep) {
        storageClientManager.init();
    }

    @Override
    public MaintenanceStep build(MaintenanceJob job, int stepNumber, String[] existingNodes, String affectedNode) {
        MaintenanceStep step = new MaintenanceStep();

        step.setStep(stepNumber)
            .setName(StartStorageClient.class.getSimpleName())
            .setType(Type.ServerUpdate)
            .setSequential(true)
            .setTimeout(1000)
            .setMaintenanceJob(job);

        return step;
    }
}
