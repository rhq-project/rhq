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
package org.rhq.coregui.client.util;

import java.util.HashSet;
import java.util.Set;

import com.google.gwt.i18n.client.NumberFormat;

import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.composite.MeasurementNumericValueAndUnits;
import org.rhq.core.domain.measurement.util.MeasurementConversionException;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.Messages;

public class MeasurementConverterClient {
    private static final Messages MSG = CoreGUI.getMessages();

    private static final int MAX_PRECISION_DIGITS = 4;
    private static final String NULL_OR_NAN_FORMATTED_VALUE = MSG.view_measure_nan();

    private static NumberFormat getDefaultNumberFormat() {
        NumberFormat nf = NumberFormat.getFormat("0.0");

        return nf;
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
            String abbr = getMeasurementUnitAbbreviation(targetUnits);
            return (abbr.length() > 0) ? (value + " " + abbr) : value;
        }
    }

    public static String getMeasurementUnitAbbreviation(MeasurementUnits units) {
        switch (units) {
        case NONE:
            return "";
        case PERCENTAGE:
            return MSG.common_unit_abbrev_percentage();
        case BYTES:
            return MSG.common_unit_abbrev_bytes();
        case KILOBYTES:
            return MSG.common_unit_abbrev_kilobytes();
        case MEGABYTES:
            return MSG.common_unit_abbrev_megabytes();
        case GIGABYTES:
            return MSG.common_unit_abbrev_gigabytes();
        case TERABYTES:
            return MSG.common_unit_abbrev_terabytes();
        case PETABYTES:
            return MSG.common_unit_abbrev_petabytes();
        case BITS:
            return MSG.common_unit_abbrev_bits();
        case KILOBITS:
            return MSG.common_unit_abbrev_kilobits();
        case MEGABITS:
            return MSG.common_unit_abbrev_megabits();
        case GIGABITS:
            return MSG.common_unit_abbrev_gigabits();
        case TERABITS:
            return MSG.common_unit_abbrev_terabits();
        case PETABITS:
            return MSG.common_unit_abbrev_petabits();
        case EPOCH_MILLISECONDS:
            return ""; // absolute time - no display
        case EPOCH_SECONDS:
            return ""; // absolute time - no display
        case JIFFYS:
            return MSG.common_unit_abbrev_jiffys();
        case NANOSECONDS:
            return MSG.common_unit_abbrev_nanoseconds();
        case MICROSECONDS:
            return MSG.common_unit_abbrev_microseconds();
        case MILLISECONDS:
            return MSG.common_unit_abbrev_milliseconds();
        case SECONDS:
            return MSG.common_unit_abbrev_seconds();
        case MINUTES:
            return MSG.common_unit_abbrev_minutes();
        case HOURS:
            return MSG.common_unit_abbrev_hours();
        case DAYS:
            return MSG.common_unit_abbrev_days();
        case CELSIUS:
            return MSG.common_unit_abbrev_celsius();
        case KELVIN:
            return MSG.common_unit_abbrev_kelvin();
        case FAHRENHEIGHT:
            return MSG.common_unit_abbrev_fahrenheight();
        default:
            return units.toString(); // unknown units - developer forgot to add a case statement - just use the toString for now
        }
    }

    public static MeasurementUnits bestFitUnits(double[] values,  MeasurementUnits originalUnits) {
        // find bestFit units by taking the average
        Double average = 0.0;
        for (int i = 0; i < values.length; i++) {
                /*
                 * adding fractional amount iterative leads to greater
                 * error, but prevents overflow on large data sets
                 */
            average += (values[i] / values.length);
        }

        MeasurementNumericValueAndUnits fittedAverage = fit(average, originalUnits);
        return fittedAverage.getUnits();
    }

    /**
     * Formats the given array of double values: determines the necessary precision such that when formatted, they are
     * distinct and reasonable to look at. For example, for values { 1.45 1.46 1.47 1.48 1.49 } the desired precision is
     * 2 - less precision loses significant digits, and more precision provides no added benefit. Max precision is
     * bounded for presentation considerations.
     *
     * @param values the values to be formatted
     * @param targetUnits the target units for the values
     * @param bestFit whether or not to use a normalized scale for the family of units
     *
     * @return the formatted values
     */
    public static String[] formatToSignificantPrecision(double[] values, MeasurementUnits targetUnits, boolean bestFit) {
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
            MeasurementUnits fittedUnits = bestFitUnits(values, targetUnits);

            /*
             * and change the local reference to targetUnits, so that the same logic
             * can be used both for the bestFit and non-bestFit computations
             */
            targetUnits = fittedUnits;
        }

        Set<String> existingStrings; // technically this *is* unused because
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
            nf = NumberFormat.getFormat(getFormat(0, precisionDigits));

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

                if ((!wasNewElement) && (precisionDigits < MAX_PRECISION_DIGITS)) {
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
        return format(value, targetUnits, bestFit, null, null);
    }

    public static String format(Double value, MeasurementUnits targetUnits, boolean bestFit,
        Integer minimumFractionDigits, Integer maximumFractionDigits) {
        if (value == null || Double.isNaN(value)) {
            return NULL_OR_NAN_FORMATTED_VALUE;
        }
        if (bestFit) {
            MeasurementNumericValueAndUnits valueAndUnits = fit(value, targetUnits);

            value = valueAndUnits.getValue();
            targetUnits = valueAndUnits.getUnits();
        }

        // apply relative scale at presentation time
        if (targetUnits != null && MeasurementUnits.Family.RELATIVE == targetUnits.getFamily()) {
            value = MeasurementUnits.scaleUp(value, targetUnits);
        }

        NumberFormat numberFormat = NumberFormat.getFormat(getFormat(
            minimumFractionDigits != null ? minimumFractionDigits : 1,
            maximumFractionDigits != null ? maximumFractionDigits : 1));

        String formatted = numberFormat.format(value);

        return format(formatted, targetUnits);
    }



    public static String scaleAndFormat(Double origin, MeasurementUnits targetUnits, boolean bestFit ) throws MeasurementConversionException {

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
            return new MeasurementNumericValueAndUnits(origin, null);
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

        if (MeasurementUnits.Family.TEMPERATURE == units.getFamily()) {
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

        MeasurementNumericValueAndUnits currentValueAndUnits;
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
            return new MeasurementNumericValueAndUnits(-currentValueAndUnits.getValue(),
                currentValueAndUnits.getUnits());
        }

        return currentValueAndUnits;
    }

    public static String getFormat(int minDigits, int maxDigits) {
        StringBuilder buf = new StringBuilder("0.");
        for (int i = 0; i < minDigits; i++) {
            buf.append("0");
        }
        for (int i = 0; i < (maxDigits - minDigits); i++) {
            buf.append("#");
        }
        return buf.toString();
    }
}
