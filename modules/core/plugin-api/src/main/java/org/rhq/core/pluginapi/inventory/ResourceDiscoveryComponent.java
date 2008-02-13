/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.core.pluginapi.inventory;

import java.util.Set;

/**
 * The plugin component that defines how resources are discovered. If an implementation of this interface declares its
 * resource component <code>T</code>, that means that discovery component implementation can discover child resources
 * for that type <code>T</code> parent resource. This means you can nest a hierarchy of discovery components that mimic
 * the resource type hierarchy as defined in a plugin deployment descriptor.
 *
 * <p>For example, a discovery component that can discover JBossAS data source services would declare its type <code>
 * T</code> as being the JBossAS server resource component (since JBossAS server resources can have children of type
 * "data source service".</p>
 *
 * @param <T> the parent resource component type for those resources discovered by this discovery component
 */
public interface ResourceDiscoveryComponent<T extends ResourceComponent> {
    /**
     * Asks the discovery component to discover all of its resources. The plugin container may or may not have already
     * {@link ResourceDiscoveryContext#getAutoDiscoveredProcesses() auto-discovered} some resources for this component
     * already. In this case, this discovery component should take those auto-discovered resources from the context and
     * "prepare them" to be included as part of the returned set of resources, unless there is some reason it doesn't
     * want to include those resources. By "prepare them", it means to make sure to set their
     * {@link DiscoveredResourceDetails#setResourceKey(String) key},
     * {@link DiscoveredResourceDetails#setResourceName(String) name} and other details.
     *
     * @param  context the discovery context that provides the information to the component that helps it perform its
     *                 discovery
     *
     * @return a set of discovered resource details that were discovered and can be imported/merged into inventory
     *
     * @throws InvalidPluginConfigurationException if a plugin configuration found in the context was somehow invalid
     *                                             and thus caused a failure to connect to a resource
     * @throws Exception                           if a generic error occurred that caused the discovery to abort
     */
    Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<T> context)
        throws InvalidPluginConfigurationException, Exception;
}