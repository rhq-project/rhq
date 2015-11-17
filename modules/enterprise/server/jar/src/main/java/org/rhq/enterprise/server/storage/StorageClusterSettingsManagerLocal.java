package org.rhq.enterprise.server.storage;

import javax.ejb.Local;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.cloud.StorageClusterSettings;

/**
 * @author John Sanda
 */
@Local
public interface StorageClusterSettingsManagerLocal extends StorageClusterSettingsManagerRemote {

    void setClusterSettings(Subject subject, StorageClusterSettings clusterSettings);
}
