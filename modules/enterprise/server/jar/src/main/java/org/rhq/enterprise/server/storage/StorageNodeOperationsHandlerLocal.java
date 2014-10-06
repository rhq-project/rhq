package org.rhq.enterprise.server.storage;

import java.util.List;

import javax.ejb.Asynchronous;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.operation.OperationHistory;
import org.rhq.core.domain.operation.ResourceOperationHistory;

/**
 * @author John Sanda
 */
public interface StorageNodeOperationsHandlerLocal {

    @Asynchronous
    void handleOperationUpdateIfNecessary(OperationHistory operationHistory);

    void handleAnnounce(ResourceOperationHistory operationHistory);

    void handleUnannounce(ResourceOperationHistory operationHistory);

    void handlePrepareForBootstrap(ResourceOperationHistory operationHistory);

    void handleAddNodeMaintenance(ResourceOperationHistory operationHistory);

    void handleRemoveNodeMaintenance(ResourceOperationHistory operationHistory);

    void handleDecommission(ResourceOperationHistory operationHistory);

    void handleUninstall(ResourceOperationHistory operationHistory);

    void announceStorageNode(Subject subject, StorageNode storageNode);

    void unannounceStorageNode(Subject subject, StorageNode storageNode);

    void bootstrapStorageNode(Subject subject, StorageNode storageNode);

    void performAddNodeMaintenance(Subject subject, StorageNode storageNode);

    void performAddMaintenance(Subject subject, StorageNode storageNode);

    void uninstall(Subject subject, StorageNode storageNode);

    void detachFromResource(StorageNode storageNode);

    void decommissionStorageNode(Subject subject, StorageNode storageNode);

    void performRemoveNodeMaintenance(Subject subject, StorageNode storageNode);

    void performRemoveMaintenance(Subject subject, StorageNode storageNode);

    void runRepair(Subject subject);

    void handleRepair(ResourceOperationHistory operationHistory);

    void logError(String address, String error, Exception e);

    StorageNode setMode(StorageNode storageNode, StorageNode.OperationMode newMode);

    List<StorageNode> getStorageNodesByMode(StorageNode.OperationMode mode);

    /**
     * sets all NORMAL StorageNodes to setMaintenancePending(true).
     *
     * @param storageNode
     * @param mode
     * @return the updated StorageNodes
     */
    List<StorageNode> setMaintenancePending();

    /**
     * Checks the NORMAL StorageNodes to see if they need a schema update, and if so, reflect that fact in
     * the storage node being manipulated.
     *
     * @param storageNode
     * @param mode
     * @return
     */
    StorageNode setMaintenancePendingDecommissionStorageNode(StorageNode storageNode);

    /**
     * sets all NORMAL StorageNodes to setMaintenancePending(true) as well as the supplied StorageNode. And
     * returns them all in one List.
     *
     * @param storageNode
     * @param mode
     * @return the updated StorageNodes
     */
    List<StorageNode> setMaintenancePendingPerformAddMaintenance(StorageNode storageNode);

    void finishUninstall(Subject subject, StorageNode storageNode);
}
