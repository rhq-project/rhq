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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Set;

import org.json.JSONException;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ManualAddFacet;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * @author Jeremie Lagarde
 */
public class SonarQubeDiscoveryComponent implements ResourceDiscoveryComponent, ManualAddFacet {

    public Set discoverResources(ResourceDiscoveryContext resourceDiscoveryContext)
        throws InvalidPluginConfigurationException, Exception {
        return Collections.emptySet();
    }

    public DiscoveredResourceDetails discoverResource(Configuration pluginConfig,
        ResourceDiscoveryContext resourceDiscoveryContext) throws InvalidPluginConfigurationException {
        String path = pluginConfig.getSimple("urlBase").getStringValue();
        URL url;
        try {
            url = new URL(path);
        } catch (MalformedURLException e) {
            throw new InvalidPluginConfigurationException("Value of 'urlBase' property is not a valid URL.");
        }

        try {

            DiscoveredResourceDetails sonarqube = new DiscoveredResourceDetails(
                resourceDiscoveryContext.getResourceType(), url.toString(), url.getHost() + url.getPath(),
                SonarQubeJSONUtility.getVersion(path), "SonarQube server", pluginConfig, null);
            return sonarqube;
        }

        catch (JSONException e) {
            throw new RuntimeException("Failed to obtain version for SonarQube server.", e);
        }
    }
}
