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
package org.rhq.core.pc.util;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.inventory.ResourceContainer;
import org.rhq.core.pluginapi.inventory.ResourceComponent;

/**
 * Some static utilities for use with component objects.
 *
 * @author John Mazzitelli
 */
public class ComponentUtil {
    /**
     * Gets the Resource type of the Resource identified with the given ID. An exception is thrown if it cannot be
     * determined.
     *
     * @param  resourceId the Resource id
     *
     * @return the Resource's {@link ResourceType}
     *
     * @throws PluginContainerException if the Resource is not known
     */
    @SuppressWarnings("unchecked")
    public static ResourceType getResourceType(int resourceId) throws PluginContainerException {
        InventoryManager inventoryManager = PluginContainer.getInstance().getInventoryManager();

        // get the resource container that wraps the given resource
        ResourceContainer resourceContainer = inventoryManager.getResourceContainer(resourceId);
        if (resourceContainer == null) {
            throw new PluginContainerException("Resource component container could not be retrieved for resource: "
                + resourceId);
        }

        return resourceContainer.getResource().getResourceType();
    }

    /**
     * Gets the given facet interface for the resource identified with the given ID. If the resource does not have a
     * valid resource component associated with it or if it does not support the given facet, an exception is thrown. If
     * the resource component is not yet started and <code>onlyIfStarted</code> is <code>true</code>, an exception will
     * be thrown. Under most conditions when the plugin container managers need components, those components must be in
     * the started state. This method can ensure that the component will only be returned if it is started.
     *
     * <p>The <code>lockType</code> defines how the caller wants to synchronize access to the facet method calls. If it
     * is {@link FacetLockType#NONE}, the returned object is the component itself and will allow immediate and
     * concurrent access to the component without synchronization. If it is {@link FacetLockType#READ} or
     * {@link FacetLockType#WRITE}, the returned object is actually a proxy to the component that restricts access to
     * the component's facet methods by synchronizing on the component's read or write lock. You would request
     * synchronized access to a component if you want to make calls to the facet interface that require that it not run
     * concurrently with any other component call in any other facet. For example, if you want to update a configuration
     * via the configuration facet, you would ask for a write lock to prohibit any other facet call from concurrently
     * being made. This will ensure that the configuration update will not occur at the same time an operation facet
     * method call was made.</p>
     *
     * @param  resourceId     identifies the resource whose facet component interface is to be returned
     * @param  facetInterface the resource component's facet type that is to be returned
     * @param  lockType       how access to the facet should be synchronized
     * @param  timeout        if the method invocation thread has not completed after this many milliseconds, interrupt
     *                        it; value must be positive
     * @param  daemonThread   whether or not the thread used for the invocation should be a daemon thread
     * @param  onlyIfStarted  if <code>true</code>, and the component is not started, an exception is thrown
     *
     * @return the resource's <code>T</code> component interface
     *
     * @throws PluginContainerException if the resource does not have a component or it does not support the given facet
     *                                  interface or it is not started and <code>onlyIfStarted</code> is <code>
     *                                  true</code>
     *
     * @see    ResourceContainer#createResourceComponentProxy(Class, FacetLockType, long, boolean, boolean)
     */
    @SuppressWarnings("unchecked")
    public static <T> T getComponent(int resourceId, Class<T> facetInterface, FacetLockType lockType,
                                     long timeout, boolean daemonThread, boolean onlyIfStarted) throws PluginContainerException {
        InventoryManager inventoryManager = PluginContainer.getInstance().getInventoryManager();
        ResourceContainer resourceContainer = inventoryManager.getResourceContainer(resourceId);
        if (resourceContainer == null) {
            throw new PluginContainerException("Resource component container could not be retrieved for resource: "
                + resourceId);
        }
        return resourceContainer.createResourceComponentProxy(facetInterface, lockType, timeout, daemonThread, onlyIfStarted);
    }

    @SuppressWarnings("unchecked")
    public static ResourceComponent fetchResourceComponent(int resourceId) {
        return PluginContainer.getInstance().getInventoryManager().getResourceContainer(resourceId).getResourceComponent();
    }
}