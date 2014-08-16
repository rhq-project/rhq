package org.rhq.enterprise.server.storage.maintenance.step;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.storage.MaintenanceStep;

/**
 * @author John Sanda
 */
public class DeleteStorageNode extends BaseStepRunner {

    private static final Log log = LogFactory.getLog(DeleteStorageNode.class);

    @Override
    public void execute(MaintenanceStep step) throws StepFailureException {
        String address = step.getConfiguration().getSimpleValue("targetAddress");
        log.info("Deleting storage node " + address);
        storageNodeManager.deleteStorageNode(address);
        log.info("Storage node " + address + " has been deleted");
    }

    @Override
    public StepFailureStrategy getFailureStrategy() {
        return StepFailureStrategy.ABORT;
    }
}
