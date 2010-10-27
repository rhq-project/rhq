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

package org.rhq.enterprise.gui.coregui.client.inventory.resource;

import com.smartgwt.client.widgets.grid.ListGridField;

public enum ResourceDataSourceField {

    NAME("name", "Name"),

    DESCRIPTION("description", "Description"),

    TYPE("resourceType.name", "Type"),

    PLUGIN("pluginName", "Plugin"),

    CATEGORY("resourceType.category", "Category"),

    AVAILABILITY("currentAvailability", "Availability");

    /**
     * Corresponds to a property name of Resource (e.g. resourceType.name).
     */
    private String propertyName;

    /**
     * The table header for the field or property (e.g. Type).
     */
    private String title;

    private ResourceDataSourceField(String propertyName, String title) {
        this.propertyName = propertyName;
        this.title = title;
    }

    public String propertyName() {
        return propertyName;
    }

    public String title() {
        return title;
    }

    public ListGridField getListGridField() {
        return new ListGridField(propertyName, title);
    }

    public ListGridField getListGridField(int width) {
        return new ListGridField(propertyName, title, width);
    }

}
