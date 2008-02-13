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
import java.text.DateFormat;
import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class MeasurementDataPK implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "TIME_STAMP")
    long timestamp;

    @Column(name = "SCHEDULE_ID")
    int scheduleId;

    protected MeasurementDataPK() {
        /* JPA use only */
    }

    public MeasurementDataPK(int scheduleId) {
        this.timestamp = System.currentTimeMillis();
        this.scheduleId = scheduleId;
    }

    public MeasurementDataPK(long timestamp, int scheduleId) {
        this.timestamp = timestamp;
        this.scheduleId = scheduleId;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = (PRIME * result) + scheduleId;
        result = (PRIME * result) + (int) (timestamp ^ (timestamp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (!(obj instanceof MeasurementDataPK)) {
            return false;
        }

        final MeasurementDataPK other = (MeasurementDataPK) obj;
        if (scheduleId != other.scheduleId) {
            return false;
        }

        if (timestamp != other.timestamp) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return "MeasurementDataPK: " + "timestamp=[" + DateFormat.getInstance().format(timestamp) + "], scheduleId=["
            + scheduleId + "]";
    }
}