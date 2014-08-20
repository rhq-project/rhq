package org.rhq.enterprise.server.storage;

import java.util.concurrent.atomic.AtomicBoolean;

import org.rhq.enterprise.server.storage.maintenance.StepFailureException;
import org.rhq.enterprise.server.storage.maintenance.StepFailureStrategy;
import org.rhq.enterprise.server.storage.maintenance.step.BaseStepRunner;

/**
 * @author John Sanda
 */
public class FakeStepRunner extends BaseStepRunner {

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
    public void execute() throws StepFailureException {
        executed.set(true);
    }

    @Override
    public StepFailureStrategy getFailureStrategy() {
        return null;
    }

}
