package org.rhq.enterprise.server.storage;

import java.net.InetAddress;

import javax.ejb.Asynchronous;

import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.operation.OperationHistory;

/**
 * @author John Sanda
 */
public interface StorageNodeOperationsHandlerLocal {

    @Asynchronous
    void handleOperationUpdateIfNecessary(OperationHistory operationHistory);

    void announceNewStorageNode(StorageNode newStorageNode, StorageNode clusterNode, PropertyList addresses);

    void performAddNodeMaintenance(InetAddress storageNodeAddress);
}
