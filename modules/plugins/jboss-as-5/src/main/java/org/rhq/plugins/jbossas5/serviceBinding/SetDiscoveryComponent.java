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
package org.rhq.plugins.jbossas5.serviceBinding;

import java.util.HashSet;
import java.util.Set;

import org.jboss.managed.api.ManagedComponent;
import org.jboss.metatype.api.values.CollectionValue;
import org.jboss.metatype.api.values.CompositeValue;
import org.jboss.metatype.api.values.MetaValue;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * Discovery component for binding sets.
 * 
 * @author Filip Drabek
 * @author Lukas Krejci
 */
public class SetDiscoveryComponent implements ResourceDiscoveryComponent<ManagerComponent> {

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<ManagerComponent> context)
        throws InvalidPluginConfigurationException, Exception {

        ResourceType resourceType = context.getResourceType();

        ManagedComponent bindingManagerComponent = context.getParentResourceComponent().getBindingManager();

        CollectionValue bindingSets = (CollectionValue) bindingManagerComponent.getProperty(Util.BINDING_SETS_PROPERTY)
            .getValue();

        Set<DiscoveredResourceDetails> discoveredResources = new HashSet<DiscoveredResourceDetails>(bindingSets
            .getSize());

        for (MetaValue m : bindingSets.getElements()) {
            CompositeValue bindingSet = (CompositeValue) m;

            String bindingSetName = Util.getValue(bindingSet, "name", String.class);
            String resourceKey = context.getParentResourceComponent().getBindingSetResourceKey(bindingSetName);

            DiscoveredResourceDetails resource = new DiscoveredResourceDetails(resourceType, resourceKey,
                bindingSetName, null, resourceType.getDescription(), context.getDefaultPluginConfiguration(), null);

            discoveredResources.add(resource);
        }
        return discoveredResources;
    }
}
