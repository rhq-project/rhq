/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.plugins.cassandra;

import java.util.Set;

import org.rhq.core.domain.util.StringUtils;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.jmx.JMXComponent;
import org.rhq.plugins.jmx.MBeanResourceDiscoveryComponent;

/**
 * @author Stefan Negrea
 */
public class MetricsDiscoveryComponent extends MBeanResourceDiscoveryComponent<JMXComponent<?>> {

    public static final String PROPERTY_NAME_MARKER = "nameMarker";

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<JMXComponent<?>> context) {
        String nameMarker = context.getDefaultPluginConfiguration().getSimpleValue(PROPERTY_NAME_MARKER);

        Set<DiscoveredResourceDetails> discoveredResources = super.discoverResources(context, true);

        String updatedResourceName;
        int index;
        for (DiscoveredResourceDetails discoveredResource : discoveredResources) {
            updatedResourceName = discoveredResource.getResourceName();
            if ( (index = updatedResourceName.indexOf(nameMarker)) != -1) {
                updatedResourceName = updatedResourceName.substring(index + nameMarker.length());
                if (updatedResourceName.length() != 0) {
                    updatedResourceName = StringUtils.deCamelCase(updatedResourceName);
                    discoveredResource.setResourceName(updatedResourceName);
                }
            }
        }

        return discoveredResources;
    }

}
