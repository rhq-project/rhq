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

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.sigar.win32.Pdh;
import org.hyperic.sigar.win32.Service;
import org.hyperic.sigar.win32.Win32Exception;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.iis.util.Win32ServiceControlDelegate;

/**
 *
 * @author Greg Hinkle
 * @author Joseph Marques
 */
public class IISServerComponent implements ResourceComponent, MeasurementFacet, OperationFacet {

    private static final String WINDOWS_SERVICE_NAME = "W3SVC";
    private static final long SERVICE_CONTROL_TIMEOUT = 30000L;

    private Log log = LogFactory.getLog(IISServerComponent.class);
    private ResourceContext resourceContext;
    private Service service;

    public void start(ResourceContext resourceContext) throws InvalidPluginConfigurationException, Exception {
        this.resourceContext = resourceContext;
    }

    public void stop() {
    }

    private Service getIISService() throws Win32Exception {
        if (service == null) {
            service = new Service(WINDOWS_SERVICE_NAME);
        }
        return service;
    }

    public AvailabilityType getAvailability() {
        try {
            return (getIISService().getStatus() == Service.SERVICE_RUNNING) ? AvailabilityType.UP
                : AvailabilityType.DOWN;
        } catch (Win32Exception e) {
            log.warn("Unable to check status of IIS Windows service.");
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

    public OperationResult invokeOperation(String name, Configuration parameters) throws InterruptedException,
        Exception {
        OperationResult result = null;

        try {
            if ((parameters == null) || (parameters.getProperties().size() == 0)) {
                if (name.equals("start")) {
                    getControlDelegate(null).start();
                } else if (name.equals("stop")) {
                    getControlDelegate(null).stop();
                } else {
                    // this should really never happen
                    throw new IllegalArgumentException("Operation [" + name + "] is not yet supported by this plugin");
                }
            } else {
                // this should really never happen
                throw new IllegalArgumentException("Operation [" + name + "] does not support params");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke operation [" + name + "]", e);
        }

        return result;
    }

    private Win32ServiceControlDelegate getControlDelegate(Long timeout) throws Win32Exception {
        Service iisService = getIISService();
        String serviceName = iisService.getConfig().getName();
        List<String> serviceDependencies = Arrays.asList(iisService.getConfig().getDependencies());
        long serviceTimeout = (timeout == null ? SERVICE_CONTROL_TIMEOUT : timeout);
        return new Win32ServiceControlDelegate(serviceName, serviceDependencies, null, serviceTimeout);
    }
}
