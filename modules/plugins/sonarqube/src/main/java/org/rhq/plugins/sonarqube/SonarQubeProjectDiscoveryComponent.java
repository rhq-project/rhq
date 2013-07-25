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
package org.rhq.plugins.sonarqube;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Jeremie Lagarde
 */
public class SonarQubeProjectDiscoveryComponent implements ResourceDiscoveryComponent<SonarQubeServerComponent> {

    private static final Log LOG = LogFactory.getLog(SonarQubeJSONUtility.class);

    public Set<DiscoveredResourceDetails> discoverResources(
        ResourceDiscoveryContext<SonarQubeServerComponent> discoveryContext)
        throws InvalidPluginConfigurationException, Exception {

        Set<DiscoveredResourceDetails> found = new HashSet<DiscoveredResourceDetails>();

        String serverPath = discoveryContext.getParentResourceComponent().getPath();

        JSONArray projects = SonarQubeJSONUtility.getDatas(serverPath, "resources");

        try {

            for (int i = 0; i < projects.length(); i++) {
                JSONObject project = projects.getJSONObject(i);

                String name = project.getString("name");
                String key = project.getString("key");
                String description = project.getString("lname");
                String version = project.getString("version");
                if (description.length() > 1000) {
                    description = description.substring(0, 999);
                }

                DiscoveredResourceDetails detail = new DiscoveredResourceDetails(discoveryContext.getResourceType(),
                    key, name, version, description, null, null);
                found.add(detail);
            }
            return found;

        } catch (Exception e) {
            LOG.warn(e);
            throw e;
        }
    }
}
