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
import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.util.ComponentService;
import static org.testng.Assert.assertSame;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ConfigurationManagerUnitTest extends JMockTest {

    static final String LEGACY_AMPS_VERSION = "2.0";

    static final String NON_LEGACY_AMPS_VERSION = "2.1";

    static final boolean FROM_STRUCTURED = true;

    static final boolean FROM_RAW = false;

    int resourceId = -1;

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
    public void factoryShouldBeCalledToGetStrategy() throws Exception {
        final LoadResourceConfiguration loadConfig = context.mock(LoadResourceConfiguration.class);
        
        context.checking(new Expectations() {{
            atLeast(1).of(loadConfigFactory).getStrategy(resourceId); will(returnValue(loadConfig));

            allowing(loadConfig).execute(resourceId, FROM_STRUCTURED); will(returnValue(new Configuration()));
        }});

        configurationMgr.loadResourceConfiguration(resourceId, true);
    }

    @Test
    public void strategyShouldBeCalledToLoadConfig() throws Exception {
        final Configuration expectedConfig = new Configuration();

        final LoadResourceConfiguration loadConfig = context.mock(LoadResourceConfiguration.class);

        context.checking(new Expectations() {{
            allowing(loadConfigFactory).getStrategy(resourceId); will(returnValue(loadConfig));

            atLeast(1).of(loadConfig).execute(resourceId, FROM_STRUCTURED);
            will(returnValue(expectedConfig));
        }});

        Configuration actualConfig = configurationMgr.loadResourceConfiguration(resourceId, FROM_STRUCTURED);

        assertSame(actualConfig, expectedConfig, "Expected the configuration from the loadConfig object to be returned.");
    }

    @Test(expectedExceptions = {PluginContainerException.class})
    public void exceptionShouldBeThrownIfConfigIsNull() throws Exception {
        final LoadResourceConfiguration loadConfig = context.mock(LoadResourceConfiguration.class);

        context.checking(new Expectations() {{
            allowing(loadConfigFactory).getStrategy(resourceId); will(returnValue(loadConfig));

            allowing(loadConfig).execute(resourceId, FROM_STRUCTURED); will(returnValue(null));

            allowing(componentService).getResourceType(resourceId); will(returnValue(new ResourceType()));
        }});

        configurationMgr.loadResourceConfiguration(resourceId, FROM_STRUCTURED);
    }

}
