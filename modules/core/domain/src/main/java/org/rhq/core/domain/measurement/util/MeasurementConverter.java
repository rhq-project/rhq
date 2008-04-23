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
package org.rhq.core.domain.measurement.util;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.composite.MeasurementNumericValueAndUnits;

public class MeasurementConverter {

    private static final int MAX_PRECISION_DIGITS = 4;

    private static Pattern numberOptionalUnitPattern;
    private static final String numberOptionalUnitPatternString = "([\\+\\-]?" + // number sign is optional but valid
        "[0-9\\.\\,]{1,})" + // followed by at least one magnitude identifier, capture number plus sign
        "([\\%a-z]*)"; // followed by an optional units identifier, capture entire suffix

    static {
        numberOptionalUnitPattern = Pattern.compile(numberOptionalUnitPatternString, Pattern.CASE_INSENSITIVE);
    }

    private static NumberFormat getDefaultNumberFormat() {
        NumberFormat nf = NumberFormat.getNumberInstance();

        nf.setMinimumFractionDigits(1);
        nf.setMaximumFractionDigits(1);

        return nf;
    }

    public static MeasurementNumericValueAndUnits parse(String input, MeasurementUnits targetUnits)
        throws MeasurementConversionException {
        input = input.replaceAll("\\s", ""); // our pattern assumes no whitespace 
        Matcher matcher = numberOptionalUnitPattern.matcher(input);

        if (matcher.matches() == false) {
            throw new MeasurementConversionException("The passed input '" + input + "' could not be parsed correctly "
                + "by the regular expression " + numberOptionalUnitPatternString);
        }

        String magnitude = matcher.group(1);
        String units = matcher.group(2);

        MeasurementUnits fromUnits;
        if (units.equals("")) {
            /* 
             * no units is valid, and we assume the passed targetUnits; however, we will 
             * still need to check that the number is well-formed, so continue processing.
             */
            fromUnits = targetUnits;
        } else {
            fromUnits = MeasurementUnits.getUsingDisplayUnits(units, targetUnits.getFamily());

            if ((fromUnits == null) || (fromUnits.isComparableTo(targetUnits) == false)) {
                throw new MeasurementConversionException("The units in '" + input + "' were not valid, " + "expected '"
                    + targetUnits.getFamily() + "' units " + "received '" + units + "' units");
            }
        }

        try {
            if (magnitude.startsWith("+")) {
                magnitude = magnitude.substring(1);
            }

            Number convertedMagnitude = DecimalFormat.getInstance().parse(magnitude);
            Double scaledMagnitude = null;

            // apply relative scale if applicable, otherwise perform standard scaling
            if (MeasurementUnits.Family.RELATIVE == targetUnits.getFamily()) {
                scaledMagnitude = MeasurementUnits.scaleDown(convertedMagnitude.doubleValue(), targetUnits);
            } else {
                MeasurementNumericValueAndUnits valueAndUnits = new MeasurementNumericValueAndUnits(convertedMagnitude
                    .doubleValue(), fromUnits);
                scaledMagnitude = scale(valueAndUnits, targetUnits);
            }

            return new MeasurementNumericValueAndUnits(scaledMagnitude, targetUnits);
        } catch (ParseException pe) {
            throw new MeasurementConversionException("The magnitude in '" + input + "' did not parse correctly "
                + "as a valid, localized, stringified number ");
        }

    }

    public static MeasurementNumericValueAndUnits parse(String input, MeasurementSchedule targetSchedule)
        throws MeasurementConversionException {
        MeasurementUnits targetUnits = targetSchedule.getDefinition().getUnits();

        return parse(input, targetUnits);
    }

    public static Double scale(MeasurementNumericValueAndUnits origin, MeasurementUnits targetUnits)
        throws MeasurementConversionException {
        MeasurementUnits originUnits = origin.getUnits();
        Double originValue = origin.getValue();

        return originValue * MeasurementUnits.calculateOffset(originUnits, targetUnits);
    }

    public static Double scale(Double origin, MeasurementUnits targetUnits) throws MeasurementConversionException {
        boolean wasNegative = false;
        if (origin < 0) {
            wasNegative = true;
            origin = -origin;
        }

        MeasurementUnits baseUnit = targetUnits.getBaseUnits();
        MeasurementNumericValueAndUnits valueAndUnits = new MeasurementNumericValueAndUnits(origin, baseUnit);

        Double results = scale(valueAndUnits, targetUnits);
        if (wasNegative) {
            results = -results;
        }

        return results;
    }

    public static String format(String value, MeasurementUnits targetUnits) {
        if (targetUnits == null) {
            return value;
        } else {
            return value + targetUnits;
        }
    }

    /**
     * Given an array of double values determine the necessary precision such that when formatted they are distinct and reasonable to look at.  For example,
     * for values { 1.45 1.46 1.47 1.48 1.49 } the desired precision is 2. Less precision loses significant digits, more precision provides no added benefit. 
     * Max precision is bounded for presentation considerations.    
     * @param values
     * @param targetUnits
     * @param bestFit For the family of units, use a normalized scale
     * @return
     */
    public static String[] formatToSignifantPrecision(double[] values, MeasurementUnits targetUnits, boolean bestFit) {
        if ((null == values) || (values.length == 0)) {
            return null;
        }

        MeasurementUnits originalUnits = targetUnits;

        /*
         * in the overwhelming majority of cases, you're going to want to apply a bestFit 
         * to the passed data, but it's not required; it's perfectly possible to allow a 
         * list of doubles to be formatted without being fit, in which case the targetUnits 
         * will be part of the formatted display for each result element 
         */
        if (bestFit) {
            // find bestFit units by taking the average
            Double average = 0.0;

            for (int i = 0, sz = values.length; i < sz; i++) {
                /* 
                 * adding fractional amount iterative leads to greater 
                 * error, but prevents overflow on large data sets
                 */
                average += (values[i] / sz);
            }

            MeasurementNumericValueAndUnits fittedAverage = fit(average, targetUnits);
            MeasurementUnits fittedUnits = fittedAverage.getUnits();

            /*
             * and change the local reference to targetUnits, so that the same logic
             * can be used both for the bestFit and non-bestFit computations
             */
            targetUnits = fittedUnits;
        }

        @SuppressWarnings("unused")
        Set<String> existingStrings = null; // technically this *is* unused because  
        int precisionDigits = 0;
        boolean scaleWithMorePrecision = true;
        String[] results = new String[values.length];
        NumberFormat nf = getDefaultNumberFormat();

        /*
         * we scale at most to MAX_PRECISION_DIGITS to allow for presentation limits
         * 
         * increase the maxPrecisionDigits in the while condition 
         * itself to ensure it gets done for every loop
         */
        while (scaleWithMorePrecision && (++precisionDigits <= MAX_PRECISION_DIGITS)) {
            /* 
             * make the assumption that we no longer need to scale beyond this iteration
             */
            scaleWithMorePrecision = false;

            /*
             * we need to record the uniquely formatted values so we can determine 
             */
            existingStrings = new HashSet<String>();
            nf.setMinimumFractionDigits(0);
            nf.setMaximumFractionDigits(precisionDigits);

            Double[] scaledValues = new Double[values.length];

            for (int i = 0; i < scaledValues.length; i++) {
                /*
                 *  For relative units apply the scale now, prior to the nf.format(), since we are not using format( Double...).
                 *  Otherwise, apply standard multi-unit scaling.
                 */
                if (MeasurementUnits.Family.RELATIVE == originalUnits.getFamily()) {
                    scaledValues[i] = MeasurementUnits.scaleUp(values[i], originalUnits);
                } else {
                    scaledValues[i] = scale(new MeasurementNumericValueAndUnits(values[i], originalUnits), targetUnits);
                }
            }

            for (int i = 0; i < results.length; i++) {
                /*
                 * JUST get the formatted value, specifically DON'T tack on the formatted units yet; 
                 * we do this to see how many units we'll have to scale to afterwards (outside this 
                 * while loop) to make the array of values passed to us unique
                 */
                String formatted = nf.format(scaledValues[i]);

                /*
                 * check whether formatted value was in the set or not; if it was, we have to 
                 * loop, but only if we're not not already at our maximum precision 
                 */
                boolean wasNewElement = existingStrings.add(formatted);

                if ((wasNewElement == false) && (precisionDigits < MAX_PRECISION_DIGITS)) {
                    scaleWithMorePrecision = true;
                    break;
                }

                results[i] = formatted;
            }
        }

        /*
         * we did the best we could in terms of trying to find a precision that adds the most
         * uniqueness to the given set of values, NOW tack on the formatted value for the units
         */
        for (int i = 0; i < results.length; i++) {
            results[i] = format(results[i], targetUnits);
        }

        return results;
    }

    public static String format(Double value, MeasurementUnits targetUnits, boolean bestFit) {
        return format(value, targetUnits, bestFit, (Integer) null, (Integer) null);
    }

    public static String format(Double value, MeasurementUnits targetUnits, boolean bestFit,
        Integer minimumFractionDigits, Integer maximumFractionDigits) {
        if (bestFit) {
            MeasurementNumericValueAndUnits valueAndUnits = fit(value, targetUnits);

            value = valueAndUnits.getValue();
            targetUnits = valueAndUnits.getUnits();
        }

        // apply relative scale at presentation time
        if (MeasurementUnits.Family.RELATIVE == targetUnits.getFamily()) {
            value = MeasurementUnits.scaleUp(value, targetUnits);
        }

        NumberFormat numberFormat = getDefaultNumberFormat();
        if (null != minimumFractionDigits) {
            numberFormat.setMinimumFractionDigits(minimumFractionDigits);
        }
        if (null != maximumFractionDigits) {
            numberFormat.setMaximumFractionDigits(maximumFractionDigits);
        }
        String formatted = numberFormat.format(value);

        return format(formatted, targetUnits);
    }

    public static String scaleAndFormat(Double origin, MeasurementSchedule targetSchedule, boolean bestFit)
        throws MeasurementConversionException {
        MeasurementUnits targetUnits = targetSchedule.getDefinition().getUnits();

        return scaleAndFormat(origin, targetUnits, bestFit, (Integer) null, (Integer) null);
    }

    public static String scaleAndFormat(Double origin, MeasurementUnits targetUnits, boolean bestFit)
        throws MeasurementConversionException {

        return scaleAndFormat(origin, targetUnits, bestFit, (Integer) null, (Integer) null);
    }

    public static String scaleAndFormat(Double origin, MeasurementUnits targetUnits, boolean bestFit,
        Integer minimumFractionDigits, Integer maximumFractionDigits) throws MeasurementConversionException {

        MeasurementUnits baseUnits = targetUnits.getBaseUnits();
        MeasurementNumericValueAndUnits valueAndUnits = new MeasurementNumericValueAndUnits(origin, baseUnits);
        Double scaledMagnitude = scale(valueAndUnits, targetUnits);

        return format(scaledMagnitude, targetUnits, bestFit);
    }

    public static MeasurementNumericValueAndUnits fit(Double origin, MeasurementUnits units) {
        return fit(origin, units, null, null);
    }

    public static MeasurementNumericValueAndUnits fit(Double origin, MeasurementUnits units, MeasurementUnits lowUnits,
        MeasurementUnits highUnits) {

        // work-around for the various Chart descendants not properly setting their units field;      
        if (null == units) {
            return new MeasurementNumericValueAndUnits(origin, units);
        }

        // by definition, absolutely specified units don't scale to anything
        if ((MeasurementUnits.Family.ABSOLUTE == units.getFamily())
            || (MeasurementUnits.Family.DURATION == units.getFamily())) {
            return new MeasurementNumericValueAndUnits(origin, units);
        }

        // by definition relative-valued units are self-scaled (converted at formatting)       
        if (MeasurementUnits.Family.RELATIVE == units.getFamily()) {
            return new MeasurementNumericValueAndUnits(origin, units);
        }

        // if the magnitude is zero, the best-fit also will spin around forever since it won't change
        if (Math.abs(origin) < 1e-9) {
            return new MeasurementNumericValueAndUnits(origin, units);
        }

        boolean wasNegative = false;

        if (origin < 0) {
            wasNegative = true;
            origin = -origin;
        }

        MeasurementNumericValueAndUnits currentValueAndUnits = null;
        MeasurementNumericValueAndUnits nextValueAndUnits = new MeasurementNumericValueAndUnits(origin, units);

        // first, make the value smaller if it's too big
        int maxOrdinal = (highUnits != null) ? (highUnits.ordinal() + 1) : MeasurementUnits.values().length;

        do {
            currentValueAndUnits = nextValueAndUnits;

            int nextOrdinal = currentValueAndUnits.getUnits().ordinal() + 1;
            if (nextOrdinal == maxOrdinal) {
                // we could theoretically get bigger, but we don't have any units to represent that
                break;
            }

            MeasurementUnits biggerUnits = MeasurementUnits.values()[nextOrdinal];
            if (biggerUnits.getFamily() != currentValueAndUnits.getUnits().getFamily()) {
                // we're as big as we can get, break out of the loop so we can return
                break;
            }

            Double smallerValue = scale(currentValueAndUnits, biggerUnits);

            nextValueAndUnits = new MeasurementNumericValueAndUnits(smallerValue, biggerUnits);
        } while (nextValueAndUnits.getValue() > 1.0);

        // next, make the value bigger if it's too small
        int minOrdinal = (lowUnits != null) ? (lowUnits.ordinal() - 1) : -1;

        while (currentValueAndUnits.getValue() < 1.0) {
            int nextOrdinal = currentValueAndUnits.getUnits().ordinal() - 1;
            if (nextOrdinal == minOrdinal) {
                // we could theoretically get smaller, but we don't have any units to represent that
                break;
            }

            MeasurementUnits smallerUnits = MeasurementUnits.values()[nextOrdinal];
            if (smallerUnits.getFamily() != currentValueAndUnits.getUnits().getFamily()) {
                // we're as small as we can get, break out of the loop so we can return
                break;
            }

            Double biggerValue = scale(currentValueAndUnits, smallerUnits);

            nextValueAndUnits = new MeasurementNumericValueAndUnits(biggerValue, smallerUnits);

            currentValueAndUnits = nextValueAndUnits;
        }

        if (wasNegative) {
            return new MeasurementNumericValueAndUnits(-currentValueAndUnits.getValue(), currentValueAndUnits
                .getUnits());
        }

        return currentValueAndUnits;
    }

}
