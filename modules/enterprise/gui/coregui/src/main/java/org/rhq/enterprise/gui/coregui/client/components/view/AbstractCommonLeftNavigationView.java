package org.rhq.enterprise.gui.coregui.client.components.view;

import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.events.CellClickEvent;
import com.smartgwt.client.widgets.grid.events.CellClickHandler;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;
import com.smartgwt.client.widgets.tab.TabSet;
import com.smartgwt.client.widgets.tree.Tree;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeNode;
import org.rhq.core.domain.authz.Permission;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ViewId;
import org.rhq.enterprise.gui.coregui.client.components.tree.EnhancedTreeNode;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableTreeGrid;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Common Abstract class providing shared functionality for
 * Tabbed and sectioned navigation views.
 * @author Mike Thompson
 */
public abstract  class AbstractCommonLeftNavigationView {

    protected String viewId;
    protected boolean initialized;
    protected ViewId currentSectionViewId;
    protected ViewId currentPageViewId;

    protected TabSet tabSet;
    protected SectionStack sectionStack;

    protected Canvas contentCanvas;
    protected Canvas currentContent;
    protected Map<String, TreeGrid> treeGrids = new LinkedHashMap<String, TreeGrid>();
    protected Map<String, NavigationSection> sectionsByName;

    // Capture the user's global permissions for use by any dashboard or portlet that may need it for rendering.
    protected Set<Permission> globalPermissions = EnumSet.noneOf(Permission.class);



    protected TreeGrid buildTreeGridForSection(NavigationSection navigationSection) {
        final TreeGrid treeGrid = new LocatableTreeGrid(navigationSection.getName());
        treeGrid.setLeaveScrollbarGap(false);
        treeGrid.setShowHeader(false);
        treeGrid.setSelectionType(SelectionStyle.SINGLE);

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

    protected void addSection(final TreeGrid treeGrid) {
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
}
