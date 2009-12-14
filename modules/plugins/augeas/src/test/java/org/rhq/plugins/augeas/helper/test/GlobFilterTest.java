/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

package org.rhq.plugins.augeas.helper.test;

import static org.testng.Assert.assertEqualsNoOrder;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.testng.annotations.Test;

import org.rhq.plugins.augeas.helper.GlobFilter;

/**
 * Tests for glob implementation.
 * 
 * The tests are based on the glob pattern documentation found
 * <a href="http://www.debian.org/doc/manuals/debian-reference/ch01.en.html#_shell_glob">here</a>.
 * 
 * 
 * @author Lukas Krejci
 */
@Test
public class GlobFilterTest {

    private static class Test {
        String glob;
        String[] testFileNames;
        Set<String> expectedResults;

        Test() {

        }

        Test(String glob, String[] testFileNames, String[] expectedResults) {
            this.glob = glob;
            this.testFileNames = testFileNames;
            this.expectedResults = new HashSet<String>(Arrays.asList(expectedResults));
        }

        void execute() {
            Set<String> collectedResults = new HashSet<String>();
            GlobFilter filter = new GlobFilter(glob);

            for (String fileName : testFileNames) {
                File f = new File(fileName);
                if (filter.accept(f)) {
                    collectedResults.add(fileName);
                }
            }

            assertEqualsNoOrder(collectedResults.toArray(), expectedResults.toArray(), "The glob '" + glob
                + "' didn't match as expected.");
        }
    }

    public static final String[] defaultTestFileNames = { "/a", "/A", "/b", "/B", "/c", "/C", "/aa", "/aA", "/Aa",
        "/AA", "/.a", "/-", "/*" };

    public void testStar() {
        //this shouldn't match the ".a" file
        new Test("/*", defaultTestFileNames, new String[] { "/a", "/A", "/b", "/B", "/c", "/C", "/aa", "/aA", "/Aa",
            "/AA", "/-", "/*" }).execute();
    }

    public void testQuestionMark() {
        new Test("/?", defaultTestFileNames, new String[] { "/a", "/A", "/b", "/B", "/c", "/C", "/-", "/*" }).execute();
    }

    public void testDotFilesMatch() {
        new Test("/.*", defaultTestFileNames, new String[] { "/.a" }).execute();
    }

    public void testRanges() {
        new Test("/[a-c]*", defaultTestFileNames, new String[] { "/a", "/b", "/c", "/aa", "/aA" });
        new Test("/[-a]", defaultTestFileNames, new String[] { "/a", "/-" });
        new Test("/[*]", defaultTestFileNames, new String[] { "/*" });
    }
}
