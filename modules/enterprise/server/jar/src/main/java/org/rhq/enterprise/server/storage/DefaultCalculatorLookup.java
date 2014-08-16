package org.rhq.enterprise.server.storage;

import org.rhq.core.domain.storage.MaintenanceStep;
import org.rhq.enterprise.server.storage.maintenance.job.DeployCalculator;
import org.rhq.enterprise.server.storage.maintenance.job.StepCalculator;
import org.rhq.enterprise.server.storage.maintenance.job.UndeployCalculator;

/**
 * @author John Sanda
 */
public class DefaultCalculatorLookup implements CalculatorLookup {

    @Override
    public StepCalculator lookup(MaintenanceStep.JobType jobType) {
        if (jobType == MaintenanceStep.JobType.DEPLOY) {
//                return (StepCalculator) new InitialContext().lookup(
//                    "java:global/rhq/rhq-server/" + DeployCalculator.class.getSimpleName());
            return new DeployCalculator();
        } else if (jobType == MaintenanceStep.JobType.UNDEPLOY) {
            return new UndeployCalculator();
        }
        throw new UnsupportedOperationException(jobType + " is not yet supported");

//        try {
//            if (jobType == MaintenanceStep.JobType.DEPLOY) {
////                return (StepCalculator) new InitialContext().lookup(
////                    "java:global/rhq/rhq-server/" + DeployCalculator.class.getSimpleName());
//                return new DeployCalculator();
//            }
//            throw new UnsupportedOperationException("There is no support yet for calculating steps for jobs of type " +
//                jobType);
//        } catch (NamingException e) {
//            throw new RuntimeException("Failed to look up step calculator", e);
//        }
    }
}
