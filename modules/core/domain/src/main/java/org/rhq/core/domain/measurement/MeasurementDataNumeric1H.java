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
import java.text.DecimalFormat;
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
@NamedQueries( { @NamedQuery(name = MeasurementDataNumeric1H.GET_NUM_AGGREGATE_MULTI, query = "SELECT min(nmd.min), avg(nmd.value), max(nmd.max),sum(nmd.value) " // TODO does sum() make any sense ?
    + "FROM MeasurementDataNumeric1H nmd "
    + "WHERE nmd.schedule IN (:schedules) AND nmd.id.timestamp BETWEEN :start AND :end") })
@Table(name = "RHQ_MEASUREMENT_DATA_NUM_1H")
public class MeasurementDataNumeric1H extends MeasurementData implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String GET_NUM_AGGREGATE_MULTI = "MeasurementDataNumeric1H.getNumAggregateMulti";

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
        return "MeasurementDataNumeric1H[" + "average=[" + new DecimalFormat("0.00").format(value) + "], "
            + super.toString() + "]";
    }
}