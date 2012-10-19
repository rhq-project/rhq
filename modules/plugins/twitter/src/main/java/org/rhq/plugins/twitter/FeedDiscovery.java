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

import java.util.Set;
import java.util.Collections;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.pluginapi.inventory.ManualAddFacet;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;

/**
 * Discovery class - allows to manually add a new twitter feed/search
 *
 * @author Heiko W. Rupp
 */
public class FeedDiscovery implements ResourceDiscoveryComponent<TwitterComponent>, ManualAddFacet {
    private final Log log = LogFactory.getLog(this.getClass());

    /**
     * Run the discovery
     */
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<TwitterComponent> discoveryContext) throws Exception {
        // We don't support auto-discovery.
        return Collections.emptySet();
    }

    public DiscoveredResourceDetails discoverResource(Configuration pluginConfig,
                                                      ResourceDiscoveryContext discoveryContext)
            throws InvalidPluginConfigurationException {
        String kind = pluginConfig.getSimpleValue("kind","user");
        String keyword = pluginConfig.getSimpleValue("keyword",null);

        // Create a new resource detail from the passed values
        DiscoveredResourceDetails detail =  new DiscoveredResourceDetails(
                discoveryContext.getResourceType(), // ResourceType
                "tw:" + kind + ":" + keyword, // resource key
                "Feed: " + kind + " : " + keyword, // resource name
                null, // version
                kind.equals("user") ? "Timeline " : "Search " + "for " + keyword, // description
                pluginConfig, // configuration
                null // ProcessInfo
        );
        log.info("Manually added " + detail);
        return detail;
    }
}