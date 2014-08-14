package org.rhq.enterprise.server.storage.maintenance.step;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.storage.MaintenanceStep;

/**
 * @author John Sanda
 */
public class UpdateStorageNodeStatus extends BaseStepRunner {

    private static final Log log = LogFactory.getLog(UpdateStorageNodeStatus.class);

    @Override
    public void execute(MaintenanceStep step) throws StepFailureException {
        Configuration configuration = step.getConfiguration();
        String targetAddress = configuration.getSimpleValue("targetAddress");
        String mode = configuration.getSimpleValue("operationMode");
        StorageNode.OperationMode operationMode = getOperationMode(mode);

        log.info("Updating operation mode of " + targetAddress + " to " + mode);

        StorageNode node = storageNodeManager.findStorageNodeByAddress(targetAddress);
        node.setOperationMode(operationMode);

        storageNodeManager.updateStorageNode(node);
    }

    private StorageNode.OperationMode getOperationMode(String mode) {
        if (mode.equals(StorageNode.OperationMode.ANNOUNCE.toString())) {
            return StorageNode.OperationMode.ANNOUNCE;
        } else if (mode.equals(StorageNode.OperationMode.BOOTSTRAP.toString())) {
            return StorageNode.OperationMode.BOOTSTRAP;
        } else if (mode.equals(StorageNode.OperationMode.ADD_MAINTENANCE.toString())) {
            return StorageNode.OperationMode.ADD_MAINTENANCE;
        } else if (mode.equals(StorageNode.OperationMode.NORMAL.toString())) {
            return StorageNode.OperationMode.NORMAL;
        } else if (mode.equals(StorageNode.OperationMode.DECOMMISSION.toString())) {
            return StorageNode.OperationMode.DECOMMISSION;
        } else if (mode.equals(StorageNode.OperationMode.UNANNOUNCE.toString())) {
            return StorageNode.OperationMode.UNANNOUNCE;
        } else if (mode.equals(StorageNode.OperationMode.UNINSTALL.toString())) {
            return StorageNode.OperationMode.UNINSTALL;
        } else if (mode.equals(StorageNode.OperationMode.REMOVE_MAINTENANCE.toString())) {
            return StorageNode.OperationMode.REMOVE_MAINTENANCE;
        } else if (mode.equals(Server.OperationMode.DOWN.toString())) {
            return StorageNode.OperationMode.DOWN;
        } else {
            throw new IllegalArgumentException(mode + " is not a recognized operation mode");
        }
    }

    @Override
    public StepFailureStrategy getFailureStrategy() {
        return StepFailureStrategy.ABORT;
    }
}
