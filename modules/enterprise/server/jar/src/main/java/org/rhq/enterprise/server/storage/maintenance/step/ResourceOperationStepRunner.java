package org.rhq.enterprise.server.storage.maintenance.step;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.criteria.ResourceOperationHistoryCriteria;
import org.rhq.core.domain.operation.OperationHistory;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.operation.bean.ResourceOperationSchedule;
import org.rhq.core.domain.storage.MaintenanceStep;
import org.rhq.core.domain.util.PageList;

/**
 * @author John Sanda
 */
public abstract class ResourceOperationStepRunner extends BaseStepRunner {

    protected static final int DEFAULT_OPERATION_TIMEOUT = 300;
    private static final Log log = LogFactory.getLog(ResourceOperationStepRunner.class);

    private String operation;

    protected ResourceOperationStepRunner(String operation) {
        this.operation = operation;
    }

    @Override
    public void execute(MaintenanceStep step) throws StepFailureException {
        Configuration configuration = step.getConfiguration();
        String targetAddress = configuration.getSimpleValue("targetAddress");
        PropertyMap params = (PropertyMap) configuration.get("parameters");
        Configuration operationParams = new Configuration();

        if (params != null) {
            for (String name : params.getMap().keySet()) {
                operationParams.put(params.get(name).deepCopy(false));
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Scheduling resource operation [" + operation + "] against " + targetAddress + " with parameters " +
                operationParams.toString(true));
        } else {
            log.info("Scheduling resource operation [" + operation + "] against " + targetAddress);
        }

        OperationHistory operationHistory = executeOperation(targetAddress, operation, operationParams);
        if (operationHistory.getStatus() != OperationRequestStatus.SUCCESS) {
            throw new StepFailureException("Resource operation [" + operation + "] against " + targetAddress +
                " failed: " + operationHistory.getErrorMessage());
        }

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
