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

    public static abstract class Field {
        public static final String SUMMARY = "summary";
        public static final String DETAIL = "detail";
        public static final String ERROR_TYPE = "errorType";
        public static final String TIME_OCCURED = "timeOccured";
    }

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

        fields.add(new DataSourceTextField(Field.SUMMARY, MSG.dataSource_resourceErrors_field_summary()));
        fields.add(new DataSourceTextField(Field.DETAIL, MSG.dataSource_resourceErrors_field_detail()));
        fields.add(new DataSourceEnumField(Field.ERROR_TYPE, MSG.dataSource_resourceErrors_field_errorType()));
        fields.add(new DataSourceDateTimeField(Field.TIME_OCCURED, MSG.dataSource_resourceErrors_field_timeOccured()));

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
                    MSG.dataSource_resourceErrors_error_fetchFailure(String.valueOf(resourceId)),
                    caught);
                response.setStatus(RPCResponse.STATUS_FAILURE);
                processResponse(request.getRequestId(), response);
            }

            public void onSuccess(List<ResourceError> result) {
                response.setData(buildRecords(result));
                processResponse(request.getRequestId(), response);
            }
        });
    }

    public ResourceError copyValues(Record from) {
        //This is read-only datasource, so no need to implement this.
        return null;
    }

    public ListGridRecord copyValues(ResourceError from) {
        ListGridRecord record = new ListGridRecord();

        record.setAttribute(Field.DETAIL, from.getDetail());
        record.setAttribute(Field.ERROR_TYPE, from.getErrorType().name());
        record.setAttribute(Field.SUMMARY, from.getSummary());
        record.setAttribute(Field.TIME_OCCURED, new Date(from.getTimeOccurred()));

        return record;
    }

}
