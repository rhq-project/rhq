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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ManualAddFacet;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.augeas.helper.Glob;

/**
 * @author Ian Springer
 * @author Lukas Krejci
 */
public class AugeasConfigurationDiscoveryComponent<T extends ResourceComponent<?>> implements ResourceDiscoveryComponent<T>, ManualAddFacet<T> {
    private final Log log = LogFactory.getLog(this.getClass());

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<T> discoveryContext) throws InvalidPluginConfigurationException,
        Exception {
        Set<DiscoveredResourceDetails> discoveredResources = new HashSet<DiscoveredResourceDetails>(1);

        checkConfigurationFiles(discoveryContext.getDefaultPluginConfiguration());

        Configuration pluginConfig = discoveryContext.getDefaultPluginConfiguration();

        DiscoveredResourceDetails resource = createResourceDetails(discoveryContext, pluginConfig);
        discoveredResources.add(resource);
        log.debug("Discovered " + discoveryContext.getResourceType().getName() + " Resource with key ["
            + resource.getResourceKey() + "].");

        return discoveredResources;
    }

    public DiscoveredResourceDetails discoverResource(Configuration pluginConfig,
        ResourceDiscoveryContext<T> discoveryContext) throws InvalidPluginConfigurationException {
        
        checkConfigurationFiles(pluginConfig);

        DiscoveredResourceDetails resource = createResourceDetails(discoveryContext, pluginConfig);
        return resource;
    }

    protected DiscoveredResourceDetails createResourceDetails(ResourceDiscoveryContext<T> discoveryContext,
        Configuration pluginConfig) {
        ResourceType resourceType = discoveryContext.getResourceType();
        String resourceKey = pluginConfig.getSimpleValue(AugeasConfigurationComponent.AUGEAS_MODULE_NAME_PROP, null);
        DiscoveredResourceDetails resource = new DiscoveredResourceDetails(resourceType, resourceKey, resourceType
            .getName(), null, resourceType.getDescription(), pluginConfig, null);
        return resource;
    }
    
    private void checkConfigurationFiles(Configuration pluginConfiguration) {
        PropertyList includeGlobsProp = pluginConfiguration.getList(AugeasConfigurationComponent.INCLUDE_GLOBS_PROP);
        PropertyList excludeGlobsProp = pluginConfiguration.getList(AugeasConfigurationComponent.EXCLUDE_GLOBS_PROP);
        
        File root = new File(AugeasConfigurationComponent.AUGEAS_ROOT_PATH);
        
        ArrayList<String> includeGlobs = new ArrayList<String>();
        
        if (includeGlobsProp == null) {
            throw new IllegalStateException("Expecting at least once inclusion pattern for configuration files.");
        }
        
        for(Property p : includeGlobsProp.getList()) {
            PropertySimple include = (PropertySimple)p;
            includeGlobs.add(include.getStringValue());
        }
        
        List<File> files = Glob.matchAll(root, includeGlobs);
        
        if (excludeGlobsProp != null) {
            for(Property p : excludeGlobsProp.getList()) {
                PropertySimple exclude = (PropertySimple)p;
                Glob.exclude(files, exclude.getStringValue());
            }
        }
        
        for(File configFile : files) {
            if (!configFile.isAbsolute()) {
                throw new IllegalStateException("Configuration files inclusion patterns contain a non-absolute file.");
            }
            if (!configFile.exists()) {
                throw new IllegalStateException("Configuration files inclusion patterns refer to a non-existent file.");
            }
            if (configFile.isDirectory()) {
                throw new IllegalStateException("Configuration files inclusion patterns refer to a directory.");
            }
        }
    }
}
