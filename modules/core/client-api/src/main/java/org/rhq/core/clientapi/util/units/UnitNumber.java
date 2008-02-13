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
package org.rhq.core.clientapi.util.units;

import java.math.BigDecimal;

public class UnitNumber {
    private double value;
    private UnitsConstants units;
    private ScaleConstants scale;

    public UnitNumber(double value, UnitsConstants units) {
        this(value, units, ScaleConstants.SCALE_NONE);
    }

    public UnitNumber(double value, UnitsConstants units, ScaleConstants scale) {
        this.value = value;
        this.units = units;
        this.scale = scale;

        UnitsUtil.checkValidScaleForUnits(units, scale);
    }

    public double getValue() {
        return this.value;
    }

    public UnitsConstants getUnits() {
        return this.units;
    }

    public ScaleConstants getScale() {
        return this.scale;
    }

    public BigDecimal getBaseValue() {
        return UnitsFormat.getBaseValue(this.value, this.units, this.scale);
    }

    public BigDecimal getScaledValue(ScaleConstants targScale) {
        return UnitsFormat.getScaledValue(this.getBaseValue(), this.units, targScale);
    }

    public String toString() {
        return Double.toString(value);
    }
}