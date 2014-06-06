/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
package org.rhq.core.pluginapi.upgrade;

import java.io.File;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.pluginapi.availability.AvailabilityContext;
import org.rhq.core.pluginapi.content.ContentContext;
import org.rhq.core.pluginapi.event.EventContext;
import org.rhq.core.pluginapi.inventory.InventoryContext;
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
 * @since 3.0
 * @author Lukas Krejci
 */
public class ResourceUpgradeContext<T extends ResourceComponent<?>> extends ResourceContext<T> {

    private final Configuration resourceConfiguration;
    private final String name;
    private final String description;
    private final String version;

    /**
     * @see ResourceContext#ResourceContext(org.rhq.core.domain.resource.Resource, org.rhq.core.pluginapi.inventory.ResourceComponent, org.rhq.core.pluginapi.inventory.ResourceContext, org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent, org.rhq.core.system.SystemInfo, java.io.File, java.io.File, String, org.rhq.core.pluginapi.event.EventContext, org.rhq.core.pluginapi.operation.OperationContext, org.rhq.core.pluginapi.content.ContentContext, org.rhq.core.pluginapi.availability.AvailabilityContext, org.rhq.core.pluginapi.inventory.InventoryContext, org.rhq.core.pluginapi.inventory.PluginContainerDeployment)
     *
     * @since 4.0
     */
    public ResourceUpgradeContext(Resource resource, ResourceContext<?> parentResourceContext,
        T parentResourceComponent, ResourceDiscoveryComponent<T> resourceDiscoveryComponent, SystemInfo systemInfo,
        File temporaryDirectory, File baseDataDirectory, String pluginContainerName, EventContext eventContext,
        OperationContext operationContext, ContentContext contentContext, AvailabilityContext availabilityContext,
        InventoryContext inventoryContext, PluginContainerDeployment pluginContainerDeployment) {

        super(resource, parentResourceComponent, parentResourceContext, resourceDiscoveryComponent, systemInfo,
            temporaryDirectory, baseDataDirectory, pluginContainerName, eventContext, operationContext, contentContext,
            availabilityContext, inventoryContext, pluginContainerDeployment);

        this.resourceConfiguration = resource.getResourceConfiguration();
        this.name = resource.getName();
        this.description = resource.getDescription();
        this.version = resource.getVersion();
    }

    /**
     * @return the context of the Resource component's parent Resource component
     *
     * @since 4.0
     */
    @Override
    public ResourceContext<?> getParentResourceContext() {
        return super.getParentResourceContext();
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

    public String getVersion() {
        return version;
    }

}
