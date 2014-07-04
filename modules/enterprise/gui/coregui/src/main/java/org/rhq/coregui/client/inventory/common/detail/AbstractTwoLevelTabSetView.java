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
package org.rhq.coregui.client.inventory.common.detail;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.user.client.History;
import com.smartgwt.client.types.Side;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.layout.Layout;

import org.rhq.core.domain.authz.Permission;
import org.rhq.coregui.client.BookmarkableView;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.PermissionsLoadedListener;
import org.rhq.coregui.client.PermissionsLoader;
import org.rhq.coregui.client.RefreshableView;
import org.rhq.coregui.client.ViewChangedException;
import org.rhq.coregui.client.ViewPath;
import org.rhq.coregui.client.components.tab.SubTab;
import org.rhq.coregui.client.components.tab.TwoLevelTab;
import org.rhq.coregui.client.components.tab.TwoLevelTabSelectedEvent;
import org.rhq.coregui.client.components.tab.TwoLevelTabSelectedHandler;
import org.rhq.coregui.client.components.tab.TwoLevelTabSet;
import org.rhq.coregui.client.components.view.ViewFactory;
import org.rhq.coregui.client.inventory.common.AbstractD3GraphListView;
import org.rhq.coregui.client.util.Log;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;

/**
 * @author Greg Hinkle
 * @author Ian Springer
 */
public abstract class AbstractTwoLevelTabSetView<T, U extends Layout, V extends AbstractD3GraphListView> extends
    EnhancedVLayout implements
    BookmarkableView, TwoLevelTabSelectedHandler {

    private String baseViewPath;
    private TwoLevelTabSet tabSet;
    private String tabName;
    private String subTabName;
    private final U titleBar;
    protected Set<Permission> globalPermissions;
    protected V graphListView;

    public AbstractTwoLevelTabSetView(String baseViewPath, U titleBar, TwoLevelTab[] tabs) {
        super();
        this.baseViewPath = baseViewPath;

        setWidth100();
        setHeight100();

        this.titleBar = titleBar;
        addMember(this.titleBar);

        this.tabSet = new TwoLevelTabSet();
        this.tabSet.setTabBarPosition(Side.TOP);
        this.tabSet.setWidth100();
        this.tabSet.setHeight100();
        this.tabSet.setEdgeMarginSize(0);
        this.tabSet.setEdgeSize(0);

        this.tabSet.setTabs(tabs);

        this.tabSet.addTwoLevelTabSelectedHandler(this);

        addMember(this.tabSet);
    }

    // ------------------ Abstract Methods --------------------
    public abstract Integer getSelectedItemId();

    protected abstract V createD3GraphListView();

    /**
     *
     * @param itemId
     * @param viewPath
     */
    protected abstract void loadSelectedItem(int itemId, ViewPath viewPath);

    protected void updateTabContent(T selectedItem, boolean isRefresh) {
        String currentViewPath = History.getToken();
        if (!currentViewPath.startsWith(this.baseViewPath)) {
            throw new ViewChangedException(this.baseViewPath + "/" + getSelectedItemId());
        }
    }

    // ---------------------------------------------------------

    protected U getTitleBar() {
        return this.titleBar;
    }

    protected boolean updateTab(TwoLevelTab tab, boolean visible, boolean enabled) {
        if (visible) {
            getTabSet().setTabHidden(tab, false);
            getTabSet().setTabEnabled(tab, enabled);
        } else {
            getTabSet().setTabHidden(tab, true);
        }

        return (visible && enabled);
    }

    protected void updateSubTab(TwoLevelTab tab, SubTab subTab, boolean visible, boolean enabled,
        ViewFactory viewFactory) {
        updateSubTab(tab, subTab, null, visible, enabled, viewFactory);
    }

    protected void updateSubTab(TwoLevelTab tab, SubTab subTab, Canvas canvas, boolean visible, boolean enabled,
        ViewFactory viewFactory) {
        tab.setVisible(subTab, visible);
        if (visible) {
            tab.setSubTabEnabled(subTab, enabled);
            if (enabled) {
                subTab.setCanvas(canvas);
                subTab.setViewFactory(viewFactory);
            }
        }
    }

    // This is invoked by events fired in TwoLevelTabSet whenever a tab/subtab combo has been selected.
    public void onTabSelected(TwoLevelTabSelectedEvent tabSelectedEvent) {

        // Establishing the proper tabbed view may involve tab add/remove and async loading of content. While doing this
        // we want to prevent user initiation of another tab change. To block users from clicking tabs we
        // disable the tab set.  We re-enable the tabset when safe. (see this method and also selectTab()).

        String historyToken = tabSelectedEvent.getHistoryToken();

        if (getSelectedItemId() == null) {
            this.tabSet.disable();
            CoreGUI.goToView(historyToken);

        } else {
            String tabId = tabSelectedEvent.getId();
            String subTabId = tabSelectedEvent.getSubTabId();
            String tabPath = "/" + tabId + "/" + subTabId;
            String path = this.baseViewPath + "/" + getSelectedItemId() + tabPath;

            // If the selected tab or subtab is not already the current history item, the user clicked on the tab, rather
            // than going directly to the tab's URL. In this case, fire a history event to go to the tab and make it the
            // current history item.
            if (!(historyToken.equals(path) || historyToken.startsWith(path + "/"))) {
                this.tabSet.disable();
                CoreGUI.goToView(path);

            } else {
                // ensure the tabset is enabled if we're not going to be doing any further tab selection
                this.tabSet.enable();
            }
        }
    }

    public void renderView(final ViewPath viewPath) {

        new PermissionsLoader().loadExplicitGlobalPermissions(new PermissionsLoadedListener() {
            @Override
            public void onPermissionsLoaded(Set<Permission> permissions) {
                globalPermissions = (permissions != null) ? permissions : new HashSet<Permission>();
                renderTabs(viewPath);
            }
        });
    }

    private void renderTabs(final ViewPath viewPath) {

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

        // We are either navigating to a new detail view (new entity: resource or group) or switching tabs
        // for the current entity. Changing entities may change the available tabs as the same tab set may not
        // be supported by the new entity's type. To maintain a valid tab selection for the TabSet, smartgwt will
        // generate events if the current tab is removed (which can happen say, when navigating from a resource of
        // type A to a resource of type B). We need to ignore tab selection events generated by smartgwt because we
        // already perform explicit tab management at a higher level. To do this we explicitly set events to be be
        // ignored. We re-enable the event handling when safe. (see selectTab()).  Similarly, even when navigating
        // within the same entity, when changing tabs we need to suppress smartgwt generated tab events (when
        // calling TabSet.selectTab). That event can conflict with our subtab management, navigating the user
        // to the default subtab for the tab and overriding our explicit navigation to a non-default subtab.
        this.tabSet.setIgnoreSelectEvents(true);

        if (getSelectedItemId() == null || getSelectedItemId() != id || viewPath.isRefresh()) {
            // A different entity (resource or group), load it and try to navigate to the same tabs if possible.
            this.loadSelectedItem(id, viewPath);

        } else {
            // Same Resource - just switch tabs.
            //
            // until we finish the following work we're susceptible to fast-click issues in
            // tree navigation.  So, wait until after it's done to notify listeners that the view is
            // safely rendered.  Make sure to notify even on failure.
            try {
                this.selectTab(this.tabName, this.subTabName, viewPath);
            } finally {
                notifyViewRenderedListeners();
            }
        }
    }

    /**
     * Select the tab/subtab with the specified titles (e.g. "Monitoring", "Graphs").
     *
     * @param tabName the title of the tab to select - if null, the default tab (the leftmost non-disabled one) will be selected
     * @param subtabName the title of the subtab to select - if null, the default subtab (the leftmost non-disabled one) will be selected
     * @param viewPath the view path, which may have additional view items to be rendered
     */
    public void selectTab(String tabName, String subtabName, ViewPath viewPath) {

        try {
            TwoLevelTab tab = (tabName != null) ? this.tabSet.getTabByName(tabName) : null;
            SubTab subtab = null;

            // if the requested tab is not available for the tabset then select the default tab/subtab. Fire
            // an event in order to navigate to the new path
            if (tab == null || tab.getDisabled()) {
                this.tabSet.setIgnoreSelectEvents(false);
                subtab = selectDefaultTabAndSubTab();
                return;
            }

            // the tab is available, now get the subtab
            subtab = (subtabName != null) ? tab.getSubTabByName(subtabName) : tab.getDefaultSubTab();

            // due to our attempt to perform sticky tabbing we may request an invalid subtab when
            // switching resources. If the requested subtab is not available the select the default subtab
            // for the tab. Fire an event in order to navigate to the new path.
            if (subtab == null || tab.getLayout().isSubTabDisabled(subtab)) {
                this.tabSet.setIgnoreSelectEvents(false);
                subtab = selectDefaultSubTab(tab);
                return;
            }

            // the requested tab/subtab are valid, continue with this path

            // select the tab and subTab (we are suppressing event handling, so the smartgwt fired event is ignored,
            // alowing us to perform the desired subtab management below)
            this.tabSet.selectTab(tab);
            // this call adds the subtab canvas as a member of the subtablayout
            // don't show the subtab canvas until after we perform any necessary rendering.
            tab.getLayout().selectSubTab(subtab, false);

            // get the target canvas
            Canvas subView = subtab.getCanvas();

            // if this is a bookmarkable view then further rendering is deferred to its renderView method. This
            // will set the basePath as well as handle any remaining view items (e.g. id of a selected item in
            // a subtab that contains a Master-Details view). Otherwise, make sure we perform any required
            // refresh.
            if (subView instanceof BookmarkableView) {
                ((BookmarkableView) subView).renderView(viewPath);
            } else if (subView instanceof RefreshableView && subView.isDrawn()) {
                // Refresh the data on the subtab, so it's not stale.
                Log.debug("Refreshing data for [" + subView.getClass().getName() + "]...");
                ((RefreshableView) subView).refresh();
            }
            subView.setVisible(true);

            // ensure the tabset is enabled (disabled in onTabSelected), and redraw
            this.tabSet.setIgnoreSelectEvents(false);
            this.tabSet.enable();
            this.tabSet.markForRedraw();

        } catch (Exception e) {
            this.tabSet.enable();
            Log.warn("Failed to select tab " + tabName + "/" + subtabName + ".", e);
        }
    }

    private SubTab selectDefaultTabAndSubTab() {
        TwoLevelTab tab = this.tabSet.getDefaultTab();
        if (tab == null) {
            throw new IllegalStateException("No default tab is defined.");
        }

        return selectDefaultSubTab(tab);
    }

    private SubTab selectDefaultSubTab(TwoLevelTab tab) {

        SubTab subTab = tab.getDefaultSubTab();
        if (subTab == null || tab.getLayout().isSubTabDisabled(subTab)) {
            CoreGUI.getErrorHandler().handleError(
                MSG.view_tabs_invalidSubTab((subTab != null ? subTab.getName() : "null")));
            subTab = tab.getLayout().getDefaultSubTab();
        }

        tab.getLayout().selectSubTab(subTab, true);

        // Now that the subtab has been selected, select the tab (this will cause a tab selected event to fire).
        this.tabSet.selectTab(tab);

        return subTab;
    }

    protected abstract T getSelectedItem();

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

    @Override
    public void destroy() {
        tabSet.destroy();
        super.destroy();
    }

    public interface ViewRenderedListener {
        void onViewRendered();
    }

    private List<ViewRenderedListener> viewRenderedListeners = new ArrayList<ViewRenderedListener>();

    public void addViewRenderedListener(ViewRenderedListener listener) {
        viewRenderedListeners.add(listener);
    }

    protected void notifyViewRenderedListeners() {
        for (ViewRenderedListener listener : viewRenderedListeners) {
            listener.onViewRendered();
        }
    }

}
