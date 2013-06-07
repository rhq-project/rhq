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
package org.rhq.enterprise.gui.coregui.client.admin.storage;

import static org.rhq.enterprise.gui.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_ADDRESS;
import static org.rhq.enterprise.gui.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_CQL_PORT;
import static org.rhq.enterprise.gui.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_CTIME;
import static org.rhq.enterprise.gui.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_ID;
import static org.rhq.enterprise.gui.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_JMX_PORT;
import static org.rhq.enterprise.gui.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_MTIME;
import static org.rhq.enterprise.gui.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_OPERATION_MODE;
import static org.rhq.enterprise.gui.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_RESOURCE_ID;

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

import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.cloud.StorageNode.OperationMode;
import org.rhq.core.domain.criteria.StorageNodeCriteria;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * Datasource for @see StorageNode.
 * 
 * @author Jirka Kremser
 */
public class StorageNodeDatasource extends RPCDataSource<StorageNode, StorageNodeCriteria>  {

    // filters
    public static final String FILTER_ADDRESS = FIELD_ADDRESS.propertyName();
    public static final String FILTER_OPERATION_MODE = FIELD_OPERATION_MODE.propertyName();

    public StorageNodeDatasource() {
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

        fields.add(FIELD_ADDRESS.getListGridField("*"));
        fields.add(FIELD_JMX_PORT.getListGridField("90"));
        fields.add(FIELD_CQL_PORT.getListGridField("90"));
        fields.add(FIELD_OPERATION_MODE.getListGridField("90"));

        ListGridField createdTimeField = FIELD_CTIME.getListGridField("120");
        TimestampCellFormatter.prepareDateField(createdTimeField);
        fields.add(createdTimeField);
        
        ListGridField lastUpdateTimeField = FIELD_MTIME.getListGridField("120");
        TimestampCellFormatter.prepareDateField(lastUpdateTimeField);
        fields.add(lastUpdateTimeField);

        ListGridField resourceIdField = FIELD_RESOURCE_ID.getListGridField("120");
//        resourceIdField.setHidden(true);
        fields.add(resourceIdField);

        return fields;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response, StorageNodeCriteria criteria) {
        GWTServiceLookup.getStorageService().findStorageNodesByCriteria(criteria, new AsyncCallback<PageList<StorageNode>>() {
            public void onSuccess(PageList<StorageNode> result) {
                response.setData(buildRecords(result));
                response.setTotalRows(result.size());
                processResponse(request.getRequestId(), response);
            }

            public void onFailure(Throwable t) {
                CoreGUI.getErrorHandler().handleError("td(i18n) Unable to fetch storage nodes.", t);
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
    public StorageNode copyValues(Record from) {
        throw new UnsupportedOperationException("StorageNodeDatasource.copyValues(Record from)");
    }

    @Override
    public ListGridRecord copyValues(StorageNode from) {
        ListGridRecord record = new ListGridRecord();
        record.setAttribute(FIELD_ID.propertyName(), from.getId());
        record.setAttribute(FIELD_ADDRESS.propertyName(), from.getAddress());
        record.setAttribute(FIELD_JMX_PORT.propertyName(), from.getJmxPort());
        record.setAttribute(FIELD_CQL_PORT.propertyName(), from.getCqlPort());
        record.setAttribute(FIELD_OPERATION_MODE.propertyName(), from.getOperationMode());
        record.setAttribute(FIELD_CTIME.propertyName(), from.getCtime());
        record.setAttribute(FIELD_MTIME.propertyName(), from.getMtime());
        if (from.getResource() != null) {
            record.setAttribute(FIELD_RESOURCE_ID.propertyName(), from.getResource().getId());
        }
        return record;
    }

    @Override
    protected StorageNodeCriteria getFetchCriteria(DSRequest request) {
        OperationMode[] modesFilter = getArrayFilter(request, FILTER_OPERATION_MODE, OperationMode.class);
        if (modesFilter == null || modesFilter.length == 0) {
            return null; // user didn't select any modes - return null to indicate no data should be displayed
        }
        StorageNodeCriteria criteria = new StorageNodeCriteria();
        criteria.addFilterId(getFilter(request, FIELD_ID.propertyName(), Integer.class));
        criteria.addFilterAddress(getFilter(request, FILTER_ADDRESS, String.class));
        criteria.addFilterOperationMode(modesFilter);

        //@todo: Remove me when finished debugging search expression
        Log.debug(" *** ServerCriteria Search String: " + getFilter(request, "search", String.class));
        criteria.setSearchExpression(getFilter(request, "search", String.class));

        return criteria;
    }
}
