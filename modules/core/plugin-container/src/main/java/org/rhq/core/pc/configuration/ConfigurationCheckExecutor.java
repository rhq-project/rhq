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
import org.rhq.core.clientapi.agent.configuration.ConfigurationUtility;
import org.rhq.core.clientapi.server.configuration.ConfigurationServerService;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.inventory.ResourceContainer;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;

/**
 * @author Greg Hinkle
 */
public class ConfigurationCheckExecutor implements Runnable, Callable {

    private final Log log = LogFactory.getLog(ConfigurationCheckExecutor.class);

    private ConfigurationManager configurationManager;
    private ConfigurationServerService configurationServerService;
    private InventoryManager inventoryManager;
    private static final long CONFIGURATION_CHECK_TIMEOUT = 30000L;

    public ConfigurationCheckExecutor(ConfigurationManager configurationManager,
        ConfigurationServerService configurationServerService, InventoryManager inventoryManager) {
        this.configurationManager = configurationManager;
        this.configurationServerService = configurationServerService;
        this.inventoryManager = inventoryManager;
    }

    public void run() {
        call();
    }

    public Object call() {
        log.info("Starting configuration update check...");
        long start = System.currentTimeMillis();

        checkConfigurations(this.inventoryManager.getPlatform(), true);
        log.info("Configuration update check completed in " + (System.currentTimeMillis() - start) + "ms");
        return null;
    }

    public void checkConfigurations(Resource resource, boolean checkChildren) {
        ResourceContainer resourceContainer = this.inventoryManager.getResourceContainer(resource);
        ConfigurationFacet resourceComponent = null;
        ResourceType resourceType = resource.getResourceType();

        if (resourceContainer != null && resourceContainer.getAvailability() != null
            && resourceContainer.getAvailability().getAvailabilityType() == AvailabilityType.UP) {

            try {
                resourceComponent = resourceContainer.createResourceComponentProxy(ConfigurationFacet.class,
                    FacetLockType.NONE, CONFIGURATION_CHECK_TIMEOUT, true, false, true);
            } catch (PluginContainerException e) {
                // Expecting when the resource does not support configuration management
            }

            if (resourceComponent != null) {
                // Only report availability for committed resources; don't bother with new, ignored or deleted resources.
                if (resource.getInventoryStatus() == InventoryStatus.COMMITTED
                    && resourceType.getResourceConfigurationDefinition() != null) {

                    if (log.isDebugEnabled()) {
                        log.debug("Checking for updated Resource configuration for " + resource + "...");
                    }

                    try {
                        Configuration liveConfiguration = resourceComponent.loadResourceConfiguration();

                        if (liveConfiguration != null) {
                            ConfigurationDefinition configurationDefinition = resourceType
                                .getResourceConfigurationDefinition();

                            // Normalize and validate the config.
                            ConfigurationUtility.normalizeConfiguration(liveConfiguration, configurationDefinition);
                            List<String> errorMessages = ConfigurationUtility.validateConfiguration(liveConfiguration,
                                configurationDefinition);
                            for (String errorMessage : errorMessages) {
                                log.warn("Plugin Error: Invalid " + resourceType.getName()
                                    + " resource configuration returned by " + resourceType.getPlugin() + " plugin - "
                                    + errorMessage);
                            }

                            Configuration original = getResourceConfiguration(inventoryManager, resource);

                            if (!liveConfiguration.equals(original)) {
                                log.info("New configuration version detected on resource: " + resource);
                                this.configurationServerService.persistUpdatedResourceConfiguration(resource.getId(),
                                    liveConfiguration);
                                //resource.setResourceConfiguration(liveConfiguration);
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
                }
            }

            if (checkChildren) {
                for (Resource child : this.inventoryManager.getContainerChildren(resource, resourceContainer)) {
                    try {
                        checkConfigurations(child, true);
                    } catch (Exception e) {
                        log.error("Failed to check Resource configuration for " + child + ".", e);
                    }
                }
            }
        }
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

    static public Configuration getResourceConfiguration(InventoryManager inventoryManager, Resource resource) {
        Configuration result = resource.getResourceConfiguration();
        if (null == result) {
            result = loadConfigurationFromFile(inventoryManager, resource.getId());
        }
        return result;
    }

    static private Configuration loadConfigurationFromFile(InventoryManager inventoryManager, int resourceId) {
        File baseDataDir = inventoryManager.getDataDirectory();
        String pathname = "rc/" + String.valueOf(resourceId / 1000); // Don't put too many files into one data dir
        File dataDir = new File(baseDataDir, pathname);
        File file = new File(dataDir, String.valueOf(resourceId));
        if (!file.exists()) {
            //log.error("File " + file.getAbsolutePath() + " does not exist");
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

}
