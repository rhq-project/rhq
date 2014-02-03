/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
package org.rhq.core.pc.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.server.configuration.ConfigurationServerService;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUtility;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.inventory.ResourceContainer;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;

/**
 * @author Greg Hinkle
 */
public class ConfigurationCheckExecutor implements Runnable, Callable {

    private static final Log log = LogFactory.getLog(ConfigurationCheckExecutor.class);

    private ConfigurationServerService configurationServerService;
    private static final long CONFIGURATION_CHECK_TIMEOUT = 30000L;

    public ConfigurationCheckExecutor(ConfigurationServerService configurationServerService) {
        this.configurationServerService = configurationServerService;
    }

    public void run() {
        call();
    }

    public Object call() {
        log.info("Starting configuration update check...");
        long start = System.currentTimeMillis();

        CountTime countTime;
        InventoryManager inventoryManager = PluginContainer.getInstance().getInventoryManager();
        Resource platform = inventoryManager.getPlatform();
        countTime = checkConfigurations(inventoryManager, platform, true);
        log.info("Configuration update check for [" + countTime.count + "] resources completed in "
            + (System.currentTimeMillis() - start) / 1000 + "s wall time, " + countTime.time + "ms check time");
        return null;
    }

    public CountTime checkConfigurations(InventoryManager inventoryManager, Resource resource, boolean checkChildren) {
        ResourceContainer resourceContainer = inventoryManager.getResourceContainer(resource.getId());
        ConfigurationFacet resourceComponent = null;
        ResourceType resourceType = resource.getResourceType();

        CountTime countTime = new CountTime();
        boolean debugEnabled = log.isDebugEnabled();

        if (resourceContainer != null && resourceContainer.getAvailability() != null
            && resourceContainer.getAvailability().getAvailabilityType() == AvailabilityType.UP) {

            if (resourceContainer.supportsFacet(ConfigurationFacet.class)) {
                try {
                    resourceComponent = resourceContainer.createResourceComponentProxy(ConfigurationFacet.class,
                        FacetLockType.NONE, CONFIGURATION_CHECK_TIMEOUT, true, false, true);
                } catch (PluginContainerException e) {
                    // Expecting when the resource does not support configuration management
                    // Should never happen after above check
                }
            }

            if (resourceComponent != null) {
                // Only report availability for committed resources; don't bother with new, ignored or deleted resources.
                if (resource.getInventoryStatus() == InventoryStatus.COMMITTED
                    && resourceType.getResourceConfigurationDefinition() != null) {

                    long t1 = System.currentTimeMillis();

                    if (debugEnabled) {
                        log.debug("Checking for updated Resource configuration for " + resource + "...");
                    }

                    try {
                        Configuration liveConfiguration = resourceComponent.loadResourceConfiguration();

                        if (liveConfiguration != null) {
                            ConfigurationDefinition configurationDefinition = resourceType
                                .getResourceConfigurationDefinition();

                            // Normalize and validate the config.
                            ConfigurationUtility.normalizeConfiguration(liveConfiguration, configurationDefinition,
                                true, true);
                            List<String> errorMessages = ConfigurationUtility.validateConfiguration(liveConfiguration,
                                configurationDefinition);
                            for (String errorMessage : errorMessages) {
                                log.warn("Plugin Error: Invalid " + resourceType.getName()
                                    + " resource configuration returned by " + resourceType.getPlugin() + " plugin - "
                                    + errorMessage);
                            }

                            Configuration original = getResourceConfiguration(inventoryManager, resource);

                            if (original == null) {
                                original = loadConfigurationFromFile(inventoryManager, resource.getId());
                            }

                            if (!liveConfiguration.equals(original)) {
                                if (debugEnabled) {
                                    log.debug("New configuration version detected on resource: " + resource);
                                }
                                this.configurationServerService.persistUpdatedResourceConfiguration(resource.getId(),
                                    liveConfiguration);
                                //                                resource.setResourceConfiguration(liveConfiguration);
                                boolean persisted = persistConfigurationToFile(inventoryManager, resource.getId(),
                                    liveConfiguration, log);
                                if (persisted) {
                                    resource.setResourceConfiguration(null);
                                }
                            }
                        }
                    } catch (Throwable t) {
                        log.warn("An error occurred while checking for an updated Resource configuration for "
                            + resource + ".", t);
                    }

                    long now = System.currentTimeMillis();
                    countTime.add(1, (now - t1));

                    // Give the agent some time to breathe
                    try {
                        Thread.sleep(750);
                    } catch (InterruptedException e) {
                        ; // We don't care
                    }
                }
            }

            if (checkChildren) {
                for (Resource child : inventoryManager.getContainerChildren(resource, resourceContainer)) {
                    try {
                        CountTime inner = checkConfigurations(inventoryManager, child, true);
                        countTime.add(inner.count, inner.time);
                    } catch (Exception e) {
                        log.error("Failed to check Resource configuration for " + child + ".", e);
                    }
                }
            }
        }
        return countTime;
    }

    static public Configuration getResourceConfiguration(InventoryManager inventoryManager, Resource resource) {
        Configuration result = resource.getResourceConfiguration();
        if (null == result) {
            result = loadConfigurationFromFile(inventoryManager, resource.getId());
        }
        return result;
    }

    static public boolean persistConfigurationToFile(InventoryManager inventoryManager, int resourceId,
        Configuration liveConfiguration, Log log) {

        boolean success = true;
        try {
            File baseDataDir = inventoryManager.getDataDirectory();
            String pathname = "rc/" + String.valueOf(resourceId / 1000); // Don't put too many files into one data dir
            File dataDir = new File(baseDataDir, pathname);
            if (!dataDir.exists()) {
                success = dataDir.mkdirs();
                if (!success) {
                    log.warn("Could not create data dir " + dataDir.getAbsolutePath());
                    return false;
                }
            }
            File file = new File(dataDir, String.valueOf(resourceId));
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(liveConfiguration);
            oos.flush();
            oos.close();
            fos.flush();
            fos.close();
        } catch (IOException e) {
            log.warn("Persisting failed: " + e.getMessage());
            success = false;
        }
        return success;

    }

    static private Configuration loadConfigurationFromFile(InventoryManager inventoryManager, int resourceId) {
        File baseDataDir = inventoryManager.getDataDirectory();
        String pathname = "rc/" + String.valueOf(resourceId / 1000); // Don't put too many files into one data dir
        File dataDir = new File(baseDataDir, pathname);
        File file = new File(dataDir, String.valueOf(resourceId));
        if (!file.exists()) {
            log.error("File " + file.getAbsolutePath() + " does not exist");
            return new Configuration();
        }

        try {
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            Configuration config = (Configuration) ois.readObject();
            ois.close();
            fis.close();
            return config;
        } catch (IOException e) {
            e.printStackTrace(); // TODO: Customize this generated block
        } catch (ClassNotFoundException e) {
            e.printStackTrace(); // TODO: Customize this generated block
        }

        return new Configuration();
    }

    private static class CountTime {
        private long count = 0L;
        private long time = 0L;

        private void add(long count, long time) {

            this.count += count;
            this.time += time;
        }

        @Override
        public String toString() {
            return "CountTime{" + "count=" + count + ", time=" + time + '}';
        }
    }

}
