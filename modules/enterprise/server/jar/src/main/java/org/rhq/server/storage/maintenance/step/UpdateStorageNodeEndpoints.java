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

import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.common.JobTrigger;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.criteria.ResourceOperationHistoryCriteria;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.operation.bean.ResourceOperationSchedule;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.storage.MaintenanceJob;
import org.rhq.core.domain.storage.MaintenanceStep;
import org.rhq.core.domain.storage.MaintenanceStep.Type;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.auth.SubjectManagerBean;
import org.rhq.enterprise.server.cloud.StorageNodeManagerBean;
import org.rhq.enterprise.server.operation.OperationManagerBean;

/**
 * @author Stefan Negrea
 *
 */
public class UpdateStorageNodeEndpoints implements MaintenanceStepFacade {

    @EJB
    private StorageNodeManagerBean storageNodeManager;

    @EJB
    private SubjectManagerBean subjectManager;

    @EJB
    private OperationManagerBean operationManager;

    @Override
    public void execute(MaintenanceStep maintenanceStep) throws Exception {

        StorageNode storageNode = storageNodeManager.findStorageNodeByAddress(maintenanceStep.getNodeAddress());
        Resource storageNodeResource = storageNode.getResource();
        //scheduling the operation
        long operationStartTime = System.currentTimeMillis();

        ResourceOperationSchedule newSchedule = new ResourceOperationSchedule();
        newSchedule.setJobTrigger(JobTrigger.createNowTrigger());
        newSchedule.setResource(storageNodeResource);
        newSchedule.setOperationName("updateEndpoints");
        newSchedule.setDescription("Run by StorageNodeManagerBean");
        newSchedule.setParameters(new Configuration());

        storageNodeManager.scheduleOperationInNewTransaction(subjectManager.getOverlord(), newSchedule);

        //waiting for the operation result then return it
        int iteration = 0;
        boolean successResultFound = false;
        while (iteration < 10 && !successResultFound) {
            ResourceOperationHistoryCriteria criteria = new ResourceOperationHistoryCriteria();
            criteria.addFilterResourceIds(storageNodeResource.getId());
            criteria.addFilterStartTime(operationStartTime);
            criteria.addFilterOperationName("updateEndpoints");
            criteria.addFilterStatus(OperationRequestStatus.SUCCESS);
            criteria.setPageControl(PageControl.getUnlimitedInstance());

            PageList<ResourceOperationHistory> results = operationManager.findResourceOperationHistoriesByCriteria(
                subjectManager.getOverlord(), criteria);

            if (results != null && results.size() > 0) {
                successResultFound = true;
            }

            if (successResultFound) {
                break;
            } else {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                }
            }

            iteration++;
        }

        if (!successResultFound) {
            throw new Exception();
        }
    }

    @Override
    public MaintenanceStep build(MaintenanceJob job, int stepNumber, String[] existingNodes, String affectedNode) {
        MaintenanceStep step = new MaintenanceStep();
        step.setStep(stepNumber)
            .setName(UpdateStorageNodeEndpoints.class.getSimpleName())
            .setNodeAddress(affectedNode)
            .setType(Type.ResourceOperation)
            .setSequential(true)
            .setTimeout(1000)
            .setMaintenanceJob(job);

        return step;
    }

}
