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

import org.jmock.Expectations;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.RawConfiguration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pluginapi.configuration.ResourceConfigurationFacet;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Set;

import static java.util.Collections.EMPTY_SET;
import static org.testng.Assert.assertNull;

public class StructuredAndRawConfigManagementTest extends ConfigManagementTest {

    ResourceConfigurationFacet configFacet;

    StructuredAndRawConfigManagement structuredAndRawConfigManagement;

    @BeforeMethod
    public void setup() {
        resourceType = new ResourceType();
        resourceType.setResourceConfigurationDefinition(new ConfigurationDefinition("", ""));

        configFacet = context.mock(ResourceConfigurationFacet.class);

        structuredAndRawConfigManagement = new StructuredAndRawConfigManagement();
        structuredAndRawConfigManagement.setComponentService(componentService);
        structuredAndRawConfigManagement.setConfigurationUtilityService(configUtilityService);
    }

    @Test
    public void rawConfigshouldGetLoaded() throws Exception {
        Configuration config = new Configuration();

        Set<RawConfiguration> rawConfigs = toSet(
            createRawConfiguration("/tmp/foo.txt"),
            createRawConfiguration("/tmp/bar.txt")
        );


        addDefaultExpectationsForLoad(config, rawConfigs);

        Configuration loadedConfig = structuredAndRawConfigManagement.executeLoad(resourceId);

        assertRawsLoaded(rawConfigs, loadedConfig);
    }

    @Test
    public void structuredConfigShouldGetLoaded() throws Exception {
        Configuration config = new Configuration();
        config.put(new PropertySimple("x", "1"));
        config.put(new PropertySimple("y", "2"));

        addDefaultExpectationsForLoad(config, EMPTY_SET);

        Configuration loadedConfig = structuredAndRawConfigManagement.executeLoad(resourceId);

        assertStructuredLoaded(config, loadedConfig);
    }

    @Test
    public void theConfigNotesShouldGetSet() throws Exception {
        Configuration config = new Configuration();
        config.setNotes(null);

        addDefaultExpectationsForLoad(config, EMPTY_SET);

        Configuration loadedConfig = structuredAndRawConfigManagement.executeLoad(resourceId);

        assertNotesSetToDefault(loadedConfig);
    }

    @Test
    public void nullStructuredShouldBeIgnored() throws Exception {
        Configuration config = null;

        Set<RawConfiguration> rawConfigs = toSet(createRawConfiguration("/tmp/foo.txt"));

        addDefaultExpectationsForLoad(config, rawConfigs);

        Configuration loadedConfig = structuredAndRawConfigManagement.executeLoad(resourceId);

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

        addDefaultExpectationsForLoad(config, rawConfigs);

        Configuration loadedConfig = structuredAndRawConfigManagement.executeLoad(resourceId);

        assertRawsLoaded(EMPTY_SET, loadedConfig);
        assertStructuredLoaded(config, loadedConfig);
    }

    @Test
    public void nullShouldBeReturnedWhenStructuredAndRawAreNull() throws Exception {
        context.checking(new Expectations() {{
            atLeast(1).of(componentService).getComponent(resourceId,
                                                    ResourceConfigurationFacet.class,
                                                    FacetLockType.READ,
                                                    ConfigManagement.FACET_METHOD_TIMEOUT,
                                                    daemonThread,
                                                    onlyIfStarted);
            will(returnValue(configFacet));

            allowing(componentService).getResourceType(resourceId); will(returnValue(resourceType));

            oneOf(configFacet).loadStructuredConfiguration(); will(returnValue(null));

            oneOf(configFacet).loadRawConfigurations(); will(returnValue(null));
        }});

        Configuration loadedConfig = structuredAndRawConfigManagement.executeLoad(resourceId);

        assertNull(loadedConfig, "Expected null to be returned when facet returns null for both structured and raw.");
    }

    private void addDefaultExpectationsForLoad(final Configuration config, final Set<RawConfiguration> rawConfigs)
        throws Exception {

        context.checking(new Expectations() {{
            atLeast(1).of(componentService).getComponent(resourceId,
                                                    ResourceConfigurationFacet.class,
                                                    FacetLockType.READ,
                                                    ConfigManagement.FACET_METHOD_TIMEOUT,
                                                    daemonThread,
                                                    onlyIfStarted);
            will(returnValue(configFacet));

            allowing(componentService).getResourceType(resourceId); will(returnValue(resourceType));

            oneOf(configFacet).loadStructuredConfiguration(); will(returnValue(config));

            oneOf(configFacet).loadRawConfigurations(); will(returnValue(rawConfigs));

            atLeast(1).of(configUtilityService).normalizeConfiguration(with(any(Configuration.class)),
                with(getResourceConfigDefinition()));

            atLeast(1).of(configUtilityService).validateConfiguration(with(any(Configuration.class)),
                with(getResourceConfigDefinition()));
        }});
    }

    @Test
    public void facetShouldBeCalledToUpdateStructuredAndRaw() throws Exception {
        final RawConfiguration raw1 = createRawConfiguration("/tmp/raw1.txt");
        final RawConfiguration raw2 = createRawConfiguration("/tmp/raw2.txt");

        final Configuration config = new Configuration();
        config.addRawConfiguration(raw1);
        config.addRawConfiguration(raw2);

        addDefaultExpectationsForUpdate();

        context.checking(new Expectations() {{
            oneOf(configFacet).validateStructuredConfiguration(config);
            oneOf(configFacet).persistStructuredConfiguration(config);

            oneOf(configFacet).validateRawConfiguration(raw1);
            oneOf(configFacet).persistRawConfiguration(raw1);

            oneOf(configFacet).validateRawConfiguration(raw2);
            oneOf(configFacet).persistRawConfiguration(raw2);
        }});

        structuredAndRawConfigManagement.executeUpdate(resourceId, config);
    }

    @Test(expectedExceptions = {ConfigurationUpdateException.class})
    public void exceptionShouldBeThrownWhenStructuredValidationFails() throws Exception {
        final RawConfiguration raw = createRawConfiguration("/tmp/raw.txt");

        final Configuration config = new Configuration();
        config.addRawConfiguration(raw);

        addDefaultExpectationsForUpdate();

        context.checking(new Expectations() {{
            oneOf(configFacet).validateStructuredConfiguration(config); will(throwException(new RuntimeException()));

            oneOf(configFacet).validateRawConfiguration(raw);
            oneOf(configFacet).persistRawConfiguration(raw);
        }});

        structuredAndRawConfigManagement.executeUpdate(resourceId, config);
    }

    @Test(expectedExceptions = {ConfigurationUpdateException.class})
    public void exceptionShouldBeThrownWhenStructuredUpdateFails() throws Exception {
        final RawConfiguration raw = createRawConfiguration("/tmp/raw.txt");

        final Configuration config = new Configuration();
        config.addRawConfiguration(raw);

        addDefaultExpectationsForUpdate();

        context.checking(new Expectations() {{
            oneOf(configFacet).validateStructuredConfiguration(config);
            oneOf(configFacet).persistStructuredConfiguration(config); will(throwException(new RuntimeException()));

            oneOf(configFacet).validateRawConfiguration(raw);
            oneOf(configFacet).persistRawConfiguration(raw);
        }});

        structuredAndRawConfigManagement.executeUpdate(resourceId, config);
    }

    @Test(expectedExceptions = {ConfigurationUpdateException.class})
    public void structuredAndSecondRawShouldStillGetUpdatedWhenFirstRawValidationFails() throws Exception {
        final RawConfiguration raw1 = createRawConfiguration("/tmp/raw1.txt");
        final RawConfiguration raw2 = createRawConfiguration("/tmp/raw2.txt");

        final Configuration config = new Configuration();
        config.addRawConfiguration(raw1);
        config.addRawConfiguration(raw2);

        addDefaultExpectationsForUpdate();

        context.checking(new Expectations() {{
            oneOf(configFacet).validateStructuredConfiguration(config);
            oneOf(configFacet).persistStructuredConfiguration(config);

            oneOf(configFacet).validateRawConfiguration(raw1); will(throwException(new RuntimeException()));

            oneOf(configFacet).validateRawConfiguration(raw2);
            oneOf(configFacet).persistRawConfiguration(raw2);
        }});

        structuredAndRawConfigManagement.executeUpdate(resourceId, config);
    }

    private void addDefaultExpectationsForUpdate() throws Exception {
        final boolean isDaemonThread = false;

        context.checking(new Expectations() {{
            atLeast(1).of(componentService).getComponent(resourceId,
                                                         ResourceConfigurationFacet.class,
                                                         FacetLockType.WRITE,
                                                         ConfigManagement.FACET_METHOD_TIMEOUT,
                                                         isDaemonThread,
                                                         onlyIfStarted);
            will(returnValue(configFacet));
        }});
    }

}
