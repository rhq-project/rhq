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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.managed.api.ComponentType;
import org.jboss.managed.api.ManagedComponent;
import org.jboss.managed.api.ManagedDeployment;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.jbossas5.util.ManagedComponentUtils;

/**
 * Discover EJB3 bean Resources.
 *
 * @author Ian Springer
 */
public class Ejb3BeanDiscoveryComponent extends ManagedComponentDiscoveryComponent<AbstractManagedDeploymentComponent> {
    private final Log log = LogFactory.getLog(this.getClass());

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(
            ResourceDiscoveryContext<AbstractManagedDeploymentComponent> discoveryContext)
            throws Exception {
        ResourceType resourceType = discoveryContext.getResourceType();
        log.trace("Discovering " + resourceType.getName() + " Resources...");

        ManagementView managementView = discoveryContext.getParentResourceComponent().getConnection().getManagementView();
        // TODO (ips): Only refresh the ManagementView *once* per runtime discovery scan, rather than every time this
        //             method is called. Do this by providing a runtime scan id in the ResourceDiscoveryContext.
        managementView.load();

        AbstractManagedDeploymentComponent parentResourceComponent = discoveryContext.getParentResourceComponent();
        ManagedDeployment parentDeployment = parentResourceComponent.getManagedDeployment();
        Collection<ManagedComponent> components = parentDeployment.getComponents().values();
        ComponentType componentType = getComponentType(discoveryContext);
        Set<DiscoveredResourceDetails> discoveredResources = new HashSet<DiscoveredResourceDetails>();
        for (ManagedComponent component : components) {
            if (component.getType().equals(componentType)) {
                String ejbName = (String)ManagedComponentUtils.getSimplePropertyValue(component, "name");
                @SuppressWarnings({"UnnecessaryLocalVariable"})
                String resourceKey = ejbName;
                @SuppressWarnings({"UnnecessaryLocalVariable"})
                String resourceName = ejbName;                
                String version = null;

                Configuration pluginConfig = discoveryContext.getDefaultPluginConfiguration();
                pluginConfig.put(new PropertySimple(ManagedComponentComponent.Config.COMPONENT_NAME,
                        component.getName()));

                @SuppressWarnings({"UnnecessaryLocalVariable"}) DiscoveredResourceDetails resource =
                        new DiscoveredResourceDetails(resourceType,
                                resourceKey,
                                resourceName,
                                version,
                                resourceType.getDescription(),
                                pluginConfig,
                                null);

                discoveredResources.add(resource);
            }
        }

        log.trace("Discovered " + discoveredResources.size() + " " + resourceType.getName() + " Resources.");
        return discoveredResources;
    }
}