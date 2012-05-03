/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.core.pc.inventory.getnativeprocess;

import java.io.IOException;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pc.inventory.getnativeprocess.testplugin.TestComponent;
import org.rhq.core.pc.inventory.getnativeprocess.testplugin.TestDiscoveryComponent;
import org.rhq.core.pc.inventory.getnativeprocess.testprocess.Main;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.test.arquillian.ResourceComponentInstances;
import org.rhq.test.arquillian.RunDiscovery;
import org.rhq.test.shrinkwrap.RhqAgentPluginArchive;

/**
 * 
 *
 * @author Lukas Krejci
 */
@RunDiscovery
public class NativeProcessRetrievalTest extends Arquillian {

    private Process testProcess;

    @ResourceComponentInstances(plugin = "getnativeprocess-plugin", resourceType = "Test Server")
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private Set<TestComponent> components;
    
    @Deployment
    @TargetsContainer("native-enabled-pc")
    public static RhqAgentPluginArchive getTestPlugin() {
        RhqAgentPluginArchive pluginJar = ShrinkWrap.create(RhqAgentPluginArchive.class, "test-plugin.jar");
        return pluginJar
            .setPluginDescriptor("getnativeprocess-rhq-plugin.xml")
            .addClasses(TestDiscoveryComponent.class, TestComponent.class);
    }

    @BeforeClass
    public void startTestProcess() throws IOException {
        ProcessBuilder bld =
            new ProcessBuilder("java", "-cp", "target/test-classes", Main.class.getName());

        testProcess = bld.start();
    }

    @AfterClass
    public void stopTestProcess() throws IOException, InterruptedException {
        if (testProcess != null) {
            testProcess.getOutputStream().write(0);
            testProcess.getOutputStream().flush();
            testProcess.waitFor();
            //SIGAR needs some time to stop reporting the dead process as sleeping...
            //so let's just wait here so that callers have the accurate info...
            Thread.sleep(2000);
            testProcess = null;
        }
    }

    @Test(groups = "pc.itest.native")
    public void testProcessInfoAccurateAfterProcessRestart() throws Exception {
        Assert.assertEquals(components.size(), 1, "There should be exactly 1 resource discovered");
        
        ConfigurationFacet component = components.iterator().next();
        
        Configuration config = component.loadResourceConfiguration();
        
        int beforeDiscoveryCallCount = config.getSimple(TestComponent.DISCOVERY_CALL_COUNT_PROP).getIntegerValue();
        int beforePid = config.getSimple(TestComponent.CURRENT_PID_PROP).getIntegerValue();
        
        stopTestProcess();   
        startTestProcess();
        
        config = component.loadResourceConfiguration();
        
        int afterDiscoveryCallCount = config.getSimple(TestComponent.DISCOVERY_CALL_COUNT_PROP).getIntegerValue();
        int afterPid = config.getSimple(TestComponent.CURRENT_PID_PROP).getIntegerValue();
        
        Assert.assertEquals(afterDiscoveryCallCount, beforeDiscoveryCallCount + 1, "Only a single discovery call should have been made to refresh the process info");
        Assert.assertNotEquals(beforePid, afterPid, "The process info should have refreshed");
    }
    
    @Test(groups = "pc.itest.native")
    public void testProcessInfoAccurateWhenProcessStopped() throws Exception {
        Assert.assertEquals(components.size(), 1, "There should be exactly 1 resource discovered");
        
        ConfigurationFacet component = components.iterator().next();
        
        Configuration config = component.loadResourceConfiguration();
        
        int beforeDiscoveryCallCount = config.getSimple(TestComponent.DISCOVERY_CALL_COUNT_PROP).getIntegerValue();
        
        stopTestProcess();   
        
        config = component.loadResourceConfiguration();
        
        int afterDiscoveryCallCount = config.getSimple(TestComponent.DISCOVERY_CALL_COUNT_PROP).getIntegerValue();
        int afterPid = config.getSimple(TestComponent.CURRENT_PID_PROP).getIntegerValue();
        
        Assert.assertEquals(afterDiscoveryCallCount, beforeDiscoveryCallCount, "No discovery call should have been made to refresh the process info if the process no longer exists");
        Assert.assertEquals(afterPid, 0, "The process info should have refreshed");  
        
        //just return to the default state for the other tests
        startTestProcess();
    }
    
    @Test(groups = "pc.itest.native")
    public void testProcessInfoAccurateAfterProcessStarted() throws Exception {
        Assert.assertEquals(components.size(), 1, "There should be exactly 1 resource discovered");
        
        ConfigurationFacet component = components.iterator().next();
        
        Configuration config = component.loadResourceConfiguration();
        
        int beforeDiscoveryCallCount = config.getSimple(TestComponent.DISCOVERY_CALL_COUNT_PROP).getIntegerValue();
        
        stopTestProcess();   
        
        config = component.loadResourceConfiguration();
        
        int afterDiscoveryCallCount = config.getSimple(TestComponent.DISCOVERY_CALL_COUNT_PROP).getIntegerValue();
        int afterPid = config.getSimple(TestComponent.CURRENT_PID_PROP).getIntegerValue();
        
        Assert.assertEquals(afterDiscoveryCallCount, beforeDiscoveryCallCount, "No discovery call should have been made to refresh the process info if the process no longer exists");
        Assert.assertEquals(afterPid, 0, "The process info should have been nulled out");  
        
        startTestProcess();

        config = component.loadResourceConfiguration();
        
        afterDiscoveryCallCount = config.getSimple(TestComponent.DISCOVERY_CALL_COUNT_PROP).getIntegerValue();
        afterPid = config.getSimple(TestComponent.CURRENT_PID_PROP).getIntegerValue();
        
        Assert.assertEquals(afterDiscoveryCallCount, beforeDiscoveryCallCount + 1, "Exactly 1 discovery call should have been made to refresh the process info after the process started again.");
        Assert.assertNotEquals(afterPid, 0, "The process info should have refreshed");  
    }
}
