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
package org.rhq.plugins.samba;

import java.util.Set;
import java.util.List;
import java.util.HashSet;

import net.augeas.Augeas;

import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.domain.configuration.Configuration;

/**
 * @author Greg Hinkle
 */
public class SambaShareDiscoveryComponent implements ResourceDiscoveryComponent<SambaServerComponent> {


    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<SambaServerComponent> discoveryContext)
            throws InvalidPluginConfigurationException, Exception {

        Set<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>();

        SambaServerComponent serverComponent = discoveryContext.getParentResourceComponent();

        Augeas augeas = serverComponent.getAugeas();
        String augeasPath = serverComponent.getAugeasPath();

        List<String> matches = augeas.match(augeasPath);
        for (String match : matches) {
            if (match.startsWith("target")) {
                String name = augeas.get(match);
                if (!name.equals("global")) {

                    Configuration config = discoveryContext.getDefaultPluginConfiguration();

                    DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
                        discoveryContext.getResourceType(),
                            name,
                            name + " share",
                            null,
                            "Samba Share [" + name + "]",
                            config,
                            null
                    );
                    details.add(detail);
                }
            }
        }
        
        return details;
    }
}
