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
import org.rhq.core.domain.configuration.definition.ConfigurationFormat;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceFacets;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.inventory.ResourceContainer;
import org.rhq.core.pc.util.ComponentService;
import org.rhq.core.pc.util.ComponentUtil;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pluginapi.configuration.ResourceConfigurationFacet;
import org.rhq.core.pluginapi.inventory.ResourceComponent;

public class ConfigManagementFactoryImpl implements ConfigManagementFactory {

    private ComponentService componentService;

    private ConfigurationUtilityService configUtilityService = new ConfigurationUtilityServiceImpl();

    public void setComponentService(ComponentService componentService) {
        this.componentService = componentService;
    }

    public ConfigManagement getStrategy(int resourceId) throws PluginContainerException {
        ConfigManagement configManagement = createStrategy(resourceId);
        initStrategyDependencies(configManagement);

        return configManagement;
    }

    private void initStrategyDependencies(ConfigManagement configManagement) {
        configManagement.setComponentService(componentService);
        configManagement.setConfigurationUtilityService(configUtilityService);
    }

    @SuppressWarnings("unchecked")
    private ConfigManagement createStrategy(int resourceId) throws PluginContainerException {
        ResourceType resourceType = componentService.getResourceType(resourceId);
        ConfigurationFormat format = resourceType.getResourceConfigurationDefinition().getConfigurationFormat();

        if (null == format) {
            return new LegacyConfigManagement();
        }

        switch (format) {
        case RAW:
            return new RawConfigManagement();
        case STRUCTURED_AND_RAW:
            return new StructuredAndRawConfigManagement();
        case STRUCTURED:
        default:
            
            if (componentService.fetchResourceComponent(resourceId) instanceof ResourceConfigurationFacet){
                return new StructuredConfigManagement();
            } else{
                return new LegacyConfigManagement();
            }
        }
    }
}
