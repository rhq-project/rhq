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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.RawConfiguration;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.configuration.ResourceConfigurationFacet;

import java.util.List;
import java.util.Set;

import static java.util.Collections.EMPTY_SET;

public class StructuredAndRawConfigManagement extends ConfigManagementSupport {

    private final Log log = LogFactory.getLog(StructuredAndRawConfigManagement.class);

    public Configuration executeLoad(int resourceId) throws PluginContainerException {
        ResourceConfigurationFacet facet = loadResouceConfiguratonFacet(resourceId);

        Configuration configuration = facet.loadStructuredConfiguration();
        Set<RawConfiguration> rawConfigs = facet.loadRawConfigurations();

        if (configuration == null && rawConfigs == null) {
            return null;
        }

        if (configuration == null) {
            configuration = new Configuration();
        }

        if (rawConfigs == null) {
            rawConfigs = EMPTY_SET;
        }

        for (RawConfiguration rawConfig : rawConfigs) {
            configuration.addRawConfiguration(rawConfig);
        }

        ResourceType resourceType = componentService.getResourceType(resourceId);

        if (configuration.getNotes() == null) {
            configuration.setNotes("Resource config for " + resourceType.getName() + " Resource w/ id " + resourceId);
        }

        configUtilityService.normalizeConfiguration(configuration, resourceType.getResourceConfigurationDefinition());
        List<String> errorMsgs = configUtilityService.validateConfiguration(configuration,
            resourceType.getResourceConfigurationDefinition());

        logErrorMsgs(errorMsgs, resourceType);

        return configuration;
    }

    private void logErrorMsgs(List<String> errorMsgs, ResourceType resourceType) {
        for (String errorMessage : errorMsgs) {
            log.warn("Plugin Error: Invalid " + resourceType.getName() + " Resource configuration returned by "
                + resourceType.getPlugin() + " plugin - " + errorMessage);
            }
    }

    public void executeUpdate(int resourceId, Configuration configuration) throws PluginContainerException {
        ResourceConfigurationFacet facet = loadResourceConfigFacetWithWriteLock(resourceId);

        StringBuilder errors = new StringBuilder();

        try {
            facet.validateStructuredConfiguration(configuration);
            facet.persistStructuredConfiguration(configuration);
        }
        catch (Throwable t) {
            errors.append(t.getMessage());
        }

        for (RawConfiguration rawConfig : configuration.getRawConfigurations()) {
            try {
                facet.validateRawConfiguration(rawConfig);
                facet.persistRawConfiguration(rawConfig);
            }
            catch (Throwable t) {
                errors.append(t.getMessage()).append("\n\n");
            }
        }

        if (errors.length() > 0) {
            throw new ConfigurationUpdateException(errors.toString());
        }
    }
}
