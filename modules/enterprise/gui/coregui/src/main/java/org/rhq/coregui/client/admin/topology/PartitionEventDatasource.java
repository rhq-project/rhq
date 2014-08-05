/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.coregui.client.admin.topology;

import static org.rhq.coregui.client.admin.topology.PartitionEventDatasourceField.FIELD_CTIME;
import static org.rhq.coregui.client.admin.topology.PartitionEventDatasourceField.FIELD_EVENT_DETAIL;
import static org.rhq.coregui.client.admin.topology.PartitionEventDatasourceField.FIELD_EVENT_TYPE;
import static org.rhq.coregui.client.admin.topology.PartitionEventDatasourceField.FIELD_EXECUTION_STATUS;
import static org.rhq.coregui.client.admin.topology.PartitionEventDatasourceField.FIELD_ID;
import static org.rhq.coregui.client.admin.topology.PartitionEventDatasourceField.FIELD_SUBJECT_NAME;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.cloud.PartitionEvent;
import org.rhq.core.domain.cloud.PartitionEvent.ExecutionStatus;
import org.rhq.core.domain.cloud.PartitionEventType;
import org.rhq.core.domain.criteria.PartitionEventCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.Log;
import org.rhq.coregui.client.util.RPCDataSource;

/**
 * Datasource for @see PartitionEvent.
 * 
 * @author Jirka Kremser
 */
public class PartitionEventDatasource extends RPCDataSource<PartitionEvent, PartitionEventCriteria> {

    // filters
    public static final String FILTER_EVENT_DETAIL = "eventDetail";
    public static final String FILTER_EXECUTION_STATUS = "executionStatus";
    public static final String FILTER_EVENT_TYPE = "eventType";

    public PartitionEventDatasource() {
        super();
        List<DataSourceField> fields = addDataSourceFields();
        addFields(fields);
    }

    @Override
    protected List<DataSourceField> addDataSourceFields() {
        List<DataSourceField> fields = super.addDataSourceFields();
        DataSourceField idField = new DataSourceIntegerField(FIELD_ID.propertyName(), FIELD_ID.title(), 50);
        idField.setPrimaryKey(true);
        idField.setHidden(true);
        fields.add(idField);
        return fields;
    }

    public List<ListGridField> getListGridFields() {
        List<ListGridField> fields = new ArrayList<ListGridField>();

        ListGridField idField = FIELD_ID.getListGridField();
        idField.setHidden(true);
        fields.add(idField);
        ListGridField executionTimeField = FIELD_CTIME.getListGridField("125");
        TimestampCellFormatter.prepareDateField(executionTimeField);
        fields.add(executionTimeField);
        fields.add(FIELD_EVENT_TYPE.getListGridField("215"));
        fields.add(FIELD_EVENT_DETAIL.getListGridField("*"));
        fields.add(FIELD_SUBJECT_NAME.getListGridField("100"));
        fields.add(FIELD_EXECUTION_STATUS.getListGridField("100"));

        return fields;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response, PartitionEventCriteria criteria) {
        if (criteria == null) {
            response.setTotalRows(0);
            processResponse(request.getRequestId(), response);
            return;
        }
        GWTServiceLookup.getTopologyService().findPartitionEventsByCriteria(criteria,
            new AsyncCallback<PageList<PartitionEvent>>() {
                public void onSuccess(PageList<PartitionEvent> result) {
                    response.setData(buildRecords(result));
                    setPagingInfo(response, result);
                    processResponse(request.getRequestId(), response);
                }

                @Override
                public void onFailure(Throwable t) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_adminTopology_message_fetchPEventFail(), t);
                    response.setStatus(DSResponse.STATUS_FAILURE);
                    processResponse(request.getRequestId(), response);
                }
            });
    }

    @Override
    public PartitionEvent copyValues(Record from) {
        throw new UnsupportedOperationException("PartitionEventDatasource.copyValues(Record from)");
    }

    @Override
    public ListGridRecord copyValues(PartitionEvent from) {
        ListGridRecord record = new ListGridRecord();
        record.setAttribute(FIELD_ID.propertyName(), from.getId());
        record.setAttribute(FIELD_CTIME.propertyName(), from.getCtime());
        record.setAttribute(FIELD_EVENT_TYPE.propertyName(), from.getEventType() == null ? "" : from.getEventType());
        record.setAttribute(FIELD_EVENT_DETAIL.propertyName(),
            from.getEventDetail() == null ? "" : from.getEventDetail());
        record.setAttribute(FIELD_SUBJECT_NAME.propertyName(),
            from.getSubjectName() == null ? "" : from.getSubjectName());
        record.setAttribute(FIELD_EXECUTION_STATUS.propertyName(),
            from.getExecutionStatus() == null ? "" : from.getExecutionStatus());
        return record;
    }

    @Override
    protected PartitionEventCriteria getFetchCriteria(DSRequest request) {
        ExecutionStatus[] statusesFilter = getArrayFilter(request, FILTER_EXECUTION_STATUS, ExecutionStatus.class);
        PartitionEventType[] typesFilter = getArrayFilter(request, FILTER_EVENT_TYPE, PartitionEventType.class);
        if (statusesFilter == null || statusesFilter.length == 0 || typesFilter == null || typesFilter.length == 0) {
            return null; // user didn't select any ex. status or event type - return null to indicate no data should
                         // be displayed
        }
        PartitionEventCriteria criteria = new PartitionEventCriteria();
        //        printRequestCriteria(request);
        criteria.addFilterId(getFilter(request, FIELD_ID.propertyName(), Integer.class));
        criteria.addFilterEventDetail(getFilter(request, FIELD_EVENT_DETAIL.propertyName(), String.class));
        criteria.addFilterExecutionStatus(statusesFilter);
        criteria.addFilterEventType(typesFilter);

        //@todo: Remove me when finished debugging search expression
        Log.debug(" *** PartitionEventCriteria Search String: " + getFilter(request, "search", String.class));
        criteria.setSearchExpression(getFilter(request, "search", String.class));

        return criteria;
    }
}
