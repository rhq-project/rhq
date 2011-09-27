/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

package org.rhq.core.pc.upgrade.plugins.multi.base;

import java.util.HashSet;
import java.util.Set;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 *
 * @author Lukas Krejci
 */
public class BaseDiscoveryComponent<T extends ResourceComponent<?>> implements ResourceDiscoveryComponent<T> {

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<T> context)
        throws InvalidPluginConfigurationException, Exception {

        HashSet<DiscoveredResourceDetails> results = new HashSet<DiscoveredResourceDetails>();

        Configuration pluginConfig = context.getDefaultPluginConfiguration();

        String keyTemplate = pluginConfig.getSimpleValue("key", null);
        int count = pluginConfig.getSimple("count").getIntegerValue();

        int parentN = 0;

        if (context.getParentResourceContext() != null) {
            PropertySimple parentOrdinalProperty = context.getParentResourceContext().getPluginConfiguration()
                .getSimple("ordinal");

            if (parentOrdinalProperty != null) {
                parentN = parentOrdinalProperty.getIntegerValue();
            }
        }

        for (int i = 0; i < count; ++i) {

            String key = getResourceKey(keyTemplate, i, parentN);

            pluginConfig = context.getDefaultPluginConfiguration();

            pluginConfig.put(new PropertySimple("ordinal", i));

            DiscoveredResourceDetails detail = new DiscoveredResourceDetails(context.getResourceType(), key, key, null,
                null, pluginConfig, null);

            results.add(detail);
        }

        return results;
    }

    protected static String getResourceKey(String template, int n, int parentN) {
        return template.replace("%n", Integer.toString(n)).replace("%p", Integer.toString(parentN));
    }
}
