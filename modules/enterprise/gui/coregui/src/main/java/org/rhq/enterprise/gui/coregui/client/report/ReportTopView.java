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
import java.util.Map;

import com.google.gwt.http.client.URL;
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
import org.rhq.enterprise.gui.coregui.client.alert.AlertsView;
import org.rhq.enterprise.gui.coregui.client.components.FullHTMLPane;
import org.rhq.enterprise.gui.coregui.client.components.tree.EnhancedTreeNode;
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

    private static final String SUBSYSTEMS_SECTION_VIEW_ID = "Subsystems";
    private static final String INVENTORY_SECTION_VIEW_ID = "Inventory";

    private ViewId currentSectionViewId;
    private ViewId currentPageViewId;

    private SectionStack sectionStack;

    private Canvas contentCanvas;
    private Canvas currentContent;
    private Map<String, TreeGrid> treeGrids = new LinkedHashMap<String, TreeGrid>();

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

        addSection(buildSubsystemsSection());
        addSection(buildInventorySection());

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
                    String viewPath = ReportTopView.VIEW_ID + "/" + sectionName + "/" + pageName;
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
        String contents = "<h1>Reports</h1>\n" + "This section provides access to global reports.";
        HTMLFlow flow = new HTMLFlow(contents);
        flow.setPadding(20);
        return flow;
    }

    private TreeGrid buildSubsystemsSection() {
        final TreeGrid inventoryTreeGrid = new LocatableTreeGrid(SUBSYSTEMS_SECTION_VIEW_ID);
        inventoryTreeGrid.setLeaveScrollbarGap(false);
        inventoryTreeGrid.setShowHeader(false);

        Tree tree = new Tree();
        final TreeNode tagCloud = new EnhancedTreeNode(TaggedView.VIEW_ID);
        tagCloud.setIcon("global/Cloud_16.png");

        final TreeNode suspectMetrics = new EnhancedTreeNode(MeasurementOOBView.VIEW_ID);
        suspectMetrics.setIcon("subsystems/monitor/Monitor_failed_16.png");

        final TreeNode recentConfigurationChanges = new EnhancedTreeNode(ConfigurationHistoryView.VIEW_ID);
        recentConfigurationChanges.setIcon("subsystems/configure/Configure_16.png");

        final TreeNode recentOperations = new EnhancedTreeNode(OperationHistoryView.VIEW_ID);
        recentOperations.setIcon("subsystems/control/Operation_16.png");

        final TreeNode recentAlerts = new EnhancedTreeNode(AlertsView.VIEW_ID);
        recentAlerts.setIcon("subsystems/alert/Alert_LOW_16.png");

        final TreeNode alertDefinitions = new EnhancedTreeNode("Alert Definitions");
        alertDefinitions.setIcon("subsystems/alert/Alerts_16.png");

        TreeNode inventoryNode = new EnhancedTreeNode(SUBSYSTEMS_SECTION_VIEW_ID, tagCloud, suspectMetrics,
            recentConfigurationChanges, recentOperations, recentAlerts, alertDefinitions);
        tree.setRoot(inventoryNode);

        inventoryTreeGrid.setData(tree);

        return inventoryTreeGrid;
    }

    private TreeGrid buildInventorySection() {

        final TreeGrid reportsTreeGrid = new LocatableTreeGrid(INVENTORY_SECTION_VIEW_ID);
        reportsTreeGrid.setLeaveScrollbarGap(false);
        reportsTreeGrid.setShowHeader(false);

        Tree tree = new Tree();
        final TreeNode inventorySummary = new EnhancedTreeNode("InventorySummary");
        inventorySummary.setIcon("subsystems/inventory/Inventory_16.png");

        final TreeNode platforms = new EnhancedTreeNode(PlatformPortletView.VIEW_ID);
        platforms.setIcon("types/Platform_up_16.png");

        TreeNode reportsNode = new EnhancedTreeNode(INVENTORY_SECTION_VIEW_ID, inventorySummary, platforms);
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

        String sectionName = currentSectionViewId.getPath();
        String pageName = currentPageViewId.getPath();

        pageName = URL.decode(pageName);

        Canvas content = null;
        if (SUBSYSTEMS_SECTION_VIEW_ID.equals(sectionName)) {
            if (TaggedView.VIEW_ID.equals(pageName)) {
                content = new TaggedView(this.extendLocatorId("Tag"));
            } else if (MeasurementOOBView.VIEW_ID.equals(pageName)) {
                content = new MeasurementOOBView(this.extendLocatorId("SuspectMetrics"));
            } else if (ConfigurationHistoryView.VIEW_ID.equals(pageName)) {
                content = new ConfigurationHistoryView();
            } else if (OperationHistoryView.VIEW_ID.equals(pageName)) {
                content = new OperationHistoryView(this.extendLocatorId("RecentOps"));
            } else if (AlertsView.VIEW_ID.equals(pageName)) {
                content = new AlertsView(this.extendLocatorId("RecentAlerts"));
            } else if ("Alert Definitions".equals(pageName)) {
                // TODO (mazz)
            }
        } else if (INVENTORY_SECTION_VIEW_ID.equals(sectionName)) {
            if ("InventorySummary".equals(pageName)) {
                content = new FullHTMLPane("/rhq/admin/report/resourceInstallReport-body.xhtml");
            } else if (PlatformPortletView.VIEW_ID.equals(pageName)) {
                content = new PlatformPortletView(this.extendLocatorId("Platforms"));
            }
        }

        for (String name : treeGrids.keySet()) {
            TreeGrid treeGrid = treeGrids.get(name);
            if (name.equals(sectionName)) {
                TreeNode node = treeGrid.getTree().find(pageName);
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