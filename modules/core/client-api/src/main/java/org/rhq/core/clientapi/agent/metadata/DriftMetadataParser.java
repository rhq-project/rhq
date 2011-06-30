/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.rhq.core.clientapi.agent.metadata;

import org.rhq.core.clientapi.descriptor.plugin.DriftDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.DriftFilterDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.DriftDescriptor.Basedir;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;
import org.rhq.core.domain.drift.DriftConfigurationDefinition;

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
        ConfigurationTemplate template = new ConfigurationTemplate(name, name + " drift configuration default template");
        template.setConfiguration(new Configuration());

        return template;
    }

    private void initEnabled(ConfigurationTemplate template) {
        template.getConfiguration().put(new PropertySimple(DriftConfigurationDefinition.PROP_ENABLED, false));
    }

    private void initName(DriftDescriptor descriptor, ConfigurationTemplate template) {
        template.getConfiguration().put(
            new PropertySimple(DriftConfigurationDefinition.PROP_NAME, descriptor.getName()));
    }

    private void initBasedir(DriftDescriptor descriptor, ConfigurationTemplate template) {
        Basedir basedir = descriptor.getBasedir();
        String valueContext = basedir.getValueContext();
        String valueName = basedir.getValueName();

        PropertyMap basedirMap = new PropertyMap(DriftConfigurationDefinition.PROP_BASEDIR);
        basedirMap.put(new PropertySimple(DriftConfigurationDefinition.PROP_BASEDIR_VALUECONTEXT, valueContext));
        basedirMap.put(new PropertySimple(DriftConfigurationDefinition.PROP_BASEDIR_VALUENAME, valueName));

        template.getConfiguration().put(basedirMap);
    }

    private void initInterval(DriftDescriptor descriptor, ConfigurationTemplate template) {
        Configuration config = template.getConfiguration();
        if (descriptor.getInterval() == null) {
            config.put(new PropertySimple(DriftConfigurationDefinition.PROP_INTERVAL, String
                .valueOf(DriftConfigurationDefinition.DEFAULT_INTERVAL)));
        } else {
            config.put(new PropertySimple(DriftConfigurationDefinition.PROP_INTERVAL, descriptor.getInterval()));
        }
    }

    private void initIncludes(DriftDescriptor descriptor, ConfigurationTemplate template) {
        if (descriptor.getIncludes() != null && descriptor.getIncludes().getInclude().size() > 0) {
            Configuration config = template.getConfiguration();
            PropertyList includes = new PropertyList(DriftConfigurationDefinition.PROP_INCLUDES);

            for (DriftFilterDescriptor include : descriptor.getIncludes().getInclude()) {
                PropertyMap includeMap = new PropertyMap(DriftConfigurationDefinition.PROP_INCLUDES_INCLUDE);
                includeMap.put(new PropertySimple(DriftConfigurationDefinition.PROP_PATH, include.getPath()));
                includeMap.put(new PropertySimple(DriftConfigurationDefinition.PROP_PATTERN, include.getPattern()));

                includes.add(includeMap);
            }
            config.put(includes);
        }
    }

    private void initExcludes(DriftDescriptor descriptor, ConfigurationTemplate template) {
        if (descriptor.getExcludes() != null && descriptor.getExcludes().getExclude().size() > 0) {
            Configuration config = template.getConfiguration();
            PropertyList excludes = new PropertyList(DriftConfigurationDefinition.PROP_EXCLUDES);

            for (DriftFilterDescriptor exclude : descriptor.getExcludes().getExclude()) {
                PropertyMap excludeMap = new PropertyMap(DriftConfigurationDefinition.PROP_EXCLUDES_EXCLUDE);
                excludeMap.put(new PropertySimple(DriftConfigurationDefinition.PROP_PATH, exclude.getPath()));
                excludeMap.put(new PropertySimple(DriftConfigurationDefinition.PROP_PATTERN, exclude.getPattern()));

                excludes.add(excludeMap);
            }
            config.put(excludes);
        }
    }

}
