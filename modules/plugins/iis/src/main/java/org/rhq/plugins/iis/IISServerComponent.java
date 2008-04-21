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
import org.hyperic.sigar.win32.Pdh;
import org.hyperic.sigar.win32.Win32;
import org.hyperic.sigar.win32.Service;
import org.hyperic.sigar.win32.Win32Exception;
import org.hyperic.sigar.cmd.Win32Service;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Set;

/**
 *
 * @author Greg Hinkle
 */
public class IISServerComponent implements ResourceComponent, MeasurementFacet {

    private Log log = LogFactory.getLog(IISServerComponent.class);
    private ResourceContext resourceContext;

    public void start(ResourceContext resourceContext) throws InvalidPluginConfigurationException, Exception {
        this.resourceContext = resourceContext;
    }

    public void stop() {
    }

    public AvailabilityType getAvailability() {
        try {
            Service service = new Service("W3SVC");
            return (service.getStatus() == Service.SERVICE_RUNNING) ? AvailabilityType.UP : AvailabilityType.DOWN;
        } catch (Win32Exception e) {
            log.warn("Unable to discover IIS");
        }
        return AvailabilityType.DOWN;
    }


    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {

        String propertyBase = "\\Web Service(_Total)\\";
        Pdh pdh = new Pdh();

        for (MeasurementScheduleRequest request : metrics) {
            double value = pdh.getRawValue(propertyBase + request.getName());
            report.addData(new MeasurementDataNumeric(request, value));
        }
    }
}
