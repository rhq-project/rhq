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

package org.rhq.plugins.postfix;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.augeas.AugeasConfigurationDiscoveryComponent;

/**
 * @author paji
 *
 */
public class PostfixAccessDiscoveryComponent extends AugeasConfigurationDiscoveryComponent<PostfixAccessComponent> {
    public static final String RESOURCE_KEY = "Access";

    public Set<DiscoveredResourceDetails> discoverResources(
        ResourceDiscoveryContext<PostfixAccessComponent> discoveryContext) throws InvalidPluginConfigurationException,
        Exception {

        Set<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>();
        if (new File("/etc/postfix/access").exists()) {
            Configuration pluginConfig = discoveryContext.getDefaultPluginConfiguration();
            DiscoveredResourceDetails detail = new DiscoveredResourceDetails(discoveryContext.getResourceType(),
                RESOURCE_KEY, "Access File", null, RESOURCE_KEY, pluginConfig, null);
            details.add(detail);
        }
        return details;
    }
}
