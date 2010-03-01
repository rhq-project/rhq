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
package org.rhq.enterprise.gui.coregui.client.admin;

import org.rhq.enterprise.gui.coregui.client.Breadcrumb;
import org.rhq.enterprise.gui.coregui.client.UnknownViewException;
import org.rhq.enterprise.gui.coregui.client.View;
import org.rhq.enterprise.gui.coregui.client.ViewId;
import org.rhq.enterprise.gui.coregui.client.ViewRenderer;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.Presenter;
import org.rhq.enterprise.gui.coregui.client.admin.report.InventorySummaryReportView;
import org.rhq.enterprise.gui.coregui.client.admin.roles.RolesView;
import org.rhq.enterprise.gui.coregui.client.admin.users.UsersView;
import org.rhq.enterprise.gui.coregui.client.places.Place;

import com.smartgwt.client.types.ContentsType;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.HTMLPane;
import com.smartgwt.client.widgets.grid.events.SelectionChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionEvent;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;
import com.smartgwt.client.widgets.tree.Tree;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 
 * @author Greg Hinkle
 */
public class AdministrationView extends HLayout implements Presenter, ViewRenderer {

    private SectionStack sectionStack;

    private Canvas contentCanvas;
    private List<TreeGrid> treeGrids;

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

        this.treeGrids = new ArrayList<TreeGrid>(5);
        sectionStack.addSection(buildSecuritySection());
        sectionStack.addSection(buildPluginsSection());
        sectionStack.addSection(buildManagementClusterSection());
        sectionStack.addSection(buildSystemConfigurationSection());
        sectionStack.addSection(buildReportsSection());

        addMember(sectionStack);
        addMember(contentCanvas);

        setContent(buildLanding());        
    }


    private HTMLFlow buildLanding() {
        String contents = "<h1>Administration</h1>\n" +
                "From this section, the RHQ global settings can be administered. This includes configuring \n" +
                "<a href=\"\">Security</a>, setting up <a href=\"\">Plugins</a> and other stuff.";
        HTMLFlow flow = new HTMLFlow(contents);
        flow.setPadding(20);
        return flow;
    }


    private SectionStackSection buildSecuritySection() {
        final SectionStackSection section = new SectionStackSection("Security");
        section.setExpanded(true);

        final TreeGrid securityTreeGrid = new TreeGrid();
        securityTreeGrid.setShowHeader(false);
        this.treeGrids.add(securityTreeGrid);

        Tree tree = new Tree();
        final TreeNode manageUsersNode = new TreeNode("Manage Users");
        final TreeNode manageRolesNode = new TreeNode("Manage Roles");
        final TreeNode manageGroups = new TreeNode("Resource Groups");
        tree.setRoot(new TreeNode("security",
                manageUsersNode,
                manageRolesNode,
                manageGroups));

        securityTreeGrid.setData(tree);

        securityTreeGrid.addSelectionChangedHandler(new SelectionChangedHandler() {
            public void onSelectionChanged(SelectionEvent selectionEvent) {
                if (selectionEvent.getState()) {
                    if (selectionEvent.getRecord() == manageUsersNode) {
                        setContent(new UsersView());
                    } else if (selectionEvent.getRecord() == manageRolesNode) {
                        setContent(new RolesView());
                    } else if (selectionEvent.getRecord() == manageGroups) {
                        setContent(new RolesView());
                    }
                    for (TreeGrid treeGrid : treeGrids) {
                        if (treeGrid != securityTreeGrid) {
                            treeGrid.deselectAllRecords();
                        }
                    }
                }
            }
        });

        section.addItem(securityTreeGrid);

        return section;
    }


    private SectionStackSection buildManagementClusterSection() {
        final SectionStackSection section = new SectionStackSection("Management Cluster");
        section.setExpanded(true);

        final TreeGrid mgmtClusterTreeGrid = new TreeGrid();
        mgmtClusterTreeGrid.setShowHeader(false);
        this.treeGrids.add(mgmtClusterTreeGrid);

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

        mgmtClusterTreeGrid.addSelectionChangedHandler(new SelectionChangedHandler() {
            public void onSelectionChanged(SelectionEvent selectionEvent) {
                if (selectionEvent.getState()) {
                    HTMLPane pane = new HTMLPane();
                    pane.setContentsType(ContentsType.PAGE);
                    pane.setWidth100();
                    pane.setHeight100();

                    String url = null;
                    if (selectionEvent.getRecord() == manageServersNode) {
                        url = "/rhq/ha/listServers.xhtml";
                    } else if (selectionEvent.getRecord() == manageAgentsNode) {
                        url = "/rhq/ha/listAgents.xhtml";
                    } else if (selectionEvent.getRecord() == manageAffinityGroupsNode) {
                        url = "/rhq/ha/listAffinityGroups.xhtml";
                    } else if (selectionEvent.getRecord() == managePartitionEventsNode) {
                        url = "/rhq/ha/listPartitionEvents.xhtml";
                    }
                    pane.setContentsURL(url + "?nomenu=true");
                    setContent(pane);
                    for (TreeGrid treeGrid : treeGrids) {
                        if (treeGrid != mgmtClusterTreeGrid) {
                            treeGrid.deselectAllRecords();
                        }
                    }
                }
            }
        });

        section.addItem(mgmtClusterTreeGrid);

        return section;
    }

    private SectionStackSection buildPluginsSection() {
        final SectionStackSection section = new SectionStackSection("Plugins");
        section.setExpanded(true);

        final TreeGrid pluginsTreeGrid = new TreeGrid();
        pluginsTreeGrid.setShowHeader(false);
        this.treeGrids.add(pluginsTreeGrid);

        Tree tree = new Tree();
        final TreeNode managePlugins = new TreeNode("Plugins");

        tree.setRoot(new TreeNode("clustering",
                managePlugins));

        pluginsTreeGrid.setData(tree);

        pluginsTreeGrid.addSelectionChangedHandler(new SelectionChangedHandler() {
            public void onSelectionChanged(SelectionEvent selectionEvent) {
                if (selectionEvent.getState()) {
                    HTMLPane pane = new HTMLPane();
                    pane.setContentsType(ContentsType.PAGE);
                    pane.setWidth100();
                    pane.setHeight100();

                    String url = null;
                    if (selectionEvent.getRecord() == managePlugins) {
                        url = "/rhq/admin/plugin/plugin-list.xhtml";
                    }
                    pane.setContentsURL(url + "?nomenu=true");
                    setContent(pane);
                    for (TreeGrid treeGrid : treeGrids) {
                        if (treeGrid != pluginsTreeGrid) {
                            treeGrid.deselectAllRecords();
                        }
                    }
                }
            }
        });

        section.addItem(pluginsTreeGrid);

        return section;
    }


    private SectionStackSection buildSystemConfigurationSection() {
        final SectionStackSection section = new SectionStackSection("System Configuration");
        section.setExpanded(true);

        final TreeGrid systemConfigTreeGrid = new TreeGrid();
        systemConfigTreeGrid.setShowHeader(false);
        this.treeGrids.add(systemConfigTreeGrid);

        Tree tree = new Tree();
        final TreeNode manageSettings = new TreeNode("System Settings");
        final TreeNode manageTemplates = new TreeNode("Templates");
        final TreeNode manageLicense = new TreeNode("License");

        tree.setRoot(new TreeNode("System Configuration",
                manageSettings, manageTemplates, manageLicense));

        systemConfigTreeGrid.setData(tree);


        systemConfigTreeGrid.addSelectionChangedHandler(new SelectionChangedHandler() {
            public void onSelectionChanged(SelectionEvent selectionEvent) {
                if (selectionEvent.getState()) {
                    HTMLPane pane = new HTMLPane();
                    pane.setContentsType(ContentsType.PAGE);
                    pane.setWidth100();
                    pane.setHeight100();

                    String url = null;
                    if (selectionEvent.getRecord() == manageSettings) {
                        url = "/admin/config/Config.do?mode=edit";
                    } else if (selectionEvent.getRecord() == manageTemplates) {
                        url = "/admin/config/EditDefaults.do?mode=monitor&viewMode=all";
                    }

                    pane.setContentsURL(url + "&nomenu=true");
                    setContent(pane);
                    for (TreeGrid treeGrid : treeGrids) {
                        if (treeGrid != systemConfigTreeGrid) {
                            treeGrid.deselectAllRecords();
                        }
                    }
                }
            }
        });

        section.addItem(systemConfigTreeGrid);

        return section;
    }

    private SectionStackSection buildReportsSection() {
        final SectionStackSection section = new SectionStackSection("Reports");
        section.setID("Reports");
        section.setExpanded(true);

        final TreeGrid reportsTreeGrid = new TreeGrid();
        reportsTreeGrid.setShowHeader(false);
        this.treeGrids.add(reportsTreeGrid);

        Tree tree = new Tree();
        final TreeNode inventorySummaryNode = new TreeNode("Inventory Summary");

        TreeNode reportsNode = new TreeNode("Reports", inventorySummaryNode);
        tree.setRoot(reportsNode);

        reportsTreeGrid.setData(tree);

        reportsTreeGrid.addSelectionChangedHandler(new SelectionChangedHandler() {
            public void onSelectionChanged(SelectionEvent selectionEvent) {
                if (selectionEvent.getState()) {
                    HTMLPane pane = new InventorySummaryReportView();
                    setContent(pane);
                    CoreGUI.goTo(InventorySummaryReportView.PATH);
                    for (TreeGrid treeGrid : treeGrids) {
                        if (treeGrid != reportsTreeGrid) {
                            treeGrid.deselectAllRecords();
                        }
                    }
                }
            }
        });

        section.addItem(reportsTreeGrid);

        return section;
    }


    public void setContent(Canvas newContent) {

        if (contentCanvas.getChildren().length > 0)
            contentCanvas.getChildren()[0].destroy();

        contentCanvas.addChild(newContent);
        contentCanvas.draw();

    }


    public boolean fireDisplay(Place base, List<Place> subLocations) {
        try {
            if (base.equals(getPlace())) {
                if (!subLocations.isEmpty()) {
                    Place child = subLocations.get(0);
                    String childId = child.getId();
                    if (childId.equals("Reports")) {
                        if (subLocations.size() >= 2) {
                            Place grandchild = subLocations.get(1);
                            String grandchildId = grandchild.getId();
                            if (grandchildId.equals("InventorySummary")) {
                                HTMLPane pane = new InventorySummaryReportView();
                                setContent(pane);
                                return true;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println(e);
        }
        return false;
    }

    public Place getPlace() {
        return new Place("Administration", "Administration");
    }

    public List<Breadcrumb> render(List<String> breadcrumbNames) {
        if (breadcrumbNames == null || breadcrumbNames.size() < 2) {
            return Collections.emptyList();
        }
        List<Breadcrumb> renderedBreadcrumbs = new ArrayList<Breadcrumb>(breadcrumbNames.size());
        String firstBreadcrumbName = breadcrumbNames.get(0);
        if (firstBreadcrumbName.equals("Reports")) {
            String secondBreadcrumbName = breadcrumbNames.get(1);
            if (secondBreadcrumbName.equals("InventorySummary")) {
                HTMLPane pane = new InventorySummaryReportView();
                setContent(pane);
                renderedBreadcrumbs.add(new Breadcrumb("Reports"));
                renderedBreadcrumbs.add(new Breadcrumb("InventorySummary"));
            }
        }
        return renderedBreadcrumbs;
    }

    public View renderView(View parentView, ViewId viewId) throws UnknownViewException {
        String parentPath = viewId.getParent().getPath();
        if (parentPath.equals("Administration")) {
            SectionStackSection stackSection = this.sectionStack.getSection(viewId.getName());
            if (stackSection != null) {
                stackSection.setExpanded(true);
                return new View(viewId, new Breadcrumb(viewId.getName(), false));
            }
        } else if (parentPath.equals("Administration/Reports")) {
            InventorySummaryReportView canvas = new InventorySummaryReportView();
            setContent(canvas);
            return new View(viewId, canvas);
        }
        throw new UnknownViewException(viewId);        
    }
}
