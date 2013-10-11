/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.coregui.client.util.measurement;

import com.google.gwt.i18n.client.NumberFormat;

import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.composite.MeasurementNumericValueAndUnits;
import org.rhq.core.domain.measurement.util.MeasurementConversionException;
import org.rhq.coregui.client.util.MeasurementConverterClient;

/**
 * Parses measurement strings given in the typical form of "[sign] magnitude [units]" such as:
 * 
 * -1.0
 * +1.0s
 * 200MB
 * 
 * This is purposefully written without java.text and java.util.regex classes so GWT 2.0
 * can use this class.
 * 
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public class MeasurementParser {

    public static MeasurementNumericValueAndUnits parse(String input, MeasurementUnits targetUnits)
        throws MeasurementConversionException {

        if (input == null) {
            return new MeasurementNumericValueAndUnits(Double.valueOf(0.0), targetUnits);
        }

        input = input.trim();

        if (input.length() == 0) {
            return new MeasurementNumericValueAndUnits(Double.valueOf(0.0), targetUnits);
        }

        int i = 0;

        // skip over the sign, we'll deal with that later
        if (input.startsWith("+") || input.startsWith("-")) {
            i = 1;
        }

        // find the end of the magnitude (i.e. the number itself)
        // note we allow for decimals - and we allow for either "." or "," as the decimal point for i18n purposes
        for (; i < input.length(); i++) {
            char ch = input.charAt(i);
            if (!(Character.isDigit(ch) || ch == '.' || ch == ',')) {
                break;
            }
        }

        String magnitude = input.substring(0, i);
        String units = "";

        if (i <= input.length()) {
            // gobble everything after the magnitude and consider it the units
            units = input.substring(i);
            units = units.trim();
        }

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

            Number convertedMagnitude = NumberFormat.getDecimalFormat().parse(magnitude);
            Double scaledMagnitude;

            // apply relative scale if applicable, otherwise perform standard scaling
            if (MeasurementUnits.Family.RELATIVE == targetUnits.getFamily()) {
                scaledMagnitude = MeasurementUnits.scaleDown(convertedMagnitude.doubleValue(), targetUnits);
            } else {
                MeasurementNumericValueAndUnits valueAndUnits = new MeasurementNumericValueAndUnits(convertedMagnitude
                    .doubleValue(), fromUnits);
                scaledMagnitude = MeasurementConverterClient.scale(valueAndUnits, targetUnits);
            }

            return new MeasurementNumericValueAndUnits(scaledMagnitude, targetUnits);
        } catch (Exception e) {
            throw new MeasurementConversionException("The magnitude in '" + input + "' did not parse correctly "
                + "as a valid, localized, stringified number ");
        }

    }
}
