package org.rhq.core.domain.drift.definition;

import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;

public class DriftConfigurationDefinition {
    private ConfigurationDefinition configurationDefinition;

    public DriftConfigurationDefinition(ConfigurationDefinition configDef) {
        configurationDefinition = configDef;
    }

    public String getName() {
        return configurationDefinition.getName();
    }

    public String getBasedir() {
        return configurationDefinition.getDefaultTemplate().getConfiguration().getSimpleValue("basedir", null);
    }

    public long getInterval() {
        return Long.parseLong(configurationDefinition.getDefaultTemplate().getConfiguration()
            .getSimpleValue("interval", null));
    }

}
