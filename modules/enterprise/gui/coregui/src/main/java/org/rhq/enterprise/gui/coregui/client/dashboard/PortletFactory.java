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
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.groups.GroupAlertsPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.groups.GroupBundleDeploymentsPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.groups.GroupEventsPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.groups.GroupMetricsPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.groups.GroupOobsPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.groups.GroupOperationsPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.groups.GroupPkgHistoryPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.inventory.queue.AutodiscoveryPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.inventory.resource.FavoriteResourcesPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.inventory.resource.graph.GraphPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.platform.PlatformSummaryPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.recent.alerts.RecentAlertsPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.recent.imported.RecentlyAddedResourcesPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.recent.operations.OperationsPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.recent.problems.ProblemResourcesPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.resource.ResourceAlertsPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.resource.ResourceBundleDeploymentsPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.resource.ResourceEventsPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.resource.ResourceMetricsPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.resource.ResourceOobsPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.resource.ResourceOperationsPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.resource.ResourcePkgHistoryPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.summary.InventorySummaryPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.summary.TagCloudPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.util.MashupPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.util.MessagePortlet;

/**
 * @author Simeon Pinder
 * @author Greg Hinkle
 */
public class PortletFactory {

    private static final HashMap<String, PortletViewFactory> registeredPortletFactoryMap;
    private static final HashMap<String, String> registeredPortletNameMap;

    //Group portlet registrations, diff from default portlets as only applicable for specific group
    private static final HashMap<String, PortletViewFactory> registeredGroupPortletFactoryMap;
    //Resource portlet registrations, diff from default portlets as only applicable for specific resource
    private static final HashMap<String, PortletViewFactory> registeredResourcePortletFactoryMap;
    private static final HashMap<String, String> registeredGroupPortletNameMap;
    private static final HashMap<String, String> registeredResourcePortletNameMap;
    private static final HashMap<String, String> registeredPortletIconMap;

    static {
        //############## Default Dashboard  ############################
        //defines portlet factory mappings for landing page Dashboard
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

        //defines portlet name mappings for landing page Dashboard
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

        //############## Group Activity Dashboard  ############################################
        //defines mapping for Group Activity Dashboard
        registeredGroupPortletFactoryMap = new HashMap<String, PortletViewFactory>();
        registeredGroupPortletFactoryMap.put(GroupAlertsPortlet.KEY, GroupAlertsPortlet.Factory.INSTANCE);
        registeredGroupPortletFactoryMap.put(GroupMetricsPortlet.KEY, GroupMetricsPortlet.Factory.INSTANCE);
        registeredGroupPortletFactoryMap.put(GroupOobsPortlet.KEY, GroupOobsPortlet.Factory.INSTANCE);
        registeredGroupPortletFactoryMap.put(GroupEventsPortlet.KEY, GroupEventsPortlet.Factory.INSTANCE);
        registeredGroupPortletFactoryMap.put(GroupOperationsPortlet.KEY, GroupOperationsPortlet.Factory.INSTANCE);
        registeredGroupPortletFactoryMap.put(GroupPkgHistoryPortlet.KEY, GroupPkgHistoryPortlet.Factory.INSTANCE);
        registeredGroupPortletFactoryMap.put(GroupBundleDeploymentsPortlet.KEY,
            GroupBundleDeploymentsPortlet.Factory.INSTANCE);

        //register group portlet names
        registeredGroupPortletNameMap = new HashMap<String, String>(registeredGroupPortletFactoryMap.size());
        registeredGroupPortletNameMap.put(GroupAlertsPortlet.KEY, GroupAlertsPortlet.NAME);
        registeredGroupPortletNameMap.put(GroupMetricsPortlet.KEY, GroupMetricsPortlet.NAME);
        registeredGroupPortletNameMap.put(GroupOobsPortlet.KEY, GroupOobsPortlet.NAME);
        registeredGroupPortletNameMap.put(GroupEventsPortlet.KEY, GroupEventsPortlet.NAME);
        registeredGroupPortletNameMap.put(GroupOperationsPortlet.KEY, GroupOperationsPortlet.NAME);
        registeredGroupPortletNameMap.put(GroupPkgHistoryPortlet.KEY, GroupPkgHistoryPortlet.NAME);
        registeredGroupPortletNameMap.put(GroupBundleDeploymentsPortlet.KEY, GroupBundleDeploymentsPortlet.NAME);

        //############## Resource Activity Dashboard  ############################################
        //defines mapping for Group Activity Dashboard
        registeredResourcePortletFactoryMap = new HashMap<String, PortletViewFactory>();
        registeredResourcePortletFactoryMap.put(ResourceMetricsPortlet.KEY, ResourceMetricsPortlet.Factory.INSTANCE);
        registeredResourcePortletFactoryMap.put(ResourceEventsPortlet.KEY, ResourceEventsPortlet.Factory.INSTANCE);
        registeredResourcePortletFactoryMap.put(ResourceOobsPortlet.KEY, ResourceOobsPortlet.Factory.INSTANCE);
        registeredResourcePortletFactoryMap.put(ResourceAlertsPortlet.KEY, ResourceAlertsPortlet.Factory.INSTANCE);
        registeredResourcePortletFactoryMap.put(ResourceOperationsPortlet.KEY,
            ResourceOperationsPortlet.Factory.INSTANCE);
        registeredResourcePortletFactoryMap.put(ResourcePkgHistoryPortlet.KEY,
            ResourcePkgHistoryPortlet.Factory.INSTANCE);
        registeredResourcePortletFactoryMap.put(ResourceBundleDeploymentsPortlet.KEY,
            ResourceBundleDeploymentsPortlet.Factory.INSTANCE);

        //register resource portlet names
        registeredResourcePortletNameMap = new HashMap<String, String>(registeredResourcePortletFactoryMap.size());
        registeredResourcePortletNameMap.put(ResourceMetricsPortlet.KEY, ResourceMetricsPortlet.NAME);
        registeredResourcePortletNameMap.put(ResourceEventsPortlet.KEY, ResourceEventsPortlet.NAME);
        registeredResourcePortletNameMap.put(ResourceOobsPortlet.KEY, ResourceOobsPortlet.NAME);
        registeredResourcePortletNameMap.put(ResourceOperationsPortlet.KEY, ResourceOperationsPortlet.NAME);
        registeredResourcePortletNameMap.put(ResourcePkgHistoryPortlet.KEY, ResourcePkgHistoryPortlet.NAME);
        registeredResourcePortletNameMap.put(ResourceAlertsPortlet.KEY, ResourceAlertsPortlet.NAME);
        registeredResourcePortletNameMap.put(ResourceBundleDeploymentsPortlet.KEY,
            ResourceBundleDeploymentsPortlet.NAME);

        //############## Portlet icon mappings  ############################################
        //register portlet names
        registeredPortletIconMap = new HashMap<String, String>(registeredPortletFactoryMap.size());
        registeredPortletIconMap.put(GroupAlertsPortlet.KEY, ImageManager.getAlertIcon());
        registeredPortletIconMap.put(ResourceAlertsPortlet.KEY, ImageManager.getAlertIcon());
        registeredPortletIconMap.put(GroupMetricsPortlet.KEY, ImageManager.getMonitorIcon());
        registeredPortletIconMap.put(ResourceMetricsPortlet.KEY, ImageManager.getMonitorIcon());
        registeredPortletIconMap.put(GroupOobsPortlet.KEY, ImageManager.getMonitorFailedIcon());
        registeredPortletIconMap.put(ResourceOobsPortlet.KEY, ImageManager.getMonitorFailedIcon());
        registeredPortletIconMap.put(GroupEventsPortlet.KEY, ImageManager.getEventIcon());
        registeredPortletIconMap.put(ResourceEventsPortlet.KEY, ImageManager.getEventIcon());
        registeredPortletIconMap.put(GroupOperationsPortlet.KEY, ImageManager.getOperationIcon());
        registeredPortletIconMap.put(ResourceOperationsPortlet.KEY, ImageManager.getOperationIcon());
        registeredPortletIconMap.put(GroupPkgHistoryPortlet.KEY, ImageManager.getActivityPackageIcon());
        registeredPortletIconMap.put(ResourcePkgHistoryPortlet.KEY, ImageManager.getActivityPackageIcon());
        registeredPortletIconMap.put(GroupBundleDeploymentsPortlet.KEY, ImageManager.getBundleIcon());
        registeredPortletIconMap.put(ResourceBundleDeploymentsPortlet.KEY, ImageManager.getBundleIcon());
    }

    public static Portlet buildPortlet(String locatorId, PortletWindow portletWindow, DashboardPortlet storedPortlet) {

        PortletViewFactory viewFactory = registeredPortletFactoryMap.get(storedPortlet.getPortletKey());
        if (viewFactory == null) {//check group view factory
            viewFactory = registeredGroupPortletFactoryMap.get(storedPortlet.getPortletKey());
        }
        if (viewFactory == null) {//check resource view factory
            viewFactory = registeredResourcePortletFactoryMap.get(storedPortlet.getPortletKey());
        }

        Portlet view = viewFactory.getInstance(locatorId);
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

    public static List<String> getRegisteredGroupPortletKeys() {

        ArrayList<String> portletKeys = new ArrayList<String>(registeredGroupPortletFactoryMap.keySet());
        return portletKeys;
    }

    public static List<String> getRegisteredResourcePortletKeys() {

        ArrayList<String> portletKeys = new ArrayList<String>(registeredResourcePortletFactoryMap.keySet());
        return portletKeys;
    }

    /** 
     * @return Unmodifiable Map of registered portlet keys to names
     */
    public static HashMap<String, String> getRegisteredPortletNameMap() {

        return registeredPortletNameMap;
    }

    public static HashMap<String, String> getRegisteredGroupPortletNameMap() {

        return registeredGroupPortletNameMap;
    }

    public static HashMap<String, String> getRegisteredResourcePortletNameMap() {

        return registeredResourcePortletNameMap;
    }

    public static String getRegisteredPortletName(String key) {

        return registeredPortletNameMap.get(key);
    }

    public static String getRegisteredGroupPortletName(String key) {

        return registeredGroupPortletNameMap.get(key);
    }

    public static String getRegisteredResourcePortletName(String key) {

        return registeredResourcePortletNameMap.get(key);
    }

    public static String getRegisteredPortletIcon(String key) {

        return registeredPortletIconMap.get(key);
    }

    public static PortletViewFactory getRegisteredPortletFactory(String key) {
        PortletViewFactory portletFactory = null;
        if ((key != null) & (!key.trim().isEmpty())) {
            portletFactory = registeredPortletFactoryMap.get(key);
        }
        return portletFactory;
    }

    public static PortletViewFactory getRegisteredGroupPortletFactory(String key) {
        PortletViewFactory portletFactory = null;
        if ((key != null) & (!key.trim().isEmpty())) {
            portletFactory = registeredGroupPortletFactoryMap.get(key);
        }
        return portletFactory;
    }

    public static PortletViewFactory getRegisteredResourcePortletFactory(String key) {
        PortletViewFactory portletFactory = null;
        if ((key != null) & (!key.trim().isEmpty())) {
            portletFactory = registeredResourcePortletFactoryMap.get(key);
        }
        return portletFactory;
    }

}
