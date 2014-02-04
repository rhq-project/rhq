/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
package org.rhq.plugins.script;

import static org.testng.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.Test;

/**
 * @author Lukas Krejci
 * @since 4.10
 */
@Test
public class ScriptArgumentParserTest {

    public void test() {
        HashMap<String, String[]> testCases = new HashMap<String, String[]>();

        testCases.put("1   2\t3", new String[]{"1", "2", "3"});
        testCases.put("1 '2 ' \"3'\" '4'abs '5\"'", new String[]{"1", "2 ", "3'", "4abs", "5\""});
        testCases.put("\\  \\2 '3\\'\\'a'", new String[]{" ", "2", "3\\'a"});
        testCases.put("\"C:\\Program Files\\Lukas' \\\"Tests\\\"\\1\"", new String[]{"C:\\Program Files\\Lukas' \"Tests\"\\1"});

        for(Map.Entry<String, String[]> testCase : testCases.entrySet()) {
            String[] result = ScriptArgumentParser.parse(testCase.getKey(), '\\');

            assertEquals(result, testCase.getValue(), "Failed to parse [" + testCase.getKey() + "]. Expected: " + Arrays
                .asList(testCase.getValue()) + ", but got: " + Arrays.asList(result));
        }
    }
}
