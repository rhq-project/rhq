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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.EmsConnection;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.jmx.JMXComponent;

/**
 * TODO
 *
 * @author John Mazzitelli
 */
public class JBossASTomcatServerComponent implements MeasurementFacet, OperationFacet, ConfigurationFacet,
    JMXComponent<JMXComponent> {
    private static Log log = LogFactory.getLog(JBossASTomcatServerComponent.class);

    private ResourceContext<JMXComponent> resourceContext;

    public void start(ResourceContext<JMXComponent> context) {
        resourceContext = context;
    }

    public void stop() {
    }

    public AvailabilityType getAvailability() {
        return AvailabilityType.UP;
    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) {
        for (MeasurementScheduleRequest request : requests) {
            String name = request.getName();

            // TODO: based on the request information, you must collect the requested measurement(s)
            //       you can use the name of the measurement to determine what you actually need to collect
            try {
                Number value = 1; // dummy measurement value - this should come from the managed resource
                report.addData(new MeasurementDataNumeric(request, value.doubleValue()));
            } catch (Exception e) {
                log.error("Failed to obtain measurement [" + name + "]. Cause: " + e);
            }
        }

        return;
    }

    public OperationResult invokeOperation(String name, Configuration configuration) {
        return null;
    }

    public Configuration loadResourceConfiguration() {
        Configuration config = null;

        return config;
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        report.setStatus(ConfigurationUpdateStatus.SUCCESS);
    }

    public EmsConnection getEmsConnection() {
        return this.resourceContext.getParentResourceComponent().getEmsConnection();
    }

    ResourceContext<JMXComponent> getResourceContext() {
        return resourceContext;
    }
}