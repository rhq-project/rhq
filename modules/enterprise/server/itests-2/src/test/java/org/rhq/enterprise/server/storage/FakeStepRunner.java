package org.rhq.enterprise.server.storage;

import java.util.concurrent.atomic.AtomicBoolean;

import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.criteria.ResourceOperationHistoryCriteria;
import org.rhq.core.domain.operation.OperationHistory;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.operation.bean.ResourceOperationSchedule;
import org.rhq.core.domain.storage.MaintenanceStep;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.storage.maintenance.step.BaseStepRunner;
import org.rhq.enterprise.server.storage.maintenance.step.StartStorageClient;
import org.rhq.enterprise.server.storage.maintenance.step.StepFailureException;
import org.rhq.enterprise.server.storage.maintenance.step.StepFailureStrategy;

/**
 * @author John Sanda
 */
public class FakeStepRunner extends BaseStepRunner {

    protected static final int DEFAULT_OPERATION_TIMEOUT = 300;
    public AtomicBoolean executed = new AtomicBoolean();

    public String stepName;

    public int stepNumber;

    public FakeStepRunner() {
    }

    public FakeStepRunner(String stepName, int stepNumber) {
        this.stepName = stepName;
        this.stepNumber = stepNumber;
    }

    public FakeStepRunner(AtomicBoolean executed) {
        this.executed = executed;
    }

    public FakeStepRunner(AtomicBoolean executed, String stepName, int stepNumber) {
        this.executed = executed;
        this.stepName = stepName;
        this.stepNumber = stepNumber;
    }

    @Override
    public void execute(MaintenanceStep maintenanceStep) throws StepFailureException {
        executed.set(true);
    }

    @Override
    public StepFailureStrategy getFailureStrategy() {
        return null;
    }

    protected OperationHistory executeOperation(String storageNodeAddress, String operation, Configuration parameters) {
        StorageNode node = storageNodeManager.findStorageNodeByAddress(storageNodeAddress);
        int resourceId = node.getResource().getId();
        ResourceOperationSchedule operationSchedule = operationManager.scheduleResourceOperation(
            subjectManager.getOverlord(), resourceId, operation, 0, 0, 0, StartStorageClient.DEFAULT_OPERATION_TIMEOUT,
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
