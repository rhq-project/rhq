package org.rhq.core.domain.common.composite;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

// TODO javadocs...
public class SystemSettings implements Serializable {

    private static final long serialVersionUID = 1L;

    private Map<String, String> systemConfiguration;

    private Map<String, String> driftPlugins = new HashMap<String, String>();

    public SystemSettings() {
    }

    public SystemSettings(Map<String, String> sysConfig, Map<String, String> driftPlugins) {
        systemConfiguration = sysConfig;
        this.driftPlugins = driftPlugins;
    }

    public Map<String, String> getSystemConfiguration() {
        return systemConfiguration;
    }

    public void setSystemConfiguration(Map<String, String> systemConfiguration) {
        this.systemConfiguration = systemConfiguration;
    }

    public Map<String, String> getDriftPlugins() {
        return driftPlugins;
    }

    public void setDriftPlugins(Map<String, String> driftPlugins) {
        this.driftPlugins = driftPlugins;
    }
}
