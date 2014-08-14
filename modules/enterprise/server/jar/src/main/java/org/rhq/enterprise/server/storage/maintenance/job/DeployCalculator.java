package org.rhq.enterprise.server.storage.maintenance.job;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import org.rhq.core.domain.cloud.StorageClusterSettings;
import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.storage.MaintenanceStep;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.storage.StorageClusterSettingsManagerLocal;
import org.rhq.enterprise.server.storage.maintenance.StorageMaintenanceJob;
import org.rhq.enterprise.server.storage.maintenance.step.AddMaintenance;
import org.rhq.enterprise.server.storage.maintenance.step.AnnounceStorageNode;
import org.rhq.enterprise.server.storage.maintenance.step.BootstrapNode;
import org.rhq.enterprise.server.storage.maintenance.step.UpdateSchema;
import org.rhq.enterprise.server.storage.maintenance.step.UpdateStorageNodeStatus;

/**
 * @author John Sanda
 */
public class DeployCalculator implements StepCalculator {

    private EntityManager entityManager;

    private StorageClusterSettingsManagerLocal clusterSettingsManager;

    private SubjectManagerLocal subjectManager;

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
        Set<String> clusterSnapshot = job.getClusterSnapshot();
        if (!clusterSnapshot.isEmpty()) {
            Set<String> newClusterSnapshot = createSnapshot(cluster);
            if (clusterSnapshot.equals(newClusterSnapshot)) {
                return job;
            }
        }

        int stepNumber = 1;
        job.setClusterSnapshot(cluster);
        PropertyMap parametersMap = job.getJobParameters();
        String newNodeAddress = parametersMap.getSimple("address").getStringValue();

        MaintenanceStep updateStatus = new MaintenanceStep()
                .setJobNumber(job.getJobNumber())
                .setJobType(job.getJobType())
                .setName(UpdateStorageNodeStatus.class.getName())
                .setStepNumber(stepNumber++)
                .setDescription("Update operation mode of " + newNodeAddress + " to " +
                    StorageNode.OperationMode.ANNOUNCE)
                .setConfiguration(new Configuration.Builder()
                    .addSimple("targetAddress", newNodeAddress)
                    .addSimple("operationMode", StorageNode.OperationMode.ANNOUNCE.toString())
                .build());
        entityManager.persist(updateStatus);
        job.addStep(updateStatus);

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

            entityManager.persist(step);
            job.addStep(step);
        }

        updateStatus = new MaintenanceStep()
            .setJobNumber(job.getJobNumber())
            .setJobType(job.getJobType())
            .setName(UpdateStorageNodeStatus.class.getName())
            .setStepNumber(stepNumber++)
            .setDescription("Update operation mode of " + newNodeAddress + " to " +
                StorageNode.OperationMode.BOOTSTRAP)
            .setConfiguration(new Configuration.Builder()
                .addSimple("targetAddress", newNodeAddress)
                .addSimple("operationMode", StorageNode.OperationMode.BOOTSTRAP.toString())
                .build());
        entityManager.persist(updateStatus);
        job.addStep(updateStatus);

        StorageClusterSettings clusterSettings = clusterSettingsManager.getClusterSettings(
            subjectManager.getOverlord());

        MaintenanceStep bootstrap = new MaintenanceStep()
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

        entityManager.persist(bootstrap);
        job.addStep(bootstrap);

        SchemaChanges schemaChanges = determineSchemaChanges(cluster.size(), cluster.size() + 1);
        if (schemaChanges.replicationFactor != null) {
            Configuration configuration = new Configuration();
            configuration.put(new PropertySimple("replicationFactor", schemaChanges.replicationFactor));
            if (schemaChanges.gcGraceSeconds != null) {
                configuration.put(new PropertySimple("gcGraceSeconds", schemaChanges.gcGraceSeconds));
            }

            MaintenanceStep updateSchema = new MaintenanceStep()
                .setJobNumber(job.getJobNumber())
                .setStepNumber(stepNumber++)
                .setJobType(job.getJobType())
                .setName(UpdateSchema.class.getName())
                .setDescription("Update Storage Cluster with new replication_factor of " +
                    schemaChanges.replicationFactor)
                .setConfiguration(configuration);
            entityManager.persist(updateSchema);
            job.addStep(updateSchema);
        }

        updateStatus = new MaintenanceStep()
            .setJobNumber(job.getJobNumber())
            .setJobType(job.getJobType())
            .setName(UpdateStorageNodeStatus.class.getName())
            .setStepNumber(stepNumber++)
            .setDescription("Update operation mode of " + newNodeAddress + " to " +
                StorageNode.OperationMode.ADD_MAINTENANCE)
            .setConfiguration(new Configuration.Builder()
                .addSimple("targetAddress", newNodeAddress)
                .addSimple("operationMode", StorageNode.OperationMode.ADD_MAINTENANCE.toString())
                .build());
        entityManager.persist(updateStatus);
        job.addStep(updateStatus);

        MaintenanceStep addMaintenance;
        for (String address : clusterSnapshot) {
            addMaintenance = new MaintenanceStep()
                .setJobNumber(job.getJobNumber())
                .setStepNumber(stepNumber++)
                .setJobType(job.getJobType())
                .setName(AddMaintenance.class.getName())
                .setDescription("Run cluster maintenance on " + address)
                .setConfiguration(new Configuration.Builder()
                    .addSimple("targetAddress", address)
                    .addSimple("runRepair", schemaChanges.replicationFactor != null)
                    .addSimple("newNodeAddress", newNodeAddress)
                    .addSimple("updateSeedsList", true)
                    .openList("seedsList", "seedsList")
                    .addSimples(job.getClusterSnapshot().toArray(new String[job.getClusterSnapshot().size()]))
                    .closeList()
                    .build());
            entityManager.persist(addMaintenance);
            job.addStep(addMaintenance);
        }

        updateStatus = new MaintenanceStep()
            .setJobNumber(job.getJobNumber())
            .setJobType(job.getJobType())
            .setName(UpdateStorageNodeStatus.class.getName())
            .setStepNumber(stepNumber++)
            .setDescription("Update operation mode of " + newNodeAddress + " to " +
                StorageNode.OperationMode.NORMAL)
            .setConfiguration(new Configuration.Builder()
                .addSimple("targetAddress", newNodeAddress)
                .addSimple("operationMode", StorageNode.OperationMode.NORMAL.toString())
                .build());
        entityManager.persist(updateStatus);
        job.addStep(updateStatus);

        return job;
    }

    private Set<String> createSnapshot(List<StorageNode> cluster) {
        Set<String> snapshot = new HashSet<String>();
        for (StorageNode node : cluster) {
            snapshot.add(node.getAddress());
        }
        return snapshot;
    }

    private PropertyList createPropertyListOfAddresses(String propertyName, List<StorageNode> nodes) {
        PropertyList list = new PropertyList(propertyName);
        for (StorageNode storageNode : nodes) {
            list.add(new PropertySimple("address", storageNode.getAddress()));
        }
        return list;
    }

    @Override
    public StorageMaintenanceJob calculateSteps(StorageMaintenanceJob originalJob, MaintenanceStep failedStep) {
        return null;
    }

    private SchemaChanges determineSchemaChanges(int oldClusterSize, int newClusterSize) {
        SchemaChanges changes = new SchemaChanges();

        if (oldClusterSize == 0) {
            throw new IllegalStateException("previousClusterSize cannot be 0");
        }
        if (newClusterSize == 0) {
            throw new IllegalStateException("newClusterSize cannot be 0");
        }
        if (Math.abs(newClusterSize - oldClusterSize) != 1) {
            throw new IllegalStateException("The absolute difference between previousClusterSize["
                + oldClusterSize + "] and newClusterSize[" + newClusterSize + "] must be 1");
        }

        if (newClusterSize == 1) {
            changes.replicationFactor = 1;
            changes.gcGraceSeconds = 0;
        } else if (newClusterSize >= 5) {
            // no changes necessary
        } else if (oldClusterSize > 4) {
            // no changes necessary
        } else if (oldClusterSize == 4 && newClusterSize == 3) {
            changes.replicationFactor = 2;
        } else if (oldClusterSize == 3 && newClusterSize == 2) {
            // no changes necessary
        } else if (oldClusterSize == 1 && newClusterSize == 2) {
            changes.replicationFactor = 2;
            changes.gcGraceSeconds = 691200;   // 8 days
        } else if (oldClusterSize == 2 && newClusterSize == 3) {
            // no changes necessary
        } else if (oldClusterSize == 3 && newClusterSize == 4) {
            changes.replicationFactor = 3;
        } else {
            throw new IllegalStateException("previousClusterSize[" + oldClusterSize + "] and newClusterSize["
                + newClusterSize + "] is not supported");
        }

        return changes;
    }

    private static class SchemaChanges {
        public Integer replicationFactor;
        public Integer gcGraceSeconds;
    }

}
