package org.rhq.enterprise.server.storage.maintenance.step;

import java.util.Set;

import org.rhq.core.domain.storage.MaintenanceStep;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.cloud.StorageNodeManagerLocal;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.storage.StorageClientManager;

/**
 * @author John Sanda
 */
public abstract class BaseStepRunner implements MaintenanceStepRunner {

    protected Set<String> clusterSnapshot;

    protected MaintenanceStep step;

    protected StorageNodeManagerLocal storageNodeManager;

    protected OperationManagerLocal operationManager;

    protected SubjectManagerLocal subjectManager;

    protected StorageClientManager storageClientManager;

    @Override
    public void setClusterSnapshot(Set<String> clusterSnapshot) {
        this.clusterSnapshot = clusterSnapshot;
    }

    @Override
    public void setStep(MaintenanceStep step) {
        this.step = step;
    }

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
