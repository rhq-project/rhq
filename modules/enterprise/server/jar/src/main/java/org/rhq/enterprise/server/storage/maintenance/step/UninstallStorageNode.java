package org.rhq.enterprise.server.storage.maintenance.step;

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
