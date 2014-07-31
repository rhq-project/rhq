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

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.rhq.core.domain.measurement.DataType.MEASUREMENT;
import static org.rhq.core.util.StringUtil.isNotBlank;
import static org.rhq.plugins.jmx.util.JvmResourceKey.Type.Explicit;
import static org.rhq.plugins.jmx.util.JvmResourceKey.Type.JmxRemotingPort;
import static org.rhq.test.AssertUtils.timedAssertion;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.util.ComponentUtil;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.jmx.JMXDiscoveryComponent;
import org.rhq.plugins.jmx.util.JvmResourceKey;
import org.rhq.test.AssertUtils;

/**
 * Integration test for the JMX plugin.
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class JMXPluginTest extends AbstractJMXPluginTest {
    private static final Log LOG = LogFactory.getLog(JMXPluginTest.class);

    private static final int JMX_REMOTING_PORT1 = 9921;
    private static final int JMX_REMOTING_PORT2 = 9922;
    private static final String EXPLICIT_RESOURCE_KEY1 = "foo1";
    private static final String EXPLICIT_RESOURCE_KEY2 = "foo2";

    static {
        File customPluginFile = null;
        try {
            customPluginFile = createCustomPluginFile();
        } catch (Exception e) {
            LOG.error("Could not create custom plugin file. Tests will fail", e);
        }
        AbstractJMXPluginTest.ADDITIONAL_PLUGIN_FILES.add(customPluginFile);
    }

    private static File createCustomPluginFile() throws Exception {

        File plugin = new File("src/test/resources/custom-test-plugin.xml");
        File targetFile = new File(System.getProperty("java.io.tmpdir"), "custom-test-plugin.jar");
        targetFile.deleteOnExit();

        ZipOutputStream outputStream = new ZipOutputStream(new FileOutputStream(targetFile));
        ZipEntry metainf = new ZipEntry("META-INF");
        outputStream.putNextEntry(metainf);
        outputStream.closeEntry();

        ZipEntry pluginXml = new ZipEntry("META-INF/rhq-plugin.xml");
        outputStream.putNextEntry(pluginXml);
        FileInputStream fis = new FileInputStream(plugin);
        int bufferSize = 1024;
        BufferedInputStream bufferedInputStream = new BufferedInputStream(fis, bufferSize);
        int size;
        byte data[] = new byte[bufferSize];
        while ((size = bufferedInputStream.read(data, 0, bufferSize)) != -1) {
            outputStream.write(data, 0, size);
        }
        bufferedInputStream.close();
        outputStream.closeEntry();
        outputStream.flush();
        outputStream.finish();

        return targetFile;
    }

    private List<Process> testServerJvms = new ArrayList<Process>();

    @BeforeClass
    public void startTestServers() throws Exception {
        this.testServerJvms.add(startTestServerJvm("-Dcom.sun.management.jmxremote.port=" + JMX_REMOTING_PORT1,
            "-Dcom.sun.management.jmxremote.ssl=false", "-Dcom.sun.management.jmxremote.authenticate=false"));

        // FIXME: Disabled until we find a fix for Sigar getProcCredName issue
        //            this.testServerJvms.add(startTestServerJvm("-D" + JMXDiscoveryComponent.SYSPROP_RHQ_RESOURCE_KEY + "="
        //                + EXPLICIT_RESOURCE_KEY1));

        this.testServerJvms.add(startTestServerJvm("-Dcom.sun.management.jmxremote.port=" + JMX_REMOTING_PORT2,
            "-Dcom.sun.management.jmxremote.ssl=false", "-Dcom.sun.management.jmxremote.authenticate=false", "-D"
                + JMXDiscoveryComponent.SYSPROP_RHQ_RESOURCE_KEY + "=" + EXPLICIT_RESOURCE_KEY2));
    }

    private static Process startTestServerJvm(String... jvmArgs) throws IOException {
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

    @AfterClass
    public void stopTestServers() {
        for (Process process : this.testServerJvms) {
            process.destroy();
        }
    }

    @Test(dependsOnMethods = "testPlatformFound")
    public void testServerDiscovery() throws Exception {
        timedAssertion(new AssertUtils.BooleanCondition() {
            @Override
            public boolean eval() {
                InventoryReport report = getInventoryManager().executeServerScanImmediately();
                LOG.info("Discovery took: " + (report.getEndTime() - report.getStartTime()) + " ms");

                boolean foundExplicitKey1Server = findTestServerResource(Explicit, EXPLICIT_RESOURCE_KEY1) != null;
                boolean foundExplicitKey2Server = findTestServerResource(Explicit, EXPLICIT_RESOURCE_KEY2) != null;
                boolean foundJmxRemotingServer = findTestServerResource(JmxRemotingPort, JMX_REMOTING_PORT1) != null;

                LOG.info("foundJmxRemotingServer = " + foundJmxRemotingServer);
                LOG.info("foundExplicitKey1Server = " + foundExplicitKey1Server);
                LOG.info("foundExplicitKey2Server = " + foundExplicitKey2Server);

                // key1Server not started, see above
                return /*foundExplicitKey1Server &&*/foundExplicitKey2Server && foundJmxRemotingServer;
            }
        }, "Could not find all JMX servers", 2, MINUTES, 10, SECONDS);
    }

    private Resource findTestServerResource(JvmResourceKey.Type keyType, Object value) {
        Resource platform = getInventoryManager().getPlatform();

        Set<Resource> jmxServers = getChildResourcesOfType(platform, SERVER_TYPE);
        for (Resource jmxServer : jmxServers) {

            JvmResourceKey key = JvmResourceKey.valueOf(jmxServer.getResourceKey());
            boolean isTestProgram = TestProgram.class.getName().equals(key.getMainClassName());
            if (isTestProgram && key.getType().equals(keyType)) {

                switch (keyType) {
                case Explicit:
                    if (key.getExplicitValue().equals(value)) {
                        return jmxServer;
                    }
                    break;
                case JmxRemotingPort:
                    if (key.getJmxRemotingPort().equals(value)) {
                        return jmxServer;
                    }
                    break;
                default:
                }
            }
        }
        return null;
    }

    @Test(dependsOnMethods = "testServerDiscovery")
    public void testServiceDiscovery() throws Exception {
        timedAssertion(new AssertUtils.BooleanCondition() {
            @Override
            public boolean eval() {
                InventoryReport report = getInventoryManager().executeServiceScanImmediately();
                LOG.info("Discovery took: " + (report.getEndTime() - report.getStartTime()) + " ms");

                Set<Resource> jmxServers = new HashSet<Resource>();

                @SuppressWarnings("unused")
                Resource explicitKey1Server = findTestServerResource(Explicit, EXPLICIT_RESOURCE_KEY1);
                // key1Server not started, see above
                // jmxServers.add(explicitKey1Server);
                Resource explicitKey2Server = findTestServerResource(Explicit, EXPLICIT_RESOURCE_KEY2);
                jmxServers.add(explicitKey2Server);
                Resource jmxRemotingServer = findTestServerResource(JmxRemotingPort, JMX_REMOTING_PORT1);
                jmxServers.add(jmxRemotingServer);

                for (Resource jmxServer : jmxServers) {
                    Set<Resource> childResources = jmxServer.getChildResources();
                    // Each JMX Server should have exactly six singleton child Resources with the following types:
                    // Operating System, Threading, VM Class Loading System, VM Compilation System, VM Memory System, and
                    // java.util.logging.
                    // And, the test servers we use also expose an additional custom MBean: rhq.test:name=TestTarget
                    int childResourcesCount = childResources.size();
                    LOG.info("childResourcesCount = " + childResourcesCount);
                    if (childResourcesCount != 7) {
                        return false;
                    }
                }

                return true;
            }
        }, "Test servers do not have 7 child resources", 2, MINUTES, 10, SECONDS);
    }

    @Test(dependsOnMethods = "testServiceDiscovery")
    public void testMeasurement() throws Exception {
        MeasurementFacet measurementFacet = getResourceComponentFacet("Operating System", MeasurementFacet.class);
        Set<MeasurementScheduleRequest> metricList = new HashSet<MeasurementScheduleRequest>();
        metricList.add(new MeasurementScheduleRequest(1, "CommittedVirtualMemorySize", 1000, true, MEASUREMENT));
        MeasurementReport report = new MeasurementReport();
        measurementFacet.getValues(report, metricList);
        Map<String, Object> metricsData = getMetricsData(report);
        assertTrue(getMetric(metricsData, "CommittedVirtualMemorySize") > 0);
    }

    @Test(dependsOnMethods = "testServiceDiscovery")
    public void testOperation1() throws Exception {
        OperationResult operationResult = invokeOperation("Threading", "threadDump", new Configuration());
        assertNotNull(operationResult);
        Configuration complexResults = operationResult.getComplexResults();
        assertNotNull(complexResults);
        assertTrue(isNotBlank(complexResults.getSimpleValue("totalCount")));
    }

    @Test(dependsOnMethods = "testServiceDiscovery")
    public void testOperation2() throws Exception {
        OperationResult operationResult = invokeOperation("TestService_", "doSomething", new Configuration());
        assertNull(operationResult); // Operation did not define a "<results>" block.
    }

    @Test(dependsOnMethods = "testServiceDiscovery")
    public void testOperation3() throws Exception {
        OperationResult operationResult = invokeOperation("TestService_", "hello", new Configuration());
        assertNotNull(operationResult);
        assertEquals(operationResult.getSimpleResult(), "Hello World");
    }

    @Test(dependsOnMethods = "testServiceDiscovery")
    public void testOperation4() throws Exception {
        Configuration parameters = new Configuration();
        parameters.put(new PropertySimple("p1", "Hello Test"));
        OperationResult operationResult = invokeOperation("TestService_", "echo", parameters);
        assertNotNull(operationResult);
        assertEquals(operationResult.getSimpleResult(), "Hello Test");
    }

    @Test(dependsOnMethods = "testServiceDiscovery")
    public void testOperation5() throws Exception {
        Configuration parameters = new Configuration();
        parameters.put(new PropertySimple("p1", "Hello"));
        parameters.put(new PropertySimple("p2", "Test"));
        OperationResult operationResult = invokeOperation("TestService_", "concat", parameters);
        assertNotNull(operationResult);
        assertEquals(operationResult.getSimpleResult(), "HelloTest");
    }

    private OperationResult invokeOperation(String typeName, String operationName, Configuration parameters)
        throws Exception {

        OperationFacet operationFacet = getResourceComponentFacet(typeName, OperationFacet.class);
        OperationResult operationResult = operationFacet.invokeOperation(operationName, parameters);

        if (operationResult != null && operationResult.getErrorMessage() != null) {
            fail("Operation (" + operationName + ") failed : " + operationResult.getErrorMessage());
        }
        return operationResult;
    }

    private <FACET> FACET getResourceComponentFacet(String typeName, Class<FACET> facetType) throws Exception {
        Resource platform = getInventoryManager().getPlatform();
        for (Resource server : platform.getChildResources()) {

            List<Resource> resources = new ArrayList<Resource>(server.getChildResources());
            for (Resource resource : resources) {

                if (resource.getResourceType().getName().equals(typeName)) {

                    return ComponentUtil.getComponent(resource.getId(), facetType, FacetLockType.WRITE,
                        MINUTES.toMillis(1), true, true, true);
                }
            }
        }

        throw new AssertionError("Not found: " + "typeName = [" + typeName + "], facetType = [" + facetType + "]");
    }

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

        @Override
        public void run() {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    LOG.info("__" + line);
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
                mBeanServer.createMBean(TestTarget.class.getName(), mBeanName);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            String jvmName = runtimeMXBean.getName();
            int atIndex = jvmName.indexOf('@');
            String pid = (atIndex != -1) ? jvmName.substring(0, atIndex) : "?";

            System.out.println("Test server JVM with pid [" + pid + "] listening on port ["
                + serverSocket.getLocalPort() + "]...");

            Runnable runnable = new Runnable() {
                @Override
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

        @Override
        public void run() {
            //noinspection InfiniteLoopStatement
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
