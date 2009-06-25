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

import org.rhq.plugins.jbossas5.ManagedComponentDiscoveryComponent;

/**
 * Manager component for the Service Binding Manager.
 * 
 * @author Filip Drabek
 * @author Lukas Krejci
 */
@Deprecated
public class ManagerDiscoveryComponent extends ManagedComponentDiscoveryComponent<ManagerComponent> {

    //    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<ProfileServiceComponent> context)
    //        throws InvalidPluginConfigurationException, Exception {
    //
    //        ResourceType resourceType = context.getResourceType();
    //        Configuration config = context.getDefaultPluginConfiguration();
    //
    //        String managerType = config.getSimple(ManagerDiscoveryComponent.MANAGER_COMPONENT_TYPE).getStringValue();
    //
    //        String managerSubType = config.getSimple(ManagerDiscoveryComponent.MANAGER_COMPONENT_SUBTYPE).getStringValue();
    //        ManagementView managementView = context.getParentResourceComponent().getConnection().getManagementView();
    //
    //        Set<ManagedComponent> components;
    //        ComponentType componentType = new ComponentType(managerType, managerSubType);
    //
    //        try {
    //            components = managementView.getComponentsForType(componentType);
    //        } catch (Exception e) {
    //            throw new IllegalStateException("Failed to get component types for Manager Component.", e);
    //        }
    //
    //        Set<DiscoveredResourceDetails> discoveredResources = new HashSet<DiscoveredResourceDetails>(components.size());
    //
    //        for (ManagedComponent component : components) {
    //
    //            Set<ManagedOperation> ope = component.getOperations();
    //            for (ManagedOperation op : ope) {
    //                log.error(op.getName());
    //                ManagedParameter[] mPar = op.getParameters();
    //                for (int i = 0; i < mPar.length; i++) {
    //                    log.error("            " + mPar[i].getMetaType().getClassName());
    //                }
    //            }
    //            String resourceName = component.getName();
    //            String resourceKey = managerType + ":" + managerSubType + ":" + resourceName;
    //
    //            String version = " "; // TODO
    //            DiscoveredResourceDetails resource = new DiscoveredResourceDetails(resourceType, resourceKey, resourceName,
    //                version, resourceType.getDescription(), context.getDefaultPluginConfiguration(), null);
    //
    //            resource.getPluginConfiguration().put(
    //                new PropertySimple(ManagedComponentComponent.Config.COMPONENT_NAME, component.getName()));
    //
    //            discoveredResources.add(resource);
    //        }
    //        return discoveredResources;
    //    }

}
