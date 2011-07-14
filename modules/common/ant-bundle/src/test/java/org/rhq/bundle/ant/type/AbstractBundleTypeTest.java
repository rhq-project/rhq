/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

package org.rhq.bundle.ant.type;

import java.util.regex.Pattern;

import org.testng.annotations.Test;

@Test
public class AbstractBundleTypeTest {
    public void testGetPattern() {
        Pattern regex;

        regex = assertIncludePatternRegex("easy.txt", "easy\\.txt", "easy.txt");
        assert !regex.matcher("easyXtxt").matches() : "should not have matched, is the . escaped?";
        assert !regex.matcher("Xeasy.txt").matches();
        assert !regex.matcher("easy.txtX").matches();
        assert !regex.matcher("/easy.txt").matches();
        assert !regex.matcher("easy.txt/").matches();

        regex = assertIncludePatternRegex("another.one/easy.txt", "another\\.one/easy\\.txt", "another.one/easy.txt");
        assert !regex.matcher("anotherXone/easyXtxt").matches() : "should not have matched, is the . escaped?";
        assert !regex.matcher("another.one/easy.txtX").matches();
        assert !regex.matcher("another.one/easy.txt/X").matches();
        assert !regex.matcher("Xanother.one/easy.txt").matches();
        assert !regex.matcher("X/another.one/easy.txt").matches();
        assert !regex.matcher("/another.one/easy.txt").matches();
        assert !regex.matcher("another.one/easy.txt/").matches();

        regex = assertIncludePatternRegex("*.properties", "[^/]*\\.properties", "any.properties");
        assert !regex.matcher("anyXproperties").matches() : "should not have matched, is the . escaped?";
        assert regex.matcher("with space.properties").matches();
        assert !regex.matcher("subdir/any.properties").matches();
        assert !regex.matcher("any.properties/foo").matches();

        regex = assertIncludePatternRegex("a*b.properties", "a[^/]*b\\.properties", "a123b.properties");
        assert !regex.matcher("a123bXproperties").matches() : "should not have matched, is the . escaped?";
        assert regex.matcher("a   b.properties").matches();
        assert regex.matcher("ab.properties").matches();
        assert !regex.matcher("subdir/a123b.properties").matches();
        assert !regex.matcher("a123b.properties/foo").matches();

        regex = assertIncludePatternRegex("/*.properties", "/[^/]*\\.properties", "/any.properties");
        assert !regex.matcher("/anyXproperties").matches() : "should not have matched, is the . escaped?";
        assert regex.matcher("/with space.properties").matches();
        assert !regex.matcher("subdir/any.properties").matches();
        assert !regex.matcher("/any.properties/foo").matches();

        regex = assertIncludePatternRegex("?.properties", ".\\.properties", "a.properties");
        assert !regex.matcher("aXproperties").matches() : "should not have matched, is the . escaped?";
        assert !regex.matcher("ab.properties").matches();
        assert regex.matcher(" .properties").matches();
        assert !regex.matcher(".properties").matches();
        assert !regex.matcher("subdir/a.properties").matches();
        assert !regex.matcher("a.properties/foo").matches();

        regex = assertIncludePatternRegex("a?b.properties", "a.b\\.properties", "aNb.properties");
        assert !regex.matcher("aNbXproperties").matches() : "should not have matched, is the . escaped?";
        assert regex.matcher("a b.properties").matches();
        assert !regex.matcher("ab.properties").matches();
        assert !regex.matcher("subdir/aNb.properties").matches();
        assert !regex.matcher("aNb.properties/foo").matches();

        regex = assertIncludePatternRegex("/?.properties", "/.\\.properties", "/a.properties");
        assert !regex.matcher("/aXproperties").matches() : "should not have matched, is the . escaped?";
        assert !regex.matcher("/ab.properties").matches();
        assert regex.matcher("/ .properties").matches();
        assert !regex.matcher("/.properties").matches();
        assert !regex.matcher("subdir/a.properties").matches();
        assert !regex.matcher("/a.properties/foo").matches();

        regex = assertIncludePatternRegex("file*.xml", "file[^/]*\\.xml", "fileANY.xml");
        assert !regex.matcher("fileANYXxml").matches() : "should not have matched, is the . escaped?";
        assert regex.matcher("fileWITH SPACE.xml").matches();
        assert !regex.matcher("subdir/fileANY.xml").matches();
        assert !regex.matcher("fileANY.xml/foo").matches();
        assert regex.matcher("file.xml").matches();

        regex = assertIncludePatternRegex("*/*.properties", "[^/]*/[^/]*\\.properties", "aaa/bbb.properties");
        assert !regex.matcher("aaa/bbbXproperties").matches() : "should not have matched, is the . escaped?";
        assert !regex.matcher("bbb.properties").matches();
        assert regex.matcher("subdir1/abc.properties").matches();
        assert !regex.matcher("subdir2/subdir1/abc.properties").matches();
        assert !regex.matcher("subdir1/abc.properties/foo").matches();

        regex = assertIncludePatternRegex("*/*/*.properties", "[^/]*/[^/]*/[^/]*\\.properties",
            "aaa/bbb/ccc.properties");
        assert !regex.matcher("aaa/bbb/cccXproperties").matches() : "should not have matched, is the . escaped?";
        assert !regex.matcher("ccc.properties").matches();
        assert !regex.matcher("bbb/ccc.properties").matches();
        assert !regex.matcher("toomany/aaa/bbb/ccc.properties").matches();
        assert !regex.matcher("aaa/bbb/ccc.properties/foo").matches();

        // ** = any level, including root level
        regex = assertIncludePatternRegex("**/*.properties", ".*[^/]*\\.properties", "aaa/bbb/ccc.properties");
        assert !regex.matcher("aaa/bbb/cccXproperties").matches() : "should not have matched, is the . escaped?";
        assert regex.matcher("bbb.properties").matches();
        assert regex.matcher("subdir1/abc.properties").matches();
        assert regex.matcher("subdir2/subdir1/abc.properties").matches();
        assert !regex.matcher("subdir1/abc.properties/foo").matches();

        regex = assertIncludePatternRegex("abc/**/*.properties", "abc/.*[^/]*\\.properties", "abc/aaa/bbb.properties");
        assert !regex.matcher("abc/aaa/bbbXproperties").matches() : "should not have matched, is the . escaped?";
        assert !regex.matcher("bbb.properties").matches();
        assert regex.matcher("abc/aaa.properties").matches();
        assert regex.matcher("abc/aaa/bbb/ccc.properties").matches();
        assert !regex.matcher("abc/aaa/bbb/ccc.properties/foo").matches();

        regex = assertIncludePatternRegex("abc/**/xyz/*.properties", "abc/.*xyz/[^/]*\\.properties",
            "abc/aaa/xyz/bbb.properties");
        assert !regex.matcher("abc/aaa/xyz/bbbXproperties").matches() : "should not have matched, is the . escaped?";
        assert !regex.matcher("bbb.properties").matches();
        assert regex.matcher("abc/xyz/ccc.properties").matches();
        assert regex.matcher("abc/aaa/bbb/xyz/ccc.properties").matches();
        assert !regex.matcher("abc/aaa/bbb/xyz/ccc.properties/foo").matches();
    }

    /**
     * @param patternToTest the include pattern to test - this is the string in the ant script like "*.properties"
     * @param expectedRegex this is the regex pattern that we expect to result from the patternToTest
     * @param valueToTest this is a value that should match the expectedRegex
     * @return the compiled Pattern, for further testing by the caller
     */
    private Pattern assertIncludePatternRegex(String patternToTest, String expectedRegex, String valueToTest) {
        StringBuilder regex = new StringBuilder();
        AbstractBundleType.buildIncludePatternRegex(patternToTest, regex);
        assert regex.toString().equals(expectedRegex) : "Tested={" + patternToTest + "}; Expected={" + expectedRegex
            + "}; Actual={" + regex + "}";

        assert Pattern.compile(regex.toString()).matcher(valueToTest).matches() : "The ExpectedRegex {" + expectedRegex
            + "} did not match the ValueToTest {" + valueToTest + "} - this unit test does not appear valid";

        return Pattern.compile(regex.toString());
    }
}
