/*
 * RHQ Management Platform
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.rhq.core.pc.availability;

import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pluginapi.availability.AvailabilityCollectorRunnable;
import org.rhq.core.pluginapi.availability.AvailabilityContext;
import org.rhq.core.pluginapi.availability.AvailabilityFacet;

/**
 * @author Jay Shaughnessy
 */
public class AvailabilityContextImpl implements AvailabilityContext {

    private final Resource resource;

    public AvailabilityContextImpl(Resource resource) {
        super();
        this.resource = resource;
    }

    @Override
    @Deprecated
    public AvailabilityCollectorRunnable createAvailabilityCollectorRunnable(AvailabilityFacet facet, long i) {
        // even though this shouldn't be used, plugins may still be using this. Allow for these to still be
        // created for backward compatibility, but its a dummy object nevertheless.
        return new AvailabilityCollectorRunnable(facet, i, null, null);
    }

    @Override
    public void requestAvailabilityCheck() {
        PluginContainer.getInstance().getInventoryManager().requestAvailabilityCheck(resource);
    }

    @Override
    public AvailabilityType getLastReportedAvailability() {
        Availability avail = PluginContainer.getInstance().getInventoryManager().getAvailabilityIfKnown(resource);
        return (null != avail) ? avail.getAvailabilityType() : null;
    }

    @Override
    public void disable() {
        PluginContainer.getInstance().getInventoryManager().setResourceEnablement(resource.getId(), false);
    }

    @Override
    public void enable() {
        PluginContainer.getInstance().getInventoryManager().setResourceEnablement(resource.getId(), true);
    }

    /**
     * Only used in tests
     */
    public Resource getResource() {
        return resource;
    }
}
