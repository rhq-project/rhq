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
package org.rhq.coregui.client.dashboard.portlets.inventory.groups.graph;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ResizedEvent;
import com.smartgwt.client.widgets.events.ResizedHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.SubmitValuesEvent;
import com.smartgwt.client.widgets.form.events.SubmitValuesHandler;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.grid.ListGridRecord;

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
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.LinkManager;
import org.rhq.coregui.client.components.form.SortedSelectItem;
import org.rhq.coregui.client.components.selector.AssignedItemsChangedEvent;
import org.rhq.coregui.client.components.selector.AssignedItemsChangedHandler;
import org.rhq.coregui.client.dashboard.AutoRefreshPortlet;
import org.rhq.coregui.client.dashboard.AutoRefreshUtil;
import org.rhq.coregui.client.dashboard.CustomSettingsPortlet;
import org.rhq.coregui.client.dashboard.Portlet;
import org.rhq.coregui.client.dashboard.PortletViewFactory;
import org.rhq.coregui.client.dashboard.PortletWindow;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.gwt.ResourceGroupGWTServiceAsync;
import org.rhq.coregui.client.inventory.common.graph.MetricGraphData;
import org.rhq.coregui.client.inventory.common.graph.graphtype.StackedBarMetricGraphImpl;
import org.rhq.coregui.client.inventory.resource.detail.monitoring.MetricD3Graph;
import org.rhq.coregui.client.inventory.resource.detail.monitoring.ResourceScheduledMetricDatasource;
import org.rhq.coregui.client.inventory.resource.selection.SingleResourceGroupSelector;
import org.rhq.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.coregui.client.util.BrowserUtility;
import org.rhq.coregui.client.util.Log;
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
    public static final String NAME = MSG.view_portlet_defaultName_groupMetric();

    // set on initial configuration, the window for this portlet view.
    private PortletWindow portletWindow;


    public static final String CFG_RESOURCE_GROUP_ID = "resourceGroupId";
    public static final String CFG_DEFINITION_ID = "definitionId";
    public static final String CFG_CUSTOM_TITLE = "customTitle";

    public ResourceGroupD3GraphPortlet() {
        setOverflow(Overflow.CLIP_H);

        addResizedHandler(new ResizedHandler() {
            public void onResized(ResizedEvent event) {
                refresh();
            }
        });
    }

    @Override
    /**
     * Portlet Charts are defined by an additional portletId to enable a particular groupId/measurementId
     * combination to be valid in multiple dashboards.
     */
    public String getFullChartId() {
        if (portletWindow != null && graph != null && graph.getMetricGraphData() != null) {
            return "rChart-" + graph.getMetricGraphData().getChartId() + "-" + portletWindow.getStoredPortlet().getId();
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
        } else if (BrowserUtility.isBrowserPreIE9()) {
            addMember(new Label("<i>" + MSG.chart_ie_not_supported() + "</i>"));
            return;
        }
        graph = GWT.create(StackedBarMetricGraphImpl.class);
        graph.setMetricGraphData(MetricGraphData.createForDashboard(portletWindow.getStoredPortlet().getId()));

        if (storedPortlet.getConfiguration().getSimple(CFG_RESOURCE_GROUP_ID) != null) {
            refreshFromConfiguration(storedPortlet);
        }
    }

    private void refreshFromConfiguration(DashboardPortlet storedPortlet) {
        PropertySimple resourceIdProperty = storedPortlet.getConfiguration().getSimple(CFG_RESOURCE_GROUP_ID);
        PropertySimple measurementDefIdProperty = storedPortlet.getConfiguration().getSimple(CFG_DEFINITION_ID);
        String customTitle = storedPortlet.getConfiguration().getSimpleValue(CFG_CUSTOM_TITLE, "");
        if (resourceIdProperty != null && measurementDefIdProperty != null) {
            final Integer entityId = resourceIdProperty.getIntegerValue();
            final Integer measurementDefId = measurementDefIdProperty.getIntegerValue();
            if (entityId != null && measurementDefId != null) {
                queryResourceGroup(entityId, measurementDefId, customTitle);
            }

        }
    }

    private void queryResourceGroup(Integer entityId, final Integer measurementDefId, String customTitle) {
        ResourceGroupGWTServiceAsync resourceService = GWTServiceLookup.getResourceGroupService();
        final String portletTitle = customTitle == null || customTitle.isEmpty() ? NAME : customTitle;
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
                final ResourceGroup group = result.get(0);
                HashSet<Integer> typesSet = new HashSet<Integer>();
                typesSet.add(group.getResourceType().getId());

                ResourceTypeRepository.Cache.getInstance().getResourceTypes(
                    typesSet.toArray(new Integer[typesSet.size()]),
                    EnumSet.of(ResourceTypeRepository.MetadataType.measurements),
                    new ResourceTypeRepository.TypesLoadedCallback() {

                        @Override
                        public void onTypesLoaded(Map<Integer, ResourceType> types) {
                            ResourceType type = types.get(group.getResourceType().getId());
                            for (final MeasurementDefinition def : type.getMetricDefinitions()) {
                                if (def.getId() == measurementDefId) {
                                    Log.debug("Found portlet measurement definition !" + def);

                                    // Adding the resource group link in the portlet pushed the chart down too far, so
                                    // I'm adding it to the title. TODO: In the future (RHQ Metrics) the link
                                    // back should be done better.
                                    portletWindow.setTitle(portletTitle
                                        + " - "
                                        + LinkManager.getHref(LinkManager.getResourceGroupLink(group.getId()),
                                            group.getName()));

                                    graph.setEntityId(group.getId());
                                    graph.setEntityName(group.getName());
                                    graph.setDefinition(def);
                                    final long startTime = System.currentTimeMillis();

                                    GWTServiceLookup.getMeasurementDataService().findDataForCompatibleGroupForLast(
                                        group.getId(), new int[] { def.getId() }, 8, MeasurementUtils.UNIT_HOURS,
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

    @Override
    protected void setupGraphDiv(HTMLFlow graphDiv) {
        graphDiv.setHeight100();
        graphDiv.setWidth("750px");
    }

    public Canvas getHelpCanvas() {
        return new HTMLFlow(MSG.view_portlet_help_graph());
    }

    @Override
    protected void onDraw() {
        DashboardPortlet storedPortlet = portletWindow.getStoredPortlet();
        if (BrowserUtility.isBrowserPreIE9()) {
            removeMembers(getMembers());
            addMember(new Label("<i>" + MSG.chart_ie_not_supported() + "</i>"));

        } else {
            PropertySimple simple = storedPortlet.getConfiguration().getSimple(CFG_RESOURCE_GROUP_ID);
            if (simple == null || simple.getIntegerValue() == null) {
                removeMembers(getMembers());
                addMember(new Label("<i>" + MSG.view_portlet_configure_needed() + "</i>"));
            } else {
                super.onDraw();
            }
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

                if (resourceGroupSelector.getSelection().size() == 1) {
                    int groupId = resourceGroupSelector.getSelection().iterator().next();
                    criteria.addCriteria(CFG_RESOURCE_GROUP_ID, groupId);
                    form.setValue(CFG_RESOURCE_GROUP_ID, groupId);
                }
                return criteria;
            }
        };

        final TextItem customTitle = new TextItem(CFG_CUSTOM_TITLE, MSG.view_portlet_configure_customTitle());
        customTitle.setWidth(300);
        customTitle.setTitleOrientation(TitleOrientation.TOP);

        metric.setWidth(300);
        metric.setTitleOrientation(TitleOrientation.TOP);
        metric.setValueField("id");
        metric.setDisplayField("displayName");
        metric.setOptionDataSource(new ResourceScheduledMetricDatasource());

        resourceGroupSelector.addAssignedItemsChangedHandler(new AssignedItemsChangedHandler() {

            public void onSelectionChanged(AssignedItemsChangedEvent event) {

                if (resourceGroupSelector.getSelection().size() == 1) {
                    metric.fetchData();
                    form.clearValue(CFG_DEFINITION_ID);
                }
            }
        });

        final DashboardPortlet storedPortlet = portletWindow.getStoredPortlet();

        if (storedPortlet.getConfiguration().getSimple(CFG_RESOURCE_GROUP_ID) != null) {
            Integer integerValue = storedPortlet.getConfiguration().getSimple(CFG_RESOURCE_GROUP_ID).getIntegerValue();
            if (integerValue != null) {
                form.setValue(CFG_RESOURCE_GROUP_ID, integerValue);
                ListGridRecord group = new ListGridRecord();
                group.setAttribute("id", integerValue);
                ListGridRecord[] groups = { group };
                resourceGroupSelector.setAssigned(groups);
            }

            PropertySimple propertySimple = storedPortlet.getConfiguration().getSimple(CFG_DEFINITION_ID);
            if (propertySimple != null && propertySimple.getIntegerValue() != null)
                form.setValue(CFG_DEFINITION_ID, propertySimple.getIntegerValue());
            form.setValue(CFG_CUSTOM_TITLE, storedPortlet.getConfiguration().getSimpleValue(CFG_CUSTOM_TITLE));
        }

        selectorItem.setCanvas(resourceGroupSelector);
        form.setFields(selectorItem, metric, customTitle, new SpacerItem());

        form.addSubmitValuesHandler(new SubmitValuesHandler() {
            public void onSubmitValues(SubmitValuesEvent submitValuesEvent) {
                ResourceGroup selectedGroup = resourceGroupSelector.getSelectedGroup();
                String groupId = selectedGroup == null ? null : String.valueOf(selectedGroup.getId()); // can be null
                storedPortlet.getConfiguration().put(new PropertySimple(CFG_RESOURCE_GROUP_ID, groupId));
                storedPortlet.getConfiguration().put(
                    new PropertySimple(CFG_DEFINITION_ID, form.getValue(CFG_DEFINITION_ID)));
                storedPortlet.getConfiguration().put(
                    new PropertySimple(CFG_CUSTOM_TITLE, form.getValueAsString(CFG_CUSTOM_TITLE)));
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
        if (BrowserUtility.isBrowserPreIE9()) {
            addMember(new Label("<i>" + MSG.chart_ie_not_supported() + "</i>"));
        } else if (simple == null || simple.getIntegerValue() == null) {
            addMember(new Label("<i>" + MSG.view_portlet_configure_needed() + "</i>"));
        } else {
            drawGraph();
        }
    }

    public void startRefreshCycle() {
        refreshTimer = AutoRefreshUtil.startRefreshCycleWithPageRefreshInterval(this, this, refreshTimer);
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
            refreshFromConfiguration(portletWindow.getStoredPortlet());
        }
    }

    public static final class Factory implements PortletViewFactory {
        public static final PortletViewFactory INSTANCE = new Factory();

        public final Portlet getInstance(EntityContext context) {

            return new ResourceGroupD3GraphPortlet();
        }
    }
}
