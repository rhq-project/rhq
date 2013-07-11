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
import java.util.EnumSet;
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
import com.smartgwt.client.widgets.grid.events.RecordExpandEvent;
import com.smartgwt.client.widgets.grid.events.RecordExpandHandler;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.UserSessionManager;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.common.graph.ButtonBarDateTimeRangeEditor;
import org.rhq.enterprise.gui.coregui.client.inventory.common.graph.MetricGraphData;
import org.rhq.enterprise.gui.coregui.client.inventory.common.graph.RedrawGraphs;
import org.rhq.enterprise.gui.coregui.client.inventory.common.graph.graphtype.StackedBarMetricGraphImpl;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.MetricD3Graph;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
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
public class MetricsTableView extends Table<MetricsTableDataSource> implements RedrawGraphs {

    private final int resourceId;

    private MeasurementUserPreferences measurementUserPrefs;
    private ButtonBarDateTimeRangeEditor buttonBarDateTimeRangeEditor;

    public MetricsTableView(int resourceId) {
        super();
        this.resourceId = resourceId;
        setDataSource(new MetricsTableDataSource(resourceId));
        measurementUserPrefs = new MeasurementUserPreferences(UserSessionManager.getUserPreferences());
        buttonBarDateTimeRangeEditor = new ButtonBarDateTimeRangeEditor(measurementUserPrefs, this);
    }

    /**
     * Creates this Table's list grid (called by onInit()). Subclasses can override this if they require a custom
     * subclass of ListGrid.
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
                Log.debug("Record Expanded: "
                    + recordExpandEvent.getRecord().getAttribute(MetricsTableDataSource.FIELD_METRIC_LABEL));
                new Timer() {

                    @Override
                    public void run() {
                        BrowserUtility.graphSparkLines();
                    }
                }.schedule(150);

            }

        });

        addExtraWidget(buttonBarDateTimeRangeEditor, true);
        addTableAction(MSG.view_measureTable_getLive(), new ShowLiveDataTableAction(this));
        addTableAction(MSG.view_measureTable_addToDashboard(), new AddToDashboardTableAction(this));

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
                Integer defId = record.getAttributeAsInt(MetricsTableDataSource.FIELD_METRIC_DEF_ID);
                definitionIds[i++] = defId;

                String name = record.getAttribute(MetricsTableDataSource.FIELD_METRIC_NAME);
                String label = record.getAttribute(MetricsTableDataSource.FIELD_METRIC_LABEL);
                String units = record.getAttribute(MetricsTableDataSource.FIELD_METRIC_UNITS);
                if (units == null || units.length() < 1) {
                    units = MeasurementUnits.NONE.name();
                }

                scheduleNamesAndUnits.put(name, new String[] { label, units });
            }

            // actually go out and ask the agents for the data
            GWTServiceLookup.getMeasurementDataService(60000).findLiveData(metricsTableView.resourceId, definitionIds,
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

    private static class AddToDashboardTableAction implements TableAction {
        private MetricsTableView metricsTableView;

        public AddToDashboardTableAction(MetricsTableView metricsTableView) {
            this.metricsTableView = metricsTableView;
        }

        @Override
        public boolean isEnabled(ListGridRecord[] selection) {
            return selection != null && selection.length > 0;
        }

        @Override
        public void executeAction(ListGridRecord[] selection, Object actionValue) {
            //@todo: Add to Dashboard
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
        public MetricsTableListGrid() {
            super();

            setCanExpandRecords(true);
            setCanExpandMultipleRecords(true);
            setExpansionMode(ExpansionMode.RELATED);
        }

        @Override
        protected Canvas getExpansionComponent(final ListGridRecord record) {
            final Integer definitionId = record.getAttributeAsInt(MetricsTableDataSource.FIELD_METRIC_DEF_ID);
            final Integer resourceId = record.getAttributeAsInt(MetricsTableDataSource.FIELD_RESOURCE_ID);
            VLayout vLayout = new VLayout();
            vLayout.setPadding(5);

            final String chartId = "rChart" + resourceId + "-" + definitionId;
            HTMLFlow htmlFlow = new HTMLFlow(MetricD3Graph.createGraphMarkerTemplate(chartId, 200));
            vLayout.addMember(htmlFlow);

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

                            new Timer() {

                                @Override
                                public void run() {
                                    BrowserUtility.graphSparkLines();
                                }
                            }.schedule(150);

                            final Resource resource = results.get(0).getResource();
                            //D3GraphListView graphListView = D3GraphListView.createSingleGraph(resource, definitionId, false);

                            // Load the fully fetched ResourceType.
                            ResourceType resourceType = resource.getResourceType();
                            ResourceTypeRepository.Cache.getInstance().getResourceTypes(
                                resourceType.getId(),
                                EnumSet.of(ResourceTypeRepository.MetadataType.content,
                                    ResourceTypeRepository.MetadataType.operations,
                                    ResourceTypeRepository.MetadataType.measurements,
                                    ResourceTypeRepository.MetadataType.events,
                                    ResourceTypeRepository.MetadataType.resourceConfigurationDefinition),
                                new ResourceTypeRepository.TypeLoadedCallback() {
                                    public void onTypesLoaded(ResourceType type) {
                                        resource.setResourceType(type);
                                        //metric definitions
                                        Set<MeasurementDefinition> definitions = type.getMetricDefinitions();
                                        final MeasurementDefinition measurementDefinition;

                                        //build id mapping for measurementDefinition instances Ex. Free Memory -> MeasurementDefinition[100071]
                                        final HashMap<String, MeasurementDefinition> measurementDefMap = new HashMap<String, MeasurementDefinition>();
                                        for (MeasurementDefinition definition : definitions) {
                                            measurementDefMap.put(definition.getDisplayName(), definition);
                                            if (definition.getId() == definitionId) {
                                                measurementDefinition = definition;
                                                break;
                                            }
                                        }

                                        int[] definitionArrayIds = new int[1];
                                        definitionArrayIds[0] = definitionId;
                                        GWTServiceLookup.getMeasurementDataService().findDataForResource(resourceId,
                                            definitionArrayIds, measurementUserPrefs.getMetricRangePreferences().begin,
                                            measurementUserPrefs.getMetricRangePreferences().end, 60,
                                            new AsyncCallback<List<List<MeasurementDataNumericHighLowComposite>>>() {
                                                @Override
                                                public void onFailure(Throwable caught) {
                                                    Log.debug("Error retrieving recent metrics charting data for resource ["
                                                        + resourceId + "]:" + caught.getMessage());
                                                }

                                                @Override
                                                public void onSuccess(
                                                    List<List<MeasurementDataNumericHighLowComposite>> results) {
                                                    if (!results.isEmpty()) {

                                                        //load the data results for the given metric definition
                                                        List<MeasurementDataNumericHighLowComposite> data = results
                                                            .get(0);

//                                                        MetricGraphData metricGraphData = MetricGraphData
//                                                            .createForResource(resourceId, resource.getName(),
//                                                                measurementDefinition, data, null);
//
//                                                        StackedBarMetricGraphImpl graph = GWT
//                                                            .create(StackedBarMetricGraphImpl.class);
//                                                        graph.setMetricGraphData(metricGraphData);
//                                                        final MetricD3Graph graphView = new MetricD3Graph(graph, this);
//                                                        new Timer() {
//                                                            @Override
//                                                            public void run() {
//                                                                graphView.drawJsniChart();
//
//                                                            }
//                                                        }.schedule(150);

                                                    } else {
                                                        Log.warn("No chart data retrieving for resource [" + resourceId
                                                            + "]");

                                                    }
                                                }
                                            });

                                    }
                                });

                        }
                    }
                });

            return vLayout;
        }
    }

}
