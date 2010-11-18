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

package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.summary;

import java.util.Date;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceDateTimeField;
import com.smartgwt.client.data.fields.DataSourceEnumField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.resource.ResourceError;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 *
 * @author Lukas Krejci
 */
public class ResourceErrorsDataSource extends RPCDataSource<ResourceError> {

    public static final String SUMMARY_ID = "summary";
    public static final String DETAIL_ID = "detail";
    public static final String ERROR_TYPE_ID = "errorType";
    public static final String TIME_OCCURED_ID = "timeOccured";

    ResourceGWTServiceAsync resourceService;
    int resourceId;

    public ResourceErrorsDataSource(int resourceId) {
        resourceService = GWTServiceLookup.getResourceService();
        this.resourceId = resourceId;
        List<DataSourceField> fields = addDataSourceFields();
        addFields(fields);
    }

    @Override
    protected List<DataSourceField> addDataSourceFields() {
        List<DataSourceField> fields = super.addDataSourceFields();

        fields.add(new DataSourceTextField(SUMMARY_ID, "Summary"));
        fields.add(new DataSourceTextField(DETAIL_ID, "Detailed Message"));
        fields.add(new DataSourceEnumField(ERROR_TYPE_ID, "Error Type"));
        fields.add(new DataSourceDateTimeField(TIME_OCCURED_ID, "Time"));

        return fields;
    }

    /* (non-Javadoc)
     * @see org.rhq.enterprise.gui.coregui.client.util.RPCDataSource#executeFetch(com.smartgwt.client.data.DSRequest, com.smartgwt.client.data.DSResponse)
     */
    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response) {
        resourceService.findResourceErrors(resourceId, new AsyncCallback<List<ResourceError>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(
                    "Failed to find resource errors for resource with id: " + resourceId, caught);
                response.setStatus(RPCResponse.STATUS_FAILURE);
                processResponse(request.getRequestId(), response);
            }

            public void onSuccess(List<ResourceError> result) {
                response.setData(buildRecords(result));
                processResponse(request.getRequestId(), response);
            };
        });
    }

    public ResourceError copyValues(Record from) {
        //This is read-only datasource, so no need to implement this.
        return null;
    }

    public ListGridRecord copyValues(ResourceError from) {
        ListGridRecord record = new ListGridRecord();

        record.setAttribute(DETAIL_ID, from.getDetail());
        record.setAttribute(ERROR_TYPE_ID, from.getErrorType().name());
        record.setAttribute(SUMMARY_ID, from.getSummary());
        record.setAttribute(TIME_OCCURED_ID, new Date(from.getTimeOccurred()));

        return record;
    }

}
