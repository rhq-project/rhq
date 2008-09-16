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
package org.rhq.plugins.hudson;

import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONObject;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * Maven archetypes cannot create empty directories, so this class simply functions to get the
 * requested package structure created.
 */
public class HudsonDiscoveryComponent implements ResourceDiscoveryComponent {

    public Set discoverResources(ResourceDiscoveryContext resourceDiscoveryContext)
        throws InvalidPluginConfigurationException, Exception {

        Set<DiscoveredResourceDetails> found = new HashSet<DiscoveredResourceDetails>();

        for (Configuration config : (List<Configuration>) resourceDiscoveryContext.getPluginConfigurations()) {

            String path = config.getSimple("urlBase").getStringValue();
            URL url = new URL(path);

            JSONObject server = HudsonJSONUtility.getData(path, 0);

            server.getString("description");

            DiscoveredResourceDetails hudson = new DiscoveredResourceDetails(
                resourceDiscoveryContext.getResourceType(), url.toString(), url.getHost() + url.getPath(),
                HudsonJSONUtility.getVersion(path), "hudson server", config, null);

            found.add(hudson);
        }

        return found;
    }
}
