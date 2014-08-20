package org.rhq.enterprise.server.storage.maintenance.step;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.criteria.ResourceOperationHistoryCriteria;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.operation.bean.ResourceOperationSchedule;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.storage.maintenance.JobProperties;

/**
 * @author John Sanda
 */
public abstract class ResourceOperationStepRunner extends BaseStepRunner {

    protected static final int DEFAULT_OPERATION_TIMEOUT = 300;
    private static final Log log = LogFactory.getLog(ResourceOperationStepRunner.class);

    private String operation;

    protected ResourceOperationHistory history;

    protected ResourceOperationStepRunner(String operation) {
        this.operation = operation;
    }

    @Override
    public void execute() throws StepFailureException {
        Configuration configuration = step.getConfiguration();
        String targetAddress = getTarget();
        PropertyMap params = (PropertyMap) configuration.get(JobProperties.PARAMETERS);
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

        history = executeOperation(targetAddress, operation, operationParams);
        if (history.getStatus() != OperationRequestStatus.SUCCESS) {
            throw new StepFailureException("Resource operation [" + operation + "] against " + targetAddress +
                " failed: " + history.getErrorMessage());
        }

    }

    protected String getTarget() {
        Configuration configuration = step.getConfiguration();
        return configuration.getSimpleValue(JobProperties.TARGET);
    }

    protected ResourceOperationHistory executeOperation(String storageNodeAddress, String operation,
        Configuration parameters) {
        StorageNode node = storageNodeManager.findStorageNodeByAddress(storageNodeAddress);
        int resourceId = node.getResource().getId();
        ResourceOperationSchedule operationSchedule = operationManager.scheduleResourceOperation(
            subjectManager.getOverlord(), resourceId, operation, 0, 0, 0, StartStorageClient.DEFAULT_OPERATION_TIMEOUT,
            parameters.deepCopyWithoutProxies(), "");
        return waitForOperationToComplete(operationSchedule);
    }

    private ResourceOperationHistory waitForOperationToComplete(ResourceOperationSchedule schedule) {
        try {
            ResourceOperationHistoryCriteria criteria = new ResourceOperationHistoryCriteria();
            criteria.addFilterJobId(schedule.getJobId());

            Thread.sleep(5000);

            ResourceOperationHistory history = getHistory(criteria);

            while (history.getStatus() == OperationRequestStatus.INPROGRESS) {
                Thread.sleep(5000);
                history = getHistory(criteria);
            }
            // Now that the operation is done, do one more fetch to load the results
            criteria.fetchResults(true);

            return getHistory(criteria);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private ResourceOperationHistory getHistory(ResourceOperationHistoryCriteria criteria) {
        PageList<ResourceOperationHistory> results = operationManager.findResourceOperationHistoriesByCriteria(
            subjectManager.getOverlord(), criteria);
        if (results.size() != 1) {
            throw new RuntimeException("Failed to find resource operation history, instead found " + results);
        }
        return results.get(0);
    }
}
