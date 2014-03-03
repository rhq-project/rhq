/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.modules.plugins.jbossas7.itest.domain;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.modules.plugins.jbossas7.itest.AbstractJBossAS7PluginTest;
import org.rhq.test.arquillian.RunDiscovery;

/**
 * Test dealing with managed servers
 * @author Heiko W. Rupp
 */
@Test(groups = { "integration", "pc", "domain" }, singleThreaded = true)
public class ManagedServerTest extends AbstractJBossAS7PluginTest {

    public static final ResourceType RESOURCE_TYPE = new ResourceType("Managed Server", PLUGIN_NAME,
        ResourceCategory.SERVER, null);
    private static final String RESOURCE_KEY = "master/server-one";

    private Resource platform;
    private Resource serverResource;
    private Resource managedServer;

    @Test(priority = 10)
    @RunDiscovery
    public void initialDiscoveryTest() throws Exception {
        platform = validatePlatform();
        serverResource = waitForResourceByTypeAndKey(platform, platform, DomainServerComponentTest.RESOURCE_TYPE,
            DomainServerComponentTest.RESOURCE_KEY);
        managedServer = waitForResourceByTypeAndKey(platform, serverResource, RESOURCE_TYPE, RESOURCE_KEY);
    }

    @Test(priority = 1021, enabled = false)
    public void testRestart() throws Exception {

        Resource resource = getResource();
        // No parameter -> AS7 api call does not block
        invokeOperationAndAssertSuccess(resource, "restart", null);
        waitForServerToBeUpAgain(resource);

        // Now test explicit parameters
        Configuration configuration = new Configuration();

        // API is supposed to block until managed server is up
        configuration.put(new PropertySimple("blocking", true));
        invokeOperationAndAssertSuccess(resource, "restart", configuration);
        waitForServerToBeUpAgain(resource);

        // API call does not block
        configuration.put(new PropertySimple("blocking", false));
        invokeOperationAndAssertSuccess(resource, "restart", null);

        waitForServerToBeUpAgain(resource);

    }

    private void waitForServerToBeUpAgain(Resource resource) throws InterruptedException, PluginContainerException {
        int count = 0;
        do {
            Thread.sleep(5000L); // We need to wait a little as this is non-blocking
            count++;
        } while (getAvailability(resource) != AvailabilityType.UP && count < 10);

        AvailabilityType avail = getAvailability(getResource());
        assertEquals(avail, AvailabilityType.UP);
    }

    private Resource getResource() {
        return managedServer;
    }

}
