/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

package org.rhq.core.pc.avail.testplugin;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;

public class AvailResourceComponent implements ResourceComponent<ResourceComponent<?>> {

    private AvailabilityType avail = AvailabilityType.UP;
    private ResourceContext<ResourceComponent<?>> context;

    public void setNextAvailability(AvailabilityType avail) {
        this.avail = avail;
    }

    public ResourceContext<ResourceComponent<?>> getResourceContext() {
        return this.context;
    }

    @Override
    public AvailabilityType getAvailability() {
        return this.avail;
    }

    @Override
    public void start(ResourceContext<ResourceComponent<?>> context) throws InvalidPluginConfigurationException,
        Exception {
        this.context = context;
    }

    @Override
    public void stop() {
    }
}
