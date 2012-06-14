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

package org.rhq.modules.plugins.jbossas7.itest.standalone;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.agent.configuration.ConfigurationUpdateRequest;
import org.rhq.core.clientapi.server.configuration.ConfigurationUpdateResponse;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.pc.configuration.ConfigurationManager;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.inventory.ResourceContainer;
import org.rhq.modules.plugins.jbossas7.itest.AbstractJBossAS7PluginTest;
import org.rhq.test.arquillian.RunDiscovery;

/**
 * @author Stefan Negrea
 *
 */
@Test(groups = { "integration", "pc", "standalone" }, singleThreaded = true)
public class ResourcesStandaloneServerTest extends AbstractJBossAS7PluginTest  {
    private Log log = LogFactory.getLog(this.getClass());

    @Test(priority = 10, groups = "discovery")
    @RunDiscovery(discoverServices = true, discoverServers = true)
    public void discoverPlatform() throws Exception {
        InventoryManager inventoryManager = this.pluginContainer.getInventoryManager();

        Resource platform = inventoryManager.getPlatform();
        assertNotNull(platform);
        assertEquals(platform.getInventoryStatus(), InventoryStatus.COMMITTED);

        Thread.sleep(20 * 1000L);

        ResourceContainer platformContainer = inventoryManager.getResourceContainer(platform);
        Resource server = getResourceByTypeAndKey(platform, StandaloneServerComponentTest.RESOURCE_TYPE,
            StandaloneServerComponentTest.RESOURCE_KEY);
        inventoryManager.activateResource(server, platformContainer, false);

        Thread.sleep(20 * 1000L);
    }


    @Test(priority = 12)
    public void loadUpdateTemplatedResourceConfiguration() throws Exception {
        List<String> ignoredResources = new ArrayList<String>();

        //ignored because of differences between test plugin container and real application
        //works well with real agent
        ignoredResources.add("VHost");

        //ignored because the settings different when started with arquillian, inet-address is not set correctly
        //works well with real agent
        ignoredResources.add("Network Interface");

        Resource platform = this.pluginContainer.getInventoryManager().getPlatform();
        Resource server = getResourceByTypeAndKey(platform, StandaloneServerComponentTest.RESOURCE_TYPE,
            StandaloneServerComponentTest.RESOURCE_KEY);

        ConfigurationManager configurationManager = this.pluginContainer.getConfigurationManager();
        configurationManager.initialize();
        Thread.sleep(40 * 1000L);

        Queue<Resource> unparsedResources = new LinkedList<Resource>();
        for (Resource topLevelResource : server.getChildResources()) {
            if (topLevelResource.getInventoryStatus().equals(InventoryStatus.COMMITTED)) {
                unparsedResources.add(topLevelResource);
            } else {
                log.info("Subsystem not COMMITTED " + topLevelResource + " - " + topLevelResource.getInventoryStatus());
            }
        }

        int errorCount = 0;

        while (!unparsedResources.isEmpty()) {
            Resource resourceUnderTest = unparsedResources.poll();

            for (Resource childResource : resourceUnderTest.getChildResources()) {
                if (childResource.getInventoryStatus().equals(InventoryStatus.COMMITTED)) {
                    unparsedResources.add(childResource);
                } else {
                    log.info("Subsystem not COMMITTED " + childResource + " - " + childResource.getInventoryStatus());
                }
            }

            if (resourceUnderTest.getResourceType().getResourceConfigurationDefinition() != null
                && !ignoredResources.contains(resourceUnderTest.getResourceType().getName())) {
                Configuration configUnderTest = configurationManager
                    .loadResourceConfiguration(resourceUnderTest.getId());

                ConfigurationUpdateRequest updateRequest = new ConfigurationUpdateRequest(1, configUnderTest,
                    resourceUnderTest.getId());
                ConfigurationUpdateResponse updateResponse = configurationManager
                    .executeUpdateResourceConfigurationImmediately(updateRequest);

                Assert.assertNotNull(updateResponse);
                if (updateResponse.getErrorMessage() != null) {
                    errorCount++;
                    log.error("------------------------------");
                    log.error(resourceUnderTest);
                    log.error(updateResponse.getErrorMessage());
                    log.error("------------------------------");
                }
            }
        }

        Assert.assertEquals(errorCount, 0);
    }

    @Test(priority = 11)
    public void executeNoArgOperations() throws Exception {
        List<String> ignoredSubsystems = new ArrayList<String>();
        ignoredSubsystems.add("ModCluster Standalone Service");

        List<String> ignoredOperations = new ArrayList<String>();
        ignoredOperations.add("subsystem:force-failover");
        ignoredOperations.add("enable");

        Resource platform = this.pluginContainer.getInventoryManager().getPlatform();
        Resource server = getResourceByTypeAndKey(platform, StandaloneServerComponentTest.RESOURCE_TYPE,
            StandaloneServerComponentTest.RESOURCE_KEY);

        Queue<Resource> unparsedResources = new LinkedList<Resource>();
        for (Resource topLevelResource : server.getChildResources()) {
            if (topLevelResource.getInventoryStatus().equals(InventoryStatus.COMMITTED)
                && !ignoredSubsystems.contains(topLevelResource.getResourceType().getName())) {
                unparsedResources.add(topLevelResource);
            }
        }

        while (!unparsedResources.isEmpty()) {
            Resource resourceUnderTest = unparsedResources.poll();

            for (Resource childResource : resourceUnderTest.getChildResources()) {
                if (childResource.getInventoryStatus().equals(InventoryStatus.COMMITTED)) {
                    unparsedResources.add(childResource);
                }
            }

            for (OperationDefinition operationUnderTest : resourceUnderTest.getResourceType().getOperationDefinitions()) {
                if (!ignoredOperations.contains(operationUnderTest.getName())) {
                    if (operationUnderTest.getParametersConfigurationDefinition() == null
                        || operationUnderTest.getParametersConfigurationDefinition().getPropertyDefinitions().isEmpty()) {
                        this.invokeOperationAndAssertSuccess(resourceUnderTest, operationUnderTest.getName(),
                            new Configuration());
                    }
                }
            }
        }
    }
}
