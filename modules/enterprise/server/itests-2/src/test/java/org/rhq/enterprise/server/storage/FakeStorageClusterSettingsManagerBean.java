package org.rhq.enterprise.server.storage;

import javax.ejb.Stateless;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.cloud.StorageClusterSettings;

/**
 * @author John Sanda
 */
@Stateless
public class FakeStorageClusterSettingsManagerBean implements StorageClusterSettingsManagerLocal {

    @Override
    public StorageClusterSettings getClusterSettings(Subject subject) {
        StorageClusterSettings settings = new StorageClusterSettings();
        settings.setGossipPort(7100);
        settings.setCqlPort(9042);

        return settings;
    }

    @Override
    public void setClusterSettings(Subject subject, StorageClusterSettings clusterSettings) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
