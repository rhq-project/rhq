/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.rhq.core.domain.measurement.AvailabilityType.UP;
import static org.rhq.modules.plugins.jbossas7.test.util.Constants.DOMAIN_RESOURCE_KEY;
import static org.rhq.modules.plugins.jbossas7.test.util.Constants.DOMAIN_RESOURCE_TYPE;
import static org.rhq.modules.plugins.jbossas7.test.util.Constants.PLUGIN_NAME;
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

    private Resource managedServer;

    @Test(priority = 10)
    @RunDiscovery
    public void initialDiscoveryTest() throws Exception {
        Resource platform = validatePlatform();
        Resource serverResource = waitForResourceByTypeAndKey(platform, platform, DOMAIN_RESOURCE_TYPE,
            DOMAIN_RESOURCE_KEY);
        managedServer = waitForResourceByTypeAndKey(platform, serverResource, RESOURCE_TYPE, RESOURCE_KEY);
    }

    @Test(priority = 1021)
    public void testRestart() throws Exception {

        Resource resource = getResource();

        Configuration configuration = new Configuration();
        configuration.put(new PropertySimple("blocking", "true"));
        configuration.put(new PropertySimple("operationTimeout", "120"));

        // API is supposed to block until managed server is up
        invokeOperationAndAssertSuccess(resource, "restart", configuration);
        waitForServerToBeUpAgain(resource);

        // API call does not block
        configuration.put(new PropertySimple("blocking", "false"));
        invokeOperationAndAssertSuccess(resource, "restart", configuration);

        waitForServerToBeUpAgain(resource);

    }

    private void waitForServerToBeUpAgain(Resource resource) throws InterruptedException, PluginContainerException {
        int count = 0;
        long pause_seconds = 1;
        do {
            Thread.sleep(SECONDS.toMillis(pause_seconds));
            count++;
        } while (getAvailability(resource) != UP && SECONDS.toMinutes(pause_seconds * count) < 5);

        AvailabilityType avail = getAvailability(getResource());
        assertEquals(avail, UP);
    }

    private Resource getResource() {
        return managedServer;
    }

}
