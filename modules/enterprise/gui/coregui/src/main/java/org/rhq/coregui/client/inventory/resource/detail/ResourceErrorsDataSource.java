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

package org.rhq.coregui.client.inventory.resource.detail;

import java.util.Date;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.criteria.Criteria;
import org.rhq.core.domain.resource.ResourceError;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.gwt.ResourceGWTServiceAsync;
import org.rhq.coregui.client.util.RPCDataSource;

/**
 * @author Lukas Krejci
 * @author Simeon Pinder
 * @author John Mazzitelli
 */
public class ResourceErrorsDataSource extends RPCDataSource<ResourceError, Criteria> {

    public static abstract class Field {
        public static final String ID = "id";
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
    protected void executeFetch(final DSRequest request, final DSResponse response, final Criteria unused) {
        resourceService.findResourceErrors(resourceId, new AsyncCallback<List<ResourceError>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(
                    MSG.dataSource_resourceErrors_error_fetchFailure(String.valueOf(resourceId)), caught);
                response.setStatus(RPCResponse.STATUS_FAILURE);
                processResponse(request.getRequestId(), response);
            }

            public void onSuccess(List<ResourceError> result) {
                response.setData(buildRecords(result));
                response.setTotalRows(result.size());
                processResponse(request.getRequestId(), response);
            }
        });
    }

    @Override
    protected Criteria getFetchCriteria(DSRequest request) {
        // we don't use criterias for this datasource, just return null
        return null;
    }

    @Override
    public ResourceError copyValues(Record from) {
        //This is read-only datasource, so no need to implement this.
        return null;
    }

    @Override
    public ListGridRecord copyValues(ResourceError from) {
        ListGridRecord record = new ListGridRecord();

        record.setAttribute(Field.ID, from.getId());
        record.setAttribute(Field.DETAIL, from.getDetail());
        record.setAttribute(Field.ERROR_TYPE, from.getErrorType().name());
        record.setAttribute(Field.SUMMARY, from.getSummary());
        record.setAttribute(Field.TIME_OCCURED, new Date(from.getTimeOccurred()));

        return record;
    }
}
