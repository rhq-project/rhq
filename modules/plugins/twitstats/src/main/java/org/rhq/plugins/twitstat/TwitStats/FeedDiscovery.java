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
package org.rhq.plugins.twitstat.TwitStats;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * Discovery class - allows to manually add a new twitter feed/search
 * 
 * @author Heiko W. Rupp
 */
public class FeedDiscovery implements ResourceDiscoveryComponent<TwitComponent> {


    private final Log log = LogFactory.getLog(this.getClass());


    /**
     * Run the discovery
     */
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<TwitComponent> discoveryContext) throws Exception {

        Set<DiscoveredResourceDetails> discoveredResources = new HashSet<DiscoveredResourceDetails>();

        // Pull out the configuration from the passed context
        List<Configuration> childConfigs = discoveryContext.getPluginConfigurations();
        for (Configuration config : childConfigs) {
            String kind = config.getSimpleValue("kind","user");
            String keyword = config.getSimpleValue("keyword",null);

            // Create a new resource detail from the passed values
            DiscoveredResourceDetails detail =  new DiscoveredResourceDetails(
                    discoveryContext.getResourceType(), // ResourceType
                    "tw:" + kind + ":" + keyword, // resource key
                    "Feed: " + kind + " : " + keyword, // resource name
                    null, // version
                    kind.equals("user") ? "Timeline " : "Search " + "for " + keyword, // description
                    config, // configuration
                    null // ProcessInfo
            );


            // Add to return values
            discoveredResources.add(detail);
            log.info("Added new " + detail );
        }

        return discoveredResources;

    }
}