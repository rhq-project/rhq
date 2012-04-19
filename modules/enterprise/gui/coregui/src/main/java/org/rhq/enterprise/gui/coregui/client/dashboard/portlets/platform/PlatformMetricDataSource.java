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
package org.rhq.enterprise.gui.coregui.client.dashboard.portlets.platform;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.criteria.Criteria;
import org.rhq.core.domain.resource.composite.PlatformMetricsSummary;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

import static org.rhq.core.domain.resource.composite.PlatformMetricsSummary.CPUMetric;
import static org.rhq.core.domain.resource.composite.PlatformMetricsSummary.MemoryMetric;
import static org.rhq.core.domain.resource.composite.PlatformMetricsSummary.SwapMetric;

/**
 * @author John Sanda
 */
public class PlatformMetricDataSource extends RPCDataSource<PlatformMetricsSummary, Criteria> {

    PlatformSummaryPortlet view;

    public PlatformMetricDataSource(PlatformSummaryPortlet view) {
        super();
        this.view = view;

        List<DataSourceField> fields = addDataSourceFields();
        addFields(fields);
    }

    @Override
    protected List<DataSourceField> addDataSourceFields() {
        return new ArrayList<DataSourceField>();
    }

    @Override
    protected Criteria getFetchCriteria(DSRequest request) {
        return null;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response, Criteria criteria) {
        GWTServiceLookup.getPlatformUtilizationService().loadPlatformMetrics(
            new AsyncCallback<PageList<PlatformMetricsSummary>>() {
            @Override
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("Failed to load platform utilization data", caught);
                response.setStatus(RPCResponse.STATUS_FAILURE);
                processResponse(request.getRequestId(), response);
            }

            @Override
            public void onSuccess(PageList<PlatformMetricsSummary> result) {
                ListGridRecord[] records = buildRecords(result);
                response.setData(records);
                response.setTotalRows(records.length);
                processResponse(request.getRequestId(), response);
            }
        });
    }

    @Override
    public PlatformMetricsSummary copyValues(Record from) {
        return null;
    }

    @Override
    public ListGridRecord copyValues(PlatformMetricsSummary summary) {
        ListGridRecord record = new ListGridRecord();

        record.setAttribute("id", summary.getResource().getId());
        record.setAttribute("name", summary.getResource().getName());
        record.setAttribute("version", summary.getResource().getVersion());

        if (summary.isMetricsAvailable()) {
            record.setAttribute(CPUMetric.Idle.getProperty(), summary.getIdleCPU().getValue());
            record.setAttribute(MemoryMetric.Total.getProperty(), summary.getTotalMemory().getValue());
            record.setAttribute(MemoryMetric.ActualUsed.getProperty(), summary.getActualUsedMemory().getValue());
            record.setAttribute(SwapMetric.Total.getProperty(), summary.getTotalSwap().getValue());
            record.setAttribute(SwapMetric.Used.getProperty(), summary.getUsedSwap().getValue());
        } else {
            record.setAttribute(PlatformSummaryPortlet.FIELD_CPU, MSG.common_val_na());
            record.setAttribute(PlatformSummaryPortlet.FIELD_MEMORY, MSG.common_val_na());
            record.setAttribute(PlatformSummaryPortlet.FIELD_SWAP, MSG.common_val_na());
        }

        return record;
    }

}
