package org.rhq.enterprise.server.storage;

import javax.ejb.Local;

import org.rhq.core.domain.storage.MaintenanceJob;

/**
 * @author John Sanda
 */
@Local
public interface StorageClusterMaintenanceManagerLocal {

    void scheduleMaintenance(MaintenanceJob job);

}
