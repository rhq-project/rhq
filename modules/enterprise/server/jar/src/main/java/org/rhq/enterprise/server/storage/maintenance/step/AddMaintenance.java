package org.rhq.enterprise.server.storage.maintenance.step;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.storage.MaintenanceStep;

/**
 * @author John Sanda
 */
public class AddMaintenance extends BaseStepRunner {

    private static final Log log = LogFactory.getLog(AddMaintenance.class);

    @Override
    public void execute(MaintenanceStep maintenanceStep) throws StepFailureException {
        Configuration configuration = maintenanceStep.getConfiguration();
        String targetAddress = configuration.getSimpleValue("targetAddress");

        log.info("Running cluster maintenance on " + targetAddress);

        Configuration operationParams = new Configuration();
        operationParams.put(configuration.get("runRepair").deepCopy(false));
        operationParams.put(configuration.get("newNodeAddress").deepCopy(false));
        operationParams.put(configuration.get("updateSeedsList").deepCopy(false));
        operationParams.put(configuration.get("seedsList").deepCopy(false));

        executeOperation(targetAddress, "addNodeMaintenance", operationParams);
    }

    @Override
    public StepFailureStrategy getFailureStrategy() {
        return StepFailureStrategy.ABORT;
    }
}
