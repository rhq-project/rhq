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
import static org.rhq.coregui.client.admin.topology.ServerDatasourceField.FIELD_PORT;
import static org.rhq.coregui.client.admin.topology.ServerDatasourceField.FIELD_SECURE_PORT;

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

import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.cloud.Server.OperationMode;
import org.rhq.core.domain.criteria.ServerCriteria;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.Log;

/**
 * Datasource for @see Server.
 * 
 * @author Jirka Kremser
 */
public class ServerDatasource extends AbstractServerNodeDatasource<Server, ServerCriteria> {

    // filters
    public static final String FILTER_ADDRESS = FIELD_ADDRESS.propertyName();
    public static final String FILTER_PORT = FIELD_PORT.propertyName();
    public static final String FILTER_SECURE_PORT = FIELD_SECURE_PORT.propertyName();
    public static final String FILTER_OPERATION_MODE = FIELD_OPERATION_MODE.propertyName();
    public static final String FILTER_COMPUTE_POWER = "computePower";
    public static final String FILTER_AFFINITY_GROUP_ID = "affinityGroupId";

    private final Integer affinityGroupId;

    public ServerDatasource(Integer affinityGroupId) {
        super();
        this.affinityGroupId = affinityGroupId;
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
        fields.add(FIELD_OPERATION_MODE.getListGridField("90"));
        fields.add(FIELD_ADDRESS.getListGridField("190"));
        fields.add(FIELD_PORT.getListGridField("90"));
        fields.add(FIELD_SECURE_PORT.getListGridField("75"));

        ListGridField lastUpdateTimeField = FIELD_MTIME.getListGridField("120");
        TimestampCellFormatter.prepareDateField(lastUpdateTimeField);
        fields.add(lastUpdateTimeField);

        ListGridField affinityGroupIdField = FIELD_AFFINITY_GROUP_ID.getListGridField();
        affinityGroupIdField.setHidden(true);
        fields.add(affinityGroupIdField);

        return fields;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response, ServerCriteria criteria) {
        if (criteria == null) {
            response.setTotalRows(0);
            processResponse(request.getRequestId(), response);
            return;
        }
        if (affinityGroupId != null) {
            criteria.addFilterAffinityGroupId(affinityGroupId);
        }
        GWTServiceLookup.getTopologyService().findServersByCriteria(criteria, new AsyncCallback<PageList<Server>>() {
            public void onSuccess(PageList<Server> result) {
                response.setData(buildRecords(result));
                setPagingInfo(response, result);
                processResponse(request.getRequestId(), response);
            }

            public void onFailure(Throwable t) {
                CoreGUI.getErrorHandler().handleError(MSG.view_adminTopology_message_fetchServers2Fail(), t);
                response.setStatus(DSResponse.STATUS_FAILURE);
                processResponse(request.getRequestId(), response);
            }
        });
    }

    /**
     * Returns a prepopulated PageControl based on the provided DSRequest. This will set sort fields,
     * pagination, but *not* filter fields.
     *
     * @param request the request to turn into a page control
     * @return the page control for passing to criteria and other queries
     */
    @Override
    protected PageControl getPageControl(DSRequest request) {
        // Initialize paging.         
        PageControl pageControl = new PageControl(0, getDataPageSize());

        // Initialize sorting.
        String sortBy = request.getAttribute("sortBy");
        if (sortBy != null) {
            String[] sorts = sortBy.split(",");
            for (String sort : sorts) {
                PageOrdering ordering = (sort.startsWith("-")) ? PageOrdering.DESC : PageOrdering.ASC;
                String columnName = (ordering == PageOrdering.DESC) ? sort.substring(1) : sort;
                pageControl.addDefaultOrderingField(columnName, ordering);
            }
        }

        return pageControl;
    }

    @Override
    public Server copyValues(Record from) {
        throw new UnsupportedOperationException("ServerDatasource.copyValues(Record from)");
    }

    @Override
    public ListGridRecord copyValues(Server from) {
        ListGridRecord record = new ListGridRecord();
        record.setAttribute(FIELD_ID.propertyName(), from.getId());
        record.setAttribute(FIELD_NAME.propertyName(), from.getName());
        record.setAttribute(FIELD_OPERATION_MODE.propertyName(), from.getOperationMode());
        record.setAttribute(FIELD_ADDRESS.propertyName(), from.getAddress());
        record.setAttribute(FIELD_PORT.propertyName(), from.getPort());
        record.setAttribute(FIELD_SECURE_PORT.propertyName(), from.getSecurePort());
        record.setAttribute(FIELD_MTIME.propertyName(), from.getMtime());
        record.setAttribute(FIELD_AFFINITY_GROUP.propertyName(), from.getAffinityGroup() == null ? "" : from
            .getAffinityGroup().getName());
        record.setAttribute(FIELD_AFFINITY_GROUP_ID.propertyName(), from.getAffinityGroup() == null ? "" : from
            .getAffinityGroup().getId());
        return record;
    }

    @Override
    protected ServerCriteria getFetchCriteria(DSRequest request) {
        OperationMode[] modesFilter = getArrayFilter(request, FILTER_OPERATION_MODE, OperationMode.class);
        if (modesFilter == null || modesFilter.length == 0) {
            return null; // user didn't select any modes - return null to indicate no data should be displayed
        }
        ServerCriteria criteria = new ServerCriteria();
        //        printRequestCriteria(request);
        criteria.addFilterId(getFilter(request, FIELD_ID.propertyName(), Integer.class));
        criteria.addFilterName(getFilter(request, FIELD_NAME.propertyName(), String.class));
        criteria.addFilterAddress(getFilter(request, FILTER_ADDRESS, String.class));
        criteria.addFilterPort(getFilter(request, FILTER_PORT, Integer.class));
        criteria.addFilterSecurePort(getFilter(request, FILTER_SECURE_PORT, Integer.class));
        criteria.addFilterOperationMode(modesFilter);
        criteria.addFilterComputePower(getFilter(request, FILTER_COMPUTE_POWER, Integer.class));
        criteria.addFilterAffinityGroupId(getFilter(request, FILTER_AFFINITY_GROUP_ID, Integer.class));

        //@todo: Remove me when finished debugging search expression
        Log.debug(" *** ServerCriteria Search String: " + getFilter(request, "search", String.class));
        criteria.setSearchExpression(getFilter(request, "search", String.class));

        return criteria;
    }
}
