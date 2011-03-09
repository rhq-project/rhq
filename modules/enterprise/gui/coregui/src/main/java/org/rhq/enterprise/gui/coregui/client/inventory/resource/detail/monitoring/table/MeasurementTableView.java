/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.table;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.events.CloseClientEvent;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.measurement.UserPreferencesMeasurementRangeEditor;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.MeasurementConverterClient;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableListGrid;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableWindow;

/**
 * Views a resource's measurements in a tabular view.
 * 
 * @author John Mazzitelli
 */
public class MeasurementTableView extends Table<MeasurementTableDataSource> {

    private final int resourceId;

    public MeasurementTableView(String locatorId, int resourceId) {
        super(locatorId);
        this.resourceId = resourceId;
        setDataSource(new MeasurementTableDataSource(resourceId));
    }

    protected void configureTable() {
        ArrayList<ListGridField> fields = getDataSource().getListGridFields();
        setListGridFields(fields.toArray(new ListGridField[0]));
        addExtraWidget(new UserPreferencesMeasurementRangeEditor(extendLocatorId("range")), true);
        addTableAction(extendLocatorId("liveValue"), MSG.view_measureTable_getLive(), new TableAction() {
            @Override
            public boolean isEnabled(ListGridRecord[] selection) {
                return selection != null && selection.length > 0;
            }

            @Override
            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                if (selection == null || selection.length == 0) {
                    return;
                }
                // keyed on metric name - string[0] is the metric label, [1] is the units
                final HashMap<String, String[]> scheduleNamesAndUnits = new HashMap<String, String[]>();
                int[] definitionIds = new int[selection.length];
                int i = 0;
                for (ListGridRecord record : selection) {
                    Integer defId = record.getAttributeAsInt(MeasurementTableDataSource.FIELD_METRIC_DEF_ID);
                    definitionIds[i++] = defId.intValue();

                    String name = record.getAttribute(MeasurementTableDataSource.FIELD_METRIC_NAME);
                    String label = record.getAttribute(MeasurementTableDataSource.FIELD_METRIC_LABEL);
                    String units = record.getAttribute(MeasurementTableDataSource.FIELD_METRIC_UNITS);
                    if (units == null || units.length() < 1) {
                        units = MeasurementUnits.NONE.name();
                    }

                    scheduleNamesAndUnits.put(name, new String[] { label, units });
                }

                // actually go out and ask the agents for the data
                GWTServiceLookup.getMeasurementDataService(60000).findLiveData(resourceId, definitionIds,
                    new AsyncCallback<Set<MeasurementData>>() {
                        @Override
                        public void onSuccess(Set<MeasurementData> result) {
                            if (result == null) {
                                result = new HashSet<MeasurementData>(0);
                            }
                            ArrayList<ListGridRecord> records = new ArrayList<ListGridRecord>(result.size());
                            for (MeasurementData data : result) {
                                String[] nameAndUnits = scheduleNamesAndUnits.get(data.getName());
                                if (nameAndUnits != null) {
                                    double doubleValue;
                                    if (data.getValue() instanceof Number) {
                                        doubleValue = ((Number) data.getValue()).doubleValue();
                                    } else {
                                        doubleValue = Double.parseDouble(data.getValue().toString());
                                    }
                                    String value = MeasurementConverterClient.formatToSignificantPrecision(
                                        new double[] { doubleValue }, MeasurementUnits.valueOf(nameAndUnits[1]), true)[0];

                                    ListGridRecord record = new ListGridRecord();
                                    record.setAttribute("name", nameAndUnits[0]);
                                    record.setAttribute("value", value);
                                    records.add(record);
                                }
                            }
                            showLiveData(records);
                        }

                        @Override
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError(MSG.view_measureTable_getLive_failure(), caught);
                        }
                    });
            }
        });
    }

    private void showLiveData(ArrayList<ListGridRecord> records) {
        final LocatableWindow liveDataWindow = new LocatableWindow(extendLocatorId("liveDataWindow"));
        liveDataWindow.setTitle(MSG.view_measureTable_live_title());
        liveDataWindow.setShowModalMask(true);
        liveDataWindow.setShowMinimizeButton(false);
        liveDataWindow.setShowMaximizeButton(true);
        liveDataWindow.setShowCloseButton(true);
        liveDataWindow.setShowResizer(true);
        liveDataWindow.setCanDragResize(true);
        liveDataWindow.setDismissOnEscape(true);
        liveDataWindow.setIsModal(true);
        liveDataWindow.setWidth(700);
        liveDataWindow.setHeight(425);
        liveDataWindow.setAutoCenter(true);
        liveDataWindow.centerInPage();
        liveDataWindow.addCloseClickHandler(new CloseClickHandler() {
            @Override
            public void onCloseClick(CloseClientEvent event) {
                liveDataWindow.destroy();
            }
        });

        LocatableListGrid liveDataGrid = new LocatableListGrid(extendLocatorId("liveDataListGrid"));
        liveDataGrid.setShowAllRecords(true);
        liveDataGrid.setData(records.toArray(new ListGridRecord[records.size()]));
        liveDataGrid.setSelectionType(SelectionStyle.NONE);
        ListGridField name = new ListGridField("name", MSG.common_title_metric());
        ListGridField value = new ListGridField("value", MSG.common_title_value());
        liveDataGrid.setFields(name, value);

        liveDataWindow.addItem(liveDataGrid);
        liveDataWindow.show();
    }
}
