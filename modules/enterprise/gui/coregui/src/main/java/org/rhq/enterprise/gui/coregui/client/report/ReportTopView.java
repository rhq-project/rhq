/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
 
package org.rhq.enterprise.gui.coregui.client.report;

import java.util.LinkedHashMap;

import com.google.gwt.http.client.URL;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.grid.events.SelectionChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionEvent;
import com.smartgwt.client.widgets.layout.HLayout;
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
import org.rhq.enterprise.gui.coregui.client.alert.AlertsView;
import org.rhq.enterprise.gui.coregui.client.components.FullHTMLPane;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.platform.PlatformPortletView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.configuration.ConfigurationHistoryView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.operation.OperationHistoryView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.discovery.ResourceAutodiscoveryView;
import org.rhq.enterprise.gui.coregui.client.report.measurement.MeasurementOOBView;
import org.rhq.enterprise.gui.coregui.client.report.tag.TaggedView;

/**
 * @author Greg Hinkle
 */
public class ReportTopView extends HLayout implements BookmarkableView {

    public static final String VIEW_PATH = "Reports";


    private ViewId currentSectionViewId;
    private ViewId currentPageViewId;


    private SectionStack sectionStack;

    private Canvas contentCanvas;
    private Canvas currentContent;
    private LinkedHashMap<String, TreeGrid> treeGrids = new LinkedHashMap<String, TreeGrid>();

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


        treeGrids.put("Inventory", buildInventorySection());
//        treeGrids.put("Configuration", buildSystemConfigurationSection());
//        treeGrids.put("Cluster", buildManagementClusterSection());
        treeGrids.put("Reports", buildReportsSection());


        for (final String name : treeGrids.keySet()) {
            TreeGrid grid = treeGrids.get(name);

            grid.addSelectionChangedHandler(new SelectionChangedHandler() {
                public void onSelectionChanged(SelectionEvent selectionEvent) {
                    if (selectionEvent.getState()) {
                        CoreGUI.goTo("Reports/" + name + "/" + selectionEvent.getRecord().getAttribute("name"));
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

    }


    private HTMLFlow defaultView() {
        String contents = "<h1>Reports</h1>\n" +
                "This section provides access to global reports.";
        HTMLFlow flow = new HTMLFlow(contents);
        flow.setPadding(20);
        return flow;
    }


    private TreeGrid buildInventorySection() {

        final TreeGrid inventoryTreeGrid = new TreeGrid();
        inventoryTreeGrid.setLeaveScrollbarGap(false);
        inventoryTreeGrid.setShowHeader(false);

        Tree tree = new Tree();
        final TreeNode tagCloud = new TreeNode("Tag Cloud");
        tagCloud.setIcon("global/Cloud_16.png");

        final TreeNode suspectMetrics = new TreeNode("Suspect Metrics");
        suspectMetrics.setIcon("subsystems/monitor/Monitor_failed_16.png");

        final TreeNode recentConfigurationChanges = new TreeNode("Recent Configuration Changes");
        recentConfigurationChanges.setIcon("subsystems/configure/Configure_16.png");

        final TreeNode recentOperations = new TreeNode("Recent Operations");
        recentOperations.setIcon("subsystems/control/Operation_16.png");

        final TreeNode recentAlerts = new TreeNode("Recent Alerts");
        recentAlerts.setIcon("subsystems/alert/Alert_LOW_16.png");

        final TreeNode alertDefinitions = new TreeNode("Alert Definitions");
        alertDefinitions.setIcon("subsystems/alert/Alerts_16.png");

        final TreeNode platforms = new TreeNode("Platforms");
        platforms.setIcon("types/Platform_up_16.png");


        tree.setRoot(new TreeNode("inventory",
                tagCloud,
                suspectMetrics,
                recentConfigurationChanges,
                recentOperations,
                recentAlerts,
                alertDefinitions,
                platforms));

        inventoryTreeGrid.setData(tree);

        return inventoryTreeGrid;
    }


    private TreeGrid buildManagementClusterSection() {

        final TreeGrid mgmtClusterTreeGrid = new TreeGrid();
        mgmtClusterTreeGrid.setLeaveScrollbarGap(false);
        mgmtClusterTreeGrid.setShowHeader(false);

        Tree tree = new Tree();
        final TreeNode manageServersNode = new TreeNode("Servers");
        final TreeNode manageAgentsNode = new TreeNode("Agents");
        final TreeNode manageAffinityGroupsNode = new TreeNode("Affinity Groups");
        final TreeNode managePartitionEventsNode = new TreeNode("Partition Events");

        tree.setRoot(new TreeNode("clustering",
                manageServersNode,
                manageAgentsNode,
                manageAffinityGroupsNode,
                managePartitionEventsNode));

        mgmtClusterTreeGrid.setData(tree);

        return mgmtClusterTreeGrid;
    }


    private TreeGrid buildSystemConfigurationSection() {

        final TreeGrid systemConfigTreeGrid = new TreeGrid();
        systemConfigTreeGrid.setLeaveScrollbarGap(false);
        systemConfigTreeGrid.setShowHeader(false);

        Tree tree = new Tree();
        final TreeNode manageSettings = new TreeNode("System Settings");
        final TreeNode manageTemplates = new TreeNode("Templates");
        final TreeNode manageDownloads = new TreeNode("Downloads");
        final TreeNode manageLicense = new TreeNode("License");
        final TreeNode managePlugins = new TreeNode("Plugins");

        tree.setRoot(new TreeNode("System Configuration",
                manageSettings, manageTemplates, manageDownloads, manageLicense, managePlugins));

        systemConfigTreeGrid.setData(tree);

        return systemConfigTreeGrid;
    }


    private TreeGrid buildReportsSection() {

        final TreeGrid reportsTreeGrid = new TreeGrid();
        reportsTreeGrid.setLeaveScrollbarGap(false);
        reportsTreeGrid.setShowHeader(false);

        Tree tree = new Tree();
        final TreeNode inventorySummaryNode = new TreeNode("Inventory Summary");

        TreeNode reportsNode = new TreeNode("Reports", inventorySummaryNode);
        tree.setRoot(reportsNode);

        reportsTreeGrid.setData(tree);

        return reportsTreeGrid;
    }


    public void setContent(Canvas newContent) {

        if (contentCanvas.getChildren().length > 0)
            contentCanvas.getChildren()[0].destroy();

        contentCanvas.addChild(newContent);
        contentCanvas.markForRedraw();
        this.currentContent = newContent;
    }


    private void renderContentView(ViewPath viewPath) {

        currentSectionViewId = viewPath.getCurrent();
        currentPageViewId = viewPath.getNext();

        String section = currentSectionViewId.getPath();
        String page = currentPageViewId.getPath();

        page = URL.decode(page);


        Canvas content = null;
        if ("Inventory".equals(section)) {

            if ("Tag Cloud".equals(page)) {
                content = new TaggedView();
            } else if ("Suspect Metrics".equals(page)) {
                content = new MeasurementOOBView();
            } else if ("Recent Configuration Changes".equals(page)) {
                content = new ConfigurationHistoryView();
            } else if ("Recent Operations".equals(page)) {
                content = new OperationHistoryView();
            } else if ("Recent Alerts".equals(page)) {
                content = new AlertsView();
            } else if ("Alert Definitions".equals(page)) {
                //todo
            } else if ("Platforms".equals(page)) {
                content = new PlatformPortletView();
            }


        } else if ("Security".equals(section)) {

            if ("Manage Users".equals(page)) {
                content = new UsersView();
            } else if ("Manage Roles".equals(page)) {
                content = new RolesView();
            } else if ("Auto Discovery Queue".equals(page)) {
                content = new ResourceAutodiscoveryView();
            } else if ("Remote Agent Install".equals(page)) {
                content = new RemoteAgentInstallView();
            }
        } else if ("Configuration".equals(section)) {

            String url = null;
            if ("System Settings".equals(page)) {
                url = "/admin/config/Config.do?mode=edit";
            } else if ("Templates".equals(page)) {
                url = "/admin/config/EditDefaults.do?mode=monitor&viewMode=all";
            } else if ("Downloads".equals(page)) {
                url = "/rhq/admin/downloads-body.xhtml";
            } else if ("License".equals(page)) {
                url = "/admin/license/LicenseAdmin.do?mode=view";
            } else if ("Plugins".equals(page)) {
                url = "/rhq/admin/plugin/plugin-list.xhtml";
            }
            url = addQueryStringParam(url, "nomenu=true");
            content = new FullHTMLPane(url);


        } else if ("Cluster".equals(section)) {
            String url = null;
            if ("Servers".equals(page)) {
                url = "/rhq/ha/listServers.xhtml";
            } else if ("Agents".equals(page)) {
                url = "/rhq/ha/listAgents.xhtml";
            } else if ("Affinity Groups".equals(page)) {
                url = "/rhq/ha/listAffinityGroups.xhtml";
            } else if ("Partition Events".equals(page)) {
                url = "/rhq/ha/listPartitionEvents.xhtml";
            }
            url = addQueryStringParam(url, "nomenu=true");
            content = new FullHTMLPane(url);
        }


        for (String name : treeGrids.keySet()) {

            TreeGrid treeGrid = treeGrids.get(name);
            if (name.equals(section)) {
                TreeNode node = treeGrid.getTree().find(page);
                if (node != null) {
                    treeGrid.selectSingleRecord(node);
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