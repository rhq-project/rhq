/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.core.server;

import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.composite.MeasurementNumericValueAndUnits;
import org.rhq.core.domain.measurement.util.MeasurementConversionException;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Greg Hinkle
 */
public class MeasurementParser {
    
    private static final String NUMBER_OPTIONAL_UNIT_PATTERN_STRING = "([\\+\\-]?" + // number sign is optional but valid
        "[0-9\\.\\,]{1,})" + // followed by at least one magnitude identifier, capture number plus sign
        "([\\%a-z]*)"; // followed by an optional units identifier, capture entire suffix
    private static final Pattern NUMBER_OPTIONAL_UNIT_PATTERN;
    static {
        NUMBER_OPTIONAL_UNIT_PATTERN = Pattern.compile(NUMBER_OPTIONAL_UNIT_PATTERN_STRING, Pattern.CASE_INSENSITIVE);
    }

    public static MeasurementNumericValueAndUnits parse(String input, MeasurementUnits targetUnits)
        throws MeasurementConversionException {
        input = input.replaceAll("\\s", ""); // our pattern assumes no whitespace
        Matcher matcher = NUMBER_OPTIONAL_UNIT_PATTERN.matcher(input);

        if (!matcher.matches()) {
            throw new MeasurementConversionException("The passed input '" + input + "' could not be parsed correctly "
                + "by the regular expression " + NUMBER_OPTIONAL_UNIT_PATTERN_STRING);
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

            if ((fromUnits == null) || (!fromUnits.isComparableTo(targetUnits))) {
                throw new MeasurementConversionException("The units in '" + input + "' were not valid, " + "expected '"
                    + targetUnits.getFamily() + "' units, received '" + units + "' units");
            }
        }

        try {
            if (magnitude.startsWith("+")) {
                magnitude = magnitude.substring(1);
            }

            Number convertedMagnitude = DecimalFormat.getInstance().parse(magnitude);
            Double scaledMagnitude;

            // apply relative scale if applicable, otherwise perform standard scaling
            if (MeasurementUnits.Family.RELATIVE == targetUnits.getFamily()) {
                scaledMagnitude = MeasurementUnits.scaleDown(convertedMagnitude.doubleValue(), targetUnits);
            } else {
                MeasurementNumericValueAndUnits valueAndUnits = new MeasurementNumericValueAndUnits(convertedMagnitude
                    .doubleValue(), fromUnits);
                scaledMagnitude = MeasurementConverter.scale(valueAndUnits, targetUnits);
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
}
