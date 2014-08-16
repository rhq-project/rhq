package org.rhq.enterprise.server.storage.maintenance.step;

/**
 * @author John Sanda
 */
public class UnannounceStorageNode extends ResourceOperationStepRunner {

    public UnannounceStorageNode() {
        super("unannounce");
    }

    @Override
    public StepFailureStrategy getFailureStrategy() {
        return StepFailureStrategy.CONTINUE;
    }
}
