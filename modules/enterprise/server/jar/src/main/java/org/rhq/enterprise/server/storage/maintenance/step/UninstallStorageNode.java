package org.rhq.enterprise.server.storage.maintenance.step;

import org.rhq.enterprise.server.storage.maintenance.StepFailureStrategy;

/**
 * @author John Sanda
 */
public class UninstallStorageNode extends ResourceOperationStepRunner {

    public UninstallStorageNode() {
        super("uninstall");
    }

    @Override
    public StepFailureStrategy getFailureStrategy() {
        return StepFailureStrategy.ABORT;
    }
}
