/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ManualAddFacet;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * @author Ian Springer
 */
public class AugeasConfigurationDiscoveryComponent implements ResourceDiscoveryComponent, ManualAddFacet {
    private final Log log = LogFactory.getLog(this.getClass());

    public Set discoverResources(ResourceDiscoveryContext discoveryContext) throws InvalidPluginConfigurationException,
        Exception {
        Set<DiscoveredResourceDetails> discoveredResources = new HashSet<DiscoveredResourceDetails>(1);

        File configFile = getConfigurationFile(discoveryContext);
        if (!configFile.isAbsolute()) {
            throw new IllegalStateException("getConfigurationFile() returned a non-absolute file.");
        }
        if (!configFile.exists()) {
            throw new IllegalStateException("getConfigurationFile() returned a non-existent file.");
        }
        if (configFile.isDirectory()) {
            throw new IllegalStateException("getConfigurationFile() returned a directory.");
        }

        Configuration pluginConfig = discoveryContext.getDefaultPluginConfiguration();
        pluginConfig
            .put(new PropertySimple(AugeasConfigurationComponent.CONFIGURATION_FILE_PROP, configFile.getPath()));
        DiscoveredResourceDetails resource = createResourceDetails(discoveryContext, pluginConfig);
        discoveredResources.add(resource);
        log.debug("Discovered " + discoveryContext.getResourceType().getName() + " Resource with key ["
            + resource.getResourceKey() + "].");

        return discoveredResources;
    }

    public DiscoveredResourceDetails discoverResource(Configuration pluginConfig,
        ResourceDiscoveryContext discoveryContext) throws InvalidPluginConfigurationException {
        String configFilePath = pluginConfig.getSimple(AugeasConfigurationComponent.CONFIGURATION_FILE_PROP)
            .getStringValue();

        File configFile = new File(configFilePath);
        if (!configFile.isAbsolute()) {
            throw new InvalidPluginConfigurationException("Location specified by '"
                + AugeasConfigurationComponent.CONFIGURATION_FILE_PROP
                + "' connection property is not an absolute path.");
        }
        if (!configFile.exists()) {
            throw new InvalidPluginConfigurationException("Location specified by '"
                + AugeasConfigurationComponent.CONFIGURATION_FILE_PROP + "' connection property does not exist.");
        }
        if (configFile.isDirectory()) {
            throw new InvalidPluginConfigurationException("Location specified by '"
                + AugeasConfigurationComponent.CONFIGURATION_FILE_PROP
                + "' connection property is a directory, not a regular file.");
        }

        DiscoveredResourceDetails resource = createResourceDetails(discoveryContext, pluginConfig);
        return resource;
    }

    protected File getConfigurationFile(ResourceDiscoveryContext discoveryContext) {
        Configuration defaultPluginConfig = discoveryContext.getDefaultPluginConfiguration();
        String configFilePath = defaultPluginConfig.getSimple(AugeasConfigurationComponent.CONFIGURATION_FILE_PROP)
            .getStringValue();
        return new File(configFilePath);
    }

    protected DiscoveredResourceDetails createResourceDetails(ResourceDiscoveryContext discoveryContext,
        Configuration pluginConfig) {
        ResourceType resourceType = discoveryContext.getResourceType();
        String resourceKey = getConfigurationFile(discoveryContext).getPath();
        DiscoveredResourceDetails resource = new DiscoveredResourceDetails(resourceType, resourceKey, resourceType
            .getName(), null, resourceType.getDescription(), pluginConfig, null);
        return resource;
    }
}
