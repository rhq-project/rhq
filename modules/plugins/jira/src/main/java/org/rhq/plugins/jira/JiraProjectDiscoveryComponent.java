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
package org.rhq.plugins.jira;

import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.plugins.jira.soapclient.jira.RemoteProject;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;

/**
 * @author Greg Hinkle
 */
public class JiraProjectDiscoveryComponent implements ResourceDiscoveryComponent<JiraServerComponent> {
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<JiraServerComponent> context) throws InvalidPluginConfigurationException, Exception {
        Set<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>();

        JiraClient client = context.getParentResourceComponent().getClient();

        Map<String, RemoteProject> projectMap = client.getProjectMap();
        for (String key : projectMap.keySet()) {
            RemoteProject project = projectMap.get(key);

            Configuration c = new Configuration();
            c.put( new PropertySimple("projectKey", key));
            c.put( new PropertySimple("projectId", project.getId()));
            DiscoveredResourceDetails detail =
                    new DiscoveredResourceDetails(
                            context.getResourceType(),
                            project.getKey(),
                            project.getName(),
                            null,
                            project.getDescription(),
                            c,
                            null);

            details.add(detail);

        }
        return details;
    }
}
