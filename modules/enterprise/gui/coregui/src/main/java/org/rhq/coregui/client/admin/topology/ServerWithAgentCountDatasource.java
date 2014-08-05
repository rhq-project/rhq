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

import static org.rhq.coregui.client.admin.topology.ServerDatasourceField.FIELD_AGENT_COUNT;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.widgets.grid.HoverCustomizer;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.cloud.composite.ServerWithAgentCountComposite;
import org.rhq.core.domain.criteria.Criteria;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.gwt.GWTServiceLookup;

/**
 * Datasource for @see ServerWithAgentCountComposite.
 * 
 * @author Jirka Kremser
 */
public class ServerWithAgentCountDatasource extends
    AbstractServerNodeDatasource<ServerWithAgentCountComposite, Criteria> {

    // server datasource delegate
    private final ServerDatasource serverDatasource;

    public ServerWithAgentCountDatasource() {
        super();
        serverDatasource = new ServerDatasource(null);
        List<DataSourceField> fields = addDataSourceFields();
        addFields(fields);
    }

    @Override
    protected List<DataSourceField> addDataSourceFields() {
        return serverDatasource.addDataSourceFields();
    }

    public List<ListGridField> getListGridFields() {
        List<ListGridField> fields = serverDatasource.getListGridFields();
        ListGridField agentCountField = FIELD_AGENT_COUNT.getListGridField("75");
        agentCountField.setHoverCustomizer(new HoverCustomizer() {
            public String hoverHTML(Object value, ListGridRecord record, int rowNum, int colNum) {
                return MSG.view_adminTopology_message_agentsCount(record.getAttributeAsString(FIELD_AGENT_COUNT
                    .propertyName()));
            }
        });
        agentCountField.setShowHover(true);
        fields.add(agentCountField);
        return fields;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response, Criteria criteria) {
        final PageControl pc = getPageControl(request);

        GWTServiceLookup.getTopologyService().getServers(pc, new AsyncCallback<PageList<ServerWithAgentCountComposite>>() {
            public void onSuccess(PageList<ServerWithAgentCountComposite> result) {
                response.setData(buildRecords(result));
                setPagingInfo(response, result);
                processResponse(request.getRequestId(), response);
            }

            @Override
            public void onFailure(Throwable t) {
                CoreGUI.getErrorHandler().handleError(MSG.view_adminTopology_message_fetchServers2Fail(), t);
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
    public ServerWithAgentCountComposite copyValues(Record from) {
        throw new UnsupportedOperationException("ServerWithAgentCountDatasource.copyValues(Record from)");
    }

    @Override
    public ListGridRecord copyValues(ServerWithAgentCountComposite from) {
        ListGridRecord record = new ListGridRecord();
        if (from == null || from.getServer() == null) {
            return record;
        }
        record = serverDatasource.copyValues(from.getServer());
        record.setAttribute(FIELD_AGENT_COUNT.propertyName(), from.getAgentCount());
        return record;
    }

    @Override
    protected Criteria getFetchCriteria(DSRequest request) {
        // we don't use criteria for this datasource, just return null
        return null;
    }
}
