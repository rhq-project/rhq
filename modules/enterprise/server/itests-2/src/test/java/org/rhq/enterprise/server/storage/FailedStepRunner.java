package org.rhq.enterprise.server.storage;

import java.util.concurrent.atomic.AtomicBoolean;

import org.rhq.core.domain.storage.MaintenanceStep;
import org.rhq.enterprise.server.storage.maintenance.step.StepFailureException;
import org.rhq.enterprise.server.storage.maintenance.step.StepFailureStrategy;

/**
 * @author John Sanda
 */
public class FailedStepRunner extends FakeStepRunner {

    private StepFailureStrategy failureStrategy;

    public FailedStepRunner(StepFailureStrategy failureStrategy) {
        this.failureStrategy = failureStrategy;
    }

    public FailedStepRunner(String stepName, int stepNumber, StepFailureStrategy failureStrategy) {
        super(stepName, stepNumber);
        this.failureStrategy = failureStrategy;
    }

    public FailedStepRunner(AtomicBoolean executed, String stepName, int stepNumber,
        StepFailureStrategy failureStrategy) {
        super(executed, stepName, stepNumber);
        this.failureStrategy = failureStrategy;
    }

    @Override
    public void execute(MaintenanceStep maintenanceStep) throws StepFailureException {
        super.execute(maintenanceStep);
        throw new RuntimeException(maintenanceStep.getName() + " failed");
    }

    @Override
    public StepFailureStrategy getFailureStrategy() {
        return failureStrategy;
    }
}
