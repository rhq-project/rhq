/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
package org.rhq.plugins.jmx.test;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
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
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.jmx.JMXDiscoveryComponent;
import org.rhq.plugins.jmx.util.JvmResourceKey;

/**
 * Integration test for the JMX plugin.
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class JMXPluginTest {

    private static final int JMX_REMOTING_PORT1 = 9921;
    private static final int JMX_REMOTING_PORT2 = 9922;

    private static final String PLUGIN_NAME = "JMX";
    private static final String SERVER_TYPE_NAME = "JMX Server";
    private static final String EXPLICIT_RESOURCE_KEY1 = "foo1";
    private static final String EXPLICIT_RESOURCE_KEY2 = "foo2";

    private List<Process> testServerJvms = new ArrayList<Process>();

    private InventoryManager inventoryManager;

    @BeforeSuite
    public void start() {
        try {
            // Start the test server JVMs.
            this.testServerJvms.add(startTestServerJvm("-Dcom.sun.management.jmxremote.port=" + JMX_REMOTING_PORT1,
                "-Dcom.sun.management.jmxremote.ssl=false", "-Dcom.sun.management.jmxremote.authenticate=false"));

            // FIXME: Disabled until we find a fix for Sigar getProcCredName issue
            //            this.testServerJvms.add(startTestServerJvm("-D" + JMXDiscoveryComponent.SYSPROP_RHQ_RESOURCE_KEY + "="
            //                + EXPLICIT_RESOURCE_KEY1));

            this.testServerJvms.add(startTestServerJvm("-Dcom.sun.management.jmxremote.port=" + JMX_REMOTING_PORT2,
                "-Dcom.sun.management.jmxremote.ssl=false", "-Dcom.sun.management.jmxremote.authenticate=false", "-D"
                    + JMXDiscoveryComponent.SYSPROP_RHQ_RESOURCE_KEY + "=" + EXPLICIT_RESOURCE_KEY2));

            // Give them time to fully start.
            Thread.sleep(3000);

            File pluginDir = new File("target/itest/plugins");
            deployCustomPlugin(pluginDir);
            PluginContainerConfiguration pcConfig = new PluginContainerConfiguration();
            pcConfig.setPluginFinder(new FileSystemPluginFinder(pluginDir));
            pcConfig.setPluginDirectory(pluginDir);
            pcConfig.setInsideAgent(false);

            PluginContainer.getInstance().setConfiguration(pcConfig);
            PluginContainer.getInstance().initialize();

            Set<String> pluginNames = PluginContainer.getInstance().getPluginManager().getMetadataManager()
                .getPluginNames();
            System.out.println("PC started with plugins " + pluginNames + ".");

            this.inventoryManager = PluginContainer.getInstance().getInventoryManager();
        } catch (Throwable t) {
            // Catch RuntimeExceptions and Errors and dump their stack trace, because Surefire will completely swallow them
            // and throw a cryptic NPE (see http://jira.codehaus.org/browse/SUREFIRE-157)!
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    private void deployCustomPlugin(File targetDir) throws Exception{

        File plugin = new File("src/test/resources/custom-test-plugin.xml");
        File targetFile = new File(targetDir,"custom-test-plugin.jar");

        ZipOutputStream outputStream = new ZipOutputStream(new FileOutputStream(targetFile));
        ZipEntry metainf = new ZipEntry("META-INF");
        outputStream.putNextEntry(metainf);
        outputStream.closeEntry();

        ZipEntry pluginXml = new ZipEntry("META-INF/rhq-plugin.xml");
        outputStream.putNextEntry(pluginXml);
        FileInputStream fis = new FileInputStream(plugin);
        int bufferSize = 1024;
        BufferedInputStream bufferedInputStream = new BufferedInputStream(fis, bufferSize);
        int size = -1;
        byte data[] = new byte[bufferSize];
        while(  (size = bufferedInputStream.read(data, 0, bufferSize)) != -1  )
        {
            outputStream.write(data, 0, size);
        }
        bufferedInputStream.close();
        outputStream.closeEntry();
        outputStream.flush();
        outputStream.finish();
    }

    private Process startTestServerJvm(String... jvmArgs) throws IOException {
        String javaHome = System.getProperty("java.home");
        String javaCmd = javaHome + "/bin/java";

        List<String> args = new ArrayList<String>();
        args.add(javaCmd);
        args.add("-cp");
        args.add("target/test-classes");
        args.addAll(Arrays.asList(jvmArgs));
        args.add(TestProgram.class.getName());

        ProcessBuilder processBuilder = new ProcessBuilder(args);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        OutputReader outputReader = new OutputReader(process.getInputStream());
        Thread outputReaderThread = new Thread(outputReader);
        outputReaderThread.setDaemon(true);
        outputReaderThread.start();

        return process;
    }

    @AfterSuite
    public void stop() {
        PluginContainer.getInstance().shutdown();

        for (Process process : this.testServerJvms) {
            process.destroy();
        }
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

        Resource platform = PluginContainer.getInstance().getInventoryManager().getPlatform();

        Set<Resource> jmxServers = getChildResourcesOfType(platform, new ResourceType(SERVER_TYPE_NAME, PLUGIN_NAME,
            ResourceCategory.SERVER, null));
        System.out.println("Found " + jmxServers.size() + " JMX Servers:");

        boolean foundJmxRemotingServer = false;
        boolean foundExplicitKey1Server = false;
        boolean foundExplicitKey2Server = false;
        for (Resource jmxServer : jmxServers) {
            System.out.println("  * " + jmxServer);
            JvmResourceKey key = JvmResourceKey.valueOf(jmxServer.getResourceKey());
            switch (key.getType()) {
            case Explicit:
                if (key.getExplicitValue().equals(EXPLICIT_RESOURCE_KEY1)) {
                    assert key.getMainClassName().equals(TestProgram.class.getName());
                    foundExplicitKey1Server = true;
                } else if (key.getExplicitValue().equals(EXPLICIT_RESOURCE_KEY2)) {
                    assert key.getMainClassName().equals(TestProgram.class.getName());
                    foundExplicitKey2Server = true;
                }
                break;
            case JmxRemotingPort:
                if (key.getMainClassName().equals(TestProgram.class.getName())
                    && key.getJmxRemotingPort().equals(JMX_REMOTING_PORT1)) {
                    assert key.getMainClassName().equals(TestProgram.class.getName());
                    foundJmxRemotingServer = true;
                }
                break;
            default:
                throw new IllegalStateException("Unsupported key type: " + key.getType());
            }
        }
        assert foundJmxRemotingServer : "JMX Remoting server not found.";
        // FIXME: Disabled until we find a fix for Sigar getProcCredName issue
        //assert foundExplicitKey1Server : "Explicit key server not found.";
        assert foundExplicitKey2Server : "JMX Remoting + explicit key server not found.";
    }

    @Test(dependsOnMethods = "testServerDiscovery")
    public void testServiceDiscovery() throws Exception {
        InventoryReport report = PluginContainer.getInstance().getInventoryManager().executeServiceScanImmediately();
        assert report != null;
        Resource platform = PluginContainer.getInstance().getInventoryManager().getPlatform();

        assert platform != null;

        Set<Resource> jmxServers = getChildResourcesOfType(platform, new ResourceType(SERVER_TYPE_NAME, PLUGIN_NAME,
            ResourceCategory.SERVER, null));

        for (Resource jmxServer : jmxServers) {
            Set<Resource> childResources = jmxServer.getChildResources();
            // Each JMX Server should have exactly six singleton child Resources with the following types:
            // Operating System, Threading, VM Class Loading System, VM Compilation System, VM Memory System, and
            // java.util.logging.
            assert childResources.size() == 7 : jmxServer + " does not have 7 child Resources but " + childResources.size()
                + " - child Resources: " + childResources;
        }

        InventoryPrinter.outputInventory(new PrintWriter(System.out), false);
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
                    if ("VM Memory System".equals(service.getResourceType().getName())) {
                        metricList.add(new MeasurementScheduleRequest(2, "Calculated.HeapUsagePercentage", 1000, true,
                            DataType.MEASUREMENT));
                    }
                    MeasurementReport report = new MeasurementReport();

                    if (serviceComponent.getAvailability().equals(AvailabilityType.UP)) {
                        ((MeasurementFacet) serviceComponent).getValues(report, metricList);
                        for (MeasurementData value : report.getNumericData()) {
                            System.out.println(value.getValue() + ":" + value.getName());
                        }
                    }
                }
            }
        }
    }

    @Test(dependsOnMethods = "testServiceDiscovery")
    public void testOperation() throws Exception {
        Resource platform = PluginContainer.getInstance().getInventoryManager().getPlatform();
        boolean found = false;
        for (Resource server : platform.getChildResources()) {
            List<Resource> services = new ArrayList<Resource>(server.getChildResources());
            Collections.sort(services);
            for (Resource service : services) {
                if (service.getResourceType().getName().equals("Threading")) {
                    ResourceComponent serviceComponent = PluginContainer.getInstance().getInventoryManager()
                        .getResourceComponent(service);
                    found = true;

                    OperationResult result = ((OperationFacet) serviceComponent).invokeOperation("findMonitorDeadlockedThreads",
                        new Configuration());
                    System.out.println("Result of operation test was: " + result);
                    if (result.getErrorMessage()!=null) {
                        throw new RuntimeException("Operation was no success: " + result.toString());
                    }
                }
            }
        }
        if (!found) {
            throw new RuntimeException("Did not find the requested operation");
        }
    }

    @Test(dependsOnMethods = "testServiceDiscovery")
    public void testOperation1() throws Exception {
        runOperation("Threading", "findMonitorDeadlockedThreads", new Configuration());
    }

    @Test(dependsOnMethods = "testServiceDiscovery")
    public void testOperation2() throws Exception {
        String result = runOperation("TestService_", "doSomething", new Configuration());
        assert result == null; // Operation did not define a "<results>" block.
    }

    @Test(dependsOnMethods = "testServiceDiscovery")
    public void testOperation3() throws Exception {
        String result = runOperation("TestService_", "hello", new Configuration());
        assert "Hello World".equals(result);
    }

    @Test(dependsOnMethods = "testServiceDiscovery")
    public void testOperation4() throws Exception {
        Configuration parameters = new Configuration();
        parameters.put(new PropertySimple("p1","Hello Test"));
        String result = runOperation("TestService_", "echo", parameters);
        assert "Hello Test".equals(result);
    }

    @Test(dependsOnMethods = "testServiceDiscovery")
    public void testOperation5() throws Exception {
        Configuration parameters = new Configuration();
        parameters.put(new PropertySimple("p1","Hello"));
        parameters.put(new PropertySimple("p2","Test"));
        String result = runOperation("TestService_", "concat", parameters);
        assert "HelloTest".equals(result);
    }



    private String runOperation(String typeName, String operationName, Configuration parameters) throws Exception {
        Resource platform = PluginContainer.getInstance().getInventoryManager().getPlatform();
        boolean found = false;
        String outcome = null;
        for (Resource server : platform.getChildResources()) {
            List<Resource> services = new ArrayList<Resource>(server.getChildResources());
            Collections.sort(services);
            for (Resource service : services) {
                if (service.getResourceType().getName().equals(typeName)) {
                    ResourceComponent serviceComponent = PluginContainer.getInstance().getInventoryManager()
                        .getResourceComponent(service);
                    found = true;

                    OperationResult result = ((OperationFacet) serviceComponent).invokeOperation(operationName,
                        parameters);
                    System.out.println("Result of operation test was: " + result);
                    if (result!=null) {
                        // Operations are not required to return anything
                        if (result.getErrorMessage() != null) {
                            throw new RuntimeException("Operation was no success: " + result.toString());
                        }
                        outcome = result.getSimpleResult();
                    }
                }
            }
        }
        if (!found) {
            throw new RuntimeException("Did not find the requested operation");
        }

        return outcome;
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

    private static Set<Resource> getChildResourcesOfType(Resource platform, ResourceType resourceType) {
        Set<Resource> childResources = platform.getChildResources();
        Set<Resource> results = new HashSet<Resource>();
        for (Resource resource : childResources) {
            ResourceType childResourceType = resource.getResourceType();
            if (childResourceType.getPlugin().equals(resourceType.getPlugin())
                && childResourceType.getName().equals(resourceType.getName())) {
                results.add(resource);
            }
        }
        return results;
    }

    public static class OutputReader implements Runnable {
        InputStream inputStream;

        public OutputReader(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        public void run() {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
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
            final ServerSocket serverSocket;
            try {
                serverSocket = new ServerSocket(0);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

            // Deploy an MBean of our own to run additional tests with a custom-jmx-plugin
            try {
                ObjectName mBeanName = new ObjectName("rhq.test:name=TestTarget");
                mBeanServer.createMBean(TestTarget.class.getName(),mBeanName);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            String jvmName = runtimeMXBean.getName();
            int atIndex = jvmName.indexOf('@');
            String pid = (atIndex != -1) ? jvmName.substring(0, atIndex) : "?";

            System.out.println("Test server JVM with pid [" + pid + "] listening on port ["
                + serverSocket.getLocalPort() + "]...");
            Runnable runnable = new Runnable() {
                public void run() {
                    Socket socket;
                    try {
                        while ((socket = serverSocket.accept()) != null) {
                            socket.close();
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            };
            runnable.run();

            TestProgram tp = new TestProgram();
            tp.run();
        }

        public void run() {
            while (true) {
                System.out.println("Test program running for " + (System.currentTimeMillis() - started) + "ms");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        }
    }

}
