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

import com.allen_sauer.gwt.log.client.Log;
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
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.criteria.ResourceTypeCriteria;
import org.rhq.core.domain.dashboard.Dashboard;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.resource.composite.ResourceLineageComposite;
import org.rhq.core.domain.resource.composite.ResourcePermission;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.Breadcrumb;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.ViewId;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.inventory.resource.graph.GraphPortlet;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGroupGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceTypeGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.inventory.InventoryView;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.ResourceGroupContextMenu;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.ResourceGroupDetailView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.ResourceTreeDatasource.AutoGroupTreeNode;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.ResourceTreeDatasource.ResourceTreeNode;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.operation.schedule.OperationCreateWizard;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.factory.ResourceFactoryCreateWizard;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.factory.ResourceFactoryImportWizard;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Greg Hinkle
 */
public class ResourceTreeView extends LocatableVLayout {

    private TreeGrid treeGrid;
    private String selectedNodeId;

    private Resource rootResource;

    private ViewId currentViewId;

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

        resourceContextMenu = new Menu();
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
                if (null != selectedNodeId) {
                    treeGrid.selectRecord(treeGrid.getTree().findById(selectedNodeId));
                }

                if (event.getNode() instanceof AutoGroupTreeNode) {
                    showContextMenu((AutoGroupTreeNode) event.getNode());
                } else if (event.getNode() instanceof ResourceTreeNode) {
                    if (!((ResourceTreeNode) event.getNode()).isLocked()) {
                        showContextMenu((ResourceTreeNode) event.getNode());
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
        String viewPath = ResourceGroupDetailView.AUTO_GROUP_VIEW_PATH + "/" + backingGroup.getId();
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

            // Update breadcrumbs
            if (currentViewId != null) {
                currentViewId.getBreadcrumbs().clear();
                if (null != parents) {
                    for (int i = parents.length - 1; i >= 0; i--) {
                        TreeNode n = parents[i];
                        adjustBreadcrumb(n, currentViewId);
                    }
                }
                adjustBreadcrumb(selectedNode, currentViewId);
                CoreGUI.refreshBreadCrumbTrail();
            }
        }
    }

    private void adjustBreadcrumb(TreeNode node, ViewId viewId) {
        if (node instanceof ResourceTreeNode) {
            Resource nr = ((ResourceTreeNode) node).getResource();
            String display = node.getName() + " <span class=\"subtitle\">" + nr.getResourceType().getName() + "</span>";
            String icon = ImageManager.getResourceIcon(nr.getResourceType().getCategory());

            viewId.getBreadcrumbs().add(new Breadcrumb(node.getAttribute("id"), display, icon, true));

        } else if (node instanceof AutoGroupTreeNode) {
            String name = ((AutoGroupTreeNode) node).getBackingGroupName();
            String display = node.getName() + " <span class=\"subtitle\">" + name + "</span>";
            String icon = ImageManager.getResourceIcon(((AutoGroupTreeNode) node).getResourceType().getCategory());

            viewId.getBreadcrumbs().add(new Breadcrumb(node.getAttribute("id"), display, icon, true));
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
        MenuItem editPluginConfiguration = new MenuItem(MSG.view_tabs_common_connectionSettings());
        editPluginConfiguration.addClickHandler(new ClickHandler() {
            public void onClick(MenuItemClickEvent event) {
                CoreGUI.goToView(LinkManager.getResourceTabLink(resource.getId(), "Inventory", "ConnectionSettings"));
            }
        });
        editPluginConfiguration.setEnabled(resourceType.getPluginConfigurationDefinition() != null);
        resourceContextMenu.addItem(editPluginConfiguration);

        // resource config
        MenuItem editResourceConfiguration = new MenuItem(MSG.view_tree_common_contextMenu_resourceConfiguration());
        boolean enabled = resourcePermission.isConfigureRead()
            && resourceType.getResourceConfigurationDefinition() != null;
        editResourceConfiguration.setEnabled(enabled);
        if (enabled) {
            editResourceConfiguration.addClickHandler(new ClickHandler() {
                public void onClick(MenuItemClickEvent event) {
                    CoreGUI.goToView(LinkManager.getResourceTabLink(resource.getId(), "Configuration", "Current"));
                }
            });
        }
        resourceContextMenu.addItem(editResourceConfiguration);

        // separator
        resourceContextMenu.addItem(new MenuItemSeparator());

        // Operations Menu
        MenuItem operations = new MenuItem(MSG.view_tree_common_contextMenu_operations());
        enabled = (resourcePermission.isControl() && !resourceType.getOperationDefinitions().isEmpty());
        operations.setEnabled(enabled);
        if (enabled) {
            Menu opSubMenu = new Menu();
            for (final OperationDefinition operationDefinition : resourceType.getOperationDefinitions()) {
                MenuItem operationItem = new MenuItem(operationDefinition.getDisplayName());
                operationItem.addClickHandler(new ClickHandler() {
                    public void onClick(MenuItemClickEvent event) {
                        int resourceId = ((ResourceTreeNode) treeGrid.getTree().findById(selectedNodeId)).getResource()
                            .getId();

                        ResourceCriteria criteria = new ResourceCriteria();
                        criteria.addFilterId(resourceId);

                        GWTServiceLookup.getResourceService().findResourcesByCriteria(criteria,
                            new AsyncCallback<PageList<Resource>>() {
                                public void onFailure(Throwable caught) {
                                    CoreGUI.getErrorHandler().handleError(
                                        MSG.view_tree_common_contextMenu_operations_loadFailed(), caught);
                                }

                                public void onSuccess(PageList<Resource> result) {
                                    new OperationCreateWizard(result.get(0), operationDefinition)
                                        .startOperationWizard();
                                }
                            });

                    }
                });
                opSubMenu.addItem(operationItem);
                // todo action
            }
            operations.setSubmenu(opSubMenu);
        }
        resourceContextMenu.addItem(operations);

        // Metric graph addition menu
        resourceContextMenu.addItem(buildMetricsMenu(resourceType));

        // Create Child Menu
        MenuItem createChildMenu = new MenuItem(MSG.common_button_create_child());
        enabled = resourcePermission.isCreateChildResources();
        if (enabled) {
            Menu createChildSubMenu = new Menu();
            for (final ResourceType childType : resourceType.getChildResourceTypes()) {
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
            enabled = createChildSubMenu.getItems().length > 0;
        }
        createChildMenu.setEnabled(enabled);
        resourceContextMenu.addItem(createChildMenu);

        // Manual Import Menu
        MenuItem importChildMenu = new MenuItem(MSG.common_button_import());
        enabled = resourcePermission.isCreateChildResources();
        if (enabled) {
            Menu importChildSubMenu = new Menu();
            for (final ResourceType childType : resourceType.getChildResourceTypes()) {
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
            enabled = importChildSubMenu.getItems().length > 0;
        }
        importChildMenu.setEnabled(enabled);
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
                for (final ResourceType type : result) {
                    if (type.getParentResourceTypes() == null || type.getParentResourceTypes().isEmpty()) {
                        MenuItem item = new MenuItem(type.getName());

                        item.addClickHandler(new ClickHandler() {
                            public void onClick(MenuItemClickEvent event) {
                                ResourceFactoryImportWizard.showImportWizard(resource, type);
                            }
                        });

                        manuallyAddMenu.addItem(item);
                    }
                }
            }
        });
    }

    private MenuItem buildMetricsMenu(final ResourceType type) {
        MenuItem measurements = new MenuItem(MSG.view_tree_common_contextMenu_measurements());
        final Menu measurementsSubMenu = new Menu();

        GWTServiceLookup.getDashboardService().findDashboardsForSubject(new AsyncCallback<List<Dashboard>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_tree_common_contextMenu_loadFailed_dashboard(), caught);
            }

            public void onSuccess(List<Dashboard> result) {

                for (final MeasurementDefinition def : type.getMetricDefinitions()) {

                    MenuItem defItem = new MenuItem(def.getDisplayName());
                    measurementsSubMenu.addItem(defItem);
                    Menu defSubItem = new Menu();
                    defItem.setSubmenu(defSubItem);

                    for (final Dashboard d : result) {
                        MenuItem addToDBItem = new MenuItem(MSG.view_tree_common_contextMenu_addChartToDashboard(d
                            .getName()));
                        defSubItem.addItem(addToDBItem);

                        addToDBItem.addClickHandler(new ClickHandler() {
                            public void onClick(MenuItemClickEvent menuItemClickEvent) {
                                int resourceId = ((ResourceTreeNode) treeGrid.getTree().findById(selectedNodeId))
                                    .getResource().getId();

                                DashboardPortlet p = new DashboardPortlet(def.getDisplayName() + " Chart",
                                    GraphPortlet.KEY, 250);
                                p.getConfiguration().put(new PropertySimple(GraphPortlet.CFG_RESOURCE_ID, resourceId));
                                p.getConfiguration().put(
                                    new PropertySimple(GraphPortlet.CFG_DEFINITION_ID, def.getId()));

                                d.addPortlet(p, 0, 0);

                                GWTServiceLookup.getDashboardService().storeDashboard(d,
                                    new AsyncCallback<Dashboard>() {
                                        public void onFailure(Throwable caught) {
                                            CoreGUI.getErrorHandler().handleError(
                                                MSG.view_tree_common_contextMenu_saveChartToDashboardFailure(), caught);
                                        }

                                        public void onSuccess(Dashboard result) {
                                            CoreGUI.getMessageCenter().notify(
                                                new Message(MSG
                                                    .view_tree_common_contextMenu_saveChartToDashboardSuccessful(result
                                                        .getName()), Message.Severity.Info));
                                        }
                                    });

                            }
                        });

                    }

                }

            }
        });
        measurements.setSubmenu(measurementsSubMenu);
        return measurements;
    }

    Resource getResource(int resourceId) {
        if (this.treeGrid != null && this.treeGrid.getTree() != null) {
            ResourceTreeNode treeNode = (ResourceTreeNode) this.treeGrid.getTree().findById(
                ResourceTreeNode.idOf(resourceId));
            if (treeNode != null) {
                return treeNode.getResource();
            }
        }
        return null;
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
            loadTree(selectedResourceId, null);
        }

    }

    private void loadTree(final int selectedResourceId, final AsyncCallback<Void> callback) {
        selectedNodeId = ResourceTreeNode.idOf(selectedResourceId);

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
                                updateSelection();

                                if (null != callback) {
                                    callback.onSuccess(null);
                                }
                            }
                        });

                        updateSelection();

                    } else {
                        ResourceTypeRepository.Cache.getInstance().loadResourceTypes(
                            lineage,
                            EnumSet.of(ResourceTypeRepository.MetadataType.operations,
                                ResourceTypeRepository.MetadataType.children,
                                ResourceTypeRepository.MetadataType.subCategory),
                            new ResourceTypeRepository.ResourceTypeLoadedCallback() {
                                public void onResourceTypeLoaded(List<Resource> result) {
                                    treeGrid.getTree()
                                        .linkNodes(ResourceTreeDatasource.buildNodes(lineage, lockedData));

                                    TreeNode selectedNode = treeGrid.getTree().findById(selectedNodeId);
                                    if (selectedNode != null) {
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
                    // load the tree up to the autogroup's parent resource                    
                    loadTree(backingGroup.getAutoGroupParentResource().getId(), new AsyncCallback<Void>() {

                        @Override
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError(MSG.view_tree_common_loadFailed_children(), caught);
                        }

                        @Override
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
        currentViewId = viewPath.getCurrent();
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
