/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.SelectionChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionEvent;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuItem;
import com.smartgwt.client.widgets.menu.MenuItemSeparator;
import com.smartgwt.client.widgets.menu.events.ClickHandler;
import com.smartgwt.client.widgets.menu.events.MenuItemClickEvent;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeNode;
import com.smartgwt.client.widgets.tree.events.DataArrivedEvent;
import com.smartgwt.client.widgets.tree.events.DataArrivedHandler;
import com.smartgwt.client.widgets.tree.events.NodeContextClickEvent;
import com.smartgwt.client.widgets.tree.events.NodeContextClickHandler;

import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.criteria.DashboardCriteria;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.criteria.ResourceTypeCriteria;
import org.rhq.core.domain.dashboard.Dashboard;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.resource.composite.ResourceLineageComposite;
import org.rhq.core.domain.resource.composite.ResourcePermission;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.ViewId;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.inventory.resource.graph.ResourceGraphPortlet;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGroupGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceTypeGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.inventory.InventoryView;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.ResourceGroupContextMenu;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.ResourceGroupDetailView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.ResourceTreeDatasource.AutoGroupTreeNode;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.ResourceTreeDatasource.ResourceTreeNode;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.factory.ResourceFactoryCreateWizard;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.factory.ResourceFactoryImportWizard;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableMenu;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Greg Hinkle
 */
public class ResourceTreeView extends LocatableVLayout {

    private TreeGrid treeGrid;
    private String selectedNodeId;

    private Resource rootResource;

    private Menu resourceContextMenu;
    private ResourceGroupContextMenu autoGroupContextMenu;

    // Maps autogroup/type backing group ids to the corresponding autogroup/type nodes.
    private Map<Integer, AutoGroupTreeNode> autoGroupNodeMap = new HashMap<Integer, AutoGroupTreeNode>();

    public ResourceTreeView(String locatorId) {
        super(locatorId);

        setWidth("250");
        setHeight100();

        setShowResizeBar(true);
    }

    @Override
    public void onInit() {
        // TODO (ips): Are we intentionally avoiding calling super.onInit() here? If so, why?
    }

    private void buildTree() {

        treeGrid = new CustomResourceTreeGrid(getLocatorId());

        treeGrid.setOpenerImage("resources/dir.png");
        treeGrid.setOpenerIconSize(16);

        // don't auto-fetch data, the initial fetch is requested manually using initial lineage information
        treeGrid.setAutoFetchData(false);

        treeGrid.setAnimateFolders(false);
        treeGrid.setSelectionType(SelectionStyle.SINGLE);
        treeGrid.setShowRollOver(false);
        treeGrid.setSortField("name");
        treeGrid.setShowHeader(false);

        treeGrid.setLeaveScrollbarGap(false);

        resourceContextMenu = new LocatableMenu(extendLocatorId("resourceContextMenu"));
        autoGroupContextMenu = new ResourceGroupContextMenu(extendLocatorId("autoGroupContextMenu"));

        treeGrid.addSelectionChangedHandler(new SelectionChangedHandler() {

            public void onSelectionChanged(SelectionEvent selectionEvent) {
                if (!selectionEvent.isRightButtonDown() && selectionEvent.getState()) {
                    ListGridRecord selectedRecord = treeGrid.getSelectedRecord();
                    if (selectedRecord instanceof ResourceTreeNode) {
                        ResourceTreeNode resourceNode = (ResourceTreeNode) selectedRecord;

                        if (!resourceNode.isLocked()) {
                            Log.info("Resource Node selected in tree: " + selectedRecord);

                            selectedNodeId = resourceNode.getID();
                            String viewPath = "Resource/" + resourceNode.getResource().getId();
                            String currentViewPath = History.getToken();
                            if (!currentViewPath.startsWith(viewPath)) {
                                CoreGUI.goToView(viewPath);
                            }
                        } else {
                            treeGrid.deselectRecord(resourceNode);
                            if (null != selectedNodeId) {
                                treeGrid.selectRecord(treeGrid.getTree().findById(selectedNodeId));
                            }
                        }
                    } else if (selectedRecord instanceof AutoGroupTreeNode) {
                        Log.info("AutoGroup Node selected in tree: " + selectedRecord);

                        AutoGroupTreeNode agNode = (AutoGroupTreeNode) selectedRecord;
                        selectedNodeId = agNode.getID();
                        getAutoGroupBackingGroup(agNode, new AsyncCallback<ResourceGroup>() {

                            public void onFailure(Throwable caught) {
                                CoreGUI.getErrorHandler().handleError(MSG.view_tree_common_loadFailed_selection(),
                                    caught);
                            }

                            public void onSuccess(ResourceGroup result) {
                                renderAutoGroup(result);
                            }
                        });
                    } else {
                        // TODO: probably clicked on a subcategory, do we need a message?
                        treeGrid.deselectRecord(selectedRecord);
                        if (null != selectedNodeId) {
                            treeGrid.selectRecord(treeGrid.getTree().findById(selectedNodeId));
                        }
                    }
                }
            }
        });

        // This constructs the context menu for the resource at the time of the click.
        // setContextMenu(resourceContextMenu);

        treeGrid.addNodeContextClickHandler(new NodeContextClickHandler() {

            public void onNodeContextClick(final NodeContextClickEvent event) {
                // stop the browser right-click menu
                event.cancel();

                // don't select the node on a right click, since we're not navigating to it
                treeGrid.deselectRecord(event.getNode());
                TreeNode eventNode = event.getNode();

                // re-select the current node if necessary
                if (null != selectedNodeId) {
                    TreeNode selectedNode = treeGrid.getTree().findById(selectedNodeId);
                    if (!eventNode.equals(selectedNode)) {
                        treeGrid.selectRecord(selectedNode);
                    }
                }

                if (eventNode instanceof AutoGroupTreeNode) {
                    showContextMenu((AutoGroupTreeNode) eventNode);
                } else if (eventNode instanceof ResourceTreeNode) {
                    if (!((ResourceTreeNode) eventNode).isLocked()) {
                        showContextMenu((ResourceTreeNode) eventNode);
                    }
                }
            }
        });

        treeGrid.addDataArrivedHandler(new DataArrivedHandler() {

            public void onDataArrived(DataArrivedEvent dataArrivedEvent) {
                updateSelection();
            }
        });
    }

    private void getAutoGroupBackingGroup(final AutoGroupTreeNode agNode, final AsyncCallback<ResourceGroup> callback) {
        final ResourceGroupGWTServiceAsync resourceGroupService = GWTServiceLookup.getResourceGroupService();

        // get the children tree nodes and build a child resourceId array 
        TreeNode[] children = treeGrid.getTree().getChildren(agNode);
        final int[] childIds = new int[children.length];
        for (int i = 0, size = children.length; (i < size); ++i) {
            childIds[i] = ((ResourceTreeNode) children[i]).getResource().getId();
        }

        // get the backing group if it exists, otherwise create the group
        ResourceGroupCriteria criteria = new ResourceGroupCriteria();
        criteria.addFilterPrivate(true);
        criteria.addFilterResourceTypeId(agNode.getResourceType().getId());
        criteria.addFilterAutoGroupParentResourceId(agNode.getParentResource().getId());
        criteria.addFilterVisible(false);
        resourceGroupService.findResourceGroupsByCriteria(criteria, new AsyncCallback<PageList<ResourceGroup>>() {

            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_tree_common_loadFailed_node(), caught);
            }

            public void onSuccess(PageList<ResourceGroup> result) {
                if (result.isEmpty()) {
                    // Not found, create new backing group
                    // the backing group name is a display name using a unique parentResource-resourceType combo 
                    final String backingGroupName = agNode.getBackingGroupName();
                    ResourceGroup backingGroup = new ResourceGroup(backingGroupName);
                    backingGroup.setAutoGroupParentResource(agNode.getParentResource());
                    backingGroup.setResourceType(agNode.getResourceType());
                    backingGroup.setVisible(false);
                    resourceGroupService.createPrivateResourceGroup(backingGroup, childIds,
                        new AsyncCallback<ResourceGroup>() {
                            public void onFailure(Throwable caught) {
                                CoreGUI.getErrorHandler().handleError(MSG.view_tree_common_loadFailed_create(), caught);
                            }

                            public void onSuccess(ResourceGroup result) {
                                // store a map entry from backingGroupId to AGTreeNode so we can easily
                                // get back to this node given the id of the backing group (from the viewpath)
                                autoGroupNodeMap.put(result.getId(), agNode);
                                callback.onSuccess(result);
                            }
                        });
                } else {
                    // backing group found
                    final ResourceGroup backingGroup = result.get(0);

                    // store a map entry from backingGroupId to AGTreeNode so we can easily
                    // get back to this node given the id of the backing group (from the viewpath)
                    autoGroupNodeMap.put(backingGroup.getId(), agNode);

                    // make sure the members are correct before rendering
                    resourceGroupService.setAssignedResources(backingGroup.getId(), childIds, false,
                        new AsyncCallback<Void>() {
                            public void onFailure(Throwable caught) {
                                CoreGUI.getErrorHandler().handleError(MSG.view_tree_common_loadFailed_update(), caught);
                            }

                            public void onSuccess(Void result) {
                                callback.onSuccess(backingGroup);
                            }
                        });
                }
            }
        });
    }

    private void renderAutoGroup(ResourceGroup backingGroup) {
        String viewPath = ResourceGroupDetailView.AUTO_GROUP_VIEW + "/" + backingGroup.getId();
        String currentViewPath = History.getToken();
        if (!currentViewPath.startsWith(viewPath)) {
            CoreGUI.goToView(viewPath);
        }
    }

    private void updateSelection() {

        TreeNode selectedNode;

        if (treeGrid != null && treeGrid.getTree() != null
            && (selectedNode = treeGrid.getTree().findById(selectedNodeId)) != null) {

            TreeNode[] parents = treeGrid.getTree().getParents(selectedNode);
            treeGrid.getTree().openFolders(parents);
            treeGrid.getTree().openFolder(selectedNode);

            if (!selectedNode.equals(treeGrid.getSelectedRecord())) {
                treeGrid.deselectAllRecords();
                treeGrid.selectRecord(selectedNode);
            }

            treeGrid.markForRedraw();
        }
    }

    private void showContextMenu(AutoGroupTreeNode agNode) {
        getAutoGroupBackingGroup(agNode, new AsyncCallback<ResourceGroup>() {

            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_tree_common_loadFailed_selection(), caught);
            }

            public void onSuccess(ResourceGroup result) {
                autoGroupContextMenu.showContextMenu(result);
            }
        });
    }

    private void showContextMenu(final ResourceTreeNode node) {
        final Resource resource = node.getResource();
        final int resourceId = resource.getId();

        // fetch the resource composite, we need resource permission info for enablement decisions
        ResourceCriteria criteria = new ResourceCriteria();
        criteria.addFilterId(resourceId);
        criteria.fetchSchedules(true);
        GWTServiceLookup.getResourceService().findResourceCompositesByCriteria(criteria,
            new AsyncCallback<PageList<ResourceComposite>>() {

                public void onFailure(Throwable caught) {
                    CoreGUI.getMessageCenter().notify(
                        new Message(MSG.view_inventory_resource_loadFailed(String.valueOf(resourceId)),
                            Message.Severity.Warning));

                    CoreGUI.goToView(InventoryView.VIEW_ID.getName());
                }

                public void onSuccess(PageList<ResourceComposite> result) {
                    if (result.isEmpty()) {
                        onFailure(new Exception(MSG.view_inventory_resource_loadFailed(String.valueOf(resourceId))));
                    } else {
                        final ResourceComposite resourceComposite = result.get(0);

                        // make sure we have all the Type information necessary to render the menus
                        ResourceType type = resource.getResourceType();
                        ResourceTypeRepository.Cache.getInstance().getResourceTypes(
                            type.getId(),
                            EnumSet.of(ResourceTypeRepository.MetadataType.operations,
                                ResourceTypeRepository.MetadataType.children,
                                ResourceTypeRepository.MetadataType.subCategory,
                                ResourceTypeRepository.MetadataType.pluginConfigurationDefinition,
                                ResourceTypeRepository.MetadataType.resourceConfigurationDefinition,
                                ResourceTypeRepository.MetadataType.measurements),
                            new ResourceTypeRepository.TypeLoadedCallback() {

                                public void onTypesLoaded(ResourceType type) {
                                    buildResourceContextMenu(resourceComposite, type);
                                    resourceContextMenu.showContextMenu();
                                }
                            });
                    }
                }
            });
    }

    private void buildResourceContextMenu(final ResourceComposite resourceComposite, final ResourceType resourceType) {
        final Resource resource = resourceComposite.getResource();
        final ResourcePermission resourcePermission = resourceComposite.getResourcePermission();

        // resource name
        resourceContextMenu.setItems(new MenuItem(resource.getName()));

        // resource type name
        resourceContextMenu.addItem(new MenuItem(MSG.view_tree_common_contextMenu_type_name_label(resourceType
            .getName())));

        // separator
        resourceContextMenu.addItem(new MenuItemSeparator());

        // plugin config
        MenuItem pluginConfiguration = new MenuItem(MSG.view_tabs_common_connectionSettings());
        boolean pluginConfigEnabled = resourceType.getPluginConfigurationDefinition() != null;
        pluginConfiguration.setEnabled(pluginConfigEnabled);
        if (pluginConfigEnabled) {
            pluginConfiguration.addClickHandler(new ClickHandler() {

                public void onClick(MenuItemClickEvent event) {
                    CoreGUI.goToView(LinkManager
                        .getResourceTabLink(resource.getId(), "Inventory", "ConnectionSettings"));
                }
            });
        }
        resourceContextMenu.addItem(pluginConfiguration);

        // resource config
        MenuItem resourceConfiguration = new MenuItem(MSG.view_tree_common_contextMenu_resourceConfiguration());
        boolean resourceConfigEnabled = resourcePermission.isConfigureRead()
            && resourceType.getResourceConfigurationDefinition() != null;
        resourceConfiguration.setEnabled(resourceConfigEnabled);
        if (resourceConfigEnabled) {
            resourceConfiguration.addClickHandler(new ClickHandler() {

                public void onClick(MenuItemClickEvent event) {
                    CoreGUI.goToView(LinkManager.getResourceTabLink(resource.getId(), "Configuration", "Current"));
                }
            });
        }
        resourceContextMenu.addItem(resourceConfiguration);

        // separator
        resourceContextMenu.addItem(new MenuItemSeparator());

        // Operations Menu
        MenuItem operations = new MenuItem(MSG.view_tree_common_contextMenu_operations());
        boolean operationsEnabled = (resourcePermission.isControl() && (resourceType.getOperationDefinitions() != null) && !resourceType
            .getOperationDefinitions().isEmpty());
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
                        String viewPath = LinkManager.getResourceTabLink(resource.getId(),
                            ResourceDetailView.Tab.OPERATIONS, ResourceDetailView.OperationsSubTab.SCHEDULES)
                            + "/0/" + operationDefinition.getId();
                        CoreGUI.goToView(viewPath);
                    }
                });
                opSubMenu.addItem(operationItem);
            }
            operations.setSubmenu(opSubMenu);
        }
        resourceContextMenu.addItem(operations);

        // Metric graph addition menu
        resourceContextMenu.addItem(buildMetricsMenu(resourceType, resource));

        // Create Child Menu
        MenuItem createChildMenu = new MenuItem(MSG.common_button_create_child());
        boolean createChildResourcesEnabled = resourcePermission.isCreateChildResources();
        if (createChildResourcesEnabled) {
            Menu createChildSubMenu = new Menu();
            //sort the display items alphabetically
            TreeSet<String> ordered = new TreeSet<String>();
            Map<String, ResourceType> typeMap = new HashMap<String, ResourceType>();
            for (ResourceType o : resourceType.getChildResourceTypes()) {
                ordered.add(o.getName());
                typeMap.put(o.getName(), o);
            }

            for (String type : ordered) {
                final ResourceType childType = typeMap.get(type);
                if (childType.isCreatable()) {
                    MenuItem createItem = new MenuItem(childType.getName());

                    createItem.addClickHandler(new ClickHandler() {

                        public void onClick(MenuItemClickEvent event) {
                            ResourceFactoryCreateWizard.showCreateWizard(resource, childType);
                        }
                    });

                    createChildSubMenu.addItem(createItem);

                }
            }
            createChildMenu.setSubmenu(createChildSubMenu);
            createChildResourcesEnabled = createChildSubMenu.getItems().length > 0;
        }
        createChildMenu.setEnabled(createChildResourcesEnabled);
        resourceContextMenu.addItem(createChildMenu);

        // Manual Import Menu
        MenuItem importChildMenu = new MenuItem(MSG.common_button_import());
        boolean manualImportEnabled = resourcePermission.isCreateChildResources();
        if (manualImportEnabled) {
            Menu importChildSubMenu = new Menu();

            //sort the display items alphabetically
            TreeSet<String> ordered = new TreeSet<String>();
            Map<String, ResourceType> typeMap = new HashMap<String, ResourceType>();
            for (ResourceType o : resourceType.getChildResourceTypes()) {
                ordered.add(o.getName());
                typeMap.put(o.getName(), o);
            }
            for (String name : ordered) {
                final ResourceType childType = typeMap.get(name);
                if (childType.isSupportsManualAdd()) {
                    MenuItem importItem = new MenuItem(childType.getName());

                    importItem.addClickHandler(new ClickHandler() {

                        public void onClick(MenuItemClickEvent event) {
                            ResourceFactoryImportWizard.showImportWizard(resource, childType);
                        }
                    });

                    importChildSubMenu.addItem(importItem);
                }
            }
            if (resourceType.getCategory() == ResourceCategory.PLATFORM) {
                loadManuallyAddServersToPlatforms(importChildSubMenu, resource);
            }

            importChildMenu.setSubmenu(importChildSubMenu);
            manualImportEnabled = importChildSubMenu.getItems().length > 0;
        }
        importChildMenu.setEnabled(manualImportEnabled);
        resourceContextMenu.addItem(importChildMenu);
    }

    private void loadManuallyAddServersToPlatforms(final Menu manuallyAddMenu, final Resource resource) {
        ResourceTypeGWTServiceAsync rts = GWTServiceLookup.getResourceTypeGWTService();

        ResourceTypeCriteria criteria = new ResourceTypeCriteria();
        criteria.addFilterSupportsManualAdd(true);
        criteria.fetchParentResourceTypes(true);
        rts.findResourceTypesByCriteria(criteria, new AsyncCallback<PageList<ResourceType>>() {

            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_tree_common_contextMenu_loadFailed_manualAddChildren(),
                    caught);
            }

            public void onSuccess(PageList<ResourceType> result) {
                //sort the display items alphabetically
                TreeSet<String> ordered = new TreeSet<String>();
                Map<String, ResourceType> displayTypes = new HashMap<String, ResourceType>();
                for (ResourceType type : result) {
                    displayTypes.put(type.getName(), type);
                    ordered.add(type.getName());
                }

                int idx = 0;
                for (String displayType : ordered) {
                    final ResourceType type = displayTypes.get(displayType);
                    if (type.getParentResourceTypes() == null || type.getParentResourceTypes().isEmpty()) {
                        MenuItem item = new MenuItem(type.getName());

                        item.addClickHandler(new ClickHandler() {

                            public void onClick(MenuItemClickEvent event) {
                                ResourceFactoryImportWizard.showImportWizard(resource, type);
                            }
                        });

                        manuallyAddMenu.addItem(item, idx++);
                    }
                }
            }
        });
    }

    private MenuItem buildMetricsMenu(final ResourceType type, final Resource resource) {
        MenuItem measurements = new MenuItem(MSG.view_tree_common_contextMenu_measurements());
        final Menu measurementsSubMenu = new Menu();

        DashboardCriteria criteria = new DashboardCriteria();
        GWTServiceLookup.getDashboardService().findDashboardsByCriteria(criteria,
            new AsyncCallback<PageList<Dashboard>>() {

                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_tree_common_contextMenu_loadFailed_dashboard(),
                        caught);
                }

                public void onSuccess(PageList<Dashboard> result) {
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
                                            .view_tree_common_contextMenu_resourceGraph(), ResourceGraphPortlet.KEY,
                                            250);
                                        p.getConfiguration().put(
                                            new PropertySimple(ResourceGraphPortlet.CFG_RESOURCE_ID, resource.getId()));
                                        p.getConfiguration().put(
                                            new PropertySimple(ResourceGraphPortlet.CFG_DEFINITION_ID, def.getId()));

                                        d.addPortlet(p);

                                        GWTServiceLookup.getDashboardService().storeDashboard(d,
                                            new AsyncCallback<Dashboard>() {

                                                public void onFailure(Throwable caught) {
                                                    CoreGUI.getErrorHandler().handleError(
                                                        MSG.view_tree_common_contextMenu_saveChartToDashboardFailure(),
                                                        caught);
                                                }

                                                public void onSuccess(Dashboard result) {
                                                    CoreGUI
                                                        .getMessageCenter()
                                                        .notify(
                                                            new Message(
                                                                MSG
                                                                    .view_tree_common_contextMenu_saveChartToDashboardSuccessful(result
                                                                        .getName()), Message.Severity.Info));
                                                }
                                            });

                                    }
                                });

                                //add new menu item for adding current graphable element to view if on Monitor/Graphs tab
                                String currentViewPath = History.getToken();
                                if (currentViewPath.indexOf("Monitoring/Graphs") > -1) {
                                    MenuItem addGraphItem = new MenuItem(MSG.common_title_add_graph_to_view());
                                    defSubItem.addItem(addGraphItem);

                                    addGraphItem.addClickHandler(new ClickHandler() {
                                        public void onClick(MenuItemClickEvent menuItemClickEvent) {
                                            //generate javascript to call out to.
                                            //Ex. menuLayers.hide();addMetric('${metric.resourceId},${metric.scheduleId}')
                                            if (getScheduleDefinitionId(resource, def.getName()) > -1) {
                                                String resourceGraphElements = resource.getId() + ","
                                                    + getScheduleDefinitionId(resource, def.getName());

                                                //construct portal.war url to access
                                                String baseUrl = "/resource/common/monitor/visibility/IndicatorCharts.do";
                                                baseUrl += "?id=" + resource.getId();
                                                baseUrl += "&view=Default";
                                                baseUrl += "&action=addChart&metric=" + resourceGraphElements;
                                                baseUrl += "&view=Default";
                                                final String url = baseUrl;
                                                //initiate HTTP request
                                                final RequestBuilder b = new RequestBuilder(RequestBuilder.GET, baseUrl);

                                                try {
                                                    b.setCallback(new RequestCallback() {
                                                        public void onResponseReceived(final Request request,
                                                            final Response response) {
                                                            Log
                                                                .trace("Successfully submitted request to add graph to view:"
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
                                            }
                                        }
                                    });
                                }
                                //                            }//end trait check
                            }//end dashboard iteration
                        }//end trait exclusion
                    }//end measurement def iteration

                }
            });
        measurements.setSubmenu(measurementsSubMenu);
        return measurements;
    }

    /** Locate the specific schedule definition using the definition identifier.
     */
    private int getScheduleDefinitionId(Resource resource, String definitionName) {
        int id = -1;
        if (resource.getSchedules() != null) {
            boolean located = false;
            MeasurementSchedule[] schedules = new MeasurementSchedule[resource.getSchedules().size()];
            resource.getSchedules().toArray(schedules);
            for (int i = 0; (!located && i < resource.getSchedules().size()); i++) {
                MeasurementSchedule schedule = schedules[i];
                MeasurementDefinition definition = schedule.getDefinition();
                if ((definition != null) && definition.getName().equals(definitionName)) {
                    located = true;
                    id = schedule.getId();
                }
            }
        }
        return id;
    }

    private void setRootResource(Resource rootResource) {
        this.rootResource = rootResource;
    }

    public void setSelectedResource(final int selectedResourceId) {

        selectedNodeId = ResourceTreeNode.idOf(selectedResourceId);

        if (treeGrid != null && treeGrid.getTree() != null && (treeGrid.getTree().findById(selectedNodeId)) != null) {
            // This is the case where the tree was previously loaded and we get fired to look at a different
            // node in the same tree and just have to switch the selection            
            updateSelection();

        } else {
            // This is for cases where we have to load the tree fresh including down to the currently visible node
            loadTree(selectedResourceId, true, null);
        }

    }

    private void loadTree(final int selectedResourceId, final boolean updateSelection,
        final AsyncCallback<Void> callback) {

        if (updateSelection) {
            selectedNodeId = ResourceTreeNode.idOf(selectedResourceId);
        }

        final ResourceGWTServiceAsync resourceService = GWTServiceLookup.getResourceService();

        // This is an expensive call, but loads all nodes that are visible in the tree given a selected resource
        resourceService.getResourceLineageAndSiblings(selectedResourceId,
            new AsyncCallback<List<ResourceLineageComposite>>() {

                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_tree_common_loadFailed_root(), caught);
                }

                public void onSuccess(List<ResourceLineageComposite> result) {
                    Resource root = result.get(0).getResource();

                    final List<Resource> lineage = new ArrayList<Resource>(result.size());
                    final List<Resource> lockedData = new ArrayList<Resource>();

                    for (ResourceLineageComposite r : result) {
                        lineage.add(r.getResource());
                        if (r.isLocked()) {
                            lockedData.add(r.getResource());
                        }
                    }

                    if (!root.equals(ResourceTreeView.this.rootResource)) {

                        if (treeGrid != null) {
                            treeGrid.destroy();
                        }
                        buildTree();

                        setRootResource(root);

                        // seed datasource with initial resource list and which ancestor resources are locked 
                        ResourceTreeDatasource dataSource = new ResourceTreeDatasource(lineage, lockedData);
                        treeGrid.setDataSource(dataSource);

                        addMember(treeGrid);

                        treeGrid.fetchData(treeGrid.getCriteria(), new DSCallback() {

                            public void execute(DSResponse response, Object rawData, DSRequest request) {
                                Log.info("Done fetching data for tree.");

                                if (updateSelection) {
                                    updateSelection();
                                }

                                if (null != callback) {
                                    callback.onSuccess(null);
                                }
                            }
                        });

                        // OK, there is no good reason for this to be here.  But there are times when the
                        // callback above seems to get called prior to the treeGrid.getTree() having been
                        // updated with the fetched data. I think this is a smartgwt bug but it's hard to
                        // prove.  Furthermore, given that the fetchData call is async there is really no
                        // reason why this should get called after the callback above. Having said all that,
                        // this seems to fix the issue as it 1) does currently get called after the callback
                        // and 2) the tree seems to be update immediately after the callback completes.
                        // So, for now use this, but TODO: find a better way.
                        if (updateSelection) {
                            updateSelection();
                        }

                    } else {
                        ResourceTypeRepository.Cache.getInstance().loadResourceTypes(lineage,
                            EnumSet.of(ResourceTypeRepository.MetadataType.subCategory),
                            new ResourceTypeRepository.ResourceTypeLoadedCallback() {

                                public void onResourceTypeLoaded(List<Resource> result) {
                                    treeGrid.getTree()
                                        .linkNodes(ResourceTreeDatasource.buildNodes(lineage, lockedData));

                                    TreeNode selectedNode = treeGrid.getTree().findById(selectedNodeId);
                                    if (selectedNode != null && updateSelection) {
                                        updateSelection();

                                    } else {
                                        CoreGUI.getMessageCenter().notify(
                                            new Message(MSG.view_tree_common_loadFailed_selection(),
                                                Message.Severity.Warning));
                                    }

                                }
                            });
                    }
                }
            });
    }

    public void setSelectedAutoGroup(final Integer selectedAutoGroupId) {

        AutoGroupTreeNode selectedNode = autoGroupNodeMap.get(selectedAutoGroupId);
        if (treeGrid != null && treeGrid.getTree() != null && selectedNode != null) {
            // This is the case where the tree was previously loaded and we get fired to look at a different
            // node in the same tree and just have to switch the selection

            this.selectedNodeId = selectedNode.getID();
            updateSelection();

        } else {
            // This is for cases where we have to load the tree fresh including down to the currently visible node

            final ResourceGroupGWTServiceAsync resourceGroupService = GWTServiceLookup.getResourceGroupService();
            ResourceGroupCriteria criteria = new ResourceGroupCriteria();
            criteria.addFilterId(selectedAutoGroupId);
            criteria.addFilterVisible(false);
            criteria.fetchResourceType(true);
            resourceGroupService.findResourceGroupsByCriteria(criteria, new AsyncCallback<PageList<ResourceGroup>>() {

                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_tree_common_loadFailed_node(), caught);
                }

                public void onSuccess(PageList<ResourceGroup> result) {
                    final ResourceGroup backingGroup = result.get(0);
                    // load the tree up to the autogroup's parent resource. Don't select the resource node
                    // to avoid an unnecessary navigation to the resource, we just need the tree in place so
                    // we can navigate to the autogroup node.
                    loadTree(backingGroup.getAutoGroupParentResource().getId(), false, new AsyncCallback<Void>() {

                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError(MSG.view_tree_common_loadFailed_children(), caught);
                        }

                        public void onSuccess(Void arg) {
                            // get the node ID and use it to add a map entry, then call this again to finish up...
                            selectedNodeId = AutoGroupTreeNode.idOf(backingGroup.getAutoGroupParentResource(),
                                backingGroup.getResourceType());
                            AutoGroupTreeNode agNode = (AutoGroupTreeNode) treeGrid.getTree().findById(selectedNodeId);
                            autoGroupNodeMap.put(backingGroup.getId(), agNode);
                            updateSelection();
                        }
                    });
                }
            });
        }
    }

    /*private List<Resource> preload(final List<Resource> lineage) {

            final ArrayList<Resource> list = new ArrayList<Resource>(lineage);

            ResourceGWTServiceAsync resourceService = ResourceGWTServiceAsync.Util.getInstance();

                ResourceCriteria c = new ResourceCriteria();
                c.addFilterParentResourceId(lineage.get(0).getId());
                resourceService.findResourcesByCriteria(CoreGUI.getSessionSubject(), c, new AsyncCallback<PageList<Resource>>() {
                    public void onFailure(Throwable caught) {
                        SC.say("NotGood");
                    }

                    public void onSuccess(PageList<Resource> result) {
                        SC.say("GotONE");

                        if (lineage.size() > 1) {
                             result.addAll(preload(lineage.subList(1, lineage.size())));
                        }
                    }
                });
            }
        }
    */

    public void renderView(ViewPath viewPath) {
        ViewId currentViewId = viewPath.getCurrent();
        String currentViewIdPath = currentViewId.getPath();
        if ("AutoGroup".equals(currentViewIdPath)) {
            // Move the currentViewId to the ID portion to play better with other code
            currentViewId = viewPath.getNext();
            String autoGroupIdString = currentViewId.getPath();
            Integer autoGroupId = Integer.parseInt(autoGroupIdString);
            setSelectedAutoGroup(autoGroupId);
        } else {
            String resourceIdString = currentViewId.getPath();
            Integer resourceId = Integer.parseInt(resourceIdString);
            setSelectedResource(resourceId);
        }
    }

}
