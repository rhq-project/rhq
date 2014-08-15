package org.rhq.enterprise.server.storage.maintenance.step;

import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.cloud.StorageNodeManagerLocal;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.storage.StorageClientManager;

/**
 * @author John Sanda
 */
public abstract class BaseStepRunner implements MaintenanceStepRunner {

    protected StorageNodeManagerLocal storageNodeManager;

    protected OperationManagerLocal operationManager;

    protected SubjectManagerLocal subjectManager;

    protected StorageClientManager storageClientManager;

    @Override
    public void setStorageNodeManager(StorageNodeManagerLocal storageNodeManager) {
        this.storageNodeManager = storageNodeManager;
    }

    @Override
    public void setOperationManager(OperationManagerLocal operationManager) {
        this.operationManager = operationManager;
    }

    @Override
    public void setStorageClientManager(StorageClientManager storageClientManager) {
        this.storageClientManager = storageClientManager;
    }

    @Override
    public void setSubjectManager(SubjectManagerLocal subjectManager) {
        this.subjectManager = subjectManager;
    }

}
