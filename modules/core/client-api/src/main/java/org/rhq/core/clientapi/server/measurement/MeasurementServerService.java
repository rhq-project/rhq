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
package org.rhq.core.clientapi.server.measurement;

import java.util.Set;
import org.rhq.core.communications.command.annotation.Asynchronous;
import org.rhq.core.communications.command.annotation.LimitedConcurrency;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.ResourceMeasurementScheduleRequest;

/**
 * The server-side interface that provides access to the metric reporting facilities. Agents will use this interface to
 * report new metric data to the server.
 *
 * @author John Mazzitelli
 */
public interface MeasurementServerService {
    String CONCURRENCY_LIMIT_MEASUREMENT_REPORT = "rhq.server.concurrency-limit.measurement-report";
    String CONCURRENCY_LIMIT_MEASUREMENT_SCHEDULE_REQUEST = "rhq.server.concurrency-limit.measurement-schedule-request";

    /**
     * This method is called when new measurements are to be reported from an agent to the server.
     *
     * @param report the report containing the metric data
     */
    @Asynchronous(guaranteedDelivery = true)
    @LimitedConcurrency(CONCURRENCY_LIMIT_MEASUREMENT_REPORT)
    void mergeMeasurementReport(MeasurementReport report);

    /**
     * Asks the server to return all measurement schedules for the given resource and optionally its child resources.
     *
     * @param  resourceId        identifies the resource whose schedules are to be returned
     * @param  getChildSchedules if true the schedules for an entire subtree will be retrieved
     *
     * @return set of all measurement schedules for the resource and its children resources, if applicable
     */
    @LimitedConcurrency(CONCURRENCY_LIMIT_MEASUREMENT_SCHEDULE_REQUEST)
    Set<ResourceMeasurementScheduleRequest> getLatestSchedulesForResourceId(int resourceId, boolean getChildSchedules);
}