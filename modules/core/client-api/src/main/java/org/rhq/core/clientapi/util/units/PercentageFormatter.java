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
package org.rhq.core.clientapi.util.units;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

public class PercentageFormatter extends SimpleFormatter {
    protected UnitsConstants getUnitType() {
        return UnitsConstants.UNIT_PERCENTAGE;
    }

    protected ScaleConstants getUnitScale() {
        return ScaleConstants.SCALE_NONE;
    }

    protected FormattedNumber formatNumber(double rawValue, NumberFormat fmt) {
        return new FormattedNumber(fmt.format(rawValue * 100.0), "%", "");
    }

    public UnitNumber parse(String val, Locale locale, ParseSpecifics specifics) throws ParseException {
        NumberFormat fmt;

        fmt = NumberFormat.getPercentInstance(locale);
        return new UnitNumber(fmt.parse(val).doubleValue(), this.getUnitType(), this.getUnitScale());
    }

    public FormattedNumber[] formatSame(double[] vals, UnitsConstants unitType, ScaleConstants scale, Locale locale,
        FormatSpecifics specifics) {
        FormattedNumber[] res;
        NumberFormat fmt;

        // TODO: refactor to rm duplication of validation
        if (unitType != this.getUnitType()) {
            throw new IllegalArgumentException("Invalid unit specified");
        }

        if (scale != this.getUnitScale()) {
            throw new IllegalArgumentException("Invalid scale specified");
        }

        fmt = NumberFormat.getInstance(locale);
        fmt.setMaximumFractionDigits(2);
        res = new FormattedNumber[vals.length];
        for (int i = 0; i < vals.length; i++) {
            res[i] = this.formatNumber(vals[i], fmt);
        }

        return res;
    }
}