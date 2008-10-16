/*
 * JBoss, Home of Professional Open Source
 * Copyright 2006, JBoss Inc. All rights reserved.
 */

package org.rhq.plugins.bluetooth;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.discovery.InventoryReport;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.plugin.FileSystemPluginFinder;
import org.rhq.core.pc.plugin.PluginEnvironment;
import org.rhq.core.pc.plugin.PluginManager;

/**
 * @author Mark Spritzler
 */
public class BluetoothPluginTest {
    private Log log = LogFactory.getLog(BluetoothPluginTest.class);

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

    //@Test
    public void testPluginLoad() {
        PluginManager pluginManager = PluginContainer.getInstance().getPluginManager();

        PluginEnvironment pluginEnvironment = pluginManager.getPlugin("Bluetooth");
        assert (pluginEnvironment != null) : "Null environment, plugin not loaded";
        System.out.println("Plugin name: " + pluginEnvironment.getPluginName());
    }

    //@Test(dependsOnMethods = "testPluginLoad")
    public void testServerDiscovery() throws Exception {
        InventoryReport report = PluginContainer.getInstance().getInventoryManager().executeServerScanImmediately();
        assert report != null;
        System.out.println("Discovery took: " + (report.getEndTime() - report.getStartTime()) + "ms");

        Set<Resource> pgServers = getDiscoveredServers();
        System.out.println("Found " + pgServers.size() + " Bluetooth devices");
    }

    //@Test
    public void testCustom() throws Exception {
        Configuration config = new Configuration();
        config.put(new PropertySimple("host", "localhost"));
        config.put(new PropertySimple("port", "1521"));
        config.put(new PropertySimple("sid", "XE"));
        config.put(new PropertySimple("driverClass", "oracle.jdbc.driver.OracleDriver"));
        config.put(new PropertySimple("principal", "sys"));
        config.put(new PropertySimple("credentials", "nautical"));

        Resource r = new Resource("test", "test", null);
        r.setPluginConfiguration(config);
    }

    private Set<Resource> getDiscoveredServers() {
        Set<Resource> found = new HashSet<Resource>();
        Resource platform = PluginContainer.getInstance().getInventoryManager().getPlatform();
        for (Resource resource : platform.getChildResources()) {
            if (resource.getResourceType().getName().equals("Oracle Server")) {
                found.add(resource);
            }
        }
        return found;
    }

}
