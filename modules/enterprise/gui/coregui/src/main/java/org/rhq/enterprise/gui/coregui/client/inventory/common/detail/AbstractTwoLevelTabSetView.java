/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.inventory.common.detail;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Side;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.layout.Layout;

import org.rhq.core.domain.authz.Permission;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.RefreshableView;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.tab.SubTab;
import org.rhq.enterprise.gui.coregui.client.components.tab.TwoLevelTab;
import org.rhq.enterprise.gui.coregui.client.components.tab.TwoLevelTabSelectedEvent;
import org.rhq.enterprise.gui.coregui.client.components.tab.TwoLevelTabSelectedHandler;
import org.rhq.enterprise.gui.coregui.client.components.tab.TwoLevelTabSet;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Greg Hinkle
 * @author Ian Springer
 */
public abstract class AbstractTwoLevelTabSetView<T, U extends Layout> extends LocatableVLayout implements
    BookmarkableView, TwoLevelTabSelectedHandler {
    private String baseViewPath;
    private TwoLevelTabSet tabSet;
    private String tabName;
    private String subTabName;
    private U titleBar;

    public AbstractTwoLevelTabSetView(String locatorId, String baseViewPath) {
        super(locatorId);
        this.baseViewPath = baseViewPath;

        setWidth100();
        setHeight100();

        this.titleBar = createTitleBar();
        addMember(this.titleBar);

        this.tabSet = new TwoLevelTabSet(extendLocatorId("TabSet"));
        this.tabSet.setTabBarPosition(Side.TOP);
        this.tabSet.setWidth100();
        this.tabSet.setHeight100();
        this.tabSet.setEdgeMarginSize(0);
        this.tabSet.setEdgeSize(0);

        List<TwoLevelTab> tabsList = createTabs();
        this.tabSet.setTabs(tabsList.toArray(new TwoLevelTab[tabsList.size()]));

        this.tabSet.addTwoLevelTabSelectedHandler(this);

        addMember(this.tabSet);
    }

    // ------------------ Abstract Methods --------------------
    public abstract Integer getSelectedItemId();

    protected abstract U createTitleBar();

    protected abstract List<TwoLevelTab> createTabs();

    protected abstract void loadSelectedItem(int itemId, ViewPath viewPath, Set<Permission> globalPermissions);

    protected abstract void updateTabContent(T selectedItem, Set<Permission> globalPermissions);

    // ---------------------------------------------------------

    protected U getTitleBar() {
        return this.titleBar;
    }

    protected boolean updateTab(TwoLevelTab tab, boolean visible, boolean enabled) {
        TwoLevelTab attachedTab = getTabSet().getTabByLocatorId(tab.getLocatorId());
        if (visible) {
            if (attachedTab == null) {
                getTabSet().addTab(tab);
                attachedTab = getTabSet().getTabByLocatorId(tab.getLocatorId());
            }
            getTabSet().setTabEnabled(attachedTab, enabled);
        } else {
            if (attachedTab != null) {
                getTabSet().removeTab(attachedTab);
            }
        }

        return (visible && enabled);
    }

    protected void updateSubTab(TwoLevelTab tab, SubTab subTab, Canvas canvas, boolean visible, boolean enabled) {
        tab.setVisible(subTab, visible);
        if (visible) {
            tab.setSubTabEnabled(subTab, enabled);
            if (enabled) {
                subTab.setCanvas(canvas);
            }
        }
    }

    public void onTabSelected(TwoLevelTabSelectedEvent tabSelectedEvent) {
        if (getSelectedItemId() == null) {
            CoreGUI.goToView(History.getToken());
        } else {

            //            selectSubTabByTitle(tabSelectedEvent.getId(), tabSelectedEvent.getSubTabId());
            String tabPath = "/" + tabSelectedEvent.getId() + "/" + tabSelectedEvent.getSubTabId();
            String path = this.baseViewPath + "/" + getSelectedItemId() + tabPath;

            // If the tab that was selected is not already the current history item, the user clicked on the tab, rather
            // than going directly to the tab's URL. In this case, fire a history event to go to the tab and make it the
            // current history item.
            if (!History.getToken().equals(path)) {
                CoreGUI.goToView(path);
            }
        }
        // TODO?: Switch tabs directly, rather than letting the history framework do it, to avoid redrawing the outer views.
    }

    public void renderView(final ViewPath viewPath) {
        // e.g. #Resource/10010/Summary/Overview
        //                ^ current path
        final int id = Integer.parseInt(viewPath.getCurrent().getPath());
        viewPath.next();

        if (!viewPath.isEnd()) {
            // e.g. #Resource/10010/Summary/Overview
            //                      ^ current path
            this.tabName = viewPath.getCurrent().getPath();
            viewPath.next();
            if (!viewPath.isEnd()) {
                // e.g. #Resource/10010/Summary/Overview
                //                              ^ current path
                this.subTabName = viewPath.getCurrent().getPath();
                viewPath.next();
            } else {
                this.subTabName = null;
            }
        } else {
            this.tabName = null;
        }

        if (getSelectedItemId() == null || getSelectedItemId() != id) {
            GWTServiceLookup.getAuthorizationService().getExplicitGlobalPermissions(
                new AsyncCallback<Set<Permission>>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(
                            "Could not determine global permissions for user, defaulting to no global permissions.",
                            caught);
                        // A different Resource or first load - go get data.
                        loadSelectedItem(id, viewPath, new HashSet<Permission>());
                    }

                    @Override
                    public void onSuccess(Set<Permission> result) {
                        // A different Resource or first load - go get data.
                        loadSelectedItem(id, viewPath, result);
                    }
                });
        } else {
            // Same Resource - just switch tabs.
            selectTab(this.tabName, this.subTabName, viewPath);
        }
    }

    /**
     * Select the tab/subtab with the specified titles (e.g. "Monitoring", "Graphs").
     *
     * @param tabTitle the title of the tab to select - if null, the default tab (the leftmost non-disabled one) will be selected
     * @param subtabTitle the title of the subtab to select - if null, the default subtab (the leftmost non-disabled one) will be selected
     * @param viewPath the view path, which may have additional view items to be rendered
     */
    public void selectTab(String tabTitle, String subtabTitle, ViewPath viewPath) {
        try {
            TwoLevelTab tab = (tabTitle != null) ? this.tabSet.getTabByTitle(tabTitle) : this.tabSet.getDefaultTab();
            if (tab == null || tab.getDisabled()) {
                CoreGUI.getErrorHandler().handleError("Invalid tab name: " + tabTitle);
                // TODO: Should we fire a history event here to redirect to a valid bookmark?
                tab = this.tabSet.getDefaultTab();
                if (tab == null) {
                    throw new IllegalStateException("No default tab is defined.");
                }
                subtabTitle = null;
            }
            // Do *not* select the tab and trigger the tab selected event until the subtab has been selected first.

            SubTab subtab = (subtabTitle != null) ? tab.getSubTabByTitle(subtabTitle) : tab.getDefaultSubTab();
            if (subtab == null || tab.getLayout().isDisabled()) {
                CoreGUI.getErrorHandler().handleError("Invalid subtab name: " + subtabTitle);
                // TODO: Should we fire a history event here to redirect to a valid bookmark?
                subtab = tab.getLayout().getDefaultSubTab();
            }
            tab.getLayout().selectSubTab(subtab);

            // Now that the subtab has been selected, select the tab (this will cause a tab selected event to fire).
            this.tabSet.selectTab(tab);

            // Handle any remaining view items (e.g. id of a selected item in a subtab that contains a Master-Details view).
            Canvas subView = subtab.getCanvas();
            if (subView instanceof RefreshableView) {
                ((RefreshableView)subView).refresh();
            }
            if (subView instanceof BookmarkableView) {
                ((BookmarkableView)subView).renderView(viewPath);
            }

            this.tabSet.markForRedraw();
        } catch (Exception e) {
            System.err.println("Failed to select tab " + tabTitle + "/" + subtabTitle + ": " + e);
        }
    }

    public TwoLevelTabSet getTabSet() {
        return tabSet;
    }

    public String getTabName() {
        return tabName;
    }

    public String getSubTabName() {
        return subTabName;
    }

    public String getBaseViewPath() {
        return baseViewPath;
    }

}
