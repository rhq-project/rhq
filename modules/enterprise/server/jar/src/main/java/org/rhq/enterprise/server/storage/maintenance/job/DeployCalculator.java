package org.rhq.enterprise.server.storage.maintenance.job;

import java.util.List;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.rhq.core.domain.cloud.StorageClusterSettings;
import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.storage.MaintenanceStep;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.storage.StorageClusterSettingsManagerLocal;
import org.rhq.enterprise.server.storage.maintenance.StorageMaintenanceJob;
import org.rhq.enterprise.server.storage.maintenance.step.AnnounceStorageNode;
import org.rhq.enterprise.server.storage.maintenance.step.BootstrapNode;

/**
 * @author John Sanda
 */
@Singleton
@LocalBean
public class DeployCalculator implements StepCalculator {

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private StorageClusterSettingsManagerLocal clusterSesttingsManager;

    @EJB
    private SubjectManagerLocal subjectManager;

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
                    .addSimple("targetAddress", address)
                    .openMap("parameters")
                    .addSimple("address", newNodeAddress)
                    .closeMap()
                    .build());
//            entityManager.persist(step.getConfiguration());
            entityManager.persist(step);
            job.addStep(step);
        }

        StorageClusterSettings clusterSettings = clusterSesttingsManager.getClusterSettings(
            subjectManager.getOverlord());

        MaintenanceStep bootstrapStep = new MaintenanceStep()
            .setJobNumber(job.getJobNumber())
            .setJobType(job.getJobType())
            .setName(BootstrapNode.class.getName())
            .setStepNumber(stepNumber++)
            .setDescription("Bootstrap new node " + newNodeAddress)
            .setConfiguration(new Configuration.Builder()
                .addSimple("targetAddress", newNodeAddress)
                .openMap("parameters")
                .addSimple("cqlPort", clusterSettings.getCqlPort())
                .addSimple("gossipPort", clusterSettings.getGossipPort())
                .openList("addresses", "addresses")
                .addSimple(newNodeAddress)
                .addSimples(job.getClusterSnapshot().toArray(new String[job.getClusterSnapshot().size()]))
                .closeList()
                .closeMap()
                .build());

        entityManager.persist(bootstrapStep);
        job.addStep(bootstrapStep);

        return job;
    }

}
