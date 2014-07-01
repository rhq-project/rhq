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

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "RHQ_MEASUREMENT_DATA_TRAIT")
public class MeasurementDataTrait extends MeasurementData {

    private static final long serialVersionUID = 1L;

    private String value;

    /**
     * Create a new trait object with the current system time for the timestamp.
     *
     * @param request basically the {@link MeasurementSchedule} for this trait
     * @param value   the metric value of this trait
     */
    public MeasurementDataTrait(MeasurementScheduleRequest request, String value) {
        super(request);
        this.value = value;
    }

    /**
     * Create a new Trait object.
     *
     * @param timestamp time when the measurement was taken
     * @param request   basically the {@link MeasurementSchedule} for this trait
     * @param value     the metric value of this trait
     */
    public MeasurementDataTrait(long timestamp, MeasurementScheduleRequest request, String value) {
        super(timestamp, request);
        this.value = value;
    }

    /**
     * Create a new Trait object. MeasurementSchedule and timestamp are given in the passed pk.
     *
     * @param pk    primary key
     * @param value the measurement value for this trait
     */
    public MeasurementDataTrait(MeasurementDataPK pk, String value) {
        super(pk);
        this.value = value;
    }

    protected MeasurementDataTrait() {
        /* JPA use only */
    }

    /**
     * Sets the schedule this trait is derived from.
     */
    public void setSchedule(MeasurementSchedule schedule) {
        this.schedule = schedule;
    }

    @Override
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "MeasurementDataTrait[name=" + getName() + ", value=\"" + this.value + "\", scheduleId="
                + this.id.scheduleId + ", timestamp=" + this.id.timestamp + "]";
    }

}