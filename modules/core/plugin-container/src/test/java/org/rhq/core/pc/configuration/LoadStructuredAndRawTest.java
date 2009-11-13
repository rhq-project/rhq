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

import static org.testng.Assert.*;
import static java.util.Collections.*;

import org.rhq.core.pc.util.ComponentService;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pluginapi.configuration.ResourceConfigurationFacet;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.RawConfiguration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.resource.ResourceType;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.jmock.Expectations;
import org.jmock.Sequence;

import java.util.Random;
import java.util.Set;
import java.util.HashSet;

public class LoadStructuredAndRawTest extends JMockTest {

    static final boolean FROM_STRUCTURED = true;

    static final boolean FROM_RAW = false;

    ComponentService componentService;

    ConfigurationUtilityService configUtilityService;

    ResourceConfigurationFacet configFacet;

    int resourceId = -1;

    ResourceType resourceType;

    boolean daemonThread = true;

    boolean onlyIfStarted = true;

    LoadStructuredAndRaw loadStructuredAndRaw;

    Random random = new Random();

    @BeforeMethod
    public void setup() {
        resourceType = new ResourceType();
        resourceType.setResourceConfigurationDefinition(new ConfigurationDefinition("", ""));

        componentService = context.mock(ComponentService.class);
        configUtilityService = context.mock(ConfigurationUtilityService.class);

        configFacet = context.mock(ResourceConfigurationFacet.class);

        loadStructuredAndRaw = new LoadStructuredAndRaw();
        loadStructuredAndRaw.setComponentService(componentService);
        loadStructuredAndRaw.setConfigurationUtilityService(configUtilityService);
    }

    @Test
    public void rawSConfigshouldBeLoaded() throws Exception {
        Configuration config = new Configuration();

        Set<RawConfiguration> rawConfigs = toSet(
            createRawConfiguration("/tmp/foo.txt"),
            createRawConfiguration("/tmp/bar.txt")
        );


        addDefaultExpectations(config, rawConfigs);

        Configuration loadedConfig = loadStructuredAndRaw.execute(resourceId, FROM_STRUCTURED);

        assertRawsLoaded(rawConfigs, loadedConfig);
    }

    @Test
    public void structuredConfigShouldBeLoaded() throws Exception {
        Configuration config = new Configuration();
        config.put(new PropertySimple("x", "1"));
        config.put(new PropertySimple("y", "2"));

        addDefaultExpectations(config, EMPTY_SET);

        Configuration loadedConfig = loadStructuredAndRaw.execute(resourceId, FROM_STRUCTURED);

        assertStructuredLoaded(config, loadedConfig);
    }

    @Test
    public void theConfigNotesShouldGetSet() throws Exception {
        Configuration config = new Configuration();
        config.setNotes(null);

        addDefaultExpectations(config, EMPTY_SET);

        Configuration loadedConfig = loadStructuredAndRaw.execute(resourceId, FROM_STRUCTURED);

        String expectedNotes = "Resource config for " + resourceType.getName() + " Resource w/ id " + resourceId;

        assertEquals(
            loadedConfig.getNotes(),
            expectedNotes,
            "The notes property should be set to a default when it is not already initialized."
        );
    }

    @Test
    public void nullStructuredShouldBeIgnored() throws Exception {
        Configuration config = null;

        Set<RawConfiguration> rawConfigs = toSet(createRawConfiguration("/tmp/foo.txt"));

        addDefaultExpectations(config, rawConfigs);

        Configuration loadedConfig = loadStructuredAndRaw.execute(resourceId, FROM_STRUCTURED);

        Configuration emptyStructured = new Configuration();

        assertRawsLoaded(rawConfigs, loadedConfig);
        assertStructuredLoaded(emptyStructured, loadedConfig);
    }

    @Test
    public void nullRawsShouldBeIgnored() throws Exception {
        Configuration config = new Configuration();
        config.put(new PropertySimple("x", "1"));
        config.put(new PropertySimple("y", "2"));

        Set<RawConfiguration> rawConfigs = null;

        addDefaultExpectations(config, rawConfigs);

        Configuration loadedConfig = loadStructuredAndRaw.execute(resourceId, FROM_STRUCTURED);

        assertRawsLoaded(EMPTY_SET, loadedConfig);
        assertStructuredLoaded(config, loadedConfig);
    }

    @Test
    public void nullShouldBeReturnedWhenStructuredAndRawAreNull() throws Exception {
        context.checking(new Expectations() {{
            atLeast(1).of(componentService).getComponent(resourceId,
                                                    ResourceConfigurationFacet.class,
                                                    FacetLockType.READ,
                                                    LoadResourceConfiguration.FACET_METHOD_TIMEOUT,
                                                    daemonThread,
                                                    onlyIfStarted);
            will(returnValue(configFacet));

            allowing(componentService).getResourceType(resourceId); will(returnValue(resourceType));

            oneOf(configFacet).loadStructuredConfiguration(); will(returnValue(null));

            oneOf(configFacet).loadRawConfigurations(); will(returnValue(null));
        }});

        Configuration loadedConfig = loadStructuredAndRaw.execute(resourceId, FROM_STRUCTURED);

        assertNull(loadedConfig, "Expected null to be returned when both structured and raw are null");
    }

    private void addDefaultExpectations(final Configuration config, final Set<RawConfiguration> rawConfigs)
        throws Exception {

        context.checking(new Expectations() {{
            atLeast(1).of(componentService).getComponent(resourceId,
                                                    ResourceConfigurationFacet.class,
                                                    FacetLockType.READ,
                                                    LoadResourceConfiguration.FACET_METHOD_TIMEOUT,
                                                    daemonThread,
                                                    onlyIfStarted);
            will(returnValue(configFacet));

            allowing(componentService).getResourceType(resourceId); will(returnValue(resourceType));

            oneOf(configFacet).loadStructuredConfiguration(); will(returnValue(config));

            oneOf(configFacet).loadRawConfigurations(); will(returnValue(rawConfigs));

            atLeast(1).of(configUtilityService).normalizeConfiguration(with(any(Configuration.class)),
                with(resourceType.getResourceConfigurationDefinition()));

            atLeast(1).of(configUtilityService).validateConfiguration(with(any(Configuration.class)),
                with(resourceType.getResourceConfigurationDefinition()));
        }});
    }

    void assertRawsLoaded(Set<RawConfiguration> expectedRaws, Configuration actualConfig) {
        assertEquals(
            actualConfig.getRawConfigurations(),
            expectedRaws,
            "The raw configs were not loaded correctly."
        );
    }

    void assertStructuredLoaded(Configuration expectedConfig, Configuration actualConfig) {
        assertEquals(
            actualConfig.getAllProperties(),
            expectedConfig.getAllProperties(),
            "The structured configuration was not loaded correctly."
        );
    }

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
}
