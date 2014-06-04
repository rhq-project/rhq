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
package org.rhq.coregui.client.inventory.groups.detail.monitoring.table;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.layout.HLayout;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.measurement.composite.MeasurementNumericValueAndUnits;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.JsonMetricProducer;
import org.rhq.coregui.client.Messages;
import org.rhq.coregui.client.dashboard.AutoRefreshUtil;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.gwt.ResourceGroupGWTServiceAsync;
import org.rhq.coregui.client.inventory.AutoRefresh;
import org.rhq.coregui.client.inventory.common.graph.ButtonBarDateTimeRangeEditor;
import org.rhq.coregui.client.inventory.common.graph.Refreshable;
import org.rhq.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.coregui.client.util.Log;
import org.rhq.coregui.client.util.MeasurementConverterClient;
import org.rhq.coregui.client.util.async.Command;
import org.rhq.coregui.client.util.async.CountDownLatch;
import org.rhq.coregui.client.util.enhanced.EnhancedHLayout;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;
import org.rhq.coregui.client.util.message.Message;

/**
 * This composite graph view has different graph types and data structures for
 * graphing multiple individual resources of the composite resource as
 * multi-line graph. Single Metric Multiple Resource graph.
 *
 * @author  Mike Thompson
 */
public abstract class CompositeGroupD3GraphListView extends EnhancedVLayout implements JsonMetricProducer, AutoRefresh, Refreshable {

    static protected final Messages MSG = CoreGUI.getMessages();
    // The d3 color palette onlys supports 20 colors so we limit to this
    private static final int MAX_NUMBER_OF_LINES_IN_GRAPH = 50;
    // string labels
    private final String chartTitleMinLabel = MSG.chart_title_min_label();
    private final String chartTitleAvgLabel = MSG.chart_title_avg_label();
    private final String chartTitlePeakLabel = MSG.chart_title_peak_label();
    private final String chartDateLabel = MSG.chart_date_label();
    private final String chartTimeLabel = MSG.chart_time_label();
    private final String chartHoverTimeFormat = MSG.chart_hover_time_format();
    private final String chartHoverDateFormat = MSG.chart_hover_date_format();
    private EntityContext context;
    private int definitionId;
    private MeasurementDefinition definition;
    private ButtonBarDateTimeRangeEditor buttonBarDateTimeRangeEditor;
    private String adjustedMeasurementUnits;
    private Set<Resource> childResources;

    /**
     * measurementForEachResource is a list of a list of single Measurement data for multiple resources.
     */
    private List<MultiLineGraphData> measurementForEachResource;
    private HLayout titleHLayout;
    private HTMLFlow graph;
    private String chartTitle;
    private Integer chartHeight;

    protected static Timer refreshTimer;
    protected boolean isRefreshing;

    public CompositeGroupD3GraphListView(EntityContext context, int defId) {
        super();
        this.context = context;
        setDefinitionId(defId);
        measurementForEachResource = new ArrayList<MultiLineGraphData>();
        buttonBarDateTimeRangeEditor = new ButtonBarDateTimeRangeEditor(this);
        setHeight100();
        setWidth100();
        setPadding(10);
        startRefreshCycle();
    }

    public void populateData() {
        ResourceGroupGWTServiceAsync groupService = GWTServiceLookup.getResourceGroupService();

        ResourceGroupCriteria criteria = new ResourceGroupCriteria();
        criteria.addFilterId(context.getGroupId());
        criteria.fetchResourceType(true);
        criteria.fetchExplicitResources(true);
        if (context.isAutoCluster()) {
            criteria.addFilterVisible(false);
        } else if (context.isAutoGroup()) {
            criteria.addFilterVisible(false);
            criteria.addFilterPrivate(true);
        }

        childResources = new TreeSet<Resource>();
        measurementForEachResource.clear();
        groupService.findResourceGroupCompositesByCriteria(criteria,
                new AsyncCallback<PageList<ResourceGroupComposite>>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(MSG.view_resource_monitor_graphs_lookupFailed(), caught);
                    }

                    @Override
                    public void onSuccess(PageList<ResourceGroupComposite> result) {
                        if (result.isEmpty()) {
                            return;
                        }

                        final ResourceGroup parentGroup = result.get(0).getResourceGroup();

                        //Limit the number of child lines in graph due to 20 colors being supported in d3 graph palette
                        final Set<Resource> fullSetOfChildResources = parentGroup.getExplicitResources();
                        int i = 0;
                        for (Iterator<Resource> resourceIterator = fullSetOfChildResources.iterator(); resourceIterator.hasNext(); ) {
                            Resource nextChildResource = resourceIterator.next();
                            if(i >= MAX_NUMBER_OF_LINES_IN_GRAPH){
                                CoreGUI.getMessageCenter().notify(new Message( MSG.chart_warning_max_composite_graphs_limit_exceeded()+" "+String.valueOf( MAX_NUMBER_OF_LINES_IN_GRAPH ), Message.Severity.Warning));
                                break;
                            }
                            childResources.add(nextChildResource);
                            i++;
                        }

                        chartTitle = parentGroup.getName();
                        Log.info("group name: " + parentGroup.getName() + ", size: " + parentGroup.getExplicitResources().size());
                        // setting up a deferred Command to execute after all resource queries have completed (successfully or unsuccessfully)
                        final CountDownLatch countDownLatch = CountDownLatch.create(childResources.size(), new Command() {
                            @Override
                            /**
                             * Do this only after ALL of the metric queries for each resource
                             */
                            public void execute() {
                                if (parentGroup.getExplicitResources().size() != measurementForEachResource.size()) {
                                    Log.warn("Number of graphs doesn't match number of resources");
                                    Log.warn("# of child resources: " + parentGroup.getExplicitResources().size());
                                    Log.warn("# of charted graphs: " + measurementForEachResource.size());
                                }
                                drawGraph();
                            }
                        });

                        if (!childResources.isEmpty()) {

                            // resourceType will be the same for all autogroup children so get first
                            Resource childResource = childResources.iterator().next();

                            ResourceTypeRepository.Cache.getInstance().getResourceTypes(
                                    childResource.getResourceType().getId(),
                                    EnumSet.of(ResourceTypeRepository.MetadataType.measurements),
                                    new ResourceTypeRepository.TypeLoadedCallback() {
                                        @Override
                                        public void onTypesLoaded(final ResourceType type) {

                                            for (MeasurementDefinition def : type.getMetricDefinitions()) {
                                                // only need the one selected measurement
                                                if (def.getId() == getDefinitionId()) {
                                                    setDefinition(def);
                                                }
                                            }

                                            for (final Resource childResource : childResources) {
                                                Log.debug("Adding child composite: " + childResource.getName()
                                                        + childResource.getId());

                                                GWTServiceLookup.getMeasurementDataService().findDataForResource(
                                                        childResource.getId(), new int[]{getDefinitionId()},
                                                        buttonBarDateTimeRangeEditor.getStartTime(),
                                                        buttonBarDateTimeRangeEditor.getEndTime(), 60,
                                                        new AsyncCallback<List<List<MeasurementDataNumericHighLowComposite>>>() {
                                                            @Override
                                                            public void onFailure(Throwable caught) {
                                                                CoreGUI.getErrorHandler().handleError(
                                                                        MSG.view_resource_monitor_graphs_loadFailed(), caught);
                                                                countDownLatch.countDown();
                                                            }

                                                            @Override
                                                            public void onSuccess(
                                                                    List<List<MeasurementDataNumericHighLowComposite>> measurements) {
                                                                addMeasurementForEachResource(childResource.getName(),
                                                                        childResource.getId(), measurements.get(0));
                                                                countDownLatch.countDown();
                                                            }
                                                        });
                                            }
                                        }
                                    });
                        }
                    }

                });

    }

    /**
     * Adding is done asynchronously, so we must synchronize the add.
     * @param resourceMeasurementList
     */
    public synchronized void addMeasurementForEachResource(String resourceName, int resourceId,
        List<MeasurementDataNumericHighLowComposite> resourceMeasurementList) {

        measurementForEachResource.add(new MultiLineGraphData(resourceName, resourceId, resourceMeasurementList));
    }

    @Override
    protected void onDraw() {
        super.onDraw();
        drawGraph();
    }

    @Override
    public void parentResized() {
        super.parentResized();
        removeMembers(getMembers());
        drawGraph();
    }

    public int getDefinitionId() {
        return definitionId;
    }

    public void setDefinitionId(int definitionId) {
        this.definitionId = definitionId;
        this.definition = null;
    }

    public String getChartId() {
        return  String.valueOf(definition.getId());
    }


    public void setDefinition(MeasurementDefinition definition) {
        this.definition = definition;
    }

    private void removeMembers() {

        removeMember(buttonBarDateTimeRangeEditor);
        if (null != titleHLayout)
            removeMember(titleHLayout);
        if (null != graph)
            removeMember(graph);
    }

    @Override
    public void refreshData() {
        populateData();
        drawGraph();
    }

    private void drawGraph() {
        Log.debug("drawGraph in CompositeGroupD3GraphListView for: " + definition + " (" + definitionId + ")");

        if (null != titleHLayout) {
            removeMembers();
        }

        addMember(buttonBarDateTimeRangeEditor);

        titleHLayout = new EnhancedHLayout();

        if (definition != null) {
            titleHLayout.setAutoHeight();
            titleHLayout.setWidth100();

            addMember(titleHLayout);

            graph = new HTMLFlow("<div id=\"mChart-" + getChartId()
                + "\" ><svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" style=\"height:95%;\"></svg></div>");
            graph.setWidth100();
            graph.setHeight100();
            addMember(graph);

            new Timer(){

                @Override
                public void run() {
                    buttonBarDateTimeRangeEditor.updateTimeRangeToNow();
                    drawJsniChart();
                }
            }.schedule(200);

        }

    }

    public String getYAxisTitle() {
        return definition.getName();
    }



    public String getXAxisTitle() {
        return MSG.view_charts_time_axis_label();
    }

    public String getChartTitleMinLabel() {
        return chartTitleMinLabel;
    }

    public String getChartTitleAvgLabel() {
        return chartTitleAvgLabel;
    }

    public String getChartTitlePeakLabel() {
        return chartTitlePeakLabel;
    }

    public String getChartDateLabel() {
        return chartDateLabel;
    }

    public String getChartTimeLabel() {
        return chartTimeLabel;
    }

    public String getChartHoverTimeFormat() {
        return chartHoverTimeFormat;
    }

    public String getChartHoverDateFormat() {
        return chartHoverDateFormat;
    }

    public String getButtonBarDateTimeFormat() {
        return MSG.common_buttonbar_datetime_format_moment_js();
    }

    public String getChartTitle() {
        return chartTitle;
    }

    public int getChartHeight() {
        return chartHeight != null ? chartHeight : 300;
    }

    public void setChartHeight(Integer chartHeight) {
        this.chartHeight = chartHeight;
    }

    /**
     * Takes a measurementList for each resource and turn it into an array.
     * @return String
     */
    private String produceInnerValuesArray(List<MeasurementDataNumericHighLowComposite> measurementList) {
        StringBuilder sb = new StringBuilder("[");
        for (MeasurementDataNumericHighLowComposite measurement : measurementList) {
            sb.append("{ \"x\":" + measurement.getTimestamp() + ",");
            if (!Double.isNaN(measurement.getValue())) {
                MeasurementNumericValueAndUnits dataValue = normalizeUnitsAndValues(measurement.getValue(),
                        definition.getUnits());
                sb.append(" \"y\":" + dataValue.getValue() + "},");
                adjustedMeasurementUnits = dataValue.getUnits().toString();
            }else {
                sb.append(" \"y\": 0},");
            }
        }
        sb.setLength(sb.length() - 1); // delete the last ','
        sb.append("]");
        return sb.toString();
    }

    @Override
    public String getJsonMetrics() {
        StringBuilder sb = new StringBuilder();
        if (null != measurementForEachResource && !measurementForEachResource.isEmpty()) {

            sb = new StringBuilder("[");
            for (MultiLineGraphData multiLineGraphData : measurementForEachResource) {
                if(null != multiLineGraphData.getMeasurementData() && multiLineGraphData.getMeasurementData().size() > 0){
                    sb.append("{ \"key\": \"");
                    sb.append(multiLineGraphData.getResourceNameEscaped());
                    sb.append("\",\"value\" : ");
                    sb.append(produceInnerValuesArray(multiLineGraphData.getMeasurementData()));
                    sb.append("},");
                }
            }
            sb.setLength(sb.length() - 1); // delete the last ','
            sb.append("]");
            Log.debug("Multi-resource Graph size: " + measurementForEachResource.size());
        }
        //Log.debug("Multi-resource Graph json: " + sb.toString());
        return sb.toString();
    }

    protected MeasurementNumericValueAndUnits normalizeUnitsAndValues(double value, MeasurementUnits measurementUnits) {
        MeasurementNumericValueAndUnits newValue = MeasurementConverterClient.fit(value, measurementUnits);
        MeasurementNumericValueAndUnits returnValue;

        // adjust for percentage numbers
        if (measurementUnits.equals(MeasurementUnits.PERCENTAGE)) {
            returnValue = new MeasurementNumericValueAndUnits(newValue.getValue() * 100, newValue.getUnits());
        } else {
            returnValue = new MeasurementNumericValueAndUnits(newValue.getValue(), newValue.getUnits());
        }

        return returnValue;
    }

    public String getYAxisUnits() {
        if (adjustedMeasurementUnits == null) {
            Log.warn("ResourceMetricD3GraphView.adjustedMeasurementUnits is populated by getJsonMetrics. Make sure it is called first.");
            return "";
        } else {
            return adjustedMeasurementUnits;
        }
    }

    protected String getXAxisTimeFormatHoursMinutes() {
        return MSG.chart_xaxis_time_format_hours_minutes();
    }

    protected String getXAxisTimeFormatHours() {
        return MSG.chart_xaxis_time_format_hours();
    }

    @Override
    public void startRefreshCycle() {
        refreshTimer = AutoRefreshUtil.startRefreshCycleWithPageRefreshInterval(this, this, refreshTimer);
    }

    @Override
    protected void onDestroy() {
        AutoRefreshUtil.onDestroy(refreshTimer);

        super.onDestroy();
    }

    @Override
    public boolean isRefreshing() {
        return isRefreshing;
    }

    //Custom refresh operation as we are not directly extending Table
    @Override
    public void refresh() {
        if (isVisible() && !isRefreshing()) {
            isRefreshing = true;
            try {
                buttonBarDateTimeRangeEditor.updateTimeRangeToNow();
                refreshData();
            } finally {
                isRefreshing = false;
            }
        }
    }
    /**
     * Client can choose which graph types to render.
     */
    public abstract void drawJsniChart();

    /**
     * Immutable data for each graph line.
     */
    private final class MultiLineGraphData {
        private String resourceName;
        private int resourceId;
        private List<MeasurementDataNumericHighLowComposite> measurementData;

        private MultiLineGraphData(String resourceName, int resourceId,
            List<MeasurementDataNumericHighLowComposite> measurmentData) {
            this.resourceName = resourceName;
            this.resourceId = resourceId;
            this.measurementData = measurmentData;
        }

        public String getResourceName() {
            return resourceName;
        }

        public String getResourceNameEscaped() {
            return resourceName.replace('"', '\'');
        }

        public int getResourceId() {
            return resourceId;
        }

        public List<MeasurementDataNumericHighLowComposite> getMeasurementData() {
            return measurementData;
        }
    }

}
