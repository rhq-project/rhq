/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation version 2 of the License.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program; if not, write to the Free Software
 *  * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

package org.rhq.enterprise.server.rest.reporting;

import java.text.DecimalFormat;

import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.composite.MeasurementNumericValueAndUnits;
import org.rhq.core.domain.measurement.util.MeasurementConversionException;

/**
 * @author jsanda
 */
public class MeasurementConverter {

    private static final int MAX_PRECISION_DIGITS = 4;
    private static final String NULL_OR_NAN_FORMATTED_VALUE = "--no data available--";

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

        DecimalFormat numberFormat = new DecimalFormat(getFormat(
            minimumFractionDigits != null ? minimumFractionDigits : 1,
            maximumFractionDigits != null ? maximumFractionDigits : 1));

        String formatted = numberFormat.format(value);

        return format(formatted, targetUnits);
    }

    public static String format(String value, MeasurementUnits targetUnits) {
        if (targetUnits == null) {
            return value;
        } else {
            return value + getMeasurementUnitAbbreviation(targetUnits);
        }
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

    public static Double scale(MeasurementNumericValueAndUnits origin, MeasurementUnits targetUnits)
        throws MeasurementConversionException {
        MeasurementUnits originUnits = origin.getUnits();
        Double originValue = origin.getValue();

        return originValue * MeasurementUnits.calculateOffset(originUnits, targetUnits);
    }

    public static String getMeasurementUnitAbbreviation(MeasurementUnits units) {
        switch (units) {
            case NONE:
                return "";
            case PERCENTAGE:
                return "%";
            case BYTES:
                return "B";
            case KILOBYTES:
                return "KB";
            case MEGABYTES:
                return "MB";
            case GIGABYTES:
                return "GB";
            case TERABYTES:
                return "TB";
            case PETABYTES:
                return "PB";
            case BITS:
                return "b";
            case KILOBITS:
                return "kb";
            case MEGABITS:
                return "Mb";
            case GIGABITS:
                return "Gb";
            case TERABITS:
                return "Tb";
            case PETABITS:
                return "Pb";
            case EPOCH_MILLISECONDS:
                return ""; // absolute time - no display
            case EPOCH_SECONDS:
                return ""; // absolute time - no display
            case JIFFYS:
                return "j";
            case NANOSECONDS:
                return "ns";
            case MICROSECONDS:
                return "us";
            case MILLISECONDS:
                return "ms";
            case SECONDS:
                return "s";
            case MINUTES:
                return "m";
            case HOURS:
                return "h";
            case DAYS:
                return "d";
            case CELSIUS:
                return "C";
            case KELVIN:
                return "K";
            case FAHRENHEIGHT:
                return "F";
            default:
                return units.toString();
        }
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
