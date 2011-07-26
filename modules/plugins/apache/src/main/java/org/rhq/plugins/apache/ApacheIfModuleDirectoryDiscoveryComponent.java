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

import org.rhq.augeas.AugeasComponent;
import org.rhq.augeas.node.AugeasNode;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.apache.util.AugeasNodeSearch;

public class ApacheIfModuleDirectoryDiscoveryComponent implements ResourceDiscoveryComponent<ApacheDirectoryComponent> {

    private static final String[] parentRes = { "<IfModule" };
    private static final String IFMODULE_NODE_NAME = "<IfModule";
    private AugeasTree tree;
    private AugeasNode parentNode;

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<ApacheDirectoryComponent> context)
        throws InvalidPluginConfigurationException, Exception {

        ApacheDirectoryComponent directory = context.getParentResourceComponent();
        Set<DiscoveredResourceDetails> discoveredResources = new LinkedHashSet<DiscoveredResourceDetails>();

        if (!directory.isAugeasEnabled())
            return discoveredResources;

        AugeasComponent comp = null;
        AugeasTree tree = null;
        try {
            comp = directory.getAugeas();
            tree = comp.getAugeasTree(ApacheServerComponent.AUGEAS_HTTP_MODULE_NAME);

            parentNode = directory.getNode(tree);

            List<AugeasNode> ifModuleNodes = AugeasNodeSearch.searchNode(parentRes, IFMODULE_NODE_NAME, parentNode);

            ResourceType resourceType = context.getResourceType();

            for (AugeasNode node : ifModuleNodes) {

                String resourceKey = AugeasNodeSearch.getNodeKey(node, parentNode);
                String[] paramArray = resourceKey.split("\\|");
                String resourceName = paramArray[1];

                discoveredResources.add(new DiscoveredResourceDetails(resourceType, resourceKey, resourceName, null,
                    null, null, null));
            }
            return discoveredResources;
        } finally {
            if (comp != null)
                comp.close();
        }
    }
}