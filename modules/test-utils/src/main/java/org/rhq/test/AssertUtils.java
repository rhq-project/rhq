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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class AssertUtils {

    /**
     * Verifies that all public, accessible properties of the two objects are equal. This method might be used when you
     * want to compare two objects without using their <code>equals()</code> methods or when they do not implement <code>
     * equals()</code>.
     *
     * @param expected The expected object to compare against. Should be non-null.
     * @param actual The actual object to be compared. Should be non-null.
     * @param msg An error message
     * @param <T> The type of the objects to be compared
     */
    public static <T> void assertPropertiesMatch(T expected, T actual, String msg) {
        assertPropertiesMatch(msg, expected, actual, Collections.<String>emptyList());
    }

    /**
     * @param msg An error message
     * @param expected The expected object to compare against. Should be non-null.
     * @param actual The actual object to be compared. Should be non-null.
     * @param ignoredProperties A list of property names to exclude from comparison.
     * @param <T> The type of the objects to be compared.
     */
    public static <T> void assertPropertiesMatch(String msg, T expected, T actual, String... ignoredProperties) {
        assertPropertiesMatch(msg, expected, actual, Arrays.asList(ignoredProperties));
    }

    /**
     * Verifies that all public, accessible properties of the two objects are equal, excluding those specified in the
     * <code>ignoredProperties</code> argument. This method might be used when you want to compare two objects without
     * using their <code>equals()</code> methods or when they do not implement <code>equals</code>.
     *
     * @param msg An error message
     * @param expected The expected object to compare against. Should be non-null.
     * @param actual The actual object to be compared. Should be non-null.
     * @param ignoredProperties A list of property names to exclude from comparison.
     * @param <T> The type of the objects to be compared.
     */
    public static <T> void assertPropertiesMatch(String msg, T expected, T actual, List<String> ignoredProperties) {
        assertNotNull(expected, "Expected object should not be null");
        assertNotNull(actual, "Actual object should not be null");

        PropertyMatcher<T> matcher = new PropertyMatcher<T>();
        matcher.setExpected(expected);
        matcher.setActual(actual);
        matcher.setIgnoredProperties(ignoredProperties);

        MatchResult result = matcher.execute();

        assertTrue(result.isMatch(), msg + " -- " + result.getDetails());
    }

    /**
     * Verifies that two collections are equal, according to the definition of <code>equals()</code> of the contained
     * elements. Order is ignored as well as the runtime type of the collections.
     *
     * @param expected The expected collection to compare against
     * @param actual The actual collection to compare against
     * @param msg An error message
     * @param <T> The type of the elements in the collections
     */
    public static <T> void assertCollectionEqualsNoOrder(Collection<T> expected, Collection<T> actual, String msg) {
        CollectionEqualsChecker<T> checker = new CollectionEqualsChecker<T>();
        checker.setExpected(expected);
        checker.setActual(actual);

        EqualsResult result = checker.execute();

        assertTrue(result.isEqual(), msg + " -- " + result.getDetails());
    }

    /**
     * Verifies that the two collections contain the same number of matching elements as is
     * done in {@link #assertPropertiesMatch(String, Object, Object, String...)}. If the
     * collections differ in size, an assertion error will be thrown; otherwise, elements
     * are compared, ignoring order. Note that all element properties are are compared in
     * this method.
     *
     * @param expected The expected collection to compare against
     * @param actual The actual collection under test
     * @param msg An error message
     * @param <T> The type of the elements in the collections
     */
    public static <T> void assertCollectionMatchesNoOrder(Collection<T> expected, Collection<T> actual, String msg) {
        CollectionMatchesChecker<T> checker = new CollectionMatchesChecker<T>();
        checker.setExpected(expected);
        checker.setActual(actual);

        MatchResult result = checker.execute();

        assertTrue(result.isMatch(), msg + " -- " + result.getDetails());
    }

    public static <T> void assertCollectionMatchesNoOrder(String msg, Collection<T> expected, Collection<T> actual,
        String... ignoredProperties) {
        CollectionMatchesChecker<T> checker = new CollectionMatchesChecker<T>();
        checker.setExpected(expected);
        checker.setActual(actual);
        checker.setIgnoredProperties(ignoredProperties);

        MatchResult result = checker.execute();

        assertTrue(result.isMatch(), msg + " -- " + result.getDetails());
    }

}
