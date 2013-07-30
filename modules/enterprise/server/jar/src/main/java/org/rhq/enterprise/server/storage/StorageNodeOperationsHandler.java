package org.rhq.enterprise.server.storage;

import javax.ejb.Asynchronous;

import org.rhq.core.domain.operation.GroupOperationHistory;
import org.rhq.core.domain.operation.OperationHistory;

/**
 * @author John Sanda
 */
public interface StorageNodeOperationsHandler {
    @Asynchronous
    void handleOperationUpdateIfNecessary(OperationHistory operationHistory);

    @Asynchronous
    void handleGroupOperationUpdateIfNecessary(GroupOperationHistory operationHistory);
}
