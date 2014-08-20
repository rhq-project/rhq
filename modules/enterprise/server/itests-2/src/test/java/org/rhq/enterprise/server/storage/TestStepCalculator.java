package org.rhq.enterprise.server.storage;

import org.rhq.core.domain.storage.MaintenanceStep;
import org.rhq.enterprise.server.storage.maintenance.StorageMaintenanceJob;
import org.rhq.enterprise.server.storage.maintenance.job.StepCalculator;

/**
 * @author John Sanda
 */
public class TestStepCalculator implements StepCalculator {

    @Override
    public StorageMaintenanceJob calculateSteps(StorageMaintenanceJob job) {
        return null;
    }

    @Override
    public void updateSteps(StorageMaintenanceJob job, MaintenanceStep failedStep) {
    }

}
