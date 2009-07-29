/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.plugins.twitter;

import java.util.HashSet;
import java.util.Set;

import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;


/**
 * Discovery class - just set up a fixed twitter subsystem.
 *
 * @author Heiko W. Rupp
 */
public class TwitterDiscovery implements ResourceDiscoveryComponent {


    /**
     * Run the discovery
     */
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext discoveryContext) throws Exception {

        Set<DiscoveredResourceDetails> discoveredResources = new HashSet<DiscoveredResourceDetails>();

        /**
         * A discovered resource must have a unique key, that must
         * stay the same when the resource is discovered the next
         * time
         */
        DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
            discoveryContext.getResourceType(), // ResourceType
                "Twitter", // ResourceKey
                "Twitter feed",
                null,
                "One twitter user",
                null,
                null  );


        // Add to return values
        discoveredResources.add(detail);

        return discoveredResources;

    }
}