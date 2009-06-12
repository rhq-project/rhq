/*
* Jopr Management Platform
* Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.plugins.jbossas5;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.jbossas5.util.ConversionUtils;

import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.managed.api.ComponentType;
import org.jboss.managed.api.ManagedComponent;

/**
 * Discovery component for ManagedComponents exposed by the JBoss AS 5.x Profile Service that will be represented as
 * child Resources of the JBoss AS Resource.
 *
 * @author Jason Dobies
 * @author Mark Spritzer
 * @author Ian Springer
 */
public class ManagedComponentDiscoveryComponent<P extends ProfileServiceComponent>
        implements ResourceDiscoveryComponent<P>
{
    private final Log log = LogFactory.getLog(this.getClass());

    public Set<DiscoveredResourceDetails> discoverResources(
            ResourceDiscoveryContext<P> discoveryContext) throws Exception
    {
        ResourceType resourceType = discoveryContext.getResourceType();
        log.trace("Discovering " + resourceType.getName() + " Resources...");

        ManagementView managementView = discoveryContext.getParentResourceComponent().getConnection().getManagementView();
        // TODO (ips): Only refresh the ManagementView *once* per runtime discovery scan, rather than every time this
        //             method is called. Do this by providing a runtime scan id in the ResourceDiscoveryContext.
        managementView.load();

        ComponentType componentType = getComponentType(discoveryContext);
        Set<ManagedComponent> components;
        try
        {
            components = managementView.getComponentsForType(componentType);
        }
        catch (Exception e)
        {
            throw new IllegalStateException("Failed to get component types for " + resourceType + ".", e);
        }

        Set<DiscoveredResourceDetails> discoveredResources = new HashSet<DiscoveredResourceDetails>(components.size());
        /* Create a resource for each managed component found. We know all managed components will be of a
           type we're interested in, so we can just add them all. There may be need for multiple iterations
           over lists retrieved from different component types, but that is possible through the current API.
        */
        for (ManagedComponent component : components)
        {
            String resourceName = getResourceName(component);
            String resourceKey = component.getName();
            String version = null; // (ips) I don't think there's anything generic we can do here.

            DiscoveredResourceDetails resource =
                    new DiscoveredResourceDetails(resourceType,
                            resourceKey,
                            resourceName,
                            version,
                            resourceType.getDescription(),
                            discoveryContext.getDefaultPluginConfiguration(),
                            null);

            resource.getPluginConfiguration().put(new PropertySimple(ManagedComponentComponent.Config.COMPONENT_NAME,
                    component.getName()));

            discoveredResources.add(resource);
        }

        log.trace("Discovered " + discoveredResources.size() + " " + resourceType.getName() + " Resources.");
        return discoveredResources;
    }

    protected ComponentType getComponentType(ResourceDiscoveryContext<P> discoveryContext) {
        ManagementView managementView = discoveryContext.getParentResourceComponent().getConnection().getManagementView();
        ResourceType resourceType = discoveryContext.getResourceType();
        return ConversionUtils.getComponentType(resourceType);
    }

    protected String getResourceName(ManagedComponent component) {
        return component.getName();
    }
}
