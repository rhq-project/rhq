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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.support.metadata.InternalVMTypeDescriptor;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

public class InternalJMXServerDiscoveryComponent implements ResourceDiscoveryComponent {
    private static final Log log = LogFactory.getLog(InternalJMXServerDiscoveryComponent.class);

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext context) {
        Set<DiscoveredResourceDetails> found = new HashSet<DiscoveredResourceDetails>();

        DiscoveredResourceDetails localVM = new DiscoveredResourceDetails(context.getResourceType(), "InternalVM",
            context.getResourceType().getName(), System.getProperty("java.version"), "VM of plugin container", null,
            null);
        Configuration configuration = localVM.getPluginConfiguration();
        configuration.put(new PropertySimple(JMXDiscoveryComponent.CONNECTOR_ADDRESS_CONFIG_PROPERTY,
            "Local Connection"));
        configuration.put(new PropertySimple(JMXDiscoveryComponent.CONNECTION_TYPE, InternalVMTypeDescriptor.class
            .getName()));

        found.add(localVM);

        return found;
    }
}