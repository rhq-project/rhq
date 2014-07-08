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

package org.rhq.modules.plugins.jbossas7.itest.standalone;

import static org.rhq.modules.plugins.jbossas7.test.util.Constants.PLUGIN_NAME;
import static org.rhq.modules.plugins.jbossas7.test.util.Constants.STANDALONE_RESOURCE_KEY;
import static org.rhq.modules.plugins.jbossas7.test.util.Constants.STANDALONE_RESOURCE_TYPE;

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
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.configuration.ConfigurationManager;
import org.rhq.modules.plugins.jbossas7.itest.AbstractJBossAS7PluginTest;
import org.rhq.test.arquillian.RunDiscovery;

@Test(groups = { "integration", "pc", "standalone" }, singleThreaded = true)
public class TemplatedResourcesTest extends AbstractJBossAS7PluginTest {

    private Log log = LogFactory.getLog(this.getClass());

    private Resource platform;
    private Resource server;

    @Test(priority = 10, groups = "discovery")
    @RunDiscovery(discoverServices = true, discoverServers = true)
    public void initialDiscoveryTest() throws Exception {
        platform = validatePlatform();
        server = waitForResourceByTypeAndKey(platform, platform, STANDALONE_RESOURCE_TYPE, STANDALONE_RESOURCE_KEY);
    }

    @Test(priority = 11)
    public void loadUpdateTemplatedResourceConfiguration() throws Exception {
        ConfigurationManager configurationManager = this.pluginContainer.getConfigurationManager();

        for (ResourceData resourceData : testResourceData) {
            ResourceType resourceType = new ResourceType(resourceData.resourceTypeName, PLUGIN_NAME,
                ResourceCategory.SERVICE, null);
            Resource subsystem = waitForResourceByTypeAndKey(platform, server, resourceType, resourceData.resourceKey);

            Assert.assertNotNull(subsystem);

            Queue<Resource> unparsedResources = new LinkedList<Resource>();
            unparsedResources.add(subsystem);

            List<Resource> foundResources = new ArrayList<Resource>();
            while (!unparsedResources.isEmpty()) {
                Resource currentResource = unparsedResources.poll();

                for (Resource childResource : currentResource.getChildResources()) {
                    unparsedResources.add(childResource);
                    if (childResource.getResourceType().getName().equals(resourceData.subResourceType)) {
                        foundResources.add(childResource);
                    }
                }
            }

            for (Resource resourceUnderTest : foundResources) {
                log.info(foundResources);

                assert resourceUnderTest.getId() != 0 : "Resource not properly initialized. Id = 0. Try extending sleep after discovery.";
                Configuration resourceUnderTestConfig = configurationManager
                    .loadResourceConfiguration(resourceUnderTest.getId());

                Assert.assertNotNull(resourceUnderTestConfig);
                Assert.assertFalse(resourceUnderTestConfig.getProperties().isEmpty());

                boolean specialPropertyFound = false;
                for (Property property : resourceUnderTestConfig.getProperties()) {
                    if (property.getName().equals(resourceData.specialConfigurationProperty)) {
                        Assert.assertNotNull(((PropertySimple) property).getStringValue());
                        specialPropertyFound = true;
                        break;
                    }
                }

                Assert.assertTrue(specialPropertyFound, resourceData.specialConfigurationProperty
                    + "property not found in the list of properties");

                ConfigurationUpdateRequest testUpdateRequest = new ConfigurationUpdateRequest(1,
                    resourceUnderTestConfig, resourceUnderTest.getId());
                ConfigurationUpdateResponse response = configurationManager
                    .executeUpdateResourceConfigurationImmediately(testUpdateRequest);

                Assert.assertNotNull(response);
            }
        }
    }

    private ResourceData[] testResourceData = new ResourceData[] {
        new ResourceData("Infinispan", "subsystem=infinispan", "Cache", "__type"),
        new ResourceData("Threads", "subsystem=threads", "ThreadPool", "__type"),
        new ResourceData("Messaging", "subsystem=messaging", "Path", "__name") };

    private static class ResourceData {
        final String resourceTypeName;
        final String resourceKey;
        final String subResourceType;
        final String specialConfigurationProperty;

        public ResourceData(String resourceTypeName, String resourceKey, String subResourceType,
            String specialConfigurationProperty) {
            this.resourceTypeName = resourceTypeName;
            this.resourceKey = resourceKey;
            this.subResourceType = subResourceType;
            this.specialConfigurationProperty = specialConfigurationProperty;
        }
    }
}
