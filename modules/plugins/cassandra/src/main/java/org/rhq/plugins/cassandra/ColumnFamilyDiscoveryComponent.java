/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License, version 2, as
 *  * published by the Free Software Foundation, and/or the GNU Lesser
 *  * General Public License, version 2.1, also as published by the Free
 *  * Software Foundation.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License and the GNU Lesser General Public License
 *  * for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * and the GNU Lesser General Public License along with this program;
 *  * if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package org.rhq.plugins.cassandra;

import java.util.HashSet;
import java.util.Set;

import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.jmx.JMXComponent;
import org.rhq.plugins.jmx.MBeanResourceDiscoveryComponent;

/**
 * @author John Sanda
 */
public class ColumnFamilyDiscoveryComponent extends MBeanResourceDiscoveryComponent<JMXComponent<?>> {

    private static final String COLUMN_FAMILY_MARKER = "columnfamily=";

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<JMXComponent<?>> context) {
        Set<DiscoveredResourceDetails> discoveredResources = super.discoverResources(context, false);
        String keyspace = context.getParentResourceContext().getResourceKey();
        Set<DiscoveredResourceDetails> selectedResources = new HashSet<DiscoveredResourceDetails>();
        for (DiscoveredResourceDetails columnFamilyMBean : discoveredResources) {
            if (columnFamilyMBean.getResourceKey().contains("keyspace=" + keyspace)) {
                String resourceKey = columnFamilyMBean.getResourceKey();
                resourceKey = resourceKey.substring(resourceKey.indexOf(COLUMN_FAMILY_MARKER)
                    + COLUMN_FAMILY_MARKER.length());
                if (resourceKey.indexOf(',') > -1) {
                    resourceKey = resourceKey.substring(0, resourceKey.indexOf(','));
                }

                columnFamilyMBean.setResourceKey(resourceKey);
                columnFamilyMBean.getPluginConfiguration().setSimpleValue("name", resourceKey);
                columnFamilyMBean.setResourceName(resourceKey);

                selectedResources.add(columnFamilyMBean);
            }
        }

        return selectedResources;
    }
}
