/*
 * RHQ Management Platform
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.rhq.server.metrics;

import java.util.Random;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Stefan Negrea
 *
 */
public class ArithmeticMeanCalculatorTest {

    private static final double TEST_PRECISION = Math.pow(10, -10);

    @Test
    public void simpleTest() {
        ArithmeticMeanCalculator objectUnderTest = new ArithmeticMeanCalculator();

        objectUnderTest.add(1);
        objectUnderTest.add(2);
        objectUnderTest.add(3);
        objectUnderTest.add(4);
        objectUnderTest.add(5);
        objectUnderTest.add(6);

        Assert.assertEquals(objectUnderTest.getArithmeticMean(), 3.5);
    }

    @Test
    public void simpleResetTest() {
        ArithmeticMeanCalculator objectUnderTest = new ArithmeticMeanCalculator();

        objectUnderTest.add(1);
        objectUnderTest.add(2);
        objectUnderTest.add(3);
        objectUnderTest.add(4);
        objectUnderTest.add(5);
        objectUnderTest.add(6);
        objectUnderTest.add(7);
        objectUnderTest.add(8);

        Assert.assertEquals(objectUnderTest.getArithmeticMean(), 4.5, TEST_PRECISION);

        objectUnderTest.reset();
        objectUnderTest.add(1);
        objectUnderTest.add(2);
        objectUnderTest.add(3);

        Assert.assertEquals(objectUnderTest.getArithmeticMean(), 2.0, TEST_PRECISION);
    }

    @Test
    public void randomNumberWithResetTest() {
        ArithmeticMeanCalculator objectUnderTest = new ArithmeticMeanCalculator();

        Random random = new Random(1243);

        for (int j = 0; j < 5; j++) {
            double sum = 0;
            objectUnderTest.reset();

            for (int i = 0; i < 123; i++) {
                double randomNumber = random.nextDouble() * 100;
                objectUnderTest.add(randomNumber);
                sum += randomNumber;
            }

            Assert.assertEquals(objectUnderTest.getArithmeticMean(), sum / 123, TEST_PRECISION);
        }
    }

}
