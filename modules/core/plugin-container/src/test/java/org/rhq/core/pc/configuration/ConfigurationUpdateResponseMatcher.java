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

package org.rhq.core.pc.configuration;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.rhq.core.clientapi.server.configuration.ConfigurationUpdateResponse;

public class ConfigurationUpdateResponseMatcher extends TypeSafeMatcher<ConfigurationUpdateResponse> {

    public static Matcher<ConfigurationUpdateResponse> matchingResponse(ConfigurationUpdateResponse expected) {
        return new ConfigurationUpdateResponseMatcher(expected);
    }

    private ConfigurationUpdateResponse expected;

    public ConfigurationUpdateResponseMatcher(ConfigurationUpdateResponse response) {
        expected = response;
    }

    public boolean matchesSafely(ConfigurationUpdateResponse actual) {
            return propertyEquals(expected.getConfiguration(), actual.getConfiguration()) &&
                   propertyEquals(expected.getConfigurationUpdateId(), actual.getConfigurationUpdateId()) &&
                   propertyEquals(expected.getErrorMessage(), actual.getErrorMessage()) &&
                   propertyEquals(expected.getStatus(), actual.getStatus());
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

    public void describeTo(Description description) {
        description.appendText(expected.toString());
    }
}
