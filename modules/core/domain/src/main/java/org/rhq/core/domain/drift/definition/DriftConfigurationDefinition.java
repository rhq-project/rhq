package org.rhq.core.domain.drift.definition;

import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;

public class DriftConfigurationDefinition {
    private ConfigurationDefinition configurationDefinition;

    public DriftConfigurationDefinition(ConfigurationDefinition configDef) {
        configurationDefinition = configDef;
    }

}
