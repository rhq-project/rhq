/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can retribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is tributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.gui.coregui.client.admin;

import java.util.LinkedHashMap;
import java.util.Map;

import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.grid.events.CellClickEvent;
import com.smartgwt.client.widgets.grid.events.CellClickHandler;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;
import com.smartgwt.client.widgets.tree.Tree;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeNode;

import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ViewId;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.admin.agent.install.RemoteAgentInstallView;
import org.rhq.enterprise.gui.coregui.client.admin.roles.RolesView;
import org.rhq.enterprise.gui.coregui.client.admin.templates.ResourceTypeTreeView;
import org.rhq.enterprise.gui.coregui.client.admin.users.UsersView;
import org.rhq.enterprise.gui.coregui.client.components.FullHTMLPane;
import org.rhq.enterprise.gui.coregui.client.components.tree.EnhancedTreeNode;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableSectionStack;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableTreeGrid;

/**
 * @author Greg Hinkle
 */
public class AdministrationView extends LocatableHLayout implements BookmarkableView {
    public static final String VIEW_ID = "Administration";

    private static final String SECTION_SECURITY_VIEW_ID = "Security";
    private static final String SECTION_TOPOLOGY_VIEW_ID = "Topology";
    private static final String SECTION_CONFIGURATION_VIEW_ID = "Configuration";

    // TODO these iframe page view ids should go away in favor of the gwt view page view_id, when available
    private static final String PAGE_SERVERS_VIEW_ID = "Servers";
    private static final String PAGE_AGENTS_VIEW_ID = "Agents";
    private static final String PAGE_AFFINITY_GROUPS_VIEW_ID = "AffinityGroups";
    private static final String PAGE_PARTITION_EVENTS_VIEW_ID = "PartitionEvents";

    private static final String PAGE_SYSTEM_SETTINGS_VIEW_ID = "SystemSettings";
    private static final String PAGE_TEMPLATES_VIEW_ID = "Templates";
    private static final String PAGE_DOWNLOADS_VIEW_ID = "Downloads";
    private static final String PAGE_LICENSE_VIEW_ID = "License";
    private static final String PAGE_PLUGINS_VIEW_ID = "Plugins";

    private ViewId currentSectionViewId;
    private ViewId currentPageViewId;

    private SectionStack sectionStack;

    private Canvas contentCanvas;
    private Canvas currentContent;
    private Map<String, TreeGrid> treeGrids = new LinkedHashMap<String, TreeGrid>();

    public AdministrationView(String locatorId) {
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

        sectionStack = new LocatableSectionStack(this.getLocatorId());
        sectionStack.setShowResizeBar(true);
        sectionStack.setVisibilityMode(VisibilityMode.MULTIPLE);
        sectionStack.setWidth(250);
        sectionStack.setHeight100();

        addSection(buildSecuritySection());
        addSection(buildSystemConfigurationSection());
        addSection(buildTopologySection());

        addMember(sectionStack);
        addMember(contentCanvas);
    }

    private void addSection(final TreeGrid treeGrid) {
        final String sectionName = treeGrid.getTree().getRoot().getName();
        this.treeGrids.put(sectionName, treeGrid);

        treeGrid.addCellClickHandler(new CellClickHandler() {
            @Override
            public void onCellClick(CellClickEvent event) {
                // we use cell click as opposed to selected changed handler
                // because we want to be able to refresh even if clicking
                // on an already selected node
                TreeNode selectedRecord = (TreeNode) treeGrid.getSelectedRecord();
                if (selectedRecord != null) {
                    String pageName = selectedRecord.getName();
                    String viewPath = AdministrationView.VIEW_ID + "/" + sectionName + "/" + pageName;
                    CoreGUI.goToView(viewPath);
                }
            }
        });

        SectionStackSection section = new SectionStackSection(sectionName);
        section.setExpanded(true);
        section.addItem(treeGrid);

        this.sectionStack.addSection(section);
    }

    private HTMLFlow defaultView() {
        String contents = "<h1>Administration</h1>\n"
            + "From this section, the RHQ global settings can be administered. This includes configuring \n"
            + "<a href=\"\">Security</a>, setting up <a href=\"\">Plugins</a> and other stuff.";
        HTMLFlow flow = new HTMLFlow(contents);
        flow.setPadding(20);
        return flow;
    }

    private TreeGrid buildSecuritySection() {

        final TreeGrid securityTreeGrid = new LocatableTreeGrid(SECTION_SECURITY_VIEW_ID);
        securityTreeGrid.setLeaveScrollbarGap(false);
        securityTreeGrid.setShowHeader(false);

        Tree tree = new Tree();
        final TreeNode manageUsersNode = new EnhancedTreeNode(UsersView.VIEW_ID);
        manageUsersNode.setIcon("global/User_16.png");

        final TreeNode manageRolesNode = new EnhancedTreeNode(RolesView.VIEW_ID);
        manageRolesNode.setIcon("global/Role_16.png");

        final TreeNode remoteAgentInstall = new EnhancedTreeNode(RemoteAgentInstallView.VIEW_ID);
        remoteAgentInstall.setIcon("global/Agent_16.png");

        TreeNode rootNode = new EnhancedTreeNode(SECTION_SECURITY_VIEW_ID, manageUsersNode, manageRolesNode,
            remoteAgentInstall);
        tree.setRoot(rootNode);

        securityTreeGrid.setData(tree);

        return securityTreeGrid;
    }

    private TreeGrid buildTopologySection() {

        final TreeGrid mgmtClusterTreeGrid = new LocatableTreeGrid(SECTION_TOPOLOGY_VIEW_ID);
        mgmtClusterTreeGrid.setLeaveScrollbarGap(false);
        mgmtClusterTreeGrid.setShowHeader(false);

        Tree tree = new Tree();
        final TreeNode manageServersNode = new EnhancedTreeNode(PAGE_SERVERS_VIEW_ID);
        final TreeNode manageAgentsNode = new EnhancedTreeNode(PAGE_AGENTS_VIEW_ID);
        final TreeNode manageAffinityGroupsNode = new EnhancedTreeNode(PAGE_AFFINITY_GROUPS_VIEW_ID);
        final TreeNode managePartitionEventsNode = new EnhancedTreeNode(PAGE_PARTITION_EVENTS_VIEW_ID);

        TreeNode rootNode = new EnhancedTreeNode(SECTION_TOPOLOGY_VIEW_ID, manageServersNode, manageAgentsNode,
            manageAffinityGroupsNode, managePartitionEventsNode);
        tree.setRoot(rootNode);

        mgmtClusterTreeGrid.setData(tree);

        return mgmtClusterTreeGrid;
    }

    private TreeGrid buildSystemConfigurationSection() {

        final TreeGrid systemConfigTreeGrid = new LocatableTreeGrid(SECTION_CONFIGURATION_VIEW_ID);
        systemConfigTreeGrid.setLeaveScrollbarGap(false);
        systemConfigTreeGrid.setShowHeader(false);

        Tree tree = new Tree();
        final TreeNode manageSettings = new EnhancedTreeNode(PAGE_SYSTEM_SETTINGS_VIEW_ID);
        final TreeNode manageTemplates = new EnhancedTreeNode(PAGE_TEMPLATES_VIEW_ID);
        final TreeNode manageDownloads = new EnhancedTreeNode(PAGE_DOWNLOADS_VIEW_ID);
        final TreeNode manageLicense = new EnhancedTreeNode(PAGE_LICENSE_VIEW_ID);
        final TreeNode managePlugins = new EnhancedTreeNode(PAGE_PLUGINS_VIEW_ID);

        TreeNode rootNode = new EnhancedTreeNode(SECTION_CONFIGURATION_VIEW_ID, manageSettings, manageTemplates,
            manageDownloads, manageLicense, managePlugins);
        tree.setRoot(rootNode);

        systemConfigTreeGrid.setData(tree);

        return systemConfigTreeGrid;
    }

    public void setContent(Canvas newContent) {

        // A call to destroy (e.g. certain IFrames/FullHTMLPane) can actually remove multiple children of the
        // contentCanvas. As such, we need to query for the children after each destroy to ensure only valid children
        // are in the array.
        Canvas[] children;
        while ((children = contentCanvas.getChildren()).length > 0) {
            children[0].destroy();
        }

        contentCanvas.addChild(newContent);
        contentCanvas.markForRedraw();
        currentContent = newContent;
    }

    private void renderContentView(ViewPath viewPath) {
        currentSectionViewId = viewPath.getCurrent();
        currentPageViewId = viewPath.getNext();

        String sectionName = currentSectionViewId.getPath();
        String pageName = currentPageViewId.getPath();

        Canvas content = null;
        if (SECTION_SECURITY_VIEW_ID.equals(sectionName)) {

            if (UsersView.VIEW_ID.equals(pageName)) {
                content = new UsersView(this.extendLocatorId("Users"));
            } else if (RolesView.VIEW_ID.equals(pageName)) {
                content = new RolesView(this.extendLocatorId("Roles"));
            } else if (RemoteAgentInstallView.VIEW_ID.equals(pageName)) {
                content = new RemoteAgentInstallView(this.extendLocatorId("RemoteAgentInstall"));
            }
        } else if (SECTION_CONFIGURATION_VIEW_ID.equals(sectionName)) {

            String url = null;
            if (PAGE_SYSTEM_SETTINGS_VIEW_ID.equals(pageName)) {
                url = "/admin/config/Config.do?mode=edit";
            } else if (PAGE_TEMPLATES_VIEW_ID.equals(pageName)) {
                content = new ResourceTypeTreeView(this.extendLocatorId("Templates"));
                currentPageViewId = null; // we always want to refresh, even if we renavigate back
            } else if (PAGE_DOWNLOADS_VIEW_ID.equals(pageName)) {
                url = "/rhq/admin/downloads-body.xhtml";
            } else if (PAGE_LICENSE_VIEW_ID.equals(pageName)) {
                url = "/admin/license/LicenseAdmin.do?mode=view";
            } else if (PAGE_PLUGINS_VIEW_ID.equals(pageName)) {
                url = "/rhq/admin/plugin/plugin-list-plain.xhtml";
            }
            if (url != null) {
                url = addQueryStringParam(url, "nomenu=true");
                content = new FullHTMLPane(url);
            }

        } else if (SECTION_TOPOLOGY_VIEW_ID.equals(sectionName)) {
            String url = null;
            if (PAGE_SERVERS_VIEW_ID.equals(pageName)) {
                url = "/rhq/ha/listServers-plain.xhtml";
            } else if (PAGE_AGENTS_VIEW_ID.equals(pageName)) {
                url = "/rhq/ha/listAgents-plain.xhtml";
            } else if (PAGE_AFFINITY_GROUPS_VIEW_ID.equals(pageName)) {
                url = "/rhq/ha/listAffinityGroups-plain.xhtml";
            } else if (PAGE_PARTITION_EVENTS_VIEW_ID.equals(pageName)) {
                url = "/rhq/ha/listPartitionEvents-plain.xhtml";
            }
            content = new FullHTMLPane(url);
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

    public void renderView(ViewPath viewPath) {
        if (!viewPath.isCurrent(currentSectionViewId) || !viewPath.isNext(currentPageViewId)) {
            if (viewPath.isEnd()) {
                // Display default view
                setContent(defaultView());
            } else {
                renderContentView(viewPath);
            }
        } else {
            if (this.currentContent instanceof BookmarkableView) {
                ((BookmarkableView) this.currentContent).renderView(viewPath.next().next());
            }
        }
    }

    private void selectSectionPageTreeGridNode(String sectionName, String pageName) {
        for (String name : treeGrids.keySet()) {
            TreeGrid treeGrid = treeGrids.get(name);
            if (!name.equals(sectionName)) {
                treeGrid.deselectAllRecords();
            } else {
                TreeNode node = treeGrid.getTree().find(pageName);
                if (node != null) {
                    treeGrid.selectSingleRecord(node);
                } else {
                    CoreGUI.getErrorHandler().handleError("Unknown page name - URL is incorrect");
                }
            }
        }
    }

    private static String addQueryStringParam(String url, String param) {
        char separatorChar = (url.indexOf('?') == -1) ? '?' : '&';
        return url + separatorChar + param;
    }
}
