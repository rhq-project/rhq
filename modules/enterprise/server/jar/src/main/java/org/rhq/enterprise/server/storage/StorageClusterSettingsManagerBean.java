package org.rhq.enterprise.server.storage;

import java.util.Map;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.cloud.StorageClusterSettings;
import org.rhq.core.domain.common.composite.SystemSetting;
import org.rhq.core.domain.common.composite.SystemSettings;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.system.SystemManagerLocal;

/**
 * @author John Sanda
 */
@Stateless
public class StorageClusterSettingsManagerBean implements StorageClusterSettingsManagerLocal {

    @EJB
    private SystemManagerLocal systemManager;

    @Override
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public StorageClusterSettings getClusterSettings(Subject subject) {
        SystemSettings settings = systemManager.getSystemSettings(subject);
        Map<String, String> settingsMap = settings.toMap();
        StorageClusterSettings clusterSettings = new StorageClusterSettings();

        if (!settingsMap.containsKey(SystemSetting.STORAGE_CQL_PORT.getInternalName())) {
            return null;
        } else {
            clusterSettings.setCqlPort(Integer.parseInt(settingsMap.get(
                SystemSetting.STORAGE_CQL_PORT.getInternalName())));
        }

        if (!settingsMap.containsKey(SystemSetting.STORAGE_GOSSIP_PORT.getInternalName())) {
            return null;
        } else {
            clusterSettings.setGossipPort(Integer.parseInt(settingsMap.get(
                SystemSetting.STORAGE_GOSSIP_PORT.getInternalName())));
        }

        return clusterSettings;
    }

    @Override
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public void setClusterSettings(Subject subject, StorageClusterSettings clusterSettings) {
        SystemSettings settings = new SystemSettings();
        settings.put(SystemSetting.STORAGE_CQL_PORT, Integer.toString(clusterSettings.getCqlPort()));
        settings.put(SystemSetting.STORAGE_GOSSIP_PORT, Integer.toString(clusterSettings.getGossipPort()));
        systemManager.setStorageClusterSettings(subject, settings);
    }

}
