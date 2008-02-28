/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.core.domain.alert;

public enum AlertConditionCategory {
    AVAILABILITY("Resource Availability"), // 
    THRESHOLD("Measurement Threshold"), //
    BASELINE("Measurement Baseline"), //
    CHANGE("Measurement Value Change"), //
    TRAIT("Measurement Trait"), //
    CONTROL("Control Action"), //
    ALERT("Alert Fired"), //
    CONFIGURATION_PROPERTY("Configuration Property Value Change"), // used to be called custom property
    EVENT("Log Event");

    /*
     * legacyOrder exists to support code that still uses the old EventConstants.TYPE_* attributes, which was one-based
     */
    private static final AlertConditionCategory[] legacyOrder = new AlertConditionCategory[] { THRESHOLD, BASELINE,
        CONTROL, CHANGE, ALERT, CONFIGURATION_PROPERTY, EVENT };

    // return new type-safe enum based off legacy index w/offset
    public static AlertConditionCategory make(int legacyIndex) {
        // legacy index was one-based, our legacyOrder is zero-based
        return legacyOrder[legacyIndex - 1];
    }

    private final String displayName;

    AlertConditionCategory(String displayName) {
        this.displayName = displayName;
    }

    /**
     * A Java bean style getter to allow us to access the enum name from JSPs
     *
     * @return the enum name
     */
    public String getName() {
        return name();
    }

    @Override
    public String toString() {
        return this.displayName;
    }
}