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

import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.layout.HLayout;

import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.components.measurement.UserPreferencesMeasurementRangeEditor;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGroupGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.MeasurementUtility;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.SeleniumUtility;

/**
 * This composite graph view has different graph types and data structures for
 * graphing multiple individual resources of the composite resource as
 * multiline graph
 * @author  Mike Thompson
 */
public class CompositeGroupD3GraphListView extends LocatableVLayout
{

    private HTMLFlow resourceTitle;

    private int groupId;
    private int definitionId;

    private MeasurementDefinition definition;

    private UserPreferencesMeasurementRangeEditor measurementRangeEditor;
    /**
     * multiData is a list of a list of Measurement data for multiple metrics.
     */
    private List<List<MeasurementDataNumericHighLowComposite>> multiData;



    public CompositeGroupD3GraphListView(String locatorId, int groupId, int defId) {
        super(locatorId);
        this.groupId = groupId;
        setDefinitionId(defId);
        measurementRangeEditor = new UserPreferencesMeasurementRangeEditor(this.getLocatorId());
        setHeight100();
        setWidth100();
    }

    public void populateData() {
        Log.debug(" ** populateData");
        ResourceGroupGWTServiceAsync groupService = GWTServiceLookup.getResourceGroupService();

        ResourceGroupCriteria criteria = new ResourceGroupCriteria();
        criteria.addFilterId(groupId);
        criteria.fetchResourceType(true);
        criteria.addFilterVisible(false);
        groupService.findResourceGroupsByCriteria(criteria, new AsyncCallback<PageList<ResourceGroup>>() {
            @Override
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_resource_monitor_graphs_lookupFailed(), caught);
            }

            @Override
            public void onSuccess(PageList<ResourceGroup> result) {
                if (result.isEmpty()) {
                    Log.debug(" *** group is empty ");
                    return;
                }

                final ResourceGroup group = result.get(0);
                Log.debug(" *** group: "+group.getName()+","+group.getAutoGroupParentResource().getId());
                Log.debug(" *** type: "+group.getResourceType().getId());

                ResourceGroupGWTServiceAsync groupService = GWTServiceLookup.getResourceGroupService();

                ResourceGroupCriteria autoGroupCriteria = new ResourceGroupCriteria();


                // for autoclusters and private groups (autogroups) we need to add more criteria
                boolean isAutoCluster = (null != group.getClusterResourceGroup());
                boolean isAutoGroup = (null != group.getSubject());

                if (isAutoCluster) {
                    autoGroupCriteria.addFilterVisible(false);

                } else if (isAutoGroup) {
                    autoGroupCriteria.addFilterVisible(false);
                    autoGroupCriteria.addFilterPrivate(true);
                }

                autoGroupCriteria.addFilterResourceTypeId(group.getResourceType().getId());
                autoGroupCriteria.addFilterAutoGroupParentResourceId(group.getAutoGroupParentResource().getId());
                groupService.findResourceGroupsByCriteria(autoGroupCriteria, new AsyncCallback<PageList<ResourceGroup>>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(MSG.view_resource_monitor_graphs_lookupFailed(), caught);
                    }

                    @Override
                    public void onSuccess(PageList<ResourceGroup> result) {
                        Log.debug(" *** autogroup members: "+ result.size());

                        final ResourceGroup autoGroup = result.get(0);
                        Log.debug(" autogroup name: "+autoGroup.getName() +", "+autoGroup.getAutoGroupParentResource().getName());
                        Log.debug(" autogroup ids: "+autoGroup.getId() +", parent:"+autoGroup.getAutoGroupParentResource().getId());
                        Log.debug(" autogroup size #: "+autoGroup.getAutoGroupParentResource().getChildResources().size());

                        Set<Resource> childResources = autoGroup.getAutoGroupParentResource().getChildResources();
                        for (Resource res : childResources)
                        {
                            Log.debug(" *** Resource -> " + res.getName() + ":" + res.getId());
                        }

                            }
                        });


                String url = LinkManager.getResourceGroupLink(group);
                resourceTitle = new HTMLFlow(SeleniumUtility.getLocatableHref(url, group.getName(), null));

                ResourceTypeRepository.Cache.getInstance().getResourceTypes(group.getResourceType().getId(),
                    EnumSet.of(ResourceTypeRepository.MetadataType.measurements),
                    new ResourceTypeRepository.TypeLoadedCallback() {
                        @Override
                        public void onTypesLoaded(final ResourceType type) {

                            for (MeasurementDefinition def : type.getMetricDefinitions()) {
                                // only need the one selected measurement
                                if (def.getId() == getDefinitionId()) {
                                    setDefinition(def);

                                    GWTServiceLookup.getMeasurementDataService().findDataForCompatibleGroupForLast(
                                        groupId, new int[] { getDefinitionId() }, 8,
                                        MeasurementUtility.UNIT_HOURS, 60,
                                        new AsyncCallback<List<List<MeasurementDataNumericHighLowComposite>>>() {
                                            @Override
                                            public void onFailure(Throwable caught) {
                                                CoreGUI.getErrorHandler().handleError(
                                                    MSG.view_resource_monitor_graphs_loadFailed(), caught);
                                            }

                                            @Override
                                            public void onSuccess(
                                                List<List<MeasurementDataNumericHighLowComposite>> result) {
                                                setMultiData(result);
                                                Log.debug(" *** #metrics for group: " + result.size());
                                                drawGraph();
                                            }
                                        });
                                }
                            }
                        }
                    });
            }
        });

    }

    public List<List<MeasurementDataNumericHighLowComposite>> getMultiData() {
        return multiData;
    }

    public void setMultiData(List<List<MeasurementDataNumericHighLowComposite>> multiData) {
        this.multiData = multiData;
    }
    public int getDefinitionId() {
        return definitionId;
    }
    public String getChartId(){
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


    private void drawGraph() {
        Log.debug("drawGraph in CompositeGroupD3GraphListView for: "+ definition + ","+definitionId);

        addMember(measurementRangeEditor );

        HLayout titleHLayout = new LocatableHLayout(extendLocatorId("HTitle"));

        if (definition != null) {
            titleHLayout.setAutoHeight();
            titleHLayout.setWidth100();

            if (null != resourceTitle) {
                resourceTitle.setWidth("*");
                titleHLayout.addMember(resourceTitle);
            }

            addMember(titleHLayout);

            HTMLFlow title = new HTMLFlow("<b>" + definition.getDisplayName() + "</b> " + definition.getDescription());
            title.setWidth100();
            addMember(title);
            HTMLFlow graph = new HTMLFlow("<div id=\"mChart-"+getChartId()+"\" ><svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" style=\"height:100%;\"></svg></div>");
            graph.setWidth100();
            graph.setHeight100();
            addMember(graph);

            //drawJsniCharts();
            markForRedraw();

        }

    }

    public native void drawJsniCharts() /*-{
        console.log("Draw nvd3 charts for composite multiline graph");
        var chartId =  this.@org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMetricD3GraphView::getChartId()();
        var chartHandle = "#mChart-"+chartId;
        var chartSelection = chartHandle + " svg";
        var yAxisLabel = this.@org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMetricD3GraphView::getYAxisTitle()();
        var yAxisUnits = this.@org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMetricD3GraphView::getYAxisUnits()();
        var xAxisLabel = this.@org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMetricD3GraphView::getXAxisTitle()();
        var json = eval(this.@org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMetricD3GraphView::getJsonMetrics()());

        var data = function() {
        return [
                {
                values: json,
                key: yAxisLabel ,
                color: '#ff7f0e'
                }
            ];
        };

        $wnd.nv.addGraph(function() {
            var chart = $wnd.nv.models.multiBarChart()
            .showControls(false)
            .tooltips(true);

        chart.xAxis.axisLabel(xAxisLabel)
            .tickFormat(function(d) { return $wnd.d3.time.format('%a %I %p')(new Date(d)) });

        chart.yAxis
            .axisLabel(yAxisUnits)
            .tickFormat($wnd.d3.format(',f'));

        $wnd.d3.select(chartSelection)
            .datum(data())
            .transition().duration(300)
            .call(chart);

        $wnd.nv.utils.windowResize(chart.update);

        return chart;
        });

    }-*/;


    public CompositeGroupD3GraphListView getInstance(String locatorId, int groupId, int definitionId){
        return new CompositeGroupD3GraphListView(locatorId, groupId, definitionId);
    }


}
