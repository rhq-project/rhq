/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.coregui.client.admin.topology;

import com.smartgwt.client.widgets.grid.ListGridField;

import org.rhq.coregui.client.CoreGUI;

/**
 * @author Jirka Kremser
 *
 */
public enum PartitionEventDatasourceField {

    FIELD_ID("id", CoreGUI.getMessages().common_title_id()),

    FIELD_CTIME("ctime", CoreGUI.getMessages().view_adminTopology_partitionEvents_execTime()),

    FIELD_EVENT_TYPE("eventType", CoreGUI.getMessages().view_adminTopology_partitionEvents_type()),

    FIELD_EVENT_DETAIL("eventDetail", CoreGUI.getMessages().view_adminTopology_partitionEvents_details()),

    FIELD_SUBJECT_NAME("subjectName", CoreGUI.getMessages().view_adminTopology_partitionEvents_initiatedBy()),

    FIELD_EXECUTION_STATUS("executionStatus", CoreGUI.getMessages()
        .view_adminTopology_partitionEvents_executionStatus());

    /**
     * Corresponds to a property name of Server (e.g. operationMode).
     */
    private String propertyName;

    /**
     * The table header for the field or property (e.g. Mode).
     */
    private String title;

    private PartitionEventDatasourceField(String propertyName, String title) {
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

    public ListGridField getListGridField(String width) {
        ListGridField field = new ListGridField(propertyName, title);
        field.setWidth(width);
        return field;
    }
}