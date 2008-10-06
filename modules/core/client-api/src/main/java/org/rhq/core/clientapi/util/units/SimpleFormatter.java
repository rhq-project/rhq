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

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

public abstract class SimpleFormatter implements Formatter {
    protected abstract UnitsConstants getUnitType();

    protected abstract ScaleConstants getUnitScale();

    public FormattedNumber format(UnitNumber val, Locale locale, FormatSpecifics specifics) {
        NumberFormat fmt;

        fmt = this.getNumberFormat(locale);
        return this.formatNumber(val.getValue(), fmt);
    }

    public FormattedNumber[] formatSame(double[] vals, UnitsConstants unitType, ScaleConstants scale, Locale locale,
        FormatSpecifics specifics) {
        FormattedNumber[] res;
        NumberFormat fmt;

        if (unitType != this.getUnitType()) {
            throw new IllegalArgumentException("Invalid unit specified");
        }

        if (scale != this.getUnitScale()) {
            throw new IllegalArgumentException("Invalid scale specified");
        }

        fmt = UnitsUtil.getNumberFormat(vals, locale);
        res = new FormattedNumber[vals.length];

        for (int i = 0; i < vals.length; i++) {
            res[i] = this.formatNumber(vals[i], fmt);
        }

        return res;
    }

    protected abstract FormattedNumber formatNumber(double rawValue, NumberFormat fmt);

    protected NumberFormat getNumberFormat(Locale locale) {
        NumberFormat res = NumberFormat.getInstance(locale);

        res.setMinimumFractionDigits(1);
        res.setMaximumFractionDigits(1);
        return res;
    }

    public BigDecimal getBaseValue(double value, ScaleConstants scale) {
        if (scale != this.getUnitScale()) {
            throw new IllegalArgumentException("Invalid scale specified");
        }

        return new BigDecimal(value);
    }

    public BigDecimal getScaledValue(BigDecimal value, ScaleConstants targScale) {
        if (targScale != this.getUnitScale()) {
            throw new IllegalArgumentException("Invalid scale specified");
        }

        return value;
    }

    public UnitNumber parse(String val, Locale locale, ParseSpecifics specifics) throws ParseException {
        NumberFormat fmt;

        fmt = NumberFormat.getInstance(locale);
        return new UnitNumber(fmt.parse(val).doubleValue(), this.getUnitType(), this.getUnitScale());
    }
}