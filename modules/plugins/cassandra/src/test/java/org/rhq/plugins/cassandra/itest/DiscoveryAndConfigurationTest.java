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

package org.rhq.plugins.cassandra.itest;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.agent.configuration.ConfigurationUpdateRequest;
import org.rhq.core.clientapi.server.configuration.ConfigurationUpdateResponse;
import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.configuration.ConfigurationManager;
import org.rhq.core.pc.plugin.FileSystemPluginFinder;
import org.rhq.core.pc.plugin.PluginEnvironment;
import org.rhq.core.pc.plugin.PluginManager;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;

/**
 * @author Stefan Negrea
 *
 */
@Test(groups = { "integration" }, singleThreaded = true)
public class DiscoveryAndConfigurationTest {

    private static final int TYPE_HIERARCHY_DEPTH = 6;
    protected static final String PLUGIN_NAME = "Cassandra";

    private Log log = LogFactory.getLog(this.getClass());
    private ConfigurationManager configManager;


    protected int getTypeHierarchyDepth() {
        return TYPE_HIERARCHY_DEPTH;
    }

    protected String getPluginName() {
        return PLUGIN_NAME;
    }

    @BeforeSuite
    public void setupTestResources() {
        try {
            File pluginDir = new File("target/testsetup/plugins");
            PluginContainerConfiguration pcConfig = new PluginContainerConfiguration();
            pcConfig.setPluginFinder(new FileSystemPluginFinder(pluginDir));
            pcConfig.setPluginDirectory(pluginDir);

            pcConfig.setInsideAgent(false);
            PluginContainer.getInstance().setConfiguration(pcConfig);
            PluginContainer.getInstance().initialize();
            log.info("PC started.");
            for (String plugin : PluginContainer.getInstance().getPluginManager().getMetadataManager().getPluginNames()) {
                log.info("...Loaded plugin: " + plugin);
            }

            configManager = PluginContainer.getInstance().getConfigurationManager();
            configManager.initialize();
            Thread.sleep(10 * 1000L);

        } catch (Exception e) {
            log.info("Error initializing the context", e);
        }
    }

    @AfterSuite
    public void stopTestResources() {
        PluginContainer.getInstance().shutdown();
    }

    @Test
    public void pluginLoad() {
        PluginManager pluginManager = PluginContainer.getInstance().getPluginManager();
        PluginEnvironment pluginEnvironment = pluginManager.getPlugin(PLUGIN_NAME);
        assert (pluginEnvironment != null) : "Null environment, plugin not loaded";
        assert (pluginEnvironment.getPluginName().equals(PLUGIN_NAME));
    }

    @Test(dependsOnMethods = "pluginLoad")
    public void discoverResources() throws Exception {
        InventoryReport report = PluginContainer.getInstance().getInventoryManager().executeServerScanImmediately();
        Assert.assertNotNull(report);
        log.info("Discovery took: " + (report.getEndTime() - report.getStartTime()) + "ms");

        Thread.sleep(1000);

        report = PluginContainer.getInstance().getInventoryManager().executeServiceScanImmediately();
        Assert.assertNotNull(report);
        log.info("Discovery took: " + (report.getEndTime() - report.getStartTime()) + "ms");

        Set<String> ignoredResourceTypes = new HashSet<String>();
        ignoredResourceTypes.add("Cassandra Server JVM");

        Set<String> ignoredResourceNames = new HashSet<String>();
        ignoredResourceNames.add("system");

        Set<Resource> resources = findResourcesForTest(PluginContainer.getInstance().getInventoryManager().getPlatform(),
            ignoredResourceTypes, ignoredResourceNames);
        log.info("Found " + resources.size() + " Cassandra resources.");

        Assert.assertNotEquals(resources.size(), 0, "No cassandra or related instances found.");

        for (Object resource : resources.toArray()) {
            loadResourceMetrics((Resource) resource);
            loadUpdateResourceConfiguration((Resource) resource);
            executeResourceOperations((Resource) resource);
        }
    }

    private void loadUpdateResourceConfiguration(Resource resource) throws Exception {
        if (resource.getResourceType().getResourceConfigurationDefinition() != null
            && resource.getResourceType().getResourceConfigurationDefinition().getPropertyDefinitions().size() != 0) {
            try {
                Configuration configUnderTest = configManager.loadResourceConfiguration(resource.getId());

                ConfigurationUpdateRequest updateRequest = new ConfigurationUpdateRequest(1, configUnderTest,
                    resource.getId());
                ConfigurationUpdateResponse updateResponse = configManager
                    .executeUpdateResourceConfigurationImmediately(updateRequest);

                if (updateResponse == null) {
                    log.error("------------------------------");
                    log.error(resource);
                    log.error("Update Response is NULL!!!!");
                    log.error("------------------------------\n");
                }
                if (updateResponse.getErrorMessage() != null) {
                    log.error("------------------------------");
                    log.error(resource);
                    log.error(updateResponse.getErrorMessage());
                    log.error("------------------------------\n");
                }
            } catch (Exception e) {
                log.error(resource, e);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private void loadResourceMetrics(Resource resource) throws Exception {
        ResourceComponent resourceComponent = PluginContainer.getInstance().getInventoryManager()
            .getResourceComponent(resource);

        if (resourceComponent instanceof MeasurementFacet) {
            for (MeasurementDefinition def : resource.getResourceType().getMetricDefinitions()) {
                Set<MeasurementScheduleRequest> metricList = new HashSet<MeasurementScheduleRequest>();
                metricList.add(new MeasurementScheduleRequest(1, def.getName(), 1000, true, def.getDataType(), null));
                MeasurementReport report = new MeasurementReport();
                ((MeasurementFacet) resourceComponent).getValues(report, metricList);

                if (def.getDataType().equals(DataType.TRAIT)) {
                    MeasurementData data = report.getTraitData().iterator().next();
                    Assert.assertNotNull(data, "Unable to collect trait [" + def.getName() + "] on " + resource);
                    log.info("Measurement: " + def.getName() + "=" + data.getValue());
                } else if (def.getDataType().equals(DataType.MEASUREMENT)) {
                    MeasurementData data = report.getNumericData().iterator().next();
                    Assert.assertNotNull(data, "Unable to collect measurement [" + def.getName() + "] on " + resource);
                    log.info("Measurement: " + def.getName() + "=" + data.getValue());
                }
            }
        }
    }

    private void executeResourceOperations(Resource resource) throws Exception {
        String resourceTypeName = resource.getResourceType().getName();

        if (resourceTypeName.equals("CacheService")) {
            Configuration config = new Configuration();
            executeOperation(resource, "saveCaches", config);
            executeOperation(resource, "invalidateKeyCache", config);
            executeOperation(resource, "invalidateRowCache", config);
        } else if (resourceTypeName.equals("Keyspace")) {
            Configuration config = new Configuration();
            executeOperation(resource, "repair", config);
            executeOperation(resource, "compact", config);
            executeOperation(resource, "takeSnapshot", config);
        } else if (resourceTypeName.equals("ColumnFamily")) {
            Configuration config = new Configuration();
            executeOperation(resource, "repair", config);
            executeOperation(resource, "compact", config);
            executeOperation(resource, "takeSnapshot", config);
            executeOperation(resource, "disableAutoCompaction", config);
        }
    }

    @SuppressWarnings("rawtypes")
    private void executeOperation(Resource resource, String operationName, Configuration config) throws Exception {
        ResourceComponent resourceComponent = PluginContainer.getInstance().getInventoryManager()
            .getResourceComponent(resource);

        OperationResult result = ((OperationFacet) resourceComponent).invokeOperation(operationName, config);

        if (result != null) {
            log.info("Result of operation " + operationName + " was: " + result.getSimpleResult());
            if (result.getErrorMessage() != null) {
                Assert.fail("Operation execution failed");
            }
        }
    }

    private Set<Resource> findResourcesForTest(Resource parent, Set<String> ignoredResourceTypes,
        Set<String> ignoredResourceNames) {
        Set<Resource> foundResources = new HashSet<Resource>();

        Queue<Resource> discoveryQueue = new LinkedList<Resource>();
        discoveryQueue.add(parent);

        while (!discoveryQueue.isEmpty()) {
            Resource currentResource = discoveryQueue.poll();

            if (ignoredResourceTypes.contains(currentResource.getResourceType().getName())
                || ignoredResourceNames.contains(currentResource.getName())) {
                continue;
            }

            log.info("Discovered resource of type: " + currentResource.getResourceType().getName());
            if (currentResource.getResourceType().getPlugin().equals(PLUGIN_NAME)) {
                foundResources.add(currentResource);
            }

            if (currentResource.getChildResources() != null) {
                for (Resource child : currentResource.getChildResources()) {
                    discoveryQueue.add(child);
                }
            }
        }

        return foundResources;
    }
}