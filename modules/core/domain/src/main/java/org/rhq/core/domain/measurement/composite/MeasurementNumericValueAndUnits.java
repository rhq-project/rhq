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

import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.util.MeasurementConverter;

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

    @Override
    public String toString() {
        return MeasurementConverter.format(value, units, true);
    }
}