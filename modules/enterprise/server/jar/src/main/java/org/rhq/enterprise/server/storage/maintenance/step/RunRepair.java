package org.rhq.enterprise.server.storage.maintenance.step;

/**
 * @author John Sanda
 */
public class RunRepair extends ResourceOperationStepRunner {

    public RunRepair() {
        super("repair");
    }

    @Override
    public StepFailureStrategy getFailureStrategy() {
        return StepFailureStrategy.ABORT;
    }
}
