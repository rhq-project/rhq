package org.rhq.enterprise.server.storage.maintenance.step;

import org.rhq.enterprise.server.storage.maintenance.StepFailureStrategy;

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
