package org.rhq.enterprise.server.storage.maintenance.job;

import static org.rhq.core.domain.storage.MaintenanceStep.JobType.FAILED_ANNOUNCE;

import java.util.HashSet;
import java.util.Set;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;

import org.rhq.core.domain.cloud.StorageClusterSettings;
import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.storage.MaintenanceStep;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.storage.StorageClusterSettingsManagerLocal;
import org.rhq.enterprise.server.storage.maintenance.StorageMaintenanceJob;
import org.rhq.enterprise.server.storage.maintenance.step.AnnounceStorageNode;
import org.rhq.enterprise.server.storage.maintenance.step.BootstrapNode;
import org.rhq.enterprise.server.storage.maintenance.step.RunRepair;
import org.rhq.enterprise.server.storage.maintenance.step.UpdateSchema;
import org.rhq.enterprise.server.storage.maintenance.step.UpdateStorageNodeStatus;
import org.rhq.server.metrics.SystemDAO;

/**
 * @author John Sanda
 */
public class DeployCalculator implements StepCalculator {

    private StorageClusterSettingsManagerLocal clusterSettingsManager;

    private SystemDAO systemDAO;

    private SubjectManagerLocal subjectManager;

    public void setClusterSettingsManager(StorageClusterSettingsManagerLocal clusterSettingsManager) {
        this.clusterSettingsManager = clusterSettingsManager;
    }

    public void setSystemDAO(SystemDAO systemDAO) {
        this.systemDAO = systemDAO;
    }

    public void setSubjectManager(SubjectManagerLocal subjectManager) {
        this.subjectManager = subjectManager;
    }

    @Override
    public StorageMaintenanceJob calculateSteps(StorageMaintenanceJob job) {
        Set<String> clusterSnapshot = job.getClusterSnapshot();
        PropertyMap parametersMap = job.getJobParameters();
        String newNodeAddress = parametersMap.getSimple("address").getStringValue();

        MaintenanceStep updateStatus = new MaintenanceStep()
                .setName(UpdateStorageNodeStatus.class.getName())
                .setDescription("Update operation mode of " + newNodeAddress + " to " +
                    StorageNode.OperationMode.ANNOUNCE)
                .setConfiguration(new Configuration.Builder()
                    .addSimple("targetAddress", newNodeAddress)
                    .addSimple("operationMode", StorageNode.OperationMode.ANNOUNCE.toString())
                .build());
        job.addStep(updateStatus);

        for (String address : job.getClusterSnapshot()) {
            MaintenanceStep step = new MaintenanceStep()
                .setName(AnnounceStorageNode.class.getName())
                .setDescription("Announce new node " + newNodeAddress + " to " + address)
                .setConfiguration(new Configuration.Builder()
                    .addSimple("targetAddress", address)
                    .openMap("parameters")
                        .addSimple("address", newNodeAddress)
                    .closeMap()
                    .build());
            job.addStep(step);
        }

        updateStatus = new MaintenanceStep()
            .setName(UpdateStorageNodeStatus.class.getName())
            .setDescription("Update operation mode of " + newNodeAddress + " to " +
                StorageNode.OperationMode.BOOTSTRAP)
            .setConfiguration(new Configuration.Builder()
                .addSimple("targetAddress", newNodeAddress)
                .addSimple("operationMode", StorageNode.OperationMode.BOOTSTRAP.toString())
                .build());
        job.addStep(updateStatus);

        StorageClusterSettings clusterSettings = clusterSettingsManager.getClusterSettings(
            subjectManager.getOverlord());

        MaintenanceStep bootstrap = new MaintenanceStep()
            .setName(BootstrapNode.class.getName())
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
        job.addStep(bootstrap);

        SchemaChanges schemaChanges = determineSchemaChanges(clusterSnapshot.size(), clusterSnapshot.size() + 1);
        applySchemaChanges(job, schemaChanges);

        updateStatus = new MaintenanceStep()
            .setName(UpdateStorageNodeStatus.class.getName())
            .setDescription("Update operation mode of " + newNodeAddress + " to " +
                StorageNode.OperationMode.ADD_MAINTENANCE)
            .setConfiguration(new Configuration.Builder()
                .addSimple("targetAddress", newNodeAddress)
                .addSimple("operationMode", StorageNode.OperationMode.ADD_MAINTENANCE.toString())
                .build());
        job.addStep(updateStatus);

        Set<String> addressess = new HashSet<String>(clusterSnapshot);
        addressess.add(newNodeAddress);

        addRepairSteps(job, SystemDAO.Keyspace.SYSTEM_AUTH, addressess);
        addRepairSteps(job, SystemDAO.Keyspace.RHQ, addressess);

        updateStatus = new MaintenanceStep()
            .setName(UpdateStorageNodeStatus.class.getName())
            .setDescription("Update operation mode of " + newNodeAddress + " to " +
                StorageNode.OperationMode.NORMAL)
            .setConfiguration(new Configuration.Builder()
                .addSimple("targetAddress", newNodeAddress)
                .addSimple("operationMode", StorageNode.OperationMode.NORMAL.toString())
                .build());
        job.addStep(updateStatus);

        return job;
    }

    protected void addRepairSteps(StorageMaintenanceJob job, SystemDAO.Keyspace keyspace, Set<String> addresses) {
        ResultSet resultSet = systemDAO.findTables(keyspace);
        for (Row row : resultSet) {
            String table = row.getString(0);
            for (String address : addresses) {
                job.addStep(new MaintenanceStep()
                    .setName(RunRepair.class.getName())
                    .setDescription("Run repair on " + keyspace + "." + table + " on " + address)
                    .setConfiguration(new Configuration.Builder()
                        .addSimple("targetAddress", address)
                        .openMap("parameters")
                        .addSimple("keyspace", keyspace)
                        .addSimple("table", table)
                        .closeMap()
                        .build()));
            }
        }
    }

    protected void applySchemaChanges(StorageMaintenanceJob job, SchemaChanges schemaChanges) {
        if (schemaChanges.replicationFactor != null) {
            Configuration configuration = new Configuration();
            configuration.put(new PropertySimple("replicationFactor", schemaChanges.replicationFactor));
            if (schemaChanges.gcGraceSeconds != null) {
                configuration.put(new PropertySimple("gcGraceSeconds", schemaChanges.gcGraceSeconds));
            }

            MaintenanceStep updateSchema = new MaintenanceStep()
                .setName(UpdateSchema.class.getName())
                .setDescription("Update Storage Cluster with new replication_factor of " +
                    schemaChanges.replicationFactor)
                .setConfiguration(configuration);
            job.addStep(updateSchema);
        }
    }

    protected SchemaChanges determineSchemaChanges(int oldClusterSize, int newClusterSize) {
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

    protected static class SchemaChanges {
        public Integer replicationFactor;
        public Integer gcGraceSeconds;
    }

    @Override
    public StorageMaintenanceJob calculateSteps(StorageMaintenanceJob originalJob, MaintenanceStep failedStep) {
        if (failedStep.getName().equals(AnnounceStorageNode.class.getName())) {
            createFailedAnnounceJob(originalJob, failedStep);
        }

        return null;
    }

    private StorageMaintenanceJob createFailedAnnounceJob(StorageMaintenanceJob originalJob,
        MaintenanceStep failedStep) {
        String address = failedStep.getConfiguration().getSimpleValue("targetAddress");
        StorageMaintenanceJob newJob = new StorageMaintenanceJob(FAILED_ANNOUNCE, FAILED_ANNOUNCE + " " + address,
            new Configuration.Builder().addSimple("address", address).build());

        newJob.addStep(failedStep);
        for (MaintenanceStep step : originalJob) {
            if (step.getName().equals(RunRepair.class.getName()) &&
                step.getConfiguration().getSimpleValue("targetAddress").equals(address)) {
                newJob.addStep(step);
            }
        }

        return newJob;
    }

}
