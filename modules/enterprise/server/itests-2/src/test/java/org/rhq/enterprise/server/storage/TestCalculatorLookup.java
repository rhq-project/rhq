package org.rhq.enterprise.server.storage;

import org.rhq.core.domain.storage.MaintenanceStep;
import org.rhq.enterprise.server.storage.maintenance.job.StepCalculator;

/**
 * @author John Sanda
 */
public class TestCalculatorLookup implements CalculatorLookup {

    @Override
    public StepCalculator lookup(MaintenanceStep.JobType jobType) {
        if (jobType == MaintenanceStep.JobType.DEPLOY) {
            return new TestDeployCalculator();
        }
        throw new UnsupportedOperationException(jobType + " is not supported");
//
//        try {
//            if (jobType == MaintenanceStep.JobType.DEPLOY) {
//                return (StepCalculator) new InitialContext().lookup("java:global/rhq/test-ejb/TestDeployCalculator");
//            }
//            throw new UnsupportedOperationException("There is no support yet for calculating steps for jobs of type " +
//                jobType);
//        } catch (NamingException e) {
//            throw new RuntimeException("Failed to look up step calculator", e);
//        }
    }
}
