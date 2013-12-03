/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.coregui.client.dashboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.TabBarControls;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.events.CloseClickHandler;
import com.smartgwt.client.widgets.tab.events.TabCloseClickEvent;
import com.smartgwt.client.widgets.tab.events.TabSelectedEvent;
import com.smartgwt.client.widgets.tab.events.TabSelectedHandler;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.common.ProductInfo;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.criteria.DashboardCriteria;
import org.rhq.core.domain.dashboard.Dashboard;
import org.rhq.core.domain.dashboard.DashboardCategory;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.coregui.client.BookmarkableView;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.InitializableView;
import org.rhq.coregui.client.LinkManager;
import org.rhq.coregui.client.PermissionsLoadedListener;
import org.rhq.coregui.client.PermissionsLoader;
import org.rhq.coregui.client.ViewPath;
import org.rhq.coregui.client.components.tab.NamedTab;
import org.rhq.coregui.client.components.tab.NamedTabSet;
import org.rhq.coregui.client.components.view.ViewName;
import org.rhq.coregui.client.dashboard.portlets.recent.alerts.RecentAlertsPortlet;
import org.rhq.coregui.client.dashboard.portlets.recent.operations.OperationHistoryPortlet;
import org.rhq.coregui.client.dashboard.portlets.recent.problems.ProblemResourcesPortlet;
import org.rhq.coregui.client.dashboard.portlets.summary.InventorySummaryPortlet;
import org.rhq.coregui.client.dashboard.portlets.util.MashupPortlet;
import org.rhq.coregui.client.dashboard.portlets.util.MessagePortlet;
import org.rhq.coregui.client.gwt.DashboardGWTServiceAsync;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.Log;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;

/**
 * @author Jay Shaughnessy
 * @author Greg Hinkle
 */
public class DashboardsView extends EnhancedVLayout implements DashboardContainer, BookmarkableView, InitializableView {

    public static final ViewName VIEW_ID = new ViewName("Dashboards", MSG.view_dashboards_title());

    // Each NamedTab is a Dashboard, name=Dashboard.id, title=Dashboard.name
    private NamedTabSet tabSet;

    // The ID
    private String selectedTabName;

    private IButton editButton;

    private boolean editMode = false;

    private Map<String, Dashboard> dashboardsByName;

    private DashboardView selectedDashboardView;
    private Dashboard selectedDashboard;

    private DashboardGWTServiceAsync dashboardService = GWTServiceLookup.getDashboardService();

    // Capture the user's global permissions for use by any dashboard or portlet that may need it for rendering.
    private HashSet<Permission> globalPermissions;

    private boolean initialized = false;

    public DashboardsView() {
        super();
        setOverflow(Overflow.AUTO);
        setPadding(5);
        setWidth100();
        setHeight100();
    }

    @Override
    protected void onInit() {
        super.onInit();

        DashboardCriteria criteria = new DashboardCriteria();
        criteria.addSortName(PageOrdering.ASC);
        dashboardService.findDashboardsByCriteria(criteria, new AsyncCallback<PageList<Dashboard>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_dashboardsManager_error1(), caught);
            }

            public void onSuccess(final PageList<Dashboard> result) {
                // now, a second async call to load global perms
                new PermissionsLoader().loadExplicitGlobalPermissions(new PermissionsLoadedListener() {

                    public void onPermissionsLoaded(Set<Permission> permissions) {
                        globalPermissions = new HashSet<Permission>(permissions);

                        if (result.isEmpty()) {
                            // if the user has no dashboards persist a default dashboard for him to work with. In
                            // this way we're always working with a persisted dashboard and real entities.
                            addDefaultDashboard();

                        } else {
                            updateDashboards(result);

                        }

                        initialized = true;
                    }
                });
            }
        });
    }

    private void addDefaultDashboard() {
        dashboardService.storeDashboard(getDefaultDashboard(), new AsyncCallback<Dashboard>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_dashboardsManager_error1(), caught);
            }

            public void onSuccess(Dashboard defaultDashboard) {
                List<Dashboard> dashboards = new ArrayList<Dashboard>(1);
                dashboards.add(defaultDashboard);
                updateDashboards(dashboards);
            }
        });
    }

    private void updateDashboards(List<Dashboard> dashboards) {
        Canvas[] members = getMembers();
        removeMembers(members);

        if (null != tabSet) {
            tabSet.destroy();
        }

        this.dashboardsByName = new HashMap<String, Dashboard>(dashboards.size());
        for (Dashboard dashboard : dashboards) {
            this.dashboardsByName.put(dashboard.getName(), dashboard);
        }

        tabSet = new NamedTabSet();

        tabSet.setWidth100();
        tabSet.setHeight100();

        editButton = new EnhancedIButton(editMode ? MSG.common_title_view_mode() : MSG.common_title_edit_mode());
        editButton.setAutoFit(true);
        editButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                editMode = !editMode;
                editButton.setTitle(editMode ? MSG.common_title_view_mode() : MSG.common_title_edit_mode());
                if (selectedDashboardView.isMaximized()) {
                    selectedDashboardView.restorePortlet();
                }
                selectedDashboardView.setEditMode(editMode);
            }
        });

        final IButton newDashboardButton = new EnhancedIButton(MSG.common_title_new_dashboard());
        newDashboardButton.setAutoFit(true);
        newDashboardButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                addNewDashboard();
            }
        });

        HLayout buttons = new HLayout(5);
        buttons.addMember(editButton);
        buttons.addMember(newDashboardButton);

        tabSet.setTabBarControls(TabBarControls.TAB_SCROLLER, TabBarControls.TAB_PICKER, buttons );

        tabSet.addTabSelectedHandler(new TabSelectedHandler() {
            public void onTabSelected(TabSelectedEvent tabSelectedEvent) {
                NamedTab selectedTab = tabSet.getTabByTitle(tabSelectedEvent.getTab().getTitle());

                /*
                 * do not record history item if initially loading the DashboardsView.  if the selectedDashboardView is
                 * null, suppression will prevent redirection from #Dashboards to #Dashboards/dashboardId,
                 * which would require the user to hit the back button twice to return to the previous page.
                 */
                if (selectedDashboardView != null) {
                    Log.debug(" ***** selectedTab: " + selectedTab.getName() + ", " + selectedTab.getTitle());
                    CoreGUI.goToView(LinkManager.getDashboardLink(Integer.valueOf(selectedTab.getName())), true);
                }

                selectedDashboardView = (DashboardView) selectedTab.getPane();
                selectedDashboard = selectedDashboardView.getDashboard();
                editButton.setTitle(editMode ? MSG.common_title_view_mode() : MSG.common_title_edit_mode());
                selectedDashboardView.setEditMode(editMode);

                // If re-selecting a Dashboard, make sure it, and its portlets, are up to date
                if (selectedDashboardView.isDrawn()) {
                    // I think this should work with markForRedraw but for some reason it does not                    
                    selectedDashboardView.redraw();
                }
            }
        });

        for (Dashboard dashboard : dashboards) {
            String dashboardName = String.valueOf(dashboard.getId());
            String dashboardTitle = dashboard.getName();
            DashboardView dashboardView = new DashboardView(this, dashboard);
            Tab tab = new NamedTab(new ViewName(dashboardName, dashboardTitle), null);
            tab.setPane(dashboardView);
            tab.setCanClose(true);

            tabSet.addTab(tab);
            if (dashboard.getName().equals(selectedTabName)) {
                tabSet.selectTab(tab);
            }
        }

        tabSet.addCloseClickHandler(new CloseClickHandler() {
            public void onCloseClick(final TabCloseClickEvent tabCloseClickEvent) {
                tabCloseClickEvent.cancel();
                final DashboardView dashboardView = (DashboardView) tabCloseClickEvent.getTab().getPane();

                SC.ask(MSG.view_dashboards_confirm1() + " [" + dashboardView.getStoredDashboard().getName() + "]?",
                    new BooleanCallback() {
                        public void execute(Boolean confirmed) {
                            if (confirmed) {
                                dashboardsByName.remove(tabCloseClickEvent.getTab().getTitle());
                                tabSet.removeTab(tabCloseClickEvent.getTab());
                                dashboardView.delete();

                                // if it's the last tab go back to a default tab
                                if (0 == tabSet.getTabs().length) {
                                    addDefaultDashboard();
                                }
                            }
                        }
                    });
            }
        });

        addMember(tabSet);
    }

    protected Dashboard getDefaultDashboard() {

        Dashboard dashboard = new Dashboard();
        dashboard.setName(MSG.common_title_default());
        dashboard.setCategory(DashboardCategory.INVENTORY);
        dashboard.setColumns(2);
        // only leftmost column width is currently settable, the rest are equally divided        
        dashboard.setColumnWidths("32%");
        dashboard.getConfiguration().put(new PropertySimple(Dashboard.CFG_BACKGROUND, "#F1F2F3"));

        ProductInfo productInfo = CoreGUI.get().getProductInfo();
        boolean isRHQ = (productInfo != null) && "RHQ".equals(productInfo.getShortName());

        // Left Column
        int columnIndex = 0;
        int rowIndex = 0;

        if (isRHQ) {
            DashboardPortlet welcome = new DashboardPortlet(MessagePortlet.NAME, MessagePortlet.KEY, 250);
            dashboard.addPortlet(welcome, columnIndex, rowIndex++);
        }

        DashboardPortlet summary = new DashboardPortlet(InventorySummaryPortlet.NAME, InventorySummaryPortlet.KEY, 250);
        dashboard.addPortlet(summary, columnIndex, rowIndex++);

        DashboardPortlet news = new DashboardPortlet(MashupPortlet.NAME, MashupPortlet.KEY, 300);
        if (isRHQ) {
            news.getConfiguration().put(
                new PropertySimple("address", "https://docs.jboss.org/author/display/RHQ/User+Documentation"));
        } else {
            news.getConfiguration().put(
                new PropertySimple("address",
                    "https://access.redhat.com/ext/software/JBoss_Operations_Network/portlet/"));
        }
        dashboard.addPortlet(news, columnIndex, rowIndex++);

        // Right Column
        columnIndex = 1;
        rowIndex = 0;

        DashboardPortlet recentAlerts = new DashboardPortlet(RecentAlertsPortlet.NAME, RecentAlertsPortlet.KEY, 250);
        dashboard.addPortlet(recentAlerts, columnIndex, rowIndex++);

        DashboardPortlet problemResources = new DashboardPortlet(ProblemResourcesPortlet.NAME,
            ProblemResourcesPortlet.KEY, 250);
        //initialize config for the problemResources portlet.
        problemResources.getConfiguration().put(
            new PropertySimple(ProblemResourcesPortlet.PROBLEM_RESOURCE_SHOW_MAX,
                ProblemResourcesPortlet.defaultShowMax));
        problemResources.getConfiguration().put(
            new PropertySimple(ProblemResourcesPortlet.PROBLEM_RESOURCE_SHOW_HRS,
                ProblemResourcesPortlet.defaultShowHours));
        dashboard.addPortlet(problemResources, columnIndex, rowIndex++);

        DashboardPortlet operations = new DashboardPortlet(OperationHistoryPortlet.NAME, OperationHistoryPortlet.KEY,
            200);
        dashboard.addPortlet(operations, columnIndex, rowIndex++);

        return dashboard;
    }

    public boolean isValidDashboardName(String name) {
        if (name == null || name.equals("")) {
            return false;
        }
        return (!this.dashboardsByName.containsKey(name));
    }

    public void addNewDashboard() {
        int i = 0;
        String availableDashboardName = null;
        while (availableDashboardName == null) {
            String candidateDashboardName = MSG.common_title_custom() + i++;
            if (!this.dashboardsByName.containsKey(candidateDashboardName)) {
                availableDashboardName = candidateDashboardName;
            }
        }

        Dashboard dashboard = new Dashboard();
        dashboard.setCategory(DashboardCategory.INVENTORY);
        dashboard.setName(availableDashboardName);
        dashboard.setColumns(2);
        // only leftmost column width is currently settable, the rest are equally divided
        dashboard.setColumnWidths("32%");

        dashboardService.storeDashboard(dashboard, new AsyncCallback<Dashboard>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_dashboardsManager_error1(), caught);
            }

            public void onSuccess(Dashboard result) {
                String dashboardName = String.valueOf(result.getId());
                String dashboardTitle = result.getName();
                dashboardsByName.put(dashboardTitle, result); // update map so name can not be reused
                DashboardView dashboardView = new DashboardView(DashboardsView.this, result);
                NamedTab tab = new NamedTab(new ViewName(dashboardName, dashboardTitle), null);
                tab.setPane(dashboardView);
                tab.setCanClose(true);

                editMode = true;

                tabSet.addTab(tab);
                tabSet.selectTab(tab);
            }
        });
    }

    public void renderView(ViewPath viewPath) {
        // make sure we have at least a default dashboard tab
        if (null == tabSet || 0 == tabSet.getTabs().length) {
            return;
        }

        NamedTab[] tabs = tabSet.getTabs();

        // if nothing selected or pathtab does not exist, default to the first tab
        NamedTab selectedTab = tabs[0];
        selectedTabName = selectedTab.getName();

        if (!viewPath.isEnd()) {
            String pathTabName = viewPath.getCurrent().getPath();

            for (NamedTab tab : tabSet.getTabs()) {
                if (tab.getName().equals(pathTabName)) {
                    selectedTab = tab;
                    selectedTabName = pathTabName;
                    break;
                }
            }
        }

        if (!selectedTab.equals(tabSet.getSelectedTab())) {
            tabSet.selectTab(selectedTab);

        } else if (viewPath.isRefresh()) {
            refresh();
        }
    }

    public Dashboard getDashboard() {
        return selectedDashboard;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    public HashSet<Permission> getGlobalPermissions() {
        return globalPermissions;
    }

    public boolean supportsDashboardNameEdit() {
        return true;
    }

    public void updateDashboardNames() {
        for (Tab t : tabSet.getTabs()) {
            DashboardView view = (DashboardView) t.getPane();
            t.setTitle(view.getDashboard().getName());
        }
    }

    @Override
    public void refresh() {
        if (isInitialized()) {
            if (null != selectedDashboardView && selectedDashboardView.isDrawn()) {
                selectedDashboardView.rebuild();
            }
        }
    }

}
