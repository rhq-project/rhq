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

package org.rhq.test;

import static org.testng.Assert.*;

public class AssertUtils {

    /**
     * Verifies that all public, accessible properties of the two objects are equal. This method might be used when you
     * want to compare two objects without using their <code>equals()</code> methods or when do not implement <code>
     * equals()</code>.
     *
     * @param expected The expected object to compare against
     * @param actual The actual object to be compared
     * @param msg An error message
     * @param <T> The type of the objects to be compared
     */
    public static <T> void assertPropertiesMatch(T expected, T actual, String msg) {
        PropertyMatcher<T> matcher = new PropertyMatcher<T>();
        matcher.setExpected(expected);
        matcher.setActual(actual);

        MatchResult result = matcher.execute();

        assertTrue(result.isMatch(), msg + " -- " + result.getDetails());
    }

}
