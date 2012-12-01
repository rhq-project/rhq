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
package org.rhq.core.domain.util.units.test;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.rhq.core.server.MeasurementConverter;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.testng.annotations.Test;

import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.composite.MeasurementNumericValueAndUnits;
import org.rhq.core.domain.measurement.util.MeasurementConversionException;
import org.rhq.core.server.MeasurementParser;

// TODO two tests in here need to be re-enables once the GWT client issues are resolved
@Test
public class MeasurementConverterTest extends AbstractEJB3Test {
    private final double POSITIVE = 300;
    private final double NEGATIVE = -42;

    private final String[] WHITESPACE = new String[] { " ", "\f", "\n", "\r", "\t" };

    private ThreadLocal<Integer> precisionScalingTestCount = null;

    private void incrementPrecisionScalingTestCount() {
        if (precisionScalingTestCount == null) {
            precisionScalingTestCount = new ThreadLocal<Integer>() {
                @Override
                protected Integer initialValue() {
                    return 0;
                }
            };
        }
        precisionScalingTestCount.set(precisionScalingTestCount.get() + 1);
    }

    @Test(groups = "integration.ejb3", enabled = false)
    // TODO fix me
    public void testPrecisionScaling() throws Exception {

        // if all values are equal, it will format to the max precision

        testPrecisionScaling(new double[] { 1.41, 1.41, 1.41, 1.41, 1.41 }, MeasurementUnits.NONE, new String[] {
            "1.41", "1.41", "1.41", "1.41", "1.41" }, MeasurementUnits.NONE);

        // if all values are unique, it won't need to format any of them to more digits
        testPrecisionScaling(new double[] { 1.41, 1.42, 1.43, 1.44, 1.45 }, MeasurementUnits.NONE, new String[] {
            "1.41", "1.42", "1.43", "1.44", "1.45" }, MeasurementUnits.NONE);

        // even if only two are equal, it will scale all the way to max precision looking for uniqueness
        testPrecisionScaling(new double[] { 1.4, 1.4, 1.5, 1.6, 1.7 }, MeasurementUnits.NONE, new String[] { "1.4",
            "1.4", "1.5", "1.6", "1.7" }, MeasurementUnits.NONE);

        // to final a "good looking" target scaling units, it will use the mathematical mean
        // NOTE: formatting doesn't round up/down, thus introducing error from cutting digits (256 -> 0.2KB [instead of 0.25KB])      
        testPrecisionScaling(new double[] { 128, 256, 512, 1024, 8192 }, MeasurementUnits.BYTES, new String[] {
            "0.1KB", "0.2KB", "0.5KB", "1KB", "8KB" }, MeasurementUnits.KILOBYTES);

        // same test with negative numbers
        testPrecisionScaling(new double[] { -128, -256, -512, -1024, -8192 }, MeasurementUnits.BYTES, new String[] {
            "-0.1KB", "-0.2KB", "-0.5KB", "-1KB", "-8KB" }, MeasurementUnits.KILOBYTES);

        // test self scaling relative units
        testPrecisionScaling(new double[] { 0.0, 0.065, 1.0, 0.123456 }, MeasurementUnits.PERCENTAGE, new String[] {
            "0%", "6.5%", "100%", "12.3%" }, MeasurementUnits.PERCENTAGE);

        // test self scaling relative units
        testPrecisionScaling(new double[] { 0.0621, 0.065, 0.0620, 0.6202 }, MeasurementUnits.PERCENTAGE, new String[] {
            "6.21%", "6.5%", "6.2%", "62.02%" }, MeasurementUnits.PERCENTAGE);
    }

    private void testPrecisionScaling(double[] values, MeasurementUnits units, String[] expectedResults,
        MeasurementUnits expectedUnits) {
        incrementPrecisionScalingTestCount();

        String[] results = MeasurementConverter.formatToSignificantPrecision(values, units, true);
        for (int i = 0; i < results.length; i++) {
            String expected = expectedResults[i];
            String actual = results[i];

            // The expected output is in Locale.US. Handle running the tests in a different default locale
            if (expected.contains(".") && (!Locale.getDefault().equals(Locale.US))) {
                Locale defaultLocale = Locale.getDefault();
                Locale.setDefault(Locale.US);
                MeasurementNumericValueAndUnits vu = MeasurementParser.parse(expected, expectedUnits);
                Locale.setDefault(defaultLocale);
                expected = MeasurementConverter.format(vu.getValue(), expectedUnits, false, null, 4);
            }

            assert actual.equals(expected) : "Test " + precisionScalingTestCount.get() + ": " + "Index " + i + ", "
                + "Expected " + expected + ", " + "Received " + actual + ", " + "Units " + units.getName();
        }
    }

    @Test(groups = "integration.ejb3", enabled = false)
    // TODO fix me
    public void testConversionSuccess() throws Exception {
        // test the straight-forward, non-whitespace cases
        for (MeasurementUnits units : MeasurementUnits.values()) {
            validateFormatConvert(POSITIVE, units);
            validateFormatConvert(NEGATIVE, units);
        }

        List<String> signVariations;
        List<String> magnitudeVariations;
        List<String> unitsVariations;

        // use the same NumberFormat that the MeasurementConverter uses, so that we get the correct expected results 
        NumberFormat nf = DecimalFormat.getNumberInstance();
        nf.setMinimumFractionDigits(1);
        nf.setMaximumFractionDigits(1);

        // test a couple hundred thousand variations of whitespace around each positive number with units
        signVariations = getWhitespaceVariationsAroundSingleTokens("", "+");
        magnitudeVariations = getWhitespaceVariationsAroundSingleTokens(nf.format(POSITIVE));
        for (MeasurementUnits units : MeasurementUnits.values()) {
            unitsVariations = getWhitespaceVariationsAroundSingleTokens(units.toString());

            // splice all combinations together and test conversion validity
            for (String signVariation : signVariations) {
                for (String magnitudeVariation : magnitudeVariations) {
                    if (MeasurementUnits.Family.RELATIVE == units.getFamily()) {
                        magnitudeVariation = nf.format(MeasurementUnits.scaleUp(nf.parse(magnitudeVariation.trim())
                            .doubleValue(), units));
                    }

                    for (String unitVariation : unitsVariations) {
                        String original = MeasurementConverter.format(POSITIVE, units, false);
                        String toBeTested = magnitudeVariation.trim() + unitVariation.trim();

                        assert original.equals(toBeTested) : "Error constructing whitespace string: " + "Expected '"
                            + original + "', " + "Received '" + toBeTested + "' " + "for units '" + units.name() + "'";

                        validateConvert(POSITIVE, signVariation + magnitudeVariation + unitVariation, units);
                    }
                }
            }
        }

        // test a couple hundred thousand variations of whitespace around each negative number with units
        signVariations = getWhitespaceVariationsAroundSingleTokens("-");
        for (MeasurementUnits units : MeasurementUnits.values()) {
            unitsVariations = getWhitespaceVariationsAroundSingleTokens(units.toString());

            for (String signVariation : signVariations) {
                for (String magnitudeVariation : magnitudeVariations) {
                    if (units == MeasurementUnits.PERCENTAGE) {
                        magnitudeVariation = nf.format(100 * nf.parse(magnitudeVariation.trim()).doubleValue());
                    }

                    for (String unitVariation : unitsVariations) {
                        String original = MeasurementConverter.format(-POSITIVE, units, false);
                        String toBeTested = "-" + magnitudeVariation.trim() + unitVariation.trim();

                        assert original.equals(toBeTested) : "Error constructing whitespace string: " + "Expected '"
                            + original + "', " + "Received '" + toBeTested + "' " + "for units '" + units.name() + "'";

                        validateConvert(-POSITIVE, signVariation + magnitudeVariation + unitVariation, units);
                    }
                }
            }
        }
    }

    @Test(groups = "integration.ejb3")
    public void testScalingSuccess() throws Exception {
        // Family.BYTES
        validateScaleAndInverse(1024.0, MeasurementUnits.BYTES, 1.0, MeasurementUnits.KILOBYTES);
        validateScaleAndInverse(1048576.0, MeasurementUnits.BYTES, 1.0, MeasurementUnits.MEGABYTES);
        validateScaleAndInverse(1073741824.0, MeasurementUnits.BYTES, 1.0, MeasurementUnits.GIGABYTES);
        validateScaleAndInverse(1099511627776.0, MeasurementUnits.BYTES, 1.0, MeasurementUnits.TERABYTES);
        validateScaleAndInverse(1125899906842624.0, MeasurementUnits.BYTES, 1.0, MeasurementUnits.PETABYTES);

        validateScaleAndInverse(1024.0, MeasurementUnits.KILOBYTES, 1.0, MeasurementUnits.MEGABYTES);
        validateScaleAndInverse(1048576.0, MeasurementUnits.KILOBYTES, 1.0, MeasurementUnits.GIGABYTES);
        validateScaleAndInverse(1073741824.0, MeasurementUnits.KILOBYTES, 1.0, MeasurementUnits.TERABYTES);
        validateScaleAndInverse(1099511627776.0, MeasurementUnits.KILOBYTES, 1.0, MeasurementUnits.PETABYTES);

        validateScaleAndInverse(1024.0, MeasurementUnits.MEGABYTES, 1.0, MeasurementUnits.GIGABYTES);
        validateScaleAndInverse(1048576.0, MeasurementUnits.MEGABYTES, 1.0, MeasurementUnits.TERABYTES);
        validateScaleAndInverse(1073741824.0, MeasurementUnits.MEGABYTES, 1.0, MeasurementUnits.PETABYTES);

        validateScaleAndInverse(1024.0, MeasurementUnits.GIGABYTES, 1.0, MeasurementUnits.TERABYTES);
        validateScaleAndInverse(1048576.0, MeasurementUnits.GIGABYTES, 1.0, MeasurementUnits.PETABYTES);

        validateScaleAndInverse(1024.0, MeasurementUnits.TERABYTES, 1.0, MeasurementUnits.PETABYTES);

        // Family.BITS
        validateScaleAndInverse(1024.0, MeasurementUnits.BITS, 1.0, MeasurementUnits.KILOBITS);
        validateScaleAndInverse(1048576.0, MeasurementUnits.BITS, 1.0, MeasurementUnits.MEGABITS);
        validateScaleAndInverse(1073741824.0, MeasurementUnits.BITS, 1.0, MeasurementUnits.GIGABITS);
        validateScaleAndInverse(1099511627776.0, MeasurementUnits.BITS, 1.0, MeasurementUnits.TERABITS);
        validateScaleAndInverse(1125899906842624.0, MeasurementUnits.BITS, 1.0, MeasurementUnits.PETABITS);

        validateScaleAndInverse(1024.0, MeasurementUnits.KILOBITS, 1.0, MeasurementUnits.MEGABITS);
        validateScaleAndInverse(1048576.0, MeasurementUnits.KILOBITS, 1.0, MeasurementUnits.GIGABITS);
        validateScaleAndInverse(1073741824.0, MeasurementUnits.KILOBITS, 1.0, MeasurementUnits.TERABITS);
        validateScaleAndInverse(1099511627776.0, MeasurementUnits.KILOBITS, 1.0, MeasurementUnits.PETABITS);

        validateScaleAndInverse(1024.0, MeasurementUnits.MEGABITS, 1.0, MeasurementUnits.GIGABITS);
        validateScaleAndInverse(1048576.0, MeasurementUnits.MEGABITS, 1.0, MeasurementUnits.TERABITS);
        validateScaleAndInverse(1073741824.0, MeasurementUnits.MEGABITS, 1.0, MeasurementUnits.PETABITS);

        validateScaleAndInverse(1024.0, MeasurementUnits.GIGABITS, 1.0, MeasurementUnits.TERABITS);
        validateScaleAndInverse(1048576.0, MeasurementUnits.GIGABITS, 1.0, MeasurementUnits.PETABITS);

        validateScaleAndInverse(1024.0, MeasurementUnits.TERABITS, 1.0, MeasurementUnits.PETABITS);

        // Family.DURATION
        validateScaleAndInverse(1000.0, MeasurementUnits.EPOCH_MILLISECONDS, 1.0, MeasurementUnits.EPOCH_SECONDS);

        // Family.TIME
        validateScaleAndInverse(1000.0, MeasurementUnits.MILLISECONDS, 1.0, MeasurementUnits.SECONDS);
        validateScaleAndInverse(60000.0, MeasurementUnits.MILLISECONDS, 1.0, MeasurementUnits.MINUTES);
        validateScaleAndInverse(3600000.0, MeasurementUnits.MILLISECONDS, 1.0, MeasurementUnits.HOURS);
        validateScaleAndInverse(86400000.0, MeasurementUnits.MILLISECONDS, 1.0, MeasurementUnits.DAYS);

        validateScaleAndInverse(60.0, MeasurementUnits.SECONDS, 1.0, MeasurementUnits.MINUTES);
        validateScaleAndInverse(3600.0, MeasurementUnits.SECONDS, 1.0, MeasurementUnits.HOURS);
        validateScaleAndInverse(86400.0, MeasurementUnits.SECONDS, 1.0, MeasurementUnits.DAYS);

        validateScaleAndInverse(60.0, MeasurementUnits.MINUTES, 1.0, MeasurementUnits.HOURS);
        validateScaleAndInverse(1440.0, MeasurementUnits.MINUTES, 1.0, MeasurementUnits.DAYS);

        validateScaleAndInverse(24.0, MeasurementUnits.HOURS, 1.0, MeasurementUnits.DAYS);
    }

    private void validateScaleAndInverse(double fromValue, MeasurementUnits fromUnits, double toValue,
        MeasurementUnits toUnits) {
        validateScale(fromValue, fromUnits, toValue, toUnits);
        validateScale(toValue, toUnits, fromValue, fromUnits);
    }

    private void validateScale(double fromValue, MeasurementUnits fromUnits, double toValue, MeasurementUnits toUnits) {
        MeasurementNumericValueAndUnits valueAndUnits = new MeasurementNumericValueAndUnits(fromValue, fromUnits);
        try {
            double derived = MeasurementConverter.scale(valueAndUnits, toUnits);
            assert Math.abs(toValue - derived) < 1e-9 : "Scale conversion error: " +

            "From value '" + fromValue + "', " + "with units of '" + fromUnits.name() + "', " + "displayed as '"
                + fromUnits + "', " +

                "To value '" + toValue + "', " + "with units of '" + toUnits.name() + "', " + "displayed as '"
                + toUnits + "', " +

                "Received value '" + derived + "'";

        } catch (MeasurementConversionException mce) {
            assert false : "Unexpected MeasurementConversionException: " +

            "From value '" + fromValue + "', " + "with units of '" + fromUnits.name() + "', " + "displayed as '"
                + fromUnits + "', " +

                "To value '" + toValue + "', " + "with units of '" + toUnits.name() + "', " + "displayed as '"
                + toUnits;
        }
    }

    private void validateFormatConvert(double value, MeasurementUnits units) {
        validateFormatConvert(value, value, units);
    }

    private void validateFormatConvert(double passedValue, double expectedValue, MeasurementUnits units) {
        try {
            String intermediate = MeasurementConverter.format(passedValue, units, false);
            MeasurementNumericValueAndUnits results = MeasurementParser.parse(intermediate, units);

            assert (Math.abs(results.getValue() - expectedValue) < 1e-9 && results.getUnits() == units) : "double input was '"
                + passedValue
                + "', "
                + "units input was '"
                + units.name()
                + "', "
                +

                "intermediate string was '"
                + intermediate
                + "', "
                +

                "Expected value '"
                + expectedValue
                + "', "
                + "with units of '"
                + units.name()
                + "', "
                + "displayed as '"
                + units
                + "', "
                +

                "Received value '"
                + results.getValue()
                + "', "
                + "with units of '"
                + results.getUnits().getName()
                + "', " + "displayed as '" + results.getUnits() + "'";
        } catch (MeasurementConversionException mce) {
            assert false : "Error during conversion: " + mce.getMessage();
        }
    }

    private void validateConvert(double value, String intermediate, MeasurementUnits units) {
        validateConvert(value, value, intermediate, units);
    }

    private void validateConvert(double passedValue, double expectedValue, String intermediate, MeasurementUnits units) {
        try {
            MeasurementNumericValueAndUnits results = MeasurementParser.parse(intermediate, units);

            assert (Math.abs(results.getValue() - expectedValue) < 1e-9 && results.getUnits() == units) : "double input was '"
                + passedValue
                + "', "
                + "units input was '"
                + units.name()
                + "', "
                +

                "intermediate string was '"
                + intermediate
                + "', "
                +

                "Expected value '"
                + expectedValue
                + "', "
                + "with units of '"
                + units.name()
                + "', "
                + "displayed as '"
                + units
                + "', "
                +

                "Received value '"
                + results.getValue()
                + "', "
                + "with units of '"
                + results.getUnits().getName()
                + "', " + "displayed as '" + results.getUnits() + "'";
        } catch (MeasurementConversionException mce) {
            assert false : "Error during conversion: " + mce.getMessage();
        }
    }

    private List<String> getWhitespaceVariationsAroundSingleTokens(String... tokens) {
        List<String> variations = new ArrayList<String>();

        for (String token : tokens) {
            for (String white : WHITESPACE) {
                variations.add(token);
                //variations.add( token + white );
                //variations.add( white + token );
                //variations.add( white + token + white );

                variations.add(token + white + white);
                //variations.add( white + white + token );
                //variations.add( white + white + token + white + white );
            }
        }

        return variations;
    }

}
