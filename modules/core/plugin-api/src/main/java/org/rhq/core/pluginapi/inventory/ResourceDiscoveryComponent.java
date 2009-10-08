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
 * 
 * @see ClassLoaderFacet
 */
public interface ResourceDiscoveryComponent<T extends ResourceComponent> {
    /**
     * <p>Asks the discovery component to discover all of its resources. The plugin container may or may not have already
     * {@link ResourceDiscoveryContext#getAutoDiscoveredProcesses() auto-discovered} some resources for this component
     * already. In this case, this discovery component should take those auto-discovered resources from the context and
     * "prepare them" to be included as part of the returned set of resources, unless there is some reason it doesn't
     * want to include those resources. By "prepare them", it means to make sure to set their
     * {@link DiscoveredResourceDetails#setResourceKey(String) key},
     * {@link DiscoveredResourceDetails#setResourceName(String) name} and other details.</p>
     *
     * <b>ClassLoader Note:</b>
     * 
     * <p>There are two main usages of this method: first, when top-level resources need to be discovered (i.e. servers
     * running as direct children underneath the top-level platform) and second, when discovering child services and
     * child servers running inside those top-level server resources that were previously discovered.</p>
     * 
     * <p>For the first case, this discoverResources method implementation can't know where any specific client jars are
     * that may be needed to connect to the managed resource, because the managed resource hasn't been discovered yet!
     * Therefore, when discovering top-level servers, this method cannot import or use connection classes that are available
     * from a managed resource's client jars, because it isn't yet known where they are! (see {@link ClassLoaderFacet} for
     * information on client jar detection). In this case, this method is called within the context of the main
     * plugin classloader.</p>
     * 
     * <p>In the second case, this method is called within the context of its parent resource thus giving this method
     * access to connection classes that are found within that parent resource's client jars. The plugin container
     * will ensure that this method is invoked with a context classloader that has the appropriate client jars available.</p>
     * 
     * <p>What the above means is that this class must not import or directly access client jar classes, because within some
     * contexts, it will not have access to those jars. The easy way to think of this is the following: this discovery component
     * must not directly import, load or access any class that is found in any jar that is returned by this object's
     * implementation of 
     * {@link ClassLoaderFacet#getAdditionalClasspathUrls(ResourceDiscoveryContext, DiscoveredResourceDetails)}.</p>
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