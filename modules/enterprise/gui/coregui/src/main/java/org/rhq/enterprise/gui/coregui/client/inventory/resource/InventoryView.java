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
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;
import com.smartgwt.client.widgets.tree.Tree;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeNode;

import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ViewId;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.ResourceGroupListView;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.definitions.GroupDefinitionListView;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableTreeGrid;

/**
 * @author Greg Hinkle
 */
public class InventoryView extends LocatableHLayout implements BookmarkableView {

    public static final String VIEW_PATH = "Inventory";

    private static final String SECTION_GROUPS = "Groups";
    private static final String SECTION_RESOURCES = "Resources";

    private static final String SUBSECTION_INVENTORY = "Inventory";
    private static final String SUBSECTION_SAVED_SEARCHES = "Saved Searches";

    private static final String PAGE_COMPATIBLE_GROUPS = "Compatible Groups";
    private static final String PAGE_DOWN = "Down Servers";
    private static final String PAGE_GROUPS = "All Groups";
    private static final String PAGE_GROUP_DEFINITIONS = "Group Definitions";
    private static final String PAGE_MIXED_GROUPS = "Mixed Groups";
    private static final String PAGE_PLATFORMS = "Platforms";
    private static final String PAGE_PROBLEM_GROUPS = "Problem Groups";
    private static final String PAGE_RESOURCES = "All Resources";
    private static final String PAGE_SERVERS = "Servers";
    private static final String PAGE_SERVICES = "Services";

    private ViewId currentSectionViewId;
    private ViewId currentPageViewId;

    private Canvas contentCanvas;
    private Canvas currentContent;
    private LinkedHashMap<String, TreeGrid> treeGrids = new LinkedHashMap<String, TreeGrid>();

    private SectionStack sectionStack;

    public InventoryView(String locatorId) {
        super(locatorId);
    }

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
        ResourceSearchView searchView = new ResourceSearchView(extendLocatorId("Inventory"));
        searchView.addResourceSelectedListener(new ResourceSelectListener() {
            public void onResourceSelected(ResourceComposite resourceComposite) {
                //CoreGUI.setContent(new ResourceView(resource));
                CoreGUI.goTo("Resource/" + resourceComposite.getResource().getId());
            }
        });
        return searchView;
    }

    private SectionStackSection buildResourcesSection() {
        final SectionStackSection section = new SectionStackSection(SECTION_RESOURCES);
        section.setExpanded(true);

        final TreeNode allResources = new TreeNode(PAGE_RESOURCES);
        final TreeNode onlyPlatforms = new TreeNode(PAGE_PLATFORMS);
        onlyPlatforms.setIcon("types/Platform_up_16.png");

        final TreeNode onlyServers = new TreeNode(PAGE_SERVERS);
        onlyServers.setIcon("types/Server_up_16.png");

        final TreeNode onlyServices = new TreeNode(PAGE_SERVICES);
        onlyServices.setIcon("types/Service_up_16.png");

        final TreeNode inventory = new TreeNode(SUBSECTION_INVENTORY, allResources, onlyPlatforms, onlyServers,
            onlyServices);

        final TreeNode downServers = new TreeNode(PAGE_DOWN);
        downServers.setIcon("types/Server_down_16.png");

        final TreeNode savedSearches = new TreeNode(SUBSECTION_SAVED_SEARCHES, downServers);

        TreeGrid treeGrid = new LocatableTreeGrid(SECTION_RESOURCES);
        treeGrid.setShowHeader(false);
        Tree tree = new Tree();
        tree.setRoot(new TreeNode(SECTION_RESOURCES, inventory, savedSearches));
        treeGrid.setData(tree);

        treeGrid.getTree().openAll();
        treeGrids.put(SECTION_RESOURCES, treeGrid);

        section.addItem(treeGrid);

        return section;
    }

    private SectionStackSection buildGroupsSection() {
        final SectionStackSection section = new SectionStackSection(SECTION_GROUPS);
        section.setExpanded(true);

        final TreeNode allGroups = new TreeNode(PAGE_GROUPS);
        final TreeNode onlyCompatible = new TreeNode(PAGE_COMPATIBLE_GROUPS);
        final TreeNode onlyMixed = new TreeNode(PAGE_MIXED_GROUPS);
        final TreeNode groupGroupDefinitions = new TreeNode(PAGE_GROUP_DEFINITIONS);
        final TreeNode inventory = new TreeNode(SECTION_GROUPS, allGroups, onlyCompatible, onlyMixed,
            groupGroupDefinitions);

        final TreeNode problemGroups = new TreeNode(PAGE_PROBLEM_GROUPS);
        final TreeNode savedSearches = new TreeNode(SUBSECTION_SAVED_SEARCHES, problemGroups);

        TreeGrid treeGrid = new LocatableTreeGrid(SECTION_GROUPS);
        treeGrid.setShowHeader(false);
        Tree tree = new Tree();
        tree.setRoot(new TreeNode(SECTION_GROUPS, inventory, savedSearches));
        treeGrid.setData(tree);

        treeGrid.getTree().openAll();
        treeGrids.put(SECTION_GROUPS, treeGrid);
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
        if (SECTION_RESOURCES.equals(section)) {

            if (PAGE_RESOURCES.equals(page)) {
                content = new ResourceSearchView(extendLocatorId("AllResources"), null, PAGE_RESOURCES);
            } else if (PAGE_PLATFORMS.equals(page)) {
                content = new ResourceSearchView(extendLocatorId("Platforms"), new Criteria(
                    ResourceDataSourceField.CATEGORY.propertyName(), ResourceCategory.PLATFORM.name()), PAGE_PLATFORMS);
            } else if (PAGE_SERVERS.equals(page)) {
                content = new ResourceSearchView(extendLocatorId("Servers"), new Criteria(
                    ResourceDataSourceField.CATEGORY.propertyName(), ResourceCategory.SERVER.name()), PAGE_SERVERS);
            } else if (PAGE_SERVICES.equals(page)) {
                content = new ResourceSearchView(extendLocatorId("Services"), new Criteria(
                    ResourceDataSourceField.CATEGORY.propertyName(), ResourceCategory.SERVICE.name()), PAGE_SERVICES);
            } else if (PAGE_DOWN.equals(page)) {
                Criteria criteria = new Criteria(ResourceDataSourceField.AVAILABILITY.propertyName(),
                    ResourceCategory.PLATFORM.name());
                criteria.addCriteria(ResourceDataSourceField.CATEGORY.propertyName(), ResourceCategory.SERVER.name());
                content = new ResourceSearchView(extendLocatorId("DownResources"), criteria, PAGE_DOWN);
            }

        } else if (SECTION_GROUPS.equals(section)) {

            if (PAGE_GROUPS.equals(page)) {
                content = new ResourceGroupListView(extendLocatorId("AllGroups"));
            } else if (PAGE_COMPATIBLE_GROUPS.equals(page)) {
                content = new ResourceGroupListView(extendLocatorId("Compatible"), new Criteria("category",
                    "compatible"), PAGE_COMPATIBLE_GROUPS);
            } else if (PAGE_MIXED_GROUPS.equals(page)) {
                content = new ResourceGroupListView(extendLocatorId("Mixed"), new Criteria("category", "mixed"),
                    PAGE_MIXED_GROUPS);
            } else if (PAGE_GROUP_DEFINITIONS.equals(page)) {
                content = new GroupDefinitionListView(extendLocatorId("Definitions"));
            } else if (PAGE_PROBLEM_GROUPS.equals(page)) {
                content = new ResourceGroupListView(extendLocatorId("DownGroups"),
                    new Criteria("availability", "down"), PAGE_PROBLEM_GROUPS);
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
