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
package org.rhq.enterprise.gui.coregui.client.admin.storage;

import com.smartgwt.client.widgets.grid.ListGridField;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;

/**
 * @author Jirka Kremser
 *
 */
public enum StorageNodeDatasourceField {

    FIELD_ID("id", CoreGUI.getMessages().common_title_id()),
    
    FIELD_ADDRESS("address", CoreGUI.getMessages().view_adminTopology_server_endpointAddress()),

    FIELD_JMX_PORT("jmxPort", "JMX Port"),
    
    FIELD_ALERTS("alerts", "Alerts"),
    
    FIELD_CQL_PORT("cqlPort", "CQL Port"),

    FIELD_OPERATION_MODE("operationMode", CoreGUI.getMessages().view_adminTopology_server_mode()),
    
    FIELD_STATUS("status", "Status"),
    
    FIELD_MEMORY("memory", "Memory"),
    
    FIELD_DISK("disk", "Disk"),
    
    FIELD_ERROR_MESSAGE("errorMessage", "Error"),
    
    FIELD_FAILED_OPERATION("failedOperation", "Failed Operation"),

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
        
        
    

//        private MeasurementAggregateWithUnits heapCommitted;
//        private MeasurementAggregateWithUnits heapUsed;
//        private MeasurementAggregateWithUnits heapPercentageUsed;
//        private MeasurementAggregateWithUnits load;
//        private MeasurementAggregate tokens; 
//        private MeasurementAggregateWithUnits actuallyOwns;
        
        
        FIELD_PARENT_ID("parentId", "parentId"),
        FIELD_NAME("name", CoreGUI.getMessages().common_title_metric()),
        FIELD_MIN("min", CoreGUI.getMessages().view_resource_monitor_table_min()),
        FIELD_AVG("avg", CoreGUI.getMessages().view_resource_monitor_table_avg()),
        FIELD_MAX("max", CoreGUI.getMessages().view_resource_monitor_table_max());


        
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