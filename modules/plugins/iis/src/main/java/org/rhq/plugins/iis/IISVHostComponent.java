/*
 * JBoss, a division of Red Hat.
 * Copyright 2007, Red Hat Middleware, LLC. All rights reserved.
 */

package org.rhq.plugins.iis;

import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.calltime.CallTimeData;
import org.hyperic.sigar.win32.Pdh;

import java.util.Set;
import java.io.File;

/**
 *
 * @author Greg Hinkle
 */
public class IISVHostComponent implements ResourceComponent<IISServerComponent>, MeasurementFacet {

    private ResourceContext<IISServerComponent> resourceContext;
    private IISResponseTimeDelegate responseTimeDelegate;

    public void start(ResourceContext<IISServerComponent> resourceContext) throws InvalidPluginConfigurationException, Exception {
        this.resourceContext = resourceContext;

        String logDirectory = resourceContext.getPluginConfiguration().getSimpleValue("logDirectory",null);

        responseTimeDelegate = new IISResponseTimeDelegate(new File(logDirectory));

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

            if (request.getName().equals("responseTimes")) {
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