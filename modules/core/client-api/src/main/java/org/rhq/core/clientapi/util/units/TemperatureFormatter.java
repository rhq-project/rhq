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

import java.util.Locale;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.NumberFormat;

/**
 * @author Greg Hinkle
 */
public class TemperatureFormatter implements Formatter {

    public FormattedNumber format(UnitNumber val, Locale locale, FormatSpecifics format) {

        BigDecimal celsius = convert(val.getBaseValue(), val.getScale());

        return new FormattedNumber(getSpecificFormatter().format(celsius.doubleValue()), "C");
    }

    private BigDecimal convert(BigDecimal val, ScaleConstants scale) {
        BigDecimal celsius = null;
        switch (scale) {
            case SCALE_KELVIN:
                celsius = val.subtract(new BigDecimal(273.15d));
                break;
            case SCALE_CELSIUS:
                celsius = val;
                break;
            case SCALE_FAHRENHEIT:
                celsius = val.subtract(new BigDecimal(32)).divide(new BigDecimal(5).divide(new BigDecimal(9)));
                break;
        }
        return celsius;
    }

    public FormattedNumber[] formatSame(double[] values, UnitsConstants unitType, ScaleConstants scale, Locale locale, FormatSpecifics format) {
        FormattedNumber[] numbers = new FormattedNumber[values.length];

        int i=0;
        for (double val : values) {
            numbers[i] = format(new UnitNumber(values[i], unitType), locale, format);
            i++;
        }
        return numbers;
    }

    public BigDecimal getBaseValue(double value, ScaleConstants scale) {
        return new BigDecimal(value);
    }

    public BigDecimal getScaledValue(BigDecimal value, ScaleConstants targScale) {
        return convert(value, targScale);
    }

    public UnitNumber parse(String val, Locale locale, ParseSpecifics specifics) throws ParseException {
        return null;
    }


    private NumberFormat getSpecificFormatter() {
        NumberFormat res = NumberFormat.getInstance();


        res.setMaximumFractionDigits(1);
        res.setMinimumFractionDigits(1);


        return res;
    }

}
