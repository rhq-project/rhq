/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.report.measurement;

import java.util.Collection;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.criteria.Criteria;
import org.rhq.core.domain.measurement.composite.MeasurementOOBComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.MeasurementConverterClient;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * @author Greg Hinkle
 */
public class MeasurementOOBDataSource extends RPCDataSource<MeasurementOOBComposite, Criteria> {

    private int maximumFactor = 0;

    public MeasurementOOBDataSource() {
        List<DataSourceField> fields = addDataSourceFields();
        addFields(fields);
    }

    @Override
    protected List<DataSourceField> addDataSourceFields() {
        List<DataSourceField> fields = super.addDataSourceFields();

        DataSourceTextField metricField = new DataSourceTextField("scheduleName", MSG
            .dataSource_measurementOob_field_scheduleName());
        fields.add(metricField);

        DataSourceTextField resourceField = new DataSourceTextField("resourceName", MSG
            .dataSource_measurementOob_field_resourceName());
        fields.add(resourceField);

        DataSourceTextField parentField = new DataSourceTextField("parentName", MSG
            .dataSource_measurementOob_field_parentName());
        fields.add(parentField);

        DataSourceTextField bandField = new DataSourceTextField("formattedBaseband", MSG
            .dataSource_measurementOob_field_formattedBaseband());
        fields.add(bandField);

        DataSourceTextField outlierField = new DataSourceTextField("formattedOutlier", MSG
            .dataSource_measurementOob_field_formattedOutlier());
        fields.add(outlierField);

        DataSourceTextField factorField = new DataSourceTextField("factor", MSG
            .dataSource_measurementOob_field_factor());
        fields.add(factorField);

        return fields;

    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response, final Criteria unused) {

        PageControl pc = getPageControl(request);

        GWTServiceLookup.getMeasurementDataService().getSchedulesWithOOBs(null, null, null, pc,
            new AsyncCallback<PageList<MeasurementOOBComposite>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.dataSource_measurementOob_error_fetchFailure(), caught);
                }

                public void onSuccess(PageList<MeasurementOOBComposite> result) {
                    response.setData(buildRecords(result));
                    response.setTotalRows(result.getTotalSize());
                    processResponse(request.getRequestId(), response);
                }
            });

    }

    @Override
    protected Criteria getFetchCriteria(DSRequest request) {
        // we don't use criterias for this datasource, just return null
        return null;
    }

    @Override
    public MeasurementOOBComposite copyValues(Record from) {
        throw new UnsupportedOperationException("OOBs Read only");
    }

    @Override
    public ListGridRecord[] buildRecords(Collection<MeasurementOOBComposite> dataObjects) {
        for (MeasurementOOBComposite oob : dataObjects) {
            if (oob.getFactor() > maximumFactor) {
                maximumFactor = oob.getFactor();
            }
        }

        return super.buildRecords(dataObjects);
    }

    @Override
    public ListGridRecord copyValues(MeasurementOOBComposite from) {
        applyFormatting(from);

        ListGridRecord record = new ListGridRecord();

        record.setAttribute("scheduleId", from.getScheduleId());
        record.setAttribute("scheduleName", from.getScheduleName());
        record.setAttribute("definitionId", from.getDefinitionId());
        record.setAttribute("resourceId", from.getResourceId());
        record.setAttribute("resourceName", from.getResourceName());

        record.setAttribute("factor", from.getFactor());
        record.setAttribute("formattedBaseband", from.getFormattedBaseband());
        record.setAttribute("formattedOutlier", from.getFormattedOutlier());
        record.setAttribute("blMin", from.getBlMin());
        record.setAttribute("blMax", from.getBlMax());
        record.setAttribute("parentId", from.getParentId());
        record.setAttribute("parentName", from.getParentName());

        int factorRankingWidth = (int) (((double) from.getFactor()) / (double) maximumFactor * 100d);

        record.setBackgroundComponent(new HTMLFlow("<div style=\"width: " + factorRankingWidth
            + "%; height: 100%; background-color: #A5B391;\">&nbsp;</div>"));

        return record;

    }

    private void applyFormatting(MeasurementOOBComposite oob) {
        oob.setFormattedOutlier(MeasurementConverterClient.format(oob.getOutlier(), oob.getUnits(), true));
        formatBaseband(oob);
    }

    private void formatBaseband(MeasurementOOBComposite oob) {
        String min = MeasurementConverterClient.format(oob.getBlMin(), oob.getUnits(), true);
        String max = MeasurementConverterClient.format(oob.getBlMax(), oob.getUnits(), true);
        oob.setFormattedBaseband(min + ", " + max);
    }
}
