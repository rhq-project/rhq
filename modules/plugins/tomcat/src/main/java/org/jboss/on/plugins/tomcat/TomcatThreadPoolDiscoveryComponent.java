/*
 * Jopr Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.jboss.on.plugins.tomcat;

import java.util.HashSet;
import java.util.Set;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.jmx.MBeanResourceDiscoveryComponent;

/**
 * Discovery component for Tomcat connectors. The bulk of the discovery is performed by the super class. This
 * class exists to work with the bean attribute values once they were read.
 *
 * @author Jay Shaughnessy
 */
public class TomcatThreadPoolDiscoveryComponent extends MBeanResourceDiscoveryComponent<TomcatConnectorComponent> {

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<TomcatConnectorComponent> discoveryContext) {

        Set<DiscoveredResourceDetails> resources = super.discoverResources(discoveryContext);
        Set<DiscoveredResourceDetails> validResources = new HashSet<DiscoveredResourceDetails>();
        Configuration parentConfig = discoveryContext.getParentResourceContext().getPluginConfiguration();

        for (DiscoveredResourceDetails detail : resources) {
            String threadPoolName = detail.getPluginConfiguration().getSimpleValue(TomcatThreadPoolComponent.PROPERTY_NAME, "");
            if (threadPoolName.contains(parentConfig.getSimpleValue(TomcatConnectorComponent.PROPERTY_PORT, "-1"))) {
                validResources.add(detail);
            }
        }

        return validResources;
    }
}