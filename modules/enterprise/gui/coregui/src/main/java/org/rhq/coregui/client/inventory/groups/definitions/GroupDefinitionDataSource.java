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
package org.rhq.coregui.client.inventory.groups.definitions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.rhq.core.domain.resource.group.DuplicateExpressionTypeException;
import org.rhq.core.domain.resource.group.GroupDefinition;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.gwt.ResourceGroupGWTServiceAsync;
import org.rhq.coregui.client.util.RPCDataSource;
import org.rhq.coregui.client.util.message.Message;

/**
 * @author Greg Hinkle
 * @author Joseph Marques
 */
public class GroupDefinitionDataSource extends RPCDataSource<GroupDefinition, ResourceGroupDefinitionCriteria> {

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

        DataSourceField idField = new DataSourceIntegerField("id", MSG.common_title_id());
        idField.setPrimaryKey(true);
        idField.setCanEdit(false);
        fields.add(idField);

        DataSourceTextField nameField = new DataSourceTextField("name", MSG.common_title_name(), 100, true);
        fields.add(nameField);

        DataSourceTextField descriptionField = new DataSourceTextField("description", MSG.common_title_description(),
            100);
        fields.add(descriptionField);

        DataSourceTextField expressionField = new DataSourceTextField("expression", MSG.view_dynagroup_expressionSet(),
            1000, true);
        fields.add(expressionField);

        // it is a Long, but there is no DataSourceLongField and I've seen problems trying to use anything other than text field
        DataSourceTextField lastCalculationTimeIntervalField = new DataSourceTextField("lastCalculationTime",
            MSG.view_dynagroup_lastCalculationTime());
        fields.add(lastCalculationTimeIntervalField);

        // it is a Long, but there is no DataSourceLongField and I've seen problems trying to use anything other than text field
        DataSourceTextField nextCalculationTimeField = new DataSourceTextField("nextCalculationTime",
            MSG.view_dynagroup_nextCalculationTime());
        fields.add(nextCalculationTimeField);

        DataSourceTextField cannedExpressionField = new DataSourceTextField("cannedExpression", "Origin");
        fields.add(cannedExpressionField);

        return fields;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response,
        final ResourceGroupDefinitionCriteria criteria) {
        groupService.findGroupDefinitionsByCriteria(criteria, new AsyncCallback<PageList<GroupDefinition>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_dynagroup_definitionLoadFailure(), caught);
                response.setStatus(RPCResponse.STATUS_FAILURE);
                processResponse(request.getRequestId(), response);
            }

            public void onSuccess(PageList<GroupDefinition> result) {
                sendSuccessResponse(request, response, result);
            }
        });
    }

    @Override
    protected ResourceGroupDefinitionCriteria getFetchCriteria(final DSRequest request) {
        ResourceGroupDefinitionCriteria criteria = new ResourceGroupDefinitionCriteria();
        return criteria;
    }

    @Override
    protected void executeAdd(Record recordToAdd, final DSRequest request, final DSResponse response) {
        final GroupDefinition newGroupDefinition = copyValues(recordToAdd);
        final String name = newGroupDefinition.getName();

        GWTServiceLookup.getResourceGroupService().createGroupDefinition(newGroupDefinition,
            new AsyncCallback<GroupDefinition>() {
                @Override
                public void onFailure(Throwable caught) {
                    Map<String, String> errors = new HashMap<String, String>();
                    String msg = caught.getMessage();
                    String cannotParse = "Cannot parse the expression: ";
                    if (msg != null && msg.contains(cannotParse)) {
                        errors.put("expression", msg.substring(msg.indexOf(cannotParse) + cannotParse.length()));
                    } else {
                        errors.put("name", MSG.view_dynagroup_definitionAlreadyExists());
                    }
                    response.setErrors(errors);
                    response.setStatus(RPCResponse.STATUS_VALIDATION_ERROR);
                    processResponse(request.getRequestId(), response);
                }

                @Override
                public void onSuccess(GroupDefinition result) {
                    CoreGUI.getMessageCenter().notify(new Message(MSG.view_dynagroup_definitionCreated(name)));
                    response.setData(new Record[] { copyValues(result) });
                    processResponse(request.getRequestId(), response);
                }
            });
    }

    @Override
    protected void executeUpdate(Record editedRecord, Record oldRecord, final DSRequest request,
        final DSResponse response) {
        final GroupDefinition updatedGroupDefinition = copyValues(editedRecord);
        final String name = updatedGroupDefinition.getName();

        GWTServiceLookup.getResourceGroupService().updateGroupDefinition(updatedGroupDefinition,
            new AsyncCallback<Void>() {
                @Override
                public void onFailure(Throwable caught) {
                    if (caught instanceof DuplicateExpressionTypeException) {
                        CoreGUI.getMessageCenter().notify(new Message(caught.getMessage(), Message.Severity.Warning));
                    } else {
                        CoreGUI.getErrorHandler().handleError(MSG.view_dynagroup_saveFailure(name), caught);
                    }
                }

                @Override
                public void onSuccess(Void result) {
                    CoreGUI.getMessageCenter().notify(new Message(MSG.view_dynagroup_saveSuccessful(name)));
                    response.setData(new Record[] { copyValues(updatedGroupDefinition) });
                    processResponse(request.getRequestId(), response);
                }
            });
    }

    @Override
    public GroupDefinition copyValues(Record from) {
        GroupDefinition groupDefinition = new GroupDefinition();
        groupDefinition.setId(from.getAttributeAsInt("id"));
        groupDefinition.setName(from.getAttributeAsString("name"));
        groupDefinition.setDescription(from.getAttributeAsString("description"));
        groupDefinition.setExpression(from.getAttributeAsString("expression"));
        groupDefinition.setRecursive(from.getAttributeAsBoolean("recursive"));
        String recalcInt = from.getAttributeAsString("recalculationInterval");
        //groupDefinition.setRecalculationInterval((recalcInt != null) ? Long.parseLong(recalcInt) : 0L);
        // convert the recalculation interval from minutes to millis for db storage
        groupDefinition.setRecalculationInterval((recalcInt != null) ? Long.parseLong(recalcInt) * 60 * 1000 : 0L);
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
        record.setAttribute("cannedExpression", from.getCannedExpression());
        // convert millis to minutes for display
        long recalcIntervalLong = from.getRecalculationInterval() / (60 * 1000);
        record.setAttribute("recalculationInterval", convertLongToString(recalcIntervalLong));
        record.setAttribute("modifiedTime", from.getModifiedTime());
        record.setAttribute("createdTime", from.getCreatedTime());
        record.setAttribute("lastCalculationTime", convertLongToString(from.getLastCalculationTime()));
        record.setAttribute("nextCalculationTime", convertLongToString(from.getNextCalculationTime())); // derived
        //record.setAttribute("object", from);
        return record;
    }

    private String convertLongToString(Long val) {
        String ret = null;
        if (val != null) {
            ret = Long.toString(val.longValue());
        }
        return ret;
    }
}
