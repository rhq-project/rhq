package org.rhq.enterprise.server.storage;

import org.rhq.core.domain.storage.MaintenanceStep;
import org.rhq.enterprise.server.storage.maintenance.step.BaseStepRunner;
import org.rhq.enterprise.server.storage.maintenance.step.StepFailureException;
import org.rhq.enterprise.server.storage.maintenance.step.StepFailureStrategy;

/**
 * @author John Sanda
 */
public class FakeStepRunner extends BaseStepRunner {

    @Override
    public void execute(MaintenanceStep maintenanceStep) throws StepFailureException {
    }

    @Override
    public StepFailureStrategy getFailureStrategy() {
        return null;
    }

}
