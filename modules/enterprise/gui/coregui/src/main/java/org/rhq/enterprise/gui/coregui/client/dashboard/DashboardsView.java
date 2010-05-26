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
package org.rhq.enterprise.gui.coregui.client.dashboard;

import java.util.ArrayList;

import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.TabSet;

import org.rhq.core.domain.resource.InventorySummary;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.inventory.queue.AutodiscoveryPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.inventory.resource.graph.GraphPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.platform.PlatformPortletView;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.recent.alerts.RecentAlertsPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.recent.imported.RecentlyAddedView;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.summary.InventorySummaryView;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.summary.TagCloudPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.store.DashboardStore;
import org.rhq.enterprise.gui.coregui.client.dashboard.store.StoredDashboard;
import org.rhq.enterprise.gui.coregui.client.dashboard.store.StoredPortlet;

/**
 * @author Greg Hinkle
 */
public class DashboardsView extends VLayout {

    private TabSet tabSet;

    private DashboardStore dashboardStore;


    public DashboardsView() {
        setOverflow(Overflow.AUTO);
        setPadding(5);
        setWidth100();
        setHeight100();

    }

    @Override
    protected void onInit() {
        super.onInit();

        tabSet = new TabSet();
        tabSet.setWidth100();
        tabSet.setHeight100();

        dashboardStore = new DashboardStore();

        dashboardStore.getStoredDashboards().add(getDefaultDashboard());


        for (StoredDashboard dashboard : dashboardStore.getStoredDashboards()) {
            DashboardView dashboardView = new DashboardView(dashboard);

            Tab tab = new Tab(dashboard.getName());
            tab.setPane(dashboardView);

            tabSet.addTab(tab);
        }

        addMember(tabSet);
    }


    protected StoredDashboard getDefaultDashboard() {

        StoredDashboard dashboard = new StoredDashboard();
        dashboard.setName("Default Dashboard");
        dashboard.setColumns(2);
        dashboard.setColumnWidths("30%","70%");


        ArrayList<StoredPortlet> col1 = new ArrayList<StoredPortlet>();
        ArrayList<StoredPortlet> col2 = new ArrayList<StoredPortlet>();




        StoredPortlet summary = new StoredPortlet("Inventory Summary", InventorySummaryView.KEY, 250);
        col1.add(summary);

        StoredPortlet tagCloud = new StoredPortlet("Tag Cloud", TagCloudPortlet.KEY, 250);
        col1.add(tagCloud);

        StoredPortlet graph = new StoredPortlet("ghinkle (MAC OS X) CPU", GraphPortlet.KEY, 250);
        graph.setProperty("resourceId","10001");
        graph.setProperty("definitionId","10100");
        col1.add(graph);

        StoredPortlet graphTwo = new StoredPortlet("JBoss AS Txn / Min", GraphPortlet.KEY, 250);
        graphTwo.setProperty("resourceId","10003");
        graphTwo.setProperty("definitionId","10916");
        col1.add(graphTwo);



        // Experimental
//        StoredPortlet platformSummary = new StoredPortlet("Platform Summary", PlatformPortletView.KEY, 300);
//        col2.add(platformSummary);
//
        StoredPortlet discoveryQueue = new StoredPortlet("Discovery Queue", AutodiscoveryPortlet.KEY, 250);
        col2.add(discoveryQueue);

        StoredPortlet recentAlerts = new StoredPortlet("Recent Alerts", RecentAlertsPortlet.KEY, 250);
        col2.add(recentAlerts);

        StoredPortlet recentlyAdded = new StoredPortlet("Recently Added Resources", RecentlyAddedView.KEY, 250);
        col2.add(recentlyAdded);


        dashboard.getPortlets().add(col1);
        dashboard.getPortlets().add(col2);



        return dashboard;

    }
}
