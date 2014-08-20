package org.rhq.enterprise.server.storage.maintenance.step;

import static org.rhq.core.domain.storage.MaintenanceStep.JobType.FAILED_ANNOUNCE;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.enterprise.server.storage.maintenance.JobProperties;
import org.rhq.enterprise.server.storage.maintenance.StepFailureStrategy;
import org.rhq.enterprise.server.storage.maintenance.StorageMaintenanceJob;

/**
 * @author John Sanda
 */
public class AnnounceStorageNode extends ResourceOperationStepRunner {

    private static final Log log = LogFactory.getLog(AnnounceStorageNode.class);

    public AnnounceStorageNode() {
        super("announce");
    }

    @Override
    public StepFailureStrategy getFailureStrategy() {
        Configuration configuration = step.getConfiguration();
        PropertySimple property = configuration.getSimple(JobProperties.FAILURE_STRATEGY);
        if (property != null) {
            return StepFailureStrategy.fromString(property.getStringValue());
        }

        // If we are a single node cluster we have to abort since the new node will not be
        // able to gossip with the existing node. But if we are running multiple nodes, we
        // can since the new node will be able to bootstrap from them.
        if (clusterSnapshot.size() == 1) {
            return StepFailureStrategy.ABORT;
        }
        return StepFailureStrategy.CONTINUE;
    }

    @Override
    public StorageMaintenanceJob createNewJobForFailedStep() {
        Configuration newJobConfiguration = new Configuration.Builder()
            .addSimple(JobProperties.TARGET, getTarget())
            .addSimple("newNodeAddress", step.getConfiguration().getMap(JobProperties.PARAMETERS)
                .getSimple("address"))
            .build();

        return new StorageMaintenanceJob(FAILED_ANNOUNCE, FAILED_ANNOUNCE + " " + getTarget(), newJobConfiguration);
    }
}
