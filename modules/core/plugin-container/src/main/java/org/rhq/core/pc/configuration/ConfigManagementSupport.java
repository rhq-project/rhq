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
import org.rhq.core.pc.util.ComponentService;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ResourceConfigurationFacet;

public abstract class ConfigManagementSupport implements ConfigManagement {

    protected ComponentService componentService;

    protected ConfigurationUtilityService configUtilityService;

    public ComponentService getComponentService() {
        return componentService;
    }

    public void setComponentService(ComponentService componentService) {
        this.componentService = componentService;
    }

    public ConfigurationUtilityService getConfigurationUtilityService() {
        return configUtilityService;
    }

    public void setConfigurationUtilityService(ConfigurationUtilityService configUtilityService) {
        this.configUtilityService = configUtilityService;
    }

    protected ResourceConfigurationFacet loadResouceConfiguratonFacet(int resourceId) throws PluginContainerException {
        boolean daemonOnly = true;
        boolean onlyIfStarted = true;

        return componentService.getComponent(resourceId, ResourceConfigurationFacet.class, FacetLockType.READ,
                FACET_METHOD_TIMEOUT, daemonOnly, onlyIfStarted);
    }

    protected ResourceConfigurationFacet loadResourceConfigFacetWithWriteLock(int resourceId)
        throws PluginContainerException {

        FacetLockType lockType = FacetLockType.WRITE;
        boolean daemonThread = (lockType != FacetLockType.WRITE);
        boolean onlyIfStarted = true;

        return componentService.getComponent(resourceId, ResourceConfigurationFacet.class, lockType,
                FACET_METHOD_TIMEOUT, daemonThread, onlyIfStarted);
    }

    protected ConfigurationFacet loadConfigurationFacet(int resourceId, FacetLockType lockType)
        throws PluginContainerException {
        
        boolean daemonThread = (lockType != FacetLockType.WRITE);
        boolean onlyIfStarted = true;

        return componentService.getComponent(resourceId, ConfigurationFacet.class, lockType, FACET_METHOD_TIMEOUT,
            daemonThread, onlyIfStarted);
    }
}
