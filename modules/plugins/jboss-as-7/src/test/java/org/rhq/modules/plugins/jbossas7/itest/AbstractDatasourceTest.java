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

package org.rhq.modules.plugins.jbossas7.itest;

import static java.lang.Boolean.TRUE;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.rhq.core.domain.resource.CreateResourceStatus.SUCCESS;
import static org.rhq.modules.plugins.jbossas7.test.util.Constants.PLUGIN_NAME;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.util.List;
import java.util.Set;

import org.testng.annotations.Test;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.configuration.ConfigurationUpdateRequest;
import org.rhq.core.clientapi.agent.inventory.CreateResourceRequest;
import org.rhq.core.clientapi.agent.inventory.CreateResourceResponse;
import org.rhq.core.clientapi.server.configuration.ConfigurationUpdateResponse;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.inventory.ResourceContainer;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.test.arquillian.RunDiscovery;

/**
 * @author Thomas Segismont
 */
public abstract class AbstractDatasourceTest extends AbstractJBossAS7PluginTest {

    private Resource datasourcesSubsystemResource;
    private ResourceType datasourceResourceType;
    private Resource datasourceTestResource;

    protected abstract String getDatasourceResourceTypeName();

    protected abstract String getTestDatasourceName();

    protected abstract Resource getDatasourcesSubsystemResource() throws Exception;

    @Test(priority = -10000)
    @RunDiscovery(discoverServers = true, discoverServices = false)
    public void initialDiscoveryTest() throws Exception {
        datasourcesSubsystemResource = getDatasourcesSubsystemResource();
        for (ResourceType resourceType : datasourcesSubsystemResource.getResourceType().getChildResourceTypes()) {
            if (getDatasourceResourceTypeName().equals(resourceType.getName())) {
                datasourceResourceType = resourceType;
                break;
            }
        }
        assertNotNull(datasourceResourceType, "Could not find resource type: " + getDatasourceResourceTypeName());
    }

    @Test(dependsOnMethods = { "initialDiscoveryTest" })
    public void testCreateDatasource() throws Exception {
        Configuration pluginConfig = new Configuration();
        pluginConfig.put(new PropertySimple("path", "data-source=" + getTestDatasourceName()));

        Configuration resourceConfig = datasourceResourceType.getResourceConfigurationDefinition().getDefaultTemplate()
            .createConfiguration();
        resourceConfig.put(new PropertySimple("connection-url", "jdbc:h2:mem:" + getTestDatasourceName()
            + ";DB_CLOSE_DELAY=-1"));
        resourceConfig.put(new PropertySimple("user-name", "sa"));
        resourceConfig.put(new PropertySimple("password", "sa"));
        resourceConfig.put(new PropertySimple("driver-class", "org.h2.Driver"));
        resourceConfig.put(new PropertySimple("driver-name", "h2"));
        resourceConfig.put(new PropertySimple("jndi-name", "java:jboss/datasources/" + getTestDatasourceName()));
        resourceConfig.put(new PropertyList("*1", new PropertyMap("*:pname", new PropertySimple("pname",
            "DatasourceTestKey"), new PropertySimple("value", "DatasourceTestValue"))));

        CreateResourceRequest createRequest = new CreateResourceRequest();
        createRequest.setParentResourceId(datasourcesSubsystemResource.getId());
        createRequest.setPluginConfiguration(pluginConfig);
        createRequest.setPluginName(PLUGIN_NAME);
        createRequest.setResourceConfiguration(resourceConfig);
        createRequest.setResourceName(getTestDatasourceName());
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
        assertNotNull(datasourceTestResource, getTestDatasourceName() + " was not discovered");
        assertEquals(datasourceTestResource.getInventoryStatus(), InventoryStatus.COMMITTED);
    }

    @Test(dependsOnMethods = { "testCreateDatasource" })
    public void testDatasourceLoadConfiguration() throws Exception {
        Configuration configuration = pluginContainer.getConfigurationManager().loadResourceConfiguration(
            datasourceTestResource.getId());
        assertTrue(Boolean.parseBoolean(configuration.getSimpleValue("enabled")), "");
        assertEquals(configuration.getSimpleValue("connection-url"), "jdbc:h2:mem:" + getTestDatasourceName()
            + ";DB_CLOSE_DELAY=-1");
        assertEquals(configuration.getSimpleValue("user-name"), "sa");
        assertEquals(configuration.getSimpleValue("password"), "sa");
        assertEquals(configuration.getSimpleValue("driver-class"), "org.h2.Driver");
        assertEquals(configuration.getSimpleValue("driver-name"), "h2");
        assertEquals(configuration.getSimpleValue("jndi-name"), "java:jboss/datasources/" + getTestDatasourceName());
        PropertyList connectionPropertiesListWrapper = configuration.getList("*1");
        assertNotNull(connectionPropertiesListWrapper, "Connection properties list wrapper is null");
        List<Property> connectionPropertiesList = connectionPropertiesListWrapper.getList();
        assertEquals(connectionPropertiesList.size(), 1);
        Property property = connectionPropertiesList.iterator().next();
        assertTrue(property instanceof PropertyMap, "Connection properties should be a list of maps");
        PropertyMap propertyMap = (PropertyMap) property;
        String pname = propertyMap.getSimpleValue("pname", null);
        assertNotNull(pname, "Connection property key is null");
        assertEquals(pname, "DatasourceTestKey");
        String value = propertyMap.getSimpleValue("value", null);
        assertNotNull(value, "Connection property value is null");
        assertEquals(value, "DatasourceTestValue");
    }

    @Test(dependsOnMethods = { "testDatasourceLoadConfiguration" })
    public void changingMaxPoolSizeShouldNotRequireDisablingTheDatasourceNorReloadingServer() throws Exception {
        Configuration configuration = pluginContainer.getConfigurationManager().loadResourceConfiguration(
            datasourceTestResource.getId());
        // Make a working copy
        configuration = configuration.deepCopy(false);
        // Change value
        PropertySimple propertySimple = configuration.getSimple("max-pool-size");
        Long currentMaxPoolSize = propertySimple.getLongValue();
        long newMaxPoolSize = currentMaxPoolSize == null ? 40 : currentMaxPoolSize + 1;
        propertySimple.setStringValue(String.valueOf(newMaxPoolSize));
        ConfigurationUpdateRequest configurationUpdateRequest = new ConfigurationUpdateRequest(-1, configuration,
            datasourceTestResource.getId());
        ConfigurationUpdateResponse configurationUpdateResponse = pluginContainer.getConfigurationManager()
            .executeUpdateResourceConfigurationImmediately(configurationUpdateRequest);
        assertSame(configurationUpdateResponse.getStatus(), ConfigurationUpdateStatus.SUCCESS);
        configuration = pluginContainer.getConfigurationManager().loadResourceConfiguration(
            datasourceTestResource.getId());
        assertTrue(Boolean.parseBoolean(configuration.getSimpleValue("enabled")), "");
        assertEquals(configuration.getSimple("max-pool-size").getLongValue(), Long.valueOf(newMaxPoolSize));
        assertFalse(
            configuration.getNames().contains("__OOB"),
            "The configuration object should not contain out of band messages: "
                + configuration.getSimpleValue("__OOB"));
    }

    @Test(dependsOnMethods = { "changingMaxPoolSizeShouldNotRequireDisablingTheDatasourceNorReloadingServer" })
    public void disableWithRestartThenChangeThenEnableShouldNotRequireReload() throws Exception {
        // First disable datasource (allow service restart)
        OperationDefinition disableOperation = getOperationDefinition("disable");
        Configuration disableOperationParams = disableOperation.getParametersConfigurationDefinition()
            .getDefaultTemplate().createConfiguration();
        disableOperationParams.setSimpleValue("allow-resource-service-restart", TRUE.toString());
        OperationFacet operationFacet = getOperationFacet(datasourceTestResource);
        OperationResult operationResult = operationFacet.invokeOperation(disableOperation.getName(),
            disableOperationParams);
        assertNull(operationResult.getErrorMessage(),
            "Disable operation failed with error: " + operationResult.getErrorMessage());

        // Then update properties which can only be changed when datasource is in disabled state
        Configuration configuration = pluginContainer.getConfigurationManager().loadResourceConfiguration(
            datasourceTestResource.getId());
        assertFalse(Boolean.parseBoolean(configuration.getSimpleValue("enabled")), "");
        // Make a working copy
        configuration = configuration.deepCopy(false);
        // Change values
        PropertySimple propertySimple = configuration.getSimple("prepared-statements-cache-size");
        Long currentCacheSize = propertySimple.getLongValue();
        long newCacheSize = currentCacheSize == null ? 40 : currentCacheSize + 1;
        propertySimple.setStringValue(String.valueOf(newCacheSize));
        PropertyList connectionPropertiesListWrapper = configuration.getList("*1");
        assertNotNull(connectionPropertiesListWrapper, "Connection properties list wrapper is null");
        List<Property> connectionPropertiesList = connectionPropertiesListWrapper.getList();
        connectionPropertiesList.add(new PropertyMap("*:pname", new PropertySimple("pname", "pipo"),
            new PropertySimple("value", "molo")));
        ConfigurationUpdateRequest configurationUpdateRequest = new ConfigurationUpdateRequest(-1, configuration,
            datasourceTestResource.getId());
        ConfigurationUpdateResponse configurationUpdateResponse = pluginContainer.getConfigurationManager()
            .executeUpdateResourceConfigurationImmediately(configurationUpdateRequest);
        assertSame(configurationUpdateResponse.getStatus(), ConfigurationUpdateStatus.SUCCESS);
        configuration = pluginContainer.getConfigurationManager().loadResourceConfiguration(
            datasourceTestResource.getId());
        assertFalse(Boolean.parseBoolean(configuration.getSimpleValue("enabled")), "");
        assertEquals(configuration.getSimple("prepared-statements-cache-size").getLongValue(),
            Long.valueOf(newCacheSize));
        connectionPropertiesListWrapper = configuration.getList("*1");
        assertNotNull(connectionPropertiesListWrapper, "Connection properties list wrapper is null");
        connectionPropertiesList = connectionPropertiesListWrapper.getList();
        String connectionPropertyValue = null;
        for (Property property : connectionPropertiesList) {
            assertTrue(property instanceof PropertyMap, "Connection properties should be a list of maps");
            PropertyMap propertyMap = (PropertyMap) property;
            String pname = propertyMap.getSimpleValue("pname", null);
            assertNotNull(pname, "Connection property key is null");
            String value = propertyMap.getSimpleValue("value", null);
            assertNotNull(value, "Connection property value is null");
            if ("pipo".equals(pname)) {
                connectionPropertyValue = value;
                break;
            }
        }
        assertEquals(connectionPropertyValue, "molo");
        assertFalse(
            configuration.getNames().contains("__OOB"),
            "The configuration object should not contain out of band messages: "
                + configuration.getSimpleValue("__OOB"));

        // Now re-enable datasource
        OperationDefinition enableOperation = getOperationDefinition("enable");
        Configuration enableOperationParams = enableOperation.getParametersConfigurationDefinition()
            .getDefaultTemplate().createConfiguration();
        operationResult = operationFacet.invokeOperation(enableOperation.getName(), enableOperationParams);
        assertNull(operationResult.getErrorMessage(),
            "Enable operation failed with error: " + operationResult.getErrorMessage());

        // Check server state
        configuration = pluginContainer.getConfigurationManager().loadResourceConfiguration(
            datasourceTestResource.getId());
        assertTrue(Boolean.parseBoolean(configuration.getSimpleValue("enabled")), "");
        assertFalse(
            configuration.getNames().contains("__OOB"),
            "The configuration object should not contain out of band messages: "
                + configuration.getSimpleValue("__OOB"));
    }

    private OperationDefinition getOperationDefinition(String operationName) {
        Set<OperationDefinition> operationDefinitions = datasourceResourceType.getOperationDefinitions();
        OperationDefinition operationDefinition = null;
        for (OperationDefinition opDef : operationDefinitions) {
            if (opDef.getName().equals(operationName)) {
                operationDefinition = opDef;
                break;
            }
        }
        assertNotNull(operationDefinition);
        return operationDefinition;
    }

    private OperationFacet getOperationFacet(Resource resource) {
        ResourceContainer resourceContainer = pluginContainer.getInventoryManager().getResourceContainer(resource);
        try {
            return resourceContainer.createResourceComponentProxy(OperationFacet.class, FacetLockType.WRITE,
                SECONDS.toMillis(5), false, false, false);
        } catch (PluginContainerException e) {
            return null;
        }
    }
}
