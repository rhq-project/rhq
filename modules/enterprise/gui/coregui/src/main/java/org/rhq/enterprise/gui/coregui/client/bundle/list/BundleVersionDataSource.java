/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.bundle.list;

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
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.BundleGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * @author John Mazzitelli
 */
public class BundleVersionDataSource extends RPCDataSource<BundleVersion> {

    private BundleGWTServiceAsync bundleService = GWTServiceLookup.getBundleService();

    public BundleVersionDataSource() {
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

        DataSourceTextField latestVersionField = new DataSourceTextField("version", MSG.common_title_version());
        fields.add(latestVersionField);

        DataSourceTextField nameField = new DataSourceTextField("name", MSG.common_title_name());
        fields.add(nameField);

        DataSourceTextField descriptionField = new DataSourceTextField("description", MSG.common_title_description());
        fields.add(descriptionField);

        DataSourceIntegerField deploymentCountField = new DataSourceIntegerField("fileCount", MSG
            .view_bundle_bundleFiles());
        fields.add(deploymentCountField);

        return fields;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response) {

        BundleVersionCriteria criteria = new BundleVersionCriteria();
        criteria.fetchBundleFiles(true);
        criteria.fetchBundle(true);
        criteria.setPageControl(getPageControl(request));

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

        bundleService.findBundleVersionsByCriteria(criteria, new AsyncCallback<PageList<BundleVersion>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_bundleVersion_loadFailure(), caught);
                response.setStatus(DSResponse.STATUS_FAILURE);
                processResponse(request.getRequestId(), response);
            }

            public void onSuccess(PageList<BundleVersion> result) {
                response.setData(buildRecords(result));
                response.setTotalRows(result.getTotalSize());
                processResponse(request.getRequestId(), response);
            }
        });

    }

    @Override
    public BundleVersion copyValues(Record from) {
        return (BundleVersion) from.getAttributeAsObject("object");
    }

    @Override
    public ListGridRecord copyValues(BundleVersion from) {
        ListGridRecord record = new ListGridRecord();

        record.setAttribute("id", from.getId());
        record.setAttribute("bundleId", from.getBundle().getId());
        record.setAttribute("name", from.getName());
        record.setAttribute("description", from.getDescription());
        record.setAttribute("version", from.getVersion());
        record.setAttribute("fileCount", Integer.valueOf(from.getBundleFiles().size()));

        record.setAttribute("object", from);

        return record;

    }
}
