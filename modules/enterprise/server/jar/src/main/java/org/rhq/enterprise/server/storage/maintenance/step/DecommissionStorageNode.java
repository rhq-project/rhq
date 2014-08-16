package org.rhq.enterprise.server.storage.maintenance.step;

/**
 * @author John Sanda
 */
public class DecommissionStorageNode extends ResourceOperationStepRunner {

    public DecommissionStorageNode() {
        super("decommission");
    }

    @Override
    public StepFailureStrategy getFailureStrategy() {
        return StepFailureStrategy.ABORT;
    }
}
