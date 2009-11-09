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
import org.rhq.core.pc.inventory.InventoryService;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationFormat;
import org.jmock.Expectations;

public class ResourceConfigurationFactoryStrategyImplTest extends JMockTest {

    static final String LEGACY_AMPS_VERSION = "2.0";

    static final String NON_LEGACY_AMPS_VERSION = "2.1";

    InventoryService componentService;

    int resourceId = -1;

    ResourceConfigurationStrategyFactoryImpl factory;

    @BeforeMethod
    public void setup() {
        componentService = context.mock(InventoryService.class);

        factory = new ResourceConfigurationStrategyFactoryImpl();
        factory.setComponentService(componentService);
    }

    @Test
    public void factoryShouldReturnLegacyStrategyWhenAmpsVersionIsLessThan2dot1() throws Exception {
        context.checking(new Expectations() {{
            allowing(componentService).getAmpsVersion(resourceId); will(returnValue(LEGACY_AMPS_VERSION));
        }});

        ResourceConfigurationStrategy strategy = factory.getStrategy(resourceId);

        assertTrue(
            strategy instanceof LegacyResourceConfigurationStrategy,
            "Expected to get an instance of " + LegacyResourceConfigurationStrategy.class.getSimpleName() + " when the " +
            "resource is from a plugin having an ampsVersion less than " + NON_LEGACY_AMPS_VERSION
        );
    }

    @Test
    public void factoryShouldInitializeLegacyStrategyWithComponentService() throws Exception {
        context.checking(new Expectations() {{
            allowing(componentService).getAmpsVersion(resourceId); will(returnValue(LEGACY_AMPS_VERSION));
        }});

        ResourceConfigurationStrategy strategy = factory.getStrategy(resourceId);

        assertComponentServiceInitialized(strategy);
    }

    @Test
    public void factoryShouldInitializeLegacyStrategyWithConfigurationUtilityService() throws Exception {
        context.checking(new Expectations() {{
            allowing(componentService).getAmpsVersion(resourceId); will(returnValue(LEGACY_AMPS_VERSION));
        }});

        ResourceConfigurationStrategy strategy = factory.getStrategy(resourceId);

        assertConfigurationUtilityServiceInitialized(strategy);
    }

    @Test
    public void factoryShouldReturnStructuredStrategyWhenAmpsVersionIs2dot1AndFormatIsStructured() throws Exception {
        context.checking(new Expectations() {{
            allowing(componentService).getAmpsVersion(resourceId); will(returnValue(NON_LEGACY_AMPS_VERSION));

            allowing(componentService).getResourceType(resourceId);
            will(returnValue(createResourceTypeThatSupportsStructured()));
        }});

        ResourceConfigurationStrategy strategy = factory.getStrategy(resourceId);

        assertTrue(
            strategy instanceof StructuredResourceConfigurationStrategy,
            "Expected to get an instance of " + StructuredResourceConfigurationStrategy.class.getSimpleName() +
            "when resource is from a plugin having an ampsversion >= " + NON_LEGACY_AMPS_VERSION + " and the resource " +
            "configuration format is structured."
        );
    }

    @Test
    public void factoryShouldInitializeStructuredStrategyWithComponentService() throws Exception {
        context.checking(new Expectations() {{
            allowing(componentService).getAmpsVersion(resourceId); will(returnValue(NON_LEGACY_AMPS_VERSION));

            allowing(componentService).getResourceType(resourceId);
            will(returnValue(createResourceTypeThatSupportsStructured()));
        }});

        ResourceConfigurationStrategy strategy = factory.getStrategy(resourceId);

        assertComponentServiceInitialized(strategy);
    }

    @Test
    public void factoryShouldInitializeStructuredStrategyWithConfigurationUtilityService() throws Exception {
        context.checking(new Expectations() {{
            allowing(componentService).getAmpsVersion(resourceId); will(returnValue(NON_LEGACY_AMPS_VERSION));

            allowing(componentService).getResourceType(resourceId);
            will(returnValue(createResourceTypeThatSupportsStructured()));
        }});

        ResourceConfigurationStrategy strategy = factory.getStrategy(resourceId);

        assertConfigurationUtilityServiceInitialized(strategy);
    }

    @Test
    public void factoryShouldReturnRawStrategyWhenAmpsVersionIs2dot1AndFormatIsRaw() throws Exception {
        context.checking(new Expectations() {{
            allowing(componentService).getAmpsVersion(resourceId); will(returnValue(NON_LEGACY_AMPS_VERSION));

            allowing(componentService).getResourceType(resourceId);
            will(returnValue(createResourceTypeThatSupportsRaw()));
        }});

        ResourceConfigurationStrategy strategy = factory.getStrategy(resourceId);

        assertTrue(
            strategy instanceof RawResourceConfigurationStrategy,
            "Expected to get an instance of " + RawResourceConfigurationStrategy.class.getSimpleName() + " when " +
            "resource is from a plugin having an ampsversion >= " + NON_LEGACY_AMPS_VERSION + " and the resource " +
            "configuration format is raw."
        );
   }

   @Test
    public void factoryShouldInitializeRawStrategyWithComponentService() throws Exception {
        context.checking(new Expectations() {{
            allowing(componentService).getAmpsVersion(resourceId); will(returnValue(NON_LEGACY_AMPS_VERSION));

            allowing(componentService).getResourceType(resourceId);
            will(returnValue(createResourceTypeThatSupportsStructured()));
        }});

        ResourceConfigurationStrategy strategy = factory.getStrategy(resourceId);

        assertComponentServiceInitialized(strategy);
    }

    @Test
    public void factoryShouldInitializeRawStrategyWithConfigurationUtilityService() throws Exception {
        context.checking(new Expectations() {{
            allowing(componentService).getAmpsVersion(resourceId); will(returnValue(NON_LEGACY_AMPS_VERSION));

            allowing(componentService).getResourceType(resourceId);
            will(returnValue(createResourceTypeThatSupportsStructured()));
        }});

        ResourceConfigurationStrategy strategy = factory.getStrategy(resourceId);

        assertConfigurationUtilityServiceInitialized(strategy);
    }

    @Test
    public void factoryShouldReturnStructuredAndRawStrategyWhenAmpsVersionIs2dot1AndFormatIsBoth() throws Exception {
        context.checking(new Expectations() {{
            allowing(componentService).getAmpsVersion(resourceId); will(returnValue(NON_LEGACY_AMPS_VERSION));

            allowing(componentService).getResourceType(resourceId);
            will(returnValue(createResourceTypeThatSupportsStructuredAndRaw()));
        }});

        ResourceConfigurationStrategy strategy = factory.getStrategy(resourceId);

        assertTrue(
            strategy instanceof StructuredAndRawResourceConfigurationStrategy,
            "Expected to get an instance of" + StructuredAndRawResourceConfigurationStrategy.class.getSimpleName() +
            " when resource is from a plugin having an ampsVersion >= " + NON_LEGACY_AMPS_VERSION + " and the " +
            "resource configuration format is both (structured and raw)."
        );
    }

    @Test
    public void factoryShouldInitializeStructuredAndRawStrategyWithComponentService() throws Exception {
        context.checking(new Expectations() {{
            allowing(componentService).getAmpsVersion(resourceId); will(returnValue(NON_LEGACY_AMPS_VERSION));

            allowing(componentService).getResourceType(resourceId);
            will(returnValue(createResourceTypeThatSupportsStructured()));
        }});

        ResourceConfigurationStrategy strategy = factory.getStrategy(resourceId);

        assertComponentServiceInitialized(strategy);
    }

    @Test
    public void factoryShouldInitializeStructuredAndRawStrategyWithConfigurationUtilityService() throws Exception {
        context.checking(new Expectations() {{
            allowing(componentService).getAmpsVersion(resourceId); will(returnValue(NON_LEGACY_AMPS_VERSION));

            allowing(componentService).getResourceType(resourceId);
            will(returnValue(createResourceTypeThatSupportsStructured()));
        }});

        ResourceConfigurationStrategy strategy = factory.getStrategy(resourceId);

        assertConfigurationUtilityServiceInitialized(strategy);
    }

    void assertComponentServiceInitialized(ResourceConfigurationStrategy strategy) {
        assertNotNull(strategy.getComponentService(), "The factory must initialize the componentService " +
                "property of the strategy object.");
    }

    void assertConfigurationUtilityServiceInitialized(ResourceConfigurationStrategy strategy) {
        assertNotNull(strategy.getConfigurationUtilityService(), "The factory must initialize the " +
                "configurationUtilityService property of the strategy object.");
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
