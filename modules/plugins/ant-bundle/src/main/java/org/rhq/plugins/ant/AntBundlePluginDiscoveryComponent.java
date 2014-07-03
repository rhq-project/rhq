/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.plugins.ant;

import java.util.HashSet;
import java.util.Set;

import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * @author John Mazzitelli
 */
@SuppressWarnings("unchecked")
public class AntBundlePluginDiscoveryComponent implements ResourceDiscoveryComponent {

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext context) {

        HashSet<DiscoveredResourceDetails> set = new HashSet<DiscoveredResourceDetails>();

        String key = "ant-bundle";
        String name = "Bundle Handler - Ant";
        String version = this.getClass().getPackage().getImplementationVersion();
        String description = "For provisioning bundles with Ant script recipes";

        DiscoveredResourceDetails resource = new DiscoveredResourceDetails(context.getResourceType(), key, name,
            version, description, null, null);

        set.add(resource);

        return set;
    }
}
