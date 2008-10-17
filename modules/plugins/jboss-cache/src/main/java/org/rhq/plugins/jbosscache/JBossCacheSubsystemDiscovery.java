/*
 * Jopr Management Platform Copyright (C) 2005-2008 Red Hat, Inc. All rights
 * reserved. This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser General
 * Public License, version 2.1, also as published by the Free Software
 * Foundation. This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License and the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU General Public License and the GNU
 * Lesser General Public License along with this program; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */
package org.rhq.plugins.jbosscache;

import java.util.HashSet;
import java.util.Set;

import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * Discover (i.e. create) the JBossCache subsystem
 * 
 * @author Heiko W. Rupp
 */
public class JBossCacheSubsystemDiscovery implements ResourceDiscoveryComponent<JBossCacheSubsystemComponent>
{

   private static final String JBOSS_CACHE_SUBSYSTEM = "JBoss Cache subsystem";

   /*
    * (non-Javadoc)
    * 
    * @see org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent#discoverResources(org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext)
    */
   public Set<DiscoveredResourceDetails> discoverResources( ResourceDiscoveryContext<JBossCacheSubsystemComponent> context)
            throws InvalidPluginConfigurationException, Exception {

      Set<DiscoveredResourceDetails> subsystem = new HashSet<DiscoveredResourceDetails>(1);

      DiscoveredResourceDetails detail = new DiscoveredResourceDetails(context
               .getResourceType(), // resource type
               JBOSS_CACHE_SUBSYSTEM, // resource key
               JBOSS_CACHE_SUBSYSTEM, // resource name
               "1.0", // version
               JBOSS_CACHE_SUBSYSTEM, // description
               context.getDefaultPluginConfiguration(), // plugin configuration
               null); // no process discovery

      subsystem.add(detail);

      return subsystem;
   }

}
