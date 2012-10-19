/*
  * RHQ Management Platform
  * Copyright (C) 2010 Red Hat, Inc.
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
package org.rhq.plugins.jbossas5;

import org.jboss.managed.api.ManagedComponent;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.jbossas5.util.ManagedComponentUtils;

import java.util.Set;

/**
 * The ResourceDiscoveryComponent for the singleton JBoss Messaging ResourceType.
 *
 * @author Ian Springer
 */
public class JBossMessagingDiscoveryComponent extends ManagedComponentDiscoveryComponent<ApplicationServerComponent> {
    private static final String DEFAULT_RESOURCE_NAME = "JBoss Messaging";

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<ApplicationServerComponent> discoveryContext)
            throws Exception {
        return super.discoverResources(discoveryContext);                
    }

    @Override
    protected String getResourceName(ManagedComponent component) {
        return DEFAULT_RESOURCE_NAME;
    }

    @Override
    protected String getResourceVersion(ManagedComponent component) {
        return (String)ManagedComponentUtils.getSimplePropertyValue(component, "providerVersion");
    }
}
