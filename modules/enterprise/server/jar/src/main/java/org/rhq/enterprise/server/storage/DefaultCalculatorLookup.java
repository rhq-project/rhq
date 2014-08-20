package org.rhq.enterprise.server.storage;

import org.rhq.core.domain.storage.MaintenanceStep;
import org.rhq.enterprise.server.storage.maintenance.MaintenanceJobFactory;
import org.rhq.enterprise.server.storage.maintenance.job.DeployCalculator;
import org.rhq.enterprise.server.storage.maintenance.job.FailedRepairCalculator;
import org.rhq.enterprise.server.storage.maintenance.job.UndeployCalculator;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author John Sanda
 */
public class DefaultCalculatorLookup implements CalculatorLookup {

    @Override
    public MaintenanceJobFactory lookup(MaintenanceStep.JobType jobType) {
        if (jobType == MaintenanceStep.JobType.DEPLOY || jobType == MaintenanceStep.JobType.FAILED_ANNOUNCE) {
            DeployCalculator calculator = new DeployCalculator();
            calculator.setClusterSettingsManager(LookupUtil.getStorageClusterSettingsManagerLocal());
            calculator.setSubjectManager(LookupUtil.getSubjectManager());
            calculator.setSystemDAO(LookupUtil.getStorageClientManager().getSystemDAO());

            return calculator;
        } else if (jobType == MaintenanceStep.JobType.UNDEPLOY) {
            return new UndeployCalculator();
        } else if (jobType == MaintenanceStep.JobType.FAILED_REPAIR) {
            return new FailedRepairCalculator();
        }
        throw new UnsupportedOperationException(jobType + " is not yet supported");
    }
}
