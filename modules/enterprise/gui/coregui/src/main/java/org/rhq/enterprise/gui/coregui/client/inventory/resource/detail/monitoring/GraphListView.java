/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceSelectListener;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.layout.VLayout;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * @author Greg Hinkle
 */
public class GraphListView extends VLayout implements ResourceSelectListener {


    private Resource resource;


    public GraphListView() {
    }

    public GraphListView(Resource resource) {
        this.resource = resource;
        setOverflow(Overflow.SCROLL);
    }


    @Override
    protected void onDraw() {
        super.onDraw();

        for (Canvas c : getMembers()) {
            c.destroy();
        }
        if (resource != null) {
            buildGraphs();
        }
    }


    private void buildGraphs() {


        ResourceTypeRepository.Cache.getInstance().getResourceTypes(
                resource.getResourceType().getId(), EnumSet.of(ResourceTypeRepository.MetadataType.measurements),
                new ResourceTypeRepository.TypeLoadedCallback() {
                    public void onTypesLoaded(final ResourceType type) {

                        final ArrayList<MeasurementDefinition> measurementDefinitions = new ArrayList<MeasurementDefinition>();

                        for (MeasurementDefinition def : type.getMetricDefinitions()) {
                            if (def.getDataType() == DataType.MEASUREMENT && def.getDisplayType() == DisplayType.SUMMARY) {
                                measurementDefinitions.add(def);
                            }
                        }

                        int[] measDefIdArray = new int[measurementDefinitions.size()];
                        for (int i = 0; i < measDefIdArray.length; i++) {
                            measDefIdArray[i] = measurementDefinitions.get(i).getId();
                        }

                        GWTServiceLookup.getMeasurementDataService().findDataForResource(
                                resource.getId(),
                                measDefIdArray,
                                System.currentTimeMillis() - (1000L*60*60*8),
                                System.currentTimeMillis(),
                                60,
                                new AsyncCallback<List<List<MeasurementDataNumericHighLowComposite>>>() {
                                    public void onFailure(Throwable caught) {
                                        SC.say("Failed data load");
                                    }

                                    public void onSuccess(List<List<MeasurementDataNumericHighLowComposite>> result) {
                                        int i = 0;
                                        for (List<MeasurementDataNumericHighLowComposite> data : result) {
                                            buildGraph(measurementDefinitions.get(i++), data);
                                        }
                                    }
                                }
                                );

                    }
                }
        );
    }

    private void buildGraph(MeasurementDefinition def, List<MeasurementDataNumericHighLowComposite> data) {
        SmallGraphView graph = new SmallGraphView(def, data);
        graph.setWidth("80%");
        graph.setHeight(250);

        addMember(graph);

    }

    public void onResourceSelected(Resource resource) {
        this.resource = resource;




        buildGraphs();
        markForRedraw();
    }
}
