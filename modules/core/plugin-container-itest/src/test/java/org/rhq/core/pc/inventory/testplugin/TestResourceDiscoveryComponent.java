/*
 * RHQ Management Platform
 * Copyright (C) 2012 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.core.pc.inventory.testplugin;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * @author Ian Springer
 */
public class TestResourceDiscoveryComponent implements ResourceDiscoveryComponent<ResourceComponent<?>> {

    private static Map<ResourceType, Integer> executionCountsByResourceType = new HashMap();

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<ResourceComponent<?>> discoveryContext)
        throws InvalidPluginConfigurationException, Exception {

        ResourceType resourceType = discoveryContext.getResourceType();

        Integer count = executionCountsByResourceType.get(resourceType);
        if (count == null) {
            executionCountsByResourceType.put(resourceType, 1);
        } else {
            executionCountsByResourceType.put(resourceType, count + 1);
        }

        DiscoveredResourceDetails resourceDetails = new DiscoveredResourceDetails(resourceType,
            "SINGLETON", resourceType.getName(), "1.0", resourceType.getDescription(),
            discoveryContext.getDefaultPluginConfiguration(), null);

        return Collections.singleton(resourceDetails);
    }

    public static Map<ResourceType, Integer> getExecutionCountsByResourceType() {
        return executionCountsByResourceType;
    }

}
