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
import static java.util.Collections.EMPTY_SET;

import org.jmock.Expectations;
import org.jmock.Sequence;
import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.RawConfiguration;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.util.ComponentService;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pluginapi.configuration.ResourceConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Random;
import java.util.Set;
import java.util.HashSet;

public class ConfigurationManagerUnitTest extends LoadConfigTest {

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
    public void mergingStucturedIntoRawShouldUpdateConfigWithModifiedRaws() throws Exception {
        final Configuration config = new Configuration();

        final RawConfiguration originalRaw1 = createRawConfiguration("/tmp/raw1.txt");
        config.addRawConfiguration(originalRaw1);

        final RawConfiguration originalRaw2 = createRawConfiguration("/tmp/raw2.txt");
        config.addRawConfiguration(originalRaw2);

        final RawConfiguration originalRaw3 = createRawConfiguration("/tmp/raw3.txt");
        config.addRawConfiguration(originalRaw3);

        final RawConfiguration mergedRaw1 = createRawConfiguration("/tmp/raw1.txt");
        final RawConfiguration mergedRaw2 = createRawConfiguration("/tmp/raw2.txt");
        final RawConfiguration mergedRaw3 = createRawConfiguration("/tmp/raw3.txt");

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

            atLeast(1).of(facet).loadRawConfigurations(); will(returnValue(config.getRawConfigurations()));

            oneOf(facet).mergeRawConfiguration(config, originalRaw1); will(returnValue(mergedRaw1));
            oneOf(facet).mergeRawConfiguration(config, originalRaw2); will(returnValue(mergedRaw2));
            oneOf(facet).mergeRawConfiguration(config, originalRaw3); will(returnValue(mergedRaw3));
        }});

        Configuration mergedConfig = configurationMgr.merge(config, resourceId, FROM_STRUCTURED);

        Set<RawConfiguration> mergedRaws = toSet(mergedRaw1, mergedRaw2, mergedRaw3);

        assertEquals(mergedConfig.getRawConfigurations(), mergedRaws, "Failed to merge structured into raws.");
    }

    @Test
    public void mergingStructuredIntoRawShouldIgnoreNulls() throws Exception {
        final Configuration config = new Configuration();

        final RawConfiguration originalRaw = createRawConfiguration("/tmp/raw.txt");
        config.addRawConfiguration(originalRaw);

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

            atLeast(1).of(facet).loadRawConfigurations(); will(returnValue(config.getRawConfigurations()));

            oneOf(facet).mergeRawConfiguration(config, originalRaw); will(returnValue(null));
        }});

        Configuration mergedConfig = configurationMgr.merge(config, resourceId, FROM_STRUCTURED);

        assertEquals(
            mergedConfig.getRawConfigurations(),
            config.getRawConfigurations(),
            "Expected the raw configs to be unmodified when merge operation returns null."
        );
    }

    @Test
    public void mergingStructuredIntoRawShouldDoNothingIfRawsAreNull() throws Exception {
        final Configuration config = new Configuration();
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

            atLeast(1).of(facet).loadRawConfigurations(); will(returnValue(null));
        }});

        Configuration mergedConfig = configurationMgr.merge(config, resourceId, FROM_STRUCTURED);

        assertEquals(
            mergedConfig.getRawConfigurations(),
            config.getRawConfigurations(),
            "Expected the raw configs to be unmodifed when the facet does not return any raw configs."
        );
    }

//    @Test
//    public void theFacetShouldMergeTheStructuredIntoEachRawWhenFromStructured() throws Exception {
//        final RawConfiguration rawConfig1 = createRawConfiguration("/tmp/raw1.txt");
//        final RawConfiguration rawConfig2 = createRawConfiguration("/tmp/raw2.txt");
//        final RawConfiguration rawConfig3 = createRawConfiguration("/tmp/raw3.txt");
//
//        final Set<RawConfiguration> rawConfigs = new HashSet<RawConfiguration>();
//        rawConfigs.add(rawConfig1);
//        rawConfigs.add(rawConfig2);
//        rawConfigs.add(rawConfig3);
//
//        final Configuration configuration = new Configuration();
//        configuration.addRawConfiguration(rawConfig1);
//        configuration.addRawConfiguration(rawConfig2);
//        configuration.addRawConfiguration(rawConfig3);
//
//        final ResourceConfigurationFacet facet = context.mock(ResourceConfigurationFacet.class);
//
//        final Sequence merge = context.sequence("merge");
//
//        context.checking(new Expectations(){{
//            atLeast(1).of(componentService).getComponent(resourceId,
//                                                         ResourceConfigurationFacet.class,
//                                                         FacetLockType.READ,
//                                                         LoadResourceConfiguration.FACET_METHOD_TIMEOUT,
//                                                         daemonThread,
//                                                         onlyIfStarted);
//            will(returnValue(facet));
//
//            atLeast(1).of(facet).loadRawConfigurations(); will(returnValue(rawConfigs));
//
//            oneOf(facet).mergeRawConfiguration(configuration, rawConfig1);
//            oneOf(facet).mergeRawConfiguration(configuration, rawConfig2);
//            oneOf(facet).mergeRawConfiguration(configuration, rawConfig3);
//        }});
//
//        Configuration actualConfig = configurationMgr.merge(configuration, resourceId, FROM_STRUCTURED);
//    }

    @Test
    public void theFacetShouldMergeEachRawIntoTheStructuredWhenFromRaw() throws Exception {
        final RawConfiguration rawConfig1 = createRawConfiguration("/tmp/raw1.txt");
        final RawConfiguration rawConfig2 = createRawConfiguration("/tmp/raw2.txt");
        final RawConfiguration rawConfig3 = createRawConfiguration("/tmp/raw3.txt");

        final Configuration configuration = new Configuration();
        configuration.addRawConfiguration(rawConfig1);
        configuration.addRawConfiguration(rawConfig2);
        configuration.addRawConfiguration(rawConfig3);

        final ResourceConfigurationFacet facet = context.mock(ResourceConfigurationFacet.class);

        context.checking(new Expectations(){{
            atLeast(1).of(componentService).getComponent(resourceId,
                                                         ResourceConfigurationFacet.class,
                                                         FacetLockType.READ,
                                                         LoadResourceConfiguration.FACET_METHOD_TIMEOUT,
                                                         daemonThread,
                                                         onlyIfStarted);
            will(returnValue(facet));

            oneOf(facet).mergeStructuredConfiguration(rawConfig1, configuration);
            oneOf(facet).mergeStructuredConfiguration(rawConfig2, configuration);
            oneOf(facet).mergeStructuredConfiguration(rawConfig3, configuration);
        }});

        Configuration actualConfig = configurationMgr.merge(configuration, resourceId, FROM_RAW);

        assertNotNull(actualConfig, "Expected a non-null " + Configuration.class.getSimpleName() + " to be returned.");
    }

//    @Test
//    public void theUpdateLegacyConfigCmdShouldBeCalledToUpdateLegacyConfig() throws Exception {
//        UpdateLegacyConfig updateLegacyConfig = context.mock(UpdateLegacyConfig.class);
//
//        context.checking(new Expectations() {{
//            allowing(componentService).getAmpsVersion(resourceId); will(returnValue(LEGACY_AMPS_VERSION));
//
//            //oneOf()
//        }});
//    }

}
