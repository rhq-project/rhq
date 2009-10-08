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
package org.rhq.core.domain.measurement.composite;

import java.io.Serializable;

public class MeasurementDataNumericHighLowComposite implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final double THRESHOLD = 0.00001d;

    private long timestamp;
    private double value;
    private double highValue;
    private double lowValue;

    protected MeasurementDataNumericHighLowComposite() {
        // for JPA
    }

    public MeasurementDataNumericHighLowComposite(long timestamp, double value, double highValue, double lowValue) {
        if (!Double.isNaN(value)) {
            if (highValue < value && Math.abs(highValue - value) > THRESHOLD) {
                throw new IllegalArgumentException("highValue (" + highValue
                    + ") is not greater than or equal to value (" + value + ").");
            }

            if (lowValue > value && Math.abs(lowValue - value) > THRESHOLD) {
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