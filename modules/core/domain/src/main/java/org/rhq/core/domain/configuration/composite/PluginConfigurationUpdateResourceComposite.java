package org.rhq.core.domain.configuration.composite;

import org.rhq.core.domain.configuration.PluginConfigurationUpdate;

public class PluginConfigurationUpdateResourceComposite {

    private final PluginConfigurationUpdate update;
    private final int resourceId;
    private final String resourceName;

    public PluginConfigurationUpdateResourceComposite(PluginConfigurationUpdate update, Integer resourceId,
        String resourceName) {
        this.update = update;
        this.resourceId = resourceId;
        this.resourceName = resourceName;
    }

    public PluginConfigurationUpdate getUpdate() {
        return update;
    }

    public int getResourceId() {
        return resourceId;
    }

    public String getResourceName() {
        return resourceName;
    }

}
