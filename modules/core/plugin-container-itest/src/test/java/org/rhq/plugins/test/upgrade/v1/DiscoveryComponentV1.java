/*
 * RHQ Management Platform
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.rhq.plugins.test.upgrade.v1;

import java.util.Collections;
import java.util.Set;

import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * @author Lukas Krejci
 *
 */
public class DiscoveryComponentV1 implements ResourceDiscoveryComponent<ResourceComponent<?>> {

    public static final String RESOURCE_KEY = "resourceKey";
    public static final String RESOURCE_NAME = RESOURCE_KEY;

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<ResourceComponent<?>> context)
        throws InvalidPluginConfigurationException, Exception {

        return Collections.singleton(new DiscoveredResourceDetails(context.getResourceType(), RESOURCE_KEY,
            RESOURCE_NAME, null,
            null, context.getDefaultPluginConfiguration(), null));
    }
}
