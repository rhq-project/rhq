/*
 * RHQ Management Platform
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
package org.rhq.core.domain.util;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

/**
 * Tests for {@link StringUtils}.
 *
 * @author Ian Springer
 */
public class StringUtilsTest {
    @Test
    public void testDeCamelCase() {
        testDeCamelCase("RedGreenBlue", "Red Green Blue");
        testDeCamelCase("redGreenBlue", "Red Green Blue");
        testDeCamelCase("Red Green Blue", "Red Green Blue");
        testDeCamelCase("red green blue", "Red Green Blue");
        testDeCamelCase("RHQServer", "RHQ Server");
        testDeCamelCase("Blink182", "Blink 182");
        testDeCamelCase("SimonAndGarfunkel", "Simon and Garfunkel");
        testDeCamelCase("myURL", "My URL");
    }

    private void testDeCamelCase(String input, String expectedResult) {
        String result = StringUtils.deCamelCase(input);
        assertEquals(result, expectedResult, "For input \"" + input + "\": ");
    }
}
