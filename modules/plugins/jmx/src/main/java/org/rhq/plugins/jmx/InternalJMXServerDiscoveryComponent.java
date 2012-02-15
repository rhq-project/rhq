/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.plugins.jmx;

import java.util.HashSet;
import java.util.Set;

import org.mc4j.ems.connection.support.metadata.InternalVMTypeDescriptor;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.jmx.util.ParentDefinedJMXServerNamingUtility;

/**
 * Discovers a singleton Resource representing the local JVM (i.e. the JVM in which the RHQ Plugin Container is running).
 */
public class InternalJMXServerDiscoveryComponent implements ResourceDiscoveryComponent {

    private static final String RESOURCE_KEY = "InternalVM";
    private static final String DEFAULT_RESOURCE_DESCRIPTION = "JVM of RHQ Plugin Container";

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext context) {
        Set<DiscoveredResourceDetails> discoveredResources = new HashSet<DiscoveredResourceDetails>(1);

        String name = ParentDefinedJMXServerNamingUtility.getJVMName(context);
        String version = System.getProperty("java.version");
        DiscoveredResourceDetails localVM = new DiscoveredResourceDetails(context.getResourceType(), RESOURCE_KEY,
            name, version, DEFAULT_RESOURCE_DESCRIPTION, context.getDefaultPluginConfiguration(),
            context.getSystemInformation().getThisProcess());
        Configuration configuration = localVM.getPluginConfiguration();
        configuration.put(new PropertySimple(JMXDiscoveryComponent.CONNECTOR_ADDRESS_CONFIG_PROPERTY,
            "Local Connection"));
        configuration.put(new PropertySimple(JMXDiscoveryComponent.CONNECTION_TYPE, InternalVMTypeDescriptor.class
            .getName()));
        discoveredResources.add(localVM);

        return discoveredResources;
    }

}
