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
import java.util.HashMap;
import java.util.List;

import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.inventory.queue.AutodiscoveryPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.inventory.resource.FavoriteResourcesPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.inventory.resource.graph.GraphPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.platform.PlatformSummaryPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.recent.alerts.RecentAlertsPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.recent.imported.RecentlyAddedResourcesPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.recent.operations.OperationsPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.recent.problems.ProblemResourcesPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.summary.InventorySummaryPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.summary.TagCloudPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.util.MashupPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.util.MessagePortlet;
import org.rhq.enterprise.gui.coregui.client.util.selenium.SeleniumUtility;

/**
 * @author Greg Hinkle
 */
public class PortletFactory {

    private static HashMap<String, PortletViewFactory> registeredPortletFactoryMap;
    private static HashMap<String, String> registeredPortletNameMap;

    static {
        registeredPortletFactoryMap = new HashMap<String, PortletViewFactory>();
        registeredPortletFactoryMap.put(InventorySummaryPortlet.KEY, InventorySummaryPortlet.Factory.INSTANCE);
        registeredPortletFactoryMap.put(RecentlyAddedResourcesPortlet.KEY,
            RecentlyAddedResourcesPortlet.Factory.INSTANCE);
        registeredPortletFactoryMap.put(PlatformSummaryPortlet.KEY, PlatformSummaryPortlet.Factory.INSTANCE);
        registeredPortletFactoryMap.put(AutodiscoveryPortlet.KEY, AutodiscoveryPortlet.Factory.INSTANCE);
        registeredPortletFactoryMap.put(RecentAlertsPortlet.KEY, RecentAlertsPortlet.Factory.INSTANCE);
        registeredPortletFactoryMap.put(GraphPortlet.KEY, GraphPortlet.Factory.INSTANCE);
        registeredPortletFactoryMap.put(TagCloudPortlet.KEY, TagCloudPortlet.Factory.INSTANCE);
        registeredPortletFactoryMap.put(FavoriteResourcesPortlet.KEY, FavoriteResourcesPortlet.Factory.INSTANCE);
        registeredPortletFactoryMap.put(MashupPortlet.KEY, MashupPortlet.Factory.INSTANCE);
        registeredPortletFactoryMap.put(MessagePortlet.KEY, MessagePortlet.Factory.INSTANCE);
        registeredPortletFactoryMap.put(ProblemResourcesPortlet.KEY, ProblemResourcesPortlet.Factory.INSTANCE);
        registeredPortletFactoryMap.put(OperationsPortlet.KEY, OperationsPortlet.Factory.INSTANCE);

        registeredPortletNameMap = new HashMap<String, String>(registeredPortletFactoryMap.size());
        registeredPortletNameMap.put(InventorySummaryPortlet.KEY, InventorySummaryPortlet.NAME);
        registeredPortletNameMap.put(RecentlyAddedResourcesPortlet.KEY, RecentlyAddedResourcesPortlet.NAME);
        registeredPortletNameMap.put(PlatformSummaryPortlet.KEY, PlatformSummaryPortlet.NAME);
        registeredPortletNameMap.put(AutodiscoveryPortlet.KEY, AutodiscoveryPortlet.NAME);
        registeredPortletNameMap.put(RecentAlertsPortlet.KEY, RecentAlertsPortlet.NAME);
        registeredPortletNameMap.put(GraphPortlet.KEY, GraphPortlet.NAME);
        registeredPortletNameMap.put(TagCloudPortlet.KEY, TagCloudPortlet.NAME);
        registeredPortletNameMap.put(FavoriteResourcesPortlet.KEY, FavoriteResourcesPortlet.NAME);
        registeredPortletNameMap.put(MashupPortlet.KEY, MashupPortlet.NAME);
        registeredPortletNameMap.put(MessagePortlet.KEY, MessagePortlet.NAME);
        registeredPortletNameMap.put(ProblemResourcesPortlet.KEY, ProblemResourcesPortlet.NAME);
        registeredPortletNameMap.put(OperationsPortlet.KEY, OperationsPortlet.NAME);
    }

    public static Portlet buildPortlet(PortletWindow portletWindow, DashboardPortlet storedPortlet) {

        PortletViewFactory viewFactory = registeredPortletFactoryMap.get(storedPortlet.getPortletKey());

        // TODO: Note, we're using a sequence generated ID here as a locatorId. This is not optimal for repeatable
        // tests as a change in the number of default portlets, or a change in test order could make a test
        // non-repeatable. But, at the moment we lack the infrastructure to generate a unique, predictable id. 
        Portlet view = viewFactory.getInstance(SeleniumUtility.getSafeId(storedPortlet.getPortletKey()) + "-"
            + Integer.toString(storedPortlet.getId()));
        view.configure(portletWindow, storedPortlet);

        //add code to initiate refresh cycle for portlets
        if (view instanceof AutoRefreshPortlet) {
            ((AutoRefreshPortlet) view).startRefreshCycle();
        }

        return view;
    }

    public static List<String> getRegisteredPortletKeys() {

        ArrayList<String> portletKeys = new ArrayList<String>(registeredPortletFactoryMap.keySet());
        return portletKeys;
    }

    /** 
     * @return Unmodifiable Map of registered portlet keys to names
     */
    public static HashMap<String, String> getRegisteredPortletNameMap() {

        return registeredPortletNameMap;
    }

    public static String getRegisteredPortletName(String key) {

        return registeredPortletNameMap.get(key);
    }

    public static PortletViewFactory getRegisteredPortletFactory(String key) {
        PortletViewFactory portletFactory = null;
        if ((key != null) & (!key.trim().isEmpty())) {
            portletFactory = registeredPortletFactoryMap.get(key);
        }
        return portletFactory;
    }

}
