package org.rhq.enterprise.server.storage.maintenance.step;

import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.criteria.ResourceOperationHistoryCriteria;
import org.rhq.core.domain.operation.OperationHistory;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.operation.bean.ResourceOperationSchedule;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.cloud.StorageNodeManagerLocal;
import org.rhq.enterprise.server.operation.OperationManagerLocal;

/**
 * @author John Sanda
 */
public abstract class BaseStepRunner implements MaintenanceStepRunner {

    private static final int DEFAULT_OPERATION_TIMEOUT = 300;

    private StorageNodeManagerLocal storageNodeManager;

    private OperationManagerLocal operationManager;

    private SubjectManagerLocal subjectManager;

    @Override
    public void setStorageNodeManager(StorageNodeManagerLocal storageNodeManager) {
        this.storageNodeManager = storageNodeManager;
    }

    @Override
    public void setOperationManager(OperationManagerLocal operationManager) {
        this.operationManager = operationManager;
    }

    @Override
    public void setSubjectManager(SubjectManagerLocal subjectManager) {
        this.subjectManager = subjectManager;
    }

    protected void executeOperation(String storageNodeAddress, String operation, Configuration parameters) {
        StorageNode node = storageNodeManager.findStorageNodeByAddress(storageNodeAddress);
        int resourceId = node.getResource().getId();
        ResourceOperationSchedule operationSchedule = operationManager.scheduleResourceOperation(
            subjectManager.getOverlord(), resourceId, operation, 0, 0, 0, DEFAULT_OPERATION_TIMEOUT,
            parameters.deepCopyWithoutProxies(), "");
        waitForOperationToComplete(operationSchedule);
    }

    private OperationHistory waitForOperationToComplete(ResourceOperationSchedule schedule) {
        try {
            ResourceOperationHistoryCriteria criteria = new ResourceOperationHistoryCriteria();
            criteria.addFilterJobId(schedule.getJobId());

            Thread.sleep(5000);
            OperationHistory operationHistory = operationManager.getOperationHistoryByJobId(
                subjectManager.getOverlord(), schedule.getJobId().toString());
            while (operationHistory.getStatus() == OperationRequestStatus.INPROGRESS) {
                Thread.sleep(5000);
                operationHistory = operationManager.getOperationHistoryByJobId(subjectManager.getOverlord(),
                    schedule.getJobId().toString());
            }
            return operationHistory;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
