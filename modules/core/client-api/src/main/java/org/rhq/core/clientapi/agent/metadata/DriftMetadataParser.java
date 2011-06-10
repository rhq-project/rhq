package org.rhq.core.clientapi.agent.metadata;

import org.rhq.core.clientapi.descriptor.drift.DriftDescriptor;
import org.rhq.core.clientapi.descriptor.drift.DriftFilterDescriptor;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;

import static org.rhq.core.domain.configuration.definition.PropertySimpleType.LONG;
import static org.rhq.core.domain.configuration.definition.PropertySimpleType.STRING;

public class DriftMetadataParser {

    public ConfigurationTemplate parseDriftMetadata(DriftDescriptor descriptor) {
        ConfigurationTemplate template = createTemplate(descriptor.getName());

        initEnabled(template);
        initName(descriptor, template);
        initBasedir(descriptor, template);
        initInterval(descriptor, template);
        initIncludes(descriptor, template);
        initExcludes(descriptor, template);

        return template;
    }

    private ConfigurationTemplate createTemplate(String name) {
        ConfigurationTemplate template = new ConfigurationTemplate(name,
            name + " drift configuration default template");
        template.setConfiguration(new Configuration());

        return template;
    }

    private void initEnabled(ConfigurationTemplate template) {
        template.getConfiguration().put(new PropertySimple("enabled", false));
    }

    private void initName(DriftDescriptor descriptor, ConfigurationTemplate template) {
        template.getConfiguration().put(new PropertySimple("name", descriptor.getName()));
    }

    private void initBasedir(DriftDescriptor descriptor, ConfigurationTemplate template) {
        template.getConfiguration().put(new PropertySimple("basedir", descriptor.getBasedir()));
    }

    private void initInterval(DriftDescriptor descriptor, ConfigurationTemplate template) {
        Configuration config = template.getConfiguration();
        if (descriptor.getInterval() == null) {
            config.put(new PropertySimple("interval", "1800"));
        } else {
            config.put(new PropertySimple("interval", descriptor.getInterval()));
        }
    }

    private void initIncludes(DriftDescriptor descriptor, ConfigurationTemplate template) {
        if (descriptor.getIncludes() != null && descriptor.getIncludes().getInclude().size() > 0) {
            Configuration config = template.getConfiguration();
            PropertyList includes = new PropertyList("includes");

            for (DriftFilterDescriptor include : descriptor.getIncludes().getInclude()) {
                PropertyMap includeMap = new PropertyMap("include");
                includeMap.put(new PropertySimple("path", include.getPath()));
                includeMap.put(new PropertySimple("pattern", include.getPattern()));

                includes.add(includeMap);
            }
            config.put(includes);
        }
    }

    private void initExcludes(DriftDescriptor descriptor, ConfigurationTemplate template) {
        if (descriptor.getExcludes() != null && descriptor.getExcludes().getExclude().size() > 0) {
            Configuration config = template.getConfiguration();
            PropertyList excludes = new PropertyList("excludes");

            for (DriftFilterDescriptor exclude : descriptor.getExcludes().getExclude()) {
                PropertyMap excludeMap = new PropertyMap("exclude");
                excludeMap.put(new PropertySimple("path", exclude.getPath()));
                excludeMap.put(new PropertySimple("pattern", exclude.getPattern()));

                excludes.add(excludeMap);
            }
            config.put(excludes);
        }
    }

}
