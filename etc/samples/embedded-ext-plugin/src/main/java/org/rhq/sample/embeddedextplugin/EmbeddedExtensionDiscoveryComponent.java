/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
package org.rhq.sample.embeddedextplugin;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.sample.skeletonplugin.SamplePluginDiscoveryComponent;

/**
 * This is a discovery class for the embedded extension resource type.
 */
public class EmbeddedExtensionDiscoveryComponent extends SamplePluginDiscoveryComponent {
    private final Log log = LogFactory.getLog(EmbeddedExtensionDiscoveryComponent.class);

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext context) {
        log.info("Discovering embedded extension resources");
        Set<DiscoveredResourceDetails> discoveredResources = super.discoverResources(context); // call the parent discovery code
        Set<DiscoveredResourceDetails> extendedResources = new HashSet<DiscoveredResourceDetails>();

        int i = -1;
        for (DiscoveredResourceDetails discoveredResource : discoveredResources) {
            String key = "Embedded Extension Resource Key" + (++i > 0 ? String.valueOf(i) : "");
            String name = "Embedded Extension Resource";
            String description = "This describes the Embedded Extension Resource";

            discoveredResource.setResourceKey(key);
            discoveredResource.setResourceName(name);
            discoveredResource.setResourceDescription(description);

            extendedResources.add(discoveredResource);
        }

        return extendedResources;
    }
}