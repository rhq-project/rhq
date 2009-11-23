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

package org.rhq.test.jmock;

import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.Description;
import org.rhq.test.MatchResult;

public class PropertyMatcher<T> extends TypeSafeMatcher<T> {

    private T expected;

    public PropertyMatcher(T expected) {
        this.expected = expected;
    }

    public boolean matchesSafely(T actual) {
        org.rhq.test.PropertyMatcher<T> matcher = new org.rhq.test.PropertyMatcher<T>();
        matcher.setExpected(expected);
        matcher.setActual(actual);

        MatchResult result = matcher.execute();

        return result.isMatch();
    }

    public void describeTo(Description description) {
        description.appendText(expected.toString());
    }
}
