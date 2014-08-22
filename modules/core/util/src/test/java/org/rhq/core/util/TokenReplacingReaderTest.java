/*
 * RHQ Management Platform
 * Copyright (C) 2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.core.util;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Lukas Krejci
 * @since 4.13
 */
@Test
public class TokenReplacingReaderTest {

    public void testSimple() throws Exception {
        testExpression("Hello, ${who}!").with("who", "world").matches("Hello, world!");
    }

    public void testTokensInValues() throws Exception {
        testExpression("Hello, ${who}!").with("who", "${whose} world").with("whose", "my").matches("Hello, my world!");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testInfiniteRecursionAvoidance() throws Exception {
        testExpression("Hello, ${who}!").with("who", "${who}").parse();
    }

    public void testDefaultValue() throws Exception {
        testExpression("Hello, ${who:world}!").matches("Hello, world!");
    }

    public void testMultipleChoice() throws Exception {
        testExpression("Hello, ${blah,who}!").with("who", "world").matches("Hello, world!");
    }

    public void testMultipleChoiceWithDefaultValue() throws Exception {
        testExpression("Hello, ${blah,who:world}!").matches("Hello, world!");
    }

    public void testEscapesInFreeTextAndNames() throws Exception {
        testExpression("Hel\\lo, ${who\\} \\${escaped}!").with("who\\", "world").matches("Hel\\lo, world ${escaped}!");
    }

    public void testEscapesInDefaultValues() throws Exception {
        testExpression("Hello, ${who:world\\}}!").matches("Hello, world}!");
        testExpression("Hello, ${who:world\\}s}!").matches("Hello, world}s!");
        testExpression("Hello, ${who:\\}worlds}!").matches("Hello, }worlds!");
    }

    public void testColonAsName() throws Exception {
        testExpression("Hello, ${:}!").with(":", "world").matches("Hello, world!");
        testExpression("Hello, ${:who}!").with(":who", "world").matches("Hello, world!");
    }

    public void testDefaultValueInNestedProp() throws Exception {
        testExpression("Hello, ${who}!").with("who","${guess:world}").matches("Hello, world!");
    }
    private TestCaseBuilder testExpression(String expression) {
        return new TestCaseBuilder(expression);
    }

    private static class TestCaseBuilder {
        final Map<String, String> tokens = new HashMap<String, String>();
        final String expression;

        private TestCaseBuilder(String expression) {
            this.expression = expression;
        }

        public TestCaseBuilder with(String key, String value) {
            tokens.put(key, value);
            return this;
        }

        public String parse() throws IOException {
            TokenReplacingReader rdr = new TokenReplacingReader(new StringReader(expression), tokens);
            int c;
            StringBuilder bld = new StringBuilder();
            while((c = rdr.read()) >= 0) {
                bld.append((char)c);
            }

            return bld.toString();
        }

        public void matches(String expectedParseResult) throws IOException {
            Assert.assertEquals(parse(), expectedParseResult, expectedParseResult);
        }
    }
}
