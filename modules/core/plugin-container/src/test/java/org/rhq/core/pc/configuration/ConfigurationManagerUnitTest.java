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
import org.rhq.core.pc.inventory.InventoryService;
import static org.testng.Assert.assertSame;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ConfigurationManagerUnitTest extends JMockTest {

    static final String LEGACY_AMPS_VERSION = "2.0";

    static final String NON_LEGACY_AMPS_VERSION = "2.1";

    static final boolean FROM_STRUCTURED = true;

    static final boolean FROM_RAW = false;

    int resourceId = -1;

    ResourceConfigurationStrategyFactory factory;

    InventoryService componentService;

    ConfigurationManager configurationMgr;

    @BeforeMethod
    public void setup() {
        factory = context.mock(ResourceConfigurationStrategyFactory.class);

        componentService = context.mock(InventoryService.class);

        configurationMgr = new ConfigurationManager();
        configurationMgr.setLoadConfigStrategyFactory(factory);
        configurationMgr.setInventoryService(componentService);
    }

    @Test
    public void factoryShouldBeCalledToGetStrategy() throws Exception {
        final ResourceConfigurationStrategy strategy = context.mock(ResourceConfigurationStrategy.class);
        
        context.checking(new Expectations() {{
            atLeast(1).of(factory).getStrategy(resourceId); will(returnValue(strategy));

            allowing(strategy).loadConfiguration(resourceId, FROM_STRUCTURED); will(returnValue(new Configuration()));
        }});

        configurationMgr.loadResourceConfiguration(resourceId, true);
    }

    @Test
    public void strategyShouldBeCalledToLoadConfig() throws Exception {
        final Configuration expectedConfig = new Configuration();

        final ResourceConfigurationStrategy strategy = context.mock(ResourceConfigurationStrategy.class);

        context.checking(new Expectations() {{
            allowing(factory).getStrategy(resourceId); will(returnValue(strategy));

            atLeast(1).of(strategy).loadConfiguration(resourceId, FROM_STRUCTURED);
            will(returnValue(expectedConfig));
        }});

        Configuration actualConfig = configurationMgr.loadResourceConfiguration(resourceId, FROM_STRUCTURED);

        assertSame(actualConfig, expectedConfig, "Expected the configuration from the strategy object to be returned.");
    }

    @Test(expectedExceptions = {PluginContainerException.class})
    public void exceptionShouldBeThrownIfConfigIsNull() throws Exception {
        final ResourceConfigurationStrategy strategy = context.mock(ResourceConfigurationStrategy.class);

        context.checking(new Expectations() {{
            allowing(factory).getStrategy(resourceId); will(returnValue(strategy));

            allowing(strategy).loadConfiguration(resourceId, FROM_STRUCTURED); will(returnValue(null));

            allowing(componentService).getResourceType(resourceId); will(returnValue(new ResourceType()));
        }});

        configurationMgr.loadResourceConfiguration(resourceId, FROM_STRUCTURED);
    }

}
