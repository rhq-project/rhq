package org.rhq.enterprise.server.storage;

import org.rhq.core.domain.storage.MaintenanceStep;
import org.rhq.enterprise.server.storage.maintenance.MaintenanceJobFactory;
import org.rhq.enterprise.server.storage.maintenance.job.AnnounceNewNode;
import org.rhq.enterprise.server.storage.maintenance.job.DeployNode;
import org.rhq.enterprise.server.storage.maintenance.job.FailedRepair;
import org.rhq.enterprise.server.storage.maintenance.job.UndeployNode;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author John Sanda
 */
public class DefaultCalculatorLookup implements CalculatorLookup {

    @Override
    public MaintenanceJobFactory lookup(MaintenanceStep.JobType jobType) {
        if (jobType == MaintenanceStep.JobType.DEPLOY || jobType == MaintenanceStep.JobType.FAILED_ANNOUNCE) {
            DeployNode calculator = new DeployNode();
            calculator.setClusterSettingsManager(LookupUtil.getStorageClusterSettingsManagerLocal());
            calculator.setSubjectManager(LookupUtil.getSubjectManager());
            calculator.setSystemDAO(LookupUtil.getStorageClientManager().getSystemDAO());

            return calculator;
        } else if (jobType == MaintenanceStep.JobType.UNDEPLOY) {
            return new UndeployNode();
        } else if (jobType == MaintenanceStep.JobType.FAILED_REPAIR) {
            return new FailedRepair();
        } else if (jobType == MaintenanceStep.JobType.FAILED_ANNOUNCE) {
            return new AnnounceNewNode();
        }
        throw new UnsupportedOperationException(jobType + " is not yet supported");
    }
}
