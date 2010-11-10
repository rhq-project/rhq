/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.inventory.common;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceBooleanField;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.MeasurementScheduleCriteria;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.composite.MeasurementScheduleComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.MeasurementDataGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * A server-side SmartGWT DataSource for reading and updating {@link MeasurementScheduleComposite}s.
 *
 * @author Ian Springer
 */
public abstract class AbstractMeasurementScheduleCompositeDataSource extends
    RPCDataSource<MeasurementScheduleComposite> {
    private MeasurementDataGWTServiceAsync measurementService = GWTServiceLookup.getMeasurementDataService();

    protected AbstractMeasurementScheduleCompositeDataSource() {
        super();

        setCanMultiSort(true);

        List<DataSourceField> fields = addDataSourceFields();
        addFields(fields);
    }

    @Override
    protected List<DataSourceField> addDataSourceFields() {
        List<DataSourceField> fields = super.addDataSourceFields();

        DataSourceIntegerField idField = new DataSourceIntegerField(
            MeasurementScheduleCriteria.SORT_FIELD_DEFINITION_ID, "Id");
        idField.setPrimaryKey(true);
        idField.setHidden(true);
        fields.add(idField);

        DataSourceTextField nameField = new DataSourceTextField(MeasurementScheduleCriteria.SORT_FIELD_DISPLAY_NAME,
            "Metric");
        fields.add(nameField);

        DataSourceTextField descriptionField = new DataSourceTextField(
            MeasurementScheduleCriteria.SORT_FIELD_DESCRIPTION, "Description");
        fields.add(descriptionField);

        DataSourceTextField typeField = new DataSourceTextField(MeasurementScheduleCriteria.SORT_FIELD_DATA_TYPE,
            "Type");
        fields.add(typeField);

        DataSourceBooleanField enabledField = new DataSourceBooleanField(
            MeasurementScheduleCriteria.SORT_FIELD_ENABLED, "Enabled?");
        fields.add(enabledField);

        DataSourceIntegerField intervalField = new DataSourceIntegerField(
            MeasurementScheduleCriteria.SORT_FIELD_INTERVAL, "Collection Interval");
        fields.add(intervalField);

        return fields;
    }

    protected void executeFetch(final DSRequest request, final DSResponse response) {
        final EntityContext entityContext = getEntityContext(request);

        this.measurementService.getMeasurementScheduleCompositesByContext(entityContext,
            new AsyncCallback<PageList<MeasurementScheduleComposite>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(
                        "Failed to fetch measurement schedules for context " + entityContext, caught);
                    response.setStatus(RPCResponse.STATUS_FAILURE);
                    processResponse(request.getRequestId(), response);
                }

                public void onSuccess(PageList<MeasurementScheduleComposite> result) {
                    response.setData(buildRecords(result));
                    // For paging to work, we have to specify size of full result set.
                    response.setTotalRows(result.getTotalSize());
                    processResponse(request.getRequestId(), response);
                }
            });
    }

    protected abstract EntityContext getEntityContext(DSRequest request);

    @Override
    public MeasurementScheduleComposite copyValues(Record from) {
        return null;
    }

    @Override
    public ListGridRecord copyValues(MeasurementScheduleComposite from) {
        ListGridRecord record = new ListGridRecord();

        MeasurementDefinition measurementDefinition = from.getMeasurementDefinition();
        record.setAttribute(MeasurementScheduleCriteria.SORT_FIELD_DEFINITION_ID, measurementDefinition.getId());
        record
            .setAttribute(MeasurementScheduleCriteria.SORT_FIELD_DISPLAY_NAME, measurementDefinition.getDisplayName());
        record.setAttribute(MeasurementScheduleCriteria.SORT_FIELD_DESCRIPTION, measurementDefinition.getDescription());
        record.setAttribute(MeasurementScheduleCriteria.SORT_FIELD_DATA_TYPE, measurementDefinition.getDataType()
            .name().toLowerCase());
        record.setAttribute(MeasurementScheduleCriteria.SORT_FIELD_ENABLED, from.getCollectionEnabled());
        record.setAttribute(MeasurementScheduleCriteria.SORT_FIELD_INTERVAL, from.getCollectionInterval());

        return record;
    }

    public void enableSchedules(AbstractMeasurementScheduleListView measurementScheduleListView) {
        int[] ids = getMeasurementDefinitionIds(measurementScheduleListView);
        List<String> displayNames = getMeasurementDefinitionDisplayNames(measurementScheduleListView);
        enableSchedules(measurementScheduleListView, ids, displayNames);
    }

    protected abstract void enableSchedules(AbstractMeasurementScheduleListView measurementScheduleListView,
        int[] measurementDefinitionIds, List<String> measurementDefinitionDisplayNames);

    public void disableSchedules(AbstractMeasurementScheduleListView measurementScheduleListView) {
        int[] ids = getMeasurementDefinitionIds(measurementScheduleListView);
        List<String> displayNames = getMeasurementDefinitionDisplayNames(measurementScheduleListView);
        disableSchedules(measurementScheduleListView, ids, displayNames);
    }

    protected abstract void disableSchedules(AbstractMeasurementScheduleListView measurementScheduleListView,
        int[] measurementDefinitionIds, List<String> measurementDefinitionDisplayNames);

    public void updateSchedules(AbstractMeasurementScheduleListView measurementScheduleListView, long interval) {
        int[] ids = getMeasurementDefinitionIds(measurementScheduleListView);
        List<String> displayNames = getMeasurementDefinitionDisplayNames(measurementScheduleListView);
        updateSchedules(measurementScheduleListView, ids, displayNames, interval);
    }

    private int[] getMeasurementDefinitionIds(AbstractMeasurementScheduleListView measurementScheduleListView) {
        ListGrid listGrid = measurementScheduleListView.getListGrid();
        ListGridRecord[] records = listGrid.getSelection();

        int[] measurementDefinitionIds = new int[records.length];
        for (int i = 0, selectionLength = records.length; i < selectionLength; i++) {
            ListGridRecord record = records[i];
            Integer measurementDefinitionId = record
                .getAttributeAsInt(MeasurementScheduleCriteria.SORT_FIELD_DEFINITION_ID);
            measurementDefinitionIds[i] = measurementDefinitionId;
        }
        return measurementDefinitionIds;
    }

    private List<String> getMeasurementDefinitionDisplayNames(
        AbstractMeasurementScheduleListView measurementScheduleListView) {
        ListGrid listGrid = measurementScheduleListView.getListGrid();
        ListGridRecord[] records = listGrid.getSelection();
        List<String> displayNames = new ArrayList<String>(records.length);
        for (ListGridRecord record : records) {
            String displayName = record.getAttributeAsString(MeasurementScheduleCriteria.SORT_FIELD_DISPLAY_NAME);
            displayNames.add(displayName);
        }
        return displayNames;
    }

    protected abstract void updateSchedules(final AbstractMeasurementScheduleListView measurementScheduleListView,
        final int[] measurementDefinitionIds, List<String> measurementDefinitionDisplayNames, final long interval);
}
