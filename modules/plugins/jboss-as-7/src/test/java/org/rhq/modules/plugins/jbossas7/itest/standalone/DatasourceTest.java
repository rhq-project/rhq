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

import static org.rhq.core.domain.resource.CreateResourceStatus.SUCCESS;
import static org.rhq.core.domain.resource.ResourceCategory.SERVICE;
import static org.rhq.modules.plugins.jbossas7.test.util.Constants.PLUGIN_NAME;
import static org.rhq.modules.plugins.jbossas7.test.util.Constants.STANDALONE_RESOURCE_KEY;
import static org.rhq.modules.plugins.jbossas7.test.util.Constants.STANDALONE_RESOURCE_TYPE;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.List;
import java.util.Set;

import org.testng.annotations.Test;

import org.rhq.core.clientapi.agent.inventory.CreateResourceRequest;
import org.rhq.core.clientapi.agent.inventory.CreateResourceResponse;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.modules.plugins.jbossas7.itest.AbstractJBossAS7PluginTest;
import org.rhq.test.arquillian.RunDiscovery;

/**
 * @author Thomas Segismont
 */
@Test(groups = { "integration", "pc", "standalone" }, singleThreaded = true)
public class DatasourceTest extends AbstractJBossAS7PluginTest {

    private static final String DATASOURCES_SUBSYSTEM_RESOURCE_TYPE_NAME = "Datasources (Standalone)";
    private static final String DATASOURCES_SUBSYSTEM_RESOURCE_KEY = "subsystem=datasources";
    private static final String DATASOURCE_RESOURCE_TYPE_NAME = "DataSource (Standalone)";
    private static final String DATASOURCE_TEST_DS = "DatasourceTestDS";

    private Resource datasourceSubsystemResource;
    private ResourceType datasourceResourceType;
    private Resource datasourceTestResource;

    // only need servers to create the management users necessary to load the module options
    @Test(priority = -10000)
    @RunDiscovery(discoverServers = true, discoverServices = false)
    public void initialDiscoveryTest() throws Exception {

        Resource platform = validatePlatform();
        Resource server = waitForResourceByTypeAndKey(platform, platform, STANDALONE_RESOURCE_TYPE,
            STANDALONE_RESOURCE_KEY);
        datasourceSubsystemResource = waitForResourceByTypeAndKey(platform, server, new ResourceType(
            DATASOURCES_SUBSYSTEM_RESOURCE_TYPE_NAME, getPluginName(), SERVICE, null),
            DATASOURCES_SUBSYSTEM_RESOURCE_KEY);
        for (ResourceType resourceType : datasourceSubsystemResource.getResourceType().getChildResourceTypes()) {
            if (DATASOURCE_RESOURCE_TYPE_NAME.equals(resourceType.getName())) {
                datasourceResourceType = resourceType;
                break;
            }
        }
        assertNotNull(datasourceResourceType, "Could not find resource type: " + DATASOURCE_RESOURCE_TYPE_NAME);
    }

    @Test(dependsOnMethods = { "initialDiscoveryTest" })
    public void testCreateDatasource() throws Exception {
        Configuration pluginConfig = new Configuration();
        pluginConfig.put(new PropertySimple("path", "data-source=" + DATASOURCE_TEST_DS));

        Configuration resourceConfig = datasourceResourceType.getResourceConfigurationDefinition().getDefaultTemplate()
            .createConfiguration();
        resourceConfig.put(new PropertySimple("connection-url", "jdbc:h2:mem:" + DATASOURCE_TEST_DS
            + ";DB_CLOSE_DELAY=-1"));
        resourceConfig.put(new PropertySimple("user-name", "sa"));
        resourceConfig.put(new PropertySimple("password", "sa"));
        resourceConfig.put(new PropertySimple("driver-class", "org.h2.Driver"));
        resourceConfig.put(new PropertySimple("driver-name", "h2"));
        resourceConfig.put(new PropertySimple("jndi-name", "java:jboss/datasources/" + DATASOURCE_TEST_DS));
        resourceConfig.put(new PropertyList("*1", new PropertyMap("*:pname", new PropertySimple("pname",
            "DatasourceTestKey"), new PropertySimple("value", "DatasourceTestValue"))));

        CreateResourceRequest createRequest = new CreateResourceRequest();
        createRequest.setParentResourceId(datasourceSubsystemResource.getId());
        createRequest.setPluginConfiguration(pluginConfig);
        createRequest.setPluginName(PLUGIN_NAME);
        createRequest.setResourceConfiguration(resourceConfig);
        createRequest.setResourceName(DATASOURCE_TEST_DS);
        createRequest.setResourceTypeName(datasourceResourceType.getName());

        CreateResourceResponse createResourceResponse = pluginContainer.getResourceFactoryManager()
            .executeCreateResourceImmediately(createRequest);
        assertEquals(createResourceResponse.getStatus(), SUCCESS, createResourceResponse.getErrorMessage());

        Set<Resource> datasourceResources = pluginContainer.getInventoryManager().getResourcesWithType(
            datasourceResourceType);
        datasourceTestResource = null;
        for (Resource datasourceResource : datasourceResources) {
            if (datasourceResource.getResourceKey().equals(createResourceResponse.getResourceKey())) {
                datasourceTestResource = datasourceResource;
                break;
            }
        }
        assertNotNull(datasourceTestResource, DATASOURCE_TEST_DS + " was not discovered");
        assertEquals(datasourceTestResource.getInventoryStatus(), InventoryStatus.COMMITTED);
    }

    @Test(dependsOnMethods = { "testCreateDatasource" })
    public void testDatasourceLoadConfiguration() throws Exception {
        Configuration configuration = pluginContainer.getConfigurationManager().loadResourceConfiguration(
            datasourceTestResource.getId());
        assertEquals(configuration.getSimpleValue("connection-url"), "jdbc:h2:mem:" + DATASOURCE_TEST_DS
            + ";DB_CLOSE_DELAY=-1");
        assertEquals(configuration.getSimpleValue("user-name"), "sa");
        assertEquals(configuration.getSimpleValue("password"), "sa");
        assertEquals(configuration.getSimpleValue("driver-class"), "org.h2.Driver");
        assertEquals(configuration.getSimpleValue("driver-name"), "h2");
        assertEquals(configuration.getSimpleValue("jndi-name"), "java:jboss/datasources/" + DATASOURCE_TEST_DS);
        PropertyList connectionPropertiesListWrapper = configuration.getList("*1");
        assertNotNull(connectionPropertiesListWrapper);
        List<Property> connectionPropertiesList = connectionPropertiesListWrapper.getList();
        assertEquals(connectionPropertiesList.size(), 1);
        Property property = connectionPropertiesList.iterator().next();
        assertTrue(property instanceof PropertyMap);
        PropertyMap propertyMap = (PropertyMap) property;
        String pname = propertyMap.getSimpleValue("pname", null);
        assertNotNull(pname);
        assertEquals(pname, "DatasourceTestKey");
        String value = propertyMap.getSimpleValue("value", null);
        assertNotNull(value);
        assertEquals(value, "DatasourceTestValue");
    }
}
