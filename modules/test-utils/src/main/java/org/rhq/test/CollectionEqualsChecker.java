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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CollectionEqualsChecker<T> {

    private Collection<T> expected;

    private Collection<T> actual;

    public void setExpected(Collection<T> expected) {
        this.expected = expected;
    }

    public void setActual(Collection<T> actual) {
        this.actual = actual;
    }

    public EqualsResult execute() {
        boolean isMatch = true;
        StringBuilder details = new StringBuilder();

        if (expected.size() != actual.size()) {
            isMatch = false;
            details.append("Expected " + expected.size() + " elements but found " + actual.size() + " elements\n");
        }

        List<T> elementsThatShouldBePresent = findMissingElements(expected, actual);
        if (!elementsThatShouldBePresent.isEmpty()) {
            isMatch = false;
            details.append("Expected to find the following elements:\n\t" +
                toString(elementsThatShouldBePresent) + "\n\n");
        }

        List<T> elementsThatShouldNotBePresent = findMissingElements(actual, expected);
        if (!elementsThatShouldNotBePresent.isEmpty()) {
            isMatch = false;
            details.append("Did not expect to find the following elements:\n\t" +
                toString(elementsThatShouldNotBePresent) + "\n\n");
        }

        return new EqualsResult(isMatch, details.toString());
    }

    private List<T> findMissingElements(Collection<T> elementsToSearchFor, Collection<T> elementsToSearch) {
        List<T> missingElements = new ArrayList<T>();
        for (T element : elementsToSearchFor) {
            if (!elementsToSearch.contains(element)) {
                missingElements.add(element);
            }
        }
        return missingElements;
    }

    private String toString(List<T> list) {
        StringBuilder buffer = new StringBuilder("[");
        for (T element : list) {
            buffer.append(element.toString() + ", ");
        }
        buffer.delete(buffer.length() - 2, buffer.length());
        buffer.append("]");

        return buffer.toString();
    }

}
