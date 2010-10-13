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

import org.rhq.core.domain.measurement.MeasurementUnits;

public class MeasurementNumericValueAndUnits implements MeasurementValueAndUnits {
    /*
     * if there is ever an error retrieving the data to be passed into the constructor for this method we don't want to
     * immediate break or toss exceptions back up to the user.  instead, we can provide a consistent error handling
     * schema by passing zero data without units back up to the user.  by defining this public static field here, all
     * pages that need to display measurement errors can use the same mechanism.
     */
    public static final MeasurementNumericValueAndUnits ERROR = new MeasurementNumericValueAndUnits(0.0,
        MeasurementUnits.NONE);

    private final Double value;
    private final MeasurementUnits units;

    public MeasurementNumericValueAndUnits(Double value, MeasurementUnits units) {
        super();
        this.value = value;
        this.units = units;
    }

    public Double getValue() {
        return value;
    }

    public MeasurementUnits getUnits() {
        return units;
    }

    public String toString() {
        return value + " " + units;
    }
}