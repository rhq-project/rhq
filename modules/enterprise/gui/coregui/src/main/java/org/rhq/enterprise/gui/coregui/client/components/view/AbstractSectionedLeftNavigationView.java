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
package org.rhq.enterprise.gui.coregui.client.components.view;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.allen_sauer.gwt.log.client.Log;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.events.CellClickEvent;
import com.smartgwt.client.widgets.grid.events.CellClickHandler;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;
import com.smartgwt.client.widgets.tree.Tree;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeNode;

import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.InitializableView;
import org.rhq.enterprise.gui.coregui.client.RefreshableView;
import org.rhq.enterprise.gui.coregui.client.ViewId;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.tree.EnhancedTreeNode;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableSectionStack;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableTreeGrid;

/**
 * The base class for the various top-level views which have a sectioned navigation menu on the left side and a content
 * pane on the right side.
 *
 * @author Greg Hinkle
 * @author Ian Springer
 * @author Jay Shaughnessy
 */
public abstract class AbstractSectionedLeftNavigationView extends LocatableHLayout implements BookmarkableView,
    InitializableView {
    
    private String viewId;
    private boolean initialized;
    private ViewId currentSectionViewId;
    private ViewId currentPageViewId;

    private SectionStack sectionStack;

    private Canvas contentCanvas;
    private Canvas currentContent;
    private Map<String, TreeGrid> treeGrids = new LinkedHashMap<String, TreeGrid>();
    private Map<String, NavigationSection> sectionsByName;

    public AbstractSectionedLeftNavigationView(String viewId) {
        super(viewId);
        this.viewId = viewId;
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

        List<NavigationSection> sections = getNavigationSections();
        this.sectionsByName = new HashMap<String, NavigationSection>(sections.size());
        for (NavigationSection section : sections) {
            TreeGrid treeGrid = buildTreeGridForSection(section);
            addSection(treeGrid);
            treeGrid.getTree().openAll();
            this.sectionsByName.put(section.getName(), section);
        }

        addMember(sectionStack);
        addMember(contentCanvas);

        this.initialized = true;
    }

    @Override
    public boolean isInitialized() {
        return this.initialized;
    }

    protected abstract Canvas defaultView();

    protected abstract List<NavigationSection> getNavigationSections();

    private TreeGrid buildTreeGridForSection(NavigationSection navigationSection) {
        final TreeGrid treeGrid = new LocatableTreeGrid(navigationSection.getName());
        treeGrid.setLeaveScrollbarGap(false);
        treeGrid.setShowHeader(false);

        List<NavigationItem> navigationItems = navigationSection.getNavigationItems();
        TreeNode[] treeNodes = new TreeNode[navigationItems.size()];
        for (int i = 0, navigationItemsSize = navigationItems.size(); i < navigationItemsSize; i++) {
            NavigationItem item = navigationItems.get(i);
            final TreeNode treeNode = new EnhancedTreeNode(item.getName(), item.getTitle());
            treeNode.setIcon(item.getIcon());
            treeNode.setEnabled(item.isEnabled());
            treeNodes[i] = treeNode;
        }

        TreeNode rootNode = new EnhancedTreeNode(navigationSection.getName(), navigationSection.getTitle(), treeNodes);
        Tree tree = new Tree();
        tree.setRoot(rootNode);
        treeGrid.setData(tree);

        return treeGrid;
    }

    private void addSection(final TreeGrid treeGrid) {
        final String sectionName = treeGrid.getTree().getRoot().getName();
        final String sectionTitle = treeGrid.getTree().getRoot().getTitle();
        this.treeGrids.put(sectionName, treeGrid);

        treeGrid.addCellClickHandler(new CellClickHandler() {
            @Override
            public void onCellClick(CellClickEvent event) {
                // We use cell click as opposed to selected changed handler
                // because we want to be able to refresh even if clicking
                // on an already selected node.
                TreeNode selectedRecord = (TreeNode) treeGrid.getSelectedRecord();
                if (selectedRecord != null) {
                    String pageName = selectedRecord.getName();
                    String viewPath = viewId + "/" + sectionName + "/" + pageName;
                    CoreGUI.goToView(viewPath);
                }
            }
        });

        SectionStackSection section = new SectionStackSection(sectionTitle);
        section.setExpanded(true);
        section.addItem(treeGrid);

        this.sectionStack.addSection(section);
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
        this.currentSectionViewId = viewPath.getCurrent();
        this.currentPageViewId = viewPath.getNext();

        String sectionName = this.currentSectionViewId.getPath();
        String pageName = this.currentPageViewId.getPath();

        NavigationSection section = this.sectionsByName.get(sectionName);
        if (section == null) {
            throw new IllegalStateException("Invalid section: " + sectionName);
        }
        NavigationItem item = section.getNavigationItem(pageName);
        if (item == null) {
            throw new IllegalStateException("Invalid page: " + pageName);
        }

        if (item.isRefreshRequired()) {
            this.currentPageViewId = null;
        }

        // When changing sections, make sure the previous section's selection is deselected.
        selectSectionPageTreeGridNode(sectionName, pageName);

        // Only update the content pane if the item has an associated view factory.
        ViewFactory viewFactory = item.getViewFactory();
        Canvas content = (viewFactory != null) ? viewFactory.createView() : null;
        if (content != null) {
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
            } else if (this.currentContent instanceof RefreshableView && this.currentContent.isDrawn()) {
                // Refresh the data on the content pane, so it's not stale.
                Log.debug("Refreshing data for [" + this.currentContent.getClass().getName() + "]...");
                ((RefreshableView) this.currentContent).refresh();
            }
        }
    }

    private void selectSectionPageTreeGridNode(String sectionName, String pageName) {
        for (String name : treeGrids.keySet()) {
            TreeGrid treeGrid = treeGrids.get(name);
            if (!name.equals(sectionName)) {
                treeGrid.deselectAllRecords();
            } else {
                Tree tree = treeGrid.getTree();
                TreeNode node = tree.find(sectionName + "/" + pageName);
                if (node != null) {
                    treeGrid.selectSingleRecord(node);
                } else {
                    CoreGUI.getErrorHandler().handleError(MSG.view_leftNav_unknownPage(pageName, sectionName));
                }
            }
        }
    }

}
