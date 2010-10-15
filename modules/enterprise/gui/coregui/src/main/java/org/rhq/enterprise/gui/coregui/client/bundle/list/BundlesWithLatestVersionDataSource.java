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
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourceLinkField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.bundle.composite.BundleWithLatestVersionComposite;
import org.rhq.core.domain.criteria.BundleCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.BundleGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * @author John Mazzitelli
 */
public class BundlesWithLatestVersionDataSource extends RPCDataSource<BundleWithLatestVersionComposite> {

    private BundleGWTServiceAsync bundleService = GWTServiceLookup.getBundleService();

    public BundlesWithLatestVersionDataSource() {
        super();
        List<DataSourceField> fields = addDataSourceFields();
        addFields(fields);
    }

    @Override
    protected List<DataSourceField> addDataSourceFields() {
        List<DataSourceField> fields = super.addDataSourceFields();

        DataSourceIntegerField idField = new DataSourceIntegerField("id", "ID");
        idField.setPrimaryKey(true);
        fields.add(idField);

        DataSourceLinkField linkField = new DataSourceLinkField("link", "Name");
        fields.add(linkField);

        DataSourceTextField descriptionField = new DataSourceTextField("description", "Description");
        fields.add(descriptionField);

        DataSourceTextField latestVersionField = new DataSourceTextField("latestVersion", "Latest Version");
        fields.add(latestVersionField);

        DataSourceIntegerField deploymentCountField = new DataSourceIntegerField("versionsCount", "Versions Count");
        fields.add(deploymentCountField);

        return fields;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response) {

        BundleCriteria criteria = new BundleCriteria();
        criteria.setPageControl(getPageControl(request));
        if (request.getCriteria().getValues().get("tagNamespace") != null) {
            criteria.addFilterTagNamespace((String) request.getCriteria().getValues().get("tagNamespace"));
        }

        if (request.getCriteria().getValues().get("tagSemantic") != null) {
            criteria.addFilterTagSemantic((String) request.getCriteria().getValues().get("tagSemantic"));
        }

        if (request.getCriteria().getValues().get("tagName") != null) {
            criteria.addFilterTagName((String) request.getCriteria().getValues().get("tagName"));
        }

        bundleService.findBundlesWithLatestVersionCompositesByCriteria(criteria,
            new AsyncCallback<PageList<BundleWithLatestVersionComposite>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError("Failed to load bundle-with-latest-version data", caught);
                    response.setStatus(DSResponse.STATUS_FAILURE);
                    processResponse(request.getRequestId(), response);
                }

                public void onSuccess(PageList<BundleWithLatestVersionComposite> result) {
                    response.setData(buildRecords(result));
                    response.setTotalRows(result.getTotalSize());
                    processResponse(request.getRequestId(), response);
                }
            });

    }

    @Override
    public BundleWithLatestVersionComposite copyValues(ListGridRecord from) {
        Integer idAttrib = from.getAttributeAsInt("id");
        String nameAttrib = from.getAttribute("name");
        String descriptionAttrib = from.getAttribute("description");
        String latestVersionAttrib = from.getAttribute("latestVersion");
        Integer versionsCountAttrib = from.getAttributeAsInt("versionsCount");

        return new BundleWithLatestVersionComposite(idAttrib, nameAttrib, descriptionAttrib, latestVersionAttrib,
            versionsCountAttrib.longValue());
    }

    @Override
    public ListGridRecord copyValues(BundleWithLatestVersionComposite from) {
        ListGridRecord record = new ListGridRecord();

        record.setAttribute("id", from.getBundleId());
        record.setAttribute("name", from.getBundleName());

        record.setAttribute("link", "#Bundles/Bundle/" + from.getBundleId());
        record.setLinkText(from.getBundleName());

        record.setAttribute("description", from.getBundleDescription());
        record.setAttribute("latestVersion", from.getLatestVersion());
        record.setAttribute("versionsCount", Integer.valueOf(from.getVersionsCount().intValue())); // want int, not long

        record.setAttribute("object", from);

        return record;

    }
}
