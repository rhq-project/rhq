/*
 * RHQ Management Platform
 * Copyright (C) 2012 Red Hat, Inc.
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

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.Criteria;
import org.rhq.core.domain.criteria.MeasurementScheduleCriteria;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.composite.MeasurementScheduleComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.table.BooleanCellFormatter;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.MeasurementDataGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * A server-side SmartGWT DataSource for reading and updating {@link MeasurementScheduleComposite}s.
 *
 * @author JayShaughnessy
 * @author Ian Springer
 */
public class SchedulesDataSource extends RPCDataSource<MeasurementScheduleComposite, Criteria> {

    public static final String ATTR_DATA_TYPE = MeasurementScheduleCriteria.SORT_FIELD_DATA_TYPE;
    public static final String ATTR_DEFINITION_ID = "definitionId";
    public static final String ATTR_DESCRIPTION = "description";
    public static final String ATTR_DISPLAY_NAME = MeasurementScheduleCriteria.SORT_FIELD_DISPLAY_NAME;
    public static final String ATTR_ENABLED = "enabled";
    public static final String ATTR_INTERVAL = "interval";

    private MeasurementDataGWTServiceAsync measurementService = GWTServiceLookup.getMeasurementDataService();

    protected EntityContext entityContext;

    public SchedulesDataSource() {
        this(EntityContext.forSubsystemView());
    }

    public SchedulesDataSource(EntityContext entityContext) {
        this.entityContext = entityContext;
        addDataSourceFields();
    }

    /**
     * The view that contains the list grid which will display this datasource's data will call this
     * method to get the field information which is used to control the display of the data.
     * 
     * @return list grid fields used to display the datasource data
     */
    public ArrayList<ListGridField> getListGridFields() {
        ArrayList<ListGridField> fields = new ArrayList<ListGridField>();
        
        ListGridField scheduleIdField = new ListGridField(ATTR_DEFINITION_ID, MSG.dataSource_traits_field_definitionID());
        scheduleIdField.setHidden(true);
        fields.add(scheduleIdField);

        ListGridField displayNameField = new ListGridField(ATTR_DISPLAY_NAME, MSG.common_title_metric());
        fields.add(displayNameField);

        ListGridField descriptionField = new ListGridField(ATTR_DESCRIPTION, MSG.common_title_description());
        fields.add(descriptionField);

        ListGridField typeField = new ListGridField(ATTR_DATA_TYPE, MSG.common_title_type());
        fields.add(typeField);

        ListGridField enabledField = new ListGridField(ATTR_ENABLED, MSG.common_title_enabled());
        enabledField.setCellFormatter(new CollectionEnabledCellFormatter());
        fields.add(enabledField);

        ListGridField intervalField = new ListGridField(ATTR_INTERVAL, MSG.view_inventory_collectionInterval());
        intervalField.setCellFormatter(new CollectionIntervalCellFormatter());
        fields.add(intervalField);

        displayNameField.setWidth("20%");
        descriptionField.setWidth("40%");
        typeField.setWidth("10%");
        enabledField.setWidth("10%");
        intervalField.setWidth("*");

        return fields;
    }

    @Override
    protected Criteria getFetchCriteria(DSRequest request) {
        // we don't use criterias for this datasource, just return null
        return null;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response, final Criteria unused) {

        this.measurementService.getMeasurementScheduleCompositesByContext(entityContext,
            new AsyncCallback<PageList<MeasurementScheduleComposite>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(
                        MSG.dataSource_schedules_loadFailedContext(entityContext.toString()), caught);
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

    @Override
    public MeasurementScheduleComposite copyValues(Record from) {
        return null;
    }

    @Override
    public ListGridRecord copyValues(MeasurementScheduleComposite from) {

        ListGridRecord record = new ListGridRecord();
        MeasurementDefinition measurementDefinition = from.getMeasurementDefinition();

        record.setAttribute(ATTR_DEFINITION_ID, measurementDefinition.getId());
        record.setAttribute(ATTR_DISPLAY_NAME, measurementDefinition.getDisplayName());
        record.setAttribute(ATTR_DESCRIPTION, measurementDefinition.getDescription());
        record.setAttribute(ATTR_DATA_TYPE, measurementDefinition.getDataType().name().toLowerCase());
        record.setAttribute(ATTR_ENABLED, from.getCollectionEnabled());
        record.setAttribute(ATTR_INTERVAL, from.getCollectionInterval());

        return record;
    }

    protected class CollectionEnabledCellFormatter extends BooleanCellFormatter {
        @Override
        public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
            String result = super.format(value, record, rowNum, colNum);
            return ("".equals(result)) ? MSG.view_inventory_mixed() : result;
        }
    }

    protected class CollectionIntervalCellFormatter implements CellFormatter {

        public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
            if (value == null) {
                return MSG.view_inventory_mixed();
            }

            long milliseconds = ((Number) value).longValue();
            if (milliseconds == 0) {
                return MSG.view_inventory_mixed();
            }

            StringBuilder result = new StringBuilder();
            if (milliseconds > 1000) {
                long seconds = milliseconds / 1000;
                milliseconds = milliseconds % 1000;
                if (seconds >= 60) {
                    long minutes = seconds / 60;
                    seconds = seconds % 60;
                    if (minutes > 60) {
                        long hours = minutes / 60;
                        minutes = minutes % 60;
                        result.append(hours).append(" ").append(MSG.common_unit_hours());
                    }
                    if (minutes != 0) {
                        if (result.length() != 0) {
                            result.append(", ");
                        }
                        result.append(minutes).append(" ").append(MSG.common_unit_minutes());
                    }
                }
                if (seconds != 0) {
                    if (result.length() != 0) {
                        result.append(", ");
                    }
                    result.append(seconds).append(" ").append(MSG.common_unit_seconds());
                }
            }
            if (milliseconds != 0) {
                if (result.length() != 0) {
                    result.append(", ");
                }
                result.append(milliseconds).append(" ").append(MSG.common_unit_milliseconds());
            }
            return result.toString();
        }
    }

}
