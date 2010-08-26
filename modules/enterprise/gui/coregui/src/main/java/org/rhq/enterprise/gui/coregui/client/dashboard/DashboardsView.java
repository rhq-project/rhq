/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.dashboard;

import java.util.List;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.TabSet;
import com.smartgwt.client.widgets.tab.events.CloseClickHandler;
import com.smartgwt.client.widgets.tab.events.TabCloseClickEvent;
import com.smartgwt.client.widgets.tab.events.TabSelectedEvent;
import com.smartgwt.client.widgets.tab.events.TabSelectedHandler;

import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.dashboard.Dashboard;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.inventory.queue.AutodiscoveryPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.recent.alerts.RecentAlertsPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.recent.imported.RecentlyAddedView;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.recent.operations.OperationsPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.recent.problems.ProblemResourcesPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.summary.InventorySummaryView;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.summary.TagCloudPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.util.MashupPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.util.MessagePortlet;
import org.rhq.enterprise.gui.coregui.client.gwt.DashboardGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;

/**
 * @author Greg Hinkle
 */
public class DashboardsView extends VLayout implements BookmarkableView {

    private TabSet tabSet;

    private IButton editButton;

    private boolean editMode = false;

    private List<Dashboard> dashboards;

    private DashboardView selectedDashboardView;
    private Dashboard selectedDashboard;

    private DashboardGWTServiceAsync dashboardService = GWTServiceLookup.getDashboardService();

    private String selectedTab;

    public DashboardsView() {
        setOverflow(Overflow.AUTO);
        setPadding(5);
        setWidth100();
        setHeight100();

    }

    @Override
    protected void onInit() {
        super.onInit();

        dashboardService.findDashboardsForSubject(new AsyncCallback<List<Dashboard>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("Failed to load user dashboards", caught);
            }

            public void onSuccess(List<Dashboard> result) {
                if (result.isEmpty()) {
                    result.add(getDefaultDashboard());
                }
                updateDashboards(result);
            }
        });
    }

    private void updateDashboards(List<Dashboard> dashboards) {
        removeMembers(getMembers());
        this.dashboards = dashboards;

        tabSet = new TabSet();

        tabSet.setWidth100();
        tabSet.setHeight100();

        tabSet.setCanCloseTabs(true);

        editButton = new IButton(editMode ? "View Mode" : "Edit Mode");
        editButton.setAutoFit(true);
        editButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                editMode = !editMode;
                editButton.setTitle(editMode ? "View Mode" : "Edit Mode");
                selectedDashboardView.setEditMode(editMode);
            }
        });

        final IButton newDashboardButton = new IButton("New Dashboard");
        newDashboardButton.setAutoFit(true);
        newDashboardButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                addNewDashboard();
            }
        });

        HLayout buttons = new HLayout(5);
        buttons.addMember(editButton);
        buttons.addMember(newDashboardButton);

        tabSet.setTabBarControls(buttons);

        tabSet.addTabSelectedHandler(new TabSelectedHandler() {
            public void onTabSelected(TabSelectedEvent tabSelectedEvent) {
                History.newItem("Dashboard/" + tabSelectedEvent.getTab().getTitle(), false);
                selectedDashboardView = (DashboardView) tabSelectedEvent.getTab().getPane();
                selectedDashboard = selectedDashboardView.getDashboard();
                selectedDashboardView.setEditMode(editMode);
            }
        });

        for (Dashboard dashboard : dashboards) {
            DashboardView dashboardView = new DashboardView(this, dashboard);

            Tab tab = new Tab(dashboard.getName());
            tab.setPane(dashboardView);
            tab.setCanClose(true);

            tabSet.addTab(tab);
            if (dashboard.getName().equals(selectedTab)) {
                tabSet.selectTab(tab);
            }
        }

        tabSet.addCloseClickHandler(new CloseClickHandler() {
            public void onCloseClick(final TabCloseClickEvent tabCloseClickEvent) {
                final DashboardView dashboardView = (DashboardView) tabCloseClickEvent.getTab().getPane();
                SC.ask("Are you sure you want to delete [" + tabCloseClickEvent.getTab().getTitle() + "]?",
                    new BooleanCallback() {
                        public void execute(Boolean aBoolean) {
                            if (aBoolean) {
                                dashboardView.delete();
                            } else {
                                tabCloseClickEvent.cancel();
                            }
                        }
                    });
            }
        });

        addMember(tabSet);

    }

    protected Dashboard getDefaultDashboard() {

        Dashboard dashboard = new Dashboard();
        dashboard.setName("Default Dashboard");
        dashboard.setColumns(2);
        dashboard.setColumnWidths("32%", "68%");
        dashboard.getConfiguration().put(new PropertySimple(Dashboard.CFG_BACKGROUND, "#F1F2F3"));

        DashboardPortlet summary = new DashboardPortlet("Inventory Summary", InventorySummaryView.KEY, 230);
        dashboard.addPortlet(summary, 0, 0);

        DashboardPortlet tagCloud = new DashboardPortlet("Tag Cloud", TagCloudPortlet.KEY, 200);
        dashboard.addPortlet(tagCloud, 0, 1);

        // Experimental
        //        StoredPortlet platformSummary = new StoredPortlet("Platform Summary", PlatformPortletView.KEY, 300);
        //        col2.add(platformSummary);

        DashboardPortlet welcome = new DashboardPortlet("Welcome To RHQ", MessagePortlet.KEY, 180);
        welcome.getConfiguration().put(
            new PropertySimple("message", "<h1>Welcome to RHQ</h1>\n"
                + "<p>The RHQ project is an abstraction and plug-in based systems management suite that provides "
                + "extensible and integrated systems management for multiple products and platforms across a set "
                + "of core features. The project is designed with layered modules that provide a flexible "
                + "architecture for deployment. It delivers a core user interface that delivers audited and "
                + "historical management across an entire enterprise. A Server/Agent architecture provides "
                + "remote management and plugins implement all specific support for managed products.</p>\n"
                + "<p>This default dashboard can be edited by clicking the \"edit mode\" button above.</p>"));
        dashboard.addPortlet(welcome, 1, 0);

        DashboardPortlet news = new DashboardPortlet("RHQ News", MashupPortlet.KEY, 320);
        news.getConfiguration().put(
            new PropertySimple("address", "http://rhq-project.org/display/RHQ/RHQ+News?decorator=popup"));
        dashboard.addPortlet(news, 1, 1);
        //
        DashboardPortlet discoveryQueue = new DashboardPortlet("Discovery Queue", AutodiscoveryPortlet.KEY, 250);
        dashboard.addPortlet(discoveryQueue, 1, 2);

        DashboardPortlet recentAlerts = new DashboardPortlet("Recent Alerts", RecentAlertsPortlet.KEY, 250);
        dashboard.addPortlet(recentAlerts, 1, 3);

        DashboardPortlet recentlyAdded = new DashboardPortlet("Recently Added Resources", RecentlyAddedView.KEY, 250);
        dashboard.addPortlet(recentlyAdded, 1, 4);

        DashboardPortlet operations = new DashboardPortlet("Operations", OperationsPortlet.KEY, 250);
        dashboard.addPortlet(operations, 1, 5);

        DashboardPortlet hasAlertsCurrentlyUnavailable = new DashboardPortlet("Has Alerts or Currently Unavailable",
            ProblemResourcesPortlet.KEY, 250);
        dashboard.addPortlet(hasAlertsCurrentlyUnavailable, 1, 6);

        return dashboard;

    }

    public void addNewDashboard() {

        Dashboard dashboard = new Dashboard();
        dashboard.setName("Dashboard");

        int i = 1;
        while (true) {
            boolean exists = false;
            for (Dashboard db : dashboards) {
                if (("Dashboard " + i).equals(db.getName()))
                    exists = true;
            }
            if (!exists) {
                break;
            }
        }
        dashboard.setName("Dashboard " + i);

        dashboard.setColumns(2);
        dashboard.setColumnWidths("30%", "70%");

        dashboardService.storeDashboard(dashboard, new AsyncCallback<Dashboard>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("Failed to add new dashboard", caught);
            }

            public void onSuccess(Dashboard result) {
                DashboardView dashboardView = new DashboardView(DashboardsView.this, result);

                Tab tab = new Tab(result.getName());
                tab.setPane(dashboardView);
                tab.setCanClose(true);

                tabSet.addTab(tab);

                tabSet.selectTab(tab);
                editMode = true;
                editButton.setTitle(editMode ? "View Mode" : "Edit Mode");
                dashboardView.setEditMode(editMode);

            }
        });
    }

    public void updateNames() {
        for (Tab t : tabSet.getTabs()) {
            DashboardView view = (DashboardView) t.getPane();
            t.setTitle(view.getDashboard().getName());
        }
    }

    public void renderView(ViewPath viewPath) {
        if (!viewPath.isEnd()) {
            selectedTab = viewPath.getCurrent().getPath();
            //added to avoid NPE in gwt debug window. 
            if (tabSet != null) {
                for (Tab tab : tabSet.getTabs()) {
                    if (tab.getTitle().equals(selectedTab)) {
                        tabSet.selectTab(tab);
                    }
                }
            } else {
                System.out.println("WARN: While rendering DashboardsView tabSet is null.");
            }
        }
    }

    public Dashboard getDashboard() {
        return selectedDashboard;
    }
}
