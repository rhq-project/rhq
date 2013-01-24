/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License, version 2, as
 *  * published by the Free Software Foundation, and/or the GNU Lesser
 *  * General Public License, version 2.1, also as published by the Free
 *  * Software Foundation.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License and the GNU Lesser General Public License
 *  * for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * and the GNU Lesser General Public License along with this program;
 *  * if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package org.rhq.enterprise.server.measurement;

import org.rhq.core.domain.measurement.MeasurementOOB;

/**
 * This is used for verifying results since the properties in {@link MeasurementOOB} are
 * read only making harder to set up expected values.
 *
* @author John Sanda
*/
class TestMeasurementOOB {
    // TestMeasurementOOB is used to verify results since MeasurementOOB does not expose
    // setters for its properties.
    private int scheduleId;
    private long timestamp;
    private int oobFactor;

    public TestMeasurementOOB(int scheduleId, long timestamp, int oobFactor) {
        this.scheduleId = scheduleId;
        this.timestamp = timestamp;
        this.oobFactor = oobFactor;
    }

    public TestMeasurementOOB(MeasurementOOB oob) {
        scheduleId = oob.getScheduleId();
        timestamp = oob.getTimestamp();
        oobFactor = oob.getOobFactor();
    }

    public int getScheduleId() {
        return scheduleId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getOobFactor() {
        return oobFactor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TestMeasurementOOB that = (TestMeasurementOOB) o;

        if (oobFactor != that.oobFactor) return false;
        if (scheduleId != that.scheduleId) return false;
        if (timestamp != that.timestamp) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = scheduleId;
        result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
        result = 31 * result + oobFactor;
        return result;
    }

    @Override
    public String toString() {
        return "TestMeasurementOOB[" +
            "scheduleId=" + scheduleId +
            ", timestamp=" + timestamp +
            ", oobFactor=" + oobFactor +
            "]";
    }
}
