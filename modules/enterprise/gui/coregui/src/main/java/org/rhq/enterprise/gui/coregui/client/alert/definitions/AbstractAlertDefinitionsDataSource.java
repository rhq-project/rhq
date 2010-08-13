/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.alert.definitions;

import java.util.Date;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.types.FieldType;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.criteria.AlertDefinitionCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public abstract class AbstractAlertDefinitionsDataSource extends RPCDataSource<AlertDefinition> {

    protected static final String FIELD_ID = "id";
    protected static final String FIELD_NAME = "name";
    protected static final String FIELD_DESCRIPTION = "description";
    protected static final String FIELD_CTIME = "ctime";
    protected static final String FIELD_MTIME = "mtime";
    protected static final String FIELD_ENABLED = "enabled";
    protected static final String FIELD_DELETED = "deleted";
    protected static final String FIELD_PRIORITY = "priority"; // not the actual object; a string for the UI
    protected static final String FIELD_PRIORITY_ENUM = "priority_enum"; // the actual enum name

    public AbstractAlertDefinitionsDataSource() {
        setupFields();
    }

    @Override
    public AlertDefinition copyValues(ListGridRecord from) {
        AlertDefinition alertDef = new AlertDefinition();
        alertDef.setId(from.getAttributeAsInt(FIELD_ID));
        alertDef.setName(from.getAttributeAsString(FIELD_NAME));
        alertDef.setDescription(from.getAttributeAsString(FIELD_DESCRIPTION));
        alertDef.setCtime(from.getAttributeAsDate(FIELD_CTIME).getTime());
        alertDef.setMtime(from.getAttributeAsDate(FIELD_MTIME).getTime());
        alertDef.setEnabled(from.getAttributeAsBoolean(FIELD_ENABLED));
        alertDef.setPriority(AlertPriority.valueOf(from.getAttributeAsString(FIELD_PRIORITY_ENUM)));
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
        record.setAttribute(FIELD_ENABLED, from.getEnabled());
        record.setAttribute(FIELD_DELETED, from.getDeleted());
        record.setAttribute(FIELD_PRIORITY, from.getPriority().getDisplayName());
        record.setAttribute(FIELD_PRIORITY_ENUM, from.getPriority().name());
        return record;
    }

    /**
     * Sets up some basic alert definition fields for this data source. Subclasses are
     * free to call this method and then add more, or add their own custom set and not
     * call this method at all (if they don't want some of these basic fields or want to reorder them).
     */
    protected void setupFields() {
        DataSourceTextField nameField = new DataSourceTextField(FIELD_NAME, "Name");
        addField(nameField);

        DataSourceTextField descriptionField = new DataSourceTextField(FIELD_DESCRIPTION, "Description");
        addField(descriptionField);

        DataSourceTextField ctimeField = new DataSourceTextField(FIELD_CTIME, "Created Time");
        ctimeField.setType(FieldType.DATETIME);
        addField(ctimeField);

        DataSourceTextField mtimeField = new DataSourceTextField(FIELD_MTIME, "Modified Time");
        mtimeField.setType(FieldType.DATETIME);
        addField(mtimeField);

        DataSourceTextField enabledField = new DataSourceTextField(FIELD_ENABLED, "Enabled");
        enabledField.setType(FieldType.BOOLEAN);
        addField(enabledField);

        DataSourceTextField priorityField = new DataSourceTextField(FIELD_PRIORITY, "Priority");
        addField(priorityField);
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response) {
        AlertDefinitionCriteria criteria = getCriteria(request);
        GWTServiceLookup.getAlertService().findAlertDefinitionsByCriteria(criteria,
            new AsyncCallback<PageList<AlertDefinition>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError("Failed to load alert definition data", caught);
                    response.setStatus(DSResponse.STATUS_FAILURE);
                    processResponse(request.getRequestId(), response);
                }

                public void onSuccess(PageList<AlertDefinition> result) {
                    response.setData(buildRecords(result));
                    processResponse(request.getRequestId(), response);
                }
            });
    }

    protected abstract AlertDefinitionCriteria getCriteria(DSRequest request);
}
