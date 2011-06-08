package org.rhq.core.clientapi.agent.metadata;

import org.rhq.core.clientapi.descriptor.drift.DriftDescriptor;
import org.rhq.core.clientapi.descriptor.drift.DriftFilterDescriptor;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
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
        initIncludes(descriptor, configDef);
        initExcludes(descriptor, configDef);

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
        basedir.setOrder(0);
        configDef.put(basedir);

        Configuration defaultConfig = configDef.getDefaultTemplate().getConfiguration();
        defaultConfig.put(new PropertySimple("basedir", descriptor.getBasedir()));
    }

    private void initInterval(DriftDescriptor descriptor, ConfigurationDefinition configDef) {
        String description = "The frequency in seconds in which drift monitoring should run. Defaults to thirty " +
            "minutes.";
        PropertyDefinitionSimple intervalDef = new PropertyDefinitionSimple("interval", description, false, LONG);
        intervalDef.setDisplayName("Drift Monitoring Interval");
        intervalDef.setOrder(1);
        configDef.put(intervalDef);

        Configuration defaultConfig = configDef.getDefaultTemplate().getConfiguration();
        if (descriptor.getInterval() == null) {
            defaultConfig.put(new PropertySimple("interval", "1800"));
        } else {
            defaultConfig.put(new PropertySimple("interval", descriptor.getInterval()));
        }
    }

    private void initIncludes(DriftDescriptor descriptor, ConfigurationDefinition configDef) {
        String description = "A set of patterns that specify files and/or directories to include.";
        PropertyDefinitionList includesDef = new PropertyDefinitionList();
        includesDef.setName("includes");
        includesDef.setDisplayName("Includes");
        includesDef.setDescription(description);
        includesDef.setRequired(false);
        includesDef.setOrder(2);

        PropertyDefinitionMap includesMapDef = new PropertyDefinitionMap("include", null, false, null);

        String pathDescription = "A file system path that can be a directory or a file. The path is assumed to be " +
            "relative to the base directory of the drift configuration.";
        PropertyDefinitionSimple pathDef = new PropertyDefinitionSimple("path", pathDescription, false, STRING);
        pathDef.setDisplayName("Path");

        // TODO Need to decide on verbage for the pattern description
        PropertyDefinitionSimple patternDef = new PropertyDefinitionSimple("pattern", null, false, STRING);
        patternDef.setDisplayName("Pattern");

        includesMapDef.put(pathDef);
        includesMapDef.put(patternDef);
        includesDef.setMemberDefinition(includesMapDef);
        configDef.put(includesDef);

        if (descriptor.getIncludes() != null && descriptor.getIncludes().getInclude().size() > 0) {
            Configuration defaultConfig = configDef.getDefaultTemplate().getConfiguration();
            PropertyList includes = new PropertyList("includes");

            for (DriftFilterDescriptor include : descriptor.getIncludes().getInclude()) {
                PropertyMap includeMap = new PropertyMap("include");
                includeMap.put(new PropertySimple("path", include.getPath()));
                includeMap.put(new PropertySimple("pattern", include.getPattern()));

                includes.add(includeMap);
            }
            defaultConfig.put(includes);
        }
    }

    private void initExcludes(DriftDescriptor descriptor, ConfigurationDefinition configDef) {
        String description = "A set of patterns that specify files and/or directories to exclude.";
        PropertyDefinitionList excludesDef = new PropertyDefinitionList();
        excludesDef.setName("excludes");
        excludesDef.setDisplayName("Excludes");
        excludesDef.setDescription(description);
        excludesDef.setRequired(false);
        excludesDef.setOrder(3);

        PropertyDefinitionMap excludesMapDef = new PropertyDefinitionMap("exclude", null, false, null);

        String pathDescription = "A file system path that can be a directory or a file. The path is assumed to be " +
            "relative to the base directory of the drift configuration.";
        PropertyDefinitionSimple pathDef = new PropertyDefinitionSimple("path", pathDescription, false, STRING);
        pathDef.setDisplayName("Path");

        // TODO Need to decide on verbage for the pattern description
        PropertyDefinitionSimple patternDef = new PropertyDefinitionSimple("pattern", null, false, STRING);
        patternDef.setDisplayName("Pattern");

        excludesMapDef.put(pathDef);
        excludesMapDef.put(patternDef);
        excludesDef.setMemberDefinition(excludesMapDef);
        configDef.put(excludesDef);

        if (descriptor.getExcludes() != null && descriptor.getExcludes().getExclude().size() > 0) {
            Configuration defaultConfig = configDef.getDefaultTemplate().getConfiguration();
            PropertyList excludes = new PropertyList("excludes");

            for (DriftFilterDescriptor exclude : descriptor.getExcludes().getExclude()) {
                PropertyMap excludeMap = new PropertyMap("exclude");
                excludeMap.put(new PropertySimple("path", exclude.getPath()));
                excludeMap.put(new PropertySimple("pattern", exclude.getPattern()));

                excludes.add(excludeMap);
            }
            defaultConfig.put(excludes);
        }
    }

}
