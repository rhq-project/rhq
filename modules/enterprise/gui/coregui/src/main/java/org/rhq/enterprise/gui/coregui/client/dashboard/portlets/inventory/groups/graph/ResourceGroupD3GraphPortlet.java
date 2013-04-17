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
package org.rhq.enterprise.gui.coregui.client.dashboard.portlets.inventory.groups.graph;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.SubmitValuesEvent;
import com.smartgwt.client.widgets.form.events.SubmitValuesHandler;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.SpacerItem;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.form.SortedSelectItem;
import org.rhq.enterprise.gui.coregui.client.components.selector.AssignedItemsChangedEvent;
import org.rhq.enterprise.gui.coregui.client.components.selector.AssignedItemsChangedHandler;
import org.rhq.enterprise.gui.coregui.client.dashboard.AutoRefreshPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.AutoRefreshUtil;
import org.rhq.enterprise.gui.coregui.client.dashboard.CustomSettingsPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.Portlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletViewFactory;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletWindow;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGroupGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.MetricGraphData;
import org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.MetricStackedBarGraph;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.MetricD3Graph;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.ResourceScheduledMetricDatasource;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.selection.SingleResourceGroupSelector;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.util.BrowserUtility;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.server.measurement.util.MeasurementUtils;

/**
 * @author Greg Hinkle
 * @author Jay Shaughnessy
 * @author Mike Thompson
 */
public class ResourceGroupD3GraphPortlet extends MetricD3Graph implements AutoRefreshPortlet, CustomSettingsPortlet {

    // A non-displayed, persisted identifier for the portlet
    public static final String KEY = "ResourceGroupMetricD3";
    // A default displayed, persisted name for the portlet
    public static final String NAME = "d3-"+MSG.view_portlet_defaultName_groupMetric();

    // set on initial configuration, the window for this portlet view.
    private PortletWindow portletWindow;


    public static final String CFG_RESOURCE_GROUP_ID = "resourceGroupId";
    public static final String CFG_DEFINITION_ID = "definitionId";

    public ResourceGroupD3GraphPortlet() {
        setOverflow(Overflow.HIDDEN);
    }

    @Override
    /**
     * Portlet Charts are defined by an additional portletId to enable a particular groupId/measurementId
     * combination to be valid in multiple dashboards.
     */
    public String getFullChartId(){
        if(portletWindow != null && graph != null && graph.getMetricGraphData() != null){
            return "rChart-"+ graph.getMetricGraphData().getChartId() +"-"+portletWindow.getStoredPortlet().getId();
        } else {
            // handle the case where the portlet has not been configured yet
            return "";
        }
    }

    public void configure(PortletWindow portletWindow, DashboardPortlet storedPortlet) {

        if (null == this.portletWindow && null != portletWindow) {
            this.portletWindow = portletWindow;
        }
        destroyMembers();


        if ((null == storedPortlet) || (null == storedPortlet.getConfiguration())) {
            return;
        }else  if (BrowserUtility.isBrowserPreIE9()){
            addMember(new Label("<i>" + MSG.chart_ie_not_supported() + "</i>"));
            return;
        }
        setGraph(new MetricStackedBarGraph(MetricGraphData.createForDashboard(portletWindow.getStoredPortlet().getId())));

        if (storedPortlet.getConfiguration().getSimple(CFG_RESOURCE_GROUP_ID) != null) {
            PropertySimple resourceIdProperty = storedPortlet.getConfiguration().getSimple(CFG_RESOURCE_GROUP_ID);
            PropertySimple measurementDefIdProperty = storedPortlet.getConfiguration().getSimple(CFG_DEFINITION_ID);
            if (resourceIdProperty != null && measurementDefIdProperty != null) {
                final Integer entityId = resourceIdProperty.getIntegerValue();
                final Integer measurementDefId = measurementDefIdProperty.getIntegerValue();
                if (entityId != null && measurementDefId != null) {
                    queryResourceGroup(entityId, measurementDefId);
                }

            }
        }
    }


    private void queryResourceGroup(Integer entityId, final Integer measurementDefId) {
        ResourceGroupGWTServiceAsync resourceService = GWTServiceLookup.getResourceGroupService();

        ResourceGroupCriteria resourceCriteria = new ResourceGroupCriteria();
        resourceCriteria.addFilterId(entityId);
        resourceService.findResourceGroupsByCriteria(resourceCriteria, new AsyncCallback<PageList<ResourceGroup>>() {
            @Override
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_resource_monitor_graphs_lookupFailed(), caught);
            }

            @Override
            public void onSuccess(PageList<ResourceGroup> result) {
                if (result.isEmpty()) {
                    return;
                }
                // only concerned with first resource since this is a query by id
                final ResourceGroup resource = result.get(0);
                HashSet<Integer> typesSet = new HashSet<Integer>();
                typesSet.add(resource.getResourceType().getId());

                ResourceTypeRepository.Cache.getInstance().getResourceTypes(
                        typesSet.toArray(new Integer[typesSet.size()]),
                        EnumSet.of(ResourceTypeRepository.MetadataType.measurements),
                        new ResourceTypeRepository.TypesLoadedCallback() {

                            @Override
                            public void onTypesLoaded(Map<Integer, ResourceType> types) {
                                ResourceType type = types.get(resource.getResourceType().getId());
                                for (final MeasurementDefinition def : type.getMetricDefinitions()) {
                                    if (def.getId() == measurementDefId) {
                                        Log.debug("Found portlet measurement definition !" + def);

                                        getJsniChart().setEntityId(resource.getId());
                                        getJsniChart().setEntityName(resource.getName());
                                        getJsniChart().setDefinition(def);
                                        final long startTime = System.currentTimeMillis();

                                        GWTServiceLookup.getMeasurementDataService().findDataForCompatibleGroupForLast(
                                                resource.getId(), new int[]{def.getId()}, 8, MeasurementUtils.UNIT_HOURS,
                                                60, new AsyncCallback<List<List<MeasurementDataNumericHighLowComposite>>>() {
                                            @Override
                                            public void onFailure(Throwable caught) {
                                                CoreGUI.getErrorHandler().handleError(
                                                        MSG.view_resource_monitor_graphs_loadFailed(), caught);
                                            }

                                            @Override
                                            public void onSuccess(
                                                    final List<List<MeasurementDataNumericHighLowComposite>> measurementData) {
                                                Log.debug("Dashboard Metric data in: "
                                                        + (System.currentTimeMillis() - startTime) + " ms.");
                                                graph.getMetricGraphData().setMetricData(measurementData.get(0));
                                                drawGraph();
                                            }
                                        });
                                        break;
                                    }
                                }
                            }
                        });
            }
        });
    }

    public Canvas getHelpCanvas() {
        return new HTMLFlow(MSG.view_portlet_help_graph());
    }

    @Override
    protected void onDraw() {
        DashboardPortlet storedPortlet = portletWindow.getStoredPortlet();

        PropertySimple simple = storedPortlet.getConfiguration().getSimple(CFG_RESOURCE_GROUP_ID);
        if (simple == null || simple.getIntegerValue() == null) {
            removeMembers(getMembers());
            addMember(new Label("<i>" + MSG.view_portlet_configure_needed() + "</i>"));
        } else {
            super.onDraw();
        }
    }

    public DynamicForm getCustomSettingsForm() {
        final DynamicForm form = new DynamicForm();
        form.setWidth(750);
        form.setNumCols(1);

        final CanvasItem selectorItem = new CanvasItem();
        selectorItem.setTitleOrientation(TitleOrientation.TOP);
        selectorItem.setShowTitle(false);

        final SingleResourceGroupSelector resourceGroupSelector = new SingleResourceGroupSelector(
            GroupCategory.COMPATIBLE, false);
        resourceGroupSelector.setWidth(700);
        resourceGroupSelector.setHeight(300);

        final SelectItem metric = new SortedSelectItem(CFG_DEFINITION_ID, MSG.common_title_metric()) {
            @Override
            protected Criteria getPickListFilterCriteria() {
                Criteria criteria = new Criteria();

                if (resourceGroupSelector.getSelectedItems().size() == 1) {
                    int groupId = resourceGroupSelector.getSelectedItems().iterator().next().getId();
                    criteria.addCriteria(CFG_RESOURCE_GROUP_ID, groupId);
                    form.setValue(CFG_RESOURCE_GROUP_ID, groupId);
                }
                return criteria;
            }
        };
        metric.setWidth(300);
        metric.setTitleOrientation(TitleOrientation.TOP);
        metric.setValueField("id");
        metric.setDisplayField("displayName");
        metric.setOptionDataSource(new ResourceScheduledMetricDatasource());

        resourceGroupSelector.addAssignedItemsChangedHandler(new AssignedItemsChangedHandler() {

            public void onSelectionChanged(AssignedItemsChangedEvent event) {

                if (resourceGroupSelector.getSelectedItems().size() == 1) {
                    metric.fetchData();
                    form.clearValue(CFG_DEFINITION_ID);
                }
            }
        });

        final DashboardPortlet storedPortlet = portletWindow.getStoredPortlet();

        if (storedPortlet.getConfiguration().getSimple(CFG_RESOURCE_GROUP_ID) != null) {
            Integer integerValue = storedPortlet.getConfiguration().getSimple(CFG_RESOURCE_GROUP_ID).getIntegerValue();
            if (integerValue != null)
                form.setValue(CFG_RESOURCE_GROUP_ID, integerValue);

            PropertySimple propertySimple = storedPortlet.getConfiguration().getSimple(CFG_DEFINITION_ID);
            if (propertySimple != null && propertySimple.getIntegerValue() != null)
                form.setValue(CFG_DEFINITION_ID, propertySimple.getIntegerValue());
        }

        selectorItem.setCanvas(resourceGroupSelector);
        form.setFields(selectorItem, metric, new SpacerItem());

        form.addSubmitValuesHandler(new SubmitValuesHandler() {
            public void onSubmitValues(SubmitValuesEvent submitValuesEvent) {
                storedPortlet.getConfiguration().put(
                    new PropertySimple(CFG_RESOURCE_GROUP_ID, form.getValue(CFG_RESOURCE_GROUP_ID)));
                storedPortlet.getConfiguration().put(
                    new PropertySimple(CFG_DEFINITION_ID, form.getValue(CFG_DEFINITION_ID)));

                configure(portletWindow, storedPortlet);

                redraw();
            }
        });

        return form;
    }

    @Override
    public void redraw() {
        super.redraw();

        removeMembers(getMembers());

        DashboardPortlet storedPortlet = portletWindow.getStoredPortlet();
        PropertySimple simple = storedPortlet.getConfiguration().getSimple(CFG_RESOURCE_GROUP_ID);
        if (BrowserUtility.isBrowserPreIE9()){
            addMember(new Label("<i>" + MSG.chart_ie_not_supported() + "</i>"));
        } else if (simple == null || simple.getIntegerValue() == null) {
            addMember(new Label("<i>" + MSG.view_portlet_configure_needed() + "</i>"));
        } else {
            drawGraph();
        }
    }
    public void startRefreshCycle() {
        refreshTimer = AutoRefreshUtil.startRefreshCycle(this, this, refreshTimer);
    }

    @Override
    protected void onDestroy() {
        AutoRefreshUtil.onDestroy(refreshTimer);

        super.onDestroy();
    }

    public boolean isRefreshing() {
        return false;
    }

    //Custom refresh operation as we are not directly extending Table
    @Override
    public void refresh() {
        if (isVisible() && !isRefreshing()) {
            redraw();
        }
    }

    public static final class Factory implements PortletViewFactory {
        public static PortletViewFactory INSTANCE = new Factory();

        public final Portlet getInstance(EntityContext context) {

            return new ResourceGroupD3GraphPortlet();
        }
    }
}
