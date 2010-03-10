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

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.types.ContentsType;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLPane;
import com.smartgwt.client.widgets.grid.events.SelectionChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionEvent;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;
import com.smartgwt.client.widgets.tree.Tree;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeNode;

import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.ResourceGroupListView;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.definitions.GroupDefinitionListView;

/**
 * @author Greg Hinkle
 */
public class InventoryView extends HLayout {

    public static final String VIEW_PATH = "Inventory";

    private SectionStack sectionStack;
    private Canvas contentCanvas;

    @Override
    protected void onInit() {
        super.onInit();

        setWidth100();
        setHeight100();

        contentCanvas = new Canvas();
        contentCanvas.setWidth("*");
        contentCanvas.setHeight100();

        sectionStack = new SectionStack();
        sectionStack.setShowResizeBar(true);
        sectionStack.setVisibilityMode(VisibilityMode.MULTIPLE);
        sectionStack.setWidth(250);
        sectionStack.setHeight100();

        sectionStack.addSection(buildResourcesSection());
        sectionStack.addSection(buildGroupsSection());

        addMember(sectionStack);
        addMember(contentCanvas);

        setContent(buildResourceSearchView());
    }

    private ResourceSearchView buildResourceSearchView() {
        ResourceSearchView searchView = new ResourceSearchView();
        searchView.addResourceSelectedListener(new ResourceSelectListener() {
            public void onResourceSelected(Resource resource) {
                //CoreGUI.setContent(new ResourceView(resource));
                CoreGUI.goTo("Resource/" + resource.getId());
            }
        });
        return searchView;
    }

    private SectionStackSection buildResourcesSection() {
        final SectionStackSection section = new SectionStackSection("Inventory");
        section.setExpanded(true);

        final TreeNode allResources = new TreeNode("All Resources");
        final TreeNode onlyPlatforms = new TreeNode("Platforms");
        final TreeNode onlyServers = new TreeNode("Servers");
        final TreeNode onlyServices = new TreeNode("Services");
        final TreeNode inventory = new TreeNode("Inventory", allResources, onlyPlatforms, onlyServers, onlyServices);

        final TreeNode downServers = new TreeNode("Down Servers");
        final TreeNode savedSearches = new TreeNode("Saved Searches", downServers);

        TreeGrid treeGrid = new TreeGrid();
        treeGrid.setShowHeader(false);
        Tree tree = new Tree();
        tree.setRoot(new TreeNode("security", inventory, savedSearches));
        treeGrid.setData(tree);
        treeGrid.addSelectionChangedHandler(new SelectionChangedHandler() {
            public void onSelectionChanged(SelectionEvent selectionEvent) {
                if (selectionEvent.getRecord() == allResources) {
                    setContent(new ResourceSearchView());
                } else if (selectionEvent.getRecord() == onlyPlatforms) {
                    setContent(new ResourceSearchView(new Criteria("category", "platform")));
                } else if (selectionEvent.getRecord() == onlyServers) {
                    setContent(new ResourceSearchView(new Criteria("category", "server")));
                } else if (selectionEvent.getRecord() == onlyServices) {
                    setContent(new ResourceSearchView(new Criteria("category", "service")));
                } else if (selectionEvent.getRecord() == downServers) {
                    setContent(new ResourceSearchView(new Criteria("availability", "down")));
                }

            }
        });

        section.addItem(treeGrid);

        return section;
    }

    private SectionStackSection buildGroupsSection() {
        final SectionStackSection section = new SectionStackSection("Groups");
        section.setExpanded(true);

        final TreeNode allGroups = new TreeNode("All Groups");
        final TreeNode onlyCompatible = new TreeNode("Compatible");
        final TreeNode onlyMixed = new TreeNode("Mixed");
        final TreeNode groupGroupDefinitions = new TreeNode("Group Definitions");
        final TreeNode inventory = new TreeNode("Inventory", allGroups, onlyCompatible, onlyMixed,
            groupGroupDefinitions);

        final TreeNode problemGroups = new TreeNode("Problem Groups");
        final TreeNode savedSearches = new TreeNode("Saved Searches", problemGroups);

        TreeGrid treeGrid = new TreeGrid();
        treeGrid.setShowHeader(false);
        Tree tree = new Tree();
        tree.setRoot(new TreeNode("clustering", inventory, savedSearches));
        treeGrid.setData(tree);
        treeGrid.addSelectionChangedHandler(new SelectionChangedHandler() {
            public void onSelectionChanged(SelectionEvent selectionEvent) {
                HTMLPane pane = new HTMLPane();
                pane.setContentsType(ContentsType.PAGE);
                pane.setWidth100();
                pane.setHeight100();

                String url = null;
                if (selectionEvent.getRecord() == allGroups) {
                    setContent(new ResourceGroupListView());
                } else if (selectionEvent.getRecord() == onlyCompatible) {
                    setContent(new ResourceGroupListView(new Criteria("category", "compatible")));
                } else if (selectionEvent.getRecord() == onlyMixed) {
                    setContent(new ResourceGroupListView(new Criteria("category", "mixed")));
                } else if (selectionEvent.getRecord() == groupGroupDefinitions) {
                    setContent(new GroupDefinitionListView());
                } else if (selectionEvent.getRecord() == problemGroups) {
                    setContent(new ResourceGroupListView(new Criteria("availability", "down")));
                }

            }
        });

        section.addItem(treeGrid);

        return section;
    }

    public void setContent(Canvas newContent) {
        if (contentCanvas.getChildren().length > 0)
            contentCanvas.getChildren()[0].destroy();
        newContent.setWidth100();
        newContent.setHeight100();
        contentCanvas.addChild(newContent);
        contentCanvas.draw();
    }
}
