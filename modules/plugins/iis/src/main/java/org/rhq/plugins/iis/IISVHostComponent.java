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
package org.rhq.plugins.iis;

import java.util.Set;

import org.hyperic.sigar.win32.Pdh;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.measurement.calltime.CallTimeData;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;

/**
 *
 * @author Greg Hinkle
 * @author Joseph Marques
 */
public class IISVHostComponent implements ResourceComponent<IISServerComponent>, MeasurementFacet {

    private ResourceContext<IISServerComponent> resourceContext;
    private IISResponseTimeDelegate responseTimeDelegate;

    public void start(ResourceContext<IISServerComponent> resourceContext) throws InvalidPluginConfigurationException,
        Exception {
        this.resourceContext = resourceContext;

        String logDirectory = resourceContext.getPluginConfiguration().getSimpleValue("logDirectory", null);

        responseTimeDelegate = new IISResponseTimeDelegate(logDirectory);

    }

    public void stop() {
    }

    public AvailabilityType getAvailability() {
        return AvailabilityType.UP;
    }

    public String getSiteName() {
        return this.resourceContext.getPluginConfiguration().getSimpleValue("siteName", null);
    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {

        String propertyBase = "\\Web Service(" + getSiteName() + ")\\";
        Pdh pdh = new Pdh();

        for (MeasurementScheduleRequest request : metrics) {
            if (request.getDataType() == DataType.CALLTIME) {
                CallTimeData callTimeData = new CallTimeData(request);
                this.responseTimeDelegate.parseLogs(callTimeData);
                report.addData(callTimeData);
            } else {
                double value = pdh.getRawValue(propertyBase + request.getName());
                report.addData(new MeasurementDataNumeric(request, value));
            }
        }
    }
}