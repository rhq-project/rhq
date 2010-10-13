/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

package org.rhq.core.pluginapi.upgrade;

import java.io.File;
import java.util.concurrent.Executor;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.pluginapi.content.ContentContext;
import org.rhq.core.pluginapi.event.EventContext;
import org.rhq.core.pluginapi.inventory.PluginContainerDeployment;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.operation.OperationContext;
import org.rhq.core.system.SystemInfo;

/**
 * Represents a resource during the resource upgrade phase of discovery.
 * 
 * @see ResourceUpgradeFacet
 * 
 * @author Lukas Krejci
 */
@SuppressWarnings("unchecked")
public class ResourceUpgradeContext<T extends ResourceComponent> extends ResourceContext<T> {

    private final Configuration resourceConfiguration;
    private final String name;
    private final String description;
    
    /**
     * @see ResourceContext#ResourceContext(Resource, ResourceComponent, ResourceDiscoveryComponent, SystemInfo, File, File, String, EventContext, OperationContext, ContentContext, Executor, PluginContainerDeployment)
     */
    public ResourceUpgradeContext(Resource resource, T parentResourceComponent,
        ResourceDiscoveryComponent resourceDiscoveryComponent, SystemInfo systemInfo, File temporaryDirectory,
        File dataDirectory, String pluginContainerName, EventContext eventContext, OperationContext operationContext,
        ContentContext contentContext, Executor availCollectorThreadPool,
        PluginContainerDeployment pluginContainerDeployment) {
        
        super(resource, parentResourceComponent, resourceDiscoveryComponent, systemInfo, temporaryDirectory, dataDirectory,
            pluginContainerName, eventContext, operationContext, contentContext, availCollectorThreadPool,
            pluginContainerDeployment);
        
        this.resourceConfiguration = resource.getResourceConfiguration();
        this.name = resource.getName();
        this.description = resource.getDescription();
    }

    public ResourceUpgradeContext(Resource resource, ResourceDiscoveryComponent discoveryComponent, ResourceContext<T> context, Executor availCollectorThreadPool) {
        this(resource, context.getParentResourceComponent(), discoveryComponent, context.getSystemInformation(), context.getTemporaryDirectory(),
            context.getDataDirectory(), context.getPluginContainerName(), context.getEventContext(), context.getOperationContext(),
            context.getContentContext(), availCollectorThreadPool, context.getPluginContainerDeployment());
    }
    
    public Configuration getResourceConfiguration() {
        return resourceConfiguration;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
