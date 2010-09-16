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

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
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

        DataSourceField idDataField = new DataSourceIntegerField("id", "ID");
        idDataField.setPrimaryKey(true);
        idDataField.setCanEdit(false);

        DataSourceTextField nameField = new DataSourceTextField("name", "Name");
        DataSourceTextField descriptionField = new DataSourceTextField("description", "Description");
        DataSourceTextField expressionField = new DataSourceTextField("expression", "Expression");
        DataSourceTextField nextCalculationTimeField = new DataSourceTextField("nextCalculationTime",
            "Next Calculation Time");

        setFields(idDataField, nameField, descriptionField, expressionField, nextCalculationTimeField);

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
    public GroupDefinition copyValues(ListGridRecord from) {
        return null; // TODO: Implement this method.
    }

    @Override
    public ListGridRecord copyValues(GroupDefinition from) {
        ListGridRecord record = new ListGridRecord();
        record.setAttribute("id", from.getId());
        record.setAttribute("name", from.getName());
        record.setAttribute("description", from.getDescription());
        record.setAttribute("expression", from.getExpression());
        record.setAttribute("recalculationInterval", from.getRecalculationInterval());
        record.setAttribute("modifiedTime", from.getModifiedTime());
        record.setAttribute("createdTime", from.getCreatedTime());
        record.setAttribute("lastCalculationTime", from.getLastCalculationTime());
        record.setAttribute("nextCalculationTime", from.getNextCalculationTime());
        //        record.setAttribute("managedResourceGroups", from.getManagedResourceGroups());

        return record;
    }
}
