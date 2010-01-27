/*
* RHQ Management Platform
* Copyright (C) 2009 Red Hat, Inc.
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
package org.rhq.core.domain.configuration;

/**
 * Represents a single value that should be displayed to a user as an enumerated possibility for a dynamic
 * configuration property. This object pairs a display name (shown to the user in the UI) with the programmatic
 * value that is used if this value is selected.
 *
 * @author Jason Dobies
 */
public class DynamicConfigurationPropertyValue {

    /**
     * Display name to use for the value. This will be displayed to the user in the UI.
     */
    private String display;

    /**
     * Programmatic representation of the value that will be used when this value is selected by
     * the user.
     */
    private String value;

    public DynamicConfigurationPropertyValue(String display, String value) {
        this.display = display;
        this.value = value;
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Value [Display = " + display + ", Value = " + value + "]";
    }
}
