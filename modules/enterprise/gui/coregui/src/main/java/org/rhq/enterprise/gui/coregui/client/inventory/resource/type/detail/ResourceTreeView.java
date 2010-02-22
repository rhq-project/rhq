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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.type.detail;

import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.gui.coregui.client.components.configuration.ConfigurationEditor;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceTreeDatasource;

import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.events.CloseClientEvent;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuItem;
import com.smartgwt.client.widgets.menu.MenuItemSeparator;
import com.smartgwt.client.widgets.menu.events.ClickHandler;
import com.smartgwt.client.widgets.menu.events.MenuItemClickEvent;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.events.DataArrivedEvent;
import com.smartgwt.client.widgets.tree.events.DataArrivedHandler;
import com.smartgwt.client.widgets.tree.events.NodeContextClickEvent;
import com.smartgwt.client.widgets.tree.events.NodeContextClickHandler;

/**
 * @author Greg Hinkle
 */
public class ResourceTreeView extends ResourceTreeView.ResourceTreeGrid {

    public ResourceTreeView() {
        super();


        resourceTree.setWidth(500);
        resourceTree.setShowResizeBar(true);
        resourceTree.setHeight100();
        resourceTree.setOpenerImage("resources/dir.png");
        resourceTree.setOpenerIconSize(16);

        resourceTree.setAutoFetchData(true);
        resourceTree.setAnimateFolders(false);

        resourceTree.setDataSource(new ResourceTreeDatasource());
//        ((Layout)treeTab.getPane()).add.addChild(resourceTree);


        final Menu contextMenu = new Menu();
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
        resourceTree.addDataArrivedHandler(new DataArrivedHandler() {
            public void onDataArrived(DataArrivedEvent dataArrivedEvent) {
                resourceTree.getTree().openFolders(resourceTree.getTree().getChildren(resourceTree.getTree().getRoot()));
            }
        });


        resourceTree.setContextMenu(contextMenu);
        resourceTree.addNodeContextClickHandler(new NodeContextClickHandler() {
            public void onNodeContextClick(final NodeContextClickEvent nodeContextClickEvent) {
                nodeContextClickEvent.getNode();
                contextMenu.setItems(new MenuItem(nodeContextClickEvent.getNode().getName()));
                resourceTree.getTree();
                if (nodeContextClickEvent.getNode() instanceof ResourceTreeDatasource.ResourceTreeNode) {

                    ResourceTreeDatasource.ResourceTreeNode node = (ResourceTreeDatasource.ResourceTreeNode) nodeContextClickEvent.getNode();
                    contextMenu.addItem(new MenuItem("Type: " + ((ResourceTreeDatasource.ResourceTreeNode) nodeContextClickEvent.getNode()).getResourceType().getName()));


                    MenuItem editPluginConfiguration = new MenuItem("Plugin Configuration");
                    editPluginConfiguration.addClickHandler(new ClickHandler() {
                        public void onClick(MenuItemClickEvent event) {
                            Resource resource = ((ResourceTreeDatasource.ResourceTreeNode) nodeContextClickEvent.getNode()).getResource();
                            int resourceId = ((ResourceTreeDatasource.ResourceTreeNode) nodeContextClickEvent.getNode()).getResource().getId();
                            int resourceTypeId = ((ResourceTreeDatasource.ResourceTreeNode) nodeContextClickEvent.getNode()).getResourceType().getId();

                            Window configEditor = new Window();
                            configEditor.setTitle("Edit " + resource.getName() + " plugin configuration");
                            configEditor.setWidth(800);
                            configEditor.setHeight(800);
                            configEditor.setIsModal(true);
                            configEditor.setShowModalMask(true);
                            configEditor.centerInPage();
//                            configEditor.setShowResizeBar = 
                            configEditor.addItem(new ConfigurationEditor(resourceId, resourceTypeId, ConfigurationEditor.ConfigType.plugin));
                            configEditor.show();

                        }
                    });
                    editPluginConfiguration.setEnabled(node.getResource().getResourceType().getPluginConfigurationDefinition() != null);
                    contextMenu.addItem(editPluginConfiguration);


                    MenuItem editResourceConfiguration = new MenuItem("Resource Configuration");
                    editResourceConfiguration.addClickHandler(new ClickHandler() {
                        public void onClick(MenuItemClickEvent event) {
                            Resource resource = ((ResourceTreeDatasource.ResourceTreeNode) nodeContextClickEvent.getNode()).getResource();
                            int resourceId = ((ResourceTreeDatasource.ResourceTreeNode) nodeContextClickEvent.getNode()).getResource().getId();
                            int resourceTypeId = ((ResourceTreeDatasource.ResourceTreeNode) nodeContextClickEvent.getNode()).getResourceType().getId();

                            final Window configEditor = new Window();
                            configEditor.setTitle("Edit " + resource.getName() + " resource configuration");
                            configEditor.setWidth(800);
                            configEditor.setHeight(800);
                            configEditor.setIsModal(true);
                            configEditor.setShowModalMask(true);
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

                    MenuItem operations = new MenuItem("Operations");
                    Menu opSubMenu = new Menu();
                    for (OperationDefinition operationDefinition : node.getResourceType().getOperationDefinitions()) {
                        opSubMenu.addItem(new MenuItem(operationDefinition.getDisplayName()));

                    }
                    operations.setEnabled(!node.getResourceType().getOperationDefinitions().isEmpty());
                    operations.setSubmenu(opSubMenu);
                    contextMenu.addItem(operations);

                }


                contextMenu.showContextMenu();
            }
        });


        addMember(resourceTree);

    }


  
}
