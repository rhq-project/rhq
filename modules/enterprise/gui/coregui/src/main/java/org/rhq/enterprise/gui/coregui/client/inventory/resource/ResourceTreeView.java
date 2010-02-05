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
package org.rhq.enterprise.gui.coregui.client.inventory.resource;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuItem;
import com.smartgwt.client.widgets.menu.MenuItemSeparator;
import com.smartgwt.client.widgets.menu.events.MenuItemClickEvent;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.events.NodeContextClickEvent;
import com.smartgwt.client.widgets.tree.events.NodeContextClickHandler;

/**
 * @author Greg Hinkle
 */
public class ResourceTreeView extends VLayout {

    public ResourceTreeView() {
        super();


        TreeGrid resourceTree = new TreeGrid();
        resourceTree.setWidth(500);
        resourceTree.setHeight100();
//        resourceTree.setNodeIcon("/images/icn_tree_Category_on.png");
//        resourceTree.setFolderIcon("/images/icn_tree_Category_on.png");
//        resourceTree.setShowOpenIcons(false);
//        resourceTree.setShowDropIcons(false);
//        resourceTree.setClosedIconSuffix("");
        resourceTree.setAutoFetchData(true);
        resourceTree.setAnimateFolders(false);

        resourceTree.setDataSource(new ResourceTreeDatasource());
//        ((Layout)treeTab.getPane()).add.addChild(resourceTree);

        final Menu contextMenu = new Menu();
        MenuItem item = new MenuItem("Expand node");
        item.addClickHandler(new com.smartgwt.client.widgets.menu.events.ClickHandler() {
            public void onClick(MenuItemClickEvent event) {
                TreeGrid treeGrid = (TreeGrid)event.getTarget();
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


//        contextMenu.addItem(item);
        resourceTree.setContextMenu(contextMenu);
        resourceTree.addNodeContextClickHandler(new NodeContextClickHandler() {
            public void onNodeContextClick(NodeContextClickEvent nodeContextClickEvent) {
                nodeContextClickEvent.getNode();
                contextMenu.setItems(new MenuItem(nodeContextClickEvent.getNode().getName()));
                if (nodeContextClickEvent.getNode() instanceof ResourceTreeDatasource.ResourceTreeNode) {
                    contextMenu.addItem(new MenuItem("Type: " + ((ResourceTreeDatasource.ResourceTreeNode)nodeContextClickEvent.getNode()).getResourceType().getName()));
                }
                contextMenu.addItem(new MenuItemSeparator());
                MenuItem operations = new MenuItem("Operations");
                Menu opSubMenu = new Menu();
                opSubMenu.setItems(new MenuItem("Start"), new MenuItem("Stop"), new MenuItem("Restart"));
                operations.setSubmenu(opSubMenu);
                contextMenu.addItem(operations);
                contextMenu.showContextMenu();
            }
        });


        addMember(resourceTree);

    }
}
