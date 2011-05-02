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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ManualAddFacet;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.augeas.util.Glob;

/**
 * @author Ian Springer
 * @author Lukas Krejci
 */
public class AugeasConfigurationDiscoveryComponent<T extends ResourceComponent> implements
    ResourceDiscoveryComponent<T>, ManualAddFacet<T> {
    private final Log log = LogFactory.getLog(this.getClass());

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<T> discoveryContext)
        throws InvalidPluginConfigurationException, Exception {
        Set<DiscoveredResourceDetails> discoveredResources = new HashSet<DiscoveredResourceDetails>(1);

        List<String> includes = determineIncludeGlobs(discoveryContext);
        List<String> excludes = determineExcludeGlobs(discoveryContext);

        Configuration pluginConfig = discoveryContext.getDefaultPluginConfiguration();
        PropertySimple includeProps = getGlobList(AugeasConfigurationComponent.INCLUDE_GLOBS_PROP, includes);
        PropertySimple excludeProps = getGlobList(AugeasConfigurationComponent.EXCLUDE_GLOBS_PROP, excludes);
        pluginConfig.put(includeProps);
        pluginConfig.put(excludeProps);

        try {
            checkFiles(pluginConfig);

            DiscoveredResourceDetails resource = createResourceDetails(discoveryContext, pluginConfig);
            discoveredResources.add(resource);
            log.debug("Discovered " + discoveryContext.getResourceType().getName() + " Resource with key ["
                + resource.getResourceKey() + "].");
        } catch (IllegalStateException e) { // Thrown by augeas if it can not read a file
            log.warn("Discovery failed: " + e.getMessage());
        }

        return discoveredResources;
    }

    public DiscoveredResourceDetails discoverResource(Configuration pluginConfig,
        ResourceDiscoveryContext<T> discoveryContext) throws InvalidPluginConfigurationException {

        checkFiles(pluginConfig);

        DiscoveredResourceDetails resource = createResourceDetails(discoveryContext, pluginConfig);
        return resource;
    }

    protected DiscoveredResourceDetails createResourceDetails(ResourceDiscoveryContext<T> discoveryContext,
        Configuration pluginConfig) {
        ResourceType resourceType = discoveryContext.getResourceType();
        String resourceKey = composeResourceKey(pluginConfig);
        DiscoveredResourceDetails resource = new DiscoveredResourceDetails(resourceType, resourceKey, resourceType
            .getName(), null, resourceType.getDescription(), pluginConfig, null);
        return resource;
    }

    protected List<String> determineIncludeGlobs(ResourceDiscoveryContext<T> discoveryContext) {
        Configuration pluginConfiguration = discoveryContext.getDefaultPluginConfiguration();
        PropertySimple includeGlobsProp = pluginConfiguration
            .getSimple(AugeasConfigurationComponent.INCLUDE_GLOBS_PROP);

        List<String> ret = getGlobList(includeGlobsProp);
        if (ret == null || ret.size() == 0) {
            throw new IllegalStateException("Expecting at least once inclusion pattern for configuration files.");
        }

        return ret;
    }

    protected List<String> determineExcludeGlobs(ResourceDiscoveryContext<T> discoveryContext) {
        Configuration pluginConfiguration = discoveryContext.getDefaultPluginConfiguration();
        PropertySimple excludeGlobsProp = pluginConfiguration
            .getSimple(AugeasConfigurationComponent.EXCLUDE_GLOBS_PROP);

        List<String> ret = getGlobList(excludeGlobsProp);

        return ret;
    }

    private void checkFiles(Configuration pluginConfiguration) {
        PropertySimple includeGlobsProp = pluginConfiguration
            .getSimple(AugeasConfigurationComponent.INCLUDE_GLOBS_PROP);
        PropertySimple excludeGlobsProp = pluginConfiguration
            .getSimple(AugeasConfigurationComponent.EXCLUDE_GLOBS_PROP);

        String augeasRootPath = pluginConfiguration.getSimpleValue(AugeasConfigurationComponent.AUGEAS_ROOT_PATH_PROP,
                AugeasConfigurationComponent.DEFAULT_AUGEAS_ROOT_PATH);
        File root = new File(augeasRootPath);

        List<String> includeGlobs = getGlobList(includeGlobsProp);

        if (includeGlobsProp == null) {
            throw new IllegalStateException("Expecting at least one inclusion pattern for configuration files.");
        }

        List<File> files = Glob.matchAll(root, includeGlobs, Glob.ALPHABETICAL_COMPARATOR);

        if (excludeGlobsProp != null) {
            List<String> excludeGlobs = getGlobList(excludeGlobsProp);
            Glob.excludeAll(files, excludeGlobs);
        }

        for (File configFile : files) {
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

    private String composeResourceKey(Configuration pluginConfiguration) {
        PropertySimple includeGlobsProp = pluginConfiguration
            .getSimple(AugeasConfigurationComponent.INCLUDE_GLOBS_PROP);
        PropertySimple excludeGlobsProp = pluginConfiguration
            .getSimple(AugeasConfigurationComponent.EXCLUDE_GLOBS_PROP);

        StringBuilder bld = new StringBuilder();

        bld.append(includeGlobsProp.getStringValue());

        if (excludeGlobsProp != null && excludeGlobsProp.getStringValue().length() > 0) {
            bld.append("---");
            bld.append(excludeGlobsProp.getStringValue());
        }

        return bld.toString();
    }

    public static PropertySimple getGlobList(String name, List<String> simples) {
        StringBuilder bld = new StringBuilder();
        if (simples != null) {
            for (String s : simples) {
                bld.append(s).append("|");
            }
        }
        if (bld.length() > 0) {
            bld.deleteCharAt(bld.length() - 1);
        }
        return new PropertySimple(name, bld);
    }

    public static List<String> getGlobList(PropertySimple list) {
        if (list != null) {
            return Arrays.asList(list.getStringValue().split("\\s*\\|\\s*"));
        } else {
            return Collections.emptyList();
        }
    }
}
