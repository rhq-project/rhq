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

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
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

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ViewId;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.ResourceGroupListView;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.definitions.GroupDefinitionListView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.discovery.ResourceAutodiscoveryView;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableSectionStack;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableTreeGrid;

/**
 * @author Greg Hinkle
 * @author Joseph Marques
 */
public class InventoryView extends LocatableHLayout implements BookmarkableView {
    public static final String VIEW_ID = "Inventory";

    private static final String SECTION_GROUPS = "Groups";
    private static final String SECTION_RESOURCES = "Resources";

    private static final String SUBSECTION_RESOURCE_INVENTORY = "Resources";
    private static final String SUBSECTION_GROUP_INVENTORY = "Groups";
    private static final String SUBSECTION_SAVED_SEARCHES = "Saved Searches";

    private static final String PAGE_ADQ = "Discovery Manager";
    private static final String PAGE_COMPATIBLE_GROUPS = "Compatible Groups";
    private static final String PAGE_DOWN = "Down Servers";
    private static final String PAGE_GROUPS = "All Groups";
    private static final String PAGE_GROUP_DEFINITIONS = "DynaGroup Manager";
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
    private Map<String, TreeGrid> treeGrids = new LinkedHashMap<String, TreeGrid>();

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

        sectionStack = new LocatableSectionStack(getLocatorId());
        sectionStack.setShowResizeBar(true);
        sectionStack.setVisibilityMode(VisibilityMode.MULTIPLE);
        sectionStack.setWidth(250);
        sectionStack.setHeight100();

        GWTServiceLookup.getAuthorizationService().getExplicitGlobalPermissions(new AsyncCallback<Set<Permission>>() {
            @Override
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("Could not determine user's global permissions, assuming none",
                    caught);
                finishOnInit(EnumSet.noneOf(Permission.class));
            }

            @Override
            public void onSuccess(Set<Permission> result) {
                finishOnInit(result);
            }
        });
    }

    private void finishOnInit(Set<Permission> globalPermissions) {
        buildResourcesSection(globalPermissions);
        buildGroupsSection(globalPermissions);

        for (final String sectionName : treeGrids.keySet()) {
            TreeGrid grid = treeGrids.get(sectionName);

            grid.addSelectionChangedHandler(new SelectionChangedHandler() {
                public void onSelectionChanged(SelectionEvent selectionEvent) {
                    if (selectionEvent.getState()) {
                        String pageName = selectionEvent.getRecord().getAttribute("name");
                        String viewPath = "Inventory/" + sectionName + "/" + pageName;
                        String currentViewPath = History.getToken();
                        if (!currentViewPath.startsWith(viewPath)) {
                            CoreGUI.goToView(viewPath);
                        }
                    }
                }
            });

            SectionStackSection section = new SectionStackSection(sectionName);
            section.setExpanded(true);
            section.addItem(grid);

            sectionStack.addSection(section);
        }

        addMember(sectionStack);
        addMember(contentCanvas);
    }

    private SectionStackSection buildResourcesSection(Set<Permission> globalPermissions) {

        final SectionStackSection section = new SectionStackSection(SECTION_RESOURCES);
        section.setExpanded(true);

        final TreeNode discoveryQueue = new TreeNode(PAGE_ADQ);
        discoveryQueue.setEnabled(globalPermissions.contains(Permission.MANAGE_INVENTORY));
        discoveryQueue.setIcon("global/Recent_16.png");

        final TreeNode onlyPlatforms = new TreeNode(PAGE_PLATFORMS);
        onlyPlatforms.setIcon("types/Platform_up_16.png");

        final TreeNode onlyServers = new TreeNode(PAGE_SERVERS);
        onlyServers.setIcon("types/Server_up_16.png");

        final TreeNode onlyServices = new TreeNode(PAGE_SERVICES);
        onlyServices.setIcon("types/Service_up_16.png");

        final TreeNode inventory = new TreeNode(SUBSECTION_RESOURCE_INVENTORY, onlyPlatforms, onlyServers, onlyServices);

        final TreeNode downServers = new TreeNode(PAGE_DOWN);
        downServers.setIcon("types/Server_down_16.png");

        final TreeNode savedSearches = new TreeNode(SUBSECTION_SAVED_SEARCHES, downServers);

        TreeGrid treeGrid = new LocatableTreeGrid(SECTION_RESOURCES);
        treeGrid.setShowHeader(false);
        Tree tree = new Tree();
        tree.setRoot(new TreeNode(SECTION_RESOURCES, discoveryQueue, inventory, savedSearches));
        treeGrid.setData(tree);

        treeGrid.getTree().openAll();
        treeGrids.put(SECTION_RESOURCES, treeGrid);

        section.addItem(treeGrid);

        return section;
    }

    private SectionStackSection buildGroupsSection(Set<Permission> globalPermissions) {
        final SectionStackSection section = new SectionStackSection(SECTION_GROUPS);
        section.setExpanded(true);

        final TreeNode groupGroupDefinitions = new TreeNode(PAGE_GROUP_DEFINITIONS);
        groupGroupDefinitions.setEnabled(globalPermissions.contains(Permission.MANAGE_INVENTORY));
        groupGroupDefinitions.setIcon("types/GroupDefinition_16.png");

        final TreeNode onlyCompatible = new TreeNode(PAGE_COMPATIBLE_GROUPS);
        onlyCompatible.setIcon("types/Cluster_up_16.png");
        final TreeNode onlyMixed = new TreeNode(PAGE_MIXED_GROUPS);
        onlyMixed.setIcon("types/Group_up_16.png");

        final TreeNode inventory = new TreeNode(SUBSECTION_GROUP_INVENTORY, onlyCompatible, onlyMixed);

        final TreeNode problemGroups = new TreeNode(PAGE_PROBLEM_GROUPS);
        final TreeNode savedSearches = new TreeNode(SUBSECTION_SAVED_SEARCHES, problemGroups);

        TreeGrid treeGrid = new LocatableTreeGrid(SECTION_GROUPS);
        treeGrid.setShowHeader(false);
        Tree tree = new Tree();
        tree.setRoot(new TreeNode(SECTION_GROUPS, groupGroupDefinitions, inventory, savedSearches));
        treeGrid.setData(tree);

        treeGrid.getTree().openAll();
        treeGrids.put(SECTION_GROUPS, treeGrid);
        section.addItem(treeGrid);

        return section;
    }

    public void setContent(Canvas newContent) {
        for (Canvas child : this.contentCanvas.getChildren()) {
            child.destroy();
        }
        this.contentCanvas.addChild(newContent);
        this.contentCanvas.markForRedraw();
        this.currentContent = newContent;
    }

    private void renderContentView(ViewPath viewPath) {

        currentSectionViewId = viewPath.getCurrent();
        currentPageViewId = viewPath.getNext();

        String section = currentSectionViewId.getPath();
        String page = currentPageViewId.getPath();

        Canvas content = null;
        if (SECTION_RESOURCES.equals(section)) {
            if (PAGE_PLATFORMS.equals(page)) {
                content = new ResourceSearchView(extendLocatorId("Platforms"), new Criteria(
                    ResourceDataSourceField.CATEGORY.propertyName(), ResourceCategory.PLATFORM.name()), PAGE_PLATFORMS,
                    "types/Platform_up_24.png");
            } else if (PAGE_SERVERS.equals(page)) {
                content = new ResourceSearchView(extendLocatorId("Servers"), new Criteria(
                    ResourceDataSourceField.CATEGORY.propertyName(), ResourceCategory.SERVER.name()), PAGE_SERVERS,
                    "types/Server_up_24.png");
            } else if (PAGE_SERVICES.equals(page)) {
                content = new ResourceSearchView(extendLocatorId("Services"), new Criteria(
                    ResourceDataSourceField.CATEGORY.propertyName(), ResourceCategory.SERVICE.name()), PAGE_SERVICES,
                    "types/Service_up_24.png");
            } else if (PAGE_ADQ.equals(page)) {
                content = new ResourceAutodiscoveryView(this.extendLocatorId("ADQ"));
            } else if (PAGE_DOWN.equals(page)) {
                Criteria criteria = new Criteria(ResourceDataSourceField.AVAILABILITY.propertyName(),
                    AvailabilityType.DOWN.name());
                criteria.addCriteria(ResourceDataSourceField.CATEGORY.propertyName(), ResourceCategory.SERVER.name());
                content = new ResourceSearchView(extendLocatorId("DownResources"), criteria, PAGE_DOWN);
            } else { // selected the Inventory node itself
                content = new ResourceSearchView(extendLocatorId("AllResources"), null, PAGE_RESOURCES,
                    "types/Platform_up_24.png", "types/Server_up_24.png", "types/Service_up_24.png");
            }

        } else if (SECTION_GROUPS.equals(section)) {
            if (PAGE_COMPATIBLE_GROUPS.equals(page)) {
                content = new ResourceGroupListView(extendLocatorId("Compatible"), new Criteria("category",
                    "compatible"), PAGE_COMPATIBLE_GROUPS, "types/Cluster_up_24.png");
            } else if (PAGE_MIXED_GROUPS.equals(page)) {
                content = new ResourceGroupListView(extendLocatorId("Mixed"), new Criteria("category", "mixed"),
                    PAGE_MIXED_GROUPS, "types/Group_up_24.png");
            } else if (PAGE_GROUP_DEFINITIONS.equals(page)) {
                content = new GroupDefinitionListView(extendLocatorId("Definitions"), "types/GroupDefinition_16.png");
            } else if (PAGE_PROBLEM_GROUPS.equals(page)) {
                //TODO - there is no underlying support for this criteria. Also, there should not be an active
                // new button on this page.
                content = new ResourceGroupListView(extendLocatorId("DownGroups"),
                    new Criteria("availability", "down"), PAGE_PROBLEM_GROUPS);
            } else { // selected the Inventory node itself
                content = new ResourceGroupListView(extendLocatorId("AllGroups"), null, PAGE_GROUPS,
                    "types/Cluster_up_24.png", "types/Group_up_24.png");
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

        // ignore clicks on subsection folder nodes
        if (null != content) {
            setContent(content);

            if (content instanceof BookmarkableView) {
                ((BookmarkableView) content).renderView(viewPath.next().next());
            }
        }

    }

    public void renderView(ViewPath viewPath) {
        if (!viewPath.isCurrent(currentSectionViewId) || !viewPath.isNext(currentPageViewId)) {
            if (viewPath.isEnd()) {
                // i.e. "Inventory"
                // Currently, leave content empty until the user selects something from section stack. To default
                // to platform list uncomment following line.
                // History.newItem("Inventory/Resources/Platforms", true);
            } else {
                // e.g. Inventory/Administration"
                renderContentView(viewPath);
            }
        } else {
            if (this.currentContent instanceof BookmarkableView) {
                ((BookmarkableView) this.currentContent).renderView(viewPath.next().next());
            }
        }
    }

}
