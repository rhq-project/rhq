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
package org.rhq.core.clientapi.server.measurement;

import java.util.Set;

import org.rhq.core.communications.command.annotation.Asynchronous;
import org.rhq.core.communications.command.annotation.LimitedConcurrency;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
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
     * Asks the server to return all measurement schedules for the given resources and optionally their child resources.
     *
     * @param  resourceIds       identifies resources whose schedules are to be returned
     * @param  getChildSchedules if true the schedules for an entire subtree will be retrieved
     *
     * @return set of all measurement schedules for the resources and their children resources, if applicable
     */
    @LimitedConcurrency(CONCURRENCY_LIMIT_MEASUREMENT_SCHEDULE_REQUEST)
    Set<ResourceMeasurementScheduleRequest> getLatestSchedulesForResourceIds(Set<Integer> resourceIds,
        boolean getChildSchedules);

    /**
     * Asks the server to return all measurement schedules for the given resource and optionally its child resources.
     *
     * @param  resourceId        identifies the resource whose schedules are to be returned
     * @param  getChildSchedules if true the schedules for an entire subtree will be retrieved
     *
     * @return set of all measurement schedules for the resource and its children resources, if applicable
     */
    @LimitedConcurrency(CONCURRENCY_LIMIT_MEASUREMENT_SCHEDULE_REQUEST)
    Set<ResourceMeasurementScheduleRequest> getLatestSchedulesForResourceId(int resourceIds, boolean getChildSchedules);

    /**
     * Asks the server for the last value of the trait that is currently known to it.
     * @param scheduleId the schedule id of the trait
     * @return the trait value or null if no value is known to the server
     */
    MeasurementDataTrait getLastKnownTraitValue(int scheduleId);
}
