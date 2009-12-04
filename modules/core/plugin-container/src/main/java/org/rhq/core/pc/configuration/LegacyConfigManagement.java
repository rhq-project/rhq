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

import static org.rhq.core.pc.util.FacetLockType.*;
import org.rhq.core.pc.util.FacetLockType;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.configuration.ConfigurationUtility;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

public class LegacyConfigManagement extends ConfigManagementSupport {

    private static final Log log = LogFactory.getLog(LegacyConfigManagement.class);

    public Configuration executeLoad(int resourceId) throws PluginContainerException {
        Configuration configuration = loadConfigFromFacet(resourceId, READ);

        if (configuration == null) {
            return null;
        }
        
        ResourceType resourceType = componentService.getResourceType(resourceId);

        if (configuration.getNotes() == null) {
            configuration.setNotes("Resource config for " + resourceType.getName() + " Resource w/ id " + resourceId);
        }

        ConfigurationDefinition configurationDefinition = resourceType.getResourceConfigurationDefinition();

        configUtilityService.normalizeConfiguration(configuration, configurationDefinition);
        List<String> errorMessages = configUtilityService.validateConfiguration(configuration,
            configurationDefinition);
        for (String errorMessage : errorMessages) {
            log.warn("Plugin Error: Invalid " + resourceType.getName() + " Resource configuration returned by "
                + resourceType.getPlugin() + " plugin - " + errorMessage);
        }

        return configuration;
    }

    private Configuration loadConfigFromFacet(int resourceId, FacetLockType lockType) throws PluginContainerException {
        ConfigurationFacet configFacet = loadConfigurationFacet(resourceId, lockType);

        try {
            return configFacet.loadResourceConfiguration();
        } catch (Exception e) {
            throw new PluginContainerException(e);
        }
    }

    public void executeUpdate(int resourceId, Configuration configuration)
        throws PluginContainerException {
        ConfigurationFacet facet = loadConfigurationFacet(resourceId, WRITE);
        ConfigurationUpdateReport report = new ConfigurationUpdateReport(configuration);

        facet.updateResourceConfiguration(report);

        if (ConfigurationUpdateStatus.SUCCESS == report.getStatus()) {
            return;
        }

        if (ConfigurationUpdateStatus.INPROGRESS == report.getStatus()) {
            throw new UpdateInProgressException();
        }

        if (ConfigurationUpdateStatus.FAILURE == report.getStatus()) {
            throw new ConfigurationUpdateException(report.getErrorMessage());
        }
    }

}
