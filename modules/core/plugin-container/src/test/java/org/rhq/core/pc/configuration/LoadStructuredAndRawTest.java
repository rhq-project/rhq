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

import org.rhq.core.pc.util.ComponentService;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pluginapi.configuration.ResourceConfigurationFacet;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.RawConfiguration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.resource.ResourceType;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.jmock.Expectations;

import java.util.Random;

public class LoadStructuredAndRawTest extends JMockTest {

    static final boolean FROM_STRUCTURED = true;

    static final boolean FROM_RAW = false;

    ComponentService componentService;

    ConfigurationUtilityService configUtilityService;

    ResourceConfigurationFacet configFacet;

    int resourceId = -1;

    boolean daemonThread = true;

    boolean onlyIfStarted = true;

    LoadStructuredAndRaw loadStructuredAndRaw;

    Random random = new Random();

    @BeforeMethod
    public void setup() {
        componentService = context.mock(ComponentService.class);
        configUtilityService = context.mock(ConfigurationUtilityService.class);

        configFacet = context.mock(ResourceConfigurationFacet.class);

        loadStructuredAndRaw = new LoadStructuredAndRaw();
        loadStructuredAndRaw.setComponentService(componentService);
        loadStructuredAndRaw.setConfigurationUtilityService(configUtilityService);
    }

    @Test
    public void theResourceConfigFacetShouldGetLoaded() throws Exception {
        context.checking(new Expectations() {{
            atLeast(1).of(componentService).getComponent(resourceId,
                                                         ResourceConfigurationFacet.class,
                                                         FacetLockType.READ,
                                                         LoadResourceConfiguration.FACET_METHOD_TIMEOUT,
                                                         daemonThread,
                                                         onlyIfStarted);
            will(returnValue(configFacet));

            allowing(configFacet).loadStructuredConfiguration();
            
            ignoring(configUtilityService);
        }});

        loadStructuredAndRaw.execute(resourceId, FROM_STRUCTURED);
    }

    @Test
    public void theStructuredConfigShouldGetLoadedWhenFromStructured() throws Exception {
        context.checking(new Expectations() {{
            allowing(componentService).getComponent(resourceId,
                                                    ResourceConfigurationFacet.class,
                                                    FacetLockType.READ,
                                                    LoadResourceConfiguration.FACET_METHOD_TIMEOUT,
                                                    daemonThread,
                                                    onlyIfStarted);
            will(returnValue(configFacet));

            allowing(configFacet).loadStructuredConfiguration();

            ignoring(configUtilityService);
        }});

        loadStructuredAndRaw.execute(resourceId, FROM_STRUCTURED);
    }

    @Test
    public void eachRawConfigShouldBeMergedIntoTheStructuredConfigWhenFromStructured() throws Exception {
        final RawConfiguration rawConfig1 = createRawConfiguration("/tmp/foo.txt");
        final RawConfiguration rawConfig2 = createRawConfiguration("/tmp/bar.txt");
        final RawConfiguration rawConfig3 = createRawConfiguration("/tmp/bam.txt");

        final Configuration configuration = new Configuration();
        configuration.addRawConfiguration(rawConfig1);
        configuration.addRawConfiguration(rawConfig2);
        configuration.addRawConfiguration(rawConfig3);

        final ResourceType resourceType = new ResourceType();

        context.checking(new Expectations() {{
            allowing(componentService).getComponent(resourceId,
                                                    ResourceConfigurationFacet.class,
                                                    FacetLockType.READ,
                                                    LoadResourceConfiguration.FACET_METHOD_TIMEOUT,
                                                    daemonThread,
                                                    onlyIfStarted);
            will(returnValue(configFacet));

            allowing(componentService).getResourceType(resourceId); will(returnValue(resourceType));

            atLeast(1).of(configFacet).loadStructuredConfiguration(); will(returnValue(configuration));

            oneOf(configFacet).mergeStructuredConfiguration(rawConfig1, configuration);
            oneOf(configFacet).mergeStructuredConfiguration(rawConfig2, configuration);
            oneOf(configFacet).mergeStructuredConfiguration(rawConfig3, configuration);

            ignoring(configUtilityService);
        }});

        loadStructuredAndRaw.execute(resourceId, FROM_STRUCTURED);
    }

    @Test
    public void theRawConfigShouldGetLoadedWhenFromRaw() throws Exception {
        context.checking(new Expectations() {{
            allowing(componentService).getComponent(resourceId,
                                                    ResourceConfigurationFacet.class,
                                                    FacetLockType.READ,
                                                    LoadResourceConfiguration.FACET_METHOD_TIMEOUT,
                                                    daemonThread,
                                                    onlyIfStarted);
            will(returnValue(configFacet));

            allowing(componentService).getResourceType(resourceId); will(returnValue(new ResourceType()));

            atLeast(1).of(configFacet).loadRawConfigurations(); will(returnValue(new Configuration()));

            ignoring(configUtilityService);
        }});

        loadStructuredAndRaw.execute(resourceId, FROM_RAW);
    }

    @Test
    public void theStructuredConfigShouldBeMergedIntoEachRawConfigWhenFromRaw() throws Exception {
        final RawConfiguration rawConfig1 = createRawConfiguration("/tmp/foo.txt");
        final RawConfiguration rawConfig2 = createRawConfiguration("/tmp/bar.txt");
        final RawConfiguration rawConfig3 = createRawConfiguration("/tmp/bam.txt");

        final Configuration configuration = new Configuration();
        configuration.addRawConfiguration(rawConfig1);
        configuration.addRawConfiguration(rawConfig2);
        configuration.addRawConfiguration(rawConfig3);

        context.checking(new Expectations() {{
            allowing(componentService).getComponent(resourceId,
                                                    ResourceConfigurationFacet.class,
                                                    FacetLockType.READ,
                                                    LoadResourceConfiguration.FACET_METHOD_TIMEOUT,
                                                    daemonThread,
                                                    onlyIfStarted);
            will(returnValue(configFacet));

            allowing(componentService).getResourceType(resourceId); will(returnValue(new ResourceType()));

            allowing(configFacet).loadRawConfigurations(); will(returnValue(configuration));

            oneOf(configFacet).mergeRawConfiguration(configuration, rawConfig1);
            oneOf(configFacet).mergeRawConfiguration(configuration, rawConfig2);
            oneOf(configFacet).mergeRawConfiguration(configuration, rawConfig3);

            ignoring(configUtilityService);
        }});

        loadStructuredAndRaw.execute(resourceId, FROM_RAW);
    }

    @Test
    public void theConfigReceivedFromTheFacetShouldBeReturned() throws Exception {
        final Configuration configuration = new Configuration();

        final ResourceType resourceType = new ResourceType();
        resourceType.setResourceConfigurationDefinition(new ConfigurationDefinition("", ""));

        context.checking(new Expectations() {{
            allowing(componentService).getComponent(resourceId,
                                                    ResourceConfigurationFacet.class,
                                                    FacetLockType.READ,
                                                    LoadResourceConfiguration.FACET_METHOD_TIMEOUT,
                                                    daemonThread,
                                                    onlyIfStarted);
            will(returnValue(configFacet));

            allowing(componentService).getResourceType(resourceId); will(returnValue(resourceType));

            allowing(configFacet).loadStructuredConfiguration(); will(returnValue(configuration));

            ignoring(configUtilityService);
        }});

        Configuration loadedConfig = loadStructuredAndRaw.execute(resourceId, FROM_STRUCTURED);

        assertSame(
            loadedConfig,
            configuration,
            "The loadRawConfig should return the configuration it receives from the facet component."
        );
    }

    @Test
    public void theConfigNotesShouldGetSetIfItIsNotAlreadySet() throws Exception {
        final Configuration configuration = new Configuration();

        final ResourceType resourceType = new ResourceType();
        resourceType.setName("test resource");

        context.checking(new Expectations() {{
            allowing(componentService).getComponent(resourceId,
                                                    ResourceConfigurationFacet.class,
                                                    FacetLockType.READ,
                                                    LoadResourceConfiguration.FACET_METHOD_TIMEOUT,
                                                    daemonThread,
                                                    onlyIfStarted);
            will(returnValue(configFacet));

            allowing(componentService).getResourceType(resourceId); will(returnValue(resourceType));

            allowing(configFacet).loadStructuredConfiguration(); will(returnValue(configuration));

            ignoring(configUtilityService);
        }});

        Configuration loadedConfig = loadStructuredAndRaw.execute(resourceId, FROM_STRUCTURED);

        String expectedNotes = "Resource config for " + resourceType.getName() + " Resource w/ id " + resourceId;

        assertEquals(loadedConfig.getNotes(), expectedNotes, "The notes property should be set to a default when it is not already initialized.");
    }

    @Test
    public void theConfigShouldGetNormalized() throws Exception {
        final Configuration configuration = new Configuration();

        final ResourceType resourceType = new ResourceType();
        resourceType.setResourceConfigurationDefinition(new ConfigurationDefinition("", ""));

        context.checking(new Expectations() {{
            allowing(componentService).getComponent(resourceId,
                                                    ResourceConfigurationFacet.class,
                                                    FacetLockType.READ,
                                                    LoadResourceConfiguration.FACET_METHOD_TIMEOUT,
                                                    daemonThread,
                                                    onlyIfStarted);
            will(returnValue(configFacet));

            allowing(componentService).getResourceType(resourceId); will(returnValue(resourceType));

            allowing(configFacet).loadStructuredConfiguration(); will(returnValue(configuration));

            atLeast(1).of(configUtilityService).normalizeConfiguration(configuration, resourceType.getResourceConfigurationDefinition());

            allowing(configUtilityService).validateConfiguration(configuration, resourceType.getResourceConfigurationDefinition());
        }});

        loadStructuredAndRaw.execute(resourceId, FROM_STRUCTURED);
    }

    @Test
    public void theConfigShouldGetValidated() throws Exception {
        final Configuration configuration = new Configuration();

        final ResourceType resourceType = new ResourceType();
        resourceType.setResourceConfigurationDefinition(new ConfigurationDefinition("", ""));

        context.checking(new Expectations() {{
            allowing(componentService).getComponent(resourceId,
                                                    ResourceConfigurationFacet.class,
                                                    FacetLockType.READ,
                                                    LoadResourceConfiguration.FACET_METHOD_TIMEOUT,
                                                    daemonThread,
                                                    onlyIfStarted);
            will(returnValue(configFacet));

            allowing(componentService).getResourceType(resourceId); will(returnValue(resourceType));

            allowing(configFacet).loadStructuredConfiguration(); will(returnValue(configuration));

            allowing(configUtilityService).normalizeConfiguration(configuration, resourceType.getResourceConfigurationDefinition());

            atLeast(1).of(configUtilityService).validateConfiguration(configuration, resourceType.getResourceConfigurationDefinition());
        }});

        loadStructuredAndRaw.execute(resourceId, FROM_STRUCTURED);
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
