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
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;
import com.smartgwt.client.widgets.tree.Tree;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeNode;

import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ViewId;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.alert.AlertsView;
import org.rhq.enterprise.gui.coregui.client.components.FullHTMLPane;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.platform.PlatformPortletView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.configuration.ConfigurationHistoryView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.operation.OperationHistoryView;
import org.rhq.enterprise.gui.coregui.client.report.measurement.MeasurementOOBView;
import org.rhq.enterprise.gui.coregui.client.report.tag.TaggedView;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableSectionStack;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableTreeGrid;

/**
 * @author Greg Hinkle
 */
public class ReportTopView extends LocatableHLayout implements BookmarkableView {
    public static final String VIEW_ID = "Reports";    

    private static final String SECTION_INVENTORY = "Inventory";
    private static final String SECTION_REPORTS = "Reports";

    private ViewId currentSectionViewId;
    private ViewId currentPageViewId;

    private SectionStack sectionStack;

    private Canvas contentCanvas;
    private Canvas currentContent;
    private LinkedHashMap<String, TreeGrid> treeGrids = new LinkedHashMap<String, TreeGrid>();

    public ReportTopView(String locatorId) {
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

        treeGrids.put(SECTION_INVENTORY, buildInventorySection());
        treeGrids.put(SECTION_REPORTS, buildReportsSection());

        for (final String name : treeGrids.keySet()) {
            TreeGrid grid = treeGrids.get(name);

            grid.addSelectionChangedHandler(new SelectionChangedHandler() {
                public void onSelectionChanged(SelectionEvent selectionEvent) {
                    if (selectionEvent.getState()) {
                        CoreGUI.goToView("Reports/" + name + "/" + selectionEvent.getRecord().getAttribute("name"));
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
        String contents = "<h1>Reports</h1>\n" + "This section provides access to global reports.";
        HTMLFlow flow = new HTMLFlow(contents);
        flow.setPadding(20);
        return flow;
    }

    private TreeGrid buildInventorySection() {

        final TreeGrid inventoryTreeGrid = new LocatableTreeGrid(SECTION_INVENTORY);
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

        TreeNode inventoryNode = new TreeNode(SECTION_INVENTORY, tagCloud, suspectMetrics, recentConfigurationChanges,
            recentOperations, recentAlerts, alertDefinitions);
        tree.setRoot(inventoryNode);

        inventoryTreeGrid.setData(tree);

        return inventoryTreeGrid;
    }

    private TreeGrid buildReportsSection() {

        final TreeGrid reportsTreeGrid = new LocatableTreeGrid(SECTION_REPORTS);
        reportsTreeGrid.setLeaveScrollbarGap(false);
        reportsTreeGrid.setShowHeader(false);

        Tree tree = new Tree();
        final TreeNode inventorySummary = new TreeNode("Inventory Summary");
        inventorySummary.setIcon("subsystems/inventory/Inventory_16.png");

        final TreeNode platforms = new TreeNode("CPU & Memory Utilization");
        platforms.setIcon("types/Platform_up_16.png");

        TreeNode reportsNode = new TreeNode(SECTION_REPORTS, inventorySummary, platforms);
        tree.setRoot(reportsNode);

        reportsTreeGrid.setData(tree);

        return reportsTreeGrid;
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

        page = URL.decode(page);

        Canvas content = null;
        if (SECTION_INVENTORY.equals(section)) {

            if ("Tag Cloud".equals(page)) {
                content = new TaggedView(this.extendLocatorId("TagCloud"));
            } else if ("Suspect Metrics".equals(page)) {
                content = new MeasurementOOBView(this.extendLocatorId("SuspectMetrics"));
            } else if ("Recent Configuration Changes".equals(page)) {
                content = new ConfigurationHistoryView(this.extendLocatorId("RecentConfigChanges"));
            } else if ("Recent Operations".equals(page)) {
                content = new OperationHistoryView(this.extendLocatorId("RecentOps"));
            } else if ("Recent Alerts".equals(page)) {
                content = new AlertsView(this.extendLocatorId("RecentAlerts"));
            } else if ("Alert Definitions".equals(page)) {
                //todo
            }

        } else if (SECTION_REPORTS.equals(section)) {
            if ("Inventory Summary".equals(page)) {
                content = new FullHTMLPane("/rhq/admin/report/resourceInstallReport-body.xhtml");
            } else if ("CPU & Memory Utilization".equals(page)) {
                content = new PlatformPortletView(this.extendLocatorId("Platforms"));
            }
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
}