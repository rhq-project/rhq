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

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;

import org.rhq.core.domain.criteria.AvailabilityCriteria;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.measurement.composite.MeasurementOOBComposite;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.measurement.UserPreferencesMeasurementRangeEditor;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.MetricGraphData;
import org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.MetricStackedBarGraph;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.async.Command;
import org.rhq.enterprise.gui.coregui.client.util.async.CountDownLatch;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * Build the View that shows the individual graph views for multi-graph
 * views if just a resource is provided and single graph view if resource
 * and  measurement definitionId are provided.
 *
 * @author Mike Thompson
 */
public class D3GraphListView extends LocatableVLayout {

    private static int NUM_ASYNC_CALLS = 3; // wait for X async calls in Latch

    private Resource resource;
    private Set<Integer> definitionIds = null;
    private Label loadingLabel = new Label(MSG.common_msg_loading());
    private UserPreferencesMeasurementRangeEditor measurementRangeEditor;
    private boolean useSummaryData = false;
    private PageList<Availability> downAvailList;
    private PageList<MeasurementOOBComposite> measurementOOBCompositeList;
    private List<List<MeasurementDataNumericHighLowComposite>> metricsDataList;

    public static D3GraphListView createMultipleGraphs(String locatorId, Resource resource, Set<Integer> definitionIds) {

        return new D3GraphListView(locatorId, resource, definitionIds);
    }

    public static D3GraphListView createSummaryMultipleGraphs(String locatorId, Resource resource) {
        return new D3GraphListView(locatorId, resource);
    }

    public static D3GraphListView createSingleGraph(String locatorId, Resource resource, Integer measurementId) {
        TreeSet<Integer> definitionIds = new TreeSet<Integer>();
        definitionIds.add(measurementId);
        return new D3GraphListView(locatorId, resource, definitionIds);
    }

    private D3GraphListView(String locatorId, Resource resource, Set<Integer> definitionIds) {
        super(locatorId);
        this.resource = resource;
        commonConstructorSettings();
        this.definitionIds = definitionIds;
    }

    private D3GraphListView(String locatorId, Resource resource) {
        super(locatorId);
        this.resource = resource;
        commonConstructorSettings();
        useSummaryData = true;
    }

    private void commonConstructorSettings() {
        measurementRangeEditor = new UserPreferencesMeasurementRangeEditor(this.getLocatorId());
        setOverflow(Overflow.AUTO);
    }

    public void addSetButtonClickHandler(ClickHandler clickHandler) {
        Log.debug("measurementRangeEditor " + measurementRangeEditor);
        measurementRangeEditor.getSetButton().addClickHandler(clickHandler);
    }

    @Override
    protected void onDraw() {
        super.onDraw();
        destroyMembers();

        addMember(measurementRangeEditor);

        if (resource != null) {
            buildGraphs();
        }
    }

    public void redrawGraphs() {
        this.onDraw();
    }

    /**
     * Build whatever graph (summary or not) by grabbing the MeasurementDefinitions
     * that are defined for the resource and then querying the metric and availability data.
     */
    public void buildGraphs() {
        final long startTimer = System.currentTimeMillis();
        List<Long> startEndList = measurementRangeEditor.getBeginEndTimes();
        final long startTime = startEndList.get(0);
        final long endTime = startEndList.get(1);

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
                            }
                        }
                    });

                    queryMetricData(measDefIdArray, countDownLatch);
                    queryAvailability(resource, countDownLatch);
                    queryOOBMetrics(resource, countDownLatch);
                    // now the countDown latch will run sometime asynchronously

                }

                private void queryMetricData(final int[] measDefIdArray, final CountDownLatch countDownLatch) {
                    GWTServiceLookup.getMeasurementDataService().findDataForResource(resource.getId(), measDefIdArray,
                        startTime, endTime, 60,
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
                                Log.debug("Metric graph data queried in: "
                                    + (System.currentTimeMillis() - startTimer + " ms."));
                                countDownLatch.countDown();

                            }
                        });
                }

                private void queryAvailability(final Resource resource, final CountDownLatch countDownLatch) {

                    final long startTime = System.currentTimeMillis();

                    // now return the availability
                    AvailabilityCriteria c = new AvailabilityCriteria();
                    c.addFilterResourceId(resource.getId());
                    c.addFilterInitialAvailability(false);
                    c.addSortStartTime(PageOrdering.ASC);
                    GWTServiceLookup.getAvailabilityService().findAvailabilityByCriteria(c,
                        new AsyncCallback<PageList<Availability>>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                CoreGUI.getErrorHandler().handleError(
                                    MSG.view_resource_monitor_availability_loadFailed(), caught);
                                countDownLatch.countDown();
                            }

                            @Override
                            public void onSuccess(PageList<Availability> availList) {
                                Log.debug("\nSuccessfully queried availability in: "
                                    + (System.currentTimeMillis() - startTime) + " ms.");
                                downAvailList = new PageList<Availability>();
                                for (Availability availability : availList) {
                                    if (availability.getAvailabilityType().equals(AvailabilityType.DOWN)
                                        || availability.getAvailabilityType().equals(AvailabilityType.DISABLED)) {
                                        downAvailList.add(availability);
                                    }
                                }
                                Log.debug("Down avail list: " + downAvailList.size());
                                countDownLatch.countDown();
                            }
                        });
                }

                private void queryOOBMetrics(final Resource resource, final CountDownLatch countDownLatch) {

                    final long startTime = System.currentTimeMillis();

                    // now return the availability
                    GWTServiceLookup.getMeasurementDataService().getHighestNOOBsForResource(resource.getId(), 60,

                    new AsyncCallback<PageList<MeasurementOOBComposite>>() {
                        @Override
                        public void onSuccess(PageList<MeasurementOOBComposite> measurementOOBComposites) {

                            measurementOOBCompositeList = measurementOOBComposites;
                            Log.debug("\nSuccessfully queried OOB data in: "
                                    + (System.currentTimeMillis() - startTime) + " ms.");
                            Log.debug("OOB Data size: "+measurementOOBCompositeList.size());
                            if(null != measurementOOBCompositeList){
                                for (MeasurementOOBComposite measurementOOBComposite : measurementOOBComposites) {
                                    Log.debug("measurementOOBComposite = " + measurementOOBComposite);
                                }
                            }
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
                            buildSingleGraph(downAvailList, measurementOOBCompositeList, measurementDefinition,
                                measurementData.get(i), 250);
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
                                    buildSingleGraph(downAvailList, measurementOOBCompositeList, measurementDefinition,
                                        metric, 450);
                                }
                            } else {
                                // multiple graph case
                                buildSingleGraph(downAvailList, measurementOOBCompositeList, measurementDefinition,
                                    metric, 250);
                            }
                        }
                        i++;
                    }
                }
            });

    }

    private void buildSingleGraph(PageList<Availability> downAvailList,
        PageList<MeasurementOOBComposite> measurementOOBCompositeList, MeasurementDefinition measurementDefinition,
        List<MeasurementDataNumericHighLowComposite> data, int height) {
        MetricGraphData metricGraphData = new MetricGraphData(resource.getId(), resource.getName(),
            measurementDefinition, data);
        MetricStackedBarGraph graph = new MetricStackedBarGraph(metricGraphData);
        graph.setAvailabilityDownList(downAvailList);
        graph.setMeasurementOOBCompositeList(measurementOOBCompositeList);

        ResourceMetricD3GraphView graphView = new ResourceMetricD3GraphView(
            extendLocatorId(measurementDefinition.getName()), metricGraphData, graph);

        graphView.setWidth("95%");
        graphView.setHeight(height);

        addMember(graphView);
    }

}
