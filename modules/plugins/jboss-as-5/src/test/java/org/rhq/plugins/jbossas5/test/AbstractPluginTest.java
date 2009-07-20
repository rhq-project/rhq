/*
 * Jopr Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

package org.rhq.plugins.jbossas5.test;

import static org.testng.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.plugin.FileSystemPluginFinder;
import org.rhq.plugins.jbossas5.test.util.AppServerUtils;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

/**
 * @author Ian Springer
 */
public abstract class AbstractPluginTest {
    private static final File ITEST_DIR = new File("target/itest");
    private static final long ONE_WEEK_IN_SECONDS = 60L * 60 * 24;

    @BeforeSuite(groups = "as5-plugin")
    public void start() {
        try {
            PluginContainerConfiguration pcConfig = new PluginContainerConfiguration();
            File pluginDir = new File(ITEST_DIR, "plugins");
            pcConfig.setPluginFinder(new FileSystemPluginFinder(pluginDir));
            pcConfig.setPluginDirectory(pluginDir);
            pcConfig.setInsideAgent(false);
            pcConfig.setCreateResourceClassloaders(true);
            pcConfig.setRootPluginClassLoaderRegex(getRootClassLoaderClassesToHideRegex());

            // Set initial delays for all scheduled scans to one week to effectively disable them.
            pcConfig.setServerDiscoveryInitialDelay(ONE_WEEK_IN_SECONDS);
            pcConfig.setServiceDiscoveryInitialDelay(ONE_WEEK_IN_SECONDS);
            pcConfig.setAvailabilityScanInitialDelay(ONE_WEEK_IN_SECONDS);
            pcConfig.setConfigurationDiscoveryInitialDelay(ONE_WEEK_IN_SECONDS);
            pcConfig.setContentDiscoveryInitialDelay(ONE_WEEK_IN_SECONDS);

            File tmpDir = new File(ITEST_DIR, "tmp");
            tmpDir.mkdirs();
            if (!tmpDir.isDirectory() || !tmpDir.canWrite()) {
                throw new IOException("Failed to create temporary directory: " + tmpDir);
            }
            pcConfig.setTemporaryDirectory(tmpDir);
            PluginContainer.getInstance().setConfiguration(pcConfig);
            System.out.println("Starting PC...");
            PluginContainer.getInstance().initialize();
            Set<String> pluginNames = PluginContainer.getInstance().getPluginManager().getMetadataManager()
                .getPluginNames();
            System.out.println("PC started with the following plugins: " + pluginNames);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to start the plugin container", e);
        }
    }

    @BeforeSuite(dependsOnMethods = "start", groups = "as5-plugin")
    @Parameters( { "principal", "credentials" })
    public void configureASResource(@Optional String principal, @Optional String credentials) {
        try {
            System.out.println("Configuring AS connection properties.");
            if (principal != null) {
                //we need to have the AS5 resource to be able to configure it
                System.out.println("Issuing the first server scan. This might fail because we haven't had the chance to configure the resource yet.");
                PluginContainer.getInstance().getInventoryManager().executeServerScanImmediately();

                System.out.println("Applying AS5 server resource configuration.");
                Configuration newConfig = AppServerUtils.getASResource().getPluginConfiguration();
                newConfig.put(new PropertySimple("principal", principal));
                newConfig.put(new PropertySimple("credentials", credentials));

                int asResourceId = AppServerUtils.getASResource().getId();

                PluginContainer.getInstance().getInventoryManager().updatePluginConfiguration(asResourceId, newConfig);
            }
        } catch (Exception e) {
            fail("Failed to configure the AS resource", e);
        }
    }

    @BeforeSuite(dependsOnMethods = "configureASResource", groups = "as5-plugin")
    public void scanInventory() {
        try {
            System.out.println("Issuing inventory scan.");
            PluginContainer.getInstance().getInventoryManager().executeServerScanImmediately();
            PluginContainer.getInstance().getInventoryManager().executeServiceScanImmediately();
        } catch (Exception e) {
            fail("Failed to scan the inventory", e);
        }
    }

    @AfterSuite(groups = "as5-plugin")
    public void stop() {
        System.out.println("Stopping PC...");
        PluginContainer.getInstance().shutdown();
        System.out.println("PC stopped.");
    }

    protected String getPluginName() {
        return AppServerUtils.PLUGIN_NAME;
    }
    
    private String getRootClassLoaderClassesToHideRegex() {
        StringBuilder defaultRegex = new StringBuilder();

        // some things that we know are in the maven isolated classloader that we need to hide
        defaultRegex.append("(org\\.jboss\\.proxy\\..*)|");
        defaultRegex.append("(org\\.jboss\\.aop\\..*)|");
        defaultRegex.append("(org\\.jboss\\.jmx\\..*)|");
        defaultRegex.append("(org\\.jboss\\.invocation\\..*)|");
        defaultRegex.append("(org\\.jnp\\..*)|");
        defaultRegex.append("(org\\.rhq\\.plugins\\..*)|");
        defaultRegex.append("(org\\.mc4j\\..*)|");

        // the rest of these are the same regex statements used by the agent
        defaultRegex.append("(javax\\.xml\\.bind\\..*)|");
        defaultRegex.append("(com\\.sun\\.activation\\..*)|");
        defaultRegex.append("(com\\.sun\\.istack\\..*)|");
        defaultRegex.append("(com\\.sun\\.xml\\..*)|");
        defaultRegex.append("(org\\.apache\\.commons\\.httpclient\\..*)|");
        defaultRegex.append("(org\\.apache\\.xerces\\..*)|");
        defaultRegex.append("(org\\.jboss\\.logging\\..*)|");
        defaultRegex.append("(org\\.jboss\\.net\\..*)|");
        defaultRegex.append("(org\\.jboss\\.util\\..*)|");
        defaultRegex.append("(org\\.jboss\\.dom4j\\..*)|");
        defaultRegex.append("(org\\.jboss\\.mx\\..*)|");
        defaultRegex.append("(org\\.jboss\\.remoting\\..*)|");
        defaultRegex.append("(org\\.jboss\\.serial\\..*)|");
        defaultRegex.append("(org\\.dom4j\\..*)|");
        defaultRegex.append("(EDU\\.oswego\\..*)|");
        defaultRegex.append("(gnu\\.getopt\\..*)|");
        defaultRegex.append("(org\\.rhq\\.core\\.clientapi\\..*)|");
        defaultRegex.append("(org\\.rhq\\.core\\.communications\\..*)|");
        defaultRegex.append("(org\\.rhq\\.core\\.pc\\..*)|");
        defaultRegex.append("(org\\.rhq\\.enterprise\\.communications\\.(?!command\\.server\\.CommandProcessorMetrics.*).*)");

        return defaultRegex.toString();       
    }
}