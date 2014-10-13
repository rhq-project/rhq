/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * Simple Java Bean to hold aggregate values.
 *
 * @author Heiko W. Rupp
 */
public class MeasurementAggregate implements Serializable {

    static final long serialVersionUID = 5673395371271765240L;

    /**
     * The minimum value of the values that are used to compute this aggregate
     */
    Double min;
    /**
     * The average value of the values that are used to compute this aggregate
     */
    Double avg;
    /**
     * The maximum value of the values that are used to compute this aggregate
     */
    Double max;

    public MeasurementAggregate() {
    }

    /**
     * Create a new aggregate from the provided values. If one of the values is null,
     * it is set to Double.NaN
     * @param min Minimum value of the aggregate
     * @param avg Average value of the aggregate
     * @param max Maximum value of the aggregate
     */
    public MeasurementAggregate(Double min, Double avg, Double max) {
        this.min = (min != null) ? min : Double.NaN;
        this.avg = (avg != null) ? avg : Double.NaN;
        this.max = (max != null) ? max : Double.NaN;
    }

    public Double getMin() {
        return min;
    }

    public void setMin(Double min) {
        this.min = min;
    }

    public Double getAvg() {
        return avg;
    }

    public void setAvg(Double avg) {
        this.avg = avg;
    }

    public Double getMax() {
        return max;
    }

    public void setMax(Double max) {
        this.max = max;
    }

    @Override
    public String toString() {
        return "Min: " + min + ", Max: " + max + ", Avg: " + avg;
    }

    /**
     * @return true if the aggregate has "no real data" I.e. when all values are Not A Number.
     */
    public boolean isEmpty() {
        return min.isNaN() && avg.isNaN() && max.isNaN();
    }
}