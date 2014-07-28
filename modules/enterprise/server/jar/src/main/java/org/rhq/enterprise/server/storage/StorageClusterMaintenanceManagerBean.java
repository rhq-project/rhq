package org.rhq.enterprise.server.storage;

import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.storage.MaintenanceJob;

/**
 * @author John Sanda
 */
@Stateless
public class StorageClusterMaintenanceManagerBean implements StorageClusterMaintenanceManagerLocal {

    private final Log log = LogFactory.getLog(StorageClusterMaintenanceManagerBean.class);

    @Override
    public void scheduleMaintenance(MaintenanceJob job) {
        log.info("Adding job");
    }
}
