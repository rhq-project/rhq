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
package org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.table;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;
import com.smartgwt.client.widgets.layout.HLayout;

import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.JsonMetricProducer;
import org.rhq.enterprise.gui.coregui.client.components.measurement.UserPreferencesMeasurementRangeEditor;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGroupGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.async.Command;
import org.rhq.enterprise.gui.coregui.client.util.async.CountDownLatch;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * This composite graph view has different graph types and data structures for
 * graphing multiple individual resources of the composite resource as
 * multi-line graph. Single Metric Multiple Resource graph.
 *
 * @author  Mike Thompson
 */
public abstract class CompositeGroupD3GraphListView extends LocatableVLayout implements JsonMetricProducer
{

    private HTMLFlow resourceTitle;

    private int groupId;
    private int definitionId;

    private MeasurementDefinition definition;

    private UserPreferencesMeasurementRangeEditor measurementRangeEditor;

    /**
     * measurementForEachResource is a list of a list of single Measurement data for multiple resources.
     */
    private List<MultiLineGraphData> measurementForEachResource;
    private HLayout titleHLayout;
    private HTMLFlow title;
    private HTMLFlow graph;

    public CompositeGroupD3GraphListView(String locatorId, int groupId, int defId) {
        super(locatorId);
        this.groupId = groupId;
        setDefinitionId(defId);
        measurementForEachResource = new ArrayList<MultiLineGraphData>();
        measurementRangeEditor = new UserPreferencesMeasurementRangeEditor(this.getLocatorId());
        setHeight100();
        setWidth100();
    }

    public void populateData() {
        ResourceGroupGWTServiceAsync groupService = GWTServiceLookup.getResourceGroupService();

        ResourceGroupCriteria criteria = new ResourceGroupCriteria();
        criteria.addFilterId(groupId);
        criteria.fetchResourceType(true);
        criteria.addFilterVisible(false);
        criteria.addFilterPrivate(true);
        criteria.fetchExplicitResources(true);

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
                    Log.debug("group name: " + parentGroup.getName());
                    Log.debug("# of child resources: " + parentGroup.getExplicitResources().size());
                    // setting up a deferred Command to execute after all resource queries have completed (successfully or unsuccessfully)
                    final CountDownLatch countDownLatch = CountDownLatch.create(parentGroup.getExplicitResources().size(),
                            new Command() {
                        @Override
                        /**
                         * Do this only after ALL of the metric queries for each resource
                         */
                        public void execute() {
                            drawGraph();
                            redraw();
                        }
                    });

                    final Set<Resource> childResources = parentGroup.getExplicitResources();
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

                                    List<Long> times = measurementRangeEditor.getBeginEndTimes();
                                    Long startTime = times.get(0);
                                    Long endTime = times.get(1);
                                    measurementRangeEditor.getSetButton().addClickHandler(new ClickHandler()
                                    {
                                        @Override
                                        public void onClick(ClickEvent event)
                                        {
                                           drawGraph();
                                           populateData();
                                           markForRedraw();
                                        }
                                    });

                                    for (final Resource childResource : childResources) {
                                        Log.debug("Adding child composite: " + childResource.getName()
                                            + childResource.getId());

                                        GWTServiceLookup.getMeasurementDataService().findDataForResource(
                                                childResource.getId(), new int[]{getDefinitionId()}, startTime, endTime, 60,
                                                new AsyncCallback<List<List<MeasurementDataNumericHighLowComposite>>>()
                                                {
                                                    @Override
                                                    public void onFailure(Throwable caught)
                                                    {
                                                        countDownLatch.countDown();
                                                        CoreGUI.getErrorHandler().handleError(
                                                                MSG.view_resource_monitor_graphs_loadFailed(), caught);
                                                    }

                                                    @Override
                                                    public void onSuccess(List<List<MeasurementDataNumericHighLowComposite>> result)
                                                    {
                                                        countDownLatch.countDown();
                                                        addMeasurementForEachResource(childResource.getName(), childResource.getId(), result.get(0));
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
     * Immutable data for each graph line.
     */
    private final class MultiLineGraphData
    {
        private String resourceName;
        private int resourceId;
        private List<MeasurementDataNumericHighLowComposite> measurementData;

        private MultiLineGraphData(String resourceName, int resourceId, List<MeasurementDataNumericHighLowComposite> measurmentData)
        {
            this.resourceName = resourceName;
            this.resourceId = resourceId;
            this.measurementData = measurmentData;
        }

        public String getResourceName()
        {
            return resourceName;
        }

        public int getResourceId()
        {
            return resourceId;
        }

        public List<MeasurementDataNumericHighLowComposite> getMeasurementData()
        {
            return measurementData;
        }
    }

    /**
     * Adding is done asynchronously, so we must synchronize the add.
     * @param resourceMeasurementList
     */
    public synchronized  void addMeasurementForEachResource(String resourceName, int resourceId, List<MeasurementDataNumericHighLowComposite> resourceMeasurementList) {

        measurementForEachResource.add(new MultiLineGraphData(resourceName,resourceId,resourceMeasurementList));
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

    public String getChartId() {
        return groupId + "-" + definition.getId();
    }

    public void setDefinitionId(int definitionId) {
        this.definitionId = definitionId;
        this.definition = null;
    }

    public MeasurementDefinition getDefinition() {
        return definition;
    }

    public void setDefinition(MeasurementDefinition definition) {
        this.definition = definition;
    }

    private void removeMembers(){

        removeMember(measurementRangeEditor);
        if(null != titleHLayout) removeMember(titleHLayout);
        if(null != title) removeMember(title);
        if (null != graph) removeMember(graph);
    }

    private void drawGraph() {
        Log.debug("drawGraph in CompositeGroupD3GraphListView for: " + definition + "," + definitionId);

        if(null != titleHLayout){
            removeMembers();
        }

        addMember(measurementRangeEditor);

        titleHLayout = new LocatableHLayout(extendLocatorId("HTitle"));

        if (definition != null) {
            titleHLayout.setAutoHeight();
            titleHLayout.setWidth100();

            if (null != resourceTitle) {
                resourceTitle.setWidth("*");
                titleHLayout.addMember(resourceTitle);
            }

            addMember(titleHLayout);

            title = new HTMLFlow("<b>" + definition.getDisplayName() + "</b> " + definition.getDescription());
            title.setWidth100();
            addMember(title);
            graph = new HTMLFlow("<div id=\"mChart-" + getChartId()
                + "\" ><svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" style=\"height:95%;\"></svg></div>");
            graph.setWidth100();
            graph.setHeight100();
            addMember(graph);

            drawJsniChart();
            markForRedraw();

        }

    }

    public String getYAxisTitle() {
        return definition.getName();
    }

    public String getYAxisUnits() {
        return definition.getUnits().toString();
    }

    public String getXAxisTitle() {
        return MSG.view_charts_time_axis_label();
    }

    /**
     * Takes a measurementList for each resource and turn it into an array.
     * @return String
     */
    private String produceInnerValuesArray(List<MeasurementDataNumericHighLowComposite> measurementList) {
        StringBuilder sb = new StringBuilder("[");
        for (MeasurementDataNumericHighLowComposite measurement : measurementList) {
            if (!Double.isNaN(measurement.getValue())) {
                sb.append("{ x:" + measurement.getTimestamp() + ",");
                sb.append(" y:" + MeasurementUnits.scaleUp(measurement.getValue(), definition.getUnits()) + "},");
            }
        }
        sb.setLength(sb.length() - 1); // delete the last ','
        sb.append("]");
        return sb.toString();
    }

    @Override
    public String getJsonMetrics() {
        StringBuilder sb = new StringBuilder("[");
        for (MultiLineGraphData multiLineGraphData : measurementForEachResource) {
            sb.append("{ values: ");
            sb.append(produceInnerValuesArray(multiLineGraphData.getMeasurementData()));
            sb.append(",key: '");
            sb.append(multiLineGraphData.getResourceName());
            sb.append("'},");
        }
        sb.setLength(sb.length() - 1); // delete the last ','
        sb.append("]");
        return sb.toString();
    }

    /**
     * If there is more than 2 days time window then return true so we can show day of week
     * in axis labels. Function to switch the timescale to whichever is more appropriate hours
     * or hours with days of week.
     * @return true if difference between startTime and endTime is >= x days
     */
    public boolean shouldDisplayDayOfWeekInXAxisLabel() {
        // because of asyncrony this is possible so default it
        if(null == measurementForEachResource || measurementForEachResource.isEmpty()){
           return true;
        }
        List<MeasurementDataNumericHighLowComposite> firstResourceMeasurementList = measurementForEachResource.get(0).getMeasurementData();
        Long startTime = firstResourceMeasurementList.get(0).getTimestamp();
        Long endTime = firstResourceMeasurementList.get(firstResourceMeasurementList.size() - 1).getTimestamp();
        long timeThreshold = 24 * 60 * 60 * 1000; // 1 days
        return startTime + timeThreshold < endTime;
    }

    /**
     * Client can choose which graph types to render.
     */
    public abstract void drawJsniChart();

}
