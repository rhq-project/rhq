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
import java.util.List;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.events.CloseClientEvent;
import com.smartgwt.client.widgets.grid.events.SelectionChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionEvent;
import com.smartgwt.client.widgets.layout.VLayout;
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
import org.rhq.core.domain.criteria.ResourceTypeCriteria;
import org.rhq.core.domain.dashboard.Dashboard;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.Breadcrumb;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ViewId;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.configuration.ConfigurationEditor;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.inventory.resource.graph.GraphPortlet;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceTypeGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceSelectListener;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.operation.create.OperationCreateWizard;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.factory.ResourceFactoryCreateWizard;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

/**
 * @author Greg Hinkle
 */
public class ResourceTreeView extends VLayout {

    private int selectedResourceId;

    private Resource selectedResource;
    private Resource rootResource;

    private TreeGrid treeGrid;
    private Menu contextMenu;

    private ViewId currentViewId;

    private ArrayList<ResourceSelectListener> selectListeners = new ArrayList<ResourceSelectListener>();

    private boolean initialSelect = false;

    public ResourceTreeView() {
        super();

        setWidth("250");
        setHeight100();

        setShowResizeBar(true);
    }

    public void onInit() {

    }

    private void buildTree() {

        treeGrid = new CustomResourceTreeGrid();

        treeGrid.setOpenerImage("resources/dir.png");
        treeGrid.setOpenerIconSize(16);

        treeGrid.setAutoFetchData(true);
        treeGrid.setAnimateFolders(false);
        treeGrid.setSelectionType(SelectionStyle.SINGLE);
        treeGrid.setShowRollOver(false);
        treeGrid.setSortField("name");
        treeGrid.setShowHeader(false);

        treeGrid.setLeaveScrollbarGap(false);
        

        contextMenu = new Menu();
        MenuItem item = new MenuItem("Expand node");


        treeGrid.addSelectionChangedHandler(new SelectionChangedHandler() {
            public void onSelectionChanged(SelectionEvent selectionEvent) {
                if (!selectionEvent.isRightButtonDown() && selectionEvent.getState()) {
                    if (treeGrid.getSelectedRecord() instanceof ResourceTreeDatasource.ResourceTreeNode) {
                        ResourceTreeDatasource.ResourceTreeNode node = (ResourceTreeDatasource.ResourceTreeNode) treeGrid.getSelectedRecord();
                        System.out.println("Resource selected in tree: " + node.getResource());

                        String newToken = "Resource/" + node.getResource().getId();
                        String currentToken = History.getToken();
                        if (!currentToken.startsWith(newToken)) {

                            String ending = currentToken.replaceFirst("^[^\\/]*\\/[^\\/]*", "");

                            History.newItem("Resource/" + node.getResource().getId() + ending);

                        }
                    }
                }
            }
        });


        // This constructs the context menu for the resource at the time of the click.
        setContextMenu(contextMenu);



        treeGrid.addNodeContextClickHandler(new NodeContextClickHandler() {
            public void onNodeContextClick(final NodeContextClickEvent event) {
                event.getNode();
                event.cancel();

                if (event.getNode() instanceof ResourceTreeDatasource.TypeTreeNode) {
                    showContextMenu((ResourceTreeDatasource.TypeTreeNode) event.getNode());
                } else if (event.getNode() instanceof ResourceTreeDatasource.ResourceTreeNode) {
                    showContextMenu((ResourceTreeDatasource.ResourceTreeNode) event.getNode());
                }
            }
        });

         treeGrid.addDataArrivedHandler(new DataArrivedHandler() {
                public void onDataArrived(DataArrivedEvent dataArrivedEvent) {
                    if (!initialSelect) {

                        updateBreadcrumb();
                    }
                }
            });
    }

    private void updateBreadcrumb() {
        TreeNode selectedNode = treeGrid.getTree().findById(String.valueOf(selectedResourceId));
//                                    System.out.println("Trying to preopen: " + selectedNode);
        if (selectedNode != null) {
            TreeNode[] parents = treeGrid.getTree().getParents(selectedNode);
            treeGrid.getTree().openFolders(parents);
            treeGrid.getTree().openFolder(selectedNode);

            for (TreeNode p : parents) {
//                                            System.out.println("open? " + treeGrid.getTree().isOpen(p) + "   node: " + p.getName());
            }

            treeGrid.selectRecord(selectedNode);
            initialSelect = true;
            treeGrid.markForRedraw();

            // Update breadcrumbs
            if (currentViewId != null) {
                currentViewId.getBreadcrumbs().clear();
                for (int i = parents.length - 1; i >= 0; i--) {
                    TreeNode n = parents[i];
                    adjustBreadcrumb(n, currentViewId);
                }
                adjustBreadcrumb(selectedNode, currentViewId);
                CoreGUI.refreshBreadCrumbTrail();
            }
        }
    }


    private void showContextMenu(ResourceTreeDatasource.TypeTreeNode node) {

        contextMenu.setItems(new MenuItem(node.getName()));
        contextMenu.showContextMenu();

    }

    private void showContextMenu(final ResourceTreeDatasource.ResourceTreeNode node) {
        ResourceTypeRepository.Cache.getInstance().getResourceTypes(
                node.getResourceType().getId(),
                EnumSet.of(ResourceTypeRepository.MetadataType.operations, ResourceTypeRepository.MetadataType.children, ResourceTypeRepository.MetadataType.subCategory,
                        ResourceTypeRepository.MetadataType.pluginConfigurationDefinition, ResourceTypeRepository.MetadataType.resourceConfigurationDefinition),
                new ResourceTypeRepository.TypeLoadedCallback() {
                    public void onTypesLoaded(ResourceType type) {
                        buildResourceContextMenu(node.getResource(), type);
                        contextMenu.showContextMenu();
                    }
                });
    }


    private void buildResourceContextMenu(final Resource resource, final ResourceType resourceType) {
        contextMenu.setItems(new MenuItem(resource.getName()));

        contextMenu.addItem(new MenuItem("Type: " + resourceType.getName()));


        MenuItem editPluginConfiguration = new MenuItem("Plugin Configuration");
        editPluginConfiguration.addClickHandler(new ClickHandler() {
            public void onClick(MenuItemClickEvent event) {
                int resourceId = resource.getId();
                int resourceTypeId = resourceType.getId();

                Window configEditor = new Window();
                configEditor.setTitle("Edit " + resource.getName() + " plugin configuration");
                configEditor.setWidth(800);
                configEditor.setHeight(800);
                configEditor.setIsModal(true);
                configEditor.setShowModalMask(true);
                configEditor.setCanDragResize(true);
                configEditor.centerInPage();
                configEditor.addItem(new ConfigurationEditor(resourceId, resourceTypeId, ConfigurationEditor.ConfigType.plugin));
                configEditor.show();

            }
        });
        editPluginConfiguration.setEnabled(resourceType.getPluginConfigurationDefinition() != null);
        contextMenu.addItem(editPluginConfiguration);


        MenuItem editResourceConfiguration = new MenuItem("Resource Configuration");
        editResourceConfiguration.addClickHandler(new ClickHandler() {
            public void onClick(MenuItemClickEvent event) {
                int resourceId = resource.getId();
                int resourceTypeId = resourceType.getId();

                final Window configEditor = new Window();
                configEditor.setTitle("Edit " + resource.getName() + " resource configuration");
                configEditor.setWidth(800);
                configEditor.setHeight(800);
                configEditor.setIsModal(true);
                configEditor.setShowModalMask(true);
                configEditor.setCanDragResize(true);
                configEditor.setShowResizer(true);
                configEditor.centerInPage();
                configEditor.addCloseClickHandler(new CloseClickHandler() {
                    public void onCloseClick(CloseClientEvent closeClientEvent) {
                        configEditor.destroy();
                    }
                });
                configEditor.addItem(new ConfigurationEditor(resourceId, resourceTypeId, ConfigurationEditor.ConfigType.resource));
                configEditor.show();

            }
        });
        editResourceConfiguration.setEnabled(resourceType.getResourceConfigurationDefinition() != null);
        contextMenu.addItem(editResourceConfiguration);

        contextMenu.addItem(new MenuItemSeparator());


        // Operations Menu
        MenuItem operations = new MenuItem("Operations");
        Menu opSubMenu = new Menu();
        for (final OperationDefinition operationDefinition : resourceType.getOperationDefinitions()) {
            MenuItem operationItem = new MenuItem(operationDefinition.getDisplayName());
            operationItem.addClickHandler(new ClickHandler() {
                public void onClick(MenuItemClickEvent event) {

                    ResourceCriteria criteria = new ResourceCriteria();
                    criteria.addFilterId(selectedResourceId);

                    GWTServiceLookup.getResourceService().findResourcesByCriteria(criteria,
                            new AsyncCallback<PageList<Resource>>() {
                                public void onFailure(Throwable caught) {
                                    CoreGUI.getErrorHandler().handleError("Failed to get resource to run operation",caught);
                                }

                                public void onSuccess(PageList<Resource> result) {
                                    new OperationCreateWizard(result.get(0), operationDefinition).startOperationWizard();
                                }
                            });

                }
            });
            opSubMenu.addItem(operationItem);
            // todo action
        }
        operations.setEnabled(!resourceType.getOperationDefinitions().isEmpty());
        operations.setSubmenu(opSubMenu);
        contextMenu.addItem(operations);



        contextMenu.addItem(buildMetricsMenu(resourceType));



        // Create Menu
        MenuItem createChildMenu = new MenuItem("Create Child");
        Menu createChildSubMenu = new Menu();
        for (final ResourceType childType : resourceType.getChildResourceTypes()) {
            if (childType.isCreatable()) {
                MenuItem createItem = new MenuItem(childType.getName());
                createChildSubMenu.addItem(createItem);
                createItem.addClickHandler(new ClickHandler() {
                    public void onClick(MenuItemClickEvent event) {
                        ResourceFactoryCreateWizard.showCreateWizard(resource, childType);
                    }
                });

            }
        }
        createChildMenu.setSubmenu(createChildSubMenu);
        createChildMenu.setEnabled(createChildSubMenu.getItems().length > 0);
        contextMenu.addItem(createChildMenu);


        // Manually Add Menu
        MenuItem importChildMenu = new MenuItem("Import");
        Menu importChildSubMenu = new Menu();
        for (ResourceType childType : resourceType.getChildResourceTypes()) {
            if (childType.isSupportsManualAdd()) {
                importChildSubMenu.addItem(new MenuItem(childType.getName()));
                //todo action
            }
        }
        if (resourceType.getCategory() == ResourceCategory.PLATFORM) {
            loadManuallyAddServersToPlatforms(importChildSubMenu);
        }
        importChildMenu.setSubmenu(importChildSubMenu);
        importChildMenu.setEnabled(importChildSubMenu.getItems().length > 0);
        contextMenu.addItem(importChildMenu);
    }


    private void loadManuallyAddServersToPlatforms(final Menu manuallyAddMenu) {
        ResourceTypeGWTServiceAsync rts = GWTServiceLookup.getResourceTypeGWTService();

        ResourceTypeCriteria criteria = new ResourceTypeCriteria();
        criteria.addFilterSupportsManualAdd(true);
        criteria.fetchParentResourceTypes(true);
        rts.findResourceTypesByCriteria(criteria,
                new AsyncCallback<PageList<ResourceType>>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError("Failed to load platform manual add children",caught);
                    }

                    public void onSuccess(PageList<ResourceType> result) {
                        for (ResourceType type : result) {
                            if (type.getParentResourceTypes() == null || type.getParentResourceTypes().isEmpty()) {
                                MenuItem item = new MenuItem(type.getName());
                                manuallyAddMenu.addItem(item);
                            }
                        }
                    }
                });
    }


    private MenuItem buildMetricsMenu(final ResourceType type) {
        MenuItem measurements = new MenuItem("Measurements");
        final Menu measurementsSubMenu = new Menu();




        GWTServiceLookup.getDashboardService().findDashboardsForSubject(new AsyncCallback<List<Dashboard>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("Failed to load user dashboards", caught);
            }

            public void onSuccess(List<Dashboard> result) {

                for (final MeasurementDefinition def : type.getMetricDefinitions()) {

                    MenuItem defItem = new MenuItem(def.getDisplayName());
                    measurementsSubMenu.addItem(defItem);
                    Menu defSubItem = new Menu();
                    defItem.setSubmenu(defSubItem);

                    for (final Dashboard d : result) {
                        MenuItem addToDBItem = new MenuItem("Add chart to Dashboard: " + d.getName());
                        defSubItem.addItem(addToDBItem);

                        addToDBItem.addClickHandler(new ClickHandler() {
                            public void onClick(MenuItemClickEvent menuItemClickEvent) {

                                DashboardPortlet p = new DashboardPortlet(def.getDisplayName() + " Chart", GraphPortlet.KEY, 250);
                                p.getConfiguration().put(new PropertySimple(GraphPortlet.CFG_RESOURCE_ID, selectedResourceId));
                                p.getConfiguration().put(new PropertySimple(GraphPortlet.CFG_DEFINITION_ID, def.getId()));

                                d.addPortlet(p, 0, 0);

                                GWTServiceLookup.getDashboardService().storeDashboard(d, new AsyncCallback<Dashboard>() {
                                    public void onFailure(Throwable caught) {
                                        CoreGUI.getErrorHandler().handleError("Failed to save dashboard to server", caught);
                                    }

                                    public void onSuccess(Dashboard result) {
                                        CoreGUI.getMessageCenter().notify(new Message("Saved dashboard " + result.getName() + " to server", Message.Severity.Info));
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
            ResourceTreeDatasource.ResourceTreeNode treeNode =
                    (ResourceTreeDatasource.ResourceTreeNode) this.treeGrid.getTree().findById(String.valueOf(resourceId));
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
        this.selectedResourceId = selectedResourceId;

        TreeNode node = null;
        if (treeGrid != null && treeGrid.getTree() != null
                && (node = treeGrid.getTree().findById(String.valueOf(selectedResourceId))) != null) {

            // This is the case where the tree was previously loaded and we get fired to look at a different
            // node in the same tree and just have to switch the selection

            TreeNode[] parents = treeGrid.getTree().getParents(node);
            treeGrid.getTree().openFolders(parents);
            treeGrid.getTree().openFolder(node);

            treeGrid.deselectAllRecords();
            treeGrid.selectRecord(node);


            updateBreadcrumb();
            /*
            TODO DELETEME
            // Update breadcrumbs
            viewId.getBreadcrumbs().clear();
            for (int i = parents.length - 1; i >= 0; i--) {
                TreeNode n = parents[i];
                adjustBreadcrumb(n, viewId);
            }
            adjustBreadcrumb(node, viewId);

            CoreGUI.refreshBreadCrumbTrail();
*/

        } else {

            // This is for cases where we have to load the tree fresh including down to the currently visible node

            final ResourceGWTServiceAsync resourceService = GWTServiceLookup.getResourceService();
            // This is an expensive call, but loads all nodes that are visible in the tree given a selected resource
            resourceService.getResourceLineageAndSiblings(selectedResourceId, new AsyncCallback<List<Resource>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError("Failed to lookup platform for tree", caught);
                }

                public void onSuccess(List<Resource> result) {
                    Resource root = result.get(0);

                    if (!root.equals(ResourceTreeView.this.rootResource)) {

                        if (treeGrid != null) {
                            treeGrid.destroy();
                        }
                        buildTree();

                        setRootResource(root);




                        ResourceTreeDatasource dataSource = new ResourceTreeDatasource(result);
                        treeGrid.setDataSource(dataSource);
                        // GH: couldn't get initial data to mix with the datasource... so i put the inital data in
                        // the first datasource request
//                    treeGrid.setInitialData(selectedLineage);

                        addMember(treeGrid);


                        treeGrid.fetchData(treeGrid.getCriteria(),new DSCallback() {
                            public void execute(DSResponse dsResponse, Object o, DSRequest dsRequest) {
                                System.out.println("Here!!!!!");
                                updateBreadcrumb();
                            }
                        });



                        TreeNode selectedNode = treeGrid.getTree().findById(String.valueOf(selectedResourceId));
//                        System.out.println("Trying to preopen: " + selectedNode);
                        if (selectedNode != null) {
//                            System.out.println("Preopen node!!!");
                            TreeNode[] parents = treeGrid.getTree().getParents(selectedNode);
                            treeGrid.getTree().openFolders(parents);
                            treeGrid.getTree().openFolder(selectedNode);

                            for (TreeNode p : parents) {
                                System.out.println("open? " + treeGrid.getTree().isOpen(p) + "   node: " + p.getName());
                            }

                            updateBreadcrumb();

                            treeGrid.selectRecord(selectedNode);
                            initialSelect = true;
                            treeGrid.markForRedraw();
                        }

                    } else {

                        initialSelect = false;
                        ResourceTypeRepository.Cache.getInstance().loadResourceTypes(result,
                                EnumSet.of(ResourceTypeRepository.MetadataType.operations, ResourceTypeRepository.MetadataType.children, ResourceTypeRepository.MetadataType.subCategory),
                                new ResourceTypeRepository.ResourceTypeLoadedCallback() {
                                    public void onResourceTypeLoaded(List<Resource> result) {

                                        treeGrid.getTree().linkNodes(ResourceTreeDatasource.build(result));

                                        TreeNode selectedNode = treeGrid.getTree().findById(String.valueOf(selectedResourceId));
                                        if (selectedNode != null) {
                                            treeGrid.deselectAllRecords();
                                            treeGrid.selectRecord(selectedNode);

                                            TreeNode[] parents = treeGrid.getTree().getParents(selectedNode);
                                            treeGrid.getTree().openFolders(parents);
                                            treeGrid.getTree().openFolder(selectedNode);

                                            /*
                                            todo deleteme
                                            // Update breadcrumbs
                                            viewId.getBreadcrumbs().clear();
                                            for (int i = parents.length - 1; i >= 0; i--) {
                                                TreeNode n = parents[i];
                                                adjustBreadcrumb(n, viewId);
                                            }
                                            adjustBreadcrumb(selectedNode, viewId);
                                            CoreGUI.refreshBreadCrumbTrail();*/

                                        } else {
                                            CoreGUI.getMessageCenter().notify(new Message("Failed to select resource [" + selectedResourceId + "] in tree.", Message.Severity.Warning));
                                        }


                                    }
                                });


                    }

                    TreeNode selectedNode = treeGrid.getTree().findById(String.valueOf(selectedResourceId));
//                                    System.out.println("Trying to preopen: " + selectedNode);
                    if (selectedNode != null) {
                        TreeNode[] parents = treeGrid.getTree().getParents(selectedNode);

                        // todo update viewPath's breadcrumbs
                    }

                    // CoreGUI.addBreadCrumb(new Place(String.valueOf(result.getId()), result.getName()));
                }
            });
        }
    }


    private void adjustBreadcrumb(TreeNode node, ViewId viewId) {
        if (node instanceof ResourceTreeDatasource.ResourceTreeNode) {

            Resource nr = ((ResourceTreeDatasource.ResourceTreeNode) node).getResource();
            String display = node.getName() + " <span class=\"subtitle\">" + nr.getResourceType().getName() + "</span>";
            String icon = "types/" + nr.getResourceType().getCategory().getDisplayName() + "_up_16.png";


            viewId.getBreadcrumbs().add(new Breadcrumb(node.getAttribute("id"),
                    display, icon, true));

        } else {

//            if (node.getName() != null) {
//                viewId.getBreadcrumbs().add(new Breadcrumb(node.getAttribute("id"), node.getName(), null, true));
//            }
        }
    }


    /*private List<Resource> preload(final List<Resource> lineage) {

            final ArrayList<Resource> list = new ArrayList<Resource>(lineage);

            ResourceGWTServiceAsync resourceService = ResourceGWTServiceAsync.Util.getInstance();

                ResourceCriteria c = new ResourceCriteria();
                c.addFilterParentResourceId(lineage.get(0).getId());
                resourceService.findResourcesByCriteria(CoreGUI.getSessionSubject(), c, new AsyncCallback<PageList<Resource>>() {
                    public void onFailure(Throwable caught) {
                        SC.say("SHit");
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

    public void addResourceSelectListener(ResourceSelectListener listener) {
        this.selectListeners.add(listener);
    }

    public void renderView(ViewPath viewPath) {
        currentViewId = viewPath.getCurrent();
        Integer resourceId = Integer.parseInt(currentViewId.getPath());
        setSelectedResource(resourceId);
    }
}



