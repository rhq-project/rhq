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
package org.rhq.modules.plugins.jbossas7.helper;

/**
 * Plugin container properties.
 *
 * @author Stefan Negrea
 */
public class PluginContainerProperties {

    private static final String AVAILABILITY_FACET_TIMEOUT_SYSPROP = "rhq.agent.plugins.availability-scan.timeout";
    private static final int DEFAULT_AVAILABILITY_FACET_TIMEOUT = 5000; // millis

    // prevent instantiation
    private PluginContainerProperties() {
    }

    public static int getAvailabilityFacetTimeout() {
        int timeout;
        try {
            timeout = Integer.parseInt(System.getProperty(AVAILABILITY_FACET_TIMEOUT_SYSPROP,
                String.valueOf(DEFAULT_AVAILABILITY_FACET_TIMEOUT)));
        } catch (NumberFormatException e) {
            timeout = DEFAULT_AVAILABILITY_FACET_TIMEOUT;
        }
        return timeout;
    }

    public static int getAvailabilityFacetTimeoutSeconds() {
        return getAvailabilityFacetTimeout() / 1000;
    }
}