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
package org.rhq.plugins.jbossas5.script;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.system.ProcessInfo;
import org.rhq.plugins.jbossas5.ApplicationServerComponent;

/**
 * A discovery component for Script services.
 * 
 * @author Ian Springer
 */
public class ScriptDiscoveryComponent implements ResourceDiscoveryComponent<ApplicationServerComponent> {
    private final Log log = LogFactory.getLog(this.getClass());
    static final String SERVER_HOME_DIR = "homeDir";

    public Set<DiscoveredResourceDetails> discoverResources(
        ResourceDiscoveryContext<ApplicationServerComponent> discoveryContext)
        throws InvalidPluginConfigurationException {
        HashSet<DiscoveredResourceDetails> resources = new HashSet<DiscoveredResourceDetails>();
        List<Configuration> pluginConfigs = discoveryContext.getPluginConfigurations();
        if (pluginConfigs.isEmpty()) {
            processAutoDiscoveredResources(discoveryContext, resources);
        } else {
            processManuallyAddedResources(discoveryContext, resources, pluginConfigs);
        }
        return resources;
    }

    private void processAutoDiscoveredResources(ResourceDiscoveryContext<ApplicationServerComponent> discoveryContext,
        HashSet<DiscoveredResourceDetails> resources) {

        Configuration parentPluginConfig = discoveryContext.getParentResourceContext().getPluginConfiguration();
        String homeDir = parentPluginConfig.getSimple(SERVER_HOME_DIR).getStringValue();
        File binDir = new File(homeDir, "bin");
        log.debug("Searching for scripts beneath JBossAS server bin directory (" + binDir + ")...");
        ScriptFileFinder scriptFileFinder = new ScriptFileFinder(discoveryContext.getSystemInformation(), binDir);
        List<File> scriptFiles = scriptFileFinder.findScriptFiles();
        for (File scriptFile : scriptFiles) {
            Configuration pluginConfig = new Configuration();
            pluginConfig.put(new PropertySimple(ScriptComponent.PATH_CONFIG_PROP, scriptFile.getPath()));
            Map<String, String> defaultScriptEnvironment = getDefaultScriptEnvironment();
            pluginConfig.put(new PropertySimple(ScriptComponent.ENVIRONMENT_VARIABLES_CONFIG_PROP,
                toString(defaultScriptEnvironment)));
            DiscoveredResourceDetails resource = createResourceDetails(discoveryContext, pluginConfig);
            log.debug("Auto-discovered script service: " + resource);
            resources.add(resource);
        }
    }

    private void processManuallyAddedResources(ResourceDiscoveryContext<ApplicationServerComponent> discoveryContext,
        HashSet<DiscoveredResourceDetails> resources, List<Configuration> pluginConfigs) {
        for (Configuration pluginConfig : pluginConfigs) {
            File path = new File(pluginConfig.getSimple(ScriptComponent.PATH_CONFIG_PROP).getStringValue());
            validatePath(path);
            DiscoveredResourceDetails resource = createResourceDetails(discoveryContext, pluginConfig);
            log.debug("Manually added script service: " + resource);
            resources.add(resource);
        }
    }

    private void validatePath(File path) {
        if (!path.isAbsolute()) {
            throw new InvalidPluginConfigurationException("Path '" + path + "' is not absolute.");
        }
        if (!path.exists()) {
            throw new InvalidPluginConfigurationException("Path '" + path + "' does not exist.");
        }
        if (path.isDirectory()) {
            throw new InvalidPluginConfigurationException("Path '" + path + "' is a directory, not a file.");
        }
    }

    private Map<String, String> getDefaultScriptEnvironment() {
        Map<String, String> defaultScriptEnvironment = new HashMap();

        // Name the environment variables after the standard JBossAS properties
        // to make things more intuitive for users.
        // Don't use any "."'s in the environment variable names, as they
        // confuse UNIX shells (see JBNADM-2762).
        defaultScriptEnvironment.put("JBOSS_SERVER_NAME", "%serverName%");
        defaultScriptEnvironment.put("JBOSS_SERVER_HOME_DIR", "%serverHomeDir%");
        return defaultScriptEnvironment;
    }

    private String toString(Map<String, String> defaultScriptEnvironment) {
        StringBuilder environmentVariables = new StringBuilder();
        if (defaultScriptEnvironment != null) {
            for (String varName : defaultScriptEnvironment.keySet()) {
                String varValue = defaultScriptEnvironment.get(varName);
                environmentVariables.append(varName).append("=").append(varValue).append("\n");
            }
        }
        return environmentVariables.toString();
    }

    private DiscoveredResourceDetails createResourceDetails(
        ResourceDiscoveryContext<ApplicationServerComponent> discoveryContext, Configuration pluginConfig) {
        String key = pluginConfig.getSimple(ScriptComponent.PATH_CONFIG_PROP).getStringValue();
        String name = new File(key).getName();
        String version = null;
        String description = null;
        ProcessInfo processInfo = null;
        // noinspection ConstantConditions
        return new DiscoveredResourceDetails(discoveryContext.getResourceType(), key, name, version, description,
            pluginConfig, processInfo);
    }
}
