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

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.AfterSuite;

import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.plugin.FileSystemPluginFinder;

/**
 * @author Ian Springer
 */
public abstract class AbstractPluginTest {    
    private static final String PLUGIN_NAME = "JBossAS5";
    private static final File ITEST_DIR = new File("target/itest");
    private static final long ONE_WEEK_IN_SECONDS = 60L * 60 * 24;

    @BeforeSuite
    public void start() {
        try {
            PluginContainerConfiguration pcConfig = new PluginContainerConfiguration();
            File pluginDir = new File(ITEST_DIR, "plugins");
            pcConfig.setPluginFinder(new FileSystemPluginFinder(pluginDir));
            pcConfig.setPluginDirectory(pluginDir);
            pcConfig.setInsideAgent(false);

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
            Set<String> pluginNames = PluginContainer.getInstance().getPluginManager().getMetadataManager().getPluginNames();
            System.out.println("PC started with the following plugins: " + pluginNames);
            PluginContainer.getInstance().getInventoryManager().executeServerScanImmediately();
            PluginContainer.getInstance().getInventoryManager().executeServiceScanImmediately();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @AfterSuite
    public void stop() {
        System.out.println("Stopping PC...");
        PluginContainer.getInstance().shutdown();
        System.out.println("PC stopped.");
    }

    protected String getPluginName() {
        return PLUGIN_NAME;
    }    
}