/*
 * RHQ Management Platform
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.rhq.modules.plugins.jbossas7.itest.domain;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.inventory.ResourceContainer;
import org.rhq.modules.plugins.jbossas7.itest.AbstractJBossAS7PluginTest;
import org.rhq.test.arquillian.RunDiscovery;

/**
 * @author Stefan Negrea
 *
 */
@Test(groups = { "integration", "pc", "domain" }, singleThreaded = true)
public class ResourcesDomainServerTest extends AbstractJBossAS7PluginTest {
    private Log log = LogFactory.getLog(this.getClass());

    @Test(priority = 1030, groups = "discovery")
    @RunDiscovery(discoverServices = true, discoverServers = true)
    public void discoverPlatform() throws Exception {
        InventoryManager inventoryManager = this.pluginContainer.getInventoryManager();

        Resource platform = inventoryManager.getPlatform();
        assertNotNull(platform);
        assertEquals(platform.getInventoryStatus(), InventoryStatus.COMMITTED);

        Thread.sleep(20 * 1000L);

        ResourceContainer platformContainer = inventoryManager.getResourceContainer(platform);
        Resource server = getResourceByTypeAndKey(platform, DomainServerComponentTest.RESOURCE_TYPE,
            DomainServerComponentTest.RESOURCE_KEY);
        inventoryManager.activateResource(server, platformContainer, false);

        Thread.sleep(80 * 1000L);
    }


    @Test(priority = 1031)
    public void loadUpdateResourceConfiguration() throws Exception {
        List<String> ignoredResources = new ArrayList<String>();

        //ignored because of differences between test plugin container and real application
        //works well with real agent
        ignoredResources.add("VHost (Profile)");
        ignoredResources.add("VHost (Managed Server)");

        //created JIRA AS7-5011
        //server is started with the configuration but unable to write it back as is
        //due to marshaling error
        ignoredResources.add("Network Interface");

        //created JIRA AS7-5012
        //default value for  is float but the resource only accepts integers
        ignoredResources.add("Domain Load Metric");

        //will revisit after BZ 826542 is resolved
        //ignoredResources.add("Authentication (Classic)");

        //tested separately
        ignoredResources.add("SocketBindingGroup");

        //Update requirements validate separately
        ignoredResources.add("Pooled Connection Factory (Profile)");
        ignoredResources.add("Connection Factory (Profile)");
        ignoredResources.add("Pooled Connection Factory (Managed Server)");
        ignoredResources.add("Connection Factory (Managed Server)");

        ignoredResources.add("DataSource (Profile)");
        ignoredResources.add("DataSource (Managed)");

        ignoredResources.add("Cluster Connection (Profile)");

        ignoredResources.add("Memory Pool");
        ignoredResources.add("Periodic Rotating File Handler");
        ignoredResources.add("Console Handler");

        // Datasources need a complex workflow, cannot be tested like this

        ignoredResources.add("Datasources (Profile)");
        ignoredResources.add("Datasources (Managed)");

        // Cannot apply configuration blindly

        ignoredResources.add("Cluster Connection (Profile)");

        Resource platform = this.pluginContainer.getInventoryManager().getPlatform();
        Resource server = getResourceByTypeAndKey(platform, DomainServerComponentTest.RESOURCE_TYPE,
            DomainServerComponentTest.RESOURCE_KEY);

        int errorCount = loadUpdateConfigChildResources(server, ignoredResources);
        Assert.assertEquals(errorCount, 0);
    }

    @Test(priority = 1032)
    public void executeNoArgOperations() throws Exception {
        List<String> ignoredSubsystems = new ArrayList<String>();

        List<String> ignoredOperations = new ArrayList<String>();
        //ignored because there is no other server to fail-over to
        ignoredOperations.add("subsystem:force-failover");
        //ignored because this is not a true operation, it is handled
        //internally by a configuration property change
        ignoredOperations.add("enable");
        //ignored because the Osgi subsystem not configured out of box
        ignoredOperations.add("subsystem:activate");

        //ignored operations because they will stop the managed server and
        //the domain controller itself
        ignoredOperations.add("stop");
        ignoredOperations.add("restart");
        ignoredOperations.add("shutdown");
        ignoredOperations.add("stop-servers");
        ignoredOperations.add("restart-servers");

        Resource platform = this.pluginContainer.getInventoryManager().getPlatform();
        Resource server = getResourceByTypeAndKey(platform, DomainServerComponentTest.RESOURCE_TYPE,
            DomainServerComponentTest.RESOURCE_KEY);

        executeNoArgOperations(server, ignoredSubsystems, ignoredOperations);
    }

}
