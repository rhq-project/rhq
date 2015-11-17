/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
package org.rhq.coregui.client.admin.storage;

import com.smartgwt.client.widgets.grid.ListGridField;

import org.rhq.coregui.client.CoreGUI;

/**
 * Fields for {@link StorageNodeDatasource} class.
 * 
 * @author Jirka Kremser
 *
 */
public enum StorageNodeDatasourceField {

    FIELD_ID("id", CoreGUI.getMessages().common_title_id()),

    FIELD_ADDRESS("address", CoreGUI.getMessages().view_adminTopology_server_endpointAddress()),

    FIELD_ALERTS("alerts", CoreGUI.getMessages().view_adminTopology_storageNodes_field_alerts()),

    FIELD_RHQ_REPLICATION_FACTOR("RHQ",
        CoreGUI.getMessages().view_adminTopology_storageNodes_field_rhq_replication_factor()),

    FIELD_SYSTEM_AUTH_REPLICATION_FACTOR("systemAuth",
        CoreGUI.getMessages().view_adminTopology_storageNodes_field_system_auth_replication_factor()),

    FIELD_CQL_PORT("cqlPort", CoreGUI.getMessages().view_adminTopology_storageNodes_field_cqlPort()),

    FIELD_OPERATION_MODE("operationMode", CoreGUI.getMessages().view_adminTopology_storageNodes_field_operationMode()),

    FIELD_STATUS("status", CoreGUI.getMessages().view_adminTopology_storageNodes_field_clusterStatus()),

    FIELD_AVAILABILITY("availability", CoreGUI.getMessages().common_title_availability()),

    FIELD_MEMORY("memory", CoreGUI.getMessages().view_adminTopology_storageNodes_field_memory()),

    FIELD_DISK("disk", CoreGUI.getMessages().view_adminTopology_storageNodes_field_disk()),

    FIELD_ERROR_MESSAGE("errorMessage", CoreGUI.getMessages().view_adminTopology_storageNodes_field_error()),

    FIELD_FAILED_OPERATION("failedOperation", CoreGUI.getMessages()
        .view_adminTopology_storageNodes_field_failedOperation()),

    FIELD_CTIME("ctime", CoreGUI.getMessages().view_adminTopology_serverDetail_installationDate()),

    FIELD_MTIME("mtime", CoreGUI.getMessages().view_adminTopology_server_lastUpdateTime()),

    FIELD_RESOURCE_ID("resourceId", CoreGUI.getMessages().common_title_resource());

    /**
     * Corresponds to a property name of StorageNode (e.g. operationMode).
     */
    private String propertyName;

    /**
     * The table header for the field or property (e.g. Mode).
     */
    private String title;

    private StorageNodeDatasourceField(String propertyName, String title) {
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
    
    public enum StorageNodeLoadCompositeDatasourceField { 
        FIELD_PARENT_ID("parentId", "parentId"),
        FIELD_NAME("name", CoreGUI.getMessages().common_title_metric()),
        FIELD_MIN("min", CoreGUI.getMessages().common_title_monitor_minimum()),
        FIELD_AVG("avg", CoreGUI.getMessages().common_title_monitor_average()),
        FIELD_MAX("max", CoreGUI.getMessages().common_title_monitor_maximum());

        /**
         * Corresponds to a property name of StorageNodeLoadComposite (e.g. min).
         */
        private String propertyName;

        /**
         * The table header for the field or property (e.g. Mode).
         */
        private String title;

        private StorageNodeLoadCompositeDatasourceField(String propertyName, String title) {
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
}