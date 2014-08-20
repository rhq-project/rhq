package org.rhq.enterprise.server.storage;

import java.util.concurrent.atomic.AtomicBoolean;

import org.rhq.enterprise.server.storage.maintenance.StepFailureException;
import org.rhq.enterprise.server.storage.maintenance.StepFailureStrategy;
import org.rhq.enterprise.server.storage.maintenance.StorageMaintenanceJob;

/**
 * @author John Sanda
 */
public class FailedStepRunner extends FakeStepRunner {

    private StepFailureStrategy failureStrategy;

    private StorageMaintenanceJob jobForFailedStep;

    public FailedStepRunner(StepFailureStrategy failureStrategy) {
        this.failureStrategy = failureStrategy;
    }

    public FailedStepRunner(String stepName, int stepNumber, StepFailureStrategy failureStrategy) {
        super(stepName, stepNumber);
        this.failureStrategy = failureStrategy;
    }

    public FailedStepRunner(String stepName, int stepNumber, StepFailureStrategy failureStrategy,
        StorageMaintenanceJob jobForFailedStep) {
        super(stepName, stepNumber);
        this.failureStrategy = failureStrategy;
        this.jobForFailedStep = jobForFailedStep;
    }

    public FailedStepRunner(AtomicBoolean executed, String stepName, int stepNumber,
        StepFailureStrategy failureStrategy) {
        super(executed, stepName, stepNumber);
        this.failureStrategy = failureStrategy;
    }

    @Override
    public void execute() throws StepFailureException {
        throw new RuntimeException(step.getName() + " failed");
    }

    @Override
    public StepFailureStrategy getFailureStrategy() {
        return failureStrategy;
    }

    @Override
    public StorageMaintenanceJob createNewJobForFailedStep() {
        return jobForFailedStep;
    }
}
