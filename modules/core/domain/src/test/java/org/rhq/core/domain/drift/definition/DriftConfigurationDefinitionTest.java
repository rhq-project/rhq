/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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

package org.rhq.core.domain.drift.definition;

import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;

import static org.rhq.core.domain.configuration.definition.ConfigurationTemplate.DEFAULT_TEMPLATE_NAME;
import static org.testng.Assert.*;

public class DriftConfigurationDefinitionTest {

    @Test
    public void getName() {
        ConfigurationDefinition configDef = new ConfigurationDefinition("test", "test");
        DriftConfigurationDefinition driftConfigDef = new DriftConfigurationDefinition(configDef);

        assertEquals(driftConfigDef.getName(), configDef.getName(), "Failed to get name drift configuration name");
    }

    @Test
    public void getBasedir() {
        String basedir = "/opt/drift/test";
        Configuration config = new Configuration();
        config.put(new PropertySimple("basedir", basedir));

        ConfigurationTemplate template = new ConfigurationTemplate(DEFAULT_TEMPLATE_NAME,
            "default drift configuration");
        template.setConfiguration(config);

        ConfigurationDefinition configDef = new ConfigurationDefinition("test", "tets");
        configDef.putTemplate(template);

        DriftConfigurationDefinition driftConfigDef = new DriftConfigurationDefinition(configDef);

        assertEquals(driftConfigDef.getBasedir(), basedir, "Failed to get drift configuration basedir");
    }

    @Test
    public void getInterval() {
        long interval = 3600L;
        Configuration config = new Configuration();
        config.put(new PropertySimple("interval", interval));

        ConfigurationTemplate template = new ConfigurationTemplate(DEFAULT_TEMPLATE_NAME,
            "default drift configuration");
        template.setConfiguration(config);

        ConfigurationDefinition configDef = new ConfigurationDefinition("test", "test");
        configDef.putTemplate(template);

        DriftConfigurationDefinition driftConfigDef = new DriftConfigurationDefinition(configDef);

        assertEquals(driftConfigDef.getInterval(), interval, "Failed to get drift configuration interval");
    }

//    @Test
//    public void setIncludeFilters() {
//        Configuration config = new Configuration();
//
//        PropertyList includes = new PropertyList("includes", newInclude("/foo/bar", "*.jar"),
//            newInclude("/bar/foo", "*"));
//        config.put(includes);
//
//        ConfigurationTemplate template = new ConfigurationTemplate(DEFAULT_TEMPLATE_NAME,
//            "default drift configuration");
//        template.setConfiguration(config);
//
//        ConfigurationDefinition configDef = new ConfigurationDefinition("test", "test");
//
//        DriftConfigurationDefinition driftConfigDef = new DriftConfigurationDefinition(configDef);
//
//
//    }
//
//    PropertyMap newInclude(String path, String pattern) {
//        return new PropertyMap("include", new PropertySimple("path", path), new PropertySimple("pattern", pattern),
//            new PropertySimple("type", "include"));
//    }
//
//    PropertyMap newExclude(String path, String pattern) {
//        return new PropertyMap("exclude", new PropertySimple("path", path), new PropertySimple("pattern", pattern),
//            new PropertySimple("type", "exclude"));
//    }

}
