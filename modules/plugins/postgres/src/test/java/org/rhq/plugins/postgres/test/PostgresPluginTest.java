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

package org.rhq.plugins.postgres.test;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.domain.measurement.DataType;
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
import org.rhq.core.pc.util.InventoryPrinter;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;

/**
 * @author Greg Hinkle
 */
@Test(groups = "postgres.plugin")
public class PostgresPluginTest {
    private Log log = LogFactory.getLog(PostgresPluginTest.class);
    private static final String PLUGIN_NAME = "Postgres";

    @BeforeSuite
    public void start() {
        try {
            File pluginDir = new File("target/itest/plugins");
            PluginContainerConfiguration pcConfig = new PluginContainerConfiguration();
            pcConfig.setPluginFinder(new FileSystemPluginFinder(pluginDir));
            pcConfig.setPluginDirectory(pluginDir);

            pcConfig.setInsideAgent(false);
            PluginContainer.getInstance().setConfiguration(pcConfig);
            PluginContainer.getInstance().initialize();
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
    public void testServerDiscovery() throws Exception {
        InventoryReport report = PluginContainer.getInstance().getInventoryManager().executeServerScanImmediately();
        assert report != null;
        System.out.println("Discovery took: " + (report.getEndTime() - report.getStartTime()) + "ms");

        Set<Resource> pgServers = getDiscoveredServers();

        //assert pgSer vers.size() > 0;
        System.out.println("Found " + pgServers.size() + " postgres servers");
    }

    @Test(dependsOnMethods = "testServerDiscovery", enabled = false)
    public void testServerMeasurement() throws Exception {
        for (Resource server : getDiscoveredServers()) {
            testResourceMeasurement(server);
        }
    }

    private Set<Resource> getDiscoveredServers() {
        Set<Resource> found = new HashSet<Resource>();
        Resource platform = PluginContainer.getInstance().getInventoryManager().getPlatform();
        for (Resource resource : platform.getChildResources()) {
            if (resource.getResourceType().getName().equals("Postgres Server")) {
                found.add(resource);
            }
        }

        return found;
    }

    @Test(enabled = false)
    // TODO GH: Disable until we fix the natives integration for sigar
    private void testResourceMeasurement(Resource resource) throws Exception {
        ResourceComponent resourceComponent = PluginContainer.getInstance().getInventoryManager().getResourceComponent(
            resource);
        if (resourceComponent instanceof MeasurementFacet) {
            for (MeasurementDefinition def : resource.getResourceType().getMetricDefinitions()) {
                Set<MeasurementScheduleRequest> metricList = new HashSet<MeasurementScheduleRequest>();
                metricList.add(new MeasurementScheduleRequest(1, def.getName(), 1000, true, def.getDataType()));
                MeasurementReport report = new MeasurementReport();
                ((MeasurementFacet) resourceComponent).getValues(report, metricList);

                assert report.getNumericData().size() > 0 : "Measurement " + def.getName() + " not collected from "
                    + resource;
                MeasurementData data = report.getNumericData().iterator().next();
                assert data != null : "Unable to collect metric [" + def.getName() + "] on " + resource;
                System.out.println("Measurement: " + def.getName() + "=" + data.getValue());
            }
        }
    }

    @Test(dependsOnMethods = "testServerDiscovery")
    public void testServiceDiscovery() throws Exception {
        try {
            InventoryReport report = PluginContainer.getInstance().getInventoryManager()
                .executeServiceScanImmediately();
        } catch (Exception e) {
            log.error("Failure to run discovery", e);
            throw e;
        }

        InventoryPrinter.outputInventory(new PrintWriter(System.out), false);
    }

    @Test(dependsOnMethods = "testServiceDiscovery")
    public void testDatabaseMeasurement() throws Exception {
        Resource platform = PluginContainer.getInstance().getInventoryManager().getPlatform();
        for (Resource server : platform.getChildResources()) {
            List<Resource> services = new ArrayList<Resource>(server.getChildResources());
            Collections.sort(services);
            for (Resource service : services) {
                ResourceComponent serviceComponent = PluginContainer.getInstance().getInventoryManager()
                    .getResourceComponent(service);
                if (serviceComponent instanceof MeasurementFacet) {
                    Set<MeasurementScheduleRequest> metricList = new HashSet<MeasurementScheduleRequest>();
                    metricList.add(new MeasurementScheduleRequest(1, "numbackends", 1000, true, DataType.MEASUREMENT));
                    MeasurementReport report = new MeasurementReport();
                    ((MeasurementFacet) serviceComponent).getValues(report, metricList);
                    for (MeasurementData value : report.getNumericData()) {
                        System.out.println(value.getValue() + ":" + service.getName());
                    }
                }
            }
        }
    }
}
