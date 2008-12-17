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
package org.rhq.core.domain.measurement;

import java.io.Serializable;

/**
 * This class is a stripped down version of the {@link MeasurementSchedule} from the domain project. It is used to send
 * between Agent and Server, so it does not need all fields.
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
    private NumericType rawNumericType;

    public MeasurementScheduleRequest(MeasurementSchedule schedule) {
        this.scheduleId = schedule.getId();
        this.name = schedule.getDefinition().getName();
        this.interval = schedule.getInterval();
        this.enabled = schedule.isEnabled();
        this.dataType = schedule.getDefinition().getDataType();
        this.rawNumericType = schedule.getDefinition().getRawNumericType();
    }

    public MeasurementScheduleRequest(int scheduleId, String name, long interval, boolean enabled, DataType dataType) {
        this(scheduleId, name, interval, enabled, dataType, null);
    }

    public MeasurementScheduleRequest(int scheduleId, String name, long interval, boolean enabled, DataType dataType,
                                      NumericType rawNumericType) {
        this.scheduleId = scheduleId;
        this.name = name;
        this.interval = interval;
        this.enabled = enabled;
        this.dataType = dataType;
        this.rawNumericType = rawNumericType;
    }

    public MeasurementScheduleRequest(MeasurementScheduleRequest scheduleRequest) {
        this.scheduleId = scheduleRequest.scheduleId;
        this.name = scheduleRequest.name;
        this.interval = scheduleRequest.interval;
        this.enabled = scheduleRequest.enabled;
        this.dataType = scheduleRequest.dataType;
        this.rawNumericType = scheduleRequest.rawNumericType;
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

    public boolean isPerMinute() {
        return rawNumericType != null;
    }

    public NumericType getRawNumericType() {
        return rawNumericType;
    }

    @Override
    public String toString() {
        return "MeasurementScheduleRequest[scheduleId=" + scheduleId + ", name=" + name + ", interval=" + interval
            + ", enabled=" + enabled + ", dataType=" + dataType + ", rawNumericType=" + rawNumericType + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
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
        if (scheduleId != other.scheduleId) {
            return false;
        }

        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        
        return true;
    }

}