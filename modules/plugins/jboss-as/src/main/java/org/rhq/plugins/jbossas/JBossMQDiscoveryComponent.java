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

package org.rhq.plugins.jbossas;

import java.util.Set;

import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.jmx.JMXComponent;

/**
 * Discover JBossMQ
 *
 * @author Heiko W. Rupp
 */
public class JBossMQDiscoveryComponent extends AbstractMessagingDiscoveryComponent {
    private static final String DEFAULT_RESOURCE_NAME = "JBossMQ";
    private static final String DEFAULT_RESOURCE_DESCRIPTION = "JBossMQ subsystem";

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<JMXComponent> context) {
        String objectNameQueryTemplate = "jboss.mq:service=DestinationManager";
        Set<DiscoveredResourceDetails> result = super.discoverResources(context, objectNameQueryTemplate,
                DEFAULT_RESOURCE_NAME, DEFAULT_RESOURCE_DESCRIPTION,
                "jboss.mq:service=DestinationManager;Interceptor");
        return result;
    }
}