package org.rhq.core.clientapi.agent.metadata;

import org.rhq.core.clientapi.descriptor.drift.DriftDescriptor;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;

import static org.rhq.core.domain.configuration.definition.ConfigurationTemplate.DEFAULT_TEMPLATE_NAME;
import static org.rhq.core.domain.configuration.definition.PropertySimpleType.LONG;
import static org.rhq.core.domain.configuration.definition.PropertySimpleType.STRING;

public class DriftMetadataParser {

    public ConfigurationDefinition parseDriftMetadata(DriftDescriptor descriptor) {
        ConfigurationDefinition configDef = new ConfigurationDefinition(descriptor.getName(),
            "drift configuration definition");
        configDef.putTemplate(createDefaultTemplate());

        initBasedir(descriptor, configDef);
        initInterval(descriptor, configDef);

        return configDef;
    }

    private ConfigurationTemplate createDefaultTemplate() {
        ConfigurationTemplate template = new ConfigurationTemplate(DEFAULT_TEMPLATE_NAME,
            "drift configuration default template");
        template.setConfiguration(new Configuration());

        return template;
    }

    private void initBasedir(DriftDescriptor descriptor, ConfigurationDefinition configDef) {
        String description = "The root directory from which snapshots will be generated during drift monitoring";
        PropertyDefinitionSimple basedir = new PropertyDefinitionSimple("basedir",description, true, STRING);
        basedir.setDisplayName("Base Directory");
        configDef.put(basedir);

        Configuration defaultConfig = configDef.getDefaultTemplate().getConfiguration();
        defaultConfig.put(new PropertySimple("basedir", descriptor.getBasedir()));
    }

    private void initInterval(DriftDescriptor descriptor, ConfigurationDefinition configDef) {
        String description = "The frequency in seconds in which drift monitoring should run. Defaults to thirty " +
            "minutes.";
        PropertyDefinitionSimple intervalDef = new PropertyDefinitionSimple("interval", description, false, LONG);
        intervalDef.setDisplayName("Drift Monitoring Interval");
        configDef.put(intervalDef);

        Configuration defaultConfig = configDef.getDefaultTemplate().getConfiguration();
        if (descriptor.getInterval() == null) {
            defaultConfig.put(new PropertySimple("interval", "1800"));
        } else {
            defaultConfig.put(new PropertySimple("interval", descriptor.getInterval()));
        }
    }

}
