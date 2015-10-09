/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
package org.rhq.coregui.client.bundle.list;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.criteria.BundleVersionCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.RPCDataSource;

/**
 * @author John Mazzitelli
 */
public class BundleVersionDataSource extends RPCDataSource<BundleVersion, BundleVersionCriteria> {

    public static final String FIELD_ID = "id";
    public static final String FIELD_BUNDLE_ID = "bundleId";
    public static final String FIELD_VERSION = "version";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_FILECOUNT = "fileCount";

    public BundleVersionDataSource() {
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

        DataSourceTextField versionField = new DataSourceTextField(FIELD_VERSION, MSG.common_title_version());
        fields.add(versionField);

        DataSourceTextField nameField = new DataSourceTextField(FIELD_NAME, MSG.common_title_name());
        fields.add(nameField);

        DataSourceTextField descriptionField = new DataSourceTextField(FIELD_DESCRIPTION,
            MSG.common_title_description());
        fields.add(descriptionField);

        DataSourceIntegerField fileCountField = new DataSourceIntegerField(FIELD_FILECOUNT,
            MSG.view_bundle_bundleFiles());
        fields.add(fileCountField);

        return fields;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response, final BundleVersionCriteria criteria) {
        GWTServiceLookup.getBundleService().findBundleVersionsByCriteria(criteria,
            new AsyncCallback<PageList<BundleVersion>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_bundleVersion_loadFailure(), caught);
                response.setStatus(DSResponse.STATUS_FAILURE);
                processResponse(request.getRequestId(), response);
            }

            public void onSuccess(PageList<BundleVersion> result) {
                response.setData(buildRecords(result));
                setPagingInfo(response, result);
                processResponse(request.getRequestId(), response);
            }
        });
    }

    @Override
    protected BundleVersionCriteria getFetchCriteria(final DSRequest request) {
        BundleVersionCriteria criteria = new BundleVersionCriteria();
        criteria.fetchBundleFiles(true);
        criteria.fetchBundle(true);

        if (request.getCriteria().getValues().get("bundleId") != null) {
            criteria.addFilterBundleId(Integer.parseInt(String.valueOf(request.getCriteria().getValues()
                .get("bundleId"))));
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
        if (FIELD_FILECOUNT.equals(columnName)) {
            return null;
        }

        return super.getSortFieldForColumn(columnName);
    }

    @Override
    public BundleVersion copyValues(Record from) {
        return (BundleVersion) from.getAttributeAsObject("object");
    }

    @Override
    public ListGridRecord copyValues(BundleVersion from) {
        ListGridRecord record = new ListGridRecord();

        record.setAttribute(FIELD_ID, from.getId());
        record.setAttribute(FIELD_BUNDLE_ID, from.getBundle().getId());
        record.setAttribute(FIELD_NAME, from.getName());
        record.setAttribute(FIELD_DESCRIPTION, from.getDescription());
        record.setAttribute(FIELD_VERSION, from.getVersion());
        record.setAttribute(FIELD_FILECOUNT, Integer.valueOf(from.getBundleFiles().size()));

        record.setAttribute("object", from);

        return record;

    }
}
