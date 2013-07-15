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
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.ExpansionMode;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.RecordExpandEvent;
import com.smartgwt.client.widgets.grid.events.RecordExpandHandler;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.IconEnum;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractD3GraphListView;
import org.rhq.enterprise.gui.coregui.client.inventory.common.graph.graphtype.AvailabilityOverUnderGraphType;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.MetricD3Graph;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.avail.AvailabilityD3GraphView;
import org.rhq.enterprise.gui.coregui.client.util.BrowserUtility;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.async.CountDownLatch;
import org.rhq.enterprise.gui.coregui.client.util.enhanced.EnhancedHLayout;

/**
 * The consolidated metrics view showing metric graphs and availability data both in graphical and tabular form.
 *
 * @author Mike Thompson
 */
public class MetricsResourceView extends AbstractD3GraphListView {

    private static final String COLLAPSED_TOOLTIP = MSG.chart_metrics_collapse_tooltip();
    private static final String EXPANDED_TOOLTIP = MSG.chart_metrics_expand_tooltip();

    private Resource resource;
    private Set<Integer> definitionIds = null;
    private VLayout vLayout;
    private Img expandCollapseArrow;
    MetricsTableListGrid metricsGrid;
    MetricsViewDataSource dataSource;
    EnhancedHLayout availabilityDetails;


    public MetricsResourceView(Resource resource) {
        super();
        this.resource = resource;
        dataSource = new MetricsViewDataSource(resource);
        metricsGrid = new MetricsTableListGrid();

        ArrayList<ListGridField> fields = dataSource.getListGridFields();
        metricsGrid.setFields(fields.toArray(new ListGridField[0]));
        metricsGrid.setCanExpandRecords(true);
        metricsGrid.addRecordExpandHandler(new RecordExpandHandler() {
            @Override
            public void onRecordExpand(RecordExpandEvent recordExpandEvent) {
                Log.debug("Record Expanded: "
                    + recordExpandEvent.getRecord().getAttribute(MetricsViewDataSource.FIELD_METRIC_LABEL));
                new Timer() {

                    @Override
                    public void run() {
                        BrowserUtility.graphSparkLines();
                    }
                }.schedule(150);

            }

        });

    }

    private EnhancedHLayout createAvailabilityDetails() {
        EnhancedHLayout hLayout = new EnhancedHLayout();
        hLayout.addMember(new Label("Availability Details"));
        hLayout.hide();
        return hLayout;
    }

    public void redrawGraphs() {
        this.onDraw();
    }

    @Override
    protected void onDraw() {
        super.onDraw();
        Log.debug("MetricResourceView.onDraw() for: " + resource.getName() + " id: " + resource.getId());
        destroyMembers();


        vLayout = new VLayout();
        vLayout.setOverflow(Overflow.AUTO);
        vLayout.setWidth100();
        vLayout.setHeight(220);
        vLayout.addMember(buttonBarDateTimeRangeEditor);

        EnhancedHLayout expandCollapseHLayout = new EnhancedHLayout();

        //add expand/collapse icon
        expandCollapseArrow = new Img(IconEnum.COLLAPSED_ICON.getIcon16x16Path(), 16, 16);
        expandCollapseArrow.setTooltip(COLLAPSED_TOOLTIP);
        expandCollapseArrow.setLayoutAlign(VerticalAlignment.BOTTOM);
        expandCollapseArrow.addClickHandler(new ClickHandler() {
            private boolean collapsed = true;

            @Override
            public void onClick(ClickEvent event) {
                collapsed = !collapsed;
                if (collapsed) {
                    expandCollapseArrow.setSrc(IconEnum.COLLAPSED_ICON.getIcon16x16Path());
                    expandCollapseArrow.setTooltip(COLLAPSED_TOOLTIP);
                    availabilityDetails.hide();
                }
                else {
                    expandCollapseArrow.setSrc(IconEnum.EXPANDED_ICON.getIcon16x16Path());
                    expandCollapseArrow.setTooltip(EXPANDED_TOOLTIP);
                    availabilityDetails.show();
                }
                markForRedraw();
            }
        });

        availabilityDetails = createAvailabilityDetails();

        availabilityGraph = new AvailabilityD3GraphView<AvailabilityOverUnderGraphType>(
            new AvailabilityOverUnderGraphType(resource.getId()));

        expandCollapseHLayout.addMember(expandCollapseArrow);
        expandCollapseHLayout.addMember(availabilityGraph);
        vLayout.addMember(expandCollapseHLayout);
        vLayout.addMember(availabilityDetails);

        vLayout.addMember(metricsGrid);

        addMember(vLayout);

        if (resource != null) {
            queryAvailability(EntityContext.forResource(resource.getId()), buttonBarDateTimeRangeEditor.getStartTime(),
                    buttonBarDateTimeRangeEditor.getEndTime(), null);
        }
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
                    Log.debug("\nSuccessfully queried availability in: " + (System.currentTimeMillis() - timerStart)
                        + " ms.");
                    availabilityList = availList;
                    new Timer() {
                        @Override
                        public void run() {
                            availabilityGraph.drawJsniChart();
                        }
                    }.schedule(150);

                    if (countDownLatch != null) {
                        countDownLatch.countDown();
                    }
                }
            });
    }

    private class MetricsTableListGrid extends ListGrid {
        public MetricsTableListGrid() {
            super();

            setCanExpandRecords(true);
            setCanExpandMultipleRecords(true);
            setExpansionMode(ExpansionMode.DETAIL_FIELD);
        }

        @Override
        protected Canvas getExpansionComponent(final ListGridRecord record) {
            final Integer definitionId = record.getAttributeAsInt(MetricsViewDataSource.FIELD_METRIC_DEF_ID);
            final Integer resourceId = record.getAttributeAsInt(MetricsViewDataSource.FIELD_RESOURCE_ID);
            VLayout vLayout = new VLayout();
            vLayout.setPadding(5);

            final String chartId = "rChart" + resourceId + "-" + definitionId;
            HTMLFlow htmlFlow = new HTMLFlow(MetricD3Graph.createGraphMarkerTemplate(chartId, 200));
            vLayout.addMember(htmlFlow);

            new Timer() {

                @Override
                public void run() {
                    BrowserUtility.graphSparkLines();
                }
            }.schedule(150);


            // Load the fully fetched ResourceType.
            ResourceType resourceType = resource.getResourceType();

            Set<MeasurementDefinition> definitions = resourceType.getMetricDefinitions();
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
            GWTServiceLookup.getMeasurementDataService().findDataForResource(resourceId, definitionArrayIds,
                measurementUserPrefs.getMetricRangePreferences().begin,
                measurementUserPrefs.getMetricRangePreferences().end, 60,
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
                            List<MeasurementDataNumericHighLowComposite> data = results.get(0);
                            Log.debug(" *** Metric Data Results: "+data.size());

//                                                                                    MetricGraphData metricGraphData = MetricGraphData
//                                                                                        .createForResource(resourceId, resource.getName(),
//                                                                                            measurementDefinition, data, null);
//
//                                                                                    StackedBarMetricGraphImpl graph = GWT
//                                                                                        .create(StackedBarMetricGraphImpl.class);
//                                                                                    graph.setMetricGraphData(metricGraphData);
//                                                                                    final MetricD3Graph graphView = new MetricD3Graph(graph, this);
//                                                                                    new Timer() {
//                                                                                        @Override
//                                                                                        public void run() {
//                                                                                            graphView.drawJsniChart();
//
//                                                                                        }
//                                                                                    }.schedule(150);

                        } else {
                            Log.warn("No chart data retrieving for resource [" + resourceId + "]");

                        }
                    }
                });

            return vLayout;
        }
    }
}
