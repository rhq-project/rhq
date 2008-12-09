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
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.Property;
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

/**
 * The component that represents the agent's environment setup script.
 *
 * @author John Mazzitelli
 */
public class AgentEnvironmentScriptComponent implements ResourceComponent<AgentServerComponent>, ConfigurationFacet {
    private Log log = LogFactory.getLog(AgentEnvironmentScriptComponent.class);

    private File script;

    @Override
    public void start(ResourceContext<AgentServerComponent> resourceContext) throws Exception {
        Configuration pc = resourceContext.getPluginConfiguration();
        PropertySimple pathnameProp = pc.getSimple(AgentEnvironmentScriptDiscoveryComponent.PLUGINCONFIG_PATHNAME);
        if (pathnameProp == null) {
            throw new InvalidPluginConfigurationException("Pathname property is missing");
        }
        if (pathnameProp.getStringValue() == null) {
            throw new InvalidPluginConfigurationException("Pathname property value is null");
        }

        script = new File(pathnameProp.getStringValue());
        if (!script.exists()) {
            throw new InvalidPluginConfigurationException("Script [" + script + "] does not exist");
        }

        log.debug("Starting agent env script component: " + script);

        // we've got the script pathname and it does exist, we are good to start
        return;
    }

    @Override
    public void stop() {
        // nothing to do
    }

    @Override
    public AvailabilityType getAvailability() {
        return (script.exists()) ? AvailabilityType.UP : AvailabilityType.DOWN;
    }

    public Configuration loadResourceConfiguration() throws Exception {
        // read in the env script file and get all the env vars it defines
        EnvironmentScriptFileUpdate updater = EnvironmentScriptFileUpdate.create(script.getAbsolutePath());
        Properties variables = updater.loadExisting();

        // put the env var definitions in a config object
        Configuration config = new Configuration();
        PropertyList list = new PropertyList("environmentVariables");
        config.put(list);

        for (Map.Entry<Object, Object> pref : variables.entrySet()) {
            PropertyMap map = new PropertyMap("environmentVariable");
            map.put(new PropertySimple("name", pref.getKey()));
            map.put(new PropertySimple("value", pref.getValue()));
            list.add(map);
        }

        return config;
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport request) {
        try {
            Properties newSettings = new Properties();

            Configuration configuration = request.getConfiguration();
            PropertyList list = configuration.getList("environmentVariables");

            for (Property item : list.getList()) {
                PropertyMap map = (PropertyMap) item;
                PropertySimple name = map.getSimple("name");
                PropertySimple value = map.getSimple("value");

                if (name == null || name.getStringValue() == null) {
                    log.error("Missing an env var name: " + configuration.toString(true));
                    throw new IllegalArgumentException("Missing the name of an environment variable");
                }

                if (value != null && value.getStringValue() != null) {
                    newSettings.setProperty(name.getStringValue(), value.getStringValue());
                }
            }

            // update the env script file so it includes the new settings.
            // note that we require the request to contain ALL settings, not a subset; any settings
            // missing in the request config that currently exist in the script will be removed from the script.
            EnvironmentScriptFileUpdate updater = EnvironmentScriptFileUpdate.create(script.getAbsolutePath());
            updater.update(newSettings, true);

            request.setStatus(ConfigurationUpdateStatus.SUCCESS);
        } catch (Exception e) {
            request.setErrorMessage(new ExceptionPackage(Severity.Severe, e).toString());
        }

        return;
    }
}