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

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.RawConfiguration;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.pluginapi.configuration.ResourceConfigurationFacet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.Set;

public class LoadRaw extends BaseLoadConfig {

    private final Log log = LogFactory.getLog(LoadRaw.class);

    public Configuration execute(int resourceId, boolean fromStructured) throws PluginContainerException {
        ResourceConfigurationFacet facet = loadResouceConfiguratonFacet(resourceId);

        Set<RawConfiguration> rawConfigs = facet.loadRawConfigurations();

        if (rawConfigs == null) {
            return null;
        }

        Configuration config = new Configuration();
        for (RawConfiguration rawConfig : rawConfigs) {
            config.addRawConfiguration(rawConfig);
        }

        if (config.getNotes() == null) {
            ResourceType resourceType = componentService.getResourceType(resourceId);
            config.setNotes("Resource config for " + resourceType.getName() + " Resource w/ id " + resourceId);
        }

        return config;
    }

}
