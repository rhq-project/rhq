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

import java.util.Set;

import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pluginapi.configuration.ResourceConfigurationFacet;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.RawConfiguration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationFormat;
import org.jmock.Expectations;



public class ConfigManagementFactoryImplTest extends ConfigManagementTest {

    static final String LEGACY_AMPS_VERSION = "2.0";

    static final String NON_LEGACY_AMPS_VERSION = "2.1";

    int resourceId = -1;

    ConfigManagementFactoryImpl factory;

    @BeforeMethod
    public void setup() {
        factory = new ConfigManagementFactoryImpl();
        factory.setComponentService(componentService);
    }

    @Test
    public void factoryShouldInitializeLegacyConfigMgmtWithComponentService() throws Exception {
        final boolean daemonOnly = true;
        final ResourceComponent resourceComponent = context.mock(ResourceComponent.class);

        context.checking(new Expectations() {
            {
                allowing(componentService).getComponent(resourceId, ResourceComponent.class, FacetLockType.READ,
                    ConfigManagement.FACET_METHOD_TIMEOUT, daemonOnly, onlyIfStarted);
                will(returnValue(resourceComponent));

                allowing(componentService).getResourceType(resourceId);
                will(returnValue(createResourceTypeThatSupportsStructured()));

                allowing(componentService).fetchResourceComponent(resourceId);
                will(returnValue(new MockResourceConfigurationFacet()));
            }
        });

        ConfigManagement loadConfig = factory.getStrategy(resourceId);

        assertComponentServiceInitialized(loadConfig);
    }

    @Test
    public void factoryShouldInitializeLegacyConfigMgmtWithConfigurationUtilityService() throws Exception {
        final boolean daemonOnly = true;
        final ResourceComponent resourceComponent = context.mock(ResourceComponent.class);

        context.checking(new Expectations() {
            {
                allowing(componentService).getComponent(resourceId, ResourceComponent.class, FacetLockType.READ,
                    ConfigManagement.FACET_METHOD_TIMEOUT, daemonOnly, onlyIfStarted);
                will(returnValue(resourceComponent));

                allowing(componentService).getResourceType(resourceId);
                will(returnValue(createResourceTypeThatSupportsStructured()));

                allowing(componentService).fetchResourceComponent(resourceId);
                will(returnValue(new MockResourceConfigurationFacetForLegacy()));
            
            }
        });

        ConfigManagement loadConfig = factory.getStrategy(resourceId);

        assertConfigurationUtilityServiceInitialized(loadConfig);
    }

    @Test
    public void factoryShouldInitializeStructuredConfigMgmtWithComponentService() throws Exception {
        final ResourceComponent resourceComponent = context.mock(ResourceComponent.class);

        context.checking(new Expectations() {
            {
                boolean daemonOnly = true;

                allowing(componentService).getComponent(resourceId, ResourceComponent.class, FacetLockType.READ,
                    ConfigManagement.FACET_METHOD_TIMEOUT, daemonOnly, onlyIfStarted);
                will(returnValue(resourceComponent));

                allowing(componentService).getResourceType(resourceId);
                will(returnValue(createResourceTypeThatSupportsStructured()));
                
                allowing(componentService).fetchResourceComponent(resourceId);
                will(returnValue(new MockResourceConfigurationFacet()));
            }
        });

        ConfigManagement loadConfig = factory.getStrategy(resourceId);

        assertComponentServiceInitialized(loadConfig);
    }

    @Test
    public void factoryShouldInitializeStructuredConfigMgmtWithConfigurationUtilityService() throws Exception {
        final boolean daemonOnly = true;

        final ResourceComponent resourceComponent = context.mock(ResourceComponent.class);

        context.checking(new Expectations() {
            {
                allowing(componentService).getComponent(resourceId, ResourceComponent.class, FacetLockType.READ,
                    ConfigManagement.FACET_METHOD_TIMEOUT, daemonOnly, onlyIfStarted);
                will(returnValue(resourceComponent));

                allowing(componentService).getResourceType(resourceId);
                will(returnValue(createResourceTypeThatSupportsStructured()));
                
                allowing(componentService).fetchResourceComponent(resourceId);
                will(returnValue(new MockResourceConfigurationFacet()));

            }
        });

        ConfigManagement loadConfig = factory.getStrategy(resourceId);

        assertConfigurationUtilityServiceInitialized(loadConfig);
    }

    @Test
    public void factoryShouldInitializeRawConfigMgmtWithComponentService() throws Exception {
        final boolean daemonOnly = true;

        final ResourceComponent resourceComponent = context.mock(ResourceComponent.class);

        context.checking(new Expectations() {
            {
                allowing(componentService).getComponent(resourceId, ResourceComponent.class, FacetLockType.READ,
                    ConfigManagement.FACET_METHOD_TIMEOUT, daemonOnly, onlyIfStarted);
                will(returnValue(resourceComponent));

                allowing(componentService).getResourceType(resourceId);
                will(returnValue(createResourceTypeThatSupportsRaw()));
            }
        });

        ConfigManagement loadConfig = factory.getStrategy(resourceId);

        assertComponentServiceInitialized(loadConfig);
    }

    @Test
    public void factoryShouldInitializeRawConfigMgmtWithConfigurationUtilityService() throws Exception {
        final boolean daemonOnly = true;

        final ResourceComponent resourceComponent = context.mock(ResourceComponent.class);

        context.checking(new Expectations() {
            {
                allowing(componentService).getComponent(resourceId, ResourceComponent.class, FacetLockType.READ,
                    ConfigManagement.FACET_METHOD_TIMEOUT, daemonOnly, onlyIfStarted);
                will(returnValue(resourceComponent));

                allowing(componentService).getResourceType(resourceId);
                will(returnValue(createResourceTypeThatSupportsRaw()));
            }
        });

        ConfigManagement loadConfig = factory.getStrategy(resourceId);

        assertConfigurationUtilityServiceInitialized(loadConfig);
    }

    @Test
    public void factoryShouldInitializeLoadStructuredAndRawWithComponentService() throws Exception {
        final boolean daemonOnly = true;

        final ResourceComponent resourceComponent = context.mock(ResourceComponent.class);

        context.checking(new Expectations() {
            {
                allowing(componentService).getComponent(resourceId, ResourceComponent.class, FacetLockType.READ,
                    ConfigManagement.FACET_METHOD_TIMEOUT, daemonOnly, onlyIfStarted);
                will(returnValue(resourceComponent));

                allowing(componentService).getResourceType(resourceId);
                will(returnValue(createResourceTypeThatSupportsStructured()));

                allowing(componentService).fetchResourceComponent(resourceId);
                will(returnValue(new MockResourceConfigurationFacet()));
            }
        });

        ConfigManagement loadConfig = factory.getStrategy(resourceId);

        assertComponentServiceInitialized(loadConfig);
    }

    @Test
    public void factoryShouldInitializeLoadStructuredAndRawWithConfigurationUtilityService() throws Exception {
        final boolean daemonOnly = true;
        final boolean onlyIfStarted = true;

        final ResourceComponent resourceComponent = context.mock(ResourceComponent.class);

        context.checking(new Expectations() {
            {
                allowing(componentService).getComponent(resourceId, ResourceComponent.class, FacetLockType.READ,
                    ConfigManagement.FACET_METHOD_TIMEOUT, daemonOnly, onlyIfStarted);
                will(returnValue(resourceComponent));

                allowing(componentService).getResourceType(resourceId);
                will(returnValue(createResourceTypeThatSupportsStructured()));
             
                allowing(componentService).fetchResourceComponent(resourceId);
                will(returnValue(new MockResourceConfigurationFacet()));
            }
        });

        ConfigManagement configManagement = factory.getStrategy(resourceId);

        assertConfigurationUtilityServiceInitialized(configManagement);
    }

    void assertComponentServiceInitialized(ConfigManagement configMgmt) {
        assertNotNull(configMgmt.getComponentService(), "The factory must initialize the componentService "
            + "property of the loadConfig object.");
    }

    void assertConfigurationUtilityServiceInitialized(ConfigManagement configMgmt) {
        assertNotNull(configMgmt.getConfigurationUtilityService(), "The factory must initialize the "
            + "configurationUtilityService property of the loadConfig object.");
    }

    ResourceType createResourceType() {
        ResourceType resourceType = new ResourceType();
        resourceType
            .setResourceConfigurationDefinition(new ConfigurationDefinition("test_config", "Test Configuration"));

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
        resourceType.getResourceConfigurationDefinition()
            .setConfigurationFormat(ConfigurationFormat.STRUCTURED_AND_RAW);

        return resourceType;
    }

}

class MockResourceConfigurationFacetForLegacy implements ResourceComponent{

    public void start(ResourceContext context) throws InvalidPluginConfigurationException, Exception {
        // TODO Auto-generated method stub
        
    }

    public void stop() {
        // TODO Auto-generated method stub
        
    }

    public AvailabilityType getAvailability() {
        // TODO Auto-generated method stub
        return null;
    }
}


class MockResourceConfigurationFacet implements ResourceComponent, ResourceConfigurationFacet {

    public void validateStructuredConfiguration(Configuration configuration) {
        // TODO Auto-generated method stub
    }

    public void validateRawConfiguration(RawConfiguration rawConfiguration) throws RuntimeException {
        // TODO Auto-generated method stub

    }

    public void persistStructuredConfiguration(Configuration configuration) {
        // TODO Auto-generated method stub

    }

    public void persistRawConfiguration(RawConfiguration rawConfiguration) {
        // TODO Auto-generated method stub

    }

    public void mergeStructuredConfiguration(RawConfiguration from, Configuration to) {
        // TODO Auto-generated method stub

    }

    public RawConfiguration mergeRawConfiguration(Configuration from, RawConfiguration to) {
        // TODO Auto-generated method stub
        return null;
    }

    public Configuration loadStructuredConfiguration() {
        // TODO Auto-generated method stub
        return null;
    }

    public Set<RawConfiguration> loadRawConfigurations() {
        // TODO Auto-generated method stub
        return null;
    }

    public void start(ResourceContext context) throws InvalidPluginConfigurationException, Exception {
        // TODO Auto-generated method stub

    }

    public void stop() {
        // TODO Auto-generated method stub

    }

    public AvailabilityType getAvailability() {
        // TODO Auto-generated method stub
        return null;
    }
};


