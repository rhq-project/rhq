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

import org.testng.annotations.Test;
import org.jmock.Mockery;
import org.jmock.Expectations;
import org.rhq.core.pc.inventory.InventoryService;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ResourceConfigurationFacet;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationFormat;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.clientapi.agent.PluginContainerException;

public class ConfigurationManagerUnitTest {

    static final String LEGACY_AMPS_VERSION = "2.0";

    static final String NON_LEGACY_AMPS_VERSION = "2.1";

    @Test
    public void loadConfigurationShouldCallLegacyFacetForLegacyAmpsVersion() throws Exception {
        Mockery context = new Mockery();

        final InventoryService inventoryService = context.mock(InventoryService.class);

        final ConfigurationFacet configurationFacet = context.mock(ConfigurationFacet.class);

        final ResourceType resourceType = createResourceType();

        ConfigurationManager configurationMgr = new ConfigurationManager();
        configurationMgr.setInventoryService(inventoryService);

        final int resourceId = -1;
        boolean fromStructured = true;

        context.checking(new Expectations() {{
            allowing(inventoryService).getAmpsVersion(resourceId); will(returnValue(LEGACY_AMPS_VERSION));

            allowing(inventoryService).getResourceType(resourceId); will(returnValue(resourceType));

            atLeast(1).of(inventoryService).getComponent(with(resourceId),
                                                         with(ConfigurationFacet.class),
                                                         with(FacetLockType.READ),
                                                         (long)with(any(Long.class)),
                                                         with(true),
                                                         with(true)); will(returnValue(configurationFacet));

            atLeast(1).of(configurationFacet).loadResourceConfiguration(); will(returnValue(new Configuration()));
        }});

        configurationMgr.loadResourceConfiguration(resourceId, fromStructured);

        context.assertIsSatisfied();
    }

    @Test(expectedExceptions = {PluginContainerException.class})
    public void loadConfigurationShouldThrowExceptionWhenLegacyFacetReturnsNull() throws Exception {
        Mockery context = new Mockery();

        final InventoryService inventoryService = context.mock(InventoryService.class);

        final ConfigurationFacet configurationFacet = context.mock(ConfigurationFacet.class);

        final ResourceType resourceType = createResourceType();

        ConfigurationManager configurationMgr = new ConfigurationManager();
        configurationMgr.setInventoryService(inventoryService);

        final int resourceId = -1;
        boolean fromStructured = true;

        context.checking(new Expectations() {{
            allowing(inventoryService).getAmpsVersion(resourceId); will(returnValue(LEGACY_AMPS_VERSION));

            allowing(inventoryService).getResourceType(resourceId); will(returnValue(resourceType));

            allowing(inventoryService).getComponent(with(resourceId),
                                                         with(ConfigurationFacet.class),
                                                         with(FacetLockType.READ),
                                                         (long)with(any(Long.class)),
                                                         with(true),
                                                         with(true)); will(returnValue(configurationFacet));

            atLeast(1).of(configurationFacet).loadResourceConfiguration(); will(returnValue(null));
        }});

        configurationMgr.loadResourceConfiguration(resourceId, fromStructured);

        context.assertIsSatisfied();
    }

    @Test
    public void rawConfigsShouldBeLoadedWhenRawIsSupported() throws Exception {
        Mockery context = new Mockery();

        final InventoryService inventoryService = context.mock(InventoryService.class);

        final ResourceConfigurationFacet configurationFacet = context.mock(ResourceConfigurationFacet.class);

        final ResourceType resourceType = createResourceTypeThatSupportsRaw();

        ConfigurationManager configurationMgr = new ConfigurationManager();
        configurationMgr.setInventoryService(inventoryService);

        final int resourceId = -1;
        boolean fromStructured = false;

        context.checking(new Expectations() {{
            allowing(inventoryService).getAmpsVersion(resourceId); will(returnValue(NON_LEGACY_AMPS_VERSION));

            allowing(inventoryService).getResourceType(resourceId); will(returnValue(resourceType));

            atLeast(1).of(inventoryService).getComponent(with(resourceId),
                                                         with(ResourceConfigurationFacet.class),
                                                         with(FacetLockType.READ),
                                                         (long)with(any(Long.class)),
                                                         with(true),
                                                         with(true)); will(returnValue(configurationFacet));

            atLeast(1).of(configurationFacet).loadRawConfigurations(); will(returnValue(new Configuration()));
        }});

        configurationMgr.loadResourceConfiguration(resourceId, fromStructured);

        context.assertIsSatisfied();
    }

    @Test
    public void structuredConfigShouldBeLoadedWhenStructuredIsSupported() throws Exception {
        Mockery context = new Mockery();

        final InventoryService inventoryService = context.mock(InventoryService.class);

        final ResourceConfigurationFacet configFacet = context.mock(ResourceConfigurationFacet.class);

        final ResourceType resourceType = createResourceTypeThatSupportsStructured();

        ConfigurationManager configurationMgr = new ConfigurationManager();
        configurationMgr.setInventoryService(inventoryService);

        final int resourceId = -1;
        boolean fromStructured = false;

        context.checking(new Expectations() {{
            allowing(inventoryService).getAmpsVersion(resourceId); will(returnValue(NON_LEGACY_AMPS_VERSION));

            allowing(inventoryService).getResourceType(resourceId); will(returnValue(resourceType));

            atLeast(1).of(inventoryService).getComponent(with(resourceId),
                                                         with(ResourceConfigurationFacet.class),
                                                         with(FacetLockType.READ),
                                                         (long)with(any(Long.class)),
                                                         with(true),
                                                         with(true)); will(returnValue(configFacet));

            atLeast(1).of(configFacet).loadStructuredConfiguration(); will(returnValue(new Configuration()));
        }});

        configurationMgr.loadResourceConfiguration(resourceId, fromStructured);

        context.assertIsSatisfied();
    }

    @Test
    public void structuredShouldBeLoadedWhenStructuredAndRawIsSupportedAndIsFromStructured() throws Exception {
        Mockery context = new Mockery();

        final InventoryService inventoryService = context.mock(InventoryService.class);

        final ResourceConfigurationFacet configFacet = context.mock(ResourceConfigurationFacet.class);

        final ResourceType resourceType = createResourceTypeThatSupportsStructuredAndRaw();

        ConfigurationManager configurationMgr = new ConfigurationManager();
        configurationMgr.setInventoryService(inventoryService);

        final int resourceId = -1;
        boolean fromStructured = true;

        context.checking(new Expectations() {{
            allowing(inventoryService).getAmpsVersion(resourceId); will(returnValue(NON_LEGACY_AMPS_VERSION));

            allowing(inventoryService).getResourceType(resourceId); will(returnValue(resourceType));

            atLeast(1).of(inventoryService).getComponent(with(resourceId),
                                                         with(ResourceConfigurationFacet.class),
                                                         with(FacetLockType.READ),
                                                         (long)with(any(Long.class)),
                                                         with(true),
                                                         with(true)); will(returnValue(configFacet));

            atLeast(1).of(configFacet).loadStructuredConfiguration(); will(returnValue(new Configuration()));
        }});

        configurationMgr.loadResourceConfiguration(resourceId, fromStructured);

        context.assertIsSatisfied();
    }

    @Test
    public void rawShouldBeLoadedWhenStructuredAndRawIsSupportedAndIsFromRaw() throws Exception {
        Mockery context = new Mockery();

        final InventoryService inventoryService = context.mock(InventoryService.class);

        final ResourceConfigurationFacet configFacet = context.mock(ResourceConfigurationFacet.class);

        final ResourceType resourceType = createResourceTypeThatSupportsStructuredAndRaw();

        ConfigurationManager configurationMgr = new ConfigurationManager();
        configurationMgr.setInventoryService(inventoryService);

        final int resourceId = -1;
        boolean fromStructured = false;

        context.checking(new Expectations() {{
            allowing(inventoryService).getAmpsVersion(resourceId); will(returnValue(NON_LEGACY_AMPS_VERSION));

            allowing(inventoryService).getResourceType(resourceId); will(returnValue(resourceType));

            atLeast(1).of(inventoryService).getComponent(with(resourceId),
                                                         with(ResourceConfigurationFacet.class),
                                                         with(FacetLockType.READ),
                                                         (long)with(any(Long.class)),
                                                         with(true),
                                                         with(true)); will(returnValue(configFacet));

            atLeast(1).of(configFacet).loadRawConfigurations(); will(returnValue(new Configuration()));
        }});

        configurationMgr.loadResourceConfiguration(resourceId, fromStructured);

        context.assertIsSatisfied();
    }

//    @Test(expectedExceptions = {PluginContainerException.class})
//    public void exceptionShouldBeThrowWhenNonLegacyFacetReturnsNullForItsConfig() throws Exception {
//    public void loadConfigurationShouldThrowExceptionWhenNonLegacyFacetReturnsNullFromStructured() throws Exception {
//        Mockery context = new Mockery();
//
//        final InventoryService inventoryService = context.mock(InventoryService.class);
//
//        final ResourceConfigurationFacet configurationFacet = context.mock(ResourceConfigurationFacet.class);
//
//        final ResourceType resourceType = createResourceType();
//
//        ConfigurationManager configurationMgr = new ConfigurationManager();
//        configurationMgr.setInventoryService(inventoryService);
//
//        final int resourceId = -1;
//        boolean fromStructured = true;
//
//        context.checking(new Expectations() {{
//            allowing(inventoryService).getAmpsVersion(resourceId); will(returnValue(LEGACY_AMPS_VERSION));
//
//            allowing(inventoryService).getResourceType(resourceId); will(returnValue(resourceType));
//
//            allowing(inventoryService).getComponent(with(resourceId),
//                                                         with(ResourceConfigurationFacet.class),
//                                                         with(FacetLockType.READ),
//                                                         (long)with(any(Long.class)),
//                                                         with(true),
//                                                         with(true)); will(returnValue(configurationFacet));
//
//            atLeast(1).of(configurationFacet).loadRawConfigurations(); will(returnValue(null));
//        }});
//
//        configurationMgr.loadResourceConfiguration(resourceId, fromStructured);
//
//        context.assertIsSatisfied();
//    }

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
