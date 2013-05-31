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
 *
 */

package org.rhq.server.metrics.domain;

/**
 * @author Stefan Negrea
 *
 */
public class AggregateSimpleNumericMetric {

    private Double value;
    private AggregateType type;
    private int scheduleId;

    /**
     *
     */
    public AggregateSimpleNumericMetric() {
    }

    /**
     * @param value
     * @param type
     */
    public AggregateSimpleNumericMetric(int scheduleId, Double value, AggregateType type) {
        this.value = value;
        this.type = type;
        this.setScheduleId(scheduleId);
    }

    /**
     * @return the value
     */
    public Double getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(Double value) {
        this.value = value;
    }

    /**
     * @return the type
     */
    public AggregateType getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(AggregateType type) {
        this.type = type;
    }

    /**
     * @return the scheduleId
     */
    public int getScheduleId() {
        return scheduleId;
    }

    /**
     * @param scheduleId the scheduleId to set
     */
    public void setScheduleId(int scheduleId) {
        this.scheduleId = scheduleId;
    }

}
