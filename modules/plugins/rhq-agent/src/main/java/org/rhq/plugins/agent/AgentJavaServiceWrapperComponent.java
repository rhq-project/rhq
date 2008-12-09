/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.plugins.agent;

import java.io.File;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.util.exception.ExceptionPackage;
import org.rhq.core.util.exception.Severity;
import org.rhq.enterprise.agent.EnvironmentScriptFileUpdate;
import org.rhq.enterprise.agent.EnvironmentScriptFileUpdate.NameValuePair;

/**
 * The component that represents the agent's Java Service Wrapper (JSW).
 *
 * @author John Mazzitelli
 */
public class AgentJavaServiceWrapperComponent implements ResourceComponent<AgentServerComponent>, ConfigurationFacet {
    private Log log = LogFactory.getLog(AgentJavaServiceWrapperComponent.class);

    private File configFile;
    private File environmentFile;
    private File includeFile;

    public void start(ResourceContext<AgentServerComponent> resourceContext) throws Exception {
        Configuration pc = resourceContext.getPluginConfiguration();
        PropertySimple pathnameProp = pc.getSimple(AgentJavaServiceWrapperDiscoveryComponent.PLUGINCONFIG_CONF_FILE);
        if (pathnameProp == null) {
            throw new InvalidPluginConfigurationException("Missing Configuration File");
        }
        if (pathnameProp.getStringValue() == null) {
            throw new InvalidPluginConfigurationException("Configuration File property value is null");
        }

        configFile = new File(pathnameProp.getStringValue());
        if (!configFile.exists()) {
            throw new InvalidPluginConfigurationException("Config file [" + configFile + "] does not exist");
        }

        log.debug("Starting agent JSW component: " + configFile);

        // get the optional files (these may remain null if the paths were left undefined)
        pathnameProp = pc.getSimple(AgentJavaServiceWrapperDiscoveryComponent.PLUGINCONFIG_ENV_FILE);
        if (pathnameProp != null && pathnameProp.getStringValue() != null) {
            environmentFile = new File(pathnameProp.getStringValue());
        }

        pathnameProp = pc.getSimple(AgentJavaServiceWrapperDiscoveryComponent.PLUGINCONFIG_INC_FILE);
        if (pathnameProp != null && pathnameProp.getStringValue() != null) {
            includeFile = new File(pathnameProp.getStringValue());
        }

        return;
    }

    public void stop() {
        // nothing to do
    }

    public AvailabilityType getAvailability() {
        return (configFile.exists()) ? AvailabilityType.UP : AvailabilityType.DOWN;
    }

    public Configuration loadResourceConfiguration() throws Exception {
        Configuration config = new Configuration();
        PropertyList conf = loadConfigurationFileConfiguration();
        PropertyList env = loadEnvironmentFileConfiguration();
        PropertyList inc = loadIncludeFileConfiguration();
        if (conf != null) {
            config.put(conf);
        }
        if (env != null) {
            config.put(env);
        }
        if (inc != null) {
            config.put(inc);
        }
        return config;
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport request) {
        try {
            throw new UnsupportedOperationException();
        } catch (Exception e) {
            request.setErrorMessage(new ExceptionPackage(Severity.Severe, e).toString());
        }

        return;
    }

    private PropertyList loadConfigurationFileConfiguration() throws Exception {
        if (configFile == null || !configFile.exists()) {
            return null;
        }

        // read in the file and get all the settings it defines
        EnvironmentScriptFileUpdate updater = EnvironmentScriptFileUpdate.create(configFile.getAbsolutePath());
        List<NameValuePair> properties = updater.loadExisting();

        // put the env var definitions in a config object
        PropertyList list = new PropertyList("mainConfigurationSettings");

        for (NameValuePair prop : properties) {
            PropertyMap map = new PropertyMap("mainConfigurationSetting");
            map.put(new PropertySimple("name", prop.name));
            map.put(new PropertySimple("value", prop.value));
            list.add(map);
        }

        return list;
    }

    private PropertyList loadEnvironmentFileConfiguration() throws Exception {
        if (environmentFile == null || !environmentFile.exists()) {
            return null;
        }

        // read in the file and get all the settings it defines
        EnvironmentScriptFileUpdate updater = EnvironmentScriptFileUpdate.create(environmentFile.getAbsolutePath());
        List<NameValuePair> properties = updater.loadExisting();

        // put the env var definitions in a config object
        PropertyList list = new PropertyList("environmentSettings");

        for (NameValuePair prop : properties) {
            PropertyMap map = new PropertyMap("environmentSetting");
            map.put(new PropertySimple("name", prop.name));
            map.put(new PropertySimple("value", prop.value));
            list.add(map);
        }

        return list;
    }

    private PropertyList loadIncludeFileConfiguration() throws Exception {
        if (includeFile == null || !includeFile.exists()) {
            return null;
        }

        // read in the file and get all the settings it defines
        EnvironmentScriptFileUpdate updater = EnvironmentScriptFileUpdate.create(includeFile.getAbsolutePath());
        List<NameValuePair> properties = updater.loadExisting();

        // put the env var definitions in a config object
        PropertyList list = new PropertyList("includeSettings");

        for (NameValuePair prop : properties) {
            PropertyMap map = new PropertyMap("includeSetting");
            map.put(new PropertySimple("name", prop.name));
            map.put(new PropertySimple("value", prop.value));
            list.add(map);
        }

        return list;
    }
}