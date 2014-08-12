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
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.coregui.client.alert.definitions;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceImageField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.FieldType;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.widgets.grid.HoverCustomizer;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.criteria.AlertDefinitionCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.ImageManager;
import org.rhq.coregui.client.components.table.EscapedHtmlCellFormatter;
import org.rhq.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.RPCDataSource;

/**
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public abstract class AbstractAlertDefinitionsDataSource extends
    RPCDataSource<AlertDefinition, AlertDefinitionCriteria> {

    protected static final String FIELD_ID = "id";
    protected static final String FIELD_NAME = "name";
    protected static final String FIELD_DESCRIPTION = "description";
    protected static final String FIELD_CTIME = "ctime";
    protected static final String FIELD_MTIME = "mtime";
    protected static final String FIELD_ENABLED = "enabled";
    protected static final String FIELD_DELETED = "deleted";
    protected static final String FIELD_PRIORITY = "priority"; // an image URL
    protected static final String FIELD_OBJECT = "_object"; // the actual AlertDefinition object

    public AbstractAlertDefinitionsDataSource() {
        super();
        List<DataSourceField> fields = addDataSourceFields();
        addFields(fields);
    }

    /**
     * The view that contains the list grid which will display this datasource's data will call this
     * method to get the field information which is used to control the display of the data.
     * 
     * @return list grid fields used to display the datasource data
     */
    public ArrayList<ListGridField> getListGridFields() {
        ArrayList<ListGridField> fields = new ArrayList<ListGridField>(6);

        ListGridField nameField = new ListGridField(FIELD_NAME, MSG.common_title_name());
        nameField.setWidth("20%");
        nameField.setCellFormatter(new EscapedHtmlCellFormatter());
        fields.add(nameField);

        ListGridField descriptionField = new ListGridField(FIELD_DESCRIPTION, MSG.common_title_description());
        descriptionField.setWidth("20%");
        descriptionField.setCellFormatter(new EscapedHtmlCellFormatter());
        fields.add(descriptionField);

        ListGridField ctimeField = new ListGridField(FIELD_CTIME, MSG.common_title_createTime());
        ctimeField.setType(ListGridFieldType.DATE);
        TimestampCellFormatter.prepareDateField(ctimeField);
        ctimeField.setWidth("15%");
        fields.add(ctimeField);

        ListGridField mtimeField = new ListGridField(FIELD_MTIME, MSG.view_alerts_field_modified_time());
        mtimeField.setType(ListGridFieldType.DATE);
        TimestampCellFormatter.prepareDateField(mtimeField);
        mtimeField.setWidth("15%");
        fields.add(mtimeField);

        ListGridField enabledField = new ListGridField(FIELD_ENABLED, MSG.view_alerts_field_enabled());
        enabledField.setType(ListGridFieldType.IMAGE);
        enabledField.setAlign(Alignment.CENTER);
        enabledField.setWidth(60);
        fields.add(enabledField);

        ListGridField priorityField = new ListGridField(FIELD_PRIORITY, MSG.view_alerts_field_priority());
        priorityField.setType(ListGridFieldType.IMAGE);
        priorityField.setWidth(60);
        priorityField.setAlign(Alignment.CENTER);
        priorityField.setShowHover(true);
        priorityField.setHoverCustomizer(new HoverCustomizer() {
            @Override
            public String hoverHTML(Object value, ListGridRecord record, int rowNum, int colNum) {
                AlertDefinition alertDef = (AlertDefinition) record.getAttributeAsObject(FIELD_OBJECT);
                switch (alertDef.getPriority()) {
                case HIGH: {
                    return MSG.common_alert_high();
                }
                case MEDIUM: {
                    return MSG.common_alert_medium();
                }
                case LOW: {
                    return MSG.common_alert_low();
                }
                }
                return ""; // will never get here
            }
        });
        fields.add(priorityField);

        return fields;
    }

    @Override
    public AlertDefinition copyValues(Record from) {
        AlertDefinition alertDef = (AlertDefinition) from.getAttributeAsObject(FIELD_OBJECT);
        return alertDef;
    }

    @Override
    public ListGridRecord copyValues(AlertDefinition from) {
        ListGridRecord record = new ListGridRecord();
        record.setAttribute(FIELD_ID, from.getId());
        record.setAttribute(FIELD_NAME, from.getName());
        record.setAttribute(FIELD_DESCRIPTION, from.getDescription());
        record.setAttribute(FIELD_CTIME, new Date(from.getCtime()));
        record.setAttribute(FIELD_MTIME, new Date(from.getMtime()));
        record.setAttribute(FIELD_ENABLED, ImageManager.getAvailabilityIcon(from.getEnabled()));
        record.setAttribute(FIELD_DELETED, from.getDeleted());
        record.setAttribute(FIELD_PRIORITY, ImageManager.getAlertIcon(from.getPriority()));
        record.setAttribute(FIELD_OBJECT, from);
        return record;
    }

    /**
     * Sets up some basic alert definition fields for this data source. Subclasses are
     * free to call this method and then add more, or add their own custom set and not
     * call this method at all (if they don't want some of these basic fields or want to reorder them).
     */
    protected List<DataSourceField> addDataSourceFields() {
        List<DataSourceField> fields = super.addDataSourceFields();

        DataSourceTextField nameField = new DataSourceTextField(FIELD_NAME, MSG.common_title_name());
        fields.add(nameField);

        DataSourceTextField descriptionField = new DataSourceTextField(FIELD_DESCRIPTION, MSG
            .common_title_description());
        fields.add(descriptionField);

        DataSourceTextField ctimeField = new DataSourceTextField(FIELD_CTIME, MSG.common_title_createTime());
        ctimeField.setType(FieldType.DATETIME);
        fields.add(ctimeField);

        DataSourceTextField mtimeField = new DataSourceTextField(FIELD_MTIME, MSG.view_alerts_field_modified_time());
        mtimeField.setType(FieldType.DATETIME);
        fields.add(mtimeField);

        DataSourceImageField enabledField = new DataSourceImageField(FIELD_ENABLED, MSG.view_alerts_field_enabled());
        fields.add(enabledField);

        DataSourceImageField priorityField = new DataSourceImageField(FIELD_PRIORITY, MSG.view_alerts_field_priority());
        fields.add(priorityField);

        return fields;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response,
        final AlertDefinitionCriteria criteria) {
        GWTServiceLookup.getAlertDefinitionService().findAlertDefinitionsByCriteria(criteria,
            new AsyncCallback<PageList<AlertDefinition>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_alert_definitions_loadFailed(), caught);
                    response.setStatus(DSResponse.STATUS_FAILURE);
                    processResponse(request.getRequestId(), response);
                }

                public void onSuccess(PageList<AlertDefinition> result) {
                    setPagingInfo(response, result);
                    dataRetrieved(result, response, request);
                }
            });
    }

    protected void dataRetrieved(final PageList<AlertDefinition> result, final DSResponse response,
        final DSRequest request) {

        response.setData(buildRecords(result));
        processResponse(request.getRequestId(), response);
    }

    /**
     * Returns a criteria that will query for all alerts, but only for the ID and name fields. 
     * @return criteria for an inexpensive query to obtain all alert defs
     */
    protected abstract AlertDefinitionCriteria getSimpleCriteriaForAll();
}
