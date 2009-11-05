/*
 * RHQ Management Platform
 * Copyright (C) 2009 Red Hat, Inc.
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
package org.rhq.plugins.augeas;

import org.rhq.core.clientapi.agent.metadata.PluginMetadataManager;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.configuration.ConfigurationManager;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.plugin.FileSystemPluginFinder;
import org.rhq.core.pc.plugin.PluginManager;
import org.rhq.core.util.file.FileUtil;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.testng.Assert.fail;

/**
 * An base class for integration tests for instances of {@link AugeasConfigurationComponent}.
 *
 * @author Ian Springer
 */
public abstract class AbstractAugeasConfigurationComponentTest {
    public static final String TEST_GROUP = "linux-config";
    private static final File ITEST_DIR = new File("target/itest");
    private static final long ONE_WEEK_IN_SECONDS = 60L * 60 * 24;

    @BeforeSuite(groups = TEST_GROUP)
    public void start() {
        try {
            PluginContainerConfiguration pcConfig = new PluginContainerConfiguration();
            File pluginsDir = new File(ITEST_DIR, "plugins");
            pcConfig.setPluginFinder(new FileSystemPluginFinder(pluginsDir));
            pcConfig.setPluginDirectory(pluginsDir);
            pcConfig.setInsideAgent(false);
            pcConfig.setCreateResourceClassloaders(true);

            // Set initial delays for all scheduled scans to one week to effectively disable them.
            pcConfig.setServerDiscoveryInitialDelay(ONE_WEEK_IN_SECONDS);
            pcConfig.setServiceDiscoveryInitialDelay(ONE_WEEK_IN_SECONDS);
            pcConfig.setAvailabilityScanInitialDelay(ONE_WEEK_IN_SECONDS);
            pcConfig.setConfigurationDiscoveryInitialDelay(ONE_WEEK_IN_SECONDS);
            pcConfig.setContentDiscoveryInitialDelay(ONE_WEEK_IN_SECONDS);

            File tmpDir = new File(ITEST_DIR, "tmp");
            tmpDir.mkdirs();
            if (!tmpDir.isDirectory() || !tmpDir.canWrite()) {
                throw new IOException("Failed to create temporary directory (" + tmpDir + ").");
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
            fail("Failed to start the plugin container.", e);
        }
    }

    @BeforeSuite(dependsOnMethods = "start", groups = TEST_GROUP)
    public void updatePluginConfiguration() {
        try {
            System.out.println("Updating Augeas root in plugin configuration...");

            // We need to have the Resource inventoried in order to update its plugin config.
            InventoryManager inventoryManager = PluginContainer.getInstance().getInventoryManager();
            ResourceType resourceType = getResourceType();
            switch (resourceType.getCategory()) {
                case SERVICE:
                    System.out.println("Executing platform service scan to discover " + resourceType + " Resource...");
                    inventoryManager.performServiceScan(inventoryManager.getPlatform().getId());
                    break;
                case SERVER:
                    System.out.println("Executing server scan to discover " + resourceType + " Resource...");
                    inventoryManager.executeServerScanImmediately();
                    break;
                case PLATFORM:
                    throw new IllegalStateException("Huh? Using Augeas to configure a platform?");
            }
            Resource resource = getResource();
            Configuration pluginConfig = resource.getPluginConfiguration();

            String tmpDirPath = System.getProperty("java.io.tmpdir");
            File tmpDir = new File(tmpDirPath);
            File augeasRootPath = new File(tmpDir, "rhq-itest-augeas-root-path");
            augeasRootPath.mkdirs();

            pluginConfig.put(new PropertySimple(AugeasConfigurationComponent.AUGEAS_ROOT_PATH_PROP, augeasRootPath));

            PropertySimple includes = pluginConfig.getSimple(AugeasConfigurationComponent.INCLUDE_GLOBS_PROP);
            List<String> includeGlobs = new ArrayList<String>();
            includeGlobs.addAll(Arrays.asList(includes.getStringValue().split("\\s*\\|\\s*")));
            for (String includeGlob : includeGlobs) {
                InputStream inputStream = this.getClass().getResourceAsStream(includeGlob);
                if (inputStream != null) {
                    File outputFile = new File(augeasRootPath, includeGlob);
                    outputFile.getParentFile().mkdirs();
                    FileUtil.writeFile(inputStream, outputFile);
                }
            }

            inventoryManager.updatePluginConfiguration(resource.getId(), pluginConfig);
        } catch (Exception e) {
            fail("Failed to update Augeas root in plugin configuration.", e);
        }
    }

    @BeforeSuite(dependsOnMethods = "updatePluginConfiguration", groups = TEST_GROUP)
    public void scanInventory() {
        /*try {
            System.out.println("Executing full discovery scan...");
            PluginContainer.getInstance().getInventoryManager().executeServerScanImmediately();
            PluginContainer.getInstance().getInventoryManager().executeServiceScanImmediately();
        } catch (Exception e) {
            fail("Failed to execute full discovery scan.", e);
        }*/
    }

    @AfterSuite(groups = TEST_GROUP)
    public void stop() {
        System.out.println("Stopping PC...");
        PluginContainer.getInstance().shutdown();
        System.out.println("PC stopped.");
    }

    @Test(groups = TEST_GROUP)
    public void testResourceConfigLoad() throws Exception {
        if (getResourceType().getResourceConfigurationDefinition() != null) {
            ConfigurationManager configurationManager = PluginContainer.getInstance().getConfigurationManager();
            Resource resource = getResource();
            Configuration resourceConfig = configurationManager.loadResourceConfiguration(resource.getId());
            Configuration expectedResourceConfig = getExpectedResourceConfig();
            assert resourceConfig.equals(expectedResourceConfig) :
                    "Unexpected Resource configuration - \nExpected:\n\t"
                            + expectedResourceConfig.toString(true) + "\nActual:\n\t"
                            + resourceConfig.toString(true);
        }
    }

    protected abstract Configuration getExpectedResourceConfig();

    protected static ResourceType getResourceType(String resourceTypeName, String pluginName) {
        PluginManager pluginManager = PluginContainer.getInstance().getPluginManager();
        PluginMetadataManager pluginMetadataManager = pluginManager.getMetadataManager();
        return pluginMetadataManager.getType(resourceTypeName, pluginName);
    }

    protected final ResourceType getResourceType() {
        return getResourceType(getResourceTypeName(), getPluginName());
    }

    protected abstract String getPluginName();
    protected abstract String getResourceTypeName();

    protected Resource getResource() {
        InventoryManager inventoryManager = PluginContainer.getInstance().getInventoryManager();
        ResourceType resourceType = getResourceType();
        Set<Resource> resources = inventoryManager.getResourcesWithType(resourceType);
        if (resources.isEmpty()) {
            return null;
        }
        if (resources.size() > 1) {
            throw new IllegalStateException("Found more than one " + resourceType.getName()
                    + " Resource - expected there to be exactly one - resources: " + resources);
        }
        return resources.iterator().next();
    }
}
