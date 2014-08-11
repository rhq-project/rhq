package org.rhq.enterprise.server.storage;

import java.util.List;

import javax.ejb.Local;

import org.rhq.core.domain.storage.MaintenanceStep;
import org.rhq.enterprise.server.storage.maintenance.StorageMaintenanceJob;

/**
 * @author John Sanda
 */
@Local
public interface StorageClusterMaintenanceManagerLocal {

    void scheduleMaintenance(StorageMaintenanceJob job);

    void rescheduleJob(int jobNumber);

    List<StorageMaintenanceJob> loadQueue();

    void deleteStep(int stepId);

    void execute();

    MaintenanceStep reloadStep(int stepId);

}
