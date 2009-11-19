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

import org.jmock.Expectations;
import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.RawConfiguration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.util.ComponentService;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pluginapi.configuration.ResourceConfigurationFacet;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Set;

public class ConfigurationManagerUnitTest extends ConfigManagementTest {

    static final String LEGACY_AMPS_VERSION = "2.0";

    static final String NON_LEGACY_AMPS_VERSION = "2.1";

    boolean FROM_STRUCTURED = true;

    boolean FROM_RAW = false;

    LoadResourceConfigurationFactory loadConfigFactory;

    ComponentService componentService;

    ConfigurationManager configurationMgr;

    @BeforeMethod
    public void setup() {
        loadConfigFactory = context.mock(LoadResourceConfigurationFactory.class);

        componentService = context.mock(ComponentService.class);

        configurationMgr = new ConfigurationManager();
        configurationMgr.setLoadConfigFactory(loadConfigFactory);
        configurationMgr.setComponentService(componentService);
    }

    @Test
    public void straegyShouldBeCalledToLoadConfig() throws Exception {
        final Configuration expectedConfig = new Configuration();

        final LoadResourceConfiguration loadConfig = context.mock(LoadResourceConfiguration.class);

        context.checking(new Expectations() {{
            atLeast(1).of(loadConfigFactory).getStrategy(resourceId); will(returnValue(loadConfig));

            atLeast(1).of(loadConfig).execute(resourceId); will(returnValue(expectedConfig));
        }});

        Configuration actualConfig = configurationMgr.loadResourceConfiguration(resourceId);

        assertNotNull(actualConfig, "Expected a non-null " + Configuration.class.getSimpleName() + " to be returned.");
    }

    @Test(expectedExceptions = {PluginContainerException.class})
    public void exceptionShouldBeThrownIfConfigIsNull() throws Exception {
        final LoadResourceConfiguration loadConfig = context.mock(LoadResourceConfiguration.class);

        context.checking(new Expectations() {{
            atLeast(1).of(loadConfigFactory).getStrategy(resourceId); will(returnValue(loadConfig));

            atLeast(1).of(loadConfig).execute(resourceId); will(returnValue(null));

            allowing(componentService).getResourceType(resourceId); will(returnValue(new ResourceType()));
        }});

        configurationMgr.loadResourceConfiguration(resourceId);
    }

    @Test(expectedExceptions = {PluginContainerException.class})
    public void exceptionShouldBeThrownWhenStrategyThrowsAnException() throws Exception {
        final LoadResourceConfiguration loadConfig = context.mock(LoadResourceConfiguration.class);

        context.checking(new Expectations() {{
            allowing(componentService).getResourceType(resourceId); will(returnValue(resourceType));

            atLeast(1).of(loadConfigFactory).getStrategy(resourceId); will(returnValue(loadConfig));

            atLeast(1).of(loadConfig).execute(resourceId); will(throwException(new RuntimeException()));
        }});

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

        context.checking(new Expectations() {{
            allowing(componentService).getResourceType(resourceId); will(returnValue(resourceType));

            atLeast(1).of(componentService).getComponent(resourceId,
                                                         ResourceConfigurationFacet.class,
                                                         FacetLockType.READ,
                                                         LoadResourceConfiguration.FACET_METHOD_TIMEOUT,
                                                         daemonThread,
                                                         onlyIfStarted);
            will(returnValue(facet));

            atLeast(1).of(facet).loadRawConfigurations(); will(returnValue(latestRaws));

            oneOf(facet).mergeRawConfiguration(config, raw1); will(returnValue(mergedRaw1));
            oneOf(facet).mergeRawConfiguration(config, raw2); will(returnValue(mergedRaw2));
        }});

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

        context.checking(new Expectations() {{
            allowing(componentService).getResourceType(resourceId); will(returnValue(resourceType));

            atLeast(1).of(componentService).getComponent(resourceId,
                                                         ResourceConfigurationFacet.class,
                                                         FacetLockType.READ,
                                                         LoadResourceConfiguration.FACET_METHOD_TIMEOUT,
                                                         daemonThread,
                                                         onlyIfStarted);
            will(returnValue(facet));

            atLeast(1).of(facet).loadRawConfigurations(); will(returnValue(latestRaws));

            oneOf(facet).mergeRawConfiguration(config, originalRaw); will(returnValue(null));
        }});

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

        context.checking(new Expectations() {{
            allowing(componentService).getResourceType(resourceId); will(returnValue(resourceType));

            atLeast(1).of(componentService).getComponent(resourceId,
                                                         ResourceConfigurationFacet.class,
                                                         FacetLockType.READ,
                                                         LoadResourceConfiguration.FACET_METHOD_TIMEOUT,
                                                         daemonThread,
                                                         onlyIfStarted);
            will(returnValue(facet));

            atLeast(1).of(facet).loadRawConfigurations(); will(returnValue(null));
        }});

        Configuration mergedConfig = configurationMgr.merge(config, resourceId, FROM_STRUCTURED);

        Configuration expectedConfig = new Configuration();
        expectedConfig.addRawConfiguration(raw);

        assertStructuredMergedIntoRaws(
            mergedConfig,
            expectedConfig,
            "Expected raw configs should not be modified when the facet does not return any raw configs."
        );
    }

    void assertStructuredMergedIntoRaws(Configuration actual, Configuration expected, String msg) {
        Set<RawConfiguration> actualRaws = actual.getRawConfigurations();
        Set<RawConfiguration> expectedRaws = expected.getRawConfigurations();

        assertEquals(
            actualRaws.size(),
            expectedRaws.size(),
            "Merging structured into raws failed, got back the wrong number of raws. -- " + msg
        );
        for (RawConfiguration expectedRaw : expectedRaws) {
            assertTrue(
                actualRaws.contains(expectedRaw),
                "Merging structured into raws failed, failed to find to find raw config [" + expectedRaw + "] -- " + msg
            );
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

        context.checking(new Expectations() {{
            allowing(componentService).getResourceType(resourceId); will(returnValue(resourceType));

            atLeast(1).of(componentService).getComponent(resourceId,
                                                         ResourceConfigurationFacet.class,
                                                         FacetLockType.READ,
                                                         LoadResourceConfiguration.FACET_METHOD_TIMEOUT,
                                                         daemonThread,
                                                         onlyIfStarted);
            will(returnValue(facet));

            atLeast(1).of(facet).loadStructuredConfiguration(); will(returnValue(latestStructured));

            oneOf(facet).mergeStructuredConfiguration(raw1, config);
            oneOf(facet).mergeStructuredConfiguration(raw2, config);                         
        }});

        Configuration mergedConfig = configurationMgr.merge(config, resourceId, FROM_RAW);

        assertEquals(
            mergedConfig.getAllProperties(),
            latestStructured.getAllProperties(),
            "Failed to merged raw into structured."
        );
    }

    @Test
    public void mergingRawsIntoStructuredShouldIgnoreNull() throws Exception {
        Configuration config = new Configuration();
        config.addRawConfiguration(createRawConfiguration("/tmp/foo.txt"));

        final ResourceConfigurationFacet facet = context.mock(ResourceConfigurationFacet.class);

        context.checking(new Expectations() {{
            allowing(componentService).getResourceType(resourceId); will(returnValue(resourceType));

            atLeast(1).of(componentService).getComponent(resourceId,
                                                         ResourceConfigurationFacet.class,
                                                         FacetLockType.READ,
                                                         LoadResourceConfiguration.FACET_METHOD_TIMEOUT,
                                                         daemonThread,
                                                         onlyIfStarted);
            will(returnValue(facet));

            atLeast(1).of(facet).loadStructuredConfiguration(); will(returnValue(null));
        }});

        Configuration mergedConfig = configurationMgr.merge(config, resourceId, FROM_RAW);

        assertEquals(
            mergedConfig,
            config,
            "Structured config should not be modified when the facet does not return a structured config."
        );
    }

}
