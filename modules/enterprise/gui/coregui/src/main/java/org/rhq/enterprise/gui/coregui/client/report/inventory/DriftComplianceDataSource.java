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
import com.smartgwt.client.data.fields.DataSourceBooleanField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.criteria.Criteria;
import org.rhq.core.domain.resource.composite.ResourceInstallCount;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGWTServiceAsync;

/**
 * @author jsanda
 */
public class DriftComplianceDataSource extends InventorySummaryDataSource {

    public static final String IN_COMPLIANCE = "inCompliance";

    @Override
    public DataSourceField[] getFields() {
        DataSourceField[] fields = super.getFields();
        DataSourceField[] newFields = new DataSourceField[fields.length + 1];

        for (int i = 0; i < fields.length - 1; ++i) {
            newFields[i] = fields[i];
        }
        newFields[newFields.length - 2] = new DataSourceBooleanField(IN_COMPLIANCE);
        newFields[newFields.length - 1] = fields[fields.length -1];

        return newFields;
    }

    @Override
    public ListGridRecord copyValues(ResourceInstallCount from) {
        ListGridRecord record = super.copyValues(from);
        if (from.getNumDriftTemplates() > 0) {
            record.setAttribute(IN_COMPLIANCE, Boolean.toString(from.isInCompliance()));
        }
        return record;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response, Criteria unused) {
        ResourceGWTServiceAsync resourceService = GWTServiceLookup.getResourceService();

        resourceService.findResourceComplianceCounts(new AsyncCallback<List<ResourceInstallCount>>() {
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
}
