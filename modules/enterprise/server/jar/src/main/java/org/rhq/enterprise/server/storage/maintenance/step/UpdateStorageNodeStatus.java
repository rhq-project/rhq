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
package org.rhq.enterprise.server.storage.maintenance.step;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.criteria.ResourceOperationHistoryCriteria;
import org.rhq.core.domain.operation.OperationHistory;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.operation.bean.ResourceOperationSchedule;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.storage.maintenance.JobProperties;
import org.rhq.enterprise.server.storage.maintenance.StepFailureException;
import org.rhq.enterprise.server.storage.maintenance.StepFailureStrategy;

/**
 * @author John Sanda
 */
public class UpdateStorageNodeStatus extends BaseStepRunner {

    protected static final int DEFAULT_OPERATION_TIMEOUT = 300;
    private static final Log log = LogFactory.getLog(UpdateStorageNodeStatus.class);

    @Override
    public void execute() throws StepFailureException {
        Configuration configuration = step.getConfiguration();
        String targetAddress = configuration.getSimpleValue(JobProperties.TARGET);
        String mode = configuration.getSimpleValue(JobProperties.OPERATION_MODE);
        StorageNode.OperationMode operationMode = getOperationMode(mode);

        log.info("Updating operation mode of " + targetAddress + " to " + mode);

        StorageNode node = storageNodeManager.findStorageNodeByAddress(targetAddress);
        node.setOperationMode(operationMode);

        storageNodeManager.updateStorageNode(node);
    }

    private StorageNode.OperationMode getOperationMode(String mode) {
        if (mode.equals(StorageNode.OperationMode.ANNOUNCE.toString())) {
            return StorageNode.OperationMode.ANNOUNCE;
        } else if (mode.equals(StorageNode.OperationMode.BOOTSTRAP.toString())) {
            return StorageNode.OperationMode.BOOTSTRAP;
        } else if (mode.equals(StorageNode.OperationMode.ADD_MAINTENANCE.toString())) {
            return StorageNode.OperationMode.ADD_MAINTENANCE;
        } else if (mode.equals(StorageNode.OperationMode.NORMAL.toString())) {
            return StorageNode.OperationMode.NORMAL;
        } else if (mode.equals(StorageNode.OperationMode.DECOMMISSION.toString())) {
            return StorageNode.OperationMode.DECOMMISSION;
        } else if (mode.equals(StorageNode.OperationMode.UNANNOUNCE.toString())) {
            return StorageNode.OperationMode.UNANNOUNCE;
        } else if (mode.equals(StorageNode.OperationMode.UNINSTALL.toString())) {
            return StorageNode.OperationMode.UNINSTALL;
        } else if (mode.equals(StorageNode.OperationMode.REMOVE_MAINTENANCE.toString())) {
            return StorageNode.OperationMode.REMOVE_MAINTENANCE;
        } else if (mode.equals(Server.OperationMode.DOWN.toString())) {
            return StorageNode.OperationMode.DOWN;
        } else {
            throw new IllegalArgumentException(mode + " is not a recognized operation mode");
        }
    }

    @Override
    public StepFailureStrategy getFailureStrategy() {
        return StepFailureStrategy.ABORT;
    }

    protected OperationHistory executeOperation(String storageNodeAddress, String operation, Configuration parameters) {
        StorageNode node = storageNodeManager.findStorageNodeByAddress(storageNodeAddress);
        int resourceId = node.getResource().getId();
        ResourceOperationSchedule operationSchedule = operationManager.scheduleResourceOperation(
            subjectManager.getOverlord(), resourceId, operation, 0, 0, 0, DEFAULT_OPERATION_TIMEOUT,
            parameters.deepCopyWithoutProxies(), "");
        return waitForOperationToComplete(operationSchedule);
    }

    private OperationHistory waitForOperationToComplete(ResourceOperationSchedule schedule) {
        try {
            ResourceOperationHistoryCriteria criteria = new ResourceOperationHistoryCriteria();
            criteria.addFilterJobId(schedule.getJobId());

            Thread.sleep(5000);
            PageList<ResourceOperationHistory> results = operationManager.findResourceOperationHistoriesByCriteria(
                subjectManager.getOverlord(), criteria);
            if (results.isEmpty()) {
                throw new RuntimeException("Failed to find resource operation history for " + schedule);
            }
            OperationHistory history = results.get(0);


            while (history.getStatus() == OperationRequestStatus.INPROGRESS) {
                Thread.sleep(5000);
                history = operationManager.getOperationHistoryByHistoryId(subjectManager.getOverlord(),
                    history.getId());
            }
            return history;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
