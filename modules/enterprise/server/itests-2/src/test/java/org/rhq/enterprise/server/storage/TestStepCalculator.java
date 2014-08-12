package org.rhq.enterprise.server.storage;

import java.util.List;

import javax.persistence.EntityManager;

import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.storage.MaintenanceStep;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.storage.maintenance.StorageMaintenanceJob;
import org.rhq.enterprise.server.storage.maintenance.job.StepCalculator;

/**
 * @author John Sanda
 */
public class TestStepCalculator implements StepCalculator {

    protected SubjectManagerLocal subjectManager;

    protected StorageClusterSettingsManagerLocal clusterSettingsManager;

    protected EntityManager entityManager;

    @Override
    public void setSubjectManager(SubjectManagerLocal subjectManager) {
        this.subjectManager = subjectManager;
    }

    @Override
    public void setStorageClusterSettingsManager(StorageClusterSettingsManagerLocal clusterSettingsManager) {
        this.clusterSettingsManager = clusterSettingsManager;
    }

    @Override
    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public StorageMaintenanceJob calculateSteps(StorageMaintenanceJob job, List<StorageNode> cluster) {
        return null;
    }

    @Override
    public StorageMaintenanceJob calculateSteps(StorageMaintenanceJob originalJob, MaintenanceStep failedStep) {
        return null;
    }
}
