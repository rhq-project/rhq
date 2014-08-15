package org.rhq.enterprise.server.storage.maintenance.job;

import org.rhq.core.domain.storage.MaintenanceStep;
import org.rhq.enterprise.server.storage.maintenance.StorageMaintenanceJob;

/**
 * @author John Sanda
 */
public interface StepCalculator {

    StorageMaintenanceJob calculateSteps(StorageMaintenanceJob job);

    StorageMaintenanceJob calculateSteps(StorageMaintenanceJob originalJob, MaintenanceStep failedStep);

}
