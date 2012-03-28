/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation version 2 of the License.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program; if not, write to the Free Software
 *  * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

package org.rhq.enterprise.gui.coregui.client.report.inventory;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceBooleanField;
import com.smartgwt.client.data.fields.DataSourceImageField;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourceLinkField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.criteria.Criteria;
import org.rhq.core.domain.resource.composite.ResourceInstallCount;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * @author jsanda
 */
public class InventorySummaryDataSource extends RPCDataSource<ResourceInstallCount, Criteria> {

    public static final String COUNT = "count"; // long that we convert to int
    public static final String TYPENAME = "typeName"; // String
    public static final String TYPEPLUGIN = "typePlugin"; // String
    public static final String CATEGORY = "category"; // ResourceCategory
    public static final String TYPEID = "typeId"; // int
    public static final String VERSION = "version"; // String
    public static final String OBJECT = "object";
    public static final String EXPORT = "exportDetails";

    public InventorySummaryDataSource() {
        setFields(getFields());
    }

    @Override
    public ResourceInstallCount copyValues(Record from) {
        return null;
    }

    @Override
    public ListGridRecord copyValues(ResourceInstallCount from) {
        ListGridRecord record = new ListGridRecord();

        record.setAttribute(COUNT,
            Long.valueOf(from.getCount()).intValue()); // we'll never have over Integer.MAX_VALUE, overflow not a worry
        record.setAttribute(TYPENAME, from.getTypeName());
        record.setAttribute(TYPEPLUGIN, from.getTypePlugin());
        record.setAttribute(CATEGORY, from.getCategory().name());
        record.setAttribute(TYPEID, from.getTypeId());
        record.setAttribute(VERSION, from.getVersion());
        record.setAttribute(OBJECT, from);
        record.setAttribute(EXPORT, false);

        return record;
    }

    @Override
    public DataSourceField[] getFields() {
        return new DataSourceField[] {
            new DataSourceLinkField(TYPENAME),
            new DataSourceTextField(TYPEPLUGIN),
            new DataSourceImageField(CATEGORY),
            new DataSourceTextField(VERSION),
            new DataSourceIntegerField(COUNT),
            new DataSourceBooleanField(EXPORT)
        };
    }

    @Override
    protected Criteria getFetchCriteria(DSRequest request) {
        // we don't use criterias for this datasource, just return null
        return null;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response,
        final Criteria unused) {
        ResourceGWTServiceAsync resourceService = GWTServiceLookup.getResourceService();

        resourceService.findResourceInstallCounts(true, new AsyncCallback<List<ResourceInstallCount>>() {

            @Override
            public void onSuccess(List<ResourceInstallCount> result) {
                response.setData(buildRecords(result));
                response.setTotalRows(result.size());
                processResponse(request.getRequestId(), response);
            }

            @Override
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_reports_inventorySummary_failFetch(), caught);
                response.setStatus(DSResponse.STATUS_FAILURE);
                processResponse(request.getRequestId(), response);
            }
        });
    }

    @Override
    protected void executeUpdate(Record editedRecord, Record oldRecord, DSRequest request,
        DSResponse response) {
    }
}
