package org.rhq.enterprise.server.storage;

import org.rhq.core.domain.storage.MaintenanceStep;
import org.rhq.enterprise.server.storage.maintenance.job.MaintenanceJobFactory;

/**
 * @author John Sanda
 */
public interface CalculatorLookup {

    MaintenanceJobFactory lookup(MaintenanceStep.JobType jobType);

}
