/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
import java.util.List;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.ExpansionMode;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.CloseClickEvent;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.RecordCollapseEvent;
import com.smartgwt.client.widgets.grid.events.RecordCollapseHandler;
import com.smartgwt.client.widgets.grid.events.RecordExpandEvent;
import com.smartgwt.client.widgets.grid.events.RecordExpandHandler;
import com.smartgwt.client.widgets.grid.events.SelectionChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionEvent;
import com.smartgwt.client.widgets.grid.events.SortChangedHandler;
import com.smartgwt.client.widgets.grid.events.SortEvent;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.menu.Menu;

import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.UserSessionManager;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractD3GraphListView;
import org.rhq.enterprise.gui.coregui.client.inventory.common.graph.MetricGraphData;
import org.rhq.enterprise.gui.coregui.client.inventory.common.graph.RedrawGraphs;
import org.rhq.enterprise.gui.coregui.client.inventory.common.graph.graphtype.StackedBarMetricGraphImpl;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.DashboardLinkUtility;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.MetricD3Graph;
import org.rhq.enterprise.gui.coregui.client.util.BrowserUtility;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.MeasurementConverterClient;
import org.rhq.enterprise.gui.coregui.client.util.preferences.MeasurementUserPreferences;

/**
 * Views a resource's metrics in a tabular view with sparkline graph and optional detailed d3 graph.
 *
 * @author John Mazzitelli
 * @author Mike Thompson
 */
public class MetricsTableView extends Table<MetricsViewDataSource> implements RedrawGraphs {

    private final Resource resource;
    private final AbstractD3GraphListView abstractD3GraphListView;

    private final MeasurementUserPreferences measurementUserPrefs;
    private final Menu addToDashboardMenu;

    public MetricsTableView(Resource resource, AbstractD3GraphListView abstractD3GraphListView) {
        super();
        this.resource = resource;
        this.abstractD3GraphListView = abstractD3GraphListView;
        setDataSource(new MetricsViewDataSource(resource));
        addToDashboardMenu = new Menu();
        addToDashboardMenu.setWidth(200);
        addToDashboardMenu.setItems(DashboardLinkUtility.buildMetricsMenu(resource.getResourceType(), resource,
            MSG.view_metric_addToDashboard()));
        measurementUserPrefs = new MeasurementUserPreferences(UserSessionManager.getUserPreferences());
    }

    /**
     * Creates this Table's list grid (called by onInit()). Subclasses can override this if they require a custom
     * subclass of ListGrid.
     *
     * @return this Table's list grid (must be an instance of ListGrid)
     */
    @Override
    protected ListGrid createListGrid() {
        return new MetricsTableListGrid(resource, addToDashboardMenu);
    }

    protected void configureTable() {
        ArrayList<ListGridField> fields = getDataSource().getListGridFields();
        setListGridFields(fields.toArray(new ListGridField[0]));

        addTableAction(MSG.view_measureTable_getLive(), new ShowLiveDataTableAction(this));
        addExtraWidget(addToDashboardMenu, false);

    }

    private static class ShowLiveDataTableAction implements TableAction {
        private MetricsTableView metricsTableView;

        public ShowLiveDataTableAction(MetricsTableView metricsTableView) {
            this.metricsTableView = metricsTableView;
        }

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
                Integer defId = record.getAttributeAsInt(MetricsViewDataSource.FIELD_METRIC_DEF_ID);
                definitionIds[i++] = defId;

                String name = record.getAttribute(MetricsViewDataSource.FIELD_METRIC_NAME);
                String label = record.getAttribute(MetricsViewDataSource.FIELD_METRIC_LABEL);
                String units = record.getAttribute(MetricsViewDataSource.FIELD_METRIC_UNITS);
                if (units == null || units.length() < 1) {
                    units = MeasurementUnits.NONE.name();
                }

                scheduleNamesAndUnits.put(name, new String[] { label, units });
            }

            // actually go out and ask the agents for the data
            GWTServiceLookup.getMeasurementDataService(60000).findLiveData(metricsTableView.resource.getId(),
                definitionIds, new AsyncCallback<Set<MeasurementData>>() {
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
                    metricsTableView.refreshTableInfo();
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

    }

    @Override
    /**
     * Redraw Graphs in this context means to refresh the table and redraw open graphs.
     */
    public void redrawGraphs() {
        Log.debug("MetricsView.redrawGraphs.");

        new Timer() {

            @Override
            public void run() {
                BrowserUtility.graphSparkLines();
            }
        }.schedule(150);

    }

    private class MetricsTableListGrid extends ListGrid {

        private static final int TREEVIEW_DETAIL_CHART_HEIGHT = 205;
        private static final int NUM_METRIC_POINTS = 60;
        private Resource resource;
        final private Menu addToDashboardMenu;

        public MetricsTableListGrid(final Resource resource, final Menu dashboardMenu) {
            super();
            this.resource = resource;
            this.addToDashboardMenu = dashboardMenu;
            this.addToDashboardMenu.disable();
            setCanExpandRecords(true);
            setCanExpandMultipleRecords(true);
            setExpansionMode(ExpansionMode.DETAIL_FIELD);
            addRecordExpandHandler(new RecordExpandHandler() {
                @Override
                public void onRecordExpand(RecordExpandEvent recordExpandEvent) {
                    redrawGraphs();
                }

            });
            addRecordCollapseHandler(new RecordCollapseHandler() {
                @Override
                public void onRecordCollapse(RecordCollapseEvent recordCollapseEvent) {
                    redrawGraphs();
                }
            });
            addSortChangedHandler(new SortChangedHandler() {
                @Override
                public void onSortChanged(SortEvent sortEvent) {
                    redrawGraphs();
                }
            });

        }

        @Override
        protected Canvas getExpansionComponent(final ListGridRecord record) {
            final Integer definitionId = record.getAttributeAsInt(MetricsViewDataSource.FIELD_METRIC_DEF_ID);
            final Integer resourceId = record.getAttributeAsInt(MetricsViewDataSource.FIELD_RESOURCE_ID);
            VLayout vLayout = new VLayout();
            vLayout.setPadding(5);

            final String chartId = "rChart-" + resourceId + "-" + definitionId;
            Log.debug("getExpansionComponent for: " + chartId);
            HTMLFlow htmlFlow = new HTMLFlow(MetricD3Graph.createGraphMarkerTemplate(chartId,
                TREEVIEW_DETAIL_CHART_HEIGHT));
            vLayout.addMember(htmlFlow);

            int[] definitionArrayIds = new int[1];
            definitionArrayIds[0] = definitionId;
            GWTServiceLookup.getMeasurementDataService().findDataForResource(resourceId, definitionArrayIds,
                measurementUserPrefs.getMetricRangePreferences().begin,
                measurementUserPrefs.getMetricRangePreferences().end, NUM_METRIC_POINTS,
                new AsyncCallback<List<List<MeasurementDataNumericHighLowComposite>>>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        Log.debug("Error retrieving recent metrics charting data for resource [" + resourceId + "]:"
                            + caught.getMessage());
                    }

                    @Override
                    public void onSuccess(List<List<MeasurementDataNumericHighLowComposite>> results) {
                        if (!results.isEmpty()) {

                            //load the data results for the given metric definition
                            List<MeasurementDataNumericHighLowComposite> measurementList = results.get(0);

                            MeasurementDefinition measurementDefinition = null;
                            for (MeasurementDefinition definition : resource.getResourceType().getMetricDefinitions()) {
                                if (definition.getId() == definitionId) {
                                    measurementDefinition = definition;
                                    break;
                                }
                            }

                            MetricGraphData metricGraphData = MetricGraphData.createForResource(resourceId,
                                resource.getName(), measurementDefinition, measurementList, null);
                            metricGraphData.setHideLegend(true);

                            StackedBarMetricGraphImpl graph = GWT.create(StackedBarMetricGraphImpl.class);
                            graph.setMetricGraphData(metricGraphData);
                            final MetricD3Graph graphView = new MetricD3Graph(graph, abstractD3GraphListView);
                            new Timer() {
                                @Override
                                public void run() {
                                    graphView.drawJsniChart();
                                    BrowserUtility.graphSparkLines();

                                }
                            }.schedule(150);

                        } else {
                            Log.warn("No chart data retrieving for resource [" + resourceId + "-" + definitionId + "]");

                        }
                    }
                });

            return vLayout;
        }
    }

}
