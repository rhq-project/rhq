package org.rhq.enterprise.server.storage;

import org.rhq.core.domain.storage.MaintenanceStep;
import org.rhq.enterprise.server.storage.maintenance.job.StepCalculator;

/**
 * @author John Sanda
 */
public interface CalculatorLookup {

    StepCalculator lookup(MaintenanceStep.JobType jobType);

}
