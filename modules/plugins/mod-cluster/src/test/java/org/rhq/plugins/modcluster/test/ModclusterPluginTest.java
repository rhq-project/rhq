/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.plugins.modcluster.test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.plugin.FileSystemPluginFinder;
import org.rhq.core.pc.plugin.PluginEnvironment;
import org.rhq.core.pc.plugin.PluginManager;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;

/**
 * @author Stefan Negrea
 */
@Test(groups = "modcluster-plugin")
public class ModclusterPluginTest {
    private Log log = LogFactory.getLog(this.getClass());
    private static final String PLUGIN_NAME = "mod_cluster";

    @BeforeSuite
    public void start() {
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @AfterSuite
    public void stop() {
        PluginContainer.getInstance().shutdown();
    }

    @Test
    public void testPluginLoad() {
        PluginManager pluginManager = PluginContainer.getInstance().getPluginManager();
        PluginEnvironment pluginEnvironment = pluginManager.getPlugin(PLUGIN_NAME);
        assert (pluginEnvironment != null) : "Null environment, plugin not loaded";
        assert (pluginEnvironment.getPluginName().equals(PLUGIN_NAME));

    }

    @Test(dependsOnMethods = "testPluginLoad")
    public void testDiscovery() throws Exception {
        InventoryReport report = PluginContainer.getInstance().getInventoryManager().executeServerScanImmediately();
        assert report != null;
        System.out.println("Discovery took: " + (report.getEndTime() - report.getStartTime()) + "ms");

        report = PluginContainer.getInstance().getInventoryManager().executeServiceScanImmediately();
        assert report != null;
        System.out.println("Discovery took: " + (report.getEndTime() - report.getStartTime()) + "ms");

        List<String> typeNames = new ArrayList<String>() {
            {
                add(PLUGIN_NAME);
                add(PLUGIN_NAME + "_context");
            }
        };

        Set<Resource> resources = findResource(PluginContainer.getInstance().getInventoryManager().getPlatform(),
            typeNames);
        log.info("Found " + resources.size() + " mod_cluster and mod_cluster_context instance(s).");

        assert (resources.size() != 0) : "No mod_cluster or related instances found.";

        if (resources.size() != 0) {
            for (Object objectResource : resources.toArray()) {
                Resource resource = (Resource) objectResource;
                if (resource.getResourceType().getName().equals("mod_cluster")) {
                    testResourceMeasurement(resource);
                } else {
                    testContextOperations(resource);
                }
            }
        }
    }

    private void testResourceMeasurement(Resource resource) throws Exception {
        ResourceComponent resourceComponent = PluginContainer.getInstance().getInventoryManager()
            .getResourceComponent(resource);
        if (resourceComponent instanceof MeasurementFacet) {
            for (MeasurementDefinition def : resource.getResourceType().getMetricDefinitions()) {
                Set<MeasurementScheduleRequest> metricList = new HashSet<MeasurementScheduleRequest>();
                metricList.add(new MeasurementScheduleRequest(1, def.getName(), 1000, true, def.getDataType(), null));
                MeasurementReport report = new MeasurementReport();
                ((MeasurementFacet) resourceComponent).getValues(report, metricList);

                MeasurementData data = report.getTraitData().iterator().next();
                assert data != null : "Unable to collect trait [" + def.getName() + "] on " + resource;
                log.info("Measurement: " + def.getName() + "=" + data.getValue());
            }
        }

        if (resourceComponent instanceof OperationFacet) {
            OperationResult result = ((OperationFacet) resourceComponent).invokeOperation("reset", null);
            log.info("Result of operation test was: " + result);

            result = ((OperationFacet) resourceComponent).invokeOperation("disable", null);
            log.info("Result of operation test was: " + result.getSimpleResult());
        }
    }

    private void testContextOperations(Resource resource) throws Exception {
        ResourceComponent resourceComponent = PluginContainer.getInstance().getInventoryManager()
            .getResourceComponent(resource);

        if (resourceComponent instanceof OperationFacet) {
            try {
                OperationResult result = ((OperationFacet) resourceComponent).invokeOperation("enableContext", null);
                log.info("Result of operation " + "enableContext" + " was: " + result.getSimpleResult());
            } catch (Exception e) {
                log.info("Operation failed");
            }
            /*result = ((OperationFacet) resourceComponent).invokeOperation("disable", null);
            log.info("Result of operation test was: " + result.getSimpleResult());*/
        }
    }

    private Set<Resource> findResource(Resource parent, List<String> typeNames) {
        Set<Resource> found = new HashSet<Resource>();

        Queue<Resource> discoveryQueue = new LinkedList<Resource>();
        discoveryQueue.add(parent);

        while (!discoveryQueue.isEmpty()) {
            Resource currentResource = discoveryQueue.poll();

            log.info("Discovered resource of type: " + currentResource.getResourceType().getName());
            if (typeNames.contains(currentResource.getResourceType().getName())) {
                found.add(currentResource);
            }

            if (currentResource.getChildResources() != null) {
                for (Resource child : currentResource.getChildResources()) {
                    discoveryQueue.add(child);
                }
            }
        }

        return found;
    }
}
