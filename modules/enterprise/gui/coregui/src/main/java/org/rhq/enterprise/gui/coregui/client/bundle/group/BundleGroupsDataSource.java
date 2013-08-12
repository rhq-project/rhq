/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.bundle.group;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.bundle.BundleGroup;
import org.rhq.core.domain.criteria.BundleGroupCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.BundleGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * @author Jay Shaughnessy
 */
public class BundleGroupsDataSource extends RPCDataSource<BundleGroup, BundleGroupCriteria> {

    private BundleGWTServiceAsync bundleService = GWTServiceLookup.getBundleService();

    private static BundleGroupsDataSource INSTANCE;

    public static BundleGroupsDataSource getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new BundleGroupsDataSource();
        }
        return INSTANCE;
    }

    public BundleGroupsDataSource() {
        super();
        List<DataSourceField> fields = addDataSourceFields();
        addFields(fields);
    }

    @Override
    protected List<DataSourceField> addDataSourceFields() {
        List<DataSourceField> fields = super.addDataSourceFields();

        DataSourceIntegerField idField = new DataSourceIntegerField("id", MSG.common_title_id());
        idField.setPrimaryKey(true);
        fields.add(idField);

        DataSourceTextField nameField = new DataSourceTextField("name", MSG.common_title_name());
        fields.add(nameField);

        DataSourceTextField descriptionField = new DataSourceTextField("description", MSG.common_title_description());
        fields.add(descriptionField);

        return fields;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response, final BundleGroupCriteria criteria) {
        bundleService.findBundleGroupsByCriteria(criteria, new AsyncCallback<PageList<BundleGroup>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.dataSource_bundle_loadFailed(), caught);
                response.setStatus(DSResponse.STATUS_FAILURE);
                processResponse(request.getRequestId(), response);
            }

            public void onSuccess(PageList<BundleGroup> result) {
                response.setData(buildRecords(result));
                response.setTotalRows(result.getTotalSize());
                processResponse(request.getRequestId(), response);
            }
        });
    }

    @Override
    protected BundleGroupCriteria getFetchCriteria(final DSRequest request) {
        BundleGroupCriteria criteria = new BundleGroupCriteria();
        // may support tags in future, but not in rev1
        //criteria.addFilterTagNamespace(getFilter(request, "tagNamespace", String.class));
        //criteria.addFilterTagSemantic(getFilter(request, "tagSemantic", String.class));
        //criteria.addFilterTagName(getFilter(request, "tagName", String.class));       
        //criteria.addFilterTagSemantic(getFilter(request, "tagSemantic", String.class));
        criteria.addFilterName(getFilter(request, "search", String.class));

        return criteria;
    }

    @Override
    public BundleGroup copyValues(Record from) {
        return (BundleGroup) from.getAttributeAsObject("object");
    }

    @Override
    public ListGridRecord copyValues(BundleGroup from) {
        ListGridRecord record = new ListGridRecord();

        record.setAttribute("id", from.getId());
        record.setAttribute("name", from.getName());
        record.setAttribute("description", from.getDescription());

        record.setAttribute("object", from);

        return record;
    }
}
