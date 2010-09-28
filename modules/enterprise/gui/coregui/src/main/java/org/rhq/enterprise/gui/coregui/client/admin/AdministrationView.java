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

import com.google.gwt.user.client.History;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.grid.events.SelectionChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionEvent;
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

    private ViewId currentSectionViewId;
    private ViewId currentPageViewId;

    private SectionStack sectionStack;

    private Canvas contentCanvas;
    private Canvas currentContent;
    private Map<String, TreeGrid> treeGrids = new LinkedHashMap<String, TreeGrid>();

    private static final String SECURITY_SECTION_VIEW_ID = "Security";
    private static final String TOPOLOGY_SECTION_VIEW_ID = "Topology";
    private static final String CONFIGURATION_SECTION_VIEW_ID = "Configuration";

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
        addSection(buildManagementClusterSection());

        addMember(sectionStack);
        addMember(contentCanvas);
    }

    private void addSection(TreeGrid treeGrid) {
        final String sectionName = treeGrid.getTree().getRoot().getName();
        this.treeGrids.put(sectionName, treeGrid);

        treeGrid.addSelectionChangedHandler(new SelectionChangedHandler() {
            public void onSelectionChanged(SelectionEvent selectionEvent) {
                if (selectionEvent.getState()) {
                    TreeNode node = (TreeNode)selectionEvent.getRecord();
                    String pageName = node.getName();
                    String viewPath = AdministrationView.VIEW_ID + "/" + sectionName + "/" + pageName;
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

    private HTMLFlow defaultView() {
        String contents = "<h1>Administration</h1>\n"
            + "From this section, the RHQ global settings can be administered. This includes configuring \n"
            + "<a href=\"\">Security</a>, setting up <a href=\"\">Plugins</a> and other stuff.";
        HTMLFlow flow = new HTMLFlow(contents);
        flow.setPadding(20);
        return flow;
    }

    private TreeGrid buildSecuritySection() {

        final TreeGrid securityTreeGrid = new LocatableTreeGrid(SECURITY_SECTION_VIEW_ID);
        securityTreeGrid.setLeaveScrollbarGap(false);
        securityTreeGrid.setShowHeader(false);

        Tree tree = new Tree();
        final TreeNode manageUsersNode = new EnhancedTreeNode(UsersView.VIEW_ID);
        manageUsersNode.setIcon("global/User_16.png");

        final TreeNode manageRolesNode = new EnhancedTreeNode(RolesView.VIEW_ID);
        manageRolesNode.setIcon("global/Role_16.png");

        final TreeNode remoteAgentInstall = new EnhancedTreeNode(RemoteAgentInstallView.VIEW_ID);
        remoteAgentInstall.setIcon("global/Agent_16.png");

        TreeNode rootNode = new EnhancedTreeNode(SECURITY_SECTION_VIEW_ID, manageUsersNode, manageRolesNode,
            remoteAgentInstall);
        tree.setRoot(rootNode);

        securityTreeGrid.setData(tree);

        return securityTreeGrid;
    }

    private TreeGrid buildManagementClusterSection() {

        final TreeGrid mgmtClusterTreeGrid = new LocatableTreeGrid(TOPOLOGY_SECTION_VIEW_ID);
        mgmtClusterTreeGrid.setLeaveScrollbarGap(false);
        mgmtClusterTreeGrid.setShowHeader(false);

        Tree tree = new Tree();
        final TreeNode manageServersNode = new EnhancedTreeNode("Servers");
        final TreeNode manageAgentsNode = new EnhancedTreeNode("Agents");
        final TreeNode manageAffinityGroupsNode = new EnhancedTreeNode("AffinityGroups");
        final TreeNode managePartitionEventsNode = new EnhancedTreeNode("PartitionEvents");

        TreeNode rootNode = new EnhancedTreeNode(TOPOLOGY_SECTION_VIEW_ID, manageServersNode, manageAgentsNode,
            manageAffinityGroupsNode, managePartitionEventsNode);
        tree.setRoot(rootNode);

        mgmtClusterTreeGrid.setData(tree);

        return mgmtClusterTreeGrid;
    }

    private TreeGrid buildSystemConfigurationSection() {

        final TreeGrid systemConfigTreeGrid = new LocatableTreeGrid(CONFIGURATION_SECTION_VIEW_ID);
        systemConfigTreeGrid.setLeaveScrollbarGap(false);
        systemConfigTreeGrid.setShowHeader(false);

        Tree tree = new Tree();
        final TreeNode manageSettings = new EnhancedTreeNode("SystemSettings");
        final TreeNode manageTemplates = new EnhancedTreeNode("Templates");
        final TreeNode manageDownloads = new EnhancedTreeNode("Downloads");
        final TreeNode manageLicense = new EnhancedTreeNode("License");
        final TreeNode managePlugins = new EnhancedTreeNode("Plugins");

        TreeNode rootNode = new EnhancedTreeNode(CONFIGURATION_SECTION_VIEW_ID, manageSettings, manageTemplates, manageDownloads,
            manageLicense, managePlugins);
        tree.setRoot(rootNode);

        systemConfigTreeGrid.setData(tree);

        return systemConfigTreeGrid;
    }

    public void setContent(Canvas newContent) {
        for (Canvas child : contentCanvas.getChildren()) {
            child.destroy();
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
        if (SECURITY_SECTION_VIEW_ID.equals(sectionName)) {

            if (UsersView.VIEW_ID.equals(pageName)) {
                content = new UsersView(this.extendLocatorId("Users"));
            } else if (RolesView.VIEW_ID.equals(pageName)) {
                content = new RolesView(this.extendLocatorId("Roles"));
            } else if (RemoteAgentInstallView.VIEW_ID.equals(pageName)) {
                content = new RemoteAgentInstallView(this.extendLocatorId("RemoteAgentInstall"));
            }
        } else if (CONFIGURATION_SECTION_VIEW_ID.equals(sectionName)) {

            String url = null;
            if ("SystemSettings".equals(pageName)) {
                url = "/admin/config/Config.do?mode=edit";
            } else if ("Templates".equals(pageName)) {
                url = "/admin/config/EditDefaults.do?mode=monitor&viewMode=all";
            } else if ("Downloads".equals(pageName)) {
                url = "/rhq/admin/downloads-body.xhtml";
            } else if ("License".equals(pageName)) {
                url = "/admin/license/LicenseAdmin.do?mode=view";
            } else if ("Plugins".equals(pageName)) {
                url = "/rhq/admin/plugin/plugin-list-plain.xhtml";
            }
            url = addQueryStringParam(url, "nomenu=true");
            content = new FullHTMLPane(url);

        } else if (TOPOLOGY_SECTION_VIEW_ID.equals(sectionName)) {
            String url = null;
            if ("Servers".equals(pageName)) {
                url = "/rhq/ha/listServers-plain.xhtml";
            } else if ("Agents".equals(pageName)) {
                url = "/rhq/ha/listAgents-plain.xhtml";
            } else if ("Affinity Groups".equals(pageName)) {
                url = "/rhq/ha/listAffinityGroups-plain.xhtml";
            } else if ("Partition Events".equals(pageName)) {
                url = "/rhq/ha/listPartitionEvents-plain.xhtml";
            }
            content = new FullHTMLPane(url);
        }

        for (String name : treeGrids.keySet()) {
            TreeGrid treeGrid = treeGrids.get(name);
            if (name.equals(sectionName)) {
                //                treeGrid.setSelectedPaths(page);
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

    private static String addQueryStringParam(String url, String param) {
        char separatorChar = (url.indexOf('?') == -1) ? '?' : '&';
        return url + separatorChar + param;
    }
}
