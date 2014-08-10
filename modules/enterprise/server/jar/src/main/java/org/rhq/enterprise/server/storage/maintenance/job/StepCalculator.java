package org.rhq.enterprise.server.storage.maintenance.job;

import java.util.List;

import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.enterprise.server.storage.maintenance.StorageMaintenanceJob;

/**
 * @author John Sanda
 */
public interface StepCalculator {

    StorageMaintenanceJob calculateSteps(StorageMaintenanceJob job, List<StorageNode> cluster);

}
