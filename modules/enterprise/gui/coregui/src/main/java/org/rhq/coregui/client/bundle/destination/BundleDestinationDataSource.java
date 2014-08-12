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
package org.rhq.coregui.client.bundle.destination;

import java.util.Date;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.bundle.BundleDestination;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.criteria.BundleDestinationCriteria;
import org.rhq.core.domain.criteria.BundleVersionCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.RPCDataSource;

/**
 * @author Greg Hinkle
 */
public class BundleDestinationDataSource extends RPCDataSource<BundleDestination, BundleDestinationCriteria> {

    public static final String FIELD_ID = "id";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_BUNDLE_ID = "bundleId";
    public static final String FIELD_BUNDLE_NAME = "bundleName";
    public static final String FIELD_GROUP_ID = "groupId";
    public static final String FIELD_GROUP_NAME = "groupName";
    public static final String FIELD_DEPLOY_DIR = "deployDir";
    public static final String FIELD_BASE_DIR_NAME = "baseDirName";
    public static final String FIELD_LATEST_DEPLOY_VERSION = "latestDeploymentVersion";
    public static final String FIELD_LATEST_DEPLOY_DATE = "latestDeploymentDate";
    public static final String FIELD_LATEST_DEPLOY_STATUS = "latestDeploymentStatus";
    public static final String FIELD_LATEST_DEPLOY = "latestDeployment";

    public BundleDestinationDataSource() {
        super();
        List<DataSourceField> fields = addDataSourceFields();
        addFields(fields);
    }

    @Override
    protected List<DataSourceField> addDataSourceFields() {
        List<DataSourceField> fields = super.addDataSourceFields();

        DataSourceIntegerField idField = new DataSourceIntegerField(FIELD_ID, MSG.common_title_id());
        idField.setPrimaryKey(true);
        fields.add(idField);

        DataSourceTextField name = new DataSourceTextField(FIELD_NAME, MSG.common_title_name());
        fields.add(name);

        DataSourceTextField description = new DataSourceTextField(FIELD_DESCRIPTION, MSG.common_title_description());
        fields.add(description);

        DataSourceTextField bundle = new DataSourceTextField(FIELD_BUNDLE_NAME, MSG.common_title_bundle());
        fields.add(bundle);

        DataSourceTextField group = new DataSourceTextField(FIELD_GROUP_NAME, MSG.view_bundle_dest_group());
        fields.add(group);

        DataSourceTextField baseDirName = new DataSourceTextField(FIELD_BASE_DIR_NAME, MSG
            .view_bundle_dest_baseDirName());
        fields.add(baseDirName);

        DataSourceTextField deployDir = new DataSourceTextField(FIELD_DEPLOY_DIR, MSG.view_bundle_dest_deployDir());
        fields.add(deployDir);

        DataSourceTextField latestDeploymentVersion = new DataSourceTextField(FIELD_LATEST_DEPLOY_VERSION, MSG
            .view_bundle_dest_lastDeployedVersion());
        fields.add(latestDeploymentVersion);

        DataSourceTextField latestDeploymentDate = new DataSourceTextField(FIELD_LATEST_DEPLOY_DATE, MSG
            .view_bundle_dest_lastDeploymentDate());
        fields.add(latestDeploymentDate);

        DataSourceTextField latestDeploymentStatus = new DataSourceTextField(FIELD_LATEST_DEPLOY_STATUS, MSG
            .view_bundle_dest_lastDeploymentStatus());
        fields.add(latestDeploymentStatus);

        return fields;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response,
        final BundleDestinationCriteria criteria) {
        GWTServiceLookup.getBundleService().findBundleDestinationsByCriteria(criteria,
            new AsyncCallback<PageList<BundleDestination>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_bundle_dest_loadFailure(), caught);
                }

                public void onSuccess(final PageList<BundleDestination> result) {
                    BundleVersionCriteria versionCriteria = new BundleVersionCriteria();
                    if (request.getCriteria().getValues().containsKey(FIELD_BUNDLE_ID)) {
                        versionCriteria.addFilterBundleId(Integer.parseInt(request.getCriteria().getAttributeAsString(
                            FIELD_BUNDLE_ID)));
                    }
                    GWTServiceLookup.getBundleService().findBundleVersionsByCriteria(versionCriteria,
                        new AsyncCallback<PageList<BundleVersion>>() {
                            public void onFailure(Throwable caught) {
                                CoreGUI.getErrorHandler().handleError(MSG.view_bundle_dest_loadFailureVersionInfo(),
                                    caught);
                            }

                            public void onSuccess(PageList<BundleVersion> versions) {
                                for (BundleDestination dest : result) {
                                    for (BundleDeployment dep : dest.getDeployments()) {
                                        for (BundleVersion version : versions) {
                                            if (dep.getBundleVersion().getId() == version.getId()) {
                                                dep.setBundleVersion(version);
                                            }
                                        }
                                    }
                                }
                                response.setData(buildRecords(result));
                                setPagingInfo(response, result);
                                processResponse(request.getRequestId(), response);
                            }
                        });
                }
            });
    }

    @Override
    protected BundleDestinationCriteria getFetchCriteria(final DSRequest request) {
        BundleDestinationCriteria criteria = new BundleDestinationCriteria();

        if (request.getCriteria().getValues().containsKey(FIELD_BUNDLE_ID)) {
            criteria.addFilterBundleId(Integer.parseInt(request.getCriteria().getAttributeAsString(FIELD_BUNDLE_ID)));
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

        criteria.fetchBundle(true);
        criteria.fetchDeployments(true);
        criteria.fetchGroup(true);
        criteria.fetchTags(true);
        return criteria;
    }

    @Override
    protected String getSortFieldForColumn(String columnName) {
        if (FIELD_LATEST_DEPLOY_DATE.equals(columnName)) {
            return null;
        }
        if (FIELD_LATEST_DEPLOY_STATUS.equals(columnName)) {
            return null;
        }
        if (FIELD_LATEST_DEPLOY_VERSION.equals(columnName)) {
            return null;
        }

        return super.getSortFieldForColumn(columnName);
    }

    @Override
    public BundleDestination copyValues(Record from) {
        return (BundleDestination) from.getAttributeAsObject("object");
    }

    @Override
    public ListGridRecord copyValues(BundleDestination from) {
        ListGridRecord record = new ListGridRecord();

        record.setAttribute(FIELD_ID, from.getId());
        record.setAttribute(FIELD_NAME, from.getName());
        record.setAttribute(FIELD_DESCRIPTION, from.getDescription());
        record.setAttribute(FIELD_BUNDLE_ID, from.getBundle().getId());
        record.setAttribute(FIELD_BUNDLE_NAME, from.getBundle().getName());
        record.setAttribute(FIELD_GROUP_ID, from.getGroup().getId());
        record.setAttribute(FIELD_GROUP_NAME, from.getGroup().getName());
        record.setAttribute(FIELD_BASE_DIR_NAME, from.getDestinationBaseDirectoryName());
        record.setAttribute(FIELD_DEPLOY_DIR, from.getDeployDir());

        record.setAttribute("object", from);

        long last = 0;
        for (BundleDeployment dep : from.getDeployments()) {
            if (last < dep.getCtime()) {
                last = dep.getCtime();
                record.setAttribute(FIELD_LATEST_DEPLOY, dep);
                record.setAttribute(FIELD_LATEST_DEPLOY_VERSION, dep.getBundleVersion().getVersion());
                record.setAttribute(FIELD_LATEST_DEPLOY_DATE, new Date(dep.getCtime()));
                record.setAttribute(FIELD_LATEST_DEPLOY_STATUS, dep.getStatus().name());
            }
        }

        return record;
    }
}
