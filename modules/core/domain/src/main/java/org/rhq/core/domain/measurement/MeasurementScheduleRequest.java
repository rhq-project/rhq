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
package org.rhq.core.domain.measurement;

import java.io.Serializable;

/**
 * This class is a stripped down version of the {@link MeasurementSchedule} from the domain project. It is used to send
 * between agent and server, so it does not need all fields.
 *
 * @author <a href="mailto:heiko.rupp@redhat.com">Heiko W. Rupp</a>
 * @see    org.rhq.core.domain.measurement.MeasurementSchedule
 */
public class MeasurementScheduleRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private int scheduleId;
    private String name;
    private long interval;
    private boolean enabled;
    private DataType dataType;
    private NumericType numericType;
    private boolean perMinute;

    public MeasurementScheduleRequest(MeasurementSchedule schedule) {
        this.scheduleId = schedule.getId();
        this.name = schedule.getDefinition().getName();
        this.interval = schedule.getInterval();
        this.enabled = schedule.isEnabled();
        this.dataType = schedule.getDefinition().getDataType();
        this.numericType = schedule.getDefinition().getNumericType();
        this.perMinute = schedule.getDefinition().isPerMinute();
    }

    public MeasurementScheduleRequest(int scheduleId, String name, long interval, boolean enabled, DataType dataType) {
        this(scheduleId, name, interval, enabled, dataType, NumericType.DYNAMIC, false);
    }

    public MeasurementScheduleRequest(int scheduleId, String name, long interval, boolean enabled, DataType dataType,
        NumericType numericType, boolean isPerMinute) {
        this.scheduleId = scheduleId;
        this.name = name;
        this.interval = interval;
        this.enabled = enabled;
        this.dataType = dataType;
        this.numericType = numericType;
        this.perMinute = isPerMinute;
    }

    public MeasurementScheduleRequest(MeasurementScheduleRequest scheduleRequest) {
        this.scheduleId = scheduleRequest.scheduleId;
        this.name = scheduleRequest.name;
        this.interval = scheduleRequest.interval;
        this.enabled = scheduleRequest.enabled;
        this.dataType = scheduleRequest.dataType;
        this.numericType = scheduleRequest.numericType;
        this.perMinute = scheduleRequest.perMinute;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(int scheduleId) {
        this.scheduleId = scheduleId;
    }

    public long getInterval() {
        return interval;
    }

    public void setInterval(long interval) {
        this.interval = interval;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public DataType getDataType() {
        return dataType;
    }

    public NumericType getNumericType() {
        return numericType;
    }

    public boolean isPerMinute() {
        return perMinute;
    }

    @Override
    public String toString() {
        return "MeasurementScheduleRequest[scheduleId=" + scheduleId + ", name=" + name + ", interval=" + interval
            + ", enabled=" + enabled + ", dataType=" + dataType + ", numericType=" + numericType + ", perMinute="
            + perMinute + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + scheduleId;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || !(obj instanceof MeasurementScheduleRequest)) {
            return false;
        }

        final MeasurementScheduleRequest other = (MeasurementScheduleRequest) obj;
        return (scheduleId == other.scheduleId);
    }
}