package org.rhq.enterprise.server.storage;

import javax.ejb.Local;

import org.rhq.core.domain.storage.MaintenanceJob;
import org.rhq.core.domain.storage.MaintenanceStep;
import org.rhq.core.domain.storage.StorageMaintenanceJob;

/**
 * @author John Sanda
 */
@Local
public interface StorageClusterMaintenanceManagerLocal {

    void scheduleMaintenance(StorageMaintenanceJob job);

    MaintenanceJob updateQueue(MaintenanceJob job);

    MaintenanceJob updateSteps(MaintenanceJob job);

    StorageMaintenanceJob getNextJob();

    void deleteStep(MaintenanceStep step);

    void execute();

}
