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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import org.hamcrest.Matcher;
import org.jmock.Expectations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.configuration.ConfigurationUpdateRequest;
import org.rhq.core.clientapi.server.configuration.ConfigurationServerService;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.RawConfiguration;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.ServerServices;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pluginapi.configuration.ResourceConfigurationFacet;
import org.rhq.test.jmock.PropertyMatcher;

public class ConfigurationManagerTest extends ConfigManagementTest {

    static final String LEGACY_AMPS_VERSION = "2.0";

    static final String NON_LEGACY_AMPS_VERSION = "2.1";

    boolean FROM_STRUCTURED = true;

    boolean FROM_RAW = false;

    ConfigManagementFactory configMgmtFactory;

    ConfigurationManager configurationMgr;

    @BeforeMethod
    public void setup() {
        configMgmtFactory = context.mock(ConfigManagementFactory.class);

        configurationMgr = new ConfigurationManager();
        configurationMgr.setConfigManagementFactory(configMgmtFactory);
        configurationMgr.setComponentService(componentService);
    }

    @Test
    public void strategyShouldBeCalledToLoadConfig() throws Exception {
        final Configuration expectedConfig = new Configuration();

        final ConfigManagement loadConfig = context.mock(ConfigManagement.class);

        context.checking(new Expectations() {
            {
                atLeast(1).of(configMgmtFactory).getStrategy(resourceId);
                will(returnValue(loadConfig));

                atLeast(1).of(loadConfig).executeLoad(resourceId);
                will(returnValue(expectedConfig));
            }
        });

        Configuration actualConfig = configurationMgr.loadResourceConfiguration(resourceId);

        assertNotNull(actualConfig, "Expected a non-null " + Configuration.class.getSimpleName() + " to be returned.");
    }

    @Test(expectedExceptions = { PluginContainerException.class })
    public void exceptionShouldBeThrownIfConfigIsNull() throws Exception {
        final ConfigManagement loadConfig = context.mock(ConfigManagement.class);

        context.checking(new Expectations() {
            {
                atLeast(1).of(configMgmtFactory).getStrategy(resourceId);
                will(returnValue(loadConfig));

                atLeast(1).of(loadConfig).executeLoad(resourceId);
                will(returnValue(null));

                allowing(componentService).getResourceType(resourceId);
                will(returnValue(new ResourceType()));
            }
        });

        configurationMgr.loadResourceConfiguration(resourceId);
    }

    @Test(expectedExceptions = { PluginContainerException.class })
    public void exceptionShouldBeThrownWhenStrategyThrowsAnException() throws Exception {
        final ConfigManagement loadConfig = context.mock(ConfigManagement.class);

        context.checking(new Expectations() {
            {
                allowing(componentService).getResourceType(resourceId);
                will(returnValue(resourceType));

                atLeast(1).of(configMgmtFactory).getStrategy(resourceId);
                will(returnValue(loadConfig));

                atLeast(1).of(loadConfig).executeLoad(resourceId);
                will(throwException(new RuntimeException()));
            }
        });

        configurationMgr.loadResourceConfiguration(resourceId);
    }

    @Test
    public void mergingStucturedIntoRawShouldUpdateLatestRawConfigs() throws Exception {
        final Configuration config = new Configuration();
        config.addRawConfiguration(createRawConfiguration("/tmp/raw0.txt"));

        final RawConfiguration raw1 = createRawConfiguration("/tmp/raw1.txt");
        config.addRawConfiguration(raw1);

        final RawConfiguration raw2 = createRawConfiguration("/tmp/raw2.txt");

        final Set<RawConfiguration> latestRaws = toSet(raw1, raw2);

        final RawConfiguration mergedRaw1 = createRawConfiguration("/tmp/raw1.txt");
        final RawConfiguration mergedRaw2 = createRawConfiguration("/tmp/raw2.txt");

        final ResourceConfigurationFacet facet = context.mock(ResourceConfigurationFacet.class);

        context.checking(new Expectations() {
            {
                allowing(componentService).getResourceType(resourceId);
                will(returnValue(resourceType));

                atLeast(1).of(componentService).getComponent(resourceId, ResourceConfigurationFacet.class,
                    FacetLockType.READ, ConfigManagement.FACET_METHOD_TIMEOUT, daemonThread, onlyIfStarted);
                will(returnValue(facet));

                atLeast(1).of(facet).loadRawConfigurations();
                will(returnValue(latestRaws));

                oneOf(facet).mergeRawConfiguration(config, raw1);
                will(returnValue(mergedRaw1));
                oneOf(facet).mergeRawConfiguration(config, raw2);
                will(returnValue(mergedRaw2));
            }
        });

        Configuration mergedConfig = configurationMgr.merge(config, resourceId, FROM_STRUCTURED);

        Configuration expectedConfig = new Configuration();
        expectedConfig.addRawConfiguration(mergedRaw1);
        expectedConfig.addRawConfiguration(mergedRaw2);

        assertStructuredMergedIntoRaws(mergedConfig, expectedConfig, "Failed to update existing raw configs");
    }

    @Test
    public void mergingStructuredIntoRawShouldIgnoreNulls() throws Exception {
        final Configuration config = new Configuration();

        final RawConfiguration originalRaw = createRawConfiguration("/tmp/raw.txt");
        config.addRawConfiguration(originalRaw);

        final Set<RawConfiguration> latestRaws = toSet(originalRaw);

        final ResourceConfigurationFacet facet = context.mock(ResourceConfigurationFacet.class);

        context.checking(new Expectations() {
            {
                allowing(componentService).getResourceType(resourceId);
                will(returnValue(resourceType));

                atLeast(1).of(componentService).getComponent(resourceId, ResourceConfigurationFacet.class,
                    FacetLockType.READ, ConfigManagement.FACET_METHOD_TIMEOUT, daemonThread, onlyIfStarted);
                will(returnValue(facet));

                atLeast(1).of(facet).loadRawConfigurations();
                will(returnValue(latestRaws));

                oneOf(facet).mergeRawConfiguration(config, originalRaw);
                will(returnValue(null));
            }
        });

        Configuration mergedConfig = configurationMgr.merge(config, resourceId, FROM_STRUCTURED);

        Configuration expectedConfig = new Configuration();
        expectedConfig.addRawConfiguration(originalRaw);

        assertStructuredMergedIntoRaws(mergedConfig, expectedConfig, "cannot merge into a null raw.");
    }

    @Test
    public void mergingStructuredIntoRawShouldDoNothingIfRawsAreNull() throws Exception {
        RawConfiguration raw = createRawConfiguration("/tmp/foo.txt");

        final Configuration config = new Configuration();
        config.addRawConfiguration(raw);

        final ResourceConfigurationFacet facet = context.mock(ResourceConfigurationFacet.class);

        context.checking(new Expectations() {
            {
                allowing(componentService).getResourceType(resourceId);
                will(returnValue(resourceType));

                atLeast(1).of(componentService).getComponent(resourceId, ResourceConfigurationFacet.class,
                    FacetLockType.READ, ConfigManagement.FACET_METHOD_TIMEOUT, daemonThread, onlyIfStarted);
                will(returnValue(facet));

                atLeast(1).of(facet).loadRawConfigurations();
                will(returnValue(null));
            }
        });

        Configuration mergedConfig = configurationMgr.merge(config, resourceId, FROM_STRUCTURED);

        Configuration expectedConfig = new Configuration();
        expectedConfig.addRawConfiguration(raw);

        assertStructuredMergedIntoRaws(mergedConfig, expectedConfig,
            "Expected raw configs should not be modified when the facet does not return any raw configs.");
    }

    void assertStructuredMergedIntoRaws(Configuration actual, Configuration expected, String msg) {
        Set<RawConfiguration> actualRaws = actual.getRawConfigurations();
        Set<RawConfiguration> expectedRaws = expected.getRawConfigurations();

        assertEquals(actualRaws.size(), expectedRaws.size(),
            "Merging structured into raws failed, got back the wrong number of raws. -- " + msg);
        for (RawConfiguration expectedRaw : expectedRaws) {
            assertTrue(actualRaws.contains(expectedRaw),
                "Merging structured into raws failed, failed to find to find raw config [" + expectedRaw + "] -- "
                    + msg);
        }
    }

    @Test
    public void mergingRawsIntoStructuredShouldUpdateLatestStructuredConfig() throws Exception {
        final Configuration config = new Configuration();
        config.put(new PropertySimple("x", "0"));
        config.put(new PropertySimple("z", "3"));

        final RawConfiguration raw1 = createRawConfiguration("/tmp/foo.txt");
        config.addRawConfiguration(raw1);

        final RawConfiguration raw2 = createRawConfiguration("/tmp/bar.txt");
        config.addRawConfiguration(raw2);

        final Configuration latestStructured = new Configuration();
        latestStructured.put(new PropertySimple("x", "1"));
        latestStructured.put(new PropertySimple("y", "1"));

        final ResourceConfigurationFacet facet = context.mock(ResourceConfigurationFacet.class);

        context.checking(new Expectations() {
            {
                allowing(componentService).getResourceType(resourceId);
                will(returnValue(resourceType));

                atLeast(1).of(componentService).getComponent(resourceId, ResourceConfigurationFacet.class,
                    FacetLockType.READ, ConfigManagement.FACET_METHOD_TIMEOUT, daemonThread, onlyIfStarted);
                will(returnValue(facet));

                atLeast(1).of(facet).loadStructuredConfiguration();
                will(returnValue(latestStructured));

                oneOf(facet).mergeStructuredConfiguration(raw1, config);
                oneOf(facet).mergeStructuredConfiguration(raw2, config);
            }
        });

        Configuration mergedConfig = configurationMgr.merge(config, resourceId, FROM_RAW);

        assertEquals(mergedConfig.getAllProperties(), latestStructured.getAllProperties(),
            "Failed to merged raw into structured.");
    }

    @Test
    public void catchExceptionThrownByFailedValidationOfRawConfigs() throws Exception {

        Configuration configuration = new Configuration();
        final RawConfiguration rawConfiguration = createRawConfiguration("/tmp/foo.txt");
        configuration.addRawConfiguration(rawConfiguration);
        final ResourceConfigurationFacet facet = context.mock(ResourceConfigurationFacet.class);

        context.checking(new Expectations() {
            {
                allowing(componentService).getResourceType(resourceId);
                will(returnValue(resourceType));

                atLeast(1).of(componentService).getComponent(resourceId, ResourceConfigurationFacet.class,
                    FacetLockType.READ, ConfigManagement.FACET_METHOD_TIMEOUT, daemonThread, onlyIfStarted);
                will(returnValue(facet));

                allowing(facet).validateRawConfiguration(rawConfiguration);
                will(throwException(new IllegalArgumentException("message")));

            }
        });
        try {
            configurationMgr.validate(configuration, resourceId, false);
        } catch (PluginContainerException exception) {
            assertTrue(false, "Validate should have caught the exception.");
        }
    }

    @Test
    public void catchExceptionThrownByFailedValidationOfStructuredConfigs() throws Exception {

        final Configuration configuration = new Configuration();
        //final RawConfiguration rawConfiguration = createRawConfiguration("/tmp/foo.txt");
        //configuration.addRawConfiguration(rawConfiguration);
        final ResourceConfigurationFacet facet = context.mock(ResourceConfigurationFacet.class);

        context.checking(new Expectations() {
            {
                allowing(componentService).getResourceType(resourceId);
                will(returnValue(resourceType));

                atLeast(1).of(componentService).getComponent(resourceId, ResourceConfigurationFacet.class,
                    FacetLockType.READ, ConfigManagement.FACET_METHOD_TIMEOUT, daemonThread, onlyIfStarted);
                will(returnValue(facet));

                //allowing(facet).validateRawConfiguration(rawConfiguration);
                allowing(facet).validateStructuredConfiguration(configuration);
                will(throwException(new IllegalArgumentException("message")));
            }
        });
        try {
            configurationMgr.validate(configuration, resourceId, true);
        } catch (PluginContainerException exception) {
            assertTrue(false, "validate should have caught exception");
        }
    }

    @Test
    public void mergingRawsIntoStructuredShouldIgnoreNull() throws Exception {
        Configuration config = new Configuration();
        config.addRawConfiguration(createRawConfiguration("/tmp/foo.txt"));

        final ResourceConfigurationFacet facet = context.mock(ResourceConfigurationFacet.class);

        context.checking(new Expectations() {
            {
                allowing(componentService).getResourceType(resourceId);
                will(returnValue(resourceType));

                atLeast(1).of(componentService).getComponent(resourceId, ResourceConfigurationFacet.class,
                    FacetLockType.READ, ConfigManagement.FACET_METHOD_TIMEOUT, daemonThread, onlyIfStarted);
                will(returnValue(facet));

                atLeast(1).of(facet).loadStructuredConfiguration();
                will(returnValue(null));
            }
        });

        Configuration mergedConfig = configurationMgr.merge(config, resourceId, FROM_RAW);

        assertEquals(mergedConfig, config,
            "Structured config should not be modified when the facet does not return a structured config.");
    }

    @Test
    public void updatingConfigShouldSubmitRunnerToThreadPool() throws Exception {
        int configUpdateId = -1;

        Configuration config = new Configuration();

        ConfigurationUpdateRequest request = new ConfigurationUpdateRequest(configUpdateId, config, resourceId);

        final ScheduledExecutorService threadPool = context.mock(ScheduledExecutorService.class, "threadPool");
        configurationMgr.setThreadPool(threadPool);

        final ConfigManagement configMgmt = context.mock(ConfigManagement.class);

        final ConfigurationServerService configServerService = context.mock(ConfigurationServerService.class);

        PluginContainerConfiguration containerConfig = new PluginContainerConfiguration();
        ServerServices serverServices = new ServerServices();
        serverServices.setConfigurationServerService(configServerService);

        configurationMgr.setConfiguration(containerConfig);

        final UpdateResourceConfigurationRunner updateRunner = new UpdateResourceConfigurationRunner(
            configServerService, resourceType, configMgmt, request);

        context.checking(new Expectations() {
            {
                allowing(componentService).getResourceType(resourceId);
                will(returnValue(resourceType));

                atLeast(1).of(configMgmtFactory).getStrategy(resourceId);
                will(returnValue(configMgmt));

                oneOf(threadPool).submit((Runnable) with(matchingUpdateRunner(updateRunner)));
            }
        });

        configurationMgr.updateResourceConfiguration(request);
    }

    public static Matcher<UpdateResourceConfigurationRunner> matchingUpdateRunner(
        UpdateResourceConfigurationRunner expected) {
        return new PropertyMatcher<UpdateResourceConfigurationRunner>(expected);
    }

}
