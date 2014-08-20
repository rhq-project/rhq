package org.rhq.enterprise.server.storage;

import org.rhq.core.domain.storage.MaintenanceStep;
import org.rhq.enterprise.server.storage.maintenance.MaintenanceJobFactory;
import org.rhq.enterprise.server.storage.maintenance.StorageMaintenanceJob;

/**
 * @author John Sanda
 */
public class TestStepCalculator implements MaintenanceJobFactory {

    @Override
    public StorageMaintenanceJob calculateSteps(StorageMaintenanceJob job) {
        return null;
    }

    @Override
    public void updateSteps(StorageMaintenanceJob job, MaintenanceStep failedStep) {
    }

}
