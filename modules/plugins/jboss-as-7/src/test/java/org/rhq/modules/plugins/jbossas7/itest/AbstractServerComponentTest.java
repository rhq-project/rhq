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

import static org.rhq.modules.plugins.jbossas7.test.util.Constants.JBOSS_HOME;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hyperic.sigar.SigarException;
import org.testng.Assert;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.inventory.ResourceContainer;
import org.rhq.core.pluginapi.configuration.ListPropertySimpleWrapper;
import org.rhq.core.pluginapi.configuration.MapPropertySimpleWrapper;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.util.StartScriptConfiguration;
import org.rhq.core.system.ProcessInfo;
import org.rhq.core.system.SystemInfo;
import org.rhq.core.system.SystemInfoFactory;
import org.rhq.modules.plugins.jbossas7.JBossProductType;
import org.rhq.modules.plugins.jbossas7.helper.ServerPluginConfiguration;

/**
 * The base class for the integration tests for the two AS7 server types.
 *
 * @author Ian Springer
 */
public abstract class AbstractServerComponentTest extends AbstractJBossAS7PluginTest {

    private static final Map<String, String> EAP6_VERSION_TO_AS7_VERSION_MAP = new HashMap<String, String>();
    static {
        EAP6_VERSION_TO_AS7_VERSION_MAP.put("6.0.0", "7.1.2.Final-redhat-1");
        EAP6_VERSION_TO_AS7_VERSION_MAP.put("6.0.1", "7.1.3.Final-redhat-4");
        EAP6_VERSION_TO_AS7_VERSION_MAP.put("6.1.0.Alpha", "7.2.0.Alpha1-redhat-4");
        EAP6_VERSION_TO_AS7_VERSION_MAP.put("6.1.0", "7.2.0.Final-redhat-8");
    }

    private static final String RELEASE_VERSION_TRAIT_NAME = "_skm:release-version";

    private static final String SHUTDOWN_OPERATION_NAME = "shutdown";
    private static final String START_OPERATION_NAME = "start";

    protected abstract ResourceType getServerResourceType();

    protected abstract String getServerResourceKey();

    protected abstract Resource getServerResource();

    public void testServerAttributeValidation() throws Exception {
        AvailabilityType avail = getAvailability(getServerResource());
        assertEquals(avail, AvailabilityType.UP);

        Configuration pluginConfig = getServerResource().getPluginConfiguration();
        ServerPluginConfiguration serverPluginConfig = new ServerPluginConfiguration(pluginConfig);

        // Change the baseDir prop.
        File originalBaseDir = serverPluginConfig.getBaseDir();
        serverPluginConfig.setBaseDir(new File(System.getProperty("java.io.tmpdir")));

        // Restart the server ResourceComponent so it picks up the changes we just made to the plugin config.
        InventoryManager inventoryManager = this.pluginContainer.getInventoryManager();
        inventoryManager.deactivateResource(getServerResource());
        ResourceContainer serverContainer = inventoryManager.getResourceContainer(getServerResource());
        InvalidPluginConfigurationException ipce = null;
        try {
            inventoryManager.activateResource(getServerResource(), serverContainer, true);
        } catch (InvalidPluginConfigurationException e) {
            ipce = e;
        }

        // Set the baseDir back to the original value and restart the component before making any assertions, to ensure
        // things aren't left in a corrupt state for remaining test methods.
        serverPluginConfig.setBaseDir(originalBaseDir);
        inventoryManager.activateResource(getServerResource(), serverContainer, true);

        Assert.assertNotNull(ipce, "InvalidPluginConfigurationException was not thrown by server component's "
            + "start() method due to invalid baseDir.");

        // Change the expectedRuntimeProductName property
        boolean exceptException = true;
        String originalExpectedRuntimeProductName = pluginConfig.getSimpleValue("expectedRuntimeProductName");
        if (originalExpectedRuntimeProductName.equals(JBossProductType.AS.PRODUCT_NAME)) {
            exceptException = false;
            pluginConfig.setSimpleValue("expectedRuntimeProductName", JBossProductType.EAP.PRODUCT_NAME);
        } else {
            pluginConfig.setSimpleValue("expectedRuntimeProductName", JBossProductType.AS.PRODUCT_NAME);
        }

        // Restart the server ResourceComponent so it picks up the changes we just made to the plugin config.
        inventoryManager.deactivateResource(getServerResource());
        ipce = null;
        try {
            inventoryManager.activateResource(getServerResource(), serverContainer, true);
        } catch (InvalidPluginConfigurationException e) {
            ipce = e;
        }

        // Set the expectedRuntimeProductName property back to the original value and restart the component before
        // making any assertions, to ensure things aren't left in a corrupt state for remaining test methods.
        pluginConfig.setSimpleValue("expectedRuntimeProductName", originalExpectedRuntimeProductName);
        inventoryManager.activateResource(getServerResource(), serverContainer, true);

        if(exceptException) {
            Assert.assertNotNull(ipce, "InvalidPluginConfigurationException was not thrown by server component's "
                    + "start() method due to invalid productType.");
        } else {
            Assert.assertNull(ipce, "InvalidPluginConfigurationException should not have been thrown in case of "
            + "detected AS (could be fallback)");
        }

    }

    protected void validatePluginConfiguration(Configuration pluginConfig) {
        System.out.println("---------- " + pluginConfig.toString(true));

        // "hostname" prop
        String hostname = pluginConfig.getSimpleValue("hostname", null);
        String expectedHostname = System.getProperty(getBindAddressSystemPropertyName());
        assertEquals(hostname, expectedHostname, "Plugin config prop [hostname].");

        // "port" prop
        String portString = pluginConfig.getSimpleValue("port", null);
        Integer port = (portString != null) ? Integer.valueOf(portString) : null;
        int portOffset = getPortOffset();
        Integer expectedPort = portOffset + 9990;
        assertEquals(port, expectedPort, "Plugin config prop [port].");

        // "startScript" prop
        String startScript = pluginConfig.getSimpleValue("startScript");
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

    protected int getPortOffset() {
        String portOffsetString = System.getProperty(getPortOffsetSystemPropertyName());
        return (portOffsetString != null) ? Integer.valueOf(portOffsetString) : 0;
    }

    protected void validateStartScriptEnv(Map<String, String> env) {
        String javaHome = env.get("JAVA_HOME");
        if (javaHome != null) {
            Assert.assertTrue(new File(javaHome).isDirectory());
        }

        String path = env.get("PATH");
        if (path != null) {
            String[] pathElements = path.split(File.pathSeparator);
            Assert.assertTrue(pathElements.length >= 1);
            Assert.assertTrue(new File(pathElements[0]).isDirectory());
        }
    }

    protected abstract String getBindAddressSystemPropertyName();

    protected abstract String getPortOffsetSystemPropertyName();

    public void testReleaseVersionTrait() throws Exception {
        String releaseVersion = collectTrait(getServerResource(), RELEASE_VERSION_TRAIT_NAME);
        String as7Version = System.getProperty("as7.version");
        String expectedReleaseVersion;
        if (as7Version.startsWith("6.")) {
            // EAP6
            expectedReleaseVersion = EAP6_VERSION_TO_AS7_VERSION_MAP.get(as7Version);
            if (expectedReleaseVersion == null) {
                throw new Exception("No AS7 version mapping is defined for EAP6 version [" + as7Version + "].");
            }
        } else {
            // AS7
            expectedReleaseVersion = as7Version;
        }
        assertEquals(releaseVersion, expectedReleaseVersion, "Unexpected value for trait ["
            + RELEASE_VERSION_TRAIT_NAME + "].");
    }

    public void testExecuteCliOperations() throws Exception {
        // First make sure the server is up.
        AvailabilityType avail = getAvailability(getServerResource());
        assertEquals(avail, AvailabilityType.UP);
        Configuration c = new Configuration();
        c.put(new PropertySimple("commands", "ls"));
        invokeOperationAndAssertSuccess(getServerResource(), "executeCommands", c);
        File script = null;
        try {
            c = new Configuration();
            script = File.createTempFile("test", "script");
            writeFile("ls", script);
            c.put(new PropertySimple("file", script.getAbsolutePath()));
            invokeOperationAndAssertSuccess(getServerResource(), "executeScript", c);
        } finally {
            script.delete();
        }
    }

    private File writeFile(String content, File fileToOverwrite) throws Exception {
        FileOutputStream out = null;

        try {
            fileToOverwrite.getParentFile().mkdirs();
            out = new FileOutputStream(fileToOverwrite);
            out.write(content.getBytes());
            return fileToOverwrite;
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    public void testShutdownAndStartOperations() throws Exception {
        // First make sure the server is up.
        AvailabilityType avail = getAvailability(getServerResource());
        assertEquals(avail, AvailabilityType.UP);

        // Now shut it down using the Shutdown op and make sure it has gone down.
        invokeOperationAndAssertSuccess(getServerResource(), SHUTDOWN_OPERATION_NAME, null);
        avail = getAvailability(getServerResource());
        assertEquals(avail, AvailabilityType.DOWN);

        // Before restarting it, add some stuff to the 'startScriptEnv' and 'startScriptArgs' props so we can verify
        // they are used correctly by the Start op.
        Configuration pluginConfig = getServerResource().getPluginConfiguration();
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
        InventoryManager inventoryManager = this.pluginContainer.getInventoryManager();
        inventoryManager.deactivateResource(getServerResource());
        ResourceContainer serverContainer = inventoryManager.getResourceContainer(getServerResource());
        inventoryManager.activateResource(getServerResource(), serverContainer, true);

        // Finally restart it using the Start op and make sure it has come back up.
        invokeOperationAndAssertSuccess(getServerResource(), START_OPERATION_NAME, null);
        avail = getAvailability(getServerResource());
        assertEquals(avail, AvailabilityType.UP);

        List<ProcessInfo> processes = getServerProcesses();
        //Assert.assertEquals(processes.size(), 1, getCommandLines(processes).toString());
        ProcessInfo serverProcess = processes.get(0);
        Map<String, String> processEnv = serverProcess.getEnvironmentVariables();
        assertEquals(processEnv.get("FOO"), "bar", processEnv.toString());

        List<String> processArgs = Arrays.asList(serverProcess.getCommandLine());
        assertTrue(processArgs.contains("-Dfoo=bar"), processArgs.toString());
    }

    protected abstract String getExpectedStartScriptFileName();

    protected abstract List<String> getExpectedStartScriptArgs();

    protected void killServerProcesses() {
        List<ProcessInfo> processes = getServerProcesses();
        System.out.println("\n=== Killing " + processes.size() + " " + getServerResourceType() + " processes...");
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
        Assert.assertEquals(processes.size(), 0, "Failed to kill " + processes.size() + " " + getServerResourceType()
            + " processes: " + processes);
    }

    private List<ProcessInfo> getServerProcesses() {
        SystemInfo systemInfo = SystemInfoFactory.createSystemInfo();
        return systemInfo
            .getProcesses("arg|*|match=org\\.jboss\\.as\\..+,arg|-Djboss.socket.binding.port-offset|match="
                + getPortOffset());
    }

}
