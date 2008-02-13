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
package org.rhq.plugins.jmx;

import java.util.HashSet;
import java.util.Set;
import org.mc4j.ems.connection.support.metadata.InternalVMTypeDescriptor;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * This discovery component can be used to include JVM information under a parent Process oriented server that supports
 * JMX. The parent resource type's component must implement JMXComponent.
 *
 * @author Greg Hinkle
 */
public class EmbeddedJMXServerDiscoveryComponent implements ResourceDiscoveryComponent<JMXComponent> {
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<JMXComponent> context) {
        Set<DiscoveredResourceDetails> found = new HashSet<DiscoveredResourceDetails>();

        Configuration configuration = context.getDefaultPluginConfiguration();
        if (context.getParentResourceComponent().getEmsConnection().getConnectionProvider().getConnectionSettings()
            .getConnectionType() instanceof InternalVMTypeDescriptor) {
            // If our parent is internal, it may have chosen a specific local mbean server (as in the jboss server)
            // so we will look our own up
            configuration.put(new PropertySimple(JMXDiscoveryComponent.CONNECTOR_ADDRESS_CONFIG_PROPERTY,
                "Local Connection"));
            configuration.put(new PropertySimple(JMXDiscoveryComponent.CONNECTION_TYPE, InternalVMTypeDescriptor.class
                .getName()));
        } else {
            configuration.put(new PropertySimple(JMXDiscoveryComponent.CONNECTION_TYPE,
                JMXDiscoveryComponent.PARENT_TYPE));
        }

        if (context.getParentResourceComponent().getEmsConnection().getBean("java.lang:type=OperatingSystem") != null) {
            // Only inventory a VM that has the platform mbean's exposed and available
            DiscoveredResourceDetails s = new DiscoveredResourceDetails(context.getResourceType(), "JVM", context
                .getResourceType().getName(), System.getProperty("java.version"), "VM that jboss runs on",
                configuration, null);

            found.add(s);
        }

        return found;
    }
}