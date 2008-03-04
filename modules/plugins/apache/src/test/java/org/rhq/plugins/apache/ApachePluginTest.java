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
package org.rhq.plugins.apache;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.rhq.core.domain.discovery.InventoryReport;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementData;
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

/**
 * The unit tests for the JON Apache plugin.
 *
 * @author Ian Springer
 */
public class ApachePluginTest {
    @BeforeSuite
    public void start() {
        //System.out.println("java.class.path=" + System.getProperty("java.class.path"));
        //System.out.println("java.library.path=" + System.getProperty("java.library.path"));
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
        PluginEnvironment pluginEnvironment = pluginManager.getPlugin("Apache");
        assert (pluginEnvironment != null) : "Null environment, plugin not loaded";
        System.out.println("Plugin package: " + pluginEnvironment.getDescriptor().getPackage());
    }

    @Test(dependsOnMethods = "testPluginLoad")
    public void testServerDiscovery() throws Exception {
        try {
            InventoryReport report = PluginContainer.getInstance().getInventoryManager().executeServerScanImmediately();
            assert report != null;
            System.out.println("Discovery took: " + (report.getEndTime() - report.getStartTime()) + "ms");
            Resource platform = PluginContainer.getInstance().getInventoryManager().getPlatform();
            Set<Resource> servers = platform.getChildResources();
            assert servers != null;
            System.out.println("Found " + servers.size() + " servers");
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Test(dependsOnMethods = "testServerDiscovery")
    public void testServiceDiscovery() throws Exception {
        try {
            InventoryReport report = PluginContainer.getInstance().getInventoryManager()
                .executeServiceScanImmediately();
            Resource platform = PluginContainer.getInstance().getInventoryManager().getPlatform();
            System.out.println("RUNTIME SERVERS: " + platform.getChildResources().size());
            for (Resource server : platform.getChildResources()) {
                System.out.println("Server: " + server.toString());
                System.out.println("Found with " + server.getChildResources().size() + " child services");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Test(dependsOnMethods = "testServiceDiscovery")
    public void testMeasurementComponent() throws Exception {
        Resource platform = PluginContainer.getInstance().getInventoryManager().getPlatform();
        for (Resource server : platform.getChildResources()) {
            List<Resource> services = new ArrayList<Resource>(server.getChildResources());
            Collections.sort(services);
            for (Resource service : services) {
                ResourceComponent serviceComponent = PluginContainer.getInstance().getInventoryManager()
                    .getResourceComponent(service);
                if (serviceComponent instanceof MeasurementFacet) {
                    Set<MeasurementScheduleRequest> metricList = new HashSet<MeasurementScheduleRequest>();
                    metricList.add(new MeasurementScheduleRequest(1, "wwwSummaryInRequests", 1000, true,
                        DataType.MEASUREMENT));
                    metricList.add(new MeasurementScheduleRequest(2, "wwwRequestInRequests.GET", 1000, true,
                        DataType.MEASUREMENT));
                    MeasurementReport report = new MeasurementReport();
                    ((MeasurementFacet) serviceComponent).getValues(report, metricList);
                    for (MeasurementData data : report.getNumericData()) {
                        System.out.println(data.getValue() + ":" + service.getName());
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        ApachePluginTest pluginTest = new ApachePluginTest();
        pluginTest.start();
    }
}