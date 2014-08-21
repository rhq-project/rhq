package org.rhq.enterprise.server.storage.maintenance.job;

import com.google.common.collect.ImmutableSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.storage.MaintenanceStep;
import org.rhq.enterprise.server.storage.maintenance.JobProperties;
import org.rhq.enterprise.server.storage.maintenance.StepFailureStrategy;
import org.rhq.enterprise.server.storage.maintenance.StorageMaintenanceJob;
import org.rhq.enterprise.server.storage.maintenance.step.AnnounceStorageNode;
import org.rhq.server.metrics.SystemDAO;

/**
 * @author John Sanda
 */
public class AnnounceNewNode extends DeployNode {

    private static final Log log = LogFactory.getLog(AnnounceNewNode.class);

    public StorageMaintenanceJob calculateSteps(StorageMaintenanceJob job) {
        String targetAddress = job.getTarget();
        String newNodeAddress = job.getConfiguration().getSimpleValue("newNodeAddress");

        job.addStep(new MaintenanceStep()
            .setName(AnnounceStorageNode.class.getName())
            .setDescription("Announce " + newNodeAddress + " to " + targetAddress)
            .setConfiguration(new Configuration.Builder()
                .addSimple(JobProperties.TARGET, targetAddress)
                .addSimple(JobProperties.FAILURE_STRATEGY, StepFailureStrategy.ABORT.toString())
                .openMap(JobProperties.PARAMETERS)
                    .addSimple("address", newNodeAddress)
                .closeMap()
                .build()));

        PropertySimple replicationFactorChanged = job.getConfiguration().getSimple(
            JobProperties.REPLICATION_FACTOR_CHANGED);
        if (replicationFactorChanged == null) {
            log.warn(job + " does not have expected property [" +
                JobProperties.REPLICATION_FACTOR_CHANGED + "]. No repair steps will be added to the job.");
        } else if (replicationFactorChanged.getBooleanValue()) {
            addRepairSteps(job, SystemDAO.Keyspace.SYSTEM_AUTH, ImmutableSet.of(targetAddress));
            addRepairSteps(job, SystemDAO.Keyspace.RHQ, ImmutableSet.of(targetAddress));
        }

        return job;
    }

    @Override
    public void updateSteps(StorageMaintenanceJob job, MaintenanceStep failedStep) {
    }
}
