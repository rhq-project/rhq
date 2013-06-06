/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.measurement.composite.MeasurementOOBComposite;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.common.graph.ButtonBarDateTimeRangeEditor;
import org.rhq.enterprise.gui.coregui.client.inventory.common.graph.MetricGraphData;
import org.rhq.enterprise.gui.coregui.client.inventory.common.graph.graphtype.AvailabilityOverUnderGraphType;
import org.rhq.enterprise.gui.coregui.client.inventory.common.graph.graphtype.StackedBarMetricGraphImpl;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.avail.AvailabilityD3GraphView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.table.MetricsTableView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.async.Command;
import org.rhq.enterprise.gui.coregui.client.util.async.CountDownLatch;

/**
 * Build the View that shows the individual graph views for multi-graph
 * views if just a resource is provided and single graph view if resource
 * and  measurement definitionId are provided.
 *
 * @author Mike Thompson
 */
public class D3ConsolidatedGraphListView extends D3GraphListView {

    private static int NUM_ASYNC_CALLS = 2; // wait for X async calls in Latch

    private Resource resource;
    private Set<Integer> definitionIds = null;
    private boolean useSummaryData = false;
    private PageList<MeasurementOOBComposite> measurementOOBCompositeList;
    private List<List<MeasurementDataNumericHighLowComposite>> metricsDataList;
    private VLayout vLayout;
    private MetricsTableView metricsTableView;

//    public static D3ConsolidatedGraphListView createMultipleGraphs(Resource resource, Set<Integer> definitionIds,
//        boolean showAvailabilityGraph) {
//
//        return new D3ConsolidatedGraphListView(resource, definitionIds, showAvailabilityGraph);
//    }
//
//    public static D3ConsolidatedGraphListView createSummaryMultipleGraphs(Resource resource, boolean monitorDetailView) {
//        return new D3ConsolidatedGraphListView(resource, monitorDetailView);
//    }
//
    public static D3ConsolidatedGraphListView createSingleGraph(Resource resource, Integer measurementId,
        boolean showAvailabilityGraph) {
        TreeSet<Integer> definitionIds = new TreeSet<Integer>();
        definitionIds.add(measurementId);
        return new D3ConsolidatedGraphListView(resource, definitionIds, showAvailabilityGraph);

    }
//
//    public static D3ConsolidatedGraphListView createSingleGraphNoAvail(Resource resource, Integer measurementId) {
//        return D3ConsolidatedGraphListView.createSingleGraph(resource, measurementId, false);
//    }

    public static D3ConsolidatedGraphListView createSparklineGraphs(Resource resource, Integer measurementId) {
        return D3ConsolidatedGraphListView.createSingleGraph(resource, measurementId, false);
    }

    private D3ConsolidatedGraphListView(Resource resource, Set<Integer> definitionIds, boolean showAvailabilityGraph) {
        super(resource, definitionIds,showAvailabilityGraph);
        this.resource = resource;
        //commonConstructorSettings();
        this.definitionIds = definitionIds;
        this.showAvailabilityGraph = showAvailabilityGraph;
    }

    private D3ConsolidatedGraphListView(Resource resource, boolean showAvailabilityGraph) {
        super(resource, showAvailabilityGraph);
        this.resource = resource;
        this.showAvailabilityGraph = showAvailabilityGraph;
        //commonConstructorSettings();
        useSummaryData = true;
    }

//    private void commonConstructorSettings() {
//        buttonBarDateTimeRangeEditor = new ButtonBarDateTimeRangeEditor(measurementUserPrefs,this);
//        setOverflow(Overflow.HIDDEN);
//    }

    @Deprecated
    public void addSetButtonClickHandler(ClickHandler clickHandler) {
        Log.debug("measurementRangeEditor " + buttonBarDateTimeRangeEditor);
      //  graphDateTimeRangeEditor.getSetButton().addClickHandler(clickHandler);
    }

    @Override
    protected void onDraw() {
        super.onDraw();
        Log.debug("D3ConsolidatedGraphListView.onDraw() for: " + resource.getName());
        destroyMembers();

        addMember(buttonBarDateTimeRangeEditor);
        buttonBarDateTimeRangeEditor.createDateSliderMarker();

            availabilityGraph = new AvailabilityD3GraphView<AvailabilityOverUnderGraphType>(new AvailabilityOverUnderGraphType(resource.getId()));
            // first step in 2 step to create d3 chart
            // create a placeholder for avail graph
            availabilityGraph.createGraphMarker();
            addMember(availabilityGraph);


        vLayout = new VLayout();
        vLayout.setOverflow(Overflow.AUTO);
        vLayout.setWidth100();
        vLayout.setHeight100();

        if (resource != null) {
            queryAndBuildGraphs();
            metricsTableView = new MetricsTableView(resource.getId());
            vLayout.addMember(metricsTableView);
        }
        addMember(vLayout);
    }

    public void redrawGraphs() {
        this.onDraw();
            availabilityGraph.drawJsniChart();
    }

    @Override
    protected void queryAvailability(final EntityContext context, Long startTime, Long endTime,
        final CountDownLatch countDownLatch) {

        final long timerStart = System.currentTimeMillis();

        // now return the availability
        GWTServiceLookup.getAvailabilityService().getAvailabilitiesForResource(context.getResourceId(), startTime,
            endTime, new AsyncCallback<List<Availability>>() {
                @Override
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_resource_monitor_availability_loadFailed(), caught);
                    if (countDownLatch != null) {
                        countDownLatch.countDown();
                    }
                }

                @Override
                public void onSuccess(List<Availability> availList) {
                    Log.debug("\nSuccessfully queried availability in: "
                            + (System.currentTimeMillis() - timerStart) + " ms.");
                    availabilityList = availList;
                    if (countDownLatch != null) {
                        countDownLatch.countDown();
                    }
                }
            });
    }

    /**
     * Build whatever graph (summary or not) by grabbing the MeasurementDefinitions
     * that are defined for the resource and then querying the metric and availability data.
     */
    private void queryAndBuildGraphs() {
        final long startTimer = System.currentTimeMillis();

        if(null != availabilityGraph){
            queryAvailability(EntityContext.forResource(resource.getId()), buttonBarDateTimeRangeEditor.getStartTime(),
                buttonBarDateTimeRangeEditor.getEndTime(), null);
        }

        ResourceTypeRepository.Cache.getInstance().getResourceTypes(resource.getResourceType().getId(),
            EnumSet.of(ResourceTypeRepository.MetadataType.measurements),
            new ResourceTypeRepository.TypeLoadedCallback() {
                public void onTypesLoaded(final ResourceType type) {

                    final ArrayList<MeasurementDefinition> measurementDefinitions = new ArrayList<MeasurementDefinition>();
                    final ArrayList<MeasurementDefinition> summaryMeasurementDefinitions = new ArrayList<MeasurementDefinition>();

                    for (MeasurementDefinition def : type.getMetricDefinitions()) {
                        if (def.getDataType() == DataType.MEASUREMENT && def.getDisplayType() == DisplayType.SUMMARY) {
                            summaryMeasurementDefinitions.add(def);
                        }
                        measurementDefinitions.add(def);
                    }

                    Collections.sort(measurementDefinitions, new Comparator<MeasurementDefinition>() {
                        @Override
                        public int compare(MeasurementDefinition o1, MeasurementDefinition o2) {
                            return new Integer(o1.getDisplayOrder()).compareTo(o2.getDisplayOrder());
                        }
                    });
                    Collections.sort(summaryMeasurementDefinitions, new Comparator<MeasurementDefinition>() {
                        @Override
                        public int compare(MeasurementDefinition o1, MeasurementDefinition o2) {
                            return new Integer(o1.getDisplayOrder()).compareTo(o2.getDisplayOrder());
                        }
                    });

                    int[] measDefIdArray = new int[measurementDefinitions.size()];
                    for (int i = 0; i < measDefIdArray.length; i++) {
                        measDefIdArray[i] = measurementDefinitions.get(i).getId();
                    }

                    // setting up a deferred Command to execute after all resource queries have completed (successfully or unsuccessfully)
                    // we know there are exactly 2 resources
                    final CountDownLatch countDownLatch = CountDownLatch.create(NUM_ASYNC_CALLS, new Command() {
                        @Override
                        /**
                         * Satisfied only after ALL of the metric queries AND availability have completed
                         */
                        public void execute() {
                            Log.debug("Total Time for async metrics/avail query: "
                                + (System.currentTimeMillis() - startTimer));
                            if (metricsDataList.isEmpty()) {
                                loadingLabel.setContents(MSG.view_resource_monitor_graphs_noneAvailable());
                            } else {
                                loadingLabel.hide();
                                if (useSummaryData) {
                                    buildSummaryGraphs(metricsDataList, summaryMeasurementDefinitions,
                                        measurementDefinitions);
                                } else {
                                    determineGraphsToBuild(metricsDataList, measurementDefinitions, definitionIds);
                                }
                                // There is a weird timing case when availabilityGraph can be null
                                if (null != availabilityGraph) {
                                    // we only need the first metricData since we are only taking the
                                    // availability data set in there for the dropdowns already
                                    availabilityGraph.setAvailabilityList(availabilityList);
                                    new Timer(){
                                        /**
                                         * This method will be called when a timer fires. Override it to implement the timer's logic.
                                         */
                                        @Override
                                        public void run() {
                                            availabilityGraph.drawJsniChart();
                                        }
                                    }.schedule(200);
                                }
                            }

                        }
                    });

                    queryMetricData(measDefIdArray, countDownLatch);
                    queryOOBMetrics(resource, countDownLatch);
                    // now the countDown latch will run sometime asynchronously

                }

                private void queryMetricData(final int[] measDefIdArray, final CountDownLatch countDownLatch) {
                    GWTServiceLookup.getMeasurementDataService().findDataForResource(resource.getId(), measDefIdArray,
                            buttonBarDateTimeRangeEditor.getStartTime(), buttonBarDateTimeRangeEditor.getEndTime(), 60,
                        new AsyncCallback<List<List<MeasurementDataNumericHighLowComposite>>>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                CoreGUI.getErrorHandler().handleError(MSG.view_resource_monitor_graphs_loadFailed(),
                                    caught);
                                loadingLabel.setContents(MSG.view_resource_monitor_graphs_loadFailed());
                                countDownLatch.countDown();
                            }

                            @Override
                            public void onSuccess(List<List<MeasurementDataNumericHighLowComposite>> metrics) {
                                metricsDataList = metrics;
                                Log.debug("Regular Metric graph data queried in: "
                                    + (System.currentTimeMillis() - startTimer + " ms."));
                                countDownLatch.countDown();

                            }
                        });
                }

                private void queryOOBMetrics(final Resource resource, final CountDownLatch countDownLatch) {

                    final long startTime = System.currentTimeMillis();

                    GWTServiceLookup.getMeasurementDataService().getHighestNOOBsForResource(resource.getId(), 60,

                    new AsyncCallback<PageList<MeasurementOOBComposite>>() {
                        @Override
                        public void onSuccess(PageList<MeasurementOOBComposite> measurementOOBComposites) {

                            measurementOOBCompositeList = measurementOOBComposites;
                            Log.debug("\nSuccessfully queried "+measurementOOBCompositeList.size() +" OOB records in: " + (System.currentTimeMillis() - startTime)
                                + " ms.");
                            countDownLatch.countDown();
                        }

                        @Override
                        public void onFailure(Throwable caught) {
                            Log.debug("Error retrieving out of bound metrics for resource [" + resource.getId() + "]:"
                                + caught.getMessage());
                            countDownLatch.countDown();
                        }
                    });

                }

                /**
                 * Spin through the measurement definitions (in order) checking to see if they are in the
                 * summary measurement definition set and if so build a graph.
                 * @param measurementData
                 * @param summaryMeasurementDefinitions
                 * @param measurementDefinitions
                 */
                private void buildSummaryGraphs(List<List<MeasurementDataNumericHighLowComposite>> measurementData,
                    List<MeasurementDefinition> summaryMeasurementDefinitions,
                    List<MeasurementDefinition> measurementDefinitions) {
                    Set<Integer> summaryIds = new TreeSet<Integer>();
                    for (MeasurementDefinition summaryMeasurementDefinition : summaryMeasurementDefinitions) {
                        summaryIds.add(summaryMeasurementDefinition.getId());
                    }

                    int i = 0;
                    for (MeasurementDefinition measurementDefinition : measurementDefinitions) {
                        if (summaryIds.contains(measurementDefinition.getId())) {
                            buildSingleGraph(measurementOOBCompositeList, measurementDefinition,
                                measurementData.get(i), MULTI_CHART_HEIGHT);
                        }
                        i++;
                    }

                }

                private void determineGraphsToBuild(List<List<MeasurementDataNumericHighLowComposite>> measurementData,
                    List<MeasurementDefinition> measurementDefinitions, Set<Integer> definitionIds) {
                    int i = 0;
                    for (List<MeasurementDataNumericHighLowComposite> metric : measurementData) {

                        for (Integer selectedDefinitionId : definitionIds) {
                            final MeasurementDefinition measurementDefinition = measurementDefinitions.get(i);
                            final int measurementId = measurementDefinition.getId();

                            if (null != selectedDefinitionId) {
                                // single graph case
                                if (measurementId == selectedDefinitionId) {
                                    buildSingleGraph(measurementOOBCompositeList, measurementDefinition, metric, SINGLE_CHART_HEIGHT);
                                }
                            } else {
                                // multiple graph case
                                buildSingleGraph(measurementOOBCompositeList, measurementDefinition, metric, MULTI_CHART_HEIGHT);
                            }
                        }
                        i++;
                    }
                }
            });

    }

    private void buildSingleGraph(PageList<MeasurementOOBComposite> measurementOOBCompositeList,
        MeasurementDefinition measurementDefinition, List<MeasurementDataNumericHighLowComposite> data, int height) {

        MetricGraphData metricGraphData = MetricGraphData.createForResource(resource.getId(), resource.getName(),
            measurementDefinition, data, measurementOOBCompositeList );
        StackedBarMetricGraphImpl graph = GWT.create(StackedBarMetricGraphImpl.class);
        graph.setMetricGraphData(metricGraphData);
        MetricD3Graph graphView = new MetricD3Graph(graph);

        graphView.setWidth("95%");
        graphView.setHeight(height);

        vLayout.addMember(graphView);
    }

}
