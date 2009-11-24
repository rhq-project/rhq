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

import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.rhq.core.pc.util.ComponentService;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationFormat;
import org.rhq.test.JMockTest;
import org.jmock.Expectations;

public class LoadResourceConfigurationFactoryImplTest extends JMockTest {

    static final String LEGACY_AMPS_VERSION = "2.0";

    static final String NON_LEGACY_AMPS_VERSION = "2.1";

    ComponentService componentService;

    int resourceId = -1;

    LoadResourceConfigurationFactoryImpl factory;

    @BeforeMethod
    public void setup() {
        componentService = context.mock(ComponentService.class);

        factory = new LoadResourceConfigurationFactoryImpl();
        factory.setComponentService(componentService);
    }

    @Test
    public void factoryShouldReturnLegacyLoadConfigWhenAmpsVersionIsLessThan2dot1() throws Exception {
        context.checking(new Expectations() {{
            allowing(componentService).getAmpsVersion(resourceId); will(returnValue(LEGACY_AMPS_VERSION));
        }});

        ConfigManagement loadConfig = factory.getStrategy(resourceId);

        assertTrue(
            loadConfig instanceof LegacyConfigManagement,
            "Expected to get an instance of " + LegacyConfigManagement.class.getSimpleName() + " when the " +
            "resource is from a plugin having an ampsVersion less than " + NON_LEGACY_AMPS_VERSION
        );
    }

    @Test
    public void factoryShouldInitializeLegacyLoadConfigWithComponentService() throws Exception {
        context.checking(new Expectations() {{
            allowing(componentService).getAmpsVersion(resourceId); will(returnValue(LEGACY_AMPS_VERSION));
        }});

        ConfigManagement loadConfig = factory.getStrategy(resourceId);

        assertComponentServiceInitialized(loadConfig);
    }

    @Test
    public void factoryShouldInitializeLegacyLoadConfigWithConfigurationUtilityService() throws Exception {
        context.checking(new Expectations() {{
            allowing(componentService).getAmpsVersion(resourceId); will(returnValue(LEGACY_AMPS_VERSION));
        }});

        ConfigManagement loadConfig = factory.getStrategy(resourceId);

        assertConfigurationUtilityServiceInitialized(loadConfig);
    }

    @Test
    public void factoryShouldReturnStructuredLoadConfigWhenAmpsVersionIs2dot1AndFormatIsStructured() throws Exception {
        context.checking(new Expectations() {{
            allowing(componentService).getAmpsVersion(resourceId); will(returnValue(NON_LEGACY_AMPS_VERSION));

            allowing(componentService).getResourceType(resourceId);
            will(returnValue(createResourceTypeThatSupportsStructured()));
        }});

        ConfigManagement loadConfig = factory.getStrategy(resourceId);

        assertTrue(
            loadConfig instanceof LoadStructured,
            "Expected to get an instance of " + LoadStructured.class.getSimpleName() +
            "when resource is from a plugin having an ampsversion >= " + NON_LEGACY_AMPS_VERSION + " and the resource " +
            "configuration format is structured."
        );
    }

    @Test
    public void factoryShouldInitializeLoadStructuredWithComponentService() throws Exception {
        context.checking(new Expectations() {{
            allowing(componentService).getAmpsVersion(resourceId); will(returnValue(NON_LEGACY_AMPS_VERSION));

            allowing(componentService).getResourceType(resourceId);
            will(returnValue(createResourceTypeThatSupportsStructured()));
        }});

        ConfigManagement loadConfig = factory.getStrategy(resourceId);

        assertComponentServiceInitialized(loadConfig);
    }

    @Test
    public void factoryShouldInitializeLoadStructuredWithConfigurationUtilityService() throws Exception {
        context.checking(new Expectations() {{
            allowing(componentService).getAmpsVersion(resourceId); will(returnValue(NON_LEGACY_AMPS_VERSION));

            allowing(componentService).getResourceType(resourceId);
            will(returnValue(createResourceTypeThatSupportsStructured()));
        }});

        ConfigManagement loadConfig = factory.getStrategy(resourceId);

        assertConfigurationUtilityServiceInitialized(loadConfig);
    }

    @Test
    public void factoryShouldReturnLoadRawWhenAmpsVersionIs2dot1AndFormatIsRaw() throws Exception {
        context.checking(new Expectations() {{
            allowing(componentService).getAmpsVersion(resourceId); will(returnValue(NON_LEGACY_AMPS_VERSION));

            allowing(componentService).getResourceType(resourceId);
            will(returnValue(createResourceTypeThatSupportsRaw()));
        }});

        ConfigManagement loadConfig = factory.getStrategy(resourceId);

        assertTrue(
            loadConfig instanceof RawConfigManagement,
            "Expected to get an instance of " + RawConfigManagement.class.getSimpleName() + " when " +
            "resource is from a plugin having an ampsversion >= " + NON_LEGACY_AMPS_VERSION + " and the resource " +
            "configuration format is raw."
        );
   }

   @Test
    public void factoryShouldInitializeLoadRawWithComponentService() throws Exception {
        context.checking(new Expectations() {{
            allowing(componentService).getAmpsVersion(resourceId); will(returnValue(NON_LEGACY_AMPS_VERSION));

            allowing(componentService).getResourceType(resourceId);
            will(returnValue(createResourceTypeThatSupportsStructured()));
        }});

        ConfigManagement loadConfig = factory.getStrategy(resourceId);

        assertComponentServiceInitialized(loadConfig);
    }

    @Test
    public void factoryShouldInitializeLoadRawWithConfigurationUtilityService() throws Exception {
        context.checking(new Expectations() {{
            allowing(componentService).getAmpsVersion(resourceId); will(returnValue(NON_LEGACY_AMPS_VERSION));

            allowing(componentService).getResourceType(resourceId);
            will(returnValue(createResourceTypeThatSupportsStructured()));
        }});

        ConfigManagement loadConfig = factory.getStrategy(resourceId);

        assertConfigurationUtilityServiceInitialized(loadConfig);
    }

    @Test
    public void factoryShouldReturnLoadStructuredAndRawWhenAmpsVersionIs2dot1AndFormatIsBoth() throws Exception {
        context.checking(new Expectations() {{
            allowing(componentService).getAmpsVersion(resourceId); will(returnValue(NON_LEGACY_AMPS_VERSION));

            allowing(componentService).getResourceType(resourceId);
            will(returnValue(createResourceTypeThatSupportsStructuredAndRaw()));
        }});

        ConfigManagement loadConfig = factory.getStrategy(resourceId);

        assertTrue(
            loadConfig instanceof LoadStructuredAndRaw,
            "Expected to get an instance of" + LoadStructuredAndRaw.class.getSimpleName() +
            " when resource is from a plugin having an ampsVersion >= " + NON_LEGACY_AMPS_VERSION + " and the " +
            "resource configuration format is both (structured and raw)."
        );
    }

    @Test
    public void factoryShouldInitializeLoadStructuredAndRawWithComponentService() throws Exception {
        context.checking(new Expectations() {{
            allowing(componentService).getAmpsVersion(resourceId); will(returnValue(NON_LEGACY_AMPS_VERSION));

            allowing(componentService).getResourceType(resourceId);
            will(returnValue(createResourceTypeThatSupportsStructured()));
        }});

        ConfigManagement loadConfig = factory.getStrategy(resourceId);

        assertComponentServiceInitialized(loadConfig);
    }

    @Test
    public void factoryShouldInitializeLoadStructuredAndRawWithConfigurationUtilityService() throws Exception {
        context.checking(new Expectations() {{
            allowing(componentService).getAmpsVersion(resourceId); will(returnValue(NON_LEGACY_AMPS_VERSION));

            allowing(componentService).getResourceType(resourceId);
            will(returnValue(createResourceTypeThatSupportsStructured()));
        }});

        ConfigManagement loadConfig = factory.getStrategy(resourceId);

        assertConfigurationUtilityServiceInitialized(loadConfig);
    }

    void assertComponentServiceInitialized(ConfigManagement loadConfig) {
        assertNotNull(loadConfig.getComponentService(), "The factory must initialize the componentService " +
                "property of the loadConfig object.");
    }

    void assertConfigurationUtilityServiceInitialized(ConfigManagement loadConfig) {
        assertNotNull(loadConfig.getConfigurationUtilityService(), "The factory must initialize the " +
                "configurationUtilityService property of the loadConfig object.");
    }

    ResourceType createResourceType() {
        ResourceType resourceType = new ResourceType();
        resourceType.setResourceConfigurationDefinition(new ConfigurationDefinition("test_config", "Test Configuration"));

        return resourceType;
    }

    ResourceType createResourceTypeThatSupportsStructured() {
        ResourceType resourceType = createResourceType();
        resourceType.getResourceConfigurationDefinition().setConfigurationFormat(ConfigurationFormat.STRUCTURED);

        return resourceType;
    }

    ResourceType createResourceTypeThatSupportsRaw() {
        ResourceType resourceType = createResourceType();
        resourceType.getResourceConfigurationDefinition().setConfigurationFormat(ConfigurationFormat.RAW);

        return resourceType;
    }

    ResourceType createResourceTypeThatSupportsStructuredAndRaw() {
        ResourceType resourceType = createResourceType();
        resourceType.getResourceConfigurationDefinition().setConfigurationFormat(ConfigurationFormat.STRUCTURED_AND_RAW);

        return resourceType;
    }

}
