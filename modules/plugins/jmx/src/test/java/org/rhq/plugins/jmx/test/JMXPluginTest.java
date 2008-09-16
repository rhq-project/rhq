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
package org.rhq.plugins.jmx.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.rhq.core.domain.discovery.InventoryReport;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.plugin.FileSystemPluginFinder;
import org.rhq.core.pc.plugin.PluginEnvironment;
import org.rhq.core.pc.plugin.PluginManager;
import org.rhq.core.pc.util.InventoryPrinter;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;

/**
 * Integration test for the JMX plugin.
 *
 * @author Greg Hinkle
 */
public class JMXPluginTest {
    static final String PROGRAM_CLASS = "org.rhq.plugins.jmx.test.JMXPluginTest$TestProgram";
    static final String MONITORABLE = "-Dcom.sun.management.jmxremote.port=9921 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false";
    static final String MBEAN_SERVER_ADDRESS = "service:jmx:rmi:///jndi/rmi://localhost:9921/jmxrmi";

    static final String PROJECT_ARTIFACT_ID_SYSPROP = "project.artifactId";
    static final String PROJECT_VERSION_SYSPROP = "project.version";

    private Process testProgram;
    private InventoryManager inventoryManager;
    private static final String PLUGIN_NAME = "JMX";

    @BeforeSuite
    public void start() {
        try {
            String javaHome = System.getProperty("java.home");
            String javaCmd = javaHome + "/bin/java";

            ProcessBuilder processBuilder = new ProcessBuilder(javaCmd, "-cp", "target/test-classes",
                "-Dcom.sun.management.jmxremote.port=9921", "-Dcom.sun.management.jmxremote.ssl=false",
                "-Dcom.sun.management.jmxremote.authenticate=false", PROGRAM_CLASS);
            processBuilder.redirectErrorStream(true);
            testProgram = processBuilder.start();

            OutputReader or = new OutputReader(testProgram.getInputStream());
            Thread ort = new Thread(or);
            ort.setDaemon(true);
            ort.start();

            // Give it time to start
            Thread.sleep(2000);

            File pluginDir = new File("target/itest/plugins");
            PluginContainerConfiguration pcConfig = new PluginContainerConfiguration();
            pcConfig.setPluginFinder(new FileSystemPluginFinder(pluginDir));
            pcConfig.setPluginDirectory(pluginDir);
            pcConfig.setInsideAgent(false);

            PluginContainer.getInstance().setConfiguration(pcConfig);
            PluginContainer.getInstance().initialize();
            System.out.println("PC Started");
            for (String plugin : PluginContainer.getInstance().getPluginManager().getMetadataManager().getPluginNames()) {
                System.out.println("PLUGIN: " + plugin);
            }

            this.inventoryManager = PluginContainer.getInstance().getInventoryManager();
        } catch (Throwable t) {
            // Catch RuntimeExceptions and Errors and dump their stack trace, because Surefire will completely swallow them
            // and throw a cryptic NPE (see http://jira.codehaus.org/browse/SUREFIRE-157)!
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    @AfterSuite
    public void stop() {
        PluginContainer.getInstance().shutdown();
        testProgram.destroy();
    }

    @Test
    public void testPluginLoad() {
        PluginManager pluginManager = PluginContainer.getInstance().getPluginManager();
        PluginEnvironment pluginEnvironment = pluginManager.getPlugin(PLUGIN_NAME);
        assert pluginEnvironment != null : "Null environment, plugin not loaded";
        assert (pluginEnvironment.getPluginName().equals(PLUGIN_NAME));
    }

    @Test(dependsOnMethods = "testPluginLoad")
    public void testServerDiscovery() throws Exception {
        InventoryReport report = PluginContainer.getInstance().getInventoryManager().executeServerScanImmediately();
        assert report != null;
        System.out.println("Discovery took: " + (report.getEndTime() - report.getStartTime()) + "ms");
        Resource platform = PluginContainer.getInstance().getInventoryManager().getPlatform();
        Set<Resource> servers = platform.getChildResources();
        System.out.println("Found " + servers.size() + " servers");
    }

    @Test(dependsOnMethods = "testServerDiscovery")
    public void testServiceDiscovery() throws Exception {
        InventoryReport report = PluginContainer.getInstance().getInventoryManager().executeServiceScanImmediately();
        assert report != null;
        Resource platform = PluginContainer.getInstance().getInventoryManager().getPlatform();

        assert platform != null;
        Set<Resource> childResources = platform.getChildResources();
        assert childResources != null;

        /*System.out.println("RUNTIME SERVERS: " + childResources.size());
         * for (Resource server : platform.getChildResources()) { System.out.println("Server: " + server.toString());
         * System.out.println("Found with " + server.getChildResources().size() + " child services");}*/
        InventoryPrinter.outputInventory(new PrintWriter(System.out), false);
    }

    @Test(dependsOnMethods = "testServerDiscovery")
    public void testNumberOfServers() throws Exception {
        InventoryReport report = PluginContainer.getInstance().getInventoryManager().executeServiceScanImmediately();
        assert report != null;
        Resource platform = PluginContainer.getInstance().getInventoryManager().getPlatform();

        assert platform != null;
        Set<Resource> childResources = platform.getChildResources();
        assert childResources != null;

        // TODO GH: ccrouch... why would this be two now?
        //assert childResources.size() == 2 : "Not all Server instances were found.";
    }

    @Test(dependsOnMethods = "testServiceDiscovery")
    public void testMeasurement() throws Exception {
        Resource platform = PluginContainer.getInstance().getInventoryManager().getPlatform();
        for (Resource server : platform.getChildResources()) {
            List<Resource> services = new ArrayList<Resource>(server.getChildResources());
            Collections.sort(services);
            for (Resource service : services) {
                ResourceComponent serviceComponent = PluginContainer.getInstance().getInventoryManager()
                    .getResourceComponent(service);
                if (serviceComponent instanceof MeasurementFacet) {
                    Set<MeasurementScheduleRequest> metricList = new HashSet<MeasurementScheduleRequest>();
                    metricList.add(new MeasurementScheduleRequest(1, "FreePhysicalMemorySize", 1000, true,
                        DataType.MEASUREMENT));
                    MeasurementReport report = new MeasurementReport();

                    if (serviceComponent.getAvailability().equals(AvailabilityType.UP)) {
                        ((MeasurementFacet) serviceComponent).getValues(report, metricList);
                        for (MeasurementData value : report.getNumericData()) {
                            System.out.println(value.getValue() + ":" + service.getName());
                        }
                    }
                }
            }
        }
    }

    @Test(dependsOnMethods = "testServiceDiscovery")
    public void testOperation() throws Exception {
        // TODO GH: Test only runs when tested on JDK 6 as the JVM services aren't detected
        // currently for the test platform
        Resource platform = PluginContainer.getInstance().getInventoryManager().getPlatform();
        for (Resource server : platform.getChildResources()) {
            List<Resource> services = new ArrayList<Resource>(server.getChildResources());
            Collections.sort(services);
            for (Resource service : services) {
                if (service.getResourceType().getName().equals("Threading")) {
                    ResourceComponent serviceComponent = PluginContainer.getInstance().getInventoryManager()
                        .getResourceComponent(service);

                    Object result = ((OperationFacet) serviceComponent).invokeOperation("findMonitorDeadlockedThreads", null);
                    System.out.println("Result of operation test was: " + result);
                }
            }
        }
    }

    /*
     * @Test(dependsOnMethods = "testServiceDiscovery") public void testNonMeasurement()  throws Exception {   Resource
     * platform = PluginContainer.getInstance().getInventoryManager().getPlatform();   for (Resource server :
     * platform.getChildResources())   {      List<Resource> services = new
     * ArrayList<Resource>(server.getChildResources());      Collections.sort(services);      for (Resource service :
     * services)      {         ResourceComponent serviceComponent           =
     * PluginContainer.getInstance().getInventoryManager().getResourceComponent(service);
     *
     *       ResourceType resourceType = service.getResourceType();
     *
     *       MeasurementAgentService measurementAgentService =
     * PluginContainer.getInstance().getMeasurementAgentService();
     *
     *       Set<MeasurementDefinition> metricDefinitions = resourceType.getMetricDefinitions();
     *
     *       if (!metricDefinitions.isEmpty())         {            System.out.println(service.getName());
     * Set<String> measurementNames = new HashSet<String>();            for (MeasurementDefinition md :
     * metricDefinitions)            {               System.out.println(md.getName());
     * measurementNames.add(md.getName());            }
     *
     *          // get the actual measurement data for those measurement names            int resourceId =
     * service.getId();            String[] measurementNamesArray = measurementNames.toArray(new String[]{});
     *
     *          List<MeasurementData> summaryMeasurementDataList = new ArrayList<MeasurementData>(
     * measurementAgentService.getRealTimeMeasurementValue(resourceId, measurementNamesArray));
     *
     *          if (serviceComponent.getAvailability().equals(AvailabilityType.UP))            {               for
     * (MeasurementData value : summaryMeasurementDataList)               {
     * System.out.println(value.getValue() + ":" + value.getName());               }            }
     *
     *          assert measurementNames.size() == summaryMeasurementDataList.size()              : "Number of
     * measurements defined did not match number of results retrieved";         }      }
     *
     * } }
     */

    public static class OutputReader implements Runnable {
        InputStream inputStream;

        public OutputReader(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        public void run() {
            try {
                BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = r.readLine()) != null) {
                    System.out.println("__" + line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static class TestProgram implements Runnable {
        long started = System.currentTimeMillis();

        public static void main(String[] args) {
            System.out.println("Test Program Running");
            TestProgram tp = new TestProgram();
            tp.run();
        }

        public void run() {
            while (true) {
                System.out.println("Test program running for " + (System.currentTimeMillis() - started) + "ms");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                }
            }
        }
    }
}