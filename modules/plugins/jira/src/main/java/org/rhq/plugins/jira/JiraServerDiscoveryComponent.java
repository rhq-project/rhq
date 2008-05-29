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
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.plugins.jira.soapclient.jira.RemoteServerInfo;

import java.util.Set;
import java.util.Collections;

/**
 * @author Greg Hinkle
 */
public class JiraServerDiscoveryComponent implements ResourceDiscoveryComponent {
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext resourceDiscoveryContext) throws InvalidPluginConfigurationException, Exception {
        for (Configuration config : (Iterable<? extends Configuration>) resourceDiscoveryContext.getPluginConfigurations()) {
            JiraClient jc = new JiraClient(config);
            RemoteServerInfo info = jc.getServerInfo();
            String url = config.getSimple("url").getStringValue();
            DiscoveredResourceDetails detail =
                    new DiscoveredResourceDetails(
                            resourceDiscoveryContext.getResourceType(),
                            url,
                            "Jira " + url,
                            info.getVersion(),
                            info.getEdition(),
                            config,
                            null);
            return  Collections.singleton(detail);
        }
        return null;
    }
}
