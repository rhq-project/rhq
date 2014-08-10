package org.rhq.enterprise.server.storage.maintenance.job;

import java.util.List;
import java.util.Set;

import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
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
            Configuration configuration = new Configuration();
            configuration.put(new PropertySimple("targetAddress", address));
            PropertyMap params = new PropertyMap("parameters");
            params.put(createPropertyListOfAddresses("addresses", job.getClusterSnapshot()));
            configuration.put(params);

            MaintenanceStep step = new MaintenanceStep()
                .setJobNumber(job.getJobNumber())
                .setJobType(job.getJobType())
                .setName(AnnounceStorageNode.class.getName())
                .setStepNumber(stepNumber++)
                .setDescription("Announce new node " + newNodeAddress + " to " + address)
                .setConfiguration(configuration);
//                .setConfiguration(new Configuration.Builder()
//                    .addSimple("targetAddress", address)
//                    .openMap("parameters")
//                    .addSimple("address", newNodeAddress)
//                    .closeMap()
//                    .build());
            entityManager.persist(step.getConfiguration());
            entityManager.persist(step);
            job.addStep(step);
        }
        return job;
    }

    private PropertyList createPropertyListOfAddresses(String propertyName, Set<String> addresses) {
        PropertyList list = new PropertyList(propertyName);
        for (String address : addresses) {
            list.add(new PropertySimple("address", address));
        }
        return list;
    }
}
