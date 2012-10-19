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
package org.rhq.plugins.augeas.test;

import static org.testng.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.augeas.jna.Aug;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.configuration.ConfigurationUpdateRequest;
import org.rhq.core.clientapi.agent.metadata.PluginMetadataManager;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.configuration.ConfigurationManager;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.plugin.FileSystemPluginFinder;
import org.rhq.core.pc.plugin.PluginManager;
import org.rhq.core.pluginapi.util.FileUtils;
import org.rhq.core.util.file.FileUtil;
import org.rhq.plugins.augeas.AugeasConfigurationComponent;

/**
 * A base class for integration tests for instances of {@link AugeasConfigurationComponent}.
 *
 * @author Ian Springer
 */
public abstract class AbstractAugeasConfigurationComponentTest {
    public static final String TEST_GROUP = "linux-config";
    private static final File ITEST_DIR = new File("target/itest");
    private static final long ONE_WEEK_IN_SECONDS = 60L * 60 * 24;
    protected static final boolean IS_WINDOWS = (File.separatorChar == '\\');

    protected static final File AUGEAS_ROOT;
    static {
        String tmpDirPath = System.getProperty("java.io.tmpdir");
        File tmpDir = new File(tmpDirPath);
        AUGEAS_ROOT = new File(tmpDir, "rhq-itest-augeas-root-path");
    }

    private final Log log = LogFactory.getLog(this.getClass());

    @BeforeClass(groups = TEST_GROUP)
    public void start() {
        if (!isResourceConfigSupported()) {
            String message;
            if (IS_WINDOWS) {
                message = "Augeas is not available on Windows. Augeas-based configuration functionality will *not* be tested.";
            } else {
                message = "Augeas not found. If on Fedora or RHEL, `yum install augeas`. Augeas-based configuration functionality will *not* be tested.";
            }
            System.out.println(message);
        }
        try {
            PluginContainerConfiguration pcConfig = createPluginContainerConfiguration();
            PluginContainer.getInstance().setConfiguration(pcConfig);

            System.out.println("Starting plugin container...");
            PluginContainer pluginContainer = PluginContainer.getInstance();
            pluginContainer.initialize();
            Set<String> pluginNames = pluginContainer.getPluginManager().getMetadataManager().getPluginNames();
            System.out.println("Plugin container started with the following plugins: " + pluginNames);

            System.out.println("Updating Augeas root in default plugin config...");
            boolean deleteRoot = true;
            for (ResourceType resourceType : getResourceTypes(getPluginName())) {
                ConfigurationDefinition pluginConfigDef = resourceType.getPluginConfigurationDefinition();
                ConfigurationTemplate defaultPluginConfigTemplate = pluginConfigDef.getDefaultTemplate();
                Configuration defaultPluginConfig = defaultPluginConfigTemplate.getConfiguration();
                tweakDefaultPluginConfig(defaultPluginConfig);
                resetConfigFiles(defaultPluginConfig, deleteRoot);
                deleteRoot = false;
            }

            InventoryManager inventoryManager = PluginContainer.getInstance().getInventoryManager();
            System.out.println("Executing server discovery scan...");
            inventoryManager.executeServerScanImmediately();
            System.out.println("Executing service discovery scan...");
            inventoryManager.executeServiceScanImmediately();
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to initialize the plugin container.", e);
        }
    }

    @AfterMethod(groups = TEST_GROUP)
    public void resetConfigFiles() throws IOException {
        boolean deleteRoot = true;
        for (Resource res : getResources()) {
            resetConfigFiles(res.getPluginConfiguration(), deleteRoot);
            deleteRoot = false;
        }

    }

    @Test(groups = TEST_GROUP)
    public void testResourceConfigLoad() throws Exception {
        ConfigurationManager configurationManager = PluginContainer.getInstance().getConfigurationManager();
        Configuration resourceConfig;
        try {
            for (Resource resource : getResources()) {
                resourceConfig = configurationManager.loadResourceConfiguration(resource.getId());
                Configuration expectedResourceConfig = getExpectedResourceConfig();
                assert resourceConfig.equals(expectedResourceConfig) : "Unexpected Resource configuration - \nExpected:\n\t"
                    + expectedResourceConfig.toString(true) + "\nActual:\n\t" + resourceConfig.toString(true);
            }
        } catch (PluginContainerException e) {
            if (isResourceConfigSupported()) {
                throw e;
            }
        }
    }

    @Test(groups = TEST_GROUP)
    public void testResourceConfigUpdate() throws Exception {
        ConfigurationManager configurationManager = PluginContainer.getInstance().getConfigurationManager();
        for (Resource resource : getResources()) {
            Configuration updatedResourceConfig = getUpdatedResourceConfig();
            ConfigurationUpdateRequest updateRequest = new ConfigurationUpdateRequest(0, updatedResourceConfig,
                resource.getId());
            configurationManager.updateResourceConfiguration(updateRequest);

            if (isResourceConfigSupported()) {
                // Give the component and the managed resource some time to properly persist the update.
                Thread.sleep(500);

                Configuration resourceConfig = configurationManager.loadResourceConfiguration(resource.getId());
                assert resourceConfig.equals(updatedResourceConfig) : "Unexpected Resource configuration - \nExpected:\n\t"
                    + updatedResourceConfig.toString(true) + "\nActual:\n\t" + resourceConfig.toString(true);
            }
        }
    }

    @AfterClass(groups = TEST_GROUP)
    public void stop() {
        deleteAugeasRootDir();
        System.out.println("Stopping plugin container...");
        PluginContainer.getInstance().shutdown();
        System.out.println("Plugin container stopped.");
    }

    protected abstract String getPluginName();

    protected abstract String getResourceTypeName();

    protected abstract Configuration getExpectedResourceConfig();

    protected abstract Configuration getUpdatedResourceConfig();

    protected PluginContainerConfiguration createPluginContainerConfiguration() throws IOException {
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
        
        File dataDir = new File(ITEST_DIR, "plugin-data");
        dataDir.mkdirs();
        if (!dataDir.isDirectory() || !dataDir.canWrite()) {
            throw new IOException("Failed to create data directory (" + dataDir + ").");
        }
        pcConfig.setDataDirectory(dataDir);
        
        return pcConfig;
    }

    protected static Set<ResourceType> getResourceTypes(String pluginName) {
        Set<ResourceType> ret = new HashSet<ResourceType>();
        PluginManager pluginManager = PluginContainer.getInstance().getPluginManager();
        PluginMetadataManager pluginMetadataManager = pluginManager.getMetadataManager();
        for (ResourceType res : pluginMetadataManager.getAllTypes()) {
            if (pluginName.equals(res.getPlugin())) {
                ret.add(res);
            }
        }
        return ret;
    }

    protected static ResourceType getResourceType(String resourceTypeName, String pluginName) {
        PluginManager pluginManager = PluginContainer.getInstance().getPluginManager();
        PluginMetadataManager pluginMetadataManager = pluginManager.getMetadataManager();
        return pluginMetadataManager.getType(resourceTypeName, pluginName);
    }

    protected final ResourceType getResourceType() {
        return getResourceType(getResourceTypeName(), getPluginName());
    }

    protected Set<Resource> getResources() {
        InventoryManager inventoryManager = PluginContainer.getInstance().getInventoryManager();
        ResourceType resourceType = getResourceType();
        Set<Resource> resources = inventoryManager.getResourcesWithType(resourceType);
        return resources;
    }

    protected void resetConfigFiles(Configuration pluginConfig, boolean deleteRoot) throws IOException {
        if (deleteRoot) {
            deleteAugeasRootDir();
        }

        AUGEAS_ROOT.mkdirs();
        String includes = pluginConfig.getSimpleValue(AugeasConfigurationComponent.INCLUDE_GLOBS_PROP, null);
        if (includes != null) {
            List<String> includeGlobs = new ArrayList<String>();
            includeGlobs.addAll(Arrays.asList(includes.split("\\s*\\|\\s*")));
            for (String includeGlob : includeGlobs) {
                String resourcePath;
                if (IS_WINDOWS) {
                    resourcePath = includeGlob.replace('\\', '/');
                    int colonIndex = resourcePath.indexOf(':');
                    if (colonIndex != -1) {
                        resourcePath = resourcePath.substring(colonIndex + 1);
                    }
                } else {
                    resourcePath = includeGlob;
                }
                InputStream inputStream = this.getClass().getResourceAsStream(resourcePath);
                if (inputStream != null) {
                    File outputFile = new File(AUGEAS_ROOT, resourcePath);
                    outputFile.getParentFile().mkdirs();
                    FileUtil.writeFile(inputStream, outputFile);
                }
            }
        }
    }

    protected boolean isResourceConfigSupported() {
        return isAugeasAvailable();
    }

    protected void tweakDefaultPluginConfig(Configuration defaultPluginConfig) {
        PropertySimple rootPathProp = new PropertySimple(AugeasConfigurationComponent.AUGEAS_ROOT_PATH_PROP,
            AUGEAS_ROOT);
        defaultPluginConfig.put(rootPathProp);
    }

    private boolean isAugeasAvailable() {
        try {
            Aug aug = Aug.INSTANCE;
            return true;
        } catch (Error e) {
            return false;
        }
    }

    private void deleteAugeasRootDir() {
        try {
            FileUtils.purge(AUGEAS_ROOT, true);
        } catch (IOException e) {
            log.warn("Failed to delete Augeas root dir (" + AUGEAS_ROOT + ").");
        }
    }
}
