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
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ViewId;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.tree.EnhancedTreeNode;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.ResourceGroupDataSourceField;
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

    private static final String GROUPS_SECTION_VIEW_ID = "Groups";
    private static final String RESOURCES_SECTION_VIEW_ID = "Resources";

    private static final String SUBSECTION_RESOURCE_INVENTORY = "Resources";
    private static final String SUBSECTION_GROUP_INVENTORY = "Groups";
    private static final String SUBSECTION_SAVED_SEARCHES = "SavedSearches";

    private static final String PAGE_ADQ = "DiscoveryManager";
    private static final String PAGE_COMPATIBLE_GROUPS = "CompatibleGroups";
    private static final String PAGE_DOWN = "DownServers";
    private static final String PAGE_GROUPS = "AllGroups";
    private static final String PAGE_GROUP_DEFINITIONS = "DynagroupManager";
    private static final String PAGE_MIXED_GROUPS = "MixedGroups";
    private static final String PAGE_PLATFORMS = "Platforms";
    private static final String PAGE_PROBLEM_GROUPS = "ProblemGroups";
    private static final String PAGE_RESOURCES = "AllResources";
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
                CoreGUI.getErrorHandler().handleError("Could not determine user's global permissions - assuming none.",
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
        addSection(buildResourcesSection(globalPermissions));
        addSection(buildGroupsSection(globalPermissions));

        addMember(sectionStack);
        addMember(contentCanvas);
    }

    private void addSection(TreeGrid treeGrid) {
        final String sectionName = treeGrid.getTree().getRoot().getName();
        this.treeGrids.put(sectionName, treeGrid);

        treeGrid.addSelectionChangedHandler(new SelectionChangedHandler() {
            public void onSelectionChanged(SelectionEvent selectionEvent) {
                if (selectionEvent.getState()) {
                    TreeNode node = (TreeNode) selectionEvent.getRecord();
                    String pageName = node.getName();
                    String viewPath = InventoryView.VIEW_ID + "/" + sectionName + "/" + pageName;
                    String currentViewPath = History.getToken();
                    if (!currentViewPath.startsWith(viewPath)) {
                        CoreGUI.goToView(viewPath);
                    }
                }
            }
        });

        SectionStackSection section = new SectionStackSection(sectionName);
        section.setExpanded(true);
        section.addItem(treeGrid);

        this.sectionStack.addSection(section);
    }

    private TreeGrid buildResourcesSection(Set<Permission> globalPermissions) {
        final TreeNode discoveryQueue = new EnhancedTreeNode(PAGE_ADQ);
        discoveryQueue.setEnabled(globalPermissions.contains(Permission.MANAGE_INVENTORY));
        discoveryQueue.setIcon("global/Recent_16.png");

        final TreeNode onlyPlatforms = new EnhancedTreeNode(PAGE_PLATFORMS);
        onlyPlatforms.setIcon("types/Platform_up_16.png");

        final TreeNode onlyServers = new EnhancedTreeNode(PAGE_SERVERS);
        onlyServers.setIcon("types/Server_up_16.png");

        final TreeNode onlyServices = new EnhancedTreeNode(PAGE_SERVICES);
        onlyServices.setIcon("types/Service_up_16.png");

        final TreeNode inventory = new EnhancedTreeNode(SUBSECTION_RESOURCE_INVENTORY, onlyPlatforms, onlyServers,
            onlyServices);

        final TreeNode downServers = new EnhancedTreeNode(PAGE_DOWN);
        downServers.setIcon("types/Server_down_16.png");

        final TreeNode savedSearches = new EnhancedTreeNode(SUBSECTION_SAVED_SEARCHES, downServers);

        TreeGrid treeGrid = new LocatableTreeGrid(RESOURCES_SECTION_VIEW_ID);
        treeGrid.setShowHeader(false);
        Tree tree = new Tree();
        TreeNode rootNode = new TreeNode(RESOURCES_SECTION_VIEW_ID, discoveryQueue, inventory, savedSearches);
        tree.setRoot(rootNode);
        treeGrid.setData(tree);

        treeGrid.getTree().openAll();
        treeGrids.put(RESOURCES_SECTION_VIEW_ID, treeGrid);

        return treeGrid;
    }

    private TreeGrid buildGroupsSection(Set<Permission> globalPermissions) {
        final TreeNode groupGroupDefinitions = new EnhancedTreeNode(PAGE_GROUP_DEFINITIONS);
        groupGroupDefinitions.setEnabled(globalPermissions.contains(Permission.MANAGE_INVENTORY));
        groupGroupDefinitions.setIcon("types/GroupDefinition_16.png");

        final TreeNode onlyCompatible = new EnhancedTreeNode(PAGE_COMPATIBLE_GROUPS);
        onlyCompatible.setIcon("types/Cluster_up_16.png");
        final TreeNode onlyMixed = new EnhancedTreeNode(PAGE_MIXED_GROUPS);
        onlyMixed.setIcon("types/Group_up_16.png");

        final TreeNode inventory = new EnhancedTreeNode(SUBSECTION_GROUP_INVENTORY, onlyCompatible, onlyMixed);

        final TreeNode problemGroups = new EnhancedTreeNode(PAGE_PROBLEM_GROUPS);
        problemGroups.setIcon("types/Cluster_down_16.png");
        final TreeNode savedSearches = new EnhancedTreeNode(SUBSECTION_SAVED_SEARCHES, problemGroups);

        TreeGrid treeGrid = new LocatableTreeGrid(GROUPS_SECTION_VIEW_ID);
        treeGrid.setShowHeader(false);
        Tree tree = new Tree();
        TreeNode rootNode = new EnhancedTreeNode(GROUPS_SECTION_VIEW_ID, groupGroupDefinitions, inventory,
            savedSearches);
        tree.setRoot(rootNode);
        treeGrid.setData(tree);

        treeGrid.getTree().openAll();
        treeGrids.put(GROUPS_SECTION_VIEW_ID, treeGrid);

        return treeGrid;
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

        String sectionName = currentSectionViewId.getPath();
        String pageName = currentPageViewId.getPath();

        Canvas content = null;
        if (RESOURCES_SECTION_VIEW_ID.equals(sectionName)) {
            if (PAGE_PLATFORMS.equals(pageName)) {
                content = new ResourceSearchView(extendLocatorId("Platforms"), new Criteria(
                    ResourceDataSourceField.CATEGORY.propertyName(), ResourceCategory.PLATFORM.name()), PAGE_PLATFORMS,
                    "types/Platform_up_24.png");
            } else if (PAGE_SERVERS.equals(pageName)) {
                content = new ResourceSearchView(extendLocatorId("Servers"), new Criteria(
                    ResourceDataSourceField.CATEGORY.propertyName(), ResourceCategory.SERVER.name()), PAGE_SERVERS,
                    "types/Server_up_24.png");
            } else if (PAGE_SERVICES.equals(pageName)) {
                content = new ResourceSearchView(extendLocatorId("Services"), new Criteria(
                    ResourceDataSourceField.CATEGORY.propertyName(), ResourceCategory.SERVICE.name()), PAGE_SERVICES,
                    "types/Service_up_24.png");
            } else if (PAGE_ADQ.equals(pageName)) {
                content = new ResourceAutodiscoveryView(this.extendLocatorId("ADQ"));
            } else if (PAGE_DOWN.equals(pageName)) {
                Criteria criteria = new Criteria(ResourceDataSourceField.AVAILABILITY.propertyName(),
                    AvailabilityType.DOWN.name());
                criteria.addCriteria(ResourceDataSourceField.CATEGORY.propertyName(), ResourceCategory.SERVER.name());
                content = new ResourceSearchView(extendLocatorId("DownResources"), criteria, PAGE_DOWN);
            } else { // selected the Inventory node itself
                content = new ResourceSearchView(extendLocatorId("AllResources"), null, PAGE_RESOURCES,
                    "types/Platform_up_24.png", "types/Server_up_24.png", "types/Service_up_24.png");
            }
        } else if (GROUPS_SECTION_VIEW_ID.equals(sectionName)) {
            if (PAGE_COMPATIBLE_GROUPS.equals(pageName)) {
                content = new ResourceGroupListView(extendLocatorId("Compatible"), new Criteria(
                    ResourceGroupDataSourceField.CATEGORY.propertyName(), GroupCategory.COMPATIBLE.name()),
                    "Compatible Groups", "types/Cluster_up_24.png");
            } else if (PAGE_MIXED_GROUPS.equals(pageName)) {
                content = new ResourceGroupListView(extendLocatorId("Mixed"), new Criteria(
                    ResourceGroupDataSourceField.CATEGORY.propertyName(), GroupCategory.MIXED.name()), "Mixed Groups",
                    "types/Group_up_24.png");
            } else if (PAGE_GROUP_DEFINITIONS.equals(pageName)) {
                content = new GroupDefinitionListView(extendLocatorId("Definitions"), "types/GroupDefinition_16.png");
            } else if (PAGE_PROBLEM_GROUPS.equals(pageName)) {
                //TODO - there is no underlying support for this criteria. Also, there should not be an active
                // new button on this page.
                content = new ResourceGroupListView(extendLocatorId("DownGroups"),
                    new Criteria("availability", "down"), "Problem Groups", "types/Cluster_down_16.png");
            } else { // selected the Inventory node itself
                content = new ResourceGroupListView(extendLocatorId("AllGroups"), null, "All Groups",
                    "types/Cluster_up_24.png", "types/Group_up_24.png");
            }
        }

        // when changing sections make sure the previous section's selection is deselected
        selectSectionPageTreeGridNode(sectionName, pageName);

        // ignore clicks on subsection folder nodes
        if (null != content) {
            setContent(content);

            if (content instanceof BookmarkableView) {
                ((BookmarkableView) content).renderView(viewPath.next().next());
            }
        }
    }

    private void selectSectionPageTreeGridNode(String sectionName, String pageName) {
        // TODO this method works, however, its getting invoked prior to treeGrids getting populated due to async authz check. need to fix that
        for (String name : treeGrids.keySet()) {
            TreeGrid treeGrid = treeGrids.get(name);
            if (!name.equals(sectionName)) {
                treeGrid.deselectAllRecords();
            } else {
                boolean gotIt = false;
                for (TreeNode node : treeGrid.getTree().getAllNodes()) {
                    if (node.getName().equals(pageName)) {
                        treeGrid.selectSingleRecord(node);
                        gotIt = true;
                        break;
                    }
                }
                if (!gotIt) {
                    CoreGUI.getErrorHandler().handleError("Unknown page name - URL is incorrect");
                }
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
