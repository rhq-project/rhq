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

import static org.rhq.core.domain.configuration.definition.ConfigurationFormat.*;

import org.rhq.core.domain.configuration.definition.ConfigurationFormat;
import org.rhq.core.pc.util.ComponentService;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.clientapi.agent.PluginContainerException;
import org.apache.maven.artifact.versioning.ComparableVersion;

public class ConfigManagementFactoryImpl implements ConfigManagementFactory {

    private static final ComparableVersion NON_LEGACY_AMPS_VERSION = new ComparableVersion("2.1");

    private ComponentService componentService;

    private ConfigurationUtilityService configUtilityService = new ConfigurationUtilityServiceImpl();

    public void setComponentService(ComponentService componentService) {
        this.componentService = componentService;
    }

    public ConfigManagement getStrategy(int resourceId) throws PluginContainerException {
        ConfigManagement loadConfig = createStrategy(resourceId);
        initStrategyDependencies(loadConfig);

        return loadConfig;
    }

    private void initStrategyDependencies(ConfigManagement loadConfig) {
        loadConfig.setComponentService(componentService);
        loadConfig.setConfigurationUtilityService(configUtilityService);
    }

    private ConfigManagement createStrategy(int resourceId) throws PluginContainerException {
        String ampsVersion = componentService.getAmpsVersion(resourceId);

        if (isLegacyVersion(ampsVersion)) {
            return new LegacyConfigManagement();
        }

        ResourceType resourceType = componentService.getResourceType(resourceId);

        ConfigurationFormat format = resourceType.getResourceConfigurationDefinition().getConfigurationFormat();
        switch (format) {
            case RAW: break;
            default: ;            
        }

        if (isStructured(resourceType)) {
            return new StructuredConfigManagement();
        }

        if (isRaw(resourceType)) {
            return new RawConfigManagement();
        }

        // else format is both structured and raw
        return new StructuredAndRawConfigManagement();
    }

    private boolean isLegacyVersion(String ampsVersion) {
        return new ComparableVersion(ampsVersion).compareTo(NON_LEGACY_AMPS_VERSION) < 0;
    }

    private boolean isStructured(ResourceType resourceType) {
        return resourceType.getResourceConfigurationDefinition().getConfigurationFormat().equals(STRUCTURED);
    }

    private boolean isRaw(ResourceType resourceType) {
        return resourceType.getResourceConfigurationDefinition().getConfigurationFormat().equals(RAW);
    }
}
