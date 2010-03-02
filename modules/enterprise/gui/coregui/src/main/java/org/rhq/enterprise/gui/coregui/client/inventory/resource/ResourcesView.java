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

import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.ResourceGroupListView;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.definitions.GroupDefinitionListView;

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

/**
 * @author Greg Hinkle
 */
public class ResourcesView extends HLayout {

    public static final String VIEW_PATH = "Resources";

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
        final SectionStackSection section = new SectionStackSection("Resources");
        section.setExpanded(true);

        TreeGrid treeGrid = new TreeGrid();
        treeGrid.setShowHeader(false);

        Tree tree = new Tree();
        final TreeNode savedSearches = new TreeNode("Saved Searches", new TreeNode("Down JBossAS (4)"));
        final TreeNode manageRolesNode = new TreeNode("Global Saved Searches");
        tree.setRoot(new TreeNode("security",
                savedSearches,
                manageRolesNode));

        treeGrid.setData(tree);


        treeGrid.addSelectionChangedHandler(new SelectionChangedHandler() {
            public void onSelectionChanged(SelectionEvent selectionEvent) {
                if (selectionEvent.getRecord() == savedSearches) {
                    setContent(new ResourceSearchView());
                } else if (selectionEvent.getRecord() == manageRolesNode) {
                    setContent(new ResourceSearchView());
                }
            }
        });


        section.addItem(treeGrid);


        return section;
    }


    private SectionStackSection buildGroupsSection() {
        final SectionStackSection section = new SectionStackSection("Resource Groups");
        section.setExpanded(true);

        TreeGrid treeGrid = new TreeGrid();
        treeGrid.setShowHeader(false);

        Tree tree = new Tree();
        final TreeNode groupsNode = new TreeNode("Groups");
        final TreeNode groupDefinitions = new TreeNode("DynaGroups");
        final TreeNode manageAffinityGroupsNode = new TreeNode("Special Groups");
        final TreeNode managePartitionEventsNode = new TreeNode("Greg's Groups");

        tree.setRoot(new TreeNode("clustering",
                groupsNode,
                groupDefinitions,
                manageAffinityGroupsNode,
                managePartitionEventsNode));

        treeGrid.setData(tree);


        treeGrid.addSelectionChangedHandler(new SelectionChangedHandler() {
            public void onSelectionChanged(SelectionEvent selectionEvent) {
                HTMLPane pane = new HTMLPane();
                pane.setContentsType(ContentsType.PAGE);
                pane.setWidth100();
                pane.setHeight100();

                String url = null;
                if (selectionEvent.getRecord() == groupsNode) {
                    setContent(new ResourceGroupListView());
                    return;
                } else if (selectionEvent.getRecord() == groupDefinitions) {
                    setContent(new GroupDefinitionListView());
                    return;
                } else if (selectionEvent.getRecord() == manageAffinityGroupsNode) {
                    url = "/rhq/ha/listAffinityGroups.xhtml";
                } else if (selectionEvent.getRecord() == managePartitionEventsNode) {
                    url = "/rhq/ha/listPartitionEvents.xhtml";
                }
                pane.setContentsURL(url + "?nomenu=true");
                setContent(pane);

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
