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

import org.rhq.core.domain.criteria.ResourceGroupDefinitionCriteria;
import org.rhq.core.domain.resource.group.GroupDefinition;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGroupGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.widgets.grid.ListGridRecord;

/**
 * @author Greg Hinkle
 */
public class GroupDefinitionDataSource extends RPCDataSource {


    private ResourceGroupGWTServiceAsync groupService = GWTServiceLookup.getResourceGroupService();


    public GroupDefinitionDataSource() {
        super();

        DataSourceField idDataField = new DataSourceIntegerField("id", "ID");
        idDataField.setPrimaryKey(true);
        idDataField.setCanEdit(false);

        DataSourceTextField nameField = new DataSourceTextField("name", "Name");
        DataSourceTextField descriptionField = new DataSourceTextField("description", "Description");
        DataSourceTextField expressionField = new DataSourceTextField("expression", "Expression");
        DataSourceTextField recalculationIntervalField = new DataSourceTextField("recalculationInterval", "Recalculation Interval");
        DataSourceTextField modifiedTimeField = new DataSourceTextField("modifiedTime", "Modified Time");
        DataSourceTextField createdTimeField = new DataSourceTextField("createdTime", "Created Time");
        DataSourceTextField lastCalculationTimeField = new DataSourceTextField("lastCalculationTime", "Last Calculation Time");
        DataSourceTextField nextCalculationTimeField = new DataSourceTextField("nextCalculationTime", "Next Calculation Time");
        DataSourceTextField managedResourceGroupsField = new DataSourceTextField("managedResourceGroups", "Managed Resource Groups");


        setFields(idDataField, nameField, descriptionField, expressionField, recalculationIntervalField, modifiedTimeField, createdTimeField, lastCalculationTimeField, nextCalculationTimeField, managedResourceGroupsField);

    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response) {
        ResourceGroupDefinitionCriteria criteria = new ResourceGroupDefinitionCriteria();
        criteria.setPageControl(getPageControl(request));

        groupService.findGroupDefinitionsByCriteria(criteria, new AsyncCallback<PageList<GroupDefinition>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("Failed to load group definitions", caught);
            }

            public void onSuccess(PageList<GroupDefinition> result) {


                response.setStatus(RPCResponse.STATUS_SUCCESS);
                response.setData(buildRecords(result));
                response.setTotalRows(result.getTotalSize());
                processResponse(request.getRequestId(), response);

            }
        });
    }


    public ListGridRecord[] buildRecords(PageList<GroupDefinition> definitions) {

        ListGridRecord[] records = new ListGridRecord[definitions.size()];
        int i = 0;
        for (GroupDefinition def : definitions) {
            ListGridRecord record = new ListGridRecord();
            record.setAttribute("id", def.getId());
            record.setAttribute("name", def.getName());
            record.setAttribute("description", def.getDescription());
            record.setAttribute("expression", def.getExpression());
            record.setAttribute("recalculationInterval", def.getRecalculationInterval());
            record.setAttribute("modifiedTime", def.getModifiedTime());
            record.setAttribute("createdTime", def.getCreatedTime());
            record.setAttribute("lastCalculationTime", def.getLastCalculationTime());
            record.setAttribute("nextCalculationTime", def.getNextCalculationTime());
            record.setAttribute("managedResourceGroups", def.getManagedResourceGroups());

            records[i++] = record;
        }

        return records;
    }
}
