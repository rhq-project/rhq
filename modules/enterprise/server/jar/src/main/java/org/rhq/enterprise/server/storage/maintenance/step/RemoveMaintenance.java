package org.rhq.enterprise.server.storage.maintenance.step;

import org.rhq.enterprise.server.storage.maintenance.StepFailureStrategy;

/**
 * @author John Sanda
 */
public class RemoveMaintenance extends ResourceOperationStepRunner {

    public RemoveMaintenance() {
        super("removeNodeMaintenance");
    }

    @Override
    public StepFailureStrategy getFailureStrategy() {
        return StepFailureStrategy.ABORT;
    }
}
