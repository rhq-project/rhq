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

import java.util.List;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;

import org.rhq.core.domain.criteria.AvailabilityCriteria;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.HasD3JsniChart;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMetricD3GraphView;
import org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.MetricGraphData;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.AncestryUtil;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.async.Command;
import org.rhq.enterprise.gui.coregui.client.util.async.CountDownLatch;
import org.rhq.enterprise.server.measurement.util.MeasurementUtils;


public class ResourceMetricD3GraphView extends AbstractMetricD3GraphView
{
    /**
     * Defines the jsniChart type like area, line, etc...
     *
     */
    private HasD3JsniChart jsniChart;

    /**
     * This constructor is for the use case in the Dashboard where we dont actually
     * have a entity or measurement yet.
     * @param locatorId
     */
    public ResourceMetricD3GraphView(String locatorId){
        super(locatorId);
        //setChartHeight("150px");
    }



    public ResourceMetricD3GraphView(String locatorId, MetricGraphData metricGraphData, HasD3JsniChart jsniChart ) {

        super(locatorId, metricGraphData);
        this.jsniChart = jsniChart;
        //setChartHeight("150px");
    }


    @Override
    /**
     * Render the graph by determining if we need to load definition for
     * the dashboard graph (which will be empty, all other graph types
     * will have the definition already defined and we can just render the graph).
     */
    protected void renderGraph() {
        boolean isDashboardGraph = (null == metricGraphData.getDefinition());
        if (isDashboardGraph) {
            Log.debug("Chart path for: dashboard metrics");
            queryMetricsDataForDashboardGraphs();
        } else {
            Log.debug("Chart path for: non-dashboard metrics");
            drawGraph();
        }
    }

    private void queryMetricsDataForDashboardGraphs(){
        Log.debug(" ** RenderGraph  Dashboard Portlet path");
        final long startTime = System.currentTimeMillis();

        ResourceGWTServiceAsync resourceService = GWTServiceLookup.getResourceService();

        ResourceCriteria resourceCriteria = new ResourceCriteria();
        resourceCriteria.addFilterId(metricGraphData.getEntityId());
        resourceService.findResourcesByCriteria(resourceCriteria, new AsyncCallback<PageList<Resource>>() {
            @Override
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_resource_monitor_graphs_lookupFailed(), caught);
            }

            @Override
            public void onSuccess(PageList<Resource> result) {
                if (result.isEmpty()) {
                    return;
                }
                // only concerned with first resource since this is a query by id
                final Resource firstResource = result.get(0);

                // setting up a deferred Command to execute after all resource queries have completed (successfully or unsuccessfully)
                // we know there are exactly 2 resources
                final CountDownLatch countDownLatch = CountDownLatch.create(2,
                        new Command() {
                            @Override
                            /**
                             * Satisfied only after ALL of the metric queries AND availability have completed
                             */
                            public void execute() {
                                Log.debug("Time for async query: "+(System.currentTimeMillis() - startTime));
                                drawGraph();
                                //redraw();
                            }
                        });

                queryAvailability(firstResource, countDownLatch);
                queryMeasurementsAndMetricData(firstResource, countDownLatch);
                // now the countDown latch will run sometime asynchronously after BOTH the previous 2 queries have executed
            }
        });
    }

    private void queryAvailability(final Resource resource, final CountDownLatch countDownLatch){

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
                        CoreGUI.getErrorHandler().handleError(MSG.view_resource_monitor_availability_loadFailed(), caught);
                        countDownLatch.countDown();
                    }

                    @Override
                    public void onSuccess(PageList<Availability> availList) {
                        Log.debug("\nSuccessfully queried availability in: "+ (System.currentTimeMillis() - startTime) + " ms.");
                        PageList<Availability> downAvailList = new PageList<Availability>();
                        for (Availability availability : availList)
                        {
                            if(availability.getAvailabilityType().equals(AvailabilityType.DOWN)
                                    || availability.getAvailabilityType().equals(AvailabilityType.DISABLED)){
                                downAvailList.add(availability);
                            }
                        }
                        metricGraphData.setAvailabilityDownList(downAvailList);
                        countDownLatch.countDown();
                    }
                });
    }


    private void queryMeasurementsAndMetricData(final Resource resource, final CountDownLatch countDownLatch){
        final long startTime = System.currentTimeMillis();
        HashSet<Integer> typesSet = new HashSet<Integer>();
        typesSet.add(resource.getResourceType().getId());
        HashSet<String> ancestries = new HashSet<String>();
        ancestries.add(resource.getAncestry());
        // In addition to the types of the result resources, get the types of their ancestry
        typesSet.addAll(AncestryUtil.getAncestryTypeIds(ancestries));

        ResourceTypeRepository.Cache.getInstance().getResourceTypes(
                typesSet.toArray(new Integer[typesSet.size()]),
                EnumSet.of(ResourceTypeRepository.MetadataType.measurements),
                new ResourceTypeRepository.TypesLoadedCallback() {

                    @Override
                    public void onTypesLoaded(Map<Integer, ResourceType> types) {
                        ResourceType type = types.get(resource.getResourceType().getId());
                        for (MeasurementDefinition def : type.getMetricDefinitions()) {
                            if (def.getId() == metricGraphData.getDefinitionId()) {
                                metricGraphData.setDefinition(def);

                                GWTServiceLookup.getMeasurementDataService().findDataForResourceForLast(resource.getId(),
                                        new int[] { metricGraphData.getDefinitionId() }, 8, MeasurementUtils.UNIT_HOURS, 60,
                                        new AsyncCallback<List<List<MeasurementDataNumericHighLowComposite>>>() {
                                            @Override
                                            public void onFailure(Throwable caught) {
                                                CoreGUI.getErrorHandler().handleError(
                                                        MSG.view_resource_monitor_graphs_loadFailed(), caught);
                                                countDownLatch.countDown();
                                            }

                                            @Override
                                            public void onSuccess(final
                                                                  List<List<MeasurementDataNumericHighLowComposite>> measurementData) {
                                                Log.debug("\nSuccessfully queried Metric data in: "+ (System.currentTimeMillis() - startTime)+ " ms." );
                                                metricGraphData.setMetricData(measurementData.get(0));
                                                countDownLatch.countDown();
                                            }
                                        });
                            }
                        }
                    }
                });
    }

    @Override
    protected boolean supportsLiveGraphViewDialog() {
        return true;
    }



    @Override
    /**
     * Delegate the call to rendering the JSNI chart.
     * This way the chart type can be swapped out at any time.
     */
    public void drawJsniChart()
    {
        jsniChart.drawJsniChart();
    }

    public void setJsniChart(HasD3JsniChart jsniChart)
    {
        this.jsniChart = jsniChart;
    }

    public HasD3JsniChart getJsniChart()
    {
        return jsniChart;
    }

    @Override
    protected void displayLiveGraphViewDialog() {
        LiveGraphD3View.displayAsDialog(getLocatorId(), metricGraphData.getEntityId(), metricGraphData.getDefinition());
    }
}
