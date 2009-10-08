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

import java.util.Collections;
import java.util.Set;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ManualAddFacet;

/**
 * Discovery class - just set up a fixed twitter subsystem.
 *
 * @author Heiko W. Rupp
 */
public class TwitterDiscovery implements ResourceDiscoveryComponent, ManualAddFacet {
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext discoveryContext) throws Exception {
        // We don't support auto-discovery.
        return Collections.emptySet();
    }

    public DiscoveredResourceDetails discoverResource(Configuration pluginConfig,
                                                      ResourceDiscoveryContext discoveryContext) 
            throws InvalidPluginConfigurationException {
          /**
           * A discovered resource must have a unique key, that must
           * stay the same when the resource is discovered the next
           * time
           */
         String user = pluginConfig.getSimpleValue("user",null);
         String password = pluginConfig.getSimpleValue("password",null);
         String url = pluginConfig.getSimpleValue("baseurl","http://twitter.com/");
         if (user==null || password==null)
            throw new InvalidPluginConfigurationException("User or password were not set");

         DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
                discoveryContext.getResourceType(), // ResourceType
                url + "_"+ user, // ResourceKey
                url + " feed for " +user,
                null,
                "One " + url + " user",
                pluginConfig,
                null  );
        return detail;
    }
}