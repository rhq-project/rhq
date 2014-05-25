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

import net.augeas.AugeasException;

import org.rhq.augeas.AugeasComponent;
import org.rhq.augeas.node.AugeasNode;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.util.StringUtil;
import org.rhq.plugins.apache.parser.ApacheDirective;
import org.rhq.plugins.apache.parser.ApacheDirectiveTree;
import org.rhq.plugins.apache.util.AugeasNodeSearch;
import org.rhq.plugins.apache.util.AugeasNodeValueUtil;

/**
 * Discovery component for Apache discovery directives.
 *
 * @author Lukas Krejci
 */
public class ApacheDirectoryDiscoveryComponent implements ResourceDiscoveryComponent<ApacheVirtualHostServiceComponent> {

    /* (non-Javadoc)
     * @see org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent#discoverResources(org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext)
     */
    public static final String DIRECTORY_DIRECTIVE = "<Directory";
    public static final String[] PARENT_DIRECTIVES = { "<IfModule" };

    public Set<DiscoveredResourceDetails> discoverResources(
        ResourceDiscoveryContext<ApacheVirtualHostServiceComponent> context)
        throws InvalidPluginConfigurationException, Exception {

        Set<DiscoveredResourceDetails> discoveredResources = new LinkedHashSet<DiscoveredResourceDetails>();

        if (!context.getParentResourceComponent().isAugeasEnabled()) {
            ApacheDirective vhost = context.getParentResourceComponent().getDirective();
            ApacheDirectiveTree tree = new ApacheDirectiveTree();
            tree.setRootNode(vhost);
            final List<ApacheDirective> allDirectories  = tree.search("/"+DIRECTORY_DIRECTIVE);
            ResourceType resourceType = context.getResourceType();
            for (ApacheDirective apacheDirective : allDirectories) {
                String directoryParam;
                boolean isRegexp;
                List<String> params = apacheDirective.getValues();
                if (params.size() > 1 && StringUtil.isNotBlank(params.get(1))) {
                    directoryParam = params.get(1);
                    isRegexp = true;
                } else {
                    directoryParam = params.get(0);
                    isRegexp = false;
                }
                
                Configuration pluginConfiguration = context.getDefaultPluginConfiguration();
                pluginConfiguration.put(new PropertySimple(ApacheDirectoryComponent.REGEXP_PROP, isRegexp));
                String resourceName = AugeasNodeValueUtil.unescape(directoryParam);

                int index = 1; 
                for (DiscoveredResourceDetails detail : discoveredResources) {
                    if(detail.getResourceName().endsWith(resourceName))
                        index++;
                }
                StringBuilder resourceKey = new StringBuilder();
                resourceKey.append(apacheDirective.getName()).append("|").append(directoryParam).append("|").append(index).append(";");
                discoveredResources.add(new DiscoveredResourceDetails(resourceType, resourceKey.toString(), resourceName, null,
                    null, pluginConfiguration, null));

            }
            return discoveredResources;
        }


        AugeasComponent comp = context.getParentResourceComponent().getAugeas();
        AugeasTree tree = null;

        try {
            tree = comp.getAugeasTree(ApacheServerComponent.AUGEAS_HTTP_MODULE_NAME);
        } catch (AugeasException e) {
            //we depend on Augeas to do anything useful with directories.
            //give up, if Augeas isn't there.
            comp.close();
            return discoveredResources;
        }

        try {
            AugeasNode parentNode = context.getParentResourceComponent().getNode(tree);
            List<AugeasNode> directories =
                AugeasNodeSearch.searchNode(PARENT_DIRECTIVES, DIRECTORY_DIRECTIVE, parentNode);

            ResourceType resourceType = context.getResourceType();

            for (AugeasNode node : directories) {
                Configuration pluginConfiguration = context.getDefaultPluginConfiguration();

                String ifmoduleParams = AugeasNodeSearch.getNodeKey(node, parentNode);
                List<AugeasNode> params = node.getChildByLabel("param");

                String directoryParam;
                boolean isRegexp;

                if (params.size() > 1) {
                    directoryParam = params.get(1).getValue();
                    isRegexp = true;
                } else {
                    directoryParam = params.get(0).getValue();
                    isRegexp = false;
                }

                pluginConfiguration.put(new PropertySimple(ApacheDirectoryComponent.REGEXP_PROP, isRegexp));

                String resourceKey = ifmoduleParams;
                String resourceName = AugeasNodeValueUtil.unescape(directoryParam);

                discoveredResources.add(new DiscoveredResourceDetails(resourceType, resourceKey, resourceName, null,
                    null, pluginConfiguration, null));
            }
            return discoveredResources;

        } finally {
            if (comp != null)
                comp.close();
        }
    }
}
