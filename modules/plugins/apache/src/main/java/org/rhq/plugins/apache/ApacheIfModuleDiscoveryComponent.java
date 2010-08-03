/*
 * RHQ Management Platform
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
package org.rhq.plugins.apache;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.apache.parser.ApacheDirective;
import org.rhq.plugins.apache.parser.ApacheDirectiveTree;
import org.rhq.plugins.apache.util.AugeasNodeSearch;

public class ApacheIfModuleDiscoveryComponent  implements ResourceDiscoveryComponent<ApacheConfigurationBase> {

    private static final String [] parentRes = {"<IfModule"};
    private static final String IFMODULE_NODE_NAME = "<IfModule";
    
    public Set<DiscoveredResourceDetails> discoverResources(
        ResourceDiscoveryContext<ApacheConfigurationBase> context)
        throws InvalidPluginConfigurationException, Exception {
   
    Set<DiscoveredResourceDetails> discoveredResources = new LinkedHashSet<DiscoveredResourceDetails>();    
    ApacheConfigurationBase parentComponent = context.getParentResourceComponent();
      
    ApacheDirectiveTree tree = parentComponent.loadParser();
    ApacheDirective parentNode = parentComponent.getNode(tree);
    
    List<ApacheDirective> ifModuleNodes = AugeasNodeSearch.searchNode(parentRes, IFMODULE_NODE_NAME, parentNode);
    
    ResourceType resourceType = context.getResourceType();

    for (ApacheDirective node : ifModuleNodes) {
        
        String resourceKey = AugeasNodeSearch.getNodeKey(node,parentNode);
        String [] paramArray = resourceKey.split("\\|");
        String resourceName = paramArray[1];

        discoveredResources.add(new DiscoveredResourceDetails(resourceType, resourceKey, resourceName, null, null,
        null, null));
        }
    return discoveredResources;
   }
}