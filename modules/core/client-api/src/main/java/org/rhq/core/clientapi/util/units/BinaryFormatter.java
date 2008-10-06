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
import org.rhq.core.clientapi.util.ArrayUtil;

public abstract class BinaryFormatter implements Formatter {
    private NumberFormat getSpecificFormatter(FormatSpecifics specifics, Locale locale) {
        NumberFormat res = NumberFormat.getInstance(locale);

        if (specifics.getPrecision() == FormatSpecifics.PRECISION_MAX) {
            res.setMaximumFractionDigits(100);
            res.setMinimumFractionDigits(0);
        } else {
            res.setMaximumFractionDigits(1);
            res.setMinimumFractionDigits(1);
        }

        return res;
    }

    public FormattedNumber format(UnitNumber val, Locale locale, FormatSpecifics specifics) {
        NumberFormat fmt;
        BigDecimal baseVal;
        double newVal;
        ScaleConstants targScale;

        baseVal = val.getBaseValue();
        targScale = this.findGoodLookingScale(baseVal);
        newVal = this.getTargetValue(baseVal, targScale);

        if (specifics == null) {
            fmt = UnitsUtil.getNumberFormat(new double[] { newVal }, locale);
        } else {
            fmt = this.getSpecificFormatter(specifics, locale);
        }

        return this.createFormattedValue(newVal, targScale, fmt);
    }

    public FormattedNumber[] formatSame(double[] vals, UnitsConstants unitType, ScaleConstants scale, Locale locale,
        FormatSpecifics specifics) {
        FormattedNumber[] res;
        NumberFormat fmt;
        UnitNumber tmpNum;
        double[] newVals;
        double average;
        ScaleConstants targScale;

        res = new FormattedNumber[vals.length];

        if (vals.length == 0) {
            return res;
        }

        average = ArrayUtil.average(vals);

        tmpNum = new UnitNumber(average, unitType, scale);
        targScale = this.findGoodLookingScale(tmpNum.getBaseValue());

        newVals = new double[vals.length];
        for (int i = 0; i < vals.length; i++) {
            tmpNum = new UnitNumber(vals[i], unitType, scale);
            newVals[i] = this.getTargetValue(tmpNum.getBaseValue(), targScale);
        }

        if (specifics == null) {
            fmt = UnitsUtil.getNumberFormat(newVals, locale);
        } else {
            fmt = this.getSpecificFormatter(specifics, locale);
        }

        for (int i = 0; i < vals.length; i++) {
            res[i] = this.createFormattedValue(newVals[i], targScale, fmt);
        }

        return res;
    }

    protected abstract String getTagName();

    protected FormattedNumber createFormattedValue(double value, ScaleConstants scale, NumberFormat fmt) {
        String tag;

        switch (scale) {
        case SCALE_NONE: {
            tag = "";
            break;
        }

        case SCALE_KILO: {
            tag = "K";
            break;
        }

        case SCALE_MEGA: {
            tag = "M";
            break;
        }

        case SCALE_GIGA: {
            tag = "G";
            break;
        }

        case SCALE_TERA: {
            tag = "T";
            break;
        }

        case SCALE_PETA: {
            tag = "P";
            break;
        }

        default: {
            throw new IllegalStateException("Unhandled scale");
        }
        }

        return new FormattedNumber(fmt.format(value), tag + this.getTagName());
    }

    private double getTargetValue(BigDecimal baseVal, ScaleConstants targetScale) {
        BigDecimal modifier;
        double lateModifier;

        modifier = UnitsUtil.FACT_NONE;
        lateModifier = 1.0;

        switch (targetScale) {
        case SCALE_KILO: {
            lateModifier = 1 << 10;
            break;
        }

        case SCALE_MEGA: {
            lateModifier = 1 << 20;
            break;
        }

        case SCALE_GIGA: {
            modifier = UnitsUtil.FACT_MEGA_BIN;
            lateModifier = 1 << 10;
            break;
        }

        case SCALE_TERA: {
            modifier = UnitsUtil.FACT_GIGA_BIN;
            lateModifier = 1 << 10;
            break;
        }

        case SCALE_PETA: {
            modifier = UnitsUtil.FACT_TERA_BIN;
            lateModifier = 1 << 10;
        }
        }

        baseVal = baseVal.divide(modifier, BigDecimal.ROUND_HALF_EVEN);
        return baseVal.doubleValue() / lateModifier;
    }

    private ScaleConstants findGoodLookingScale(BigDecimal val) {
        if (val.compareTo(UnitsUtil.FACT_PETA_BIN) >= 1) {
            return ScaleConstants.SCALE_PETA;
        } else if (val.compareTo(UnitsUtil.FACT_TERA_BIN) >= 1) {
            return ScaleConstants.SCALE_TERA;
        } else if (val.compareTo(UnitsUtil.FACT_GIGA_BIN) >= 1) {
            return ScaleConstants.SCALE_GIGA;
        } else if (val.compareTo(UnitsUtil.FACT_MEGA_BIN) >= 1) {
            return ScaleConstants.SCALE_MEGA;
        } else if (val.compareTo(UnitsUtil.FACT_KILO_BIN) >= 1) {
            return ScaleConstants.SCALE_KILO;
        } else {
            return ScaleConstants.SCALE_NONE;
        }
    }

    public BigDecimal getBaseValue(double value, ScaleConstants scale) {
        BigDecimal res;

        res = new BigDecimal(value);
        return res.multiply(this.getScaleCoeff(scale));
    }

    public BigDecimal getScaledValue(BigDecimal value, ScaleConstants targScale) {
        return value.divide(this.getScaleCoeff(targScale), BigDecimal.ROUND_HALF_EVEN);
    }

    private BigDecimal getScaleCoeff(ScaleConstants scale) {
        switch (scale) {
        case SCALE_NONE: {
            return UnitsUtil.FACT_NONE;
        }

        case SCALE_KILO: {
            return UnitsUtil.FACT_KILO_BIN;
        }

        case SCALE_MEGA: {
            return UnitsUtil.FACT_MEGA_BIN;
        }

        case SCALE_GIGA: {
            return UnitsUtil.FACT_GIGA_BIN;
        }

        case SCALE_TERA: {
            return UnitsUtil.FACT_TERA_BIN;
        }

        case SCALE_PETA: {
            return UnitsUtil.FACT_PETA_BIN;
        }

        default: {
            throw new IllegalArgumentException("Value did not have binary " + "based scale");
        }
        }
    }

    protected abstract UnitNumber parseTag(double number, String tag, int tagIdx, ParseSpecifics specifics)
        throws ParseException;

    public UnitNumber parse(String val, Locale locale, ParseSpecifics specifics) throws ParseException {
        NumberFormat fmt = NumberFormat.getInstance(locale);
        double numberPart;
        int nonIdx;

        nonIdx = UnitsUtil.findNonNumberIdx(val, fmt);
        if (nonIdx == -1) {
            throw new ParseException("Number had no units with it", val.length());
        }

        if (nonIdx == 0) {
            throw new ParseException("Invalid number specified", 0);
        }

        numberPart = fmt.parse(val.substring(0, nonIdx)).doubleValue();
        return this.parseTag(numberPart, val.substring(nonIdx, val.length()).trim(), nonIdx, specifics);
    }
}