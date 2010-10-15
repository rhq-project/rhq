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
package org.rhq.enterprise.gui.coregui.client.inventory.groups.definitions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.criteria.ResourceGroupDefinitionCriteria;
import org.rhq.core.domain.resource.group.GroupDefinition;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGroupGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * @author Greg Hinkle
 * @author Joseph Marques
 */
public class GroupDefinitionDataSource extends RPCDataSource<GroupDefinition> {

    private ResourceGroupGWTServiceAsync groupService = GWTServiceLookup.getResourceGroupService();

    private static GroupDefinitionDataSource INSTANCE;

    public static GroupDefinitionDataSource getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new GroupDefinitionDataSource();
        }
        return INSTANCE;
    }

    private GroupDefinitionDataSource() {
        super();
        List<DataSourceField> fields = addDataSourceFields();
        addFields(fields);
    }

    @Override
    protected List<DataSourceField> addDataSourceFields() {
        List<DataSourceField> fields = super.addDataSourceFields();

        DataSourceField idField = new DataSourceIntegerField("id", "ID");
        idField.setPrimaryKey(true);
        idField.setCanEdit(false);
        fields.add(idField);

        DataSourceTextField nameField = new DataSourceTextField("name", "Name");
        nameField.setRequired(true);
        fields.add(nameField);

        DataSourceTextField descriptionField = new DataSourceTextField("description", "Description");
        fields.add(descriptionField);

        DataSourceTextField expressionField = new DataSourceTextField("expression", "Expression Set");
        expressionField.setRequired(true);
        fields.add(expressionField);

        DataSourceIntegerField lastCalculationTimeIntervalField = new DataSourceIntegerField("lastCalculationTime",
            "Recalculation Interval");
        fields.add(lastCalculationTimeIntervalField);

        DataSourceIntegerField nextCalculationTimeField = new DataSourceIntegerField("nextCalculationTime",
            "Next Calculation Time");
        fields.add(nextCalculationTimeField);

        return fields;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response) {
        ResourceGroupDefinitionCriteria criteria = new ResourceGroupDefinitionCriteria();
        criteria.setPageControl(getPageControl(request));

        groupService.findGroupDefinitionsByCriteria(criteria, new AsyncCallback<PageList<GroupDefinition>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("Failed to load group definitions", caught);
                response.setStatus(RPCResponse.STATUS_FAILURE);
                processResponse(request.getRequestId(), response);
            }

            public void onSuccess(PageList<GroupDefinition> result) {
                response.setStatus(RPCResponse.STATUS_SUCCESS);
                response.setData(buildRecords(result));
                response.setTotalRows(result.getTotalSize());
                processResponse(request.getRequestId(), response);
            }
        });
    }

    @Override
    protected void executeAdd(final DSRequest request, final DSResponse response) {
        JavaScriptObject data = request.getData();
        final ListGridRecord record = new ListGridRecord(data);
        final GroupDefinition newGroupDefinition = copyValues(record);
        final String name = newGroupDefinition.getName();

        GWTServiceLookup.getResourceGroupService().createGroupDefinition(newGroupDefinition,
            new AsyncCallback<GroupDefinition>() {
                @Override
                public void onFailure(Throwable caught) {
                    Map<String, String> errors = new HashMap<String, String>();
                    errors.put("name", "A group definition with this name already exists.");
                    response.setErrors(errors);
                    response.setStatus(RPCResponse.STATUS_VALIDATION_ERROR);
                    processResponse(request.getRequestId(), response);
                }

                @Override
                public void onSuccess(GroupDefinition result) {
                    CoreGUI.getErrorHandler().handleError("Successfully created group definition '" + name + "'");
                    response.setData(new Record[] { copyValues(result) });
                    processResponse(request.getRequestId(), response);
                }
            });
    }

    @Override
    protected void executeUpdate(final DSRequest request, final DSResponse response) {
        final ListGridRecord record = getEditedRecord(request);
        final GroupDefinition updatedGroupDefinition = copyValues(record);
        final String name = updatedGroupDefinition.getName();

        GWTServiceLookup.getResourceGroupService().updateGroupDefinition(updatedGroupDefinition,
            new AsyncCallback<Void>() {
                @Override
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError("Failure saving group definition '" + name + "'", caught);
                }

                @Override
                public void onSuccess(Void result) {
                    CoreGUI.getErrorHandler().handleError("Successfully saved group definition '" + name + "'");
                    response.setData(new Record[] { copyValues(updatedGroupDefinition) });
                    processResponse(request.getRequestId(), response);
                }
            });
    }

    @Override
    public GroupDefinition copyValues(ListGridRecord from) {
        GroupDefinition groupDefinition = new GroupDefinition();
        groupDefinition.setId(from.getAttributeAsInt("id"));
        groupDefinition.setName(from.getAttributeAsString("name"));
        groupDefinition.setDescription(from.getAttributeAsString("description"));
        groupDefinition.setExpression(from.getAttributeAsString("expression"));
        groupDefinition.setRecursive(from.getAttributeAsBoolean("recursive"));
        groupDefinition.setRecalculationInterval(Long.valueOf(from.getAttributeAsString("recalculationInterval")));
        // modifiedTime, createdTime, and lastCalculationTime are updated by GroupDefinitionManagerBean only 
        // nextCalculationTime is a non-persistent, derived field

        return groupDefinition;
    }

    @Override
    public ListGridRecord copyValues(GroupDefinition from) {
        ListGridRecord record = new ListGridRecord();
        record.setAttribute("id", from.getId());
        record.setAttribute("name", from.getName());
        record.setAttribute("description", from.getDescription());
        record.setAttribute("expression", from.getExpression());
        record.setAttribute("recursive", from.isRecursive());
        record.setAttribute("recalculationInterval", from.getRecalculationInterval());
        record.setAttribute("modifiedTime", from.getModifiedTime());
        record.setAttribute("createdTime", from.getCreatedTime());
        record.setAttribute("lastCalculationTime", from.getLastCalculationTime());
        record.setAttribute("nextCalculationTime", from.getNextCalculationTime()); // derived
        //        record.setAttribute("managedResourceGroups", from.getManagedResourceGroups());

        return record;
    }
}
