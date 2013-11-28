/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.modules.plugins.jbossas7;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.ReadChildrenNames;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * @author Thomas Segismont
 */
public class QueryCacheDiscovery implements ResourceDiscoveryComponent<BaseComponent<?>> {
    private static final String QUERY_CACHE_TYPE_NAME = "query-cache";
    private static final String RESOURCE_NAME_PREFIX = "Query Cache ";

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<BaseComponent<?>> context)
        throws InvalidPluginConfigurationException {

        BaseComponent parentComponent = context.getParentResourceComponent();
        String parentComponentPath = parentComponent.getPath();
        Address parentAddress = new Address(parentComponentPath);

        Result readChildrenNamesResult = parentComponent.getASConnection().execute(
            new ReadChildrenNames(parentAddress, QUERY_CACHE_TYPE_NAME));

        if (readChildrenNamesResult.isSuccess()) {
            Set<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>();
            List<String> childrenNames = (List<String>) readChildrenNamesResult.getResult();
            for (String childName : childrenNames) {
                Configuration pluginConfiguration = context.getDefaultPluginConfiguration();
                pluginConfiguration.setSimpleValue("path", parentComponentPath + "," + QUERY_CACHE_TYPE_NAME + "="
                    + childName);
                String resourceKey = MessageDigestGenerator.getDigestString(childName);
                details.add( //
                    new DiscoveredResourceDetails( //
                        context.getResourceType(), // DataType
                        resourceKey, // Key
                        RESOURCE_NAME_PREFIX + resourceKey, // Name
                        null, // Version
                        context.getResourceType().getDescription(), // subsystem.description
                        pluginConfiguration, null));
            }
            return details;
        }
        return Collections.emptySet();
    }
}
