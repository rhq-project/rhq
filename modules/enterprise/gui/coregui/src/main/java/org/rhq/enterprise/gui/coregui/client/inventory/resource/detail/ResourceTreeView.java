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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail;

import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.gui.coregui.client.components.configuration.ConfigurationEditor;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceSelectListener;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.util.SC;
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

import java.util.ArrayList;
import java.util.List;

/**
 * @author Greg Hinkle
 */
public class ResourceTreeView extends VLayout {
    private Resource selectedResource;
    private Resource rootResource;

    private TreeGrid treeGrid;
    Menu contextMenu;

    private ArrayList<ResourceSelectListener> selectListeners = new ArrayList<ResourceSelectListener>();

    private boolean initialSelect = false;

    public ResourceTreeView(Resource selectedResource) {
        super();
        this.selectedResource = selectedResource;

        setWidth("30%");
        setHeight100();

        setShowResizeBar(true);

        if (selectedResource != null) {
            setSelectedResource(selectedResource);
        }
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


        contextMenu = new Menu();
        MenuItem item = new MenuItem("Expand node");
        item.addClickHandler(new com.smartgwt.client.widgets.menu.events.ClickHandler() {
            public void onClick(MenuItemClickEvent event) {
                TreeGrid treeGrid = (TreeGrid) event.getTarget();
                System.out.println("You right clicked: " + treeGrid.getSelectedRecord());
            }
        });

        /* Do menu support datasources? GH: Seemingly they'll only load once when done this way
        contextMenu.setDataSource(new DataSource() {
            {
                setClientOnly(false);
                setDataProtocol(DSProtocol.CLIENTCUSTOM);
                setDataFormat(DSDataFormat.CUSTOM);
            }
            protected Object transformRequest(DSRequest request) {
                System.out.println("Looking up menu info");
                return request;
            }
        });
        */

        // Should only do this once, but it opens the platform node (which is not actually the root node)
//        treeGrid.addDataArrivedHandler(new DataArrivedHandler() {
//            public void onDataArrived(DataArrivedEvent dataArrivedEvent) {
//                treeGrid.getTree().openFolders(treeGrid.getTree().getChildren(treeGrid.getTree().getRoot()));
//            }
//        });


        treeGrid.addSelectionChangedHandler(new SelectionChangedHandler() {
            public void onSelectionChanged(SelectionEvent selectionEvent) {
                if (selectionEvent.getState()) {
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

                buildContextMenu(event.getNode());

                contextMenu.showContextMenu();
            }
        });
    }


    private void buildContextMenu(TreeNode node) {
        if (node instanceof ResourceTreeDatasource.TypeTreeNode) {
            buildContextMenu((ResourceTreeDatasource.TypeTreeNode) node);
        } else if (node instanceof ResourceTreeDatasource.ResourceTreeNode) {
            buildContextMenu((ResourceTreeDatasource.ResourceTreeNode) node);
        }
    }


    private void buildContextMenu(ResourceTreeDatasource.TypeTreeNode node) {

        contextMenu.setItems(new MenuItem(node.getName()));


    }

    private void buildContextMenu(final ResourceTreeDatasource.ResourceTreeNode node) {

        contextMenu.setItems(new MenuItem(node.getName()));

        contextMenu.addItem(new MenuItem("Type: " + node.getResourceType().getName()));


        MenuItem editPluginConfiguration = new MenuItem("Plugin Configuration");
        editPluginConfiguration.addClickHandler(new ClickHandler() {
            public void onClick(MenuItemClickEvent event) {
                Resource resource = node.getResource();
                int resourceId = node.getResource().getId();
                int resourceTypeId = node.getResourceType().getId();

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
        editPluginConfiguration.setEnabled(node.getResource().getResourceType().getPluginConfigurationDefinition() != null);
        contextMenu.addItem(editPluginConfiguration);


        MenuItem editResourceConfiguration = new MenuItem("Resource Configuration");
        editResourceConfiguration.addClickHandler(new ClickHandler() {
            public void onClick(MenuItemClickEvent event) {
                Resource resource = node.getResource();
                int resourceId = node.getResource().getId();
                int resourceTypeId = node.getResourceType().getId();

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
        editResourceConfiguration.setEnabled(node.getResource().getResourceType().getResourceConfigurationDefinition() != null);
        contextMenu.addItem(editResourceConfiguration);

        contextMenu.addItem(new MenuItemSeparator());


        // Operations Menu
        MenuItem operations = new MenuItem("Operations");
        Menu opSubMenu = new Menu();
        for (OperationDefinition operationDefinition : node.getResourceType().getOperationDefinitions()) {
            opSubMenu.addItem(new MenuItem(operationDefinition.getDisplayName()));
            // todo action
        }
        operations.setEnabled(!node.getResourceType().getOperationDefinitions().isEmpty());
        operations.setSubmenu(opSubMenu);
        contextMenu.addItem(operations);


        // Create Menu
        MenuItem createChildMenu = new MenuItem("Create Child");
        Menu createChildSubMenu = new Menu();
        for (ResourceType childType : node.getResourceType().getChildResourceTypes()) {
            if (childType.isCreatable()) {
                createChildSubMenu.addItem(new MenuItem(childType.getName()));
                //todo action
            }
        }
        createChildMenu.setSubmenu(createChildSubMenu);
        createChildMenu.setEnabled(createChildSubMenu.getItems().length > 0);
        contextMenu.addItem(createChildMenu);


        // Manually Menu
        MenuItem importChildMenu = new MenuItem("Import");
        Menu importChildSubMenu = new Menu();
        for (ResourceType childType : node.getResourceType().getChildResourceTypes()) {
            if (childType.isSupportsManualAdd()) {
                importChildSubMenu.addItem(new MenuItem(childType.getName()));
                //todo action
            }
        }
        importChildMenu.setSubmenu(importChildSubMenu);
        importChildMenu.setEnabled(importChildSubMenu.getItems().length > 0);
        contextMenu.addItem(importChildMenu);

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

    public void setSelectedResource(final Resource selectedResource) {
        this.selectedResource = selectedResource;

        TreeNode node = null;
        if (treeGrid != null && treeGrid.getTree() != null
                && (node = treeGrid.getTree().findById(String.valueOf(selectedResource.getId()))) != null) {

            TreeNode[] parents = treeGrid.getTree().getParents(node);
            treeGrid.getTree().openFolders(parents);
            treeGrid.getTree().openFolder(node);

            treeGrid.deselectAllRecords();
            treeGrid.selectRecord(node);
        } else {
            final ResourceGWTServiceAsync resourceService = GWTServiceLookup.getResourceService();
            resourceService.getResourceLineageAndSiblings(selectedResource.getId(), new AsyncCallback<List<Resource>>() {
                public void onFailure(Throwable caught) {
                    SC.say("Failed to lookup platform");
                    System.out.println("Failed to lookup parent");
                }

                public void onSuccess(List<Resource> result) {
                    Resource root = result.get(0);

                    if (!root.equals(ResourceTreeView.this.rootResource)) {

                        if (treeGrid != null) {
                            treeGrid.destroy();
                        }
                        buildTree();

                        setRootResource(root);


                        treeGrid.addDataArrivedHandler(new DataArrivedHandler() {
                            public void onDataArrived(DataArrivedEvent dataArrivedEvent) {
                                if (!initialSelect) {

                                    TreeNode selectedNode = treeGrid.getTree().findById(String.valueOf(selectedResource.getId()));
                                    System.out.println("Trying to preopen: " + selectedNode);
                                    if (selectedNode != null) {
                                        System.out.println("Preopen node!!!");
                                        TreeNode[] parents = treeGrid.getTree().getParents(selectedNode);
                                        treeGrid.getTree().openFolders(parents);
                                        treeGrid.getTree().openFolder(selectedNode);

                                        for (TreeNode p : parents) {
                                            System.out.println("open? " + treeGrid.getTree().isOpen(p) + "   node: " + p.getName());
                                        }

                                        treeGrid.selectRecord(selectedNode);
                                        initialSelect = true;
                                        treeGrid.markForRedraw();
                                    }
                                }
                            }
                        });

                        ResourceTreeDatasource dataSource = new ResourceTreeDatasource(result);
                        treeGrid.setDataSource(dataSource);
                        // GH: couldn't get initial data to mix with the datasource... so i put the inital data in
                        // the first datasource request
//                    treeGrid.setInitialData(selectedLineage);

                        addMember(treeGrid);


                        TreeNode selectedNode = treeGrid.getTree().findById(String.valueOf(selectedResource.getId()));
                        System.out.println("Trying to preopen: " + selectedNode);
                        if (selectedNode != null) {
                            System.out.println("Preopen node!!!");
                            TreeNode[] parents = treeGrid.getTree().getParents(selectedNode);
                            treeGrid.getTree().openFolders(parents);
                            treeGrid.getTree().openFolder(selectedNode);

                            for (TreeNode p : parents) {
                                System.out.println("open? " + treeGrid.getTree().isOpen(p) + "   node: " + p.getName());
                            }

                            treeGrid.selectRecord(selectedNode);
                            initialSelect = true;
                            treeGrid.markForRedraw();
                        }


//                    treeGrid.fegetTree().openFolder(rootData[0]);


                    } else {
                        initialSelect = false;
                        for (TreeNode n : ResourceTreeDatasource.build(result)) {
                            treeGrid.getDataSource().addData(n);
                        }

                        TreeNode selectedNode = treeGrid.getTree().findById(String.valueOf(selectedResource.getId()));
                        if (selectedNode != null) {
                            treeGrid.deselectAllRecords();
                            treeGrid.selectRecord(selectedNode);

                            TreeNode[] parents = treeGrid.getTree().getParents(selectedNode);
                            treeGrid.getTree().openFolders(parents);
                            treeGrid.getTree().openFolder(selectedNode);
                        }
                        
                    }


                    // CoreGUI.addBreadCrumb(new Place(String.valueOf(result.getId()), result.getName()));
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
}



