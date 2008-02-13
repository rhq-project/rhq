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

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

/**
 * Represents data that was collected either due to a schedule or an on-demand, live collection.
 */
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class MeasurementData implements Serializable {

    @EmbeddedId
    MeasurementDataPK id;

    @JoinColumn(name = "SCHEDULE_ID", insertable = false, updatable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    MeasurementSchedule schedule;

    @Transient
    private String name;

    /**
     * Use this constructor if the data was collected on-demand and not part of a scheduled collection.
     */
    protected MeasurementData() {
        this(new MeasurementDataPK(System.currentTimeMillis(), 0));
    }

    protected MeasurementData(MeasurementDataPK pk) {
        if (pk == null) {
            throw new NullPointerException("pk==null");
        }

        id = pk;
    }

    protected MeasurementData(long timestamp, MeasurementScheduleRequest request) {
        this(new MeasurementDataPK(timestamp, request.getScheduleId()));
        this.setName(request.getName());
    }

    protected MeasurementData(MeasurementScheduleRequest request) {
        this(new MeasurementDataPK(System.currentTimeMillis(), request.getScheduleId()));
        this.setName(request.getName());
    }

    public int getScheduleId() {
        return id.scheduleId;
    }

    public long getTimestamp() {
        return id.timestamp;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public abstract Object getValue();

    @Override
    public String toString() {
        return "MeasurementData [" + id + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if ((o == null) || (!(o instanceof MeasurementData))) {
            return false;
        }

        MeasurementData that = (MeasurementData) o;

        if (!id.equals(that.id)) {
            return false;
        }

        if ((name != null) ? (!name.equals(that.name)) : (that.name != null)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = id.hashCode();
        result = (31 * result) + ((name != null) ? name.hashCode() : 0);
        return result;
    }
}