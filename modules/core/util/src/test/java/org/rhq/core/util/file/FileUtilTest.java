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
package org.rhq.core.util.file;

import org.testng.annotations.Test;

@Test
public class FileUtilTest {
    public void testStripDriveLetter() {
        StringBuilder str;

        str = new StringBuilder("");
        assert FileUtil.stripDriveLetter(str) == null;
        assert str.toString().equals("");

        str = new StringBuilder("\\");
        assert FileUtil.stripDriveLetter(str) == null;
        assert str.toString().equals("\\");

        str = new StringBuilder("foo");
        assert FileUtil.stripDriveLetter(str) == null;
        assert str.toString().equals("foo");

        str = new StringBuilder("foo\\bar");
        assert FileUtil.stripDriveLetter(str) == null;
        assert str.toString().equals("foo\\bar");

        str = new StringBuilder("\\foo\\bar");
        assert FileUtil.stripDriveLetter(str) == null;
        assert str.toString().equals("\\foo\\bar");

        str = new StringBuilder("C:");
        assert FileUtil.stripDriveLetter(str).equals("C");
        assert str.toString().equals("");

        str = new StringBuilder("C:\\");
        assert FileUtil.stripDriveLetter(str).equals("C");
        assert str.toString().equals("\\");

        str = new StringBuilder("C:foo");
        assert FileUtil.stripDriveLetter(str).equals("C");
        assert str.toString().equals("foo");

        str = new StringBuilder("C:foo\\bar");
        assert FileUtil.stripDriveLetter(str).equals("C");
        assert str.toString().equals("foo\\bar");

        str = new StringBuilder("C:\\foo");
        assert FileUtil.stripDriveLetter(str).equals("C");
        assert str.toString().equals("\\foo");

        str = new StringBuilder("C:\\foo\\bar");
        assert FileUtil.stripDriveLetter(str).equals("C");
        assert str.toString().equals("\\foo\\bar");

        // test all the valid drive letters
        String driveLetters = "abcdefghijklmnopqrstuvwxyz";
        String testPath = "\\foo\\bar";
        for (int i = 0; i < driveLetters.length(); i++) {
            String lowerLetter = String.valueOf(driveLetters.charAt(i));
            String upperLetter = String.valueOf(Character.toUpperCase(driveLetters.charAt(i)));
            StringBuilder lowerPath = new StringBuilder(lowerLetter + ':' + testPath);
            StringBuilder upperPath = new StringBuilder(upperLetter + ':' + testPath);

            assert FileUtil.stripDriveLetter(lowerPath).equals(lowerLetter);
            assert lowerPath.toString().equals(testPath);
            assert FileUtil.stripDriveLetter(upperPath).equals(upperLetter);
            assert upperPath.toString().equals(testPath);
        }

        // unix paths should not be affected
        str = new StringBuilder("/");
        assert FileUtil.stripDriveLetter(str) == null;
        assert str.toString().equals("/");

        str = new StringBuilder("/foo");
        assert FileUtil.stripDriveLetter(str) == null;
        assert str.toString().equals("/foo");

        str = new StringBuilder("foo/bar");
        assert FileUtil.stripDriveLetter(str) == null;
        assert str.toString().equals("foo/bar");

        str = new StringBuilder("/foo/bar");
        assert FileUtil.stripDriveLetter(str) == null;
        assert str.toString().equals("/foo/bar");

        str = new StringBuilder("hello:world/hello");
        assert FileUtil.stripDriveLetter(str) == null;
        assert str.toString().equals("hello:world/hello");
    }
}
