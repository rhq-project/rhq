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
import java.text.ParsePosition;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import org.rhq.core.clientapi.util.ArrayUtil;
import static org.rhq.core.clientapi.util.units.ScaleConstants.SCALE_NONE;
import static org.rhq.core.clientapi.util.units.UnitsConstants.UNIT_BITS;
import static org.rhq.core.clientapi.util.units.UnitsConstants.UNIT_BYTES;
import static org.rhq.core.clientapi.util.units.UnitsConstants.UNIT_DATE;
import static org.rhq.core.clientapi.util.units.UnitsConstants.UNIT_DURATION;
import static org.rhq.core.clientapi.util.units.UnitsConstants.UNIT_NONE;
import static org.rhq.core.clientapi.util.units.UnitsConstants.UNIT_PERCENTAGE;

class UnitsUtil {
    static final BigDecimal FACT_NONE = new BigDecimal(1);

    // Binary scaling factors
    static final BigDecimal FACT_KILO_BIN = new BigDecimal(1 << 10);
    static final BigDecimal FACT_MEGA_BIN = FACT_KILO_BIN.multiply(FACT_KILO_BIN);
    static final BigDecimal FACT_GIGA_BIN = FACT_MEGA_BIN.multiply(FACT_KILO_BIN);
    static final BigDecimal FACT_TERA_BIN = FACT_GIGA_BIN.multiply(FACT_KILO_BIN);
    static final BigDecimal FACT_PETA_BIN = FACT_TERA_BIN.multiply(FACT_KILO_BIN);

    // The following factors are how to convert a number in FACT_* to
    // nanoseconds.
    static final BigDecimal FACT_NANOS = new BigDecimal(1);
    static final BigDecimal FACT_MICROS = new BigDecimal(1000);
    static final BigDecimal FACT_MILLIS = new BigDecimal(1000000);
    static final BigDecimal FACT_JIFFYS = new BigDecimal(10000000);
    static final BigDecimal FACT_SECS = FACT_MILLIS.multiply(new BigDecimal(1000));
    static final BigDecimal FACT_MINS = FACT_SECS.multiply(new BigDecimal(60));
    static final BigDecimal FACT_HOURS = FACT_MINS.multiply(new BigDecimal(60));
    static final BigDecimal FACT_DAYS = FACT_HOURS.multiply(new BigDecimal(24));
    static final BigDecimal FACT_WEEKS = FACT_DAYS.multiply(new BigDecimal(7));
    static final BigDecimal FACT_YEARS = FACT_DAYS.multiply(new BigDecimal(365));

    static void checkValidScaleForUnits(UnitsConstants unit, ScaleConstants scale) {
        EnumSet<ScaleConstants> weightSet = ScaleConstants.getBinaryScaleSet();
        EnumSet<ScaleConstants> timeSet = ScaleConstants.getTimeSet();

        //       if(!(scale == SCALE_NONE ||
        //                ((unit == UNIT_BYTES || unit == UNIT_BITS) &&
        //                 scale >= SCALE_KILO && scale <= SCALE_PETA)
        //              ||
        //                ((unit == UNIT_DURATION || unit == UNIT_DATE) &&
        //                 scale >= SCALE_YEAR && scale <= SCALE_NANO)
        //              ||
        //                (unit == UNIT_PERCENTAGE && scale != SCALE_NONE) ||
        //                (unit == UNIT_NONE && scale != SCALE_NONE)))

        if (!((scale == SCALE_NONE) || (((unit == UNIT_BYTES) || (unit == UNIT_BITS)) && weightSet.contains(scale))
            || (((unit == UNIT_DURATION) || (unit == UNIT_DATE)) && timeSet.contains(scale))
            || ((unit == UNIT_PERCENTAGE) && (scale != SCALE_NONE)) || ((unit == UNIT_NONE) && (scale != SCALE_NONE)))) {
            throw new IllegalArgumentException("Scale is not valid for the " + "specified units");
        }
    }

    /**
     * Find the index of the first character which is not a number.
     *
     * @return The index of the first character which is not a number. If the entire value is a number, then -1 is
     *         returned
     */
    static int findNonNumberIdx(String val, NumberFormat fmt) {
        ParsePosition pos = new ParsePosition(0);

        fmt.parse(val, pos);
        if (pos.getIndex() == val.length()) {
            return -1;
        }

        return pos.getIndex();
    }

    /**
     * Get a NumberFormat which shows enough significant figures, such that each value passed in is distinguishable from
     * the next. (if they are not the same value)
     */
    public static NumberFormat getNumberFormat(double[] inValues, Locale locale) {
        NumberFormat res;
        String[] sValues;
        double[] values;
        int numDigits;

        res = NumberFormat.getInstance(locale);
        values = new double[inValues.length];
        System.arraycopy(inValues, 0, values, 0, inValues.length);
        Arrays.sort(values);
        values = ArrayUtil.uniq(values);

        if (values.length < 2) {
            res.setMinimumFractionDigits(1);
            res.setMaximumFractionDigits(1);
            return res;
        }

        /**
         * Find a NumberFormat, such that the numbers can all be formatted
         * uniquely.  We have to do this iteratively, checking all the
         * values, since the NumberFormat rounding can cause multiple
         * unique values to the same one.  This kinda sux, since it's
         * slow, but not much we can do here.
         */
        numDigits = 0;
        sValues = new String[values.length];
        while (numDigits < 9) {
            res.setMinimumFractionDigits(numDigits);
            res.setMaximumFractionDigits(numDigits);

            for (int i = 0; i < sValues.length; i++) {
                sValues[i] = res.format(values[i]);
            }

            Arrays.sort(sValues);
            if (ArrayUtil.isUniq(sValues) == false) {
                numDigits++;
            } else {
                break;
            }
        }

        return res;
    }

    public static int getUniqueDigits(double[] inValues, Locale locale) {
        return getNumberFormat(inValues, locale).getMaximumFractionDigits();
    }
}