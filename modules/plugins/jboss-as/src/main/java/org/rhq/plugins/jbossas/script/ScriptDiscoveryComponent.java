 /*
  * Jopr Management Platform
  * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.plugins.jbossas.script;

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
import org.rhq.core.domain.resource.ResourceUpgradeReport;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.upgrade.ResourceUpgradeContext;
import org.rhq.core.pluginapi.upgrade.ResourceUpgradeFacet;
import org.rhq.core.system.ProcessInfo;
import org.rhq.plugins.jbossas.JBossASServerComponent;

/**
 * A discovery component for Script services.
 *
 * @author Ian Springer
 */
public class ScriptDiscoveryComponent implements ResourceDiscoveryComponent<JBossASServerComponent>,
        ResourceUpgradeFacet<JBossASServerComponent> {

    private final Log log = LogFactory.getLog(this.getClass());

    public Set<DiscoveredResourceDetails> discoverResources(
            ResourceDiscoveryContext<JBossASServerComponent> discoveryContext)
            throws InvalidPluginConfigurationException {
        Set<DiscoveredResourceDetails> resources = new HashSet<DiscoveredResourceDetails>();
        // TODO: Upgrade to new manual discovery API.
        List<Configuration> pluginConfigs = discoveryContext.getPluginConfigurations();
        if (pluginConfigs.isEmpty()) {
            processAutoDiscoveredResources(discoveryContext, resources);
        } else {
            processManuallyAddedResources(discoveryContext, resources, pluginConfigs);
        }
        return resources;
    }

    @Override
    public ResourceUpgradeReport upgrade(ResourceUpgradeContext<JBossASServerComponent> upgradeContext) {
        String inventoriedResourceKey = upgradeContext.getResourceKey();

        ResourceContext parentResourceContext = upgradeContext.getParentResourceContext();
        File binDir = getServerBinDirectory(parentResourceContext);

        // The new format is to use paths relative to the server bin dir for scripts in that dir and to otherwise
        // use absolute paths, so we only need to upgrade existing keys that start with the bin dir path.
        if (!inventoriedResourceKey.startsWith(binDir.getPath())) {
            // key is already in the new format
            return null;
        }

        // key is in the old format - build a key in the new format
        String resourceKey = buildResourceKey(inventoriedResourceKey, binDir);

        ResourceUpgradeReport upgradeReport = new ResourceUpgradeReport();
        upgradeReport.setNewResourceKey(resourceKey);

        return upgradeReport;
    }

    private File getServerBinDirectory(ResourceContext parentResourceContext) {
        Configuration parentPluginConfig = parentResourceContext.getPluginConfiguration();
        String homeDir = parentPluginConfig.getSimple(JBossASServerComponent.JBOSS_HOME_DIR_CONFIG_PROP).getStringValue();
        return new File(homeDir, "bin");
    }

    private static String buildResourceKey(String scriptPath, File binDir) {
        return (scriptPath.startsWith(binDir.getPath())) ? scriptPath.substring(binDir.getPath().length() + 1) :
                scriptPath;
    }

    private void processAutoDiscoveredResources(ResourceDiscoveryContext<JBossASServerComponent> discoveryContext,
                                                Set<DiscoveredResourceDetails> resources) {
        ResourceContext parentResourceContext = discoveryContext.getParentResourceContext();
        File binDir = getServerBinDirectory(parentResourceContext);
        log.debug("Searching for scripts beneath JBossAS server bin directory (" + binDir + ")...");
        ScriptFileFinder scriptFileFinder = new ScriptFileFinder(discoveryContext.getSystemInformation(), binDir);
        List<File> scriptFiles = scriptFileFinder.findScriptFiles();
        for (File scriptFile : scriptFiles) {
            Configuration pluginConfig = new Configuration();
            pluginConfig.put(new PropertySimple(ScriptComponent.PATH_CONFIG_PROP, scriptFile.getPath()));
            Map<String, String> defaultScriptEnvironment = getDefaultScriptEnvironment();
            pluginConfig.put(new PropertySimple(ScriptComponent.ENVIRONMENT_VARIABLES_CONFIG_PROP,
                toString(defaultScriptEnvironment)));
            DiscoveredResourceDetails resource = createResourceDetails(discoveryContext, pluginConfig, binDir);
            log.debug("Auto-discovered script service: " + resource);
            resources.add(resource);
        }
    }

    private void processManuallyAddedResources(ResourceDiscoveryContext<JBossASServerComponent> discoveryContext,
                                               Set<DiscoveredResourceDetails> resources, List<Configuration> pluginConfigs) {
        ResourceContext parentResourceContext = discoveryContext.getParentResourceContext();
        File binDir = getServerBinDirectory(parentResourceContext);
        for (Configuration pluginConfig : pluginConfigs) {
            File path = new File(pluginConfig.getSimple(ScriptComponent.PATH_CONFIG_PROP).getStringValue());
            validatePath(path);
            DiscoveredResourceDetails resource = createResourceDetails(discoveryContext, pluginConfig, binDir);
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
        Map<String, String> defaultScriptEnvironment = new HashMap<String, String>();

        // Name the environment variables after the standard JBossAS properties to make things more intuitive for users.
        // Don't use any "."'s in the environment variable names, as they confuse UNIX shells (see JBNADM-2762).
        defaultScriptEnvironment.put("JBOSS_SERVER_NAME", "%configurationSet%");
        defaultScriptEnvironment.put("JBOSS_SERVER_HOME_DIR", "%configurationPath%");
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
        ResourceDiscoveryContext<JBossASServerComponent> discoveryContext, Configuration pluginConfig, File homeDir) {
        String path = pluginConfig.getSimple(ScriptComponent.PATH_CONFIG_PROP).getStringValue();
        String key = buildResourceKey(path, homeDir);
        String name = new File(path).getName();
        String version = null;
        String description = null;
        ProcessInfo processInfo = null;
        //noinspection ConstantConditions
        return new DiscoveredResourceDetails(discoveryContext.getResourceType(), key, name, version, description,
            pluginConfig, processInfo);
    }

}