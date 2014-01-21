/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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

    /**
     * This constant can be used with requests to obtain live metric data. A valid schedule
     * id is only needed for live metric data when then metric is TRENDSUP or TRENDSDOWN
     */
    public static final int NO_SCHEDULE_ID = 1;

    private final int scheduleId;
    private final String name;
    private int interval;
    private boolean enabled;
    byte dataNumType;

    public MeasurementScheduleRequest(MeasurementSchedule schedule) {
        this(schedule.getId(),schedule.getDefinition().getName(),schedule.getInterval(),
            schedule.isEnabled(),schedule.getDefinition().getDataType(),schedule.getDefinition().getRawNumericType());
    }

    public MeasurementScheduleRequest(int scheduleId, String name, long interval, boolean enabled, DataType dataType) {
        this(scheduleId, name, interval, enabled, dataType, null);
    }

    public MeasurementScheduleRequest(MeasurementScheduleRequest scheduleRequest) {
        this(scheduleRequest.getScheduleId(),scheduleRequest.getName(),scheduleRequest.getInterval(),
            scheduleRequest.isEnabled(),scheduleRequest.getDataType(),
            scheduleRequest.getRawNumericType());
    }

    public MeasurementScheduleRequest(int scheduleId, String name, long interval, boolean enabled, DataType dataType,
                                      NumericType rawNumericType) {
        this.scheduleId = scheduleId;
        if (name!=null) {
            this.name = name.intern();
    }
        else
            this.name = null;
        this.interval = (int) (interval/1000);
        this.enabled = enabled;
        this.dataNumType = toDataNumType(dataType,rawNumericType);
    }


    public String getName() {
        return name;
    }

    public int getScheduleId() {
        return scheduleId;
    }

    public long getInterval() {
        return interval*1000L;
    }

    public void setInterval(long interval) {
        this.interval = (int) (interval/1000);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public DataType getDataType() {
        return DataType.values()[dataNumType/16 -1];
    }

    public NumericType getRawNumericType() {
        byte tmp = (byte) (dataNumType & 0x0f);
        if (tmp==0)
            return null;
        return NumericType.values()[tmp-1];
//        return rawNumericType;
    }

    @Override
    public String toString() {
        return "MeasurementScheduleRequest[scheduleId=" + scheduleId + ", name=" + name + ", interval=" + interval*1000L
            + ", enabled=" + enabled + /*", dataType=" + dataType + ", rawNumericType=" + rawNumericType +*/ "]";
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

    private byte toDataNumType(DataType dataType, NumericType numericType) {
        byte dTmp = (byte) (dataType != null ? dataType.ordinal()+1 : 0);
        byte nTmp = (byte) (numericType != null ? numericType.ordinal()+1 : 0);
        return (byte) (dTmp * 16 + nTmp);
    }
}