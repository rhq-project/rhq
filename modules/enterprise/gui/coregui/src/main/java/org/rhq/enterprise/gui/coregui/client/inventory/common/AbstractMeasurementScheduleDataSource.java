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

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.fields.DataSourceBooleanField;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.criteria.MeasurementScheduleCriteria;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.MeasurementDataGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A server-side SmartGWT DataSource for reading and updating {@link MeasurementSchedule}s.
 *
 * @deprecated this class has been replaced by {@link AbstractMeasurementScheduleCompositeDataSource}, but
 *             I'm keeping it around in case we want to switch back to fetching schedules via criteria at
 *             some point. (ips, 10/06/10)
 *
 * @author Ian Springer
 */
@Deprecated
public abstract class AbstractMeasurementScheduleDataSource extends RPCDataSource<MeasurementSchedule> {
    private MeasurementDataGWTServiceAsync measurementService = GWTServiceLookup.getMeasurementDataService();

    protected AbstractMeasurementScheduleDataSource() {
        super();

        setCanMultiSort(true);

        List<DataSourceField> fields = createFields();
        addFields(fields);
    }

    protected List<DataSourceField> createFields() {
        List<DataSourceField> fields = new ArrayList<DataSourceField>();

        DataSourceIntegerField idField = new DataSourceIntegerField(MeasurementScheduleCriteria.SORT_FIELD_DEFINITION_ID,
                "Id");
        idField.setPrimaryKey(true);
        idField.setHidden(true);
        fields.add(idField);

        DataSourceTextField nameField = new DataSourceTextField(MeasurementScheduleCriteria.SORT_FIELD_DISPLAY_NAME,
                "Metric");
        fields.add(nameField);

        DataSourceTextField descriptionField = new DataSourceTextField(MeasurementScheduleCriteria.SORT_FIELD_DESCRIPTION,
                "Description");
        fields.add(descriptionField);

        DataSourceTextField typeField = new DataSourceTextField(MeasurementScheduleCriteria.SORT_FIELD_DATA_TYPE,
                "Type");
        fields.add(typeField);

        DataSourceBooleanField enabledField = new DataSourceBooleanField(MeasurementScheduleCriteria.SORT_FIELD_ENABLED,
                "Enabled?");
        fields.add(enabledField);

        DataSourceIntegerField intervalField = new DataSourceIntegerField(MeasurementScheduleCriteria.SORT_FIELD_INTERVAL,
                "Collection Interval");
        fields.add(intervalField);

        return fields;
    }

    protected void executeFetch(final DSRequest request, final DSResponse response) {
        final long startTime = System.currentTimeMillis();

        final MeasurementScheduleCriteria criteria = getCriteria(request);

        this.measurementService.findMeasurementSchedulesByCriteria(criteria, new AsyncCallback<PageList<MeasurementSchedule>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("Failed to fetch measurement schedules for criteria " + criteria,
                        caught);
                response.setStatus(RPCResponse.STATUS_FAILURE);
                processResponse(request.getRequestId(), response);
            }

            public void onSuccess(PageList<MeasurementSchedule> result) {
                long fetchDuration = System.currentTimeMillis() - startTime;
                com.allen_sauer.gwt.log.client.Log.info(result.size() + " measurement schedules fetched in: " + fetchDuration + "ms");

                response.setData(buildRecords(result));
                // For paging to work, we have to specify size of full result set.
                response.setTotalRows(result.getTotalSize());
                processResponse(request.getRequestId(), response);
            }
        });
    }

    protected MeasurementScheduleCriteria getCriteria(DSRequest request) {
        MeasurementScheduleCriteria criteria = new MeasurementScheduleCriteria();
        criteria.fetchDefinition(true);

        Criteria requestCriteria = request.getCriteria();
        if (requestCriteria != null) {
            Map values = requestCriteria.getValues();
            for (Object key : values.keySet()) {
                String fieldName = (String) key;
                if (fieldName.equals(MeasurementScheduleCriteria.FILTER_FIELD_RESOURCE_ID)) {
                    Integer resourceId = (Integer) values.get(fieldName);
                    criteria.addFilterResourceId(resourceId);
                } else if (fieldName.equals(MeasurementScheduleCriteria.FILTER_FIELD_RESOURCE_GROUP_ID)) {
                    Integer resourceGroupId = (Integer) values.get(fieldName);
                    criteria.addFilterResourceGroupId(resourceGroupId);
                }
                // TODO: Add support for other fields we need to filter by (e.g. resourceTypeId for metric templates).
            }
        }

        criteria.setPageControl(getPageControl(request));
        return criteria;
    }

    @Override
    public MeasurementSchedule copyValues(ListGridRecord from) {
        return null;
    }

    @Override
    public ListGridRecord copyValues(MeasurementSchedule from) {
        ListGridRecord record = new ListGridRecord();

        record.setAttribute(MeasurementScheduleCriteria.SORT_FIELD_DEFINITION_ID, from.getDefinition().getId());
        record.setAttribute(MeasurementScheduleCriteria.SORT_FIELD_DISPLAY_NAME, from.getDefinition().getDisplayName());
        record.setAttribute(MeasurementScheduleCriteria.SORT_FIELD_DESCRIPTION, from.getDefinition().getDescription());
        record.setAttribute(MeasurementScheduleCriteria.SORT_FIELD_DATA_TYPE,
                from.getDefinition().getDataType().name().toLowerCase());
        record.setAttribute(MeasurementScheduleCriteria.SORT_FIELD_ENABLED, from.isEnabled());
        record.setAttribute(MeasurementScheduleCriteria.SORT_FIELD_INTERVAL, from.getInterval());

        // TODO: resourceId and resourceGroupId (in subclasses)
        
        return record;
    }

    public void enableSchedules(AbstractMeasurementScheduleListView measurementScheduleListView) {
        int[] ids = getMeasurementDefinitionIds(measurementScheduleListView);
        List<String> displayNames = getMeasurementDefinitionDisplayNames(measurementScheduleListView);
        enableSchedules(measurementScheduleListView, ids, displayNames);
        measurementScheduleListView.refresh();
    }

    protected abstract void enableSchedules(AbstractMeasurementScheduleListView measurementScheduleListView,
                                            int[] measurementDefinitionIds, List<String> measurementDefinitionDisplayNames);

    public void disableSchedules(AbstractMeasurementScheduleListView measurementScheduleListView) {
        int[] ids = getMeasurementDefinitionIds(measurementScheduleListView);
        List<String> displayNames = getMeasurementDefinitionDisplayNames(measurementScheduleListView);
        disableSchedules(measurementScheduleListView, ids, displayNames);
        measurementScheduleListView.refresh();
    }

    protected abstract void disableSchedules(AbstractMeasurementScheduleListView measurementScheduleListView,
                                             int[] measurementDefinitionIds, List<String> measurementDefinitionDisplayNames);

    public void updateSchedules(AbstractMeasurementScheduleListView measurementScheduleListView, long interval) {
        int[] ids = getMeasurementDefinitionIds(measurementScheduleListView);
        List<String> displayNames = getMeasurementDefinitionDisplayNames(measurementScheduleListView);
        updateSchedules(measurementScheduleListView, ids, displayNames, interval);
        measurementScheduleListView.refresh();
    }

    private int[] getMeasurementDefinitionIds(AbstractMeasurementScheduleListView measurementScheduleListView) {
        ListGrid listGrid = measurementScheduleListView.getListGrid();
        ListGridRecord[] records = listGrid.getSelection();

        int[] measurementDefinitionIds = new int[records.length];
        for (int i = 0, selectionLength = records.length; i < selectionLength; i++) {
            ListGridRecord record = records[i];
            Integer measurementDefinitionId = record.getAttributeAsInt(MeasurementScheduleCriteria.SORT_FIELD_DEFINITION_ID);
            measurementDefinitionIds[i] = measurementDefinitionId;
        }
        return measurementDefinitionIds;
    }

    private List<String> getMeasurementDefinitionDisplayNames(AbstractMeasurementScheduleListView measurementScheduleListView) {
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
