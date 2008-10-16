/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.plugins.perftest.resource;

import java.util.HashSet;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.perftest.scenario.SimpleResourceGenerator;

/**
 * Simplistic resource factory, this class will simply return the number of resource specified. The resource key and
 * resource name will be generated incrementally based on a counter. Currently, subsequent calls to this factory will
 * result in the same set of resources returned.
 *
 * @author Jason Dobies
 */
public class SimpleResourceFactory implements ResourceFactory {
    // Attributes  --------------------------------------------

    /**
     * Generator that governs how this factory will function.
     */
    private SimpleResourceGenerator generator;

    private final Log log = LogFactory.getLog(this.getClass());

    // Constructors  --------------------------------------------

    public SimpleResourceFactory(SimpleResourceGenerator generator) {
        this.generator = generator;
    }

    // ResourceFactory Implementation  --------------------------------------------

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext context) {
        int numResources = getNumberOfResources();

        Set<DiscoveredResourceDetails> discoveredResources = new HashSet<DiscoveredResourceDetails>(numResources);
        for (int ii = 0; ii < numResources; ii++) {
            ResourceType resourceType = context.getResourceType();
            String resourceKey = context.getResourceType().getName() + "-" + ii;
            String resourceName = resourceKey;
            String resourceVersion = "1.0";
            String resourceDescription = resourceKey + " description";
            Configuration pluginConfiguration = new Configuration();

            DiscoveredResourceDetails details = new DiscoveredResourceDetails(resourceType, resourceKey, resourceName,
                resourceVersion, resourceDescription, pluginConfiguration, null);

            discoveredResources.add(details);
        }

        return discoveredResources;
    }

    // Private  --------------------------------------------

    /**
     * Determines how many resources to create based on the generator's configuration.
     *
     * @return number of resources to create
     */
    private int getNumberOfResources() {
        // If the property is set, use that value
        String propertyName = generator.getProperty();

        if (propertyName != null) {
            String propertyString = System.getProperty(propertyName);

            if (propertyString != null) {
                return Integer.parseInt(propertyString);
            } else {
                log.warn("Property was specified but no value was set. Property: " + propertyName);
            }
        }

        return generator.getNumberOfResources();
    }
}