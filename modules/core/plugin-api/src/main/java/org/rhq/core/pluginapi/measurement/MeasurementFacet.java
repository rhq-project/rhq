/*
  * RHQ Management Platform
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
package org.rhq.core.pluginapi.measurement;

import java.util.Set;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;

/**
 * Implementations of this facet perform the collection of measurement data exposed by a managed resource.
 *
 * @author Greg Hinkle
 */
public interface MeasurementFacet {
    /**
     * Collects measurement data exposed by managed resources and returns that data in a measurement report. the
     * provided set of {@link MeasurementScheduleRequest request}s specify which metrics are being requested. All
     * collected data should be added to the provided {@link MeasurementReport report}.
     * <p>
     * If there is no data available for one of the requested metrics, there are several ways an implementation of this
     * method can indicate that:
     * <ol>
     *  <li>do not add a {@link org.rhq.core.domain.measurement.MeasurementData datum} to the report</li>
     *  <li>add a {@link org.rhq.core.domain.measurement.MeasurementData datum} with a value of null to the report</li>
     *  <li>if it's a numeric metric, add a {@link org.rhq.core.domain.measurement.MeasurementData datum} with a value
     *      of {@link Double#NaN} to the report</li>
     * </ol>
     * </p>
     * <p>
     * The key to the improvement in metric collection performance and reduction in monitoring overhead is to take
     * advantage of the situations where a single remote call can return more than one piece of data; e.g. a JMX MBean
     * {@link javax.management.MBeanServer#getAttributes(javax.management.ObjectName, String[]) getAttributes} call, or
     * a database SELECT.
     * </p>
     *
     * @param report  the report to which all collected measurement data should be added
     * @param metrics a set of requests describing the metrics being requested
     *
     * @throws Exception if the component failed to obtain one or more values
     */
    // TODO GH: So far this is limited to multi-collections by a single resource. Could support cross-resource
    //          collections too?
    void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception;
}