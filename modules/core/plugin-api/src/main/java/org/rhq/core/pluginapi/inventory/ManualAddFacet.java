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
package org.rhq.core.pluginapi.inventory;

import org.rhq.core.domain.configuration.Configuration;

/**
 * This allows a discovery component to support Resources being manually added. A manual add is initiated when a GUI user
 * goes to the Child Resources section of the Resource's Inventory>Overview tab and chooses to manually add a new child
 * Resource. The user then specifies the connection properties (i.e. plugin configuration) that should be used to
 * to connect to the Resource. A request is then sent to the Plugin Container, which calls the {@link #discoverResource}
 * method on the discovery component. The option to manually add a Resource is only exposed by the GUI for Resource
 * types defined with supportsManualAdd="true" in the plugin descriptor. Manual add is used either to add an offline
 * managed resource to inventory or to add a managed resource that could not be auto-discovered for some reason.
 *
 * <i>Note:</i> this facet is to be implemented by {@link ResourceDiscoveryComponent} instances, unlike most other
 * facets that are implemented by the resource instance components.
 *
 * @param <T> the parent resource component type for those resources discovered by this discovery component
 *
 * @author Ian Springer
 */
public interface ManualAddFacet<T extends ResourceComponent> {
    /**
     * Using the specified plugin configuration, creates a DiscoveredResourceDetails object describing a new Resource
     * to be added to inventory.
     *
     * @param pluginConfiguration the plugin configuration that describes how to discover the Resource being manually
     *                            added
     * @param context information for the component that it may need for the manual add
     * @return a DiscoveredResourceDetails object describing a new Resource to be added to inventory
     *
     * @throws InvalidPluginConfigurationException if the specified plugin configuration was somehow invalid
     *                                             and thus caused a failure to connect to a resource
     */
    DiscoveredResourceDetails discoverResource(Configuration pluginConfiguration, ResourceDiscoveryContext<T> context)
        throws InvalidPluginConfigurationException;
}