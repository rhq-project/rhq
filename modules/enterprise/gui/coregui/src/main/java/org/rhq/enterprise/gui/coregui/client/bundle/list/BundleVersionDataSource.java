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

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
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
    private int bundleId;

    public BundleVersionDataSource() {
        DataSourceIntegerField idField = new DataSourceIntegerField("id", "ID");
        idField.setPrimaryKey(true);
        addField(idField);

        DataSourceTextField latestVersionField = new DataSourceTextField("version", "Version");
        addField(latestVersionField);

        DataSourceTextField nameField = new DataSourceTextField("name", "Name");
        addField(nameField);

        DataSourceTextField descriptionField = new DataSourceTextField("description", "Description");
        addField(descriptionField);



        DataSourceIntegerField deploymentCountField = new DataSourceIntegerField("fileCount", "File Count");
        addField(deploymentCountField);
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response) {

        BundleVersionCriteria criteria = new BundleVersionCriteria();
        criteria.fetchBundleFiles(true);
        criteria.fetchBundle(true);
        criteria.setPageControl(getPageControl(request));

        if (request.getCriteria().getValues().get("bundleId") != null) {
            criteria.addFilterBundleId(Integer.parseInt(String.valueOf(request.getCriteria().getValues().get("bundleId"))));
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
                CoreGUI.getErrorHandler().handleError("Failed to load bundle version data", caught);
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
    public BundleVersion copyValues(ListGridRecord from) {
        // can't I just get the "object" attribute and return it???
        Integer idAttrib = from.getAttributeAsInt("id");
        String nameAttrib = from.getAttribute("name");
        String descriptionAttrib = from.getAttribute("description");
        String versionAttrib = from.getAttribute("version");

        BundleVersion bv = new BundleVersion();
        bv.setId(idAttrib.intValue());
        bv.setName(nameAttrib);
        bv.setDescription(descriptionAttrib);
        bv.setVersion(versionAttrib);
        return bv;
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
