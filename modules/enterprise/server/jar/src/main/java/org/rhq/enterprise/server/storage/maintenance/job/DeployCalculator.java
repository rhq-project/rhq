package org.rhq.enterprise.server.storage.maintenance.job;

import java.util.List;

import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.storage.MaintenanceStep;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.storage.maintenance.StorageMaintenanceJob;
import org.rhq.enterprise.server.storage.maintenance.step.AnnounceStorageNode;

/**
 * @author John Sanda
 */
@Singleton
@LocalBean
public class DeployCalculator implements StepCalculator {

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @Override
    public StorageMaintenanceJob calculateSteps(StorageMaintenanceJob job, List<StorageNode> cluster) {
        int stepNumber = 1;
        job.setClusterSnapshot(cluster);
        PropertyMap parametersMap = job.getJobParameters();
        String newNodeAddress = parametersMap.getSimple("address").getStringValue();
        for (String address : job.getClusterSnapshot()) {
            MaintenanceStep step = new MaintenanceStep()
                .setJobNumber(job.getJobNumber())
                .setJobType(job.getJobType())
                .setName(AnnounceStorageNode.class.getName())
                .setStepNumber(stepNumber++)
                .setDescription("Announce new node " + newNodeAddress + " to " + address)
                .setConfiguration(new Configuration.Builder()
                    .openMap("parameters")
                    .addSimple("address", address)
                    .closeMap()
                    .build());
            entityManager.persist(step.getConfiguration());
            entityManager.persist(step);
            job.addStep(step);
        }
        return job;
    }
}
