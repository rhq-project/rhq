package org.rhq.enterprise.server.storage.maintenance.job;

import java.util.List;

import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.storage.MaintenanceStep;
import org.rhq.enterprise.server.storage.maintenance.StorageMaintenanceJob;

/**
 * @author John Sanda
 */
public interface StepCalculator {

    StorageMaintenanceJob calculateSteps(int jobNumber, List<StorageNode> cluster);

    StorageMaintenanceJob calculateSteps(StorageMaintenanceJob originalJob, MaintenanceStep failedStep);

}
