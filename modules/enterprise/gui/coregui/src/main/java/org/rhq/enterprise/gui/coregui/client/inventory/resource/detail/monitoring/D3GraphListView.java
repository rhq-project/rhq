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

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.Label;

import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.measurement.UserPreferencesMeasurementRangeEditor;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * Build the View that shows the individual graph views for multi-graph
 * views if just a resource is provided and single graph view if resource
 * and definitionId are provided.
 *
 * @author Mike Thompson
 */
public class D3GraphListView extends LocatableVLayout {

    private Resource resource;
    private Integer definitionId = null;
    private Label loadingLabel = new Label(MSG.common_msg_loading());
    private UserPreferencesMeasurementRangeEditor measurementRangeEditor;

    public D3GraphListView(String locatorId, Resource resource, int definitionId){
        super(locatorId);
        measurementRangeEditor = new UserPreferencesMeasurementRangeEditor(this.getLocatorId());
        this.resource = resource;
        setOverflow(Overflow.AUTO);
        this.definitionId = definitionId;
    }

    public D3GraphListView(String locatorId, Resource resource) {
        super(locatorId);
        measurementRangeEditor = new UserPreferencesMeasurementRangeEditor(this.getLocatorId());
        this.resource = resource;
        setOverflow(Overflow.AUTO);
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

    /**
     * Build whatever graph metrics (MeasurementDefinitions) are defined for the resource.
     */
    private void buildGraphs() {
        List<Long> startEndList =  measurementRangeEditor.getBeginEndTimes();
        final long startTime = startEndList.get(0);
        final long endTime = startEndList.get(1);

        ResourceTypeRepository.Cache.getInstance().getResourceTypes(resource.getResourceType().getId(),
            EnumSet.of(ResourceTypeRepository.MetadataType.measurements),
            new ResourceTypeRepository.TypeLoadedCallback() {
                public void onTypesLoaded(final ResourceType type) {

                    final ArrayList<MeasurementDefinition> measurementDefinitions = new ArrayList<MeasurementDefinition>();

                    for (MeasurementDefinition def : type.getMetricDefinitions()) {
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

                    GWTServiceLookup.getMeasurementDataService().findDataForResource(resource.getId(), measDefIdArray, startTime, endTime,60,
                            new AsyncCallback<List<List<MeasurementDataNumericHighLowComposite>>>()
                            {
                                @Override
                                public void onFailure(Throwable caught)
                                {
                                    CoreGUI.getErrorHandler().handleError(MSG.view_resource_monitor_graphs_loadFailed(),
                                            caught);
                                    loadingLabel.setContents(MSG.view_resource_monitor_graphs_loadFailed());
                                }


                                @Override
                                public void onSuccess(List<List<MeasurementDataNumericHighLowComposite>> result)
                                {
                                    if (result.isEmpty())
                                    {
                                        loadingLabel.setContents(MSG.view_resource_monitor_graphs_noneAvailable());
                                    }
                                    else
                                    {
                                        loadingLabel.hide();
                                        int i = 0;
                                        for (List<MeasurementDataNumericHighLowComposite> data : result)
                                        {
                                            if(null != definitionId){
                                                // single graph case
                                                int measurementId = measurementDefinitions.get(i).getId();
                                                if(measurementId == definitionId){
                                                    buildIndividualGraph(measurementDefinitions.get(i), data);
                                                }
                                            }else {
                                                // multiple graph case
                                                buildIndividualGraph(measurementDefinitions.get(i), data);
                                            }
                                            i++;
                                        }
                                    }
                                }
                            });

                }
            });
    }

    private void buildIndividualGraph(MeasurementDefinition measurementDefinition, List<MeasurementDataNumericHighLowComposite> data ) {
        buildIndividualGraph(measurementDefinition,data, 130);
    }

    private void buildIndividualGraph(MeasurementDefinition measurementDefinition, List<MeasurementDataNumericHighLowComposite> data, int height) {

        ResourceMetricD3GraphView graph = new ResourceMetricD3GraphView(extendLocatorId(measurementDefinition.getName()), resource.getId(),
            measurementDefinition, data);

        graph.setWidth("95%");
        graph.setHeight(height);

        addMember(graph);
    }

}
