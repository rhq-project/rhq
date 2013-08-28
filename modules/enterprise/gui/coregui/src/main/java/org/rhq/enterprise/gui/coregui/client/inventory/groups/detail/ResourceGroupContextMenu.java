/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.enterprise.gui.coregui.client.inventory.groups.detail;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuItem;
import com.smartgwt.client.widgets.menu.MenuItemSeparator;
import com.smartgwt.client.widgets.menu.events.ClickHandler;
import com.smartgwt.client.widgets.menu.events.MenuItemClickEvent;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeNode;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.criteria.DashboardCriteria;
import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.dashboard.Dashboard;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.Messages;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.inventory.groups.graph.ResourceGroupD3GraphPortlet;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.ResourceDetailView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.ResourceTreeDatasource.AutoGroupTreeNode;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.ResourceTreeView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

/**
 * @author Jay Shaughnessy
 * @author Greg Hinkle
 */
public class ResourceGroupContextMenu extends Menu {

    protected Messages MSG = CoreGUI.getMessages();

    private ResourceGroupComposite groupComposite;
    private ResourceGroup group;
    private ResourceType groupMemberType;

    private boolean isAutoCluster = false;
    private boolean isAutoGroup = false;

    public void showContextMenu(final VLayout treeView, final TreeGrid treeGrid, final TreeNode node,
        final ResourceGroup group) {
        // we need the group composite to access permissions for context menu authz, so get it now
        ResourceGroupCriteria criteria = new ResourceGroupCriteria();
        criteria.addFilterId(group.getId());

        // for autoclusters and private groups (autogroups) we need to add more criteria
        isAutoCluster = (null != group.getClusterResourceGroup());
        isAutoGroup = (null != group.getSubject());

        if (isAutoCluster) {
            criteria.addFilterVisible(false);

        } else if (isAutoGroup) {
            criteria.addFilterVisible(false);
            criteria.addFilterPrivate(true);
        }

        GWTServiceLookup.getResourceGroupService().findResourceGroupCompositesByCriteria(criteria,
            new AsyncCallback<PageList<ResourceGroupComposite>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(
                        MSG.view_group_detail_failLoadComp(String.valueOf(group.getId())), caught);
                }

                public void onSuccess(PageList<ResourceGroupComposite> result) {
                    if (result.isEmpty()) {
                        CoreGUI.getErrorHandler().handleError(
                            MSG.view_group_detail_failLoadComp(String.valueOf(group.getId())));
                    } else {
                        showContextMenu(treeView, treeGrid, node, result.get(0));
                    }
                }
            });
    }

    public void showContextMenu(final VLayout treeView, final TreeGrid treeGrid, final TreeNode node,
        ResourceGroupComposite groupComposite) {
        this.groupComposite = groupComposite;
        group = groupComposite.getResourceGroup();

        // [BZ 817604] If the group type has changed to mixed we can't show a context menu.
        if (GroupCategory.MIXED == group.getGroupCategory()) {
            CoreGUI.goToView(LinkManager.getResourceGroupLink(group.getId()));
            return;
        }

        groupMemberType = group.getResourceType();
        isAutoCluster = (null != group.getClusterResourceGroup());
        isAutoGroup = (null != group.getSubject());

        ResourceTypeRepository.Cache.getInstance().getResourceTypes(
            groupMemberType.getId(),
            EnumSet.of(ResourceTypeRepository.MetadataType.operations, ResourceTypeRepository.MetadataType.children,
                ResourceTypeRepository.MetadataType.subCategory,
                ResourceTypeRepository.MetadataType.pluginConfigurationDefinition,
                ResourceTypeRepository.MetadataType.resourceConfigurationDefinition),
            new ResourceTypeRepository.TypeLoadedCallback() {
                public void onTypesLoaded(ResourceType type) {

                    groupMemberType = type;
                    buildResourceGroupContextMenu(treeView, treeGrid, node, group, type);
                    showContextMenu();
                }
            });
    }

    private void buildResourceGroupContextMenu(final VLayout treeView, final TreeGrid treeGrid, final TreeNode node,
        final ResourceGroup group, final ResourceType resourceType) {
        // name
        setItems(new MenuItem(group.getName()));

        // type name
        addItem(new MenuItem("Type: " + resourceType.getName()));

        // Mixed group refresh is not needed as there is only a single top node. Compat group
        // refresh makes sense after a group membership change but we already perform a CoreGUI refresh to
        // reset the whole detail view after that user action. So, only support refresh for autogroup nodes
        // in the resource tree.
        if (node instanceof AutoGroupTreeNode) {
            // separator
            addItem(new MenuItemSeparator());

            // refresh node
            MenuItem refresh = new MenuItem(MSG.common_button_refresh());
            refresh.addClickHandler(new ClickHandler() {

                public void onClick(MenuItemClickEvent event) {
                    // refresh the tree and detail
                    ((ResourceTreeView) treeView).contextMenuRefresh(treeGrid, node);
                }
            });
            addItem(refresh);
        }

        // separator
        addItem(new MenuItemSeparator());

        // plugin config
        MenuItem pluginConfiguration = new MenuItem(MSG.view_tabs_common_connectionSettings());
        boolean pluginConfigEnabled = resourceType.getPluginConfigurationDefinition() != null;
        pluginConfiguration.setEnabled(pluginConfigEnabled);
        if (pluginConfigEnabled) {
            pluginConfiguration.addClickHandler(new ClickHandler() {
                public void onClick(MenuItemClickEvent event) {
                    CoreGUI.goToView(LinkManager.getEntityTabLink(EntityContext.forGroup(group), "Inventory",
                        "ConnectionSettings"));
                }
            });
        }
        addItem(pluginConfiguration);

        // resource config
        MenuItem resourceConfiguration = new MenuItem(MSG.view_tree_common_contextMenu_resourceConfiguration());
        boolean resourceConfigEnabled = groupComposite.getResourcePermission().isConfigureRead()
            && resourceType.getResourceConfigurationDefinition() != null;
        resourceConfiguration.setEnabled(resourceConfigEnabled);
        if (resourceConfigEnabled) {
            resourceConfiguration.addClickHandler(new ClickHandler() {
                public void onClick(MenuItemClickEvent event) {
                    CoreGUI.goToView(LinkManager.getEntityTabLink(EntityContext.forGroup(group), "Configuration",
                        "Current"));
                }
            });
        }
        addItem(resourceConfiguration);

        // separator
        addItem(new MenuItemSeparator());

        // Operations Menu
        MenuItem operations = new MenuItem(MSG.common_title_operations());
        boolean operationsEnabled = (groupComposite.getResourcePermission().isControl()
            && null != resourceType.getOperationDefinitions() && !resourceType.getOperationDefinitions().isEmpty());
        operations.setEnabled(operationsEnabled);
        if (operationsEnabled) {
            Menu opSubMenu = new Menu();
            //sort the display items alphabetically
            TreeSet<String> ordered = new TreeSet<String>();
            Map<String, OperationDefinition> definitionMap = new HashMap<String, OperationDefinition>();
            for (OperationDefinition o : resourceType.getOperationDefinitions()) {
                ordered.add(o.getDisplayName());
                definitionMap.put(o.getDisplayName(), o);
            }

            for (String displayName : ordered) {
                final OperationDefinition operationDefinition = definitionMap.get(displayName);

                MenuItem operationItem = new MenuItem(operationDefinition.getDisplayName());
                operationItem.addClickHandler(new ClickHandler() {
                    public void onClick(MenuItemClickEvent event) {
                        String viewPath = LinkManager.getEntityTabLink(EntityContext.forGroup(group),
                            ResourceDetailView.Tab.Operations.NAME, "Schedules")
                            + "/0/"
                            + operationDefinition.getId();
                        CoreGUI.goToView(viewPath);
                    }
                });
                opSubMenu.addItem(operationItem);
            }
            operations.setSubmenu(opSubMenu);
        }
        addItem(operations);

        // Metric graph addition menu
        addItem(buildMetricsMenu(resourceType, group));
    }

    private MenuItem buildMetricsMenu(final ResourceType type, final ResourceGroup resourceGroup) {
        MenuItem measurements = new MenuItem(MSG.view_tree_common_contextMenu_measurements());
        final Menu measurementsSubMenu = new Menu();

        DashboardCriteria criteria = new DashboardCriteria();
        final EntityContext context = EntityContext.forGroup(resourceGroup);
        if (!context.isAutoGroup()) {

            GWTServiceLookup.getDashboardService().findDashboardsByCriteria(criteria,
                new AsyncCallback<PageList<Dashboard>>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(MSG.view_tree_common_contextMenu_loadFail_dashboards(),
                            caught);
                    }

                    public void onSuccess(PageList<Dashboard> result) {

                        if (type.getMetricDefinitions() != null) {
                            //sort the display items alphabetically
                            TreeSet<String> ordered = new TreeSet<String>();
                            Map<String, MeasurementDefinition> definitionMap = new HashMap<String, MeasurementDefinition>();
                            for (MeasurementDefinition m : type.getMetricDefinitions()) {
                                ordered.add(m.getDisplayName());
                                definitionMap.put(m.getDisplayName(), m);
                            }

                            for (String displayName : ordered) {
                                final MeasurementDefinition def = definitionMap.get(displayName);
                                //only add menu items for Measurement
                                if (def.getDataType().equals(DataType.MEASUREMENT)) {
                                    MenuItem defItem = new MenuItem(def.getDisplayName());
                                    measurementsSubMenu.addItem(defItem);
                                    Menu defSubItem = new Menu();
                                    defItem.setSubmenu(defSubItem);

                                    for (final Dashboard d : result) {

                                        MenuItem addToDBItem = new MenuItem(MSG
                                            .view_tree_common_contextMenu_addChartToDashboard(d.getName()));
                                        defSubItem.addItem(addToDBItem);

                                        addToDBItem.addClickHandler(new ClickHandler() {
                                            public void onClick(MenuItemClickEvent menuItemClickEvent) {

                                                DashboardPortlet p = new DashboardPortlet(MSG
                                                    .view_tree_common_contextMenu_groupGraph(),
                                                    ResourceGroupD3GraphPortlet.KEY, 250);
                                                p.getConfiguration().put(
                                                    new PropertySimple(
                                                        ResourceGroupD3GraphPortlet.CFG_RESOURCE_GROUP_ID, group
                                                            .getId()));
                                                p.getConfiguration().put(
                                                    new PropertySimple(ResourceGroupD3GraphPortlet.CFG_DEFINITION_ID,
                                                        def.getId()));

                                                d.addPortlet(p);

                                                GWTServiceLookup.getDashboardService().storeDashboard(d,
                                                    new AsyncCallback<Dashboard>() {
                                                        public void onFailure(Throwable caught) {
                                                            CoreGUI
                                                                .getErrorHandler()
                                                                .handleError(
                                                                    MSG.view_tree_common_contextMenu_saveChartToDashboardFailure(),
                                                                    caught);
                                                        }

                                                        public void onSuccess(Dashboard result) {
                                                            String msg = MSG
                                                                .view_tree_common_contextMenu_saveChartToDashboardSuccessful(result
                                                                    .getName());
                                                            CoreGUI.getMessageCenter().notify(
                                                                new Message(msg, Message.Severity.Info));
                                                        }
                                                    });

                                            }
                                        });

                                        //add new menu item for adding current graphable element to view if on Monitor/Graphs tab
                                        String currentViewPath = History.getToken();
                                        if (currentViewPath.contains("Monitoring/NewGraphs")) {
                                            MenuItem addGraphItem = new MenuItem(MSG.common_title_add_graph_to_view());
                                            defSubItem.addItem(addGraphItem);

                                            addGraphItem.addClickHandler(new ClickHandler() {
                                                public void onClick(MenuItemClickEvent menuItemClickEvent) {
                                                    //generate javascript to call out to.
                                                    //Ex. menuLayers.hide();addMetric('${metric.resourceId},${metric.scheduleId}')
                                                    String grpGraphElements = "";
                                                    if (isAutoGroup) {
                                                        grpGraphElements += "ag,";
                                                    } else {
                                                        grpGraphElements += "cg,";
                                                    }
                                                    grpGraphElements += group.getId() + "," + def.getId();
                                                    if (isAutoGroup) {//need to postpend the resource type as third element
                                                        grpGraphElements += "," + group.getResourceType().getId();
                                                    }
                                                    //construct portal.war url to access
                                                    String baseUrl = "/resource/common/monitor/visibility/IndicatorCharts.do";
                                                    //No need to rebuild the autogroup url as everything handled as a compatible group now
                                                    //                                                if (isAutoGroup) {
                                                    //                                                    //Ex. ?parent=10001&ctype=1013&view=Default
                                                    //                                                    baseUrl += "?parent=" + group.getAutoGroupParentResource().getId()
                                                    //                                                        + "&ctype="
                                                    //                                                        + group.getResourceType().getId();
                                                    //                                                    jsCode = "ag," + group.getAutoGroupParentResource().getId() + ","
                                                    //                                                        + def.getId() + ","
                                                    //                                                        + group.getResourceType().getId();
                                                    //                                                } else {
                                                    //Ex. ?groupId=10001&view=Default
                                                    baseUrl += "?groupId=" + group.getId();
                                                    baseUrl += "&view=Default";
                                                    baseUrl += "&action=addChart&metric=" + grpGraphElements;
                                                    baseUrl += "&view=Default";
                                                    final String url = baseUrl;
                                                    //initiate HTTP request
                                                    final RequestBuilder b = new RequestBuilder(RequestBuilder.GET,
                                                        baseUrl);

                                                    try {
                                                        b.setCallback(new RequestCallback() {
                                                            public void onResponseReceived(final Request request,
                                                                final Response response) {
                                                                Log.trace("Successfully submitted request to add graph to view:"
                                                                    + url);

                                                                //kick off a page reload.
                                                                String currentViewPath = History.getToken();
                                                                CoreGUI.goToView(currentViewPath, true);
                                                            }

                                                            @Override
                                                            public void onError(Request request, Throwable t) {
                                                                Log.trace("Error adding Metric:" + url, t);
                                                            }
                                                        });
                                                        b.send();
                                                    } catch (RequestException e) {
                                                        Log.trace("Error adding Metric:" + url, e);
                                                    }
                                                }//end of onClick definition
                                            });//end of onClick Handler definition
                                        }//end of Monitoring/Graphs view check
                                    }//end of dashabord iteration
                                }//end of check for Measurement
                            }//end of metric definition iteration
                        }

                    }
                });
        }
        measurements.setSubmenu(measurementsSubMenu);
        return measurements;
    }

}
