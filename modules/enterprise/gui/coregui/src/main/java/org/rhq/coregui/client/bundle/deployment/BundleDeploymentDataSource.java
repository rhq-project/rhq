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
package org.rhq.coregui.client.bundle.deployment;

import java.util.Date;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceDateTimeField;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.criteria.BundleDeploymentCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.RPCDataSource;

/**
 * @author Greg Hinkle
 */
public class BundleDeploymentDataSource extends RPCDataSource<BundleDeployment, BundleDeploymentCriteria> {

    public static final String FIELD_ID = "id";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_DEPLOY_DIR = "deployDir";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_DEPLOY_TIME = "ctime";
    public static final String FIELD_ERROR_MESSAGE = "errorMessage";
    public static final String FIELD_CONFIG = "configuration";
    public static final String FIELD_STATUS = "status";
    public static final String FIELD_DEPLOYER = "deployer"; // a user name
    public static final String FIELD_BUNDLE_VERSION_VERSION = "bundleVersionVersion";
    public static final String FIELD_BUNDLE_VERSION_ID = "bundleVersionId";
    public static final String FIELD_BUNDLE_ID = "bundleId";

    public BundleDeploymentDataSource() {
        super();
        List<DataSourceField> fields = addDataSourceFields();
        addFields(fields);
    }

    @Override
    protected List<DataSourceField> addDataSourceFields() {
        List<DataSourceField> fields = super.addDataSourceFields();

        DataSourceIntegerField id = new DataSourceIntegerField(FIELD_ID, MSG.common_title_id());
        id.setPrimaryKey(true);
        fields.add(id);

        DataSourceTextField name = new DataSourceTextField(FIELD_NAME, MSG.view_bundle_deploy_name());
        fields.add(name);

        DataSourceTextField bundleVersion = new DataSourceTextField(FIELD_BUNDLE_VERSION_VERSION, MSG
            .view_bundle_bundleVersion());
        fields.add(bundleVersion);

        DataSourceTextField description = new DataSourceTextField(FIELD_DESCRIPTION, MSG.common_title_description());
        fields.add(description);

        DataSourceTextField status = new DataSourceTextField(FIELD_STATUS, MSG.common_title_status());
        fields.add(status);

        DataSourceDateTimeField created = new DataSourceDateTimeField(FIELD_DEPLOY_TIME, MSG.view_bundle_deploy_time());
        fields.add(created);

        return fields;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response,
        final BundleDeploymentCriteria criteria) {
        GWTServiceLookup.getBundleService().findBundleDeploymentsByCriteria(criteria,
            new AsyncCallback<PageList<BundleDeployment>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_bundle_deploy_loadDeployFailure(), caught);
            }

            public void onSuccess(PageList<BundleDeployment> result) {
                response.setData(buildRecords(result));
                setPagingInfo(response, result);
                processResponse(request.getRequestId(), response);
            }
        });
    }

    @Override
    protected BundleDeploymentCriteria getFetchCriteria(final DSRequest request) {
        BundleDeploymentCriteria criteria = new BundleDeploymentCriteria();
        criteria.fetchBundleVersion(true);

        if (request.getCriteria().getValues().containsKey(FIELD_BUNDLE_ID)) {
            criteria.addFilterBundleId(Integer.parseInt(request.getCriteria().getAttribute(FIELD_BUNDLE_ID)));
        }

        if (request.getCriteria().getValues().containsKey(FIELD_BUNDLE_VERSION_ID)) {
            criteria.addFilterBundleVersionId(Integer.parseInt(request.getCriteria().getAttribute(
                FIELD_BUNDLE_VERSION_ID)));
        }

        if (request.getCriteria().getValues().containsKey("bundleDestinationId")) {
            criteria
                .addFilterDestinationId(Integer.parseInt(request.getCriteria().getAttribute("bundleDestinationId")));
        }

        if (request.getCriteria().getValues().get("tagNamespace") != null) {
            criteria.addFilterTagNamespace((String) request.getCriteria().getValues().get("tagNamespace"));
        }

        if (request.getCriteria().getValues().get("tagSemantic") != null) {
            criteria.addFilterTagSemantic((String) request.getCriteria().getValues().get("tagSemantic"));
        }

        if (request.getCriteria().getValues().get("tagName") != null) {
            criteria.addFilterTagName((String) request.getCriteria().getValues().get("tagName"));
        }
        return criteria;
    }

    @Override
    protected String getSortFieldForColumn(String columnName) {
        if (FIELD_BUNDLE_VERSION_VERSION.equals(columnName)) {
            return "bundleVersion.version";
        }

        return super.getSortFieldForColumn(columnName);
    }

    @Override
    public BundleDeployment copyValues(Record from) {
        return (BundleDeployment) from.getAttributeAsObject("object");
    }

    @Override
    public ListGridRecord copyValues(BundleDeployment from) {

        ListGridRecord record = new ListGridRecord();

        record.setAttribute(FIELD_ID, from.getId());
        record.setAttribute(FIELD_NAME, from.getName());
        record.setAttribute(FIELD_DEPLOY_DIR, from.getDestination().getDeployDir());
        record.setAttribute(FIELD_DESCRIPTION, from.getDescription());
        record.setAttribute(FIELD_ERROR_MESSAGE, from.getErrorMessage());
        record.setAttribute(FIELD_DEPLOY_TIME, new Date(from.getCtime()));
        record.setAttribute(FIELD_CONFIG, from.getConfiguration());
        record.setAttribute(FIELD_STATUS, from.getStatus().name());
        record.setAttribute(FIELD_DEPLOYER, from.getSubjectName());

        if (from.getBundleVersion() != null) {
            record.setAttribute(FIELD_BUNDLE_VERSION_VERSION, from.getBundleVersion().getVersion());
            record.setAttribute(FIELD_BUNDLE_VERSION_ID, from.getBundleVersion().getId());

            if (from.getBundleVersion().getBundle() != null) {
                record.setAttribute(FIELD_BUNDLE_ID, from.getBundleVersion().getBundle().getId());
            }
        }

        record.setAttribute("object", from);

        return record;
    }
}
