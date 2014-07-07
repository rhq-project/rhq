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
package org.rhq.coregui.client.inventory.common;

import java.util.ArrayList;
import java.util.List;

import com.smartgwt.client.data.SortSpecifier;
import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.MeasurementScheduleCriteria;
import org.rhq.coregui.client.components.table.Table;
import org.rhq.coregui.client.components.table.TableAction;
import org.rhq.coregui.client.components.table.Table.TableActionInfo.ButtonColor;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.gwt.MeasurementDataGWTServiceAsync;

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

    public AbstractSchedulesView(String tableTitle, EntityContext entityContext, boolean hasWriteAccess) {

        super(tableTitle, SORT_SPECIFIERS);
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
        setupTableInteractions(this.hasWriteAccess);

        super.configureTable();
    }

    protected void setupTableInteractions(final boolean hasWriteAccess) {

        addTableAction(MSG.common_button_enable(), null, ButtonColor.BLUE, new TableAction() {
            public boolean isEnabled(ListGridRecord[] selection) {
                return ((selection.length >= 1) && hasWriteAccess);
            }

            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                enableSchedules();
            }
        });
        addTableAction(MSG.common_button_disable(), null, ButtonColor.GRAY, new TableAction() {
            public boolean isEnabled(ListGridRecord[] selection) {
                return ((selection.length >= 1) && hasWriteAccess);
            }

            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                disableSchedules();
            }
        });
        addExtraWidget(new UpdateCollectionIntervalWidget(this), true);
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
            Integer measurementDefinitionId = record.getAttributeAsInt(SchedulesDataSource.ATTR_DEFINITION_ID);
            measurementDefinitionIds[i] = measurementDefinitionId;
        }
        return measurementDefinitionIds;
    }

    private List<String> getMeasurementDefinitionDisplayNames() {
        ListGridRecord[] records = getListGrid().getSelectedRecords();
        List<String> displayNames = new ArrayList<String>(records.length);
        for (ListGridRecord record : records) {
            String displayName = record.getAttributeAsString(SchedulesDataSource.ATTR_DISPLAY_NAME);
            displayNames.add(displayName);
        }
        return displayNames;
    }

    public boolean hasWriteAccess() {
        return hasWriteAccess;
    }
}
