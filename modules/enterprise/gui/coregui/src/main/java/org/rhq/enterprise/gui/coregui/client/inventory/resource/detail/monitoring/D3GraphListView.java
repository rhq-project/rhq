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

import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.measurement.UserPreferencesMeasurementRangeEditor;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.MetricAreaBarGraphView;
import org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.MetricLineGraphView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * Build the View that shows the individual graph views for multi-graph
 * views if just a resource is provided and single graph view if resource
 * and  measurement definitionId are provided.
 *
 * @author Mike Thompson
 */
public class D3GraphListView extends LocatableVLayout {

    private Resource resource;
    private Set<Integer> definitionIds = null;
    private Label loadingLabel = new Label(MSG.common_msg_loading());
    private UserPreferencesMeasurementRangeEditor measurementRangeEditor;
    private boolean useSummaryData = false;

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

    private void commonConstructorSettings(){
        measurementRangeEditor = new UserPreferencesMeasurementRangeEditor(this.getLocatorId());
        setOverflow(Overflow.AUTO);
    }

    public void addSetButtonClickHandler(ClickHandler clickHandler) {
        Log.debug("measurementRangeEditor "+measurementRangeEditor);
        measurementRangeEditor.getSetButton().addClickHandler(clickHandler);
    }

    @Override
    protected void onDraw() {
        super.onDraw();
        //Log.debug(" *** Invoking onDraw()");

        destroyMembers();

        addMember(measurementRangeEditor);

        if (resource != null) {
            buildGraphs();
        }
    }


    public void redrawGraphs(){
       this.onDraw();
    }

    /**
     * Build whatever graph metrics (MeasurementDefinitions) are defined for the resource.
     */
    public void buildGraphs() {
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

                    GWTServiceLookup.getMeasurementDataService().findDataForResource(resource.getId(), measDefIdArray,
                        startTime, endTime, 60,
                        new AsyncCallback<List<List<MeasurementDataNumericHighLowComposite>>>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                CoreGUI.getErrorHandler().handleError(MSG.view_resource_monitor_graphs_loadFailed(),
                                    caught);
                                loadingLabel.setContents(MSG.view_resource_monitor_graphs_loadFailed());
                            }

                            @Override
                            public void onSuccess(List<List<MeasurementDataNumericHighLowComposite>> result) {
                                if (result.isEmpty()) {
                                    loadingLabel.setContents(MSG.view_resource_monitor_graphs_noneAvailable());
                                } else {
                                    loadingLabel.hide();
                                    if (useSummaryData) {
                                        buildSummaryGraph(result, summaryMeasurementDefinitions, measurementDefinitions);
                                    } else {
                                        determineGraphsToBuild(result, measurementDefinitions, definitionIds);
                                    }
                                }
                            }
                        });

                }

                private void buildSummaryGraph(List<List<MeasurementDataNumericHighLowComposite>> measurementData,
                    List<MeasurementDefinition> summaryMeasurementDefinitions, List<MeasurementDefinition> measurementDefinitions) {
                    Set<Integer> summaryIds = new TreeSet<Integer>();
                    for (MeasurementDefinition summaryMeasurementDefinition : summaryMeasurementDefinitions)
                    {
                        summaryIds.add(summaryMeasurementDefinition.getId());
                    }

                    int i = 0;
                    for (MeasurementDefinition measurementDefinition : measurementDefinitions)
                    {
                        if(summaryIds.contains(measurementDefinition.getId())){
                            buildIndividualGraph(measurementDefinition, measurementData.get(i), 130);
                        }
                        i++;
                    }

                }

                private void determineGraphsToBuild(List<List<MeasurementDataNumericHighLowComposite>> measurementData,
                    List<MeasurementDefinition> measurementDefinitions, Set<Integer> definitionIds) {
                    int i = 0;
                    for (List<MeasurementDataNumericHighLowComposite> measurement : measurementData) {

                        for (Integer selectedDefinitionId : definitionIds) {
                            final MeasurementDefinition measurementDefinition = measurementDefinitions.get(i);
                            final int measurementId = measurementDefinition.getId();

                            if (null != selectedDefinitionId) {
                                // single graph case
                                if (measurementId == selectedDefinitionId) {
                                    buildIndividualGraph(measurementDefinition, measurement, 360);
                                }
                            } else {
                                // multiple graph case
                                buildIndividualGraph(measurementDefinition, measurement, 330);
                            }
                        }
                        i++;
                    }
                }
            });

    }

    private void buildIndividualGraph(MeasurementDefinition measurementDefinition,
        List<MeasurementDataNumericHighLowComposite> data, int height) {

        //MetricLineGraphView graphView = new MetricLineGraphView("areaBarGraph", resource.getId(),resource.getName(), measurementDefinition, data);
        MetricAreaBarGraphView graphView = new MetricAreaBarGraphView("areaBarGraph", resource.getId(),resource.getName(), measurementDefinition, data);

        ResourceMetricD3GraphView graph = new ResourceMetricD3GraphView(
            extendLocatorId(measurementDefinition.getName()), resource.getId(),resource.getName(), measurementDefinition, data, graphView);

        graph.setWidth("95%");
        graph.setHeight(height);

        addMember(graph);
    }

}
