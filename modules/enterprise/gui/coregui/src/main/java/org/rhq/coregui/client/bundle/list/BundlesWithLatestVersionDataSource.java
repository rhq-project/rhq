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
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.bundle.composite.BundleWithLatestVersionComposite;
import org.rhq.core.domain.criteria.BundleCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.LinkManager;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.RPCDataSource;

/**
 * @author John Mazzitelli
 */
public class BundlesWithLatestVersionDataSource extends RPCDataSource<BundleWithLatestVersionComposite, BundleCriteria> {

    public static final String FIELD_ID = "id";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_NAMELINK = "namelink";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_LATEST_VERSION = "latestVersion";
    public static final String FIELD_VERSIONS_COUNT = "deploymentCount";

    public BundlesWithLatestVersionDataSource() {
        super();
        List<DataSourceField> fields = addDataSourceFields();
        addFields(fields);
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response, final BundleCriteria criteria) {
        GWTServiceLookup.getBundleService().findBundlesWithLatestVersionCompositesByCriteria(criteria,
            new AsyncCallback<PageList<BundleWithLatestVersionComposite>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_bundle_list_loadWithLatestFailure(), caught);
                    response.setStatus(DSResponse.STATUS_FAILURE);
                    processResponse(request.getRequestId(), response);
                }

                public void onSuccess(PageList<BundleWithLatestVersionComposite> result) {
                    response.setData(buildRecords(result));
                    setPagingInfo(response, result);
                    processResponse(request.getRequestId(), response);
                }
            });
    }

    @Override
    protected BundleCriteria getFetchCriteria(final DSRequest request) {
        BundleCriteria criteria = new BundleCriteria();

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
        if (FIELD_LATEST_VERSION.equals(columnName)) {
            return null;
        }
        if (FIELD_VERSIONS_COUNT.equals(columnName)) {
            return null;
        }

        return super.getSortFieldForColumn(columnName);
    }

    @Override
    public BundleWithLatestVersionComposite copyValues(Record from) {
        return (BundleWithLatestVersionComposite) from.getAttributeAsObject("object");
    }

    @Override
    public ListGridRecord copyValues(BundleWithLatestVersionComposite from) {
        ListGridRecord record = new ListGridRecord();

        record.setAttribute(FIELD_ID, from.getBundleId());
        record.setAttribute(FIELD_NAME, from.getBundleName());
        record.setAttribute(FIELD_NAMELINK, LinkManager.getBundleLink(from.getBundleId()));
        record.setAttribute(FIELD_DESCRIPTION, from.getBundleDescription());
        record.setAttribute(FIELD_LATEST_VERSION, from.getLatestVersion());
        record.setAttribute(FIELD_VERSIONS_COUNT, Integer.valueOf(from.getVersionsCount().intValue())); // want int, not long

        record.setAttribute("object", from);

        return record;

    }
}
