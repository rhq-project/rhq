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

import org.rhq.core.pc.inventory.InventoryService;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pluginapi.configuration.ResourceConfigurationFacet;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.jmock.Expectations;

public class StructuredResourceConfigurationStrategyTest extends JMockTest {

    InventoryService componentService;

    ResourceConfigurationFacet configFacet;

    int resourceId = -1;

    boolean daemonThread = true;

    boolean onlyIfStarted = true;

    StructuredResourceConfigurationStrategy strategy;

    @BeforeMethod
    public void setup() {
        componentService = context.mock(InventoryService.class);

        configFacet = context.mock(ResourceConfigurationFacet.class);

        strategy = new StructuredResourceConfigurationStrategy();
        strategy.setComponentService(componentService);
    }

    @Test
    public void resourceConfigFacetShouldGetLoaded() throws Exception {
        context.checking(new Expectations() {{
            atLeast(1).of(componentService).getComponent(resourceId,
                                                         ResourceConfigurationFacet.class,
                                                         FacetLockType.READ,
                                                         ResourceConfigurationStrategy.FACET_METHOD_TIMEOUT,
                                                         daemonThread,
                                                         onlyIfStarted);
            will(returnValue(configFacet));

            ignoring(configFacet);
        }});

        strategy.loadConfiguration(resourceId);
    }

    @Test
    public void theStructuredConfigShouldGetLoadedByTheFacet() throws Exception {
        context.checking(new Expectations() {{
            allowing(componentService).getComponent(resourceId,
                                                    ResourceConfigurationFacet.class,
                                                    FacetLockType.READ,
                                                    ResourceConfigurationStrategy.FACET_METHOD_TIMEOUT,
                                                    daemonThread,
                                                    onlyIfStarted);
            will(returnValue(configFacet));

            atLeast(1).of(configFacet).loadStructuredConfiguration();
        }});

        strategy.loadConfiguration(resourceId);
    }

}
