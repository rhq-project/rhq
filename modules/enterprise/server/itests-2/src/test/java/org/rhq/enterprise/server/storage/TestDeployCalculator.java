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
public class TestDeployCalculator implements StepCalculator {

    private EntityManager entityManager;

    @Override
    public void setSubjectManager(SubjectManagerLocal subjectManager) {

    }

    @Override
    public void setStorageClusterSettingsManager(StorageClusterSettingsManagerLocal clusterSettingsManager) {

    }

    @Override
    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public StorageMaintenanceJob calculateSteps(StorageMaintenanceJob job, List<StorageNode> cluster) {
        MaintenanceStep step = new MaintenanceStep()
            .setJobNumber(job.getJobNumber())
            .setJobType(job.getJobType())
            .setName(FakeStepRunner.class.getName())
            .setDescription("Fake step")
            .setStepNumber(1);
        entityManager.persist(step);
        job.addStep(step);

        return job;
    }

    @Override
    public StorageMaintenanceJob calculateSteps(StorageMaintenanceJob originalJob, MaintenanceStep failedStep) {
        return null;
    }
}
