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

import java.io.Serializable;

public class MeasurementStringValueAndUnits implements MeasurementValueAndUnits, Serializable {

    private static final long serialVersionUID = 1L;

    private final String value;
    private final MeasurementUnits units;

    public MeasurementStringValueAndUnits(String value, MeasurementUnits units) {
        super();
        this.value = value;
        this.units = units;
    }

    public String getValue() {
        return value;
    }

    public MeasurementUnits getUnits() {
        return units;
    }

    @Override
    public String toString() {
        return MeasurementConverter.format(getValue(), getUnits());
    }
}