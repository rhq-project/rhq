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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.ExpansionMode;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.CloseClickEvent;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.events.DrawEvent;
import com.smartgwt.client.widgets.events.DrawHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.RecordExpandEvent;
import com.smartgwt.client.widgets.grid.events.RecordExpandHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.FullHTMLPane;
import org.rhq.enterprise.gui.coregui.client.components.measurement.UserPreferencesMeasurementRangeEditor;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;
import org.rhq.enterprise.gui.coregui.client.drift.DriftSnapshotDataSource;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.InventoryView;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.summary.AbstractActivityView.ChartViewWindow;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.D3GraphListView;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.MeasurementConverterClient;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

/**
 * Views a resource's metrics in a tabular view with sparkline graph
 * and optional detailed graph .
 * 
 * @author John Mazzitelli
 * @author Mike Thompson
 */
public class MeasurementTableView extends Table<MeasurementTableDataSource> {

    private final int resourceId;

    public MeasurementTableView(int resourceId) {
        super();
        this.resourceId = resourceId;
        setDataSource(new MeasurementTableDataSource(resourceId));
    }

    /**
     * Creates this Table's list grid (called by onInit()). Subclasses can override this if they require a custom subclass
     * of ListGrid.
     *
     * @return this Table's list grid (must be an instance of ListGrid)
     */
    @Override
    protected ListGrid createListGrid() {
        return new MetricsTableListGrid();
    }

    protected void configureTable() {
        ArrayList<ListGridField> fields = getDataSource().getListGridFields();
        setListGridFields(fields.toArray(new ListGridField[0]));
        this.getListGrid().setCanExpandRecords(true);
        this.getListGrid().addRecordExpandHandler(new RecordExpandHandler() {
            @Override
            public void onRecordExpand(RecordExpandEvent recordExpandEvent) {
                Log.debug("Record Expanded: ");

            }
        });

        //addExtraWidget(new UserPreferencesMeasurementRangeEditor(), true);
        addTableAction(MSG.view_measureTable_getLive(), new TableAction() {
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
                    definitionIds[i++] = defId;

                    String name = record.getAttribute(MeasurementTableDataSource.FIELD_METRIC_NAME);
                    String label = record.getAttribute(MeasurementTableDataSource.FIELD_METRIC_LABEL);
                    String units = record.getAttribute(MeasurementTableDataSource.FIELD_METRIC_UNITS);
                    if (units == null || units.length() < 1) {
                        units = MeasurementUnits.NONE.name();
                    }

                    scheduleNamesAndUnits.put(name, new String[]{label, units});
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
                                            doubleValue = ((Number)data.getValue()).doubleValue();
                                        }
                                        else {
                                            doubleValue = Double.parseDouble(data.getValue().toString());
                                        }
                                        String value = MeasurementConverterClient.formatToSignificantPrecision(
                                                new double[]{doubleValue}, MeasurementUnits.valueOf(nameAndUnits[1]), true)[0];

                                        ListGridRecord record = new ListGridRecord();
                                        record.setAttribute("name", nameAndUnits[0]);
                                        record.setAttribute("value", value);
                                        records.add(record);
                                    }
                                }
                                Collections.sort(records, new Comparator<ListGridRecord>() {
                                    public int compare(ListGridRecord o1, ListGridRecord o2) {
                                        return o1.getAttribute("name").compareTo(o2.getAttribute("name"));
                                    }
                                });
                                showLiveData(records);
                            }

                            @Override
                            public void onFailure(Throwable caught) {
                                CoreGUI.getErrorHandler().handleError(MSG.view_measureTable_getLive_failure(), caught);
                            }
                        });
            }
        });


        addTableAction(MSG.view_measureTable_addToDashboard(), new TableAction() {

            @Override
            public boolean isEnabled(ListGridRecord[] selection) {
                return selection != null && selection.length > 0;
            }

            @Override
            public void executeAction(ListGridRecord[] selection, Object actionValue) {

            }
        });

    }

    private void showLiveData(ArrayList<ListGridRecord> records) {
        final Window liveDataWindow = new Window();
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
            public void onCloseClick(CloseClickEvent event) {
                liveDataWindow.destroy();
                refreshTableInfo();
            }
        });

        ListGrid liveDataGrid = new ListGrid();
        liveDataGrid.setShowAllRecords(true);
        liveDataGrid.setData(records.toArray(new ListGridRecord[records.size()]));
        liveDataGrid.setSelectionType(SelectionStyle.NONE);
        ListGridField name = new ListGridField("name", MSG.common_title_metric());
        ListGridField value = new ListGridField("value", MSG.common_title_value());
        liveDataGrid.setFields(name, value);

        liveDataWindow.addItem(liveDataGrid);
        liveDataWindow.show();
    }

    private class MetricsTableListGrid extends ListGrid {
        public MetricsTableListGrid() {
            super();

            setCanExpandRecords(true);
            setCanExpandMultipleRecords(true);
            setExpansionMode(ExpansionMode.RELATED);
        }

        @Override
        protected Canvas getExpansionComponent(final ListGridRecord record) {
            final Integer definitionId = record.getAttributeAsInt(MeasurementTableDataSource.FIELD_METRIC_DEF_ID);
            final Integer resourceId = record.getAttributeAsInt(MeasurementTableDataSource.FIELD_RESOURCE_ID);
            VLayout vLayout = new VLayout(5);
            vLayout.setPadding(5);
            //vLayout.addMember(graphListView);

            final DynamicForm df = new DynamicForm();
            df.setNumCols(4);
            df.setDataSource(getDataSource());

            IButton saveButton = new IButton("Mike");
            IButton cancelButton = new IButton("Done");

            HLayout hLayout = new HLayout(10);
            hLayout.setAlign(Alignment.CENTER);
            hLayout.addMember(saveButton);
            hLayout.addMember(cancelButton);

            vLayout.addMember(df);
            vLayout.addMember(hLayout);


            //locate resource reference
            ResourceCriteria criteria = new ResourceCriteria();
            criteria.addFilterId(resourceId);

            //locate the resource
            GWTServiceLookup.getResourceService().findResourceCompositesByCriteria(criteria,
                    new AsyncCallback<PageList<ResourceComposite>>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            Log.debug("Error retrieving resource resource composite for resource [" + resourceId + "]:"
                                    + caught.getMessage());
                        }

                        @Override
                        public void onSuccess(PageList<ResourceComposite> results) {
                            if (!results.isEmpty()) {
                                Resource resource = results.get(0).getResource();
                                D3GraphListView graphListView = D3GraphListView.createSingleGraph(resource, definitionId, false);


                            }
                        }
            });

            return vLayout;


        }
    }

}
