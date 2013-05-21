/*
  * RHQ Management Platform
  * Copyright (C) 2005-2013 Red Hat, Inc.
  * All rights reserved.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License, version 2, as
  * published by the Free Software Foundation, and/or the GNU Lesser
  * General Public License, version 2.1, also as published by the Free
  * Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License and the GNU Lesser General Public License
  * for more details.
  *
  * You should have received a copy of the GNU General Public License
  * and the GNU Lesser General Public License along with this program;
  * if not, write to the Free Software Foundation, Inc.,
  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
  */
package org.rhq.plugins.skinnyplatform;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

@SuppressWarnings("rawtypes")
public class PlatformDiscoveryComponent implements ResourceDiscoveryComponent {

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext context) {
        String pcName = context.getPluginContainerName();
        String hostname = getHostname();

        String name = (pcName != null ? pcName : hostname);
        String key = "skinny:" + name;
        String description = context.getResourceType().getDescription();
        String version = "1.0";

        DiscoveredResourceDetails discoveredResource = new DiscoveredResourceDetails(context.getResourceType(), key,
            name, version, description, null, null);

        HashSet<DiscoveredResourceDetails> results = new HashSet<DiscoveredResourceDetails>();
        results.add(discoveredResource);
        return results;
    }

    private String getHostname() {
        String name;
        try {
            name = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            name = null;
        }

        // we fought the good fight but we just can't get this machine's hostname, give a generic platform name
        if (name == null) {
            name = "Unnamed Skinny Platform";
        }
        return name;
    }
}