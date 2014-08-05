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

import static org.rhq.coregui.client.admin.topology.ServerDatasourceField.FIELD_ADDRESS;
import static org.rhq.coregui.client.admin.topology.ServerDatasourceField.FIELD_AFFINITY_GROUP;
import static org.rhq.coregui.client.admin.topology.ServerDatasourceField.FIELD_AFFINITY_GROUP_ID;
import static org.rhq.coregui.client.admin.topology.ServerDatasourceField.FIELD_ID;
import static org.rhq.coregui.client.admin.topology.ServerDatasourceField.FIELD_MTIME;
import static org.rhq.coregui.client.admin.topology.ServerDatasourceField.FIELD_NAME;
import static org.rhq.coregui.client.admin.topology.ServerDatasourceField.FIELD_OPERATION_MODE;
import static org.rhq.coregui.client.admin.topology.ServerDatasourceField.FIELD_ORDINAL;
import static org.rhq.coregui.client.admin.topology.ServerDatasourceField.FIELD_PORT;
import static org.rhq.coregui.client.admin.topology.ServerDatasourceField.FIELD_SECURE_PORT;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.cloud.FailoverListDetails;
import org.rhq.core.domain.criteria.Criteria;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.coregui.client.gwt.GWTServiceLookup;

/**
 * Datasource for @see FailoverListDetails.
 * 
 * @author Jirka Kremser
 */
public class FailoverListItemDatasource extends AbstractServerNodeDatasource<FailoverListDetails, Criteria> {

    private final int agentId;

    // this delegate is there to not repeat the copyValues code
    private final ServerDatasource serverDatasource;

    public FailoverListItemDatasource(int agentId) {
        super();
        this.agentId = agentId;
        this.serverDatasource = new ServerDatasource(null);
        List<DataSourceField> fields = addDataSourceFields();
        addFields(fields);
    }

    @Override
    protected List<DataSourceField> addDataSourceFields() {
        return serverDatasource.addDataSourceFields();
    }

    @Override
    public List<ListGridField> getListGridFields() {
        List<ListGridField> fields = new ArrayList<ListGridField>();

        ListGridField idField = FIELD_ID.getListGridField();
        idField.setHidden(true);
        fields.add(idField);
        fields.add(FIELD_ORDINAL.getListGridField("55"));
        fields.add(FIELD_NAME.getListGridField("*"));
        fields.add(FIELD_OPERATION_MODE.getListGridField("90"));
        fields.add(FIELD_ADDRESS.getListGridField("120"));
        fields.add(FIELD_PORT.getListGridField("90"));
        fields.add(FIELD_SECURE_PORT.getListGridField("75"));

        ListGridField lastUpdateTimeField = FIELD_MTIME.getListGridField("110");
        TimestampCellFormatter.prepareDateField(lastUpdateTimeField);
        fields.add(lastUpdateTimeField);

        fields.add(FIELD_AFFINITY_GROUP.getListGridField("80"));
        ListGridField affinityGroupIdField = FIELD_AFFINITY_GROUP_ID.getListGridField();
        affinityGroupIdField.setHidden(true);
        fields.add(affinityGroupIdField);

        return fields;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response, Criteria criteria) {
        final PageControl pc = getPageControl(request);

        // if agentId == null all FailoverListDetails are returned
        GWTServiceLookup.getTopologyService().getFailoverListDetailsByAgentId(agentId, pc,
            new AsyncCallback<PageList<FailoverListDetails>>() {
                public void onSuccess(PageList<FailoverListDetails> result) {
                    response.setData(buildRecords(result));
                    setPagingInfo(response, result);
                    processResponse(request.getRequestId(), response);
                }

                @Override
                public void onFailure(Throwable t) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_adminTopology_message_fetchFailOverLists(), t);
                    response.setStatus(DSResponse.STATUS_FAILURE);
                    processResponse(request.getRequestId(), response);
                }
            });
    }

    @Override
    protected PageControl getPageControl(DSRequest request) {
        return serverDatasource.getPageControl(request);
    }

    @Override
    public FailoverListDetails copyValues(Record from) {
        throw new UnsupportedOperationException("FailoverListItemDatasource.copyValues(Record from)");
    }

    @Override
    public ListGridRecord copyValues(FailoverListDetails from) {
        ListGridRecord record = new ListGridRecord();
        if (from == null || from.getServer() == null) {
            return record;
        }
        record = serverDatasource.copyValues(from.getServer());
        record.setAttribute(FIELD_ORDINAL.propertyName(), from.getOrdinal());
        return record;
    }

    @Override
    protected Criteria getFetchCriteria(DSRequest request) {
        // we don't use criteria for this datasource, just return null
        return null;
    }
}
