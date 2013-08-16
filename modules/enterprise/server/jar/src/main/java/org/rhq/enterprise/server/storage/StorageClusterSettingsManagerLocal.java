package org.rhq.enterprise.server.storage;

import javax.ejb.Local;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.cloud.StorageClusterSettings;

/**
 * @author John Sanda
 */
@Local
public interface StorageClusterSettingsManagerLocal {
    StorageClusterSettings getClusterSettings(Subject subject);

    void setClusterSettings(Subject subject, StorageClusterSettings clusterSettings);
}
