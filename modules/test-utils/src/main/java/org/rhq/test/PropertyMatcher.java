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

import org.apache.commons.beanutils.PropertyUtils;

import java.util.Map;
import java.lang.reflect.InvocationTargetException;

public class PropertyMatcher<T> {

    private T expected;

    private T actual;

    public void setExpected(T expected) {
        this.expected = expected;
    }

    public void setActual(T actual) {
        this.actual = actual;
    }

    public MatchResult execute() {
        try {
            Map<String, ?> expectedProps = PropertyUtils.describe(expected);
            Map<String, ?> actualProps = PropertyUtils.describe(actual);

            boolean isMatch = true;
            StringBuilder details = new StringBuilder(expected.getClass().getSimpleName() +
                " objects do not match:\n");

            for (String name : expectedProps.keySet()) {
                Object expectedValue = expectedProps.get(name);
                Object actualValue = actualProps.get(name);

                if (!propertyEquals(expectedValue, actualValue)) {
                    isMatch = false;
                    details.append("expected." + name + " = " + expectedValue + "\n");
                    details.append("actual." + name + " = " + actualValue + "\n\n");
                }
            }

            return new MatchResult(isMatch, details.toString());
        }
        catch (IllegalAccessException e) {
            throw new PropertyMatchException(e);
        }
        catch (InvocationTargetException e) {
            throw new PropertyMatchException(e);
        }
        catch (NoSuchMethodException e) {
            throw new PropertyMatchException(e);
        }
    }

    private boolean propertyEquals(Object expected, Object actual) {
        if (expected == null && actual == null) {
            return true;
        }

        if (expected == null && actual != null) {
            return false;
        }
        return expected.equals(actual);
    }
}
