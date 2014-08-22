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

import static org.rhq.coregui.client.admin.topology.AgentDatasourceField.FIELD_ADDRESS;
import static org.rhq.coregui.client.admin.topology.AgentDatasourceField.FIELD_AFFINITY_GROUP;
import static org.rhq.coregui.client.admin.topology.AgentDatasourceField.FIELD_AFFINITY_GROUP_ID;
import static org.rhq.coregui.client.admin.topology.AgentDatasourceField.FIELD_AGENTTOKEN;
import static org.rhq.coregui.client.admin.topology.AgentDatasourceField.FIELD_ID;
import static org.rhq.coregui.client.admin.topology.AgentDatasourceField.FIELD_LAST_AVAILABILITY_PING;
import static org.rhq.coregui.client.admin.topology.AgentDatasourceField.FIELD_LAST_AVAILABILITY_REPORT;
import static org.rhq.coregui.client.admin.topology.AgentDatasourceField.FIELD_NAME;
import static org.rhq.coregui.client.admin.topology.AgentDatasourceField.FIELD_PORT;
import static org.rhq.coregui.client.admin.topology.AgentDatasourceField.FIELD_REMOTE_ENDPOINT;
import static org.rhq.coregui.client.admin.topology.AgentDatasourceField.FIELD_SERVER;
import static org.rhq.coregui.client.admin.topology.AgentDatasourceField.FIELD_SERVER_ID;

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

import org.rhq.core.domain.criteria.AgentCriteria;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.Log;
import org.rhq.coregui.client.util.RPCDataSource;

/**
 * Datasource for @see Agent.
 * 
 * @author Jirka Kremser
 */
public class AgentDatasource extends RPCDataSource<Agent, AgentCriteria> {

    // filters
    public static final String FILTER_ADDRESS = FIELD_ADDRESS.propertyName();
    public static final String FILTER_PORT = FIELD_PORT.propertyName();
    public static final String FILTER_AGENTTOKEN = FIELD_AGENTTOKEN.propertyName();
    public static final String FILTER_SERVER_ID = "serverId";
    public static final String FILTER_AFFINITY_GROUP_ID = "affinityGroupId";

    private final Integer id;
    private final boolean isAffinityGroupId;

    public AgentDatasource(Integer id, boolean isAffinityGroupId) {
        super();
        this.id = id;
        this.isAffinityGroupId = isAffinityGroupId;
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
        fields.add(FIELD_NAME.getListGridField("*"));
        fields.add(FIELD_SERVER.getListGridField("120"));
        fields.add(FIELD_ADDRESS.getListGridField("110"));

        ListGridField serverIdField = FIELD_SERVER_ID.getListGridField();
        serverIdField.setHidden(true);
        fields.add(serverIdField);

        fields.add(FIELD_PORT.getListGridField("90"));
        ListGridField lastAvailabilityReportField = FIELD_LAST_AVAILABILITY_REPORT.getListGridField("125");
        TimestampCellFormatter.prepareDateField(lastAvailabilityReportField);
        fields.add(lastAvailabilityReportField);
        ListGridField lastAvailabilityPingField = FIELD_LAST_AVAILABILITY_PING.getListGridField("125");
        TimestampCellFormatter.prepareDateField(lastAvailabilityPingField);
        fields.add(lastAvailabilityPingField);

        if (!isAffinityGroupId) {
            fields.add(FIELD_AFFINITY_GROUP.getListGridField("100"));
            ListGridField affinityGroupIdField = FIELD_AFFINITY_GROUP_ID.getListGridField();
            affinityGroupIdField.setHidden(true);
            fields.add(affinityGroupIdField);
        }

        return fields;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response, AgentCriteria criteria) {
        if (isAffinityGroupId) {
            criteria.addFilterAffinityGroupId(id);
        } else {
            criteria.addFilterServerId(id);
        }

        GWTServiceLookup.getTopologyService().findAgentsByCriteria(criteria, new AsyncCallback<PageList<Agent>>() {
            public void onSuccess(PageList<Agent> result) {
                response.setData(buildRecords(result));
                setPagingInfo(response, result);
                processResponse(request.getRequestId(), response);
            }

            @Override
            public void onFailure(Throwable t) {
                CoreGUI.getErrorHandler().handleError(MSG.view_adminTopology_message_fetchAgents2Fail(), t);
                response.setStatus(DSResponse.STATUS_FAILURE);
                processResponse(request.getRequestId(), response);
            }
        });
    }

    @Override
    public Agent copyValues(Record from) {
        throw new UnsupportedOperationException("AgentNodeDatasource.copyValues(Record from)");
    }

    @Override
    public ListGridRecord copyValues(Agent from) {
        ListGridRecord record = new ListGridRecord();
        record.setAttribute(FIELD_ID.propertyName(), from.getId());
        record.setAttribute(FIELD_NAME.propertyName(), from.getName());
        record.setAttribute(FIELD_ADDRESS.propertyName(), from.getAddress());
        record.setAttribute(FIELD_REMOTE_ENDPOINT.propertyName(), from.getRemoteEndpoint());
        record.setAttribute(FIELD_PORT.propertyName(), from.getPort());
        record.setAttribute(FIELD_SERVER.propertyName(), from.getServer() == null ? "" : from.getServer().getName());
        record.setAttribute(FIELD_SERVER_ID.propertyName(), from.getServer() == null ? "" : from.getServer().getId());
        record.setAttribute(FIELD_LAST_AVAILABILITY_REPORT.propertyName(), from.getLastAvailabilityReport());
        record.setAttribute(FIELD_LAST_AVAILABILITY_PING.propertyName(), from.getLastAvailabilityPing());
        record.setAttribute(FIELD_AFFINITY_GROUP.propertyName(), from.getAffinityGroup() == null ? "" : from
            .getAffinityGroup().getName());
        record.setAttribute(FIELD_AFFINITY_GROUP_ID.propertyName(), from.getAffinityGroup() == null ? "" : from
            .getAffinityGroup().getId());

        return record;
    }

    @Override
    protected AgentCriteria getFetchCriteria(DSRequest request) {
        AgentCriteria criteria = new AgentCriteria();
        //      printRequestCriteria(request);
        criteria.addFilterId(getFilter(request, FIELD_ID.propertyName(), Integer.class));
        criteria.addFilterName(getFilter(request, FIELD_NAME.propertyName(), String.class));
        criteria.addFilterAddress(getFilter(request, FILTER_ADDRESS, String.class));
        criteria.addFilterPort(getFilter(request, FILTER_PORT, Integer.class));
        criteria.addFilterAgenttoken(getFilter(request, FILTER_AGENTTOKEN, String.class));
        criteria.addFilterServerId(getFilter(request, FILTER_SERVER_ID, Integer.class));
        criteria.addFilterAffinityGroupId(getFilter(request, FILTER_AFFINITY_GROUP_ID, Integer.class));

        //@todo: Remove me when finished debugging search expression
        Log.debug(" *** AgentCriteria Search String: " + getFilter(request, "search", String.class));
        criteria.setSearchExpression(getFilter(request, "search", String.class));

        return criteria;
    }
}
