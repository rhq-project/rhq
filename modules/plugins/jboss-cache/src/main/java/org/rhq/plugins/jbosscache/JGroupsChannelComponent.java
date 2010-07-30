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
package org.rhq.plugins.jbosscache;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;

/**
 * Resource component that deals with JGroups channels
 *
 * @author Heiko W. Rupp
 */
public class JGroupsChannelComponent implements ResourceComponent<JBossCacheComponent> {

    ResourceContext<JBossCacheComponent> context;

    /**
     * This one does not (yet) have an availability of its own, but just follows the
     * JBossCache instance it is associated with.
     * @see org.rhq.core.pluginapi.inventory.ResourceComponent#getAvailability()
     */
    public AvailabilityType getAvailability() {
        return context.getParentResourceComponent().getAvailability();
    }

    /* (non-Javadoc)
     * @see org.rhq.core.pluginapi.inventory.ResourceComponent#start(org.rhq.core.pluginapi.inventory.ResourceContext)
     */
    public void start(ResourceContext<JBossCacheComponent> context) throws  Exception {
        // TODO Auto-generated method stub
        this.context = context;

    }

    /* (non-Javadoc)
     * @see org.rhq.core.pluginapi.inventory.ResourceComponent#stop()
     */
    public void stop() {
        // TODO Auto-generated method stub

    }

}
