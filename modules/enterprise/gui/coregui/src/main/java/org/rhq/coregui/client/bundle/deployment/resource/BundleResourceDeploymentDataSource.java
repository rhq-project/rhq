/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.coregui.client.bundle.deployment.resource;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.bundle.BundleResourceDeployment;
import org.rhq.core.domain.criteria.BundleResourceDeploymentCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.gwt.BundleGWTServiceAsync;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.RPCDataSource;

/**
 * @author Greg Hinkle
 */
public class BundleResourceDeploymentDataSource extends
    RPCDataSource<BundleResourceDeployment, BundleResourceDeploymentCriteria> {
    private BundleGWTServiceAsync bundleService = GWTServiceLookup.getBundleService();

    public BundleResourceDeploymentDataSource() {
        super();
        List<DataSourceField> fields = addDataSourceFields();
        addFields(fields);
    }

    @Override
    protected List<DataSourceField> addDataSourceFields() {
        List<DataSourceField> fields = super.addDataSourceFields();

        DataSourceIntegerField id = new DataSourceIntegerField("id", MSG.common_title_id());
        id.setPrimaryKey(true);
        fields.add(id);

        DataSourceTextField resourceName = new DataSourceTextField("resourceName", MSG.common_title_resource());
        fields.add(resourceName);

        DataSourceTextField status = new DataSourceTextField("status", MSG.common_title_status());
        fields.add(status);

        return fields;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response,
        final BundleResourceDeploymentCriteria criteria) {
        bundleService.findBundleResourceDeploymentsByCriteria(criteria,
            new AsyncCallback<PageList<BundleResourceDeployment>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_bundle_resDeployDS_loadFailure(), caught);
                }

                public void onSuccess(PageList<BundleResourceDeployment> result) {
                    response.setData(buildRecords(result));
                    setPagingInfo(response, result);
                    processResponse(request.getRequestId(), response);
                }
            });
    }

    @Override
    protected BundleResourceDeploymentCriteria getFetchCriteria(final DSRequest request) {
        BundleResourceDeploymentCriteria criteria = new BundleResourceDeploymentCriteria();
        criteria.fetchResource(true);
        criteria.fetchBundleDeployment(true);
        criteria.fetchHistories(true);

        if (request.getCriteria().getValues().containsKey("bundleDeploymentId")) {
            criteria.addFilterBundleDeploymentId(Integer.parseInt(request.getCriteria().getAttribute(
                "bundleDeploymentId")));
        }
        return criteria;
    }

    @Override
    public BundleResourceDeployment copyValues(Record from) {
        return null; // TODO: Implement this method.
    }

    @Override
    public ListGridRecord copyValues(BundleResourceDeployment from) {
        ListGridRecord record = new ListGridRecord();
        record.setAttribute("id", from.getId());

        record.setAttribute("resourceName", from.getResource().getName());
        record.setAttribute("resourceId", from.getResource().getId());
        record.setAttribute("status", from.getStatus().name());

        record.setAttribute("histories", from.getBundleResourceDeploymentHistories());

        from.getBundleResourceDeploymentHistories();

        return record;
    }
}
