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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.augeas.Augeas;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

public class SambaShareDiscoveryComponent implements ResourceDiscoveryComponent<SambaServerComponent> {

    public Set<DiscoveredResourceDetails> discoverResources(
        ResourceDiscoveryContext<SambaServerComponent> discoveryContext) throws InvalidPluginConfigurationException,
        Exception {

        Set<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>();

        SambaServerComponent serverComponent = discoveryContext.getParentResourceComponent();

        Augeas augeas = null;
        try {
            augeas = serverComponent.getAugeas();

            List<String> matches = augeas.match("/files/etc/samba/smb.conf/target[. != 'global']");
            for (String match : matches) {
                String name = augeas.get(match);
                Configuration pluginConfig = discoveryContext.getDefaultPluginConfiguration();
                pluginConfig.put(new PropertySimple("targetName", name));
                DiscoveredResourceDetails detail = new DiscoveredResourceDetails(discoveryContext.getResourceType(),
                    name, name + " share", null, "Samba Share [" + name + "]", pluginConfig, null);
                details.add(detail);
            }

            return details;
        } finally {
            if (serverComponent != null)
                serverComponent.close();
        }
    }
}
