package org.rhq.enterprise.server.storage.maintenance.step;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.operation.OperationHistory;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.storage.MaintenanceStep;

/**
 * @author John Sanda
 */
public class AnnounceStorageNode extends BaseStepRunner implements MaintenanceStepRunner {

    private static final Log log = LogFactory.getLog(AnnounceStorageNode.class);

    @Override
    public void execute(MaintenanceStep step) throws StepFailureException {
        log.info("Announcing new node");
        Configuration configuration = step.getConfiguration();
        String targetAddress = configuration.getSimpleValue("targetAddress");
        PropertyMap params = (PropertyMap) configuration.get("parameters");

        Configuration operationParams = new Configuration();
        operationParams.put(new PropertySimple("address", params.getSimple("address").getStringValue()));
        OperationHistory operationHistory = executeOperation(targetAddress, "announce", operationParams);

        if (operationHistory.getStatus() == OperationRequestStatus.FAILURE) {
            throw new StepFailureException("Failed to announce " + targetAddress + " to " +
                params.getSimple("address") + ": " + operationHistory.getErrorMessage());
        }
    }

    @Override
    public StepFailureStrategy getFailureStrategy() {
        return StepFailureStrategy.ABORT;
    }
}
