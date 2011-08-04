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

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.SortSpecifier;
import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.criteria.MeasurementScheduleCriteria;
import org.rhq.enterprise.gui.coregui.client.components.table.BooleanCellFormatter;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;

/**
 * A view that displays a non-paginated table of {@link org.rhq.core.domain.measurement.MeasurementSchedule measurement
 * schedule}s, along with the ability to sort, enable, disable, or update the collection interval on those schedules.
 *
 * @author Ian Springer
 */
public abstract class AbstractMeasurementScheduleListView extends Table {

    private static final SortSpecifier[] SORT_SPECIFIERS = new SortSpecifier[] {
        new SortSpecifier(MeasurementScheduleCriteria.SORT_FIELD_DISPLAY_NAME, SortDirection.ASCENDING)
    };

    public AbstractMeasurementScheduleListView(String locatorId, String title,
        AbstractMeasurementScheduleCompositeDataSource dataSource, Criteria criteria, String[] excludedFieldNames) {
        super(locatorId, title, criteria, SORT_SPECIFIERS, excludedFieldNames);
        setDataSource(dataSource);
    }

    public abstract boolean hasManageMeasurementsPermission();

    @Override
    public AbstractMeasurementScheduleCompositeDataSource getDataSource() {
        return (AbstractMeasurementScheduleCompositeDataSource) super.getDataSource();
    }

    protected void configureTable() {
        ListGrid listGrid = getListGrid();

        // Set widths and cell formatters on the fields.
        listGrid.getField(MeasurementScheduleCriteria.SORT_FIELD_DISPLAY_NAME).setWidth("20%");
        listGrid.getField(MeasurementScheduleCriteria.SORT_FIELD_DESCRIPTION).setWidth("40%");
        listGrid.getField(MeasurementScheduleCriteria.SORT_FIELD_DATA_TYPE).setWidth("10%");
        ListGridField enabledField = listGrid.getField(MeasurementScheduleCriteria.SORT_FIELD_ENABLED);
        enabledField.setWidth("10%");
        enabledField.setCellFormatter(new CollectionEnabledCellFormatter());
        ListGridField intervalField = listGrid.getField(MeasurementScheduleCriteria.SORT_FIELD_INTERVAL);
        intervalField.setCellFormatter(new CollectionIntervalCellFormatter());
        intervalField.setWidth("25%");

        // Add action buttons and widgets.
        addTableAction(extendLocatorId("Enable"), MSG.common_button_enable(), null, new TableAction() {
            public boolean isEnabled(ListGridRecord[] selection) {
                return ((selection.length >= 1) && hasManageMeasurementsPermission());
            }

            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                getDataSource().enableSchedules(AbstractMeasurementScheduleListView.this);
            }
        });
        addTableAction(extendLocatorId("Disable"), MSG.common_button_disable(), null, new TableAction() {
            public boolean isEnabled(ListGridRecord[] selection) {
                return ((selection.length >= 1) && hasManageMeasurementsPermission());
            }

            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                getDataSource().disableSchedules(AbstractMeasurementScheduleListView.this);
            }
        });
        addExtraWidget(new UpdateCollectionIntervalWidget(this.getLocatorId(), this), true);
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
                        result.append(hours).append(" ").append(MSG.common_label_hours());
                    }
                    if (minutes != 0) {
                        if (result.length() != 0) {
                            result.append(", ");
                        }
                        result.append(minutes).append(" ").append(MSG.common_label_minutes());
                    }
                }
                if (seconds != 0) {
                    if (result.length() != 0) {
                        result.append(", ");
                    }
                    result.append(seconds).append(" ").append(MSG.common_label_seconds());
                }
            }
            if (milliseconds != 0) {
                if (result.length() != 0) {
                    result.append(", ");
                }
                result.append(milliseconds).append(" ").append(MSG.common_label_milliseconds());
            }
            return result.toString();
        }

    }

}
