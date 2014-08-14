package org.rhq.enterprise.server.storage.maintenance.step;

import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.criteria.ResourceOperationHistoryCriteria;
import org.rhq.core.domain.operation.OperationHistory;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.operation.bean.ResourceOperationSchedule;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.cloud.StorageNodeManagerLocal;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.storage.StorageClientManager;

/**
 * @author John Sanda
 */
public abstract class BaseStepRunner implements MaintenanceStepRunner {

    private static final int DEFAULT_OPERATION_TIMEOUT = 300;

    protected StorageNodeManagerLocal storageNodeManager;

    protected OperationManagerLocal operationManager;

    protected SubjectManagerLocal subjectManager;

    protected StorageClientManager storageClientManager;

    @Override
    public void setStorageNodeManager(StorageNodeManagerLocal storageNodeManager) {
        this.storageNodeManager = storageNodeManager;
    }

    @Override
    public void setOperationManager(OperationManagerLocal operationManager) {
        this.operationManager = operationManager;
    }

    @Override
    public void setStorageClientManager(StorageClientManager storageClientManager) {
        this.storageClientManager = storageClientManager;
    }

    @Override
    public void setSubjectManager(SubjectManagerLocal subjectManager) {
        this.subjectManager = subjectManager;
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
