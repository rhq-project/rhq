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
package org.rhq.coregui.client.inventory.groups.detail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupAvailability;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.inventory.AutoRefresh;
import org.rhq.coregui.client.inventory.common.AbstractD3GraphListView;
import org.rhq.coregui.client.inventory.common.detail.AbstractTwoLevelTabSetView;
import org.rhq.coregui.client.inventory.common.graph.CustomDateRangeState;
import org.rhq.coregui.client.inventory.common.graph.MetricGraphData;
import org.rhq.coregui.client.inventory.common.graph.graphtype.AvailabilityOverUnderGraphType;
import org.rhq.coregui.client.inventory.common.graph.graphtype.StackedBarMetricGraphImpl;
import org.rhq.coregui.client.inventory.resource.detail.monitoring.MetricD3Graph;
import org.rhq.coregui.client.inventory.resource.detail.monitoring.avail.AvailabilityD3GraphView;
import org.rhq.coregui.client.util.Log;
import org.rhq.coregui.client.util.async.CountDownLatch;

/**
 * Build the Group version of the View that shows the individual graph views.
 * @author Mike Thompson
 */
public final class D3GroupGraphListView extends AbstractD3GraphListView implements AutoRefresh, AbstractTwoLevelTabSetView.ViewRenderedListener {

    private final ResourceGroup resourceGroup;
    private VLayout graphsVLayout;

    public D3GroupGraphListView(ResourceGroup resourceGroup, boolean monitorDetailView) {
        super();
        this.resourceGroup = resourceGroup;
        this.showAvailabilityGraph = monitorDetailView;
        setOverflow(Overflow.HIDDEN);
    }

    @Override
    protected void onDraw() {
        super.onDraw();
        destroyMembers();

        addMember(buttonBarDateTimeRangeEditor);
        if (showAvailabilityGraph) {
            availabilityGraph = AvailabilityD3GraphView.create(
                new AvailabilityOverUnderGraphType(resourceGroup.getId()));
            addMember(availabilityGraph);
        }
        graphsVLayout = new VLayout();
        graphsVLayout.setOverflow(Overflow.AUTO);
        graphsVLayout.setWidth100();
        graphsVLayout.setHeight100();

        buildGraphs();
        addMember(graphsVLayout);
    }

    public void refreshData() {
        this.onDraw();
    }

    /**
     * Build whatever graph metrics (MeasurementDefinitions) are defined for the resource.
     */
    private void buildGraphs() {

        queryAvailability(EntityContext.forGroup(resourceGroup), CustomDateRangeState.getInstance().getStartTime(),
            CustomDateRangeState.getInstance().getEndTime(), null);

        final ArrayList<MeasurementDefinition> measurementDefinitions = new ArrayList<MeasurementDefinition>();

        for (MeasurementDefinition def : resourceGroup.getResourceType().getMetricDefinitions()) {
            if (def.getDataType() == DataType.MEASUREMENT && def.getDisplayType() == DisplayType.SUMMARY) {
                measurementDefinitions.add(def);
            }
        }

        Collections.sort(measurementDefinitions, new Comparator<MeasurementDefinition>() {
            public int compare(MeasurementDefinition o1, MeasurementDefinition o2) {
                return new Integer(o1.getDisplayOrder()).compareTo(o2.getDisplayOrder());
            }
        });

        int[] measDefIdArray = new int[measurementDefinitions.size()];
        for (int i = 0; i < measDefIdArray.length; i++) {
            measDefIdArray[i] = measurementDefinitions.get(i).getId();
        }

        GWTServiceLookup.getMeasurementDataService().findDataForCompatibleGroup(resourceGroup.getId(), measDefIdArray,
            CustomDateRangeState.getInstance().getStartTime(), CustomDateRangeState.getInstance().getEndTime(), 60,
            new AsyncCallback<List<List<MeasurementDataNumericHighLowComposite>>>() {
                @Override
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_resource_monitor_graphs_loadFailed(), caught);
                    loadingLabel.setContents(MSG.view_resource_monitor_graphs_loadFailed());
                }

                @Override
                public void onSuccess(List<List<MeasurementDataNumericHighLowComposite>> result) {
                    if (result.isEmpty()) {
                        loadingLabel.setContents(MSG.view_resource_monitor_graphs_noneAvailable());
                    } else {
                        loadingLabel.hide();
                        int i = 0;
                        for (List<MeasurementDataNumericHighLowComposite> data : result) {
                            buildIndividualGraph(measurementDefinitions.get(i++), data);
                        }
                        // There is a weird timing case when availabilityGraph can be null
                        if (availabilityGraph != null) {
                            availabilityGraph.setGroupAvailabilityList(groupAvailabilityList);
                            new Timer() {
                                @Override
                                public void run() {
                                    availabilityGraph.drawJsniChart();
                                    buttonBarDateTimeRangeEditor.updateTimeRangeToNow();
                                }
                            }.schedule(150);
                        }
                    }
                }
            });

    }

    protected void queryAvailability(final EntityContext groupContext, Long startTime, Long endTime,
        final CountDownLatch countDownLatch) {

        final long timerStart = System.currentTimeMillis();

        // now return the availability
        GWTServiceLookup.getAvailabilityService().getAvailabilitiesForResourceGroup(groupContext.getGroupId(),
            startTime, endTime, new AsyncCallback<List<ResourceGroupAvailability>>() {
                @Override
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_resource_monitor_availability_loadFailed(), caught);
                    if (countDownLatch != null) {
                        countDownLatch.countDown();
                    }
                }

                @Override
                public void onSuccess(List<ResourceGroupAvailability> groupAvailList) {
                    Log.debug("\nSuccessfully queried group availability in: "
                        + (System.currentTimeMillis() - timerStart) + " ms.");
                    groupAvailabilityList = groupAvailList;
                    if (countDownLatch != null) {
                        countDownLatch.countDown();
                    }
                }
            });

    }

    @Override
    public void onViewRendered() {
        refreshData();
    }

    private void buildIndividualGraph(MeasurementDefinition measurementDefinition,
        List<MeasurementDataNumericHighLowComposite> data) {
        Log.debug("\n***** D3GroupGraphListView.MD: "+measurementDefinition);

        MetricGraphData metricGraphData = MetricGraphData.createForResourceGroup(resourceGroup.getId(),
            resourceGroup.getName(), measurementDefinition, data);

        StackedBarMetricGraphImpl graph = GWT.create(StackedBarMetricGraphImpl.class);
        graph.setMetricGraphData(metricGraphData);
        graph.setGraphListView(this);
        MetricD3Graph graphView = new MetricD3Graph<D3GroupGraphListView>(graph, this);

        graphView.setWidth("95%");
        graphView.setHeight(MULTI_CHART_HEIGHT);

        graphsVLayout.addMember(graphView);
    }

}
