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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.criteria.MeasurementDefinitionCriteria;
import org.rhq.core.domain.criteria.MeasurementScheduleCriteria;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * @author Greg Hinkle
 */
public class ResourceScheduledMetricDatasource extends RPCDataSource<MeasurementDefinition> {

    public ResourceScheduledMetricDatasource() {
        List<DataSourceField> fields = addDataSourceFields();
        addFields(fields);
    }

    @Override
    protected List<DataSourceField> addDataSourceFields() {
        List<DataSourceField> fields = super.addDataSourceFields();

        DataSourceIntegerField id = new DataSourceIntegerField("id");
        id.setPrimaryKey(true);
        fields.add(id);

        DataSourceTextField name = new DataSourceTextField("name");
        fields.add(name);

        DataSourceTextField displayName = new DataSourceTextField("displayName");
        fields.add(displayName);

        DataSourceTextField description = new DataSourceTextField("description");
        fields.add(description);

        DataSourceTextField units = new DataSourceTextField("units");
        fields.add(units);

        DataSourceTextField numericType = new DataSourceTextField("numericType");
        fields.add(numericType);

        DataSourceTextField category = new DataSourceTextField("category");
        fields.add(category);

        return fields;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response) {

        if (request.getCriteria().getValues().containsKey("id")) {
            MeasurementDefinitionCriteria criteria = new MeasurementDefinitionCriteria();

            criteria.addFilterId(request.getCriteria().getAttributeAsInt("id"));
            GWTServiceLookup.getMeasurementDataService().findMeasurementDefinitionsByCriteria(criteria,
                new AsyncCallback<PageList<MeasurementDefinition>>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError("Failed to load metric definitions", caught);
                    }

                    public void onSuccess(PageList<MeasurementDefinition> result) {
                        response.setData(buildRecords(result));
                        processResponse(request.getRequestId(), response);
                    }
                });

        } else if (request.getCriteria().getValues().containsKey("resourceId")) {
            MeasurementScheduleCriteria criteria = new MeasurementScheduleCriteria();
            criteria.fetchDefinition(true);

            criteria.addFilterResourceId(request.getCriteria().getAttributeAsInt("resourceId"));

            GWTServiceLookup.getMeasurementDataService().findMeasurementSchedulesByCriteria(criteria,
                new AsyncCallback<PageList<MeasurementSchedule>>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError("Failed to load metric schedules", caught);
                    }

                    public void onSuccess(PageList<MeasurementSchedule> result) {
                        response.setData(buildRecords(result));
                        processResponse(request.getRequestId(), response);
                    }
                });
        } else {
            processResponse(request.getRequestId(), response);
        }
    }

    private ListGridRecord[] buildRecords(PageList<MeasurementSchedule> list) {
        PageList<MeasurementDefinition> definitions = new PageList<MeasurementDefinition>();

        for (MeasurementSchedule schedule : list) {
            if (schedule.isEnabled() && schedule.getDefinition().getDataType() == DataType.MEASUREMENT) {
                definitions.add(schedule.getDefinition());
            }
        }

        return buildRecords(definitions);
    }

    @Override
    public MeasurementDefinition copyValues(Record from) {
        return null; // TODO: Implement this method.
    }

    @Override
    public ListGridRecord copyValues(MeasurementDefinition from) {
        ListGridRecord record = new ListGridRecord();

        record.setAttribute("id", from.getId());
        record.setAttribute("name", from.getName());
        record.setAttribute("displayName", from.getDisplayName());
        record.setAttribute("description", from.getDescription());
        record.setAttribute("units", from.getUnits().name());
        record.setAttribute("numericType", from.getNumericType().name());
        record.setAttribute("category", from.getCategory().name());
        return record;
    }
}
