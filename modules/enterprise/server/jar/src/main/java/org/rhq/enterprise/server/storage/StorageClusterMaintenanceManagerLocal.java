package org.rhq.enterprise.server.storage;

import javax.ejb.Local;

import org.rhq.core.domain.storage.MaintenanceJob;
import org.rhq.enterprise.server.storage.maintenance.StorageMaintenanceJob;

/**
 * @author John Sanda
 */
@Local
public interface StorageClusterMaintenanceManagerLocal {

    void scheduleMaintenance(StorageMaintenanceJob job);

    void loadQueue();

    StorageMaintenanceJob getNextJob();

    MaintenanceJob updateQueue(MaintenanceJob job);

    MaintenanceJob updateSteps(MaintenanceJob job);

    void deleteStep(int stepId);

    void execute();

}
