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

public class ConfigurationManagerUnitTest extends JMockTest {

    static final String LEGACY_AMPS_VERSION = "2.0";

    static final String NON_LEGACY_AMPS_VERSION = "2.1";

    boolean FROM_STRUCTURED = true;

    boolean FROM_RAW = false;

    int resourceId = -1;

    boolean daemonThread = true;

    boolean onlyIfStarted = true;

    LoadResourceConfigurationFactory loadConfigFactory;

    ComponentService componentService;

    ConfigurationManager configurationMgr;

    Random random;

    @BeforeMethod
    public void setup() {
        loadConfigFactory = context.mock(LoadResourceConfigurationFactory.class);

        componentService = context.mock(ComponentService.class);

        configurationMgr = new ConfigurationManager();
        configurationMgr.setLoadConfigFactory(loadConfigFactory);
        configurationMgr.setComponentService(componentService);

        random = new Random();
    }

    @Test
    public void factoryShouldBeCalledToGetStrategy() throws Exception {
        final LoadResourceConfiguration loadConfig = context.mock(LoadResourceConfiguration.class);
        
        context.checking(new Expectations() {{
            atLeast(1).of(loadConfigFactory).getStrategy(resourceId); will(returnValue(loadConfig));

            allowing(loadConfig).execute(resourceId); will(returnValue(new Configuration()));
        }});

        configurationMgr.loadResourceConfiguration(resourceId);
    }

    @Test
    public void strategyShouldBeCalledToLoadConfig() throws Exception {
        final Configuration expectedConfig = new Configuration();

        final LoadResourceConfiguration loadConfig = context.mock(LoadResourceConfiguration.class);

        context.checking(new Expectations() {{
            allowing(loadConfigFactory).getStrategy(resourceId); will(returnValue(loadConfig));

            atLeast(1).of(loadConfig).execute(resourceId);
            will(returnValue(expectedConfig));
        }});

        Configuration actualConfig = configurationMgr.loadResourceConfiguration(resourceId);

        assertSame(actualConfig, expectedConfig, "Expected the configuration from the loadConfig object to be returned.");
    }

    @Test(expectedExceptions = {PluginContainerException.class})
    public void exceptionShouldBeThrownIfConfigIsNull() throws Exception {
        final LoadResourceConfiguration loadConfig = context.mock(LoadResourceConfiguration.class);

        context.checking(new Expectations() {{
            allowing(loadConfigFactory).getStrategy(resourceId); will(returnValue(loadConfig));

            allowing(loadConfig).execute(resourceId); will(returnValue(null));

            allowing(componentService).getResourceType(resourceId); will(returnValue(new ResourceType()));
        }});

        configurationMgr.loadResourceConfiguration(resourceId);
    }

    @Test
    public void theFacetShouldMergeTheStructuredIntoEachRawWhenFromStructured() throws Exception {
        final RawConfiguration rawConfig1 = createRawConfiguration("/tmp/raw1.txt");
        final RawConfiguration rawConfig2 = createRawConfiguration("/tmp/raw2.txt");
        final RawConfiguration rawConfig3 = createRawConfiguration("/tmp/raw3.txt");

        final Configuration configuration = new Configuration();
        configuration.addRawConfiguration(rawConfig1);
        configuration.addRawConfiguration(rawConfig2);
        configuration.addRawConfiguration(rawConfig3);

        final ResourceConfigurationFacet facet = context.mock(ResourceConfigurationFacet.class);

        final Sequence sequence = context.sequence("merge");

        context.checking(new Expectations(){{
            atLeast(1).of(componentService).getComponent(resourceId,
                                                         ResourceConfigurationFacet.class,
                                                         FacetLockType.READ,
                                                         LoadResourceConfiguration.FACET_METHOD_TIMEOUT,
                                                         daemonThread,
                                                         onlyIfStarted);
            will(returnValue(facet));

//            atLeast(1).of(facet).load

            oneOf(facet).mergeRawConfiguration(configuration, rawConfig1);
            oneOf(facet).mergeRawConfiguration(configuration, rawConfig2);
            oneOf(facet).mergeRawConfiguration(configuration, rawConfig3);
        }});

        Configuration actualConfig = configurationMgr.merge(configuration, resourceId, FROM_STRUCTURED);

        assertNotNull(actualConfig, "Expected a non-null " + Configuration.class.getSimpleName() + " to be returned.");
    }

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
