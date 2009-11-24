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

package org.rhq.core.pc.configuration;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.RawConfiguration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.test.JMockTest;

import static org.rhq.test.AssertUtils.*;
import static org.testng.Assert.assertEquals;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class ConfigManagementTest extends JMockTest {

    Random random = new Random();

    ResourceType resourceType = new ResourceType();
    int resourceId = -1;
    boolean daemonThread = true;
    boolean onlyIfStarted = true;

    Configuration createStructuredConfig() {
        Configuration config = new Configuration();
        config.put(new PropertySimple("x", "1"));
        config.put(new PropertySimple("y", "2"));

        return config;
    }

    Set<RawConfiguration> toSet(RawConfiguration... rawConfigs) {
        Set<RawConfiguration> rawConfigSet = new HashSet<RawConfiguration>();
        for (RawConfiguration rawConfig : rawConfigs) {
            rawConfigSet.add(rawConfig);
        }
        return rawConfigSet;
    }

    RawConfiguration createRawConfiguration(String path) {
        RawConfiguration rawConfig = new RawConfiguration();
        rawConfig.setContents(randomBytes());
        rawConfig.setPath(path);

        return rawConfig;
    }

    byte[] randomBytes() {
        byte[] bytes = new byte[10];
        random.nextBytes(bytes);

        return bytes;
    }

    ConfigurationDefinition getResourceConfigDefinition() {
        if (resourceType.getResourceConfigurationDefinition() == null) {
            resourceType.setResourceConfigurationDefinition(new ConfigurationDefinition("", ""));
        }
        return resourceType.getResourceConfigurationDefinition();
    }

    void assertRawsLoaded(Set<RawConfiguration> expectedRaws, Configuration actualConfig) {
        assertCollectionEqualsNoOrder(
            expectedRaws,
            actualConfig.getRawConfigurations(),
            "The raw configs were not loaded correctly."
        );
//        assertEquals(
//            actualConfig.getRawConfigurations(),
//            expectedRaws,
//            "The raw configs were not loaded correctly."
//        );
    }

    void assertStructuredLoaded(Configuration expectedConfig, Configuration actualConfig) {
        assertCollectionEqualsNoOrder(
            expectedConfig.getProperties(),
            actualConfig.getProperties(),
            "The structured configuration was not loaded correctly."
        );

//        assertEquals(
//            actualConfig.getAllProperties(),
//            expectedConfig.getAllProperties(),
//            "The structured configuration was not loaded correctly."
//        );
    }

    void assertNotesSetToDefault(Configuration loadedConfig) {
        String expectedNotes = "Resource config for " + resourceType.getName() + " Resource w/ id " + resourceId;

        assertEquals(loadedConfig.getNotes(), expectedNotes, "The notes property should be set to a default when it is not already initialized.");
    }
}
