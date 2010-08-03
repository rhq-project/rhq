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
import org.rhq.plugins.apache.parser.ApacheDirective;
import org.rhq.plugins.apache.parser.ApacheDirectiveTree;
import org.rhq.plugins.apache.util.AugeasNodeSearch;
import org.rhq.plugins.apache.util.AugeasNodeValueUtil;



/**
 * Discovery component for Apache discovery directives.
 * 
 * @author Lukas Krejci
 */
public class ApacheDirectoryDiscoveryComponent implements ResourceDiscoveryComponent<ApacheConfigurationBase> {

    /* (non-Javadoc)
     * @see org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent#discoverResources(org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext)
     */
    public static final String DIRECTORY_DIRECTIVE = "<Directory";
    public static final String [] PARENT_DIRECTIVES = {"<IfModule"};
    
    public Set<DiscoveredResourceDetails> discoverResources(
        ResourceDiscoveryContext<ApacheConfigurationBase> context)
        throws InvalidPluginConfigurationException, Exception {

        Set<DiscoveredResourceDetails> discoveredResources = new LinkedHashSet<DiscoveredResourceDetails>();
        ApacheDirectiveTree tree = context.getParentResourceComponent().loadParser();
        ApacheDirective parentNode = context.getParentResourceComponent().getNode(tree);
        List<ApacheDirective> directories = AugeasNodeSearch.searchNode(PARENT_DIRECTIVES, DIRECTORY_DIRECTIVE, parentNode);

        ResourceType resourceType = context.getResourceType();

        for (ApacheDirective node : directories) {
            Configuration pluginConfiguration = new Configuration();
                     
            String ifmoduleParams = AugeasNodeSearch.getNodeKey(node, parentNode);           
            List<String> params = node.getValues();
            
            String directoryParam;
            boolean isRegexp;
            
            if (params.size() > 1) {
                directoryParam = params.get(1);
                isRegexp = true;
            } else {
                directoryParam = params.get(0);
                isRegexp = false;
            }
            
            pluginConfiguration.put(new PropertySimple(ApacheVirtualHostServiceComponent.REGEXP_PROP, isRegexp));
            
            String resourceKey = ifmoduleParams;
            String resourceName = AugeasNodeValueUtil.unescape(directoryParam);

            discoveredResources.add(new DiscoveredResourceDetails(resourceType, resourceKey, resourceName, null, null,
                pluginConfiguration, null));
        }
        return discoveredResources;
    }
}
