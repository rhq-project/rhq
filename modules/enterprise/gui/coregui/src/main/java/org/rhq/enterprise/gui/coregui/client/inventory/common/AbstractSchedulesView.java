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

import com.smartgwt.client.data.SortSpecifier;
import com.smartgwt.client.types.ListGridEditEvent;
import com.smartgwt.client.types.RowEndEditAction;
import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.events.ChangeEvent;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.MeasurementScheduleCriteria;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.MeasurementDataGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * A view that displays a non-paginated table of {@link org.rhq.core.domain.measurement.MeasurementSchedule measurement
 * schedule}s, along with the ability to sort, enable, disable, or update the collection interval on those schedules.
 *
 * @author Jay Shaughnessy
 * @author Ian Springer
 */
public abstract class AbstractSchedulesView extends Table<SchedulesDataSource> {

    protected MeasurementDataGWTServiceAsync measurementService = GWTServiceLookup.getMeasurementDataService();

    private static final SortSpecifier[] SORT_SPECIFIERS = new SortSpecifier[] { new SortSpecifier(
        MeasurementScheduleCriteria.SORT_FIELD_DISPLAY_NAME, SortDirection.ASCENDING) };

    private EntityContext entityContext;
    private boolean hasWriteAccess;

    protected SchedulesDataSource dataSource;

    public AbstractSchedulesView(String locatorId, String tableTitle, EntityContext entityContext,
        boolean hasWriteAccess) {

        super(locatorId, tableTitle, SORT_SPECIFIERS);
        this.entityContext = entityContext;
        this.hasWriteAccess = hasWriteAccess;

        setDataSource(getDataSource());
    }

    @Override
    public SchedulesDataSource getDataSource() {
        if (null == this.dataSource) {
            this.dataSource = new SchedulesDataSource(entityContext);
        }
        return this.dataSource;
    }

    @Override
    protected void configureTable() {
        ArrayList<ListGridField> listGridFields = getDataSource().getListGridFields();
        getListGrid().setFields(listGridFields.toArray(new ListGridField[listGridFields.size()]));
        getListGrid().setCanEdit(true);
        getListGrid().setModalEditing(true);
        getListGrid().setEditEvent(ListGridEditEvent.CLICK);
        getListGrid().setAutoSaveEdits(false);
        getListGrid().setListEndEditAction(RowEndEditAction.DONE);

        setupTableInteractions(this.hasWriteAccess);
        super.configureTable();
        }

    protected void setupTableInteractions(final boolean hasWriteAccess) {

        ListGridField enabledField = getListGrid().getField(SchedulesDataSource.ATTR_ENABLED);
        CheckboxItem enabledCheckboxItem = new CheckboxItem();
        enabledCheckboxItem.addChangeHandler(new com.smartgwt.client.widgets.form.fields.events.ChangeHandler() {
            @Override
            public void onChange(com.smartgwt.client.widgets.form.fields.events.ChangeEvent event) {
                boolean enabled = Boolean.valueOf(event.getItem().getValue() + "");

                if(!enabled){
                   enableSchedules();
                }else {
                   disableSchedules();
                }
            }
        });
        enabledField.setEditorType(enabledCheckboxItem);

        ListGridField intervalField = getListGrid().getField(SchedulesDataSource.ATTR_INTERVAL);

       final ScheduleTableCollectionWidget collectionIntervalWidget = new ScheduleTableCollectionWidget("collectionInterval", this);

        collectionIntervalWidget.addChangeHandler(new com.smartgwt.client.widgets.form.fields.events.ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                Log.debug(" Collection Interval: "+ collectionIntervalWidget.getValue());
                updateSchedules(collectionIntervalWidget.getInterval());
            }
        });
       intervalField.setEditorType(collectionIntervalWidget);

    }

    protected abstract void enableSchedules(int[] measurementDefinitionIds,
        List<String> measurementDefinitionDisplayNames);

    protected abstract void disableSchedules(int[] measurementDefinitionIds,
        List<String> measurementDefinitionDisplayNames);

    protected abstract void updateSchedules(final int[] measurementDefinitionIds,
        List<String> measurementDefinitionDisplayNames, final long interval);

    public void disableSchedules() {
        int[] ids = getMeasurementDefinitionIds();
        List<String> displayNames = getMeasurementDefinitionDisplayNames();
        disableSchedules(ids, displayNames);
    }

    public void enableSchedules() {
        int[] ids = getMeasurementDefinitionIds();
        List<String> displayNames = getMeasurementDefinitionDisplayNames();
        enableSchedules(ids, displayNames);
    }

    public void updateSchedules(long interval) {
        int[] ids = getMeasurementDefinitionIds();
        List<String> displayNames = getMeasurementDefinitionDisplayNames();
        updateSchedules(ids, displayNames, interval);
    }

    private int[] getMeasurementDefinitionIds() {
        ListGridRecord[] records = getListGrid().getSelectedRecords();

        int[] measurementDefinitionIds = new int[records.length];
        for (int i = 0, selectionLength = records.length; i < selectionLength; i++) {
            ListGridRecord record = records[i];
            Integer measurementDefinitionId = record
                .getAttributeAsInt(MeasurementScheduleCriteria.SORT_FIELD_DEFINITION_ID);
            measurementDefinitionIds[i] = measurementDefinitionId;
        }
        return measurementDefinitionIds;
    }

    private List<String> getMeasurementDefinitionDisplayNames() {
        ListGridRecord[] records = getListGrid().getSelectedRecords();
        List<String> displayNames = new ArrayList<String>(records.length);
        for (ListGridRecord record : records) {
            String displayName = record.getAttributeAsString(MeasurementScheduleCriteria.SORT_FIELD_DISPLAY_NAME);
            displayNames.add(displayName);
        }
        return displayNames;
    }

    public boolean hasWriteAccess() {
        return hasWriteAccess;
    }
}
