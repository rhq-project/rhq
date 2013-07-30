package org.rhq.enterprise.server.storage;

import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.operation.GroupOperationHistory;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.operation.OperationHistory;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.cloud.StorageNodeManagerLocal;

/**
 * @author John Sanda
 */
@Stateless
public class StorageNodeOperationsHandlerBean implements StorageNodeOperationsHandler {

    private final Log log  = LogFactory.getLog(StorageNodeOperationsHandlerBean.class);

    private static final String STORAGE_NODE_TYPE_NAME = "RHQ Storage Node";
    private static final String STORAGE_NODE_PLUGIN_NAME = "RHQStorage";

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private StorageNodeManagerLocal storageNodeManager;

    @Override
    @Asynchronous
    public void handleOperationUpdateIfNecessary(OperationHistory operationHistory) {
//        if (isStorageNodeOperation(operationHistory.getOperationDefinition())) {
//            if (operationHistory.getOperationDefinition().getName().equals("prepareForBootstrap")) {
//                ResourceOperationHistory resourceOperationHistory = entityManager.find(ResourceOperationHistory.class,
//                    operationHistory.getId());
//                if (resourceOperationHistory.getStatus() == OperationRequestStatus.SUCCESS) {
//
//                }
//                StorageNode storageNode = findStorageNode(resourceOperationHistory.getResource());
//                storageNode.setOperationMode(StorageNode.OperationMode.NORMAL);
//            }
//        }
    }

    private StorageNode findStorageNode(Resource resource) {
        for (StorageNode storageNode : storageNodeManager.getStorageNodes()) {
            if (storageNode.getResource().getId() == resource.getId()) {
                return storageNode;
            }
        }
        return null;
    }

    @Override
    @Asynchronous
    public void handleGroupOperationUpdateIfNecessary(GroupOperationHistory groupOperationHistory) {
        if (isStorageNodeOperation(groupOperationHistory.getOperationDefinition())) {
            if (groupOperationHistory.getOperationDefinition().getName().equals("updateKnownNodes")) {
                if (groupOperationHistory.getStatus() == OperationRequestStatus.SUCCESS) {
                    log.info("New storage has been successfully announced to the storage node cluster.");
                    storageNodeManager.prepareNewNodesForBootstrap();
                } else if (groupOperationHistory.getStatus() == OperationRequestStatus.FAILURE) {
                    log.warn("Failed to announce new storage node to the cluster. It cannot join the cluster until " +
                        "it has been announced to existing cluster nodes.");
                } else if (groupOperationHistory.getStatus() == OperationRequestStatus.CANCELED) {
                    log.warn("New storage node has not been announced to the cluster. The group operation " +
                        groupOperationHistory.getOperationDefinition().getName() + " has been canceled. The new node " +
                        "cannot join the cluster until it has been announced to existing cluster nodes.");
                }
            }
        }
    }

    private boolean isStorageNodeOperation(OperationDefinition operationDefinition) {
        ResourceType resourceType = operationDefinition.getResourceType();
        return resourceType.getName().equals(STORAGE_NODE_TYPE_NAME) &&
            resourceType.getPlugin().equals(STORAGE_NODE_PLUGIN_NAME);
    }

}
