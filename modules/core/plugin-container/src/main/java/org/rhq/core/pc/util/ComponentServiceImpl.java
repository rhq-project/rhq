/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.core.pc.util;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.plugin.PluginManager;
import org.rhq.core.pluginapi.inventory.ResourceComponent;

public class ComponentServiceImpl implements ComponentService {

    private final PluginManager pluginManager;

    public ComponentServiceImpl(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    public ResourceType getResourceType(int resourceId) throws PluginContainerException {
        return ComponentUtil.getResourceType(resourceId);
    }

    public <T> T getComponent(int resourceId, Class<T> facetInterface, FacetLockType lockType, long timeout,
            boolean daemonThread, boolean onlyIfStarted) throws PluginContainerException {
        return ComponentUtil.getComponent(resourceId, facetInterface, lockType, timeout, daemonThread, onlyIfStarted,
            daemonThread);
    }

    public String getAmpsVersion(int resourceId) throws PluginContainerException {
        ResourceType resourceType = getResourceType(resourceId);
        return pluginManager.getAmpsVersion(resourceType.getPlugin());
    }

    public ResourceComponent fetchResourceComponent(int resourceId) {
        return ComponentUtil.fetchResourceComponent(resourceId);
    }
}
