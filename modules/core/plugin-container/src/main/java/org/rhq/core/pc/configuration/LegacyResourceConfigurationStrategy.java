/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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

package org.rhq.core.pc.configuration;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.configuration.ConfigurationUtility;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

public class LegacyResourceConfigurationStrategy extends BaseResourceConfigurationStrategy {

    private static final Log log = LogFactory.getLog(LegacyResourceConfigurationStrategy.class);

    public Configuration loadConfiguration(int resourceId, boolean fromStructured) throws PluginContainerException {
        Configuration configuration = loadConfigFromFacet(resourceId);
        ResourceType resourceType = componentService.getResourceType(resourceId);

        // If the plugin didn't already set the notes field, set it to something useful.
        if (configuration.getNotes() == null) {
            configuration.setNotes("Resource config for " + resourceType.getName() + " Resource w/ id " + resourceId);
        }

        ConfigurationDefinition configurationDefinition = resourceType.getResourceConfigurationDefinition();

        // Normalize and validate the config.
        ConfigurationUtility.normalizeConfiguration(configuration, configurationDefinition);
        List<String> errorMessages = ConfigurationUtility.validateConfiguration(configuration,
            configurationDefinition);
        for (String errorMessage : errorMessages) {
            log.warn("Plugin Error: Invalid " + resourceType.getName() + " Resource configuration returned by "
                + resourceType.getPlugin() + " plugin - " + errorMessage);
        }

        return configuration;
    }

    private Configuration loadConfigFromFacet(int resourceId) throws PluginContainerException {
        FacetLockType lockType = FacetLockType.READ;
        boolean daemonThread = (lockType != FacetLockType.WRITE);
        boolean onlyIfStarted = true;

        ConfigurationFacet configFacet = componentService.getComponent(resourceId, ConfigurationFacet.class,
            lockType, FACET_METHOD_TIMEOUT, daemonThread, onlyIfStarted);

        Configuration configuration = null;
        try {
            return configFacet.loadResourceConfiguration();
        } catch (Exception e) {
            throw new PluginContainerException(e);
        }
    }

}
