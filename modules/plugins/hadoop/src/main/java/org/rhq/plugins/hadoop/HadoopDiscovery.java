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
package org.rhq.plugins.hadoop;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * Discover a hadoop subsystem. When one of the hadoop server processes
 * is present, this class discovers a Hadoop 'subsystem' that holds all the
 * children in the resource tree.
 *
 * @author Heiko W. Rupp
 */
public class HadoopDiscovery implements ResourceDiscoveryComponent {

    private final Log log = LogFactory.getLog(HadoopDiscovery.class);

    public Set<DiscoveredResourceDetails> discoverResources(
            ResourceDiscoveryContext resourceDiscoveryContext) throws InvalidPluginConfigurationException, Exception {

        Set<DiscoveredResourceDetails> result = new HashSet<DiscoveredResourceDetails>(1);

        if (resourceDiscoveryContext.getAutoDiscoveredProcesses().size()>0) {
            // Found some Hadoop processes

            DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
                    resourceDiscoveryContext.getResourceType(),
                    "Hadoop|", // TODO add path
                    "Hadoop",
                    null, // get from core jar
                    "Hadoop Framework",
                    resourceDiscoveryContext.getDefaultPluginConfiguration(),
                    null // ProcessInfo
            );
            result.add(detail);
        }
        return result;
    }
}
