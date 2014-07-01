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
package org.rhq.core.domain.measurement.calltime;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;

/**
 * Call-time data for a set of calls to a particular service (e.g. a webapp or a session EJB) over a certain time span.
 * Plugins report call-time metric data by creating one <code>CallTimeData</code> for each requested schedule and adding
 * it to the {@link org.rhq.core.domain.measurement.MeasurementReport} provided by the plugin container.
 *
 * @author Ian Springer
 */
public class CallTimeData implements Serializable {
    private static final long serialVersionUID = 1L;

    private int scheduleId;

    private Map<String, CallTimeDataValue> values = new HashMap<String, CallTimeDataValue>();

    /**
     * Create a new <code>CallTimeData</code>.
     *
     * @param schedule the schedule for which this data was collected
     */
    public CallTimeData(MeasurementScheduleRequest schedule) {
        this(schedule.getScheduleId());
    }

    /**
     * Constructs a new <code>CallTimeData</code>.
     */
    public CallTimeData(int scheduleId) {
        this.scheduleId = scheduleId;
    }

    /**
     * Add data for a call to the specified destination.
     *
     * @param destination the call destination
     * @param beginTime   the time when the call was initiated
     * @param duration    the duration of the call, in milliseconds
     */
    public void addCallData(@NotNull String destination, @NotNull Date beginTime, long duration) {
        Date endTime = new Date(beginTime.getTime() + duration);
        CallTimeDataValue value = addValue(beginTime, endTime, destination);
        value.mergeCallTime(duration);
    }

    /**
     * Add data for a set of calls to the specified destination within the specified time interval.
     *
     * @param destination the call destination
     * @param beginTime   ...
     * @param endTime     ...
     * @param minimum     ...
     * @param maximum     ...
     * @param total       ...
     * @param count       ...
     */
    public void addAggregatedCallData(@NotNull String destination, @NotNull Date beginTime, @NotNull Date endTime,
        double minimum, double maximum, double total, long count) {
        if (count == 0) {
            throw new IllegalArgumentException(
                "Plugin attempted to add aggregated call data with a call count of 0 for destination '" + destination
                    + "' - data will not be added.");
        }

        CallTimeDataValue value = addValue(beginTime, endTime, destination);
        value.setMinimum(minimum);
        value.setMaximum(maximum);
        value.setTotal(total);
        value.setCount(count);
    }

    public int getScheduleId() {
        return this.scheduleId;
    }

    /**
     * Returns a map that maps call destinations to the associated call-time data.
     *
     * @return a map that maps call destinations to the associated call-time data
     */
    @NotNull
    public Map<String, CallTimeDataValue> getValues() {
        // Make the Map unmodifiable to prevent the plugin developer from being able to add to it directly.
        return Collections.unmodifiableMap(this.values);
    }

    private CallTimeDataValue addValue(Date beginTime, Date endTime, String destination) {
        if (beginTime.after(endTime)) {
            throw new IllegalArgumentException("Begin time (" + beginTime + " [" + beginTime.getTime()
                + "]) is after end time (" + endTime + " [" + endTime.getTime() + "]).");
        }

        if (destination.length() > CallTimeDataKey.DESTINATION_MAX_LENGTH) {
            throw new IllegalArgumentException("Call destination is longer than the maximum length ("
                + CallTimeDataKey.DESTINATION_MAX_LENGTH + " characters) - please modify your response time transform "
                + "to generate smaller URLs");
        }

        CallTimeDataValue value = this.values.get(destination);
        if (value == null) {
            value = new CallTimeDataValue(beginTime, endTime);
            this.values.put(destination, value);
        }

        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if ((o == null) || (!(o instanceof CallTimeData))) {
            return false;
        }

        final CallTimeData other = (CallTimeData) o;
        return (this.scheduleId == other.scheduleId);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.scheduleId;

        return result;
    }

}
