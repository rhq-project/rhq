/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.plugins.iptables;

import java.util.HashSet;
import java.util.Set;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.rhqtransform.RhqConfig;
/**
 * 
 * @author Filip Drabek
 *
 */
public class IptablesDiscoveryComponent implements ResourceDiscoveryComponent {

       public Set discoverResources(ResourceDiscoveryContext discoveryContext)
                     throws InvalidPluginConfigurationException, Exception {
              
              Set<DiscoveredResourceDetails> discoveredResources = new HashSet<DiscoveredResourceDetails>(1);
               
               Configuration pluginConfiguration = discoveryContext.getDefaultPluginConfiguration();

               RhqConfig config = new RhqConfig(pluginConfiguration);
               
                Set<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>();
                 DiscoveredResourceDetails resource =
               new DiscoveredResourceDetails(discoveryContext.getResourceType(), "iptables", "IPTABLES",
                   "", "IPTABLES.", pluginConfiguration, null);

           details.add(resource);
           return details;
       }



}
