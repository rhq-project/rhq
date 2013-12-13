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

package org.rhq.coregui.client.inventory.groups.detail.monitoring.metric;

import static org.rhq.coregui.client.inventory.resource.detail.monitoring.table.MetricsGridFieldName.METRIC_DEF_ID;
import static org.rhq.coregui.client.inventory.resource.detail.monitoring.table.MetricsGridFieldName.RESOURCE_GROUP_ID;

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
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.components.table.Table;
import org.rhq.coregui.client.dashboard.portlets.inventory.groups.graph.ResourceGroupD3GraphPortlet;
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

/**
 * Views a resource's metrics in a tabular view with sparkline graph and optional detailed d3 graph.
 *
 * @author John Mazzitelli
 * @author Mike Thompson
 */
public class MetricsGroupTableView extends Table<MetricsGroupViewDataSource> implements Refreshable {

    private final ResourceGroup resourceGroup;
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

    public MetricsGroupTableView(ResourceGroup resourceGroup, AbstractD3GraphListView abstractD3GraphListView,
        Set<Integer> expandedRows) {
        super();
        this.resourceGroup = resourceGroup;
        this.abstractD3GraphListView = abstractD3GraphListView;
        dashboardMenuMap = new LinkedHashMap<String, String>();
        dashboardMap = new LinkedHashMap<Integer, Dashboard>();
        setDataSource(new MetricsGroupViewDataSource(resourceGroup));
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
        metricsTableListGrid = new MetricsTableListGrid(this, resourceGroup);
        metricsTableListGrid.addSelectionChangedHandler(new SelectionChangedHandler() {
            @Override
            public void onSelectionChanged(SelectionEvent selectionEvent) {
                if (resourceGroup.getGroupCategory() == GroupCategory.COMPATIBLE) {
                    addToDashboardButton.enable();
                    ListGridRecord selectedRecord = selectionEvent.getSelectedRecord();
                    if (null != selectedRecord) {
                        selectedMetricDefinitionId = selectedRecord.getAttributeAsInt(METRIC_DEF_ID.getValue());
                    }
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
        toolStrip.setPadding(5);
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
                for (MeasurementDefinition measurementDefinition : resourceGroup.getResourceType()
                    .getMetricDefinitions()) {
                    if (measurementDefinition.getId() == selectedMetricDefinitionId) {
                        Log.debug("Add to Dashboard -- Storing: " + measurementDefinition.getDisplayName() + " in "
                            + selectedDashboard.getName());

                        storeDashboardMetric(selectedDashboard, resourceGroup.getId(), measurementDefinition);
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
                BrowserUtility.graphSparkLines();
            }
        }.schedule(150);

    }

    @Override
    public void refresh() {
        super.refresh(false);
        metricsTableListGrid.expandOpenedRows();
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

    private void storeDashboardMetric(Dashboard dashboard, int resourceGroupId, MeasurementDefinition definition) {

        DashboardPortlet dashboardPortlet = new DashboardPortlet(MSG.view_tree_common_contextMenu_groupGraph(),
            ResourceGroupD3GraphPortlet.KEY, 260);
        dashboardPortlet.getConfiguration().put(
            new PropertySimple(ResourceGroupD3GraphPortlet.CFG_RESOURCE_GROUP_ID, resourceGroupId));
        dashboardPortlet.getConfiguration().put(
            new PropertySimple(ResourceGroupD3GraphPortlet.CFG_DEFINITION_ID, definition.getId()));

        dashboard.addPortlet(dashboardPortlet);

        GWTServiceLookup.getDashboardService().storeDashboard(dashboard, new AsyncCallback<Dashboard>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_tree_common_contextMenu_saveChartToDashboardFailure(),
                    caught);
            }

            public void onSuccess(Dashboard result) {
                String msg = MSG.view_tree_common_contextMenu_saveChartToDashboardSuccessful(result.getName());
                CoreGUI.getMessageCenter().notify(new Message(msg, Message.Severity.Info));
            }
        });
    }

    public class MetricsTableListGrid extends ListGrid {

        private static final int TREEVIEW_DETAIL_CHART_HEIGHT = 205;
        private static final int NUM_METRIC_POINTS = 60;
        final MetricsGroupTableView metricsTableView;
        private ResourceGroup group;

        public MetricsTableListGrid(final MetricsGroupTableView metricsTableView, final ResourceGroup group) {
            super();
            this.group = group;
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
                    int metricDefinitionId = listGridRecord.getAttributeAsInt(METRIC_DEF_ID.getValue());
                    if (null != metricsTableView && null != expandedRows
                        && metricsTableView.expandedRows.contains(metricDefinitionId)) {
                        expandRecord(listGridRecord);
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
            final Integer resourceGroupId = record.getAttributeAsInt(RESOURCE_GROUP_ID.getValue());
            VLayout vLayout = new VLayout();
            vLayout.setPadding(5);

            final String chartId = "rChart-" + resourceGroupId + "-" + definitionId;
            HTMLFlow htmlFlow = new HTMLFlow(MetricD3Graph.createGraphMarkerTemplate(chartId,
                TREEVIEW_DETAIL_CHART_HEIGHT));
            vLayout.addMember(htmlFlow);

            int[] definitionArrayIds = new int[1];
            definitionArrayIds[0] = definitionId;
            GWTServiceLookup.getMeasurementDataService().findDataForCompatibleGroup(resourceGroupId,
                definitionArrayIds, CustomDateRangeState.getInstance().getStartTime(),
                CustomDateRangeState.getInstance().getEndTime(), NUM_METRIC_POINTS,
                new AsyncCallback<List<List<MeasurementDataNumericHighLowComposite>>>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        Log.warn("Error retrieving recent metrics charting data for resource group [" + resourceGroupId
                            + "]:" + caught.getMessage());
                    }

                    @Override
                    public void onSuccess(List<List<MeasurementDataNumericHighLowComposite>> results) {
                        if (!results.isEmpty()) {

                            //load the data results for the given metric definition
                            List<MeasurementDataNumericHighLowComposite> measurementList = results.get(0);

                            MeasurementDefinition measurementDefinition = null;
                            for (MeasurementDefinition definition : group.getResourceType().getMetricDefinitions()) {
                                if (definition.getId() == definitionId) {
                                    measurementDefinition = definition;
                                    break;
                                }
                            }

                            MetricGraphData metricGraphData = MetricGraphData.createForResourceGroup(group.getId(),
                                group.getName(), measurementDefinition, measurementList);
                            metricGraphData.setHideLegend(true);

                            StackedBarMetricGraphImpl graph = GWT.create(StackedBarMetricGraphImpl.class);
                            graph.setMetricGraphData(metricGraphData);
                            final MetricD3Graph graphView = new MetricD3Graph(graph, abstractD3GraphListView);
                            new Timer() {
                                @Override
                                public void run() {
                                    graphView.drawJsniChart();
                                    new Timer() {
                                        @Override
                                        public void run() {
                                            BrowserUtility.graphSparkLines();
                                        }
                                    }.schedule(150);
                                }
                            }.schedule(150);

                        } else {
                            Log.warn("No chart data retrieving for resource group [" + resourceGroupId + "-"
                                + definitionId + "]");
                        }
                    }
                });

            return vLayout;
        }
    }

}
