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
 * Implementations of this facet perform the collection of measurement data that is emitted from a monitored resource.
 * Resource components expose this interface when it has one or more metrics that it exposes.
 *
 * @author Greg Hinkle
 */
public interface MeasurementFacet {
    /**
     * Collects measurement data emitted from monitored resources and returns that data in a measurement report. All
     * collected data will be added to the given <code>report</code>.
     *
     * <p>The key to the improvement in metric collection performance and reduction in monitoring overhead is to take
     * advantage of the situations where a single remote call can return more than one piece of data; e.g. a JMX MBean
     * getAttributes call, or a database select.</p>
     *
     * <p>TODO GH: So far this is limited to multi-collections by a single resource. Could support cross-resource
     * collections too?</p>
     *
     * @param  report  the report where all collected measurement data will be added
     * @param  metrics the schedule of what needs to be collected when
     *
     * @throws Exception if the component failed to obtain one or more values
     */
    void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception;
}