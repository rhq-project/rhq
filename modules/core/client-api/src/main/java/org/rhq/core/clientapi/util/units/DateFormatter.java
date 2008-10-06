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
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;
import org.rhq.core.clientapi.util.TimeUtil;

public class DateFormatter implements Formatter {
    public static class DateSpecifics extends FormatSpecifics {
        private DateFormat df;

        public void setDateFormat(DateFormat df) {
            this.df = df;
        }

        public DateFormat getDateFormat() {
            return this.df;
        }
    }

    public FormattedNumber format(UnitNumber val, Locale locale) {
        DateSpecifics specifics = new DateSpecifics();

        specifics.setDateFormat(this.getDefaultFormat(locale));
        return this.format(val, locale, specifics);
    }

    public FormattedNumber format(UnitNumber val, Locale locale, FormatSpecifics specifics) {
        BigDecimal dec;
        DateFormat df;

        // We need a value in the milliseconds range
        dec = val.getBaseValue().divide(UnitsUtil.FACT_MILLIS, BigDecimal.ROUND_HALF_EVEN);
        if (specifics == null) {
            df = this.getDefaultFormat(locale);
        } else {
            df = ((DateSpecifics) specifics).getDateFormat();
        }

        return new FormattedNumber(df.format(new Date(dec.longValue())), "");
    }

    public FormattedNumber[] formatSame(double[] val, UnitsConstants unitType, ScaleConstants scale, Locale locale) {
        FormattedNumber[] res;

        res = new FormattedNumber[val.length];

        for (int i = 0; i < val.length; i++) {
            res[i] = this.format(new UnitNumber(val[i], unitType, scale), locale);
        }

        return res;
    }

    public FormattedNumber[] formatSame(double[] val, UnitsConstants unitType, ScaleConstants scale, Locale locale,
        FormatSpecifics specifics) {
        FormattedNumber[] res;

        res = new FormattedNumber[val.length];

        for (int i = 0; i < val.length; i++) {
            res[i] = this.format(new UnitNumber(val[i], unitType, scale), locale, specifics);
        }

        return res;
    }

    private DateFormat getDefaultFormat(Locale locale) {
        return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, locale);
    }

    public BigDecimal getBaseValue(double value, ScaleConstants scale) {
        return DateFormatter.getBaseTime(value, scale);
    }

    public BigDecimal getScaledValue(BigDecimal value, ScaleConstants targScale) {
        return DateFormatter.getScaledTime(value, targScale);
    }

    public static BigDecimal getScaledTime(BigDecimal value, ScaleConstants targScale) {
        return value.divide(getScaleCoeff(targScale), BigDecimal.ROUND_HALF_EVEN);
    }

    public static BigDecimal getBaseTime(double value, ScaleConstants scale) {
        BigDecimal res;

        res = new BigDecimal(value);
        return res.multiply(getScaleCoeff(scale));
    }

    private static BigDecimal getScaleCoeff(ScaleConstants scale) {
        switch (scale) {
        case SCALE_NONE: {
            return UnitsUtil.FACT_NONE;
        }

        case SCALE_NANO: {
            return UnitsUtil.FACT_NANOS;
        }

        case SCALE_MICRO: {
            return UnitsUtil.FACT_MICROS;
        }

        case SCALE_MILLI: {
            return UnitsUtil.FACT_MILLIS;
        }

        case SCALE_JIFFY: {
            return UnitsUtil.FACT_JIFFYS;
        }

        case SCALE_SEC: {
            return UnitsUtil.FACT_SECS;
        }

        case SCALE_MIN: {
            return UnitsUtil.FACT_MINS;
        }

        case SCALE_HOUR: {
            return UnitsUtil.FACT_HOURS;
        }

        case SCALE_DAY: {
            return UnitsUtil.FACT_DAYS;
        }

        case SCALE_WEEK: {
            return UnitsUtil.FACT_WEEKS;
        }

        case SCALE_YEAR: {
            return UnitsUtil.FACT_YEARS;
        }
        }

        throw new IllegalArgumentException("Value did not have time " + "based scale");
    }

    public UnitNumber parse(String val, Locale locale, ParseSpecifics specifics) throws ParseException {
        long curTime = System.currentTimeMillis();

        return new UnitNumber(TimeUtil.parseComplexTime(val, curTime, false), UnitsConstants.UNIT_DATE,
            ScaleConstants.SCALE_MILLI);
    }
}