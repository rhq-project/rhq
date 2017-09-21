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

package org.rhq.coregui.client.inventory.resource.detail.monitoring.table;

import static org.rhq.coregui.client.inventory.resource.detail.monitoring.table.MetricsGridFieldName.METRIC_DEF_ID;
import static org.rhq.coregui.client.inventory.resource.detail.monitoring.table.MetricsGridFieldName.RESOURCE_ID;
import static org.rhq.coregui.client.inventory.resource.detail.monitoring.table.MetricsGridFieldName.DISPLAY_UNITS_NAME;
import static org.rhq.coregui.client.inventory.resource.detail.monitoring.table.MetricsGridFieldName
        .DISPLAY_UNITS_FAMILY;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.ExpansionMode;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.events.ChangeEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangeHandler;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.DataArrivedEvent;
import com.smartgwt.client.widgets.grid.events.DataArrivedHandler;
import com.smartgwt.client.widgets.grid.events.RecordCollapseEvent;
import com.smartgwt.client.widgets.grid.events.RecordCollapseHandler;
import com.smartgwt.client.widgets.grid.events.RecordExpandEvent;
import com.smartgwt.client.widgets.grid.events.RecordExpandHandler;
import com.smartgwt.client.widgets.grid.events.SelectionChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionEvent;
import com.smartgwt.client.widgets.grid.events.SortChangedHandler;
import com.smartgwt.client.widgets.grid.events.SortEvent;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.criteria.DashboardCriteria;
import org.rhq.core.domain.dashboard.Dashboard;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.components.table.Table;
import org.rhq.coregui.client.dashboard.portlets.inventory.resource.graph.ResourceD3GraphPortlet;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.inventory.common.AbstractD3GraphListView;
import org.rhq.coregui.client.inventory.common.graph.CustomDateRangeState;
import org.rhq.coregui.client.inventory.common.graph.MetricGraphData;
import org.rhq.coregui.client.inventory.common.graph.Refreshable;
import org.rhq.coregui.client.inventory.common.graph.graphtype.StackedBarMetricGraphImpl;
import org.rhq.coregui.client.inventory.resource.detail.monitoring.MetricD3Graph;
import org.rhq.coregui.client.util.BrowserUtility;
import org.rhq.coregui.client.util.Log;
import org.rhq.coregui.client.util.message.Message;
import org.rhq.core.domain.measurement.MeasurementUnits;

/**
 * Views a resource's metrics in a tabular view with sparkline graph and optional detailed d3 graph.
 *
 * @author John Mazzitelli
 * @author Mike Thompson
 */
public class MetricsTableView extends Table<MetricsViewDataSource> implements Refreshable {

    private final Resource resource;
    private final AbstractD3GraphListView abstractD3GraphListView;
    private ToolStrip toolStrip;
    private SelectItem dashboardSelectItem;
    private Dashboard selectedDashboard;
    private IButton addToDashboardButton;
    private LinkedHashMap<String, String> dashboardMenuMap;
    private LinkedHashMap<Integer, Dashboard> dashboardMap;
    private Set<Integer> expandedRows;
    private MetricsTableListGrid metricsTableListGrid;
    private int selectedMetricDefinitionId;

    public MetricsTableView(Resource resource, AbstractD3GraphListView abstractD3GraphListView,
        Set<Integer> expandedRows) {
        super();
        setStyleName("metricsTableView");
        this.resource = resource;
        this.abstractD3GraphListView = abstractD3GraphListView;
        dashboardMenuMap = new LinkedHashMap<String, String>();
        dashboardMap = new LinkedHashMap<Integer, Dashboard>();
        setDataSource(new MetricsViewDataSource(resource));
        this.expandedRows = expandedRows;
    }

    @Override
    protected void onInit() {
        super.onInit();
    }

    /**
     * Creates this Table's list grid (called by onInit()). Subclasses can override this if they require a custom
     * subclass of ListGrid.
     *
     * @return this Table's list grid (must be an instance of ListGrid)
     */
    @Override
    protected ListGrid createListGrid() {
        metricsTableListGrid = new MetricsTableListGrid(this, resource);
        metricsTableListGrid.addSelectionChangedHandler(new SelectionChangedHandler() {

            @Override
            public void onSelectionChanged(SelectionEvent selectionEvent) {
                ListGridRecord selectedRecord = null;
                addToDashboardButton.enable();
                // Get the last record selected, by default it returns the first
                // this is to get around the bug that SINGLE selection policy is not working only MULTIPLE always.
                if(selectionEvent.getSelection().length == 0){
                    selectedRecord = selectionEvent.getSelectedRecord();
                }else {
                    // always just use the last selection
                    selectedRecord = selectionEvent.getSelection()[selectionEvent.getSelection().length-1];
                }

                if (null != selectedRecord) {
                    selectedMetricDefinitionId = selectedRecord.getAttributeAsInt(METRIC_DEF_ID
                            .getValue());
                }
            }
        });

        if (null == toolStrip) {
            toolStrip = createToolstrip();
        }
        addExtraWidget(toolStrip, false);
        addToDashboardButton.disable();
        return metricsTableListGrid;
    }

    protected void configureTable() {
        ArrayList<ListGridField> fields = getDataSource().getListGridFields();
        setListGridFields(fields.toArray(new ListGridField[0]));
    }

    private ToolStrip createToolstrip() {
        toolStrip = new ToolStrip();
        toolStrip.setWidth(300);
        toolStrip.setMembersMargin(15);
        toolStrip.setStyleName("footer");
        //toolStrip.setPadding(5);
        toolStrip.addSpacer(10);
        addToDashboardButton = new IButton(MSG.chart_metrics_add_to_dashboard_button());
        addToDashboardButton.setWidth(80);
        dashboardSelectItem = new SelectItem();
        dashboardSelectItem.setTitle(MSG.chart_metrics_add_to_dashboard_label());
        dashboardSelectItem.setWidth(240);
        dashboardSelectItem.setWrapTitle(false);
        populateDashboardMenu();
        toolStrip.addFormItem(dashboardSelectItem);
        toolStrip.addMember(addToDashboardButton);

        dashboardSelectItem.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent changeEvent) {
                Integer selectedDashboardId = Integer.valueOf((String) changeEvent.getValue());
                selectedDashboard = dashboardMap.get(selectedDashboardId);
            }
        });
        addToDashboardButton.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                for (MeasurementDefinition measurementDefinition : resource.getResourceType().getMetricDefinitions()) {
                    if (measurementDefinition.getId() == selectedMetricDefinitionId) {
                        Log.debug("Add to Dashboard -- Storing: " + measurementDefinition.getDisplayName() + " in "
                            + selectedDashboard.getName());
                        storeDashboardMetric(selectedDashboard, resource.getId(), measurementDefinition);
                        addToDashboardButton.disable();
                        break;
                    }
                }
            }
        });
        return toolStrip;
    }

    @Override
    /**
     * Redraw Graphs in this context means to refresh the table and redraw open graphs.
     */
    public void refreshData() {
        new Timer() {
            @Override
            public void run() {
                metricsTableListGrid.expandOpenedRows();
                addToDashboardButton.disable();
                new Timer() {
                    @Override
                    public void run() {
                        BrowserUtility.graphSparkLines();
                    }
                    // we need to add a little extra time for these to get built
                }.schedule(350);
            }
        }.schedule(150);

    }

    @Override
    public void refresh() {
        super.refresh(false);
        metricsTableListGrid.expandOpenedRows();
        addToDashboardButton.disable();
    }

    private void populateDashboardMenu() {
        dashboardMenuMap.clear();
        dashboardMap.clear();

        DashboardCriteria criteria = new DashboardCriteria();
        GWTServiceLookup.getDashboardService().findDashboardsByCriteria(criteria,
            new AsyncCallback<PageList<Dashboard>>() {

                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_tree_common_contextMenu_loadFailed_dashboard(),
                        caught);
                }

                public void onSuccess(PageList<Dashboard> dashboards) {
                    if (dashboards.size() > 0) {
                        for (final Dashboard dashboard : dashboards) {
                            dashboardMenuMap.put(String.valueOf(dashboard.getId()), dashboard.getName());
                            dashboardMap.put(dashboard.getId(), dashboard);
                        }
                        selectedDashboard = dashboards.get(0);
                        dashboardSelectItem.setValueMap(dashboardMenuMap);
                        dashboardSelectItem.setValue(selectedDashboard.getId());
                    }
                }
            });
    }

    private void storeDashboardMetric(Dashboard dashboard, int resourceId, MeasurementDefinition definition) {
        DashboardPortlet dashboardPortlet = new DashboardPortlet(MSG.view_tree_common_contextMenu_resourceGraph(),
            ResourceD3GraphPortlet.KEY, 260);
        dashboardPortlet.getConfiguration().put(
            new PropertySimple(ResourceD3GraphPortlet.CFG_RESOURCE_ID, resourceId));
        dashboardPortlet.getConfiguration().put(
            new PropertySimple(ResourceD3GraphPortlet.CFG_DEFINITION_ID, definition.getId()));

        dashboard.addPortlet(dashboardPortlet);

        GWTServiceLookup.getDashboardService().storeDashboard(dashboard, new AsyncCallback<Dashboard>() {

            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_tree_common_contextMenu_saveChartToDashboardFailure(),
                    caught);
            }

            public void onSuccess(Dashboard result) {
                CoreGUI.getMessageCenter().notify(
                    new Message(MSG.view_tree_common_contextMenu_saveChartToDashboardSuccessful(result.getName()),
                        Message.Severity.Info));
            }
        });
    }

    public class MetricsTableListGrid extends ListGrid {

        private static final int TREEVIEW_DETAIL_CHART_HEIGHT = 205;
        private static final int NUM_METRIC_POINTS = 60;
        final MetricsTableView metricsTableView;
        private Resource resource;

        public MetricsTableListGrid(final MetricsTableView metricsTableView, final Resource resource) {
            super();
            this.resource = resource;
            this.metricsTableView = metricsTableView;
            setCanExpandRecords(true);
            setSelectionType(SelectionStyle.SINGLE);
            setCanExpandMultipleRecords(true);
            setExpansionMode(ExpansionMode.DETAIL_FIELD);

            addRecordExpandHandler(new RecordExpandHandler() {
                @Override
                public void onRecordExpand(RecordExpandEvent recordExpandEvent) {
                    metricsTableView.expandedRows.add(recordExpandEvent.getRecord().getAttributeAsInt(
                        METRIC_DEF_ID.getValue()));
                    refreshData();
                }

            });
            addRecordCollapseHandler(new RecordCollapseHandler() {
                @Override
                public void onRecordCollapse(RecordCollapseEvent recordCollapseEvent) {
                    metricsTableView.expandedRows.remove(recordCollapseEvent.getRecord().getAttributeAsInt(
                        METRIC_DEF_ID.getValue()));
                    refresh();
                    new Timer() {
                        @Override
                        public void run() {
                            BrowserUtility.graphSparkLines();
                        }
                    }.schedule(150);
                }
            });
            addSortChangedHandler(new SortChangedHandler() {
                @Override
                public void onSortChanged(SortEvent sortEvent) {
                    refreshData();
                }
            });

            addDataArrivedHandler(new DataArrivedHandler() {
                @Override
                public void onDataArrived(DataArrivedEvent dataArrivedEvent) {
                    expandOpenedRows();
                }
            });

        }

        public void expandOpenedRows() {

            int startRow = 0;
            int endRow = this.getRecords().length;
            for (int i = startRow; i < endRow; i++) {
                ListGridRecord listGridRecord = getRecord(i);
                if (null != listGridRecord) {
                    int metricDefinitionId = listGridRecord
                        .getAttributeAsInt(METRIC_DEF_ID.getValue());
                    if (null != metricsTableView && null != expandedRows
                        && metricsTableView.expandedRows.contains(metricDefinitionId)) {
                        expandRecord(listGridRecord);
                        BrowserUtility.graphSparkLines();
                    }
                }
            }
        }


        @Override
        /**
         * If you expand a grid row then create a graph.
         */
        protected Canvas getExpansionComponent(final ListGridRecord record) {
            final Integer definitionId = record.getAttributeAsInt(METRIC_DEF_ID.getValue());
            final Integer resourceId = record.getAttributeAsInt(RESOURCE_ID.getValue());

            final MeasurementUnits recordUnits;
            if (record.getAttributeAsString(DISPLAY_UNITS_NAME.getValue())!=null && record.getAttributeAsString
                    (DISPLAY_UNITS_FAMILY.getValue())!=null) {

                recordUnits = MeasurementUnits.getUsingDisplayUnits(
                        record.getAttributeAsString(DISPLAY_UNITS_NAME.getValue()),
                        MeasurementUnits.Family.valueOf(record.getAttributeAsString(DISPLAY_UNITS_FAMILY.getValue())));
            } else {
                recordUnits = null;
            }


            VLayout vLayout = new VLayout();
            vLayout.setPadding(5);

            final String chartId = "rChart-" + resourceId + "-" + definitionId;
            HTMLFlow htmlFlow = new HTMLFlow(MetricD3Graph.createGraphMarkerTemplate(chartId,
                TREEVIEW_DETAIL_CHART_HEIGHT));
            vLayout.addMember(htmlFlow);

            int[] definitionArrayIds = new int[1];
            definitionArrayIds[0] = definitionId;
            GWTServiceLookup.getMeasurementDataService().findDataForResource(resourceId, definitionArrayIds,
                CustomDateRangeState.getInstance().getStartTime(), CustomDateRangeState.getInstance().getEndTime(),
                NUM_METRIC_POINTS, new AsyncCallback<List<List<MeasurementDataNumericHighLowComposite>>>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        Log.warn("Error retrieving recent metrics charting data for resource [" + resourceId + "]:"
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
                            metricGraphData.setAdjustedMeasurementUnits(recordUnits);
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
