/*
 * Jopr Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.plugins.jbossas5.test;

import static org.testng.Assert.fail;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.rhq.core.clientapi.agent.configuration.ConfigurationUpdateRequest;
import org.rhq.core.clientapi.agent.inventory.CreateResourceRequest;
import org.rhq.core.clientapi.agent.metadata.PluginMetadataManager;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCreationDataType;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.configuration.ConfigurationManager;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.inventory.ResourceFactoryManager;
import org.rhq.core.pc.plugin.PluginManager;
import org.rhq.core.pc.util.ComponentUtil;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;

/**
 * @author Ian Springer
 * @author Lukas Krejci
 */
public abstract class AbstractResourceTest extends AbstractPluginTest {
    private static final long MEASUREMENT_FACET_METHOD_TIMEOUT = 3000; // 3 seconds
    private static final long OPERATION_FACET_METHOD_TIMEOUT = 3000; // 3 seconds

    protected void testDiscovery() throws Exception {
        Set<Resource> resources = getResources();
        System.out.println("Found " + resources.size() + " " + getResourceType().getName() + " Resources.");
        assert !resources.isEmpty();
    }

    @Deprecated
    protected void testResourceCreation() throws Exception {
        ResourceType resourceType = getResourceType();
        if (!resourceType.isCreatable()) {
            return;
        }
        if (resourceType.getCreationDataType() == ResourceCreationDataType.CONFIGURATION) {
            ResourceFactoryManager resourceFactoryManager = PluginContainer.getInstance().getResourceFactoryManager();
            new CreateResourceRequest(0, getServerResource().getId(), "My" + getResourceTypeName(),
                getResourceTypeName(), getPluginName(), new Configuration(), getTestResourceConfiguration(), null);
            //resourceFactoryManager.createResource();
        } else {
            // TODO
        }

        return;
    }

    protected void testMetrics() throws Exception {
        if (supportsFacet(MeasurementFacet.class)) {
            Set<MeasurementDefinition> metricDefinitions = getResourceType().getMetricDefinitions();
            Set<Resource> resources = getResources();
            for (Resource resource : resources) {
                System.out.println("Validating metrics for " + resource + "...");
                MeasurementFacet measurementFacet = ComponentUtil.getComponent(resource.getId(),
                    MeasurementFacet.class, FacetLockType.READ, MEASUREMENT_FACET_METHOD_TIMEOUT, true, true);
                for (MeasurementDefinition metricDefinition : metricDefinitions) {
                    String name = metricDefinition.getName();
                    try {
                        DataType dataType = metricDefinition.getDataType();
                        if (dataType == DataType.MEASUREMENT || metricDefinition.getDataType() == DataType.TRAIT) {
                            MeasurementReport report = new MeasurementReport();
                            Set<MeasurementScheduleRequest> requests = new HashSet<MeasurementScheduleRequest>();
                            MeasurementScheduleRequest request = new MeasurementScheduleRequest(1, metricDefinition
                                .getName(), 0, true, metricDefinition.getDataType());
                            requests.add(request);
                            measurementFacet.getValues(report, requests);
                            if (dataType == DataType.MEASUREMENT) {
                                assert report.getNumericData().isEmpty() || report.getNumericData().size() == 1;
                                assert report.getTraitData().isEmpty();
                                assert report.getCallTimeData().isEmpty();
                                MeasurementDataNumeric dataNumeric = (report.getNumericData().isEmpty()) ? null
                                    : report.getNumericData().iterator().next();
                                Double value = (dataNumeric != null) ? dataNumeric.getValue() : null;
                                System.out.println("Validating numeric metric '" + name + "' value (" + value + ")...");
                                validateNumericMetricValue(metricDefinition.getName(), value, resource);
                            } else if (metricDefinition.getDataType() == DataType.TRAIT) {
                                assert report.getTraitData().isEmpty() || report.getTraitData().size() == 1;
                                assert report.getNumericData().isEmpty();
                                assert report.getCallTimeData().isEmpty();
                                MeasurementDataTrait dataTrait = (report.getTraitData().size() == 1) ? report
                                    .getTraitData().iterator().next() : null;
                                String value = (dataTrait != null) ? dataTrait.getValue() : null;
                                System.out.println("Validating trait '" + name + "' value (" + value + ")...");
                                validateTraitMetricValue(metricDefinition.getName(), value, resource);
                            }
                        }
                    } catch (Exception e) {
                        fail("Collection of metrics " + name + " for resource " + resource.getName() + " failed.", e);
                    }
                }
            }
        }
        return;
    }

    protected void testOperations() throws Exception {
        Set<OperationDefinition> operationDefinitions = getResourceType().getOperationDefinitions();
        Set<String> operationNames = new TreeSet<String>();
        for (OperationDefinition operationDefinition : operationDefinitions) {
            operationNames.add(operationDefinition.getName());
        }

        Set<Resource> resources = getResources();
        testOperations(resources, operationNames);
    }

    protected void testOperations(Set<Resource> resources, Set<String> operationNames) throws Exception {
        if (supportsFacet(OperationFacet.class)) {
            //Set<OperationDefinition> operationDefinitions = getResourceType().getOperationDefinitions();
            //Set<Resource> resources = getResources();
            for (Resource resource : resources) {
                System.out.println("Validating operations for " + resource + "...");
                // TODO: Execute lifecycle operations in a specific order, so they don't break the other operations.
                //       For example, execute 'restart' first, followed by 'stop', followed by 'start'.
                for (String name : operationNames) {
                    //we must get a new operation facet for each operation so that each operation gets
                    //the 3 seconds to finish. Otherwise all the operations would have to finish in 3 secs
                    //which can be a bit harsh limit.
                    OperationFacet operationFacet = ComponentUtil.getComponent(resource.getId(), OperationFacet.class,
                        FacetLockType.WRITE, OPERATION_FACET_METHOD_TIMEOUT, true, true);
                    //String name = operationDefinition.getName();
                    OperationResult result = operationFacet.invokeOperation(name, getTestOperationParameters(name));
                    System.out.println("Validating operation '" + name + "' result (" + result + ")...");
                    validateOperationResult(name, result, resource);
                }
            }
        }
        return;
    }

    protected void testResourceConfigLoad() throws Exception {
        if (supportsFacet(ConfigurationFacet.class) && getResourceType().getResourceConfigurationDefinition() != null) {
            ConfigurationManager configurationManager = PluginContainer.getInstance().getConfigurationManager();
            Set<Resource> resources = getResources();
            for (Resource resource : resources) {
                try {
                    Configuration resourceConfig = configurationManager.loadResourceConfiguration(resource.getId());
                    // TODO: Validate the properties?
                } catch (Exception e) {
                    throw new Exception("Failed to load Resource config for " + resource + ".", e);
                }
            }
        }
        return;
    }

    protected void testResourceConfigUpdate() throws Exception {
        Set<Resource> resources = getResources();
        testResourceConfigUpdate(resources);
    }

    protected void testResourceConfigUpdate(Set<Resource> resources) throws Exception {
        if (supportsFacet(ConfigurationFacet.class) && getResourceType().getResourceConfigurationDefinition() != null) {
            //Set<Resource> resources = getResources();
            for (Resource resource : resources) {
                try {
                    Configuration testResourceConfig = getTestResourceConfiguration();
                    Configuration resourceConfig = updateResourceConfiguration(resource, testResourceConfig);

                    assert resourceConfig.equals(testResourceConfig);
                } catch (Exception e) {
                    throw new Exception("Failed to update Resource config for " + resource + ".", e);
                }
            }
        }
        return;
    }

    protected abstract String getResourceTypeName();

    /**
     * Used in {@link #testResourceCreation()} and {@link #testResourceConfigUpdate()}
     * methods. The default implementation just throws UnsupportedOperationException.
     * 
     * @return the configuration to be used to create or update a resource.
     */
    protected Configuration getTestResourceConfiguration() {
        throw new UnsupportedOperationException();
    }

    protected static Set<Resource> getResources(ResourceType resourceType) {
        InventoryManager inventoryManager = PluginContainer.getInstance().getInventoryManager();
        return inventoryManager.getResourcesWithType(resourceType);
    }

    protected Set<Resource> getResources() {
        return getResources(getResourceType());
    }

    protected static ResourceType getResourceType(String resourceTypeName, String pluginName) {
        PluginManager pluginManager = PluginContainer.getInstance().getPluginManager();
        PluginMetadataManager pluginMetadataManager = pluginManager.getMetadataManager();
        return pluginMetadataManager.getType(resourceTypeName, pluginName);
    }

    protected ResourceType getResourceType() {
        return getResourceType(getResourceTypeName(), getPluginName());
    }

    @Deprecated
    protected ResourceType getServerResource() {
        PluginManager pluginManager = PluginContainer.getInstance().getPluginManager();
        PluginMetadataManager pluginMetadataManager = pluginManager.getMetadataManager();
        return pluginMetadataManager.getType("JBossAS Server", getPluginName());
    }

    protected Configuration getTestOperationParameters(String name) {
        OperationDefinition operationDefinition = getOperationDefinition(name);
        ConfigurationDefinition parametersConfigDef = operationDefinition.getParametersConfigurationDefinition();
        if (parametersConfigDef == null || parametersConfigDef.getPropertyDefinitions().isEmpty()) {
            return new Configuration();
        } else {
            // TODO: Return a Configuration auto-populated from the parameters ConfigurationDefinition.
            throw new IllegalStateException();
        }
    }

    protected void validateOperationResult(String name, OperationResult result, Resource resource) {
        return;
    }

    protected void validateNumericMetricValue(String metricName, Double value, Resource resource) {
        assert value != null;
    }

    protected void validateTraitMetricValue(String metricName, String value, Resource resource) {
        assert value != null;
    }

    protected static Configuration updateResourceConfiguration(Resource resource, Configuration newConfiguration)
        throws Exception {
        ConfigurationManager configurationManager = PluginContainer.getInstance().getConfigurationManager();
        ConfigurationUpdateRequest configurationUpdateRequest = new ConfigurationUpdateRequest(0, newConfiguration,
            resource.getId());
        configurationManager.updateResourceConfiguration(configurationUpdateRequest);

        //give the component and the managed resource some time to properly persist the update
        Thread.sleep(500);

        return configurationManager.loadResourceConfiguration(resource.getId());
    }

    protected static Configuration loadResourceConfiguration(Resource resource) throws Exception {
        ConfigurationManager configurationManager = PluginContainer.getInstance().getConfigurationManager();
        return configurationManager.loadResourceConfiguration(resource.getId());
    }

    private OperationDefinition getOperationDefinition(String name) {
        Set<OperationDefinition> operationDefinitions = getResourceType().getOperationDefinitions();
        OperationDefinition matchingOperationDefinition = null;
        for (OperationDefinition operationDefinition : operationDefinitions) {
            if (operationDefinition.getName().equals(name)) {
                matchingOperationDefinition = operationDefinition;
                break;
            }
        }
        return matchingOperationDefinition;
    }

    private boolean supportsFacet(Class<?> facetInterface) {
        PluginMetadataManager manager = PluginContainer.getInstance().getPluginManager().getMetadataManager();
        String componentClass = manager.getComponentClass(getResourceType());

        try {
            return facetInterface.isAssignableFrom(Class.forName(componentClass));
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Could not find component class " + componentClass, e);
        }
    }
}
