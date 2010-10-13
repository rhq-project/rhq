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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * Subclass for numerical measurement data
 *
 * @author Greg Hinkle
 */
@Entity
@NamedQueries( { @NamedQuery(name = MeasurementDataNumeric1H.GET_MAX_TIMESTAMP, query = "SELECT max(nmd.id.timestamp) FROM MeasurementDataNumeric1H nmd") })
@Table(name = "RHQ_MEASUREMENT_DATA_NUM_1H")
public class MeasurementDataNumeric1H extends MeasurementData implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String GET_MAX_TIMESTAMP = "MeasurementDataNumeric1H.getMaxTimestamp";

    @Column(precision = 24, scale = 3)
    private Double value;

    @Column(name = "minvalue")
    private Double min;
    @Column(name = "maxvalue")
    private Double max;

    protected MeasurementDataNumeric1H() {
        // for JPA
    }

    @Override
    public Object getValue() {
        return this.value;
    }

    public Double getMin() {
        return min;
    }

    public Double getMax() {
        return max;
    }

    @Override
    public String toString() {
        return "MeasurementDataNumeric1H[" + "average=[" + value + "], " + super.toString() + "]";
    }
}