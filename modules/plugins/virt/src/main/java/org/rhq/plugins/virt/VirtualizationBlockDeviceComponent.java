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
package org.rhq.plugins.virt;

import java.lang.reflect.Field;
import java.util.Set;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;

/**
 * @author Greg Hinkle
 */
public class VirtualizationBlockDeviceComponent implements ResourceComponent<VirtualizationComponent>, MeasurementFacet {

    ResourceContext<VirtualizationComponent> resourceContext;

    public void start(ResourceContext<VirtualizationComponent> virtualizationComponentResourceContext)
        throws InvalidPluginConfigurationException, Exception {
        this.resourceContext = virtualizationComponentResourceContext;
    }

    public void stop() {
    }

    public AvailabilityType getAvailability() {
        return resourceContext.getParentResourceComponent().getAvailability();
    }

    private LibVirtConnection getConnection() {
        return this.resourceContext.getParentResourceComponent().getConnection();
    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {

        LibVirt.VirDomainBlockStats stats = getConnection().getDomainBlockStats(
            this.resourceContext.getParentResourceComponent().getDomainName(), this.resourceContext.getResourceKey());

        for (MeasurementScheduleRequest request : metrics) {
            Field f = stats.getClass().getField(request.getName());
            report.addData(new MeasurementDataNumeric(request, (double) f.getLong(stats)));
        }
    }
}