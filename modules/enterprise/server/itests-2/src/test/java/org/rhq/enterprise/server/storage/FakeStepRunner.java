package org.rhq.enterprise.server.storage;

import java.util.concurrent.atomic.AtomicBoolean;

import org.rhq.core.domain.storage.MaintenanceStep;
import org.rhq.enterprise.server.storage.maintenance.step.BaseStepRunner;
import org.rhq.enterprise.server.storage.maintenance.step.StepFailureException;
import org.rhq.enterprise.server.storage.maintenance.step.StepFailureStrategy;

/**
 * @author John Sanda
 */
public class FakeStepRunner extends BaseStepRunner {

    public AtomicBoolean executed = new AtomicBoolean();

    public String stepName;

    public int stepNumber;

    public FakeStepRunner() {
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

}
