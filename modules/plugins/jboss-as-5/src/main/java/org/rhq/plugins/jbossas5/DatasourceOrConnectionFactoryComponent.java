/*
 * Jopr Management Platform
 * Copyright (C) 2012 Red Hat, Inc.
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

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.managed.api.ManagedComponent;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.operation.OperationResult;

/**
 * A Resource component for managing JBoss AS 5/6 datasources or connection factories.
 *
 * @author Ian Springer
 */
public class DatasourceOrConnectionFactoryComponent extends ManagedComponentComponent {

    private final Log log = LogFactory.getLog(this.getClass());

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
        getValues(getManagedComponent(), report, metrics);
    }

    @Override
    protected void getValues(ManagedComponent managedComponent, MeasurementReport report,
                             Set<MeasurementScheduleRequest> metrics) throws Exception {
        Set<MeasurementScheduleRequest> uncollectedMetrics = new HashSet<MeasurementScheduleRequest>();
        for (MeasurementScheduleRequest request : metrics) {
            String metricName = request.getName();
            if (metricName.equals("custom.connectionAvailable")) {
                try {
                    Configuration parameters = new Configuration();
                    OperationResult result = invokeOperation(managedComponent, "testConnection", parameters);
                    PropertySimple resultProp = result.getComplexResults().getSimple("result");
                    boolean connectionAvailable = resultProp.getBooleanValue();
                    MeasurementDataTrait trait = new MeasurementDataTrait(request, connectionAvailable ? "yes" : "no");
                    report.addData(trait);
                } catch (Exception e) {
                    log.error("Failed to collect trait [" + metricName + "].", e);
                }
            } else {
                uncollectedMetrics.add(request);
            }
        }

        super.getValues(managedComponent, report, uncollectedMetrics);
    }

}
