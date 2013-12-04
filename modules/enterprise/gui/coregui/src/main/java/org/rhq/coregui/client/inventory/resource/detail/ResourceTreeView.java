/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.coregui.client.inventory.resource.detail;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.SelectionChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionEvent;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuItem;
import com.smartgwt.client.widgets.menu.MenuItemSeparator;
import com.smartgwt.client.widgets.menu.events.ClickHandler;
import com.smartgwt.client.widgets.menu.events.MenuItemClickEvent;
import com.smartgwt.client.widgets.tree.Tree;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeNode;
import com.smartgwt.client.widgets.tree.events.DataArrivedEvent;
import com.smartgwt.client.widgets.tree.events.DataArrivedHandler;
import com.smartgwt.client.widgets.tree.events.NodeContextClickEvent;
import com.smartgwt.client.widgets.tree.events.NodeContextClickHandler;

import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.resource.composite.ResourceLineageComposite;
import org.rhq.core.domain.resource.composite.ResourcePermission;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.ImageManager;
import org.rhq.coregui.client.LinkManager;
import org.rhq.coregui.client.ViewId;
import org.rhq.coregui.client.ViewPath;
import org.rhq.coregui.client.components.tree.EnhancedTreeNode;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.gwt.ResourceGWTServiceAsync;
import org.rhq.coregui.client.gwt.ResourceGroupGWTServiceAsync;
import org.rhq.coregui.client.inventory.InventoryView;
import org.rhq.coregui.client.inventory.groups.detail.ResourceGroupContextMenu;
import org.rhq.coregui.client.inventory.groups.detail.ResourceGroupDetailView;
import org.rhq.coregui.client.inventory.resource.detail.ResourceTreeDatasource.AutoGroupTreeNode;
import org.rhq.coregui.client.inventory.resource.detail.ResourceTreeDatasource.ResourceTreeNode;
import org.rhq.coregui.client.inventory.resource.detail.ResourceTreeDatasource.SubCategoryTreeNode;
import org.rhq.coregui.client.inventory.resource.factory.ResourceFactoryCreateWizard;
import org.rhq.coregui.client.inventory.resource.factory.ResourceFactoryImportWizard;
import org.rhq.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.coregui.client.util.Log;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;
import org.rhq.coregui.client.util.message.Message;

/**
 * @author Jay Shaughnessy
 * @author Greg Hinkle
 */
public class ResourceTreeView extends EnhancedVLayout {

    private TreeGrid treeGrid;
    private String selectedNodeId;
    private Label loadingLabel;

    private Resource rootResource;

    private Menu resourceContextMenu;
    private ResourceGroupContextMenu autoGroupContextMenu;

    // Maps autogroup/type backing group ids to the corresponding autogroup/type nodes.
    private Map<Integer, AutoGroupTreeNode> autoGroupNodeMap = new HashMap<Integer, AutoGroupTreeNode>();

    public ResourceTreeView() {
        super();

        setWidth("250");
        setHeight100();

        setShowResizeBar(true);
    }

    @Override
    public void onInit() {
        // manually handle a loading... message at initial load and also subsequent fetches
        loadingLabel = new Label(MSG.common_msg_loading());
        loadingLabel.setIcon(ImageManager.getLoadingIcon());
        loadingLabel.setHeight(20);
        loadingLabel.hide();
        addMember(loadingLabel);
    }

    private void buildTree() {
        treeGrid = new CustomResourceTreeGrid();

        treeGrid.setOpenerImage("resources/dir.png");
        treeGrid.setOpenerIconSize(16);

        // don't auto-fetch data, the initial fetch is requested manually using initial lineage information
        treeGrid.setAutoFetchData(false);

        treeGrid.setAnimateFolders(false);
        treeGrid.setSelectionType(SelectionStyle.SINGLE);
        treeGrid.setShowRollOver(false);
        treeGrid.setSortField("name");
        treeGrid.setShowHeader(false);
        // disable what the tree grid may do, defer to loadingLabel to handle all of our cases 
        treeGrid.setLoadingDataMessage(null);

        treeGrid.setLeaveScrollbarGap(false);

        resourceContextMenu = new Menu();
        autoGroupContextMenu = new ResourceGroupContextMenu();

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

                        // [BZ 827203] Disable this view to prevent fast-click issues. It will get re-enabled when
                        //             the detail resource or group view is done with its async init and calls
                        //             notifyViewRenderedListeners() on itself.
                        disable();

                        try {
                            loadingLabel.show();
                            getAutoGroupBackingGroup(agNode, new AsyncCallback<ResourceGroup>() {
                                public void onSuccess(ResourceGroup result) {
                                    loadingLabel.hide();
                                    renderAutoGroup(result);
                                    // Make sure to re-enable ourselves.
                                    enable();
                                }

                                public void onFailure(Throwable caught) {
                                    loadingLabel.hide();
                                    // Make sure to re-enable ourselves.
                                    enable();
                                    CoreGUI.getErrorHandler().handleError(MSG.view_tree_common_loadFailed_selection(),
                                        caught);
                                }
                            });
                        } catch (RuntimeException re) {
                            loadingLabel.hide();
                            // Make sure to re-enable ourselves.
                            enable();
                            CoreGUI.getErrorHandler().handleError(MSG.view_tree_common_loadFailed_selection(), re);
                        }
                    } else if (selectedRecord instanceof SubCategoryTreeNode) {
                        treeGrid.deselectRecord(selectedRecord);
                        treeGrid.getTree().openFolder((TreeNode) selectedRecord);
                        if (null != selectedNodeId) {
                            treeGrid.selectRecord(treeGrid.getTree().findById(selectedNodeId));
                        }
                    } else if (null != selectedRecord) {
                        // TODO: Should we thrown an exception, don't know what this is
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
                TreeNode parent = dataArrivedEvent.getParentNode();

                // always expand the parent when new data arrives underneath, it means the user has expanded the node
                if (parent instanceof EnhancedTreeNode) {
                    treeGrid.getTree().openFolder(parent);
                    treeGrid.markForRedraw();
                }

                // if for some reason expansion has caused our selection to be lost then make sure it is again properly selected
                ListGridRecord selectedRecord = treeGrid.getSelectedRecord();
                TreeNode selectedNode = null != selectedNodeId ? treeGrid.getTree().findById(selectedNodeId) : null;

                if (null != selectedNode && !selectedNode.equals(selectedRecord)) {
                    updateSelection();
                }
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
        loadingLabel.show();
        resourceGroupService.findResourceGroupsByCriteria(criteria, new AsyncCallback<PageList<ResourceGroup>>() {

            public void onFailure(Throwable caught) {
                loadingLabel.hide();
                callback.onFailure(new RuntimeException(MSG.view_tree_common_loadFailed_node(), caught));
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
                                loadingLabel.hide();
                                callback.onFailure(new RuntimeException(MSG.view_tree_common_loadFailed_create(),
                                    caught));
                            }

                            public void onSuccess(ResourceGroup result) {
                                loadingLabel.hide();
                                // store a map entry from backingGroupId to AGTreeNode so we can easily
                                // get back to this node given the id of the backing group (from the viewpath)
                                autoGroupNodeMap.put(result.getId(), agNode);

                                // now that we know that backing group id, hold onto it in the node in case we need it
                                agNode.setResourceGroupId(result.getId());

                                callback.onSuccess(result);
                            }
                        });
                } else {
                    // backing group found
                    final ResourceGroup backingGroup = result.get(0);

                    // store a map entry from backingGroupId to AGTreeNode so we can easily
                    // get back to this node given the id of the backing group (from the viewpath)
                    autoGroupNodeMap.put(backingGroup.getId(), agNode);

                    // now that we know that backing group id, hold onto it in the node in case we need it
                    agNode.setResourceGroupId(backingGroup.getId());

                    // make sure the members are correct before rendering
                    resourceGroupService.setAssignedResources(backingGroup.getId(), childIds, false,
                        new AsyncCallback<Void>() {
                            public void onFailure(Throwable caught) {
                                loadingLabel.hide();
                                callback.onFailure(new RuntimeException(MSG.view_tree_common_loadFailed_update(),
                                    caught));
                            }

                            public void onSuccess(Void result) {
                                loadingLabel.hide();
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
        updateSelection(false);
    }

    private void updateSelection(boolean isRefresh) {

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

            if (isRefresh) {
                treeGrid.getTree().reloadChildren(selectedNode);
            }

            treeGrid.markForRedraw();
        }
    }

    private void showContextMenu(final AutoGroupTreeNode agNode) {
        getAutoGroupBackingGroup(agNode, new AsyncCallback<ResourceGroup>() {

            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_tree_common_loadFailed_selection(), caught);
            }

            public void onSuccess(ResourceGroup result) {
                autoGroupContextMenu.showContextMenu(ResourceTreeView.this, treeGrid, agNode, result);
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
                                    buildAndShowResourceContextMenu(node, resourceComposite, type);
                                }
                            });
                    }
                }
            });
    }

    private void buildAndShowResourceContextMenu(final ResourceTreeNode node,
        final ResourceComposite resourceComposite, final ResourceType resourceType) {
        final Resource resource = resourceComposite.getResource();
        final ResourcePermission resourcePermission = resourceComposite.getResourcePermission();

        // resource name
        resourceContextMenu.setItems(new MenuItem(resource.getName()));

        // resource type name
        resourceContextMenu.addItem(new MenuItem(MSG.view_tree_common_contextMenu_type_name_label(resourceType
            .getName())));

        // separator
        resourceContextMenu.addItem(new MenuItemSeparator());

        // refresh node
        MenuItem refresh = new MenuItem(MSG.common_button_refresh());
        refresh.addClickHandler(new ClickHandler() {

            public void onClick(MenuItemClickEvent event) {
                contextMenuRefresh(treeGrid, node, false);
            }
        });
        resourceContextMenu.addItem(refresh);

        // separator
        resourceContextMenu.addItem(new MenuItemSeparator());

        // child resources
        MenuItem childResources = new MenuItem(MSG.view_tabs_common_child_resources());
        boolean childResourcesEnabled = resourceType.getChildResourceTypes() != null
            && !resourceType.getChildResourceTypes().isEmpty();
        childResources.setEnabled(childResourcesEnabled);
        if (childResourcesEnabled) {
            childResources.addClickHandler(new ClickHandler() {
                public void onClick(MenuItemClickEvent event) {
                    CoreGUI.goToView(LinkManager.getResourceTabLink(resource.getId(), "Inventory", "Children"));
                }
            });
        }
        resourceContextMenu.addItem(childResources);

        // plugin config
        MenuItem pluginConfiguration = new MenuItem(MSG.view_tabs_common_connectionSettings());
        boolean pluginConfigEnabled = resourceType.getPluginConfigurationDefinition() != null;
        pluginConfiguration.setEnabled(pluginConfigEnabled);
        if (pluginConfigEnabled) {
            pluginConfiguration.addClickHandler(new ClickHandler() {

                public void onClick(MenuItemClickEvent event) {
                    CoreGUI.goToView(LinkManager.getResourceTabLink(resource.getId(), "Inventory", "ConnectionSettings"));
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
        MenuItem operations = new MenuItem(MSG.common_title_operations());
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
                            ResourceDetailView.Tab.Operations.NAME, "Schedules") + "/0/" + operationDefinition.getId();
                        CoreGUI.goToView(viewPath);
                    }
                });
                opSubMenu.addItem(operationItem);
            }
            operations.setSubmenu(opSubMenu);
        }
        resourceContextMenu.addItem(operations);

        // Create Child Menu and Manual Import Menu
        final Set<ResourceType> creatableChildTypes = getCreatableChildTypes(resourceType);
        final Set<ResourceType> importableChildTypes = getImportableChildTypes(resourceType);
        final boolean hasCreatableTypes = !creatableChildTypes.isEmpty();
        final boolean hasImportableTypes = !importableChildTypes.isEmpty();
        boolean canCreate = resourcePermission.isCreateChildResources();

        Integer[] singletonChildTypes = getSingletonChildTypes(resourceType);

        // To properly filter Create Child and Import menus we need existing singleton child resources. If the
        // user has created permission and the parent type has singleton child types and creatable or importable child
        // types, perform an async call to fetch the singleton children.
        if (canCreate && singletonChildTypes.length > 0 && (hasCreatableTypes || hasImportableTypes)) {

            ResourceCriteria criteria = new ResourceCriteria();
            criteria.addFilterParentResourceId(resource.getId());
            criteria.addFilterResourceTypeIds(singletonChildTypes);
            GWTServiceLookup.getResourceService().findResourcesByCriteria(criteria,
                new AsyncCallback<PageList<Resource>>() {

                    @Override
                    public void onSuccess(PageList<Resource> singletonChildren) {
                        if (hasCreatableTypes) {
                            Map<String, ResourceType> displayNameMap = getDisplayNames(creatableChildTypes);
                            addMenu(MSG.common_button_create_child(), true, singletonChildren, resource,
                                displayNameMap, true);
                        }

                        if (hasImportableTypes) {
                            Map<String, ResourceType> displayNameMap = getDisplayNames(importableChildTypes);
                            addMenu(MSG.common_button_import(), true, singletonChildren, resource, displayNameMap,
                                false);
                        }

                        resourceContextMenu.showContextMenu();
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        Log.error("Error resources with parentId:" + resource.getId(), caught);
                        resourceContextMenu.showContextMenu();
                    }
                });
        } else if (canCreate && singletonChildTypes.length == 0 && (hasCreatableTypes || hasImportableTypes)) {
            if (hasCreatableTypes) {
                Map<String, ResourceType> displayNameMap = getDisplayNames(creatableChildTypes);
                addMenu(MSG.common_button_create_child(), true, null, resource, displayNameMap, true);
            }

            if (hasImportableTypes) {
                Map<String, ResourceType> displayNameMap = getDisplayNames(importableChildTypes);
                addMenu(MSG.common_button_import(), true, null, resource, displayNameMap, false);
            }

            resourceContextMenu.showContextMenu();

        } else {
            if (!canCreate && hasCreatableTypes) {
                addMenu(MSG.common_button_create_child(), false, null, null, null, true);
            }
            if (!canCreate && hasImportableTypes) {
                addMenu(MSG.common_button_import(), false, null, null, null, false);
            }

            resourceContextMenu.showContextMenu();
        }
    }

    private void addMenu(String name, boolean enabled, List<Resource> singletonChildren, Resource resource,
        Map<String, ResourceType> displayNameMap, boolean isCreate) {
        MenuItem menu = new MenuItem(name);
        if (enabled) {
            Menu subMenu = new Menu();
            singletonChildren = (null == singletonChildren) ? new ArrayList<Resource>() : singletonChildren;
            Menu filteredSubMenu = checkForSingletons(singletonChildren, resource, displayNameMap, subMenu, isCreate);
            menu.setSubmenu(filteredSubMenu);
        } else {
            menu.setEnabled(false);
        }
        resourceContextMenu.addItem(menu);
    }

    private static Integer[] getSingletonChildTypes(ResourceType type) {
        Set<Integer> results = new TreeSet<Integer>();
        Set<ResourceType> childTypes = type.getChildResourceTypes();
        for (ResourceType childType : childTypes) {
            if (childType.isSingleton()) {
                results.add(childType.getId());
            }
        }

        return results.toArray(new Integer[results.size()]);
    }

    private Menu checkForSingletons(List<Resource> singletonChildren, final Resource resource,
        Map<String, ResourceType> displayNameMap, Menu subMenu, final boolean isCreate) {

        Set<String> displayNames = displayNameMap.keySet();
        for (final String displayName : displayNames) {
            MenuItem itemToAdd = new MenuItem(displayName);
            final ResourceType type = displayNameMap.get(displayName);
            boolean exists = false;

            // disable the menu item for a singleton type that already has a singleton child resource
            if (type.isSingleton()) {
                for (Resource child : singletonChildren) {
                    exists = child.getResourceType().equals(displayNameMap.get(displayName));
                    if (exists) {
                        break;
                    }
                }
            }

            // omit the type's menu item if the singleton already exists, otherwise add the necessary click handler.
            // note: we omit as opposed to disable the menu item to match the behavior of the buttons in the Inventory
            // -> Child Resources view, which has no facility to do the analogous disabling.
            if (!exists) {
                itemToAdd.addClickHandler(new ClickHandler() {
                    public void onClick(MenuItemClickEvent event) {
                        if (isCreate) {
                            ResourceFactoryCreateWizard.showCreateWizard(resource, type);
                        } else {
                            ResourceFactoryImportWizard.showImportWizard(resource, type);
                        }
                    }
                });
                subMenu.addItem(itemToAdd);
            }
        }
        return subMenu;
    }

    public void refreshResource(Resource resource) {
        // might happen if someone calls this very soon after creation when the tree didn't have enough time to load.
        // In that case we might as well ignore this refresh, because we'd get the fresh data in the tree anyway
        if (null == treeGrid || null == treeGrid.getTree()) {
            return;
        }

        ResourceTreeNode resourceNode = (ResourceTreeNode) treeGrid.getTree().findById(
            ResourceTreeNode.idOf(resource.getId()));

        // no need to refresh a node that is not in the tree (not sure why that would be, it should actually be selected, I think)
        if (null == resourceNode) {
            return;
        }

        contextMenuRefresh(treeGrid, resourceNode, true);
    }

    /**
     * Update the tree node (and all of its siblings, as reload is done from the parent). Also, refresh
     * the detail view.
     *
     * @param treeGrid
     * @param node
     * @param treeOnly, if possible, don't refresh the entire view, limit refresh to the tree
     */
    public void contextMenuRefresh(final TreeGrid treeGrid, TreeNode node, boolean treeOnly) {
        // There are two cases to handle here:
        // 1) The refresh node is an ancestor of the currently selected node. 
        //    - We must re-navigate to the selected node because otherwise it will be lost as part of the subtree
        //      being refreshed.
        // 2) The refresh node is not an ancestor of the current selected node (it is the node, is a descendant, or
        //    is in a different subtree.
        //    - In this case, reload the necessary portion of the tree, the selection should be maintained

        // determine if the refresh node is an ancestor of the selected node.
        boolean isAncestor = false;

        ListGridRecord selectedRecord = treeGrid.getSelectedRecord();
        if (null != selectedRecord) {
            TreeNode ancestor = (TreeNode) selectedRecord;
            while (null != (ancestor = treeGrid.getTree().getParent(ancestor))) {
                if (node.equals(ancestor)) {
                    isAncestor = true;
                    break;
                }
            }
        }

        // Case 1: An ancestor
        if (isAncestor) {
            // prune the existing subtree rooted at the refresh node, it will be rebuilt when we re-navigate to the
            // selected node.
            treeGrid.getTree().remove(node);

            if (selectedRecord instanceof ResourceTreeNode) {
                setSelectedResource(((ResourceTreeNode) selectedRecord).getResource().getId(), false);
            } else if (selectedRecord instanceof AutoGroupTreeNode) {
                Integer backingGroupId = ((AutoGroupTreeNode) selectedRecord).getResourceGroupId();
                if (null != backingGroupId) {
                    setSelectedAutoGroup(backingGroupId);
                }
            }
            return;
        }

        // Case 2: Not an ancestor

        // refresh the view. This won't refresh the tree since the resource hasn't changed, and
        // we don't really want to refresh the whole tree anyway.
        if (!treeOnly) {
            CoreGUI.refresh();
        }

        // if this is the root just refresh from the top
        Tree tree = treeGrid.getTree();
        TreeNode refreshNode = tree.getParent(node);
        if (null == refreshNode.getName()) {
            tree.reloadChildren(node);
            return;
        }

        // reloads are performed only on resource nodes. find the first parental resource node, traversing
        // through autogroup and subcategory nodes as needed.
        while (!(refreshNode instanceof ResourceTreeNode)) {
            refreshNode = tree.getParent(refreshNode);
        }

        tree.reloadChildren(refreshNode);
    }

    private void setRootResource(Resource rootResource) {
        this.rootResource = rootResource;
    }

    public void setSelectedResource(final int selectedResourceId, boolean isRefresh) {

        selectedNodeId = ResourceTreeNode.idOf(selectedResourceId);

        if (treeGrid != null && treeGrid.getTree() != null && (treeGrid.getTree().findById(selectedNodeId)) != null) {
            // This is the case where the tree was previously loaded and we get fired to look at a different
            // node in the same tree and just have to switch the selection            
            updateSelection(isRefresh);
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
        loadingLabel.show();
        resourceService.getResourceLineageAndSiblings(selectedResourceId,
            new AsyncCallback<List<ResourceLineageComposite>>() {

                public void onFailure(Throwable caught) {
                    loadingLabel.hide();
                    boolean resourceDoesNotExist = caught.getMessage().contains("ResourceNotFoundException");
                    // If a Resource with the specified id does not exist, don't emit an error, since
                    // ResourceDetailView.loadSelectedItem() will take care of emitting one.
                    if (!resourceDoesNotExist) {
                        CoreGUI.getErrorHandler().handleError(MSG.view_tree_common_loadFailed_root(), caught);
                    }
                }

                public void onSuccess(List<ResourceLineageComposite> result) {
                    Resource root = result.get(0).getResource();

                    final List<Resource> lineage = new ArrayList<Resource>(result.size());
                    final List<Resource> lockedData = new ArrayList<Resource>();

                    for (ResourceLineageComposite r : result) {
                        lineage.add(r.getResource());
                        if (r.isLocked()) {
                            if (r.getResource().getId() == selectedResourceId) {
                                // The selected Resource itself is locked. This means the user doesn't have authz to be
                                // viewing this Resource period, so just abort loading of the tree.
                                return;
                            }
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
                        ResourceTreeDatasource dataSource = new ResourceTreeDatasource(lineage, lockedData, treeGrid,
                            loadingLabel);
                        treeGrid.setDataSource(dataSource);

                        addMember(treeGrid);

                        treeGrid.fetchData(treeGrid.getCriteria(), new DSCallback() {

                            public void execute(DSResponse response, Object rawData, DSRequest request) {
                                Log.info("Done fetching data for tree.");

                                loadingLabel.hide();

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
                                    treeGrid.getTree().linkNodes(
                                        ResourceTreeDatasource.buildNodes(lineage, lockedData, treeGrid));

                                    loadingLabel.hide();

                                    if (updateSelection) {
                                        TreeNode selectedNode = treeGrid.getTree().findById(selectedNodeId);
                                        if (selectedNode != null && updateSelection) {
                                            updateSelection();

                                        } else {
                                            CoreGUI.getMessageCenter().notify(
                                                new Message(MSG.view_tree_common_loadFailed_selection(),
                                                    Message.Severity.Warning));
                                        }
                                    }

                                    if (null != callback) {
                                        callback.onSuccess(null);
                                    }
                                }
                            });
                    }
                }
            });
    }

    public void setSelectedAutoGroup(final Integer selectedAutoGroupId) {

        AutoGroupTreeNode selectedNode = autoGroupNodeMap.get(selectedAutoGroupId);
        if (treeGrid != null && treeGrid.getTree() != null && selectedNode != null
            && null != treeGrid.getTree().findById(selectedNode.getID())) {
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
            loadingLabel.show();
            resourceGroupService.findResourceGroupsByCriteria(criteria, new AsyncCallback<PageList<ResourceGroup>>() {

                public void onFailure(Throwable caught) {
                    loadingLabel.hide();
                    CoreGUI.getErrorHandler().handleError(MSG.view_tree_common_loadFailed_node(), caught);
                }

                public void onSuccess(PageList<ResourceGroup> result) {
                    final ResourceGroup backingGroup = result.get(0);

                    // load the tree up to the autogroup's parent resource. Don't select the resource node
                    // to avoid an unnecessary navigation to the resource, we just need the tree in place so
                    // we can navigate to the autogroup node.
                    loadTree(backingGroup.getAutoGroupParentResource().getId(), false, new AsyncCallback<Void>() {

                        public void onFailure(Throwable caught) {
                            loadingLabel.hide();
                            CoreGUI.getErrorHandler().handleError(MSG.view_tree_common_loadFailed_children(), caught);
                        }

                        public void onSuccess(Void arg) {
                            // get the node ID and use it to add a map entry, then call this again to finish up...
                            selectedNodeId = AutoGroupTreeNode.idOf(backingGroup.getAutoGroupParentResource(),
                                backingGroup.getResourceType());
                            AutoGroupTreeNode agNode = (AutoGroupTreeNode) treeGrid.getTree().findById(selectedNodeId);
                            autoGroupNodeMap.put(backingGroup.getId(), agNode);
                            updateSelection();

                            loadingLabel.hide();
                        }
                    });
                }
            });
        }
    }

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
            setSelectedResource(resourceId, viewPath.isRefresh());
        }
    }

    private static Set<ResourceType> getImportableChildTypes(ResourceType type) {
        Set<ResourceType> results = new TreeSet<ResourceType>();
        Set<ResourceType> childTypes = type.getChildResourceTypes();
        for (ResourceType childType : childTypes) {
            if (childType.isSupportsManualAdd()) {
                results.add(childType);
            }
        }
        return results;
    }

    private static Set<ResourceType> getCreatableChildTypes(ResourceType type) {
        Set<ResourceType> results = new TreeSet<ResourceType>();
        Set<ResourceType> childTypes = type.getChildResourceTypes();
        for (ResourceType childType : childTypes) {
            if (childType.isCreatable()) {
                results.add(childType);
            }
        }
        return results;
    }

    private static Map<String, ResourceType> getDisplayNames(Set<ResourceType> types) {
        Set<String> allNames = new HashSet<String>();
        Set<String> repeatedNames = new HashSet<String>();
        for (ResourceType type : types) {
            String typeName = type.getName();
            if (allNames.contains(typeName)) {
                repeatedNames.add(typeName);
            } else {
                allNames.add(typeName);
            }
        }
        Map<String, ResourceType> results = new TreeMap<String, ResourceType>();
        for (ResourceType type : types) {
            String displayName = type.getName();
            if (repeatedNames.contains(type.getName())) {
                displayName += " (" + type.getPlugin() + " plugin)";
            }
            results.put(displayName, type);
        }
        return results;
    }

}
