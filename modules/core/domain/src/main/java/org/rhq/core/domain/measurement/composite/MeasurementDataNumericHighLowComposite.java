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
package org.rhq.core.domain.measurement.composite;

import java.io.Serializable;

public class MeasurementDataNumericHighLowComposite implements Serializable {
    private static final long serialVersionUID = 1L;

    private long timestamp;
    private double value;
    private double highValue;
    private double lowValue;

    protected MeasurementDataNumericHighLowComposite() {
        // for JPA
    }

    public MeasurementDataNumericHighLowComposite(long timestamp, double value, double highValue, double lowValue) {
        if (!Double.isNaN(value)) {
            if (!(highValue >= value)) {
                throw new IllegalArgumentException("highValue (" + highValue
                    + ") is not greater than or equal to value (" + value + ").");
            }

            if (!(lowValue <= value)) {
                throw new IllegalArgumentException("lowValue (" + lowValue + ") is not less than or equal to value ("
                    + value + ").");
            }
        }

        this.timestamp = timestamp;
        this.value = value;
        this.highValue = highValue;
        this.lowValue = lowValue;
    }

    public double getHighValue() {
        return this.highValue;
    }

    public double getLowValue() {
        return this.lowValue;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public double getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return "MeasurementDataNumericHighLowComposite[" + "value=" + this.value + ", " + "lowValue=" + this.lowValue
            + ", " + "highValue=" + this.highValue + "]";
    }
}