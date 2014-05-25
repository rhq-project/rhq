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
import org.rhq.plugins.apache.util.AugeasNodeValueUtil;

/**
 * Discovery component for Apache discovery directives.
 *
 * @author Lukas Krejci
 * @author Jeremie Lagarde
 */
public class ApacheDirectoryDiscoveryComponent implements ResourceDiscoveryComponent<ApacheVirtualHostServiceComponent> {

    /* (non-Javadoc)
     * @see org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent#discoverResources(org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext)
     */
    public Set<DiscoveredResourceDetails> discoverResources(
        ResourceDiscoveryContext<ApacheVirtualHostServiceComponent> context)
        throws InvalidPluginConfigurationException, Exception {

        Set<DiscoveredResourceDetails> discoveredResources = new LinkedHashSet<DiscoveredResourceDetails>();
        ApacheDirective vhost = context.getParentResourceComponent().getDirective();
        ApacheDirectiveTree tree = new ApacheDirectiveTree();
        tree.setRootNode(vhost);
        final List<ApacheDirective> allDirectories  = tree.search("/"+ApacheDirectoryComponent.DIRECTORY_DIRECTIVE);
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
}
