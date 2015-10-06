/*
 * Jopr Management Platform
 * Copyright (C) 2012 Red Hat, Inc.
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
package org.rhq.plugins.jbossas5.itest;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hyperic.sigar.SigarException;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.ResourceUtility;
import org.rhq.core.domain.util.TypeAndKeyResourceFilter;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.inventory.ResourceContainer;
import org.rhq.core.pluginapi.configuration.ListPropertySimpleWrapper;
import org.rhq.core.pluginapi.configuration.MapPropertySimpleWrapper;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.pluginapi.util.FileUtils;
import org.rhq.core.pluginapi.util.StartScriptConfiguration;
import org.rhq.core.system.ProcessInfo;
import org.rhq.core.system.SystemInfo;
import org.rhq.core.system.SystemInfoFactory;
import org.rhq.plugins.jbossas5.helper.JBossProductType;
import org.rhq.test.arquillian.RunDiscovery;

/**
 * An integration test for the AS5 server type.
 *
 * @author Ian Springer
 */
@Test(groups = { "integration" }, singleThreaded = true)
public class ApplicationServerComponentTest extends AbstractJBossAS5PluginTest {

    protected static final File JBOSS_HOME = new File(FileUtils.getCanonicalPath(System.getProperty("jboss5.home")));
    protected static final String BIND_ADDRESS = System.getProperty("jboss.bind.address");
    protected static final String SERVICE_BINDING_SET = System.getProperty("jboss.serviceBindingSet");
    protected static final int PORT_OFFSET = Integer.valueOf(System.getProperty("jboss.portOffset"));

    protected static final ResourceType RESOURCE_TYPE = new ResourceType("JBossAS Server", PLUGIN_NAME,
        ResourceCategory.SERVER, null);
    // The key of an AS5 Server Resource is its configuration directory.
    protected static final String RESOURCE_KEY = FileUtils.getCanonicalPath(System.getProperty("jboss5.home")
        + "/server/default");

    private static final String SHUTDOWN_OPERATION_NAME = "shutdown";
    private static final String START_OPERATION_NAME = "start";

    @Test
    @RunDiscovery
    public void testAutoDiscovery() throws Exception {
        System.out.println("\n****** Running " + getClass().getSimpleName() + ".testAutoDiscovery...");
        Resource platform = this.pluginContainer.getInventoryManager().getPlatform();
        System.out.println("~~~~~ Platform: " + platform);

        Assert.assertNotNull(platform);
        Assert.assertEquals(platform.getInventoryStatus(), InventoryStatus.COMMITTED);

        Assert.assertNotNull(getServerResource(), RESOURCE_TYPE + " Resource with key [" + RESOURCE_KEY
            + "] was not discovered.");
        System.out.println("===== Discovered: " + getServerResource());
        Configuration pluginConfig = getServerResource().getPluginConfiguration();
        System.out.println("---------- " + pluginConfig.toString(true));
        validatePluginConfiguration(pluginConfig);
        System.out.println("---------- Finished " + getClass().getSimpleName() + ".testAutoDiscovery...");
    }

    @Test(dependsOnMethods = { "testAutoDiscovery" })
    public void testConnection() throws Exception {
        System.out.println("\n****** Running " + getClass().getSimpleName() + ".testConnection...");
        Resource platform = this.pluginContainer.getInventoryManager().getPlatform();
        Assert.assertNotNull(platform);
        Assert.assertEquals(platform.getInventoryStatus(), InventoryStatus.COMMITTED);

        Configuration pluginConfig = getServerResource().getPluginConfiguration();

        // We need to set the principal and credentials to admin:admin in order to connect to the managed server.
        pluginConfig.setSimpleValue("principal", "admin");
        pluginConfig.setSimpleValue("credentials", "admin");

        // Restart the server ResourceComponent so it picks up the changes we just made to the plugin config.
        System.out.println("===== Updating Server Resource's plugin configuration: " + pluginConfig.toString(true));
        restartServerResourceComponent();

        // If the ResourceComponent connected to the managed server successfully, the Resource should now be UP.
        Thread.sleep(1000);
        AvailabilityType avail = getAvailability(getServerResource());
        assertEquals(avail, AvailabilityType.UP);

        System.out.println("---------- Finished " + getClass().getSimpleName() + ".testConnection...");
    }

    protected void validatePluginConfiguration(Configuration pluginConfig) {
        // "hostname" prop
        String hostname = pluginConfig.getSimpleValue("hostname", null);
        String expectedHostname = BIND_ADDRESS;
        assertEquals(hostname, expectedHostname, "Plugin config prop [hostname].");

        // "serverName" prop
        String serverName = pluginConfig.getSimpleValue("serverName", null);
        String expectedServerName = System.getProperty("jboss.server.name");
        expectedServerName = (null == expectedServerName) ? JBossProductType.AS.DEFAULT_CONFIG_NAME
            : expectedServerName;
        assertEquals(serverName, expectedServerName, "Plugin config prop [serverName].");

        // "startScript" prop
        String startScript = pluginConfig.getSimpleValue("startScript");
        startScript = (null == startScript) ? ("bin/" + getExpectedStartScriptFileName()) : startScript;
        Assert.assertNotNull(startScript);

        File startScriptFile = new File(startScript);
        String expectedStartScriptFileName = getExpectedStartScriptFileName();
        Assert.assertEquals(startScriptFile.getName(), expectedStartScriptFileName);

        if (!startScriptFile.isAbsolute()) {
            // If it's relative, e.g. "bin/standalone.sh", it will be resolved relative to the AS home dir.
            startScriptFile = new File(JBOSS_HOME, startScript);
        }
        Assert.assertTrue(startScriptFile.exists(), "Start script [" + startScriptFile + "] does not exist.");

        // "startScriptEnv" prop
        PropertySimple startScriptEnvProp = pluginConfig.getSimple("startScriptEnv");
        MapPropertySimpleWrapper startScriptEnvPropWrapper = new MapPropertySimpleWrapper(startScriptEnvProp);
        Map<String, String> env = startScriptEnvPropWrapper.getValue();
        validateStartScriptEnv(env);

        // "startScriptArgs" prop
        PropertySimple startScriptArgsProp = pluginConfig.getSimple("startScriptArgs");
        ListPropertySimpleWrapper startScriptArgsPropWrapper = new ListPropertySimpleWrapper(startScriptArgsProp);
        List<String> args = startScriptArgsPropWrapper.getValue();
        Assert.assertEquals(args, getExpectedStartScriptArgs(), "Plugin config prop [startScriptArgs]");
    }

    protected void validateStartScriptEnv(Map<String, String> env) {
        Assert.assertTrue(env.size() <= 4, env.toString());

        String javaHome = env.get("JAVA_HOME");
        Assert.assertNotNull(javaHome);
        Assert.assertTrue(new File(javaHome).isDirectory());

        String path = env.get("PATH");
        Assert.assertNotNull(path);
        String[] pathElements = path.split(File.pathSeparator);
        Assert.assertTrue(pathElements.length >= 1);
        Assert.assertTrue(new File(pathElements[0]).isDirectory());

        String javaOpts = env.get("JAVA_OPTS");
        Assert.assertNull(javaOpts);
    }

    @Test(dependsOnMethods = { "testConnection" })
    public void testShutdownAndStartOperations() throws Exception {
        System.out.println("\n****** Running " + getClass().getSimpleName() + ".testShutdownAndStartOperations...");

        // First make sure the server is up.
        AvailabilityType avail = getAvailability(getServerResource());
        assertEquals(avail, AvailabilityType.UP);

        System.out.println("===== Shutting Down Managed Server: " + getServerResource());

        // Now shut it down using the Shutdown op and make sure it has gone down.
        invokeOperationAndAssertSuccess(getServerResource(), SHUTDOWN_OPERATION_NAME, null);
        avail = getAvailability(getServerResource());
        assertEquals(avail, AvailabilityType.DOWN);

        //change the plugin config to shutdown via JMX
        Configuration pluginConfig = getServerResource().getPluginConfiguration();
        pluginConfig.getSimple("shutdownMethod").setValue("JMX");
        restartServerResourceComponent();
        
        //invoke the shutdown operation again and assert that it actually ran and generated some error message.
        OperationResult operationResult = invokeOperation(getServerResource(), SHUTDOWN_OPERATION_NAME, null);
        avail = getAvailability(getServerResource());        
        assertEquals(avail, AvailabilityType.DOWN);
        assertNotNull(operationResult.getErrorMessage());
        
        //ok, now try the same with the script shutdown method
        pluginConfig = getServerResource().getPluginConfiguration();
        pluginConfig.getSimple("shutdownMethod").setValue("SCRIPT");
        restartServerResourceComponent();
        
        //invoke the shutdown operation again and assert that it actually ran
        operationResult = invokeOperation(getServerResource(), SHUTDOWN_OPERATION_NAME, null);
        avail = getAvailability(getServerResource());        
        assertEquals(avail, AvailabilityType.DOWN);
        assertNotNull(operationResult.getErrorMessage());
        assertNull(operationResult.getSimpleResult());
        
        // Before restarting it, add some stuff to the 'startScriptEnv' and 'startScriptArgs' props so we can verify
        // they are used correctly by the Start op.
        pluginConfig = getServerResource().getPluginConfiguration();
        StartScriptConfiguration startScriptConfig = new StartScriptConfiguration(pluginConfig);

        // Add a var to the start script env.
        Map<String, String> env = startScriptConfig.getStartScriptEnv();
        env.put("FOO", "bar"); // uppercase env var name or Windows will do it for you
        startScriptConfig.setStartScriptEnv(env);

        // Add an arg to the start script args.
        List<String> args = startScriptConfig.getStartScriptArgs();
        args.add("-Dfoo=bar");
        startScriptConfig.setStartScriptArgs(args);

        // Restart the server ResourceComponent so it picks up the changes we just made to the plugin config.
        System.out.println("===== Updating Server Resource's plugin configuration: " + pluginConfig.toString(true));
        restartServerResourceComponent();

        System.out.println("===== Restarting Managed Server: " + getServerResource());

        // Finally restart it using the Start op and make sure it has come back up.
        invokeOperationAndAssertSuccess(getServerResource(), START_OPERATION_NAME, null);

        avail = getAvailability(getServerResource());
        assertEquals(avail, AvailabilityType.UP);

        System.out.println("===== Validating Server Process: " + getServerResource());

        List<ProcessInfo> processes = getServerProcesses();
        Assert.assertEquals(processes.size(), 1, "Can't find AS Process.");
        ProcessInfo serverProcess = processes.get(0);
        Map<String, String> processEnv = serverProcess.getEnvironmentVariables();
        assertEquals(processEnv.get("FOO"), "bar", processEnv.toString());

        List<String> processArgs = Arrays.asList(serverProcess.getCommandLine());
        assertTrue(processArgs.contains("-Dfoo=bar"), processArgs.toString());

        System.out.println("---------- Finished " + getClass().getSimpleName() + ". testShutdownAndStartOperations...");
    }

    private void restartServerResourceComponent() throws PluginContainerException {
        InventoryManager inventoryManager = this.pluginContainer.getInventoryManager();
        inventoryManager.deactivateResource(getServerResource());
        ResourceContainer serverContainer = inventoryManager.getResourceContainer(getServerResource());
        try {
            inventoryManager.activateResource(getServerResource(), serverContainer, true);
        } catch (InvalidPluginConfigurationException ex) {
            // we may fail to start the component in case the server is down and connection to ProfileService cannot be obtained
        }
    }

    protected String getExpectedStartScriptFileName() {
        return (File.separatorChar == '/') ? "run.sh" : "run.bat";
    }

    protected List<String> getExpectedStartScriptArgs() {
        String[] args = new String[] { "--configuration=default", "--host=127.0.0.1",
            "-Djboss.service.binding.set=ports-03" };
        return Arrays.asList(args);
    }

    @AfterSuite
    public void killServerProcesses() {
        List<ProcessInfo> processes = getServerProcesses();
        System.out.println("\n=== Killing " + processes.size() + " AS5 processes...");
        for (ProcessInfo process : processes) {
            System.out.println("====== Killing process with pid [" + process.getPid() + "] and command line ["
                + Arrays.toString(process.getCommandLine()) + "]...");
            try {
                process.kill("KILL");
            } catch (SigarException e) {
                System.err.println("Failed to kill " + process + ": " + e);
            }
        }
        processes = getServerProcesses();
        Assert.assertEquals(processes.size(), 0, "Failed to kill " + processes.size() + " AS5 processes: " + processes);
    }

    private Resource getServerResource() {
        Resource platform = this.pluginContainer.getInventoryManager().getPlatform();
        Set<Resource> childResources = ResourceUtility.getChildResources(platform, new TypeAndKeyResourceFilter(
            RESOURCE_TYPE, RESOURCE_KEY));
        if (childResources.size() > 1) {
            throw new IllegalStateException(platform + " has more than one child Resource with same type ("
                + RESOURCE_TYPE + ") and key (" + RESOURCE_KEY + ").");
        }
        return (childResources.isEmpty()) ? null : childResources.iterator().next();
    }

    private List<ProcessInfo> getServerProcesses() {
        SystemInfo systemInfo = SystemInfoFactory.createSystemInfo();
        return systemInfo.getProcesses("arg|*|match=org\\.jboss\\.Main|-Djboss.service.binding.set|match="
            + SERVICE_BINDING_SET);
    }

}
