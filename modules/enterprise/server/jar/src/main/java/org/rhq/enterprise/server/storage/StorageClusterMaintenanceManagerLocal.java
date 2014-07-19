package org.rhq.enterprise.server.storage;

import javax.ejb.Local;

/**
 * @author John Sanda
 */
@Local
public interface StorageClusterMaintenanceManagerLocal {

    void addTask();

}
