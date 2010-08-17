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

import java.util.LinkedHashMap;

import com.google.gwt.user.client.History;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.events.SelectionChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionEvent;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;
import com.smartgwt.client.widgets.tree.Tree;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeNode;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ViewId;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.ResourceGroupListView;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.definitions.GroupDefinitionListView;

/**
 * @author Greg Hinkle
 */
public class InventoryView extends HLayout implements BookmarkableView {

    public static final String VIEW_PATH = "Inventory";

    private ViewId currentSectionViewId;
    private ViewId currentPageViewId;

    private Canvas contentCanvas;
    private Canvas currentContent;
    private LinkedHashMap<String, TreeGrid> treeGrids = new LinkedHashMap<String, TreeGrid>();

    private SectionStack sectionStack;


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

        buildResourcesSection();
        buildGroupsSection();

        for (final String name : treeGrids.keySet()) {
            TreeGrid grid = treeGrids.get(name);

            grid.addSelectionChangedHandler(new SelectionChangedHandler() {
                public void onSelectionChanged(SelectionEvent selectionEvent) {
                    if (selectionEvent.getState()) {
                        CoreGUI.goTo("Inventory/" + name + "/" + selectionEvent.getRecord().getAttribute("name"));
                    }
                }
            });


            SectionStackSection section = new SectionStackSection(name);
            section.setExpanded(true);
            section.addItem(grid);

            sectionStack.addSection(section);
        }


        addMember(sectionStack);
        addMember(contentCanvas);

        setContent(buildResourceSearchView());
    }

    private ResourceSearchView buildResourceSearchView() {
        ResourceSearchView searchView = new ResourceSearchView();
        searchView.addResourceSelectedListener(new ResourceSelectListener() {
            public void onResourceSelected(ResourceComposite resourceComposite) {
                //CoreGUI.setContent(new ResourceView(resource));
                CoreGUI.goTo("Resource/" + resourceComposite.getResource().getId());
            }
        });
        return searchView;
    }

    private SectionStackSection buildResourcesSection() {
        final SectionStackSection section = new SectionStackSection("Resources");
        section.setExpanded(true);

        final TreeNode allResources = new TreeNode("All Resources");
        final TreeNode onlyPlatforms = new TreeNode("Platforms");
        onlyPlatforms.setIcon("types/Platform_up_16.png");

        final TreeNode onlyServers = new TreeNode("Servers");
        onlyServers.setIcon("types/Server_up_16.png");

        final TreeNode onlyServices = new TreeNode("Services");
        onlyServices.setIcon("types/Service_up_16.png");

        final TreeNode inventory = new TreeNode("Inventory", allResources, onlyPlatforms, onlyServers, onlyServices);

        final TreeNode downServers = new TreeNode("Down Servers");
        downServers.setIcon("types/Server_down_16.png");

        final TreeNode savedSearches = new TreeNode("Saved Searches", downServers);

        TreeGrid treeGrid = new TreeGrid();
        treeGrid.setShowHeader(false);
        Tree tree = new Tree();
        tree.setRoot(new TreeNode("security", inventory, savedSearches));
        treeGrid.setData(tree);

        treeGrid.getTree().openAll();
        treeGrids.put("Resources", treeGrid);

        section.addItem(treeGrid);

        return section;
    }

    private SectionStackSection buildGroupsSection() {
        final SectionStackSection section = new SectionStackSection("Groups");
        section.setExpanded(true);

        final TreeNode allGroups = new TreeNode("All Groups");
        final TreeNode onlyCompatible = new TreeNode("Compatible Groups");
        final TreeNode onlyMixed = new TreeNode("Mixed Groups");
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


        treeGrid.getTree().openAll();
        treeGrids.put("Groups", treeGrid);
        section.addItem(treeGrid);

        return section;
    }

    public void setContent(Canvas newContent) {

        if (contentCanvas.getChildren().length > 0) {
            for (Canvas child : contentCanvas.getChildren()) {
                child.destroy();
            }
        }

        contentCanvas.addChild(newContent);
        contentCanvas.markForRedraw();
        this.currentContent = newContent;
    }


    private void renderContentView(ViewPath viewPath) {

        currentSectionViewId = viewPath.getCurrent();
        currentPageViewId = viewPath.getNext();

        String section = currentSectionViewId.getPath();
        String page = currentPageViewId.getPath();

        Canvas content = null;
        if ("Resources".equals(section)) {

            if ("All Resources".equals(page)) {
                content = new ResourceSearchView();
            } else if ("Platforms".equals(page)) {
                content = new ResourceSearchView(new Criteria(ResourceDataSourceField.CATEGORY.propertyName(), ResourceCategory.PLATFORM.name()));
            } else if ("Servers".equals(page)) {
                content = new ResourceSearchView(new Criteria(ResourceDataSourceField.CATEGORY.propertyName(), ResourceCategory.SERVER.name()));
            } else if ("Services".equals(page)) {
                content = new ResourceSearchView(new Criteria(ResourceDataSourceField.CATEGORY.propertyName(), ResourceCategory.SERVICE.name()));
            } else if ("Down Servers".equals(page)) {

                Criteria criteria = new Criteria(ResourceDataSourceField.AVAILABILITY.propertyName(), AvailabilityType.DOWN.name());
                criteria.addCriteria(ResourceDataSourceField.CATEGORY.propertyName(), ResourceCategory.SERVER.name());
                content = new ResourceSearchView(criteria);
            }

        } else if ("Groups".equals(section)) {

            if ("All Groups".equals(page)) {
                content = new ResourceGroupListView();
            } else if ("Compatible Groups".equals(page)) {
                content = new ResourceGroupListView(new Criteria("category", "compatible"));
            } else if ("Mixed Groups".equals(page)) {
                content = new ResourceGroupListView(new Criteria("category", "mixed"));
            } else if ("Group Definitions".equals(page)) {
                content = new GroupDefinitionListView();
            } else if ("Problem Groups".equals(page)) {
                content = new ResourceGroupListView(new Criteria("availability", "down"));
            }
        }


        for (String name : treeGrids.keySet()) {

            TreeGrid treeGrid = treeGrids.get(name);
            if (name.equals(section)) {
                for (TreeNode node : treeGrid.getTree().getAllNodes()) {
                    if (page.equals(node.getName())) {
                        treeGrid.selectSingleRecord(node);
                    }
                }
            } else {
                treeGrid.deselectAllRecords();
            }
        }


        setContent(content);


        if (content instanceof BookmarkableView) {
            ((BookmarkableView) content).renderView(viewPath.next().next());
        }


    }

    public void renderView(ViewPath viewPath) {

        if (!viewPath.isCurrent(currentSectionViewId) || !viewPath.isNext(currentPageViewId)) {

            if (viewPath.isEnd()) {
                // Display default view
                History.newItem("Inventory/Resources/Platforms");
            } else {

                renderContentView(viewPath);
            }
        } else {
            if (this.currentContent instanceof BookmarkableView) {
                ((BookmarkableView) this.currentContent).renderView(viewPath.next().next());
            }

        }


    }
}
