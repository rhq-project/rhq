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
package org.rhq.coregui.client.dashboard;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Label;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.ImageManager;
import org.rhq.coregui.client.dashboard.portlets.groups.FavoriteGroupsPortlet;
import org.rhq.coregui.client.dashboard.portlets.groups.GroupAlertsPortlet;
import org.rhq.coregui.client.dashboard.portlets.groups.GroupBundleDeploymentsPortlet;
import org.rhq.coregui.client.dashboard.portlets.groups.GroupConfigurationUpdatesPortlet;
import org.rhq.coregui.client.dashboard.portlets.groups.GroupEventsPortlet;
import org.rhq.coregui.client.dashboard.portlets.groups.GroupMetricsPortlet;
import org.rhq.coregui.client.dashboard.portlets.groups.GroupOobsPortlet;
import org.rhq.coregui.client.dashboard.portlets.groups.GroupOperationsPortlet;
import org.rhq.coregui.client.dashboard.portlets.groups.GroupPkgHistoryPortlet;
import org.rhq.coregui.client.dashboard.portlets.inventory.groups.graph.ResourceGroupD3GraphPortlet;
import org.rhq.coregui.client.dashboard.portlets.inventory.queue.AutodiscoveryPortlet;
import org.rhq.coregui.client.dashboard.portlets.inventory.resource.FavoriteResourcesPortlet;
import org.rhq.coregui.client.dashboard.portlets.inventory.resource.graph.ResourceD3GraphPortlet;
import org.rhq.coregui.client.dashboard.portlets.platform.PlatformSummaryPortlet;
import org.rhq.coregui.client.dashboard.portlets.recent.alerts.RecentAlertsPortlet;
import org.rhq.coregui.client.dashboard.portlets.recent.events.RecentEventsPortlet;
import org.rhq.coregui.client.dashboard.portlets.recent.imported.RecentlyAddedResourcesPortlet;
import org.rhq.coregui.client.dashboard.portlets.recent.operations.OperationHistoryPortlet;
import org.rhq.coregui.client.dashboard.portlets.recent.operations.OperationSchedulePortlet;
import org.rhq.coregui.client.dashboard.portlets.recent.problems.ProblemResourcesPortlet;
import org.rhq.coregui.client.dashboard.portlets.resource.ResourceAlertsPortlet;
import org.rhq.coregui.client.dashboard.portlets.resource.ResourceBundleDeploymentsPortlet;
import org.rhq.coregui.client.dashboard.portlets.resource.ResourceConfigurationUpdatesPortlet;
import org.rhq.coregui.client.dashboard.portlets.resource.ResourceEventsPortlet;
import org.rhq.coregui.client.dashboard.portlets.resource.ResourceMetricsPortlet;
import org.rhq.coregui.client.dashboard.portlets.resource.ResourceOobsPortlet;
import org.rhq.coregui.client.dashboard.portlets.resource.ResourceOperationsPortlet;
import org.rhq.coregui.client.dashboard.portlets.resource.ResourcePkgHistoryPortlet;
import org.rhq.coregui.client.dashboard.portlets.resource.ResourceTimelinePortlet;
import org.rhq.coregui.client.dashboard.portlets.summary.InventorySummaryPortlet;
import org.rhq.coregui.client.dashboard.portlets.summary.TagCloudPortlet;
import org.rhq.coregui.client.dashboard.portlets.util.MashupPortlet;
import org.rhq.coregui.client.dashboard.portlets.util.MessagePortlet;
import org.rhq.coregui.client.util.message.Message;
import org.rhq.coregui.client.util.message.Message.Severity;
import org.rhq.coregui.client.util.preferences.UserPreferenceNames.UiSubsystem;

/**
 * @author Simeon Pinder
 * @author Jay Shaughnessy
 */
public class PortletFactory {

    private static final HashMap<String, PortletViewFactory> globalPortletFactoryMap;
    // although portlet names are I18N, they are assumed to be unique. This  maps portlet names to portlet keys,
    // and the keyset is sorted for convenient display.
    private static final TreeMap<String, String> globalPortletNameMap;
    // although portlet names are I18N, they are assumed to be unique. This maps portlet keys to portlet names,
    // and is suitable for a sorted Menu value map.
    private static final LinkedHashMap<String, String> globalPortletMenuMap;

    //Group portlet registrations, diff from default portlets as only applicable for specific group
    private static final HashMap<String, PortletViewFactory> groupPortletFactoryMap;
    private static final TreeMap<String, String> groupPortletNameMap;
    private static final LinkedHashMap<String, String> groupPortletMenuMap;

    //Resource portlet registrations, diff from default portlets as only applicable for specific resource
    private static final HashMap<String, PortletViewFactory> resourcePortletFactoryMap;
    private static final TreeMap<String, String> resourcePortletNameMap;
    private static final LinkedHashMap<String, String> resourcePortletMenuMap;
    private static HashMap<String, String> portletIconMap;

    static {
        // GLOBAL Portlets

        // Map portlet keys to portlet factories
        globalPortletFactoryMap = new HashMap<String, PortletViewFactory>();
        globalPortletFactoryMap.put(InventorySummaryPortlet.KEY, InventorySummaryPortlet.Factory.INSTANCE);
        globalPortletFactoryMap.put(RecentlyAddedResourcesPortlet.KEY, RecentlyAddedResourcesPortlet.Factory.INSTANCE);
        globalPortletFactoryMap.put(PlatformSummaryPortlet.KEY, PlatformSummaryPortlet.Factory.INSTANCE);
        globalPortletFactoryMap.put(AutodiscoveryPortlet.KEY, AutodiscoveryPortlet.Factory.INSTANCE);
        globalPortletFactoryMap.put(RecentAlertsPortlet.KEY, RecentAlertsPortlet.Factory.INSTANCE);
        globalPortletFactoryMap.put(RecentEventsPortlet.KEY, RecentEventsPortlet.Factory.INSTANCE);
        globalPortletFactoryMap.put(ResourceD3GraphPortlet.KEY, ResourceD3GraphPortlet.Factory.INSTANCE);
        globalPortletFactoryMap.put(ResourceGroupD3GraphPortlet.KEY, ResourceGroupD3GraphPortlet.Factory.INSTANCE);
        //conditionally add tags. Defaults to true, not available in JON builds.
        if (CoreGUI.isTagsEnabledForUI()) {
            globalPortletFactoryMap.put(TagCloudPortlet.KEY, TagCloudPortlet.Factory.INSTANCE);
        }
        globalPortletFactoryMap.put(FavoriteResourcesPortlet.KEY, FavoriteResourcesPortlet.Factory.INSTANCE);
        globalPortletFactoryMap.put(FavoriteGroupsPortlet.KEY, FavoriteGroupsPortlet.Factory.INSTANCE);
        globalPortletFactoryMap.put(MashupPortlet.KEY, MashupPortlet.Factory.INSTANCE);
        globalPortletFactoryMap.put(MessagePortlet.KEY, MessagePortlet.Factory.INSTANCE);
        globalPortletFactoryMap.put(ProblemResourcesPortlet.KEY, ProblemResourcesPortlet.Factory.INSTANCE);
        globalPortletFactoryMap.put(OperationHistoryPortlet.KEY, OperationHistoryPortlet.Factory.INSTANCE);
        globalPortletFactoryMap.put(OperationSchedulePortlet.KEY, OperationSchedulePortlet.Factory.INSTANCE);
        globalPortletFactoryMap.put(ResourceTimelinePortlet.KEY, ResourceTimelinePortlet.Factory.INSTANCE);

        // sorted map of portlet names to portlet keys
        globalPortletNameMap = new TreeMap<String, String>();
        globalPortletNameMap.put(InventorySummaryPortlet.NAME, InventorySummaryPortlet.KEY);
        globalPortletNameMap.put(RecentlyAddedResourcesPortlet.NAME, RecentlyAddedResourcesPortlet.KEY);
        globalPortletNameMap.put(PlatformSummaryPortlet.NAME, PlatformSummaryPortlet.KEY);
        globalPortletNameMap.put(AutodiscoveryPortlet.NAME, AutodiscoveryPortlet.KEY);
        globalPortletNameMap.put(RecentAlertsPortlet.NAME, RecentAlertsPortlet.KEY);
        globalPortletNameMap.put(RecentEventsPortlet.NAME, RecentEventsPortlet.KEY);
        globalPortletNameMap.put(ResourceD3GraphPortlet.NAME, ResourceD3GraphPortlet.KEY);
        globalPortletNameMap.put(ResourceGroupD3GraphPortlet.NAME, ResourceGroupD3GraphPortlet.KEY);
        //conditionally add tags. Defaults to true, not available in JON builds.
        if (CoreGUI.isTagsEnabledForUI()) {
            globalPortletNameMap.put(TagCloudPortlet.NAME, TagCloudPortlet.KEY);
        }
        globalPortletNameMap.put(FavoriteResourcesPortlet.NAME, FavoriteResourcesPortlet.KEY);
        globalPortletNameMap.put(FavoriteGroupsPortlet.NAME, FavoriteGroupsPortlet.KEY);
        globalPortletNameMap.put(MashupPortlet.NAME, MashupPortlet.KEY);
        globalPortletNameMap.put(MessagePortlet.NAME, MessagePortlet.KEY);
        globalPortletNameMap.put(ProblemResourcesPortlet.NAME, ProblemResourcesPortlet.KEY);
        globalPortletNameMap.put(OperationHistoryPortlet.NAME, OperationHistoryPortlet.KEY);
        globalPortletNameMap.put(OperationSchedulePortlet.NAME, OperationSchedulePortlet.KEY);
        globalPortletNameMap.put(ResourceTimelinePortlet.NAME, ResourceTimelinePortlet.KEY);

        globalPortletMenuMap = new LinkedHashMap<String, String>(globalPortletNameMap.size());
        for (String portletName : globalPortletNameMap.keySet()) {
            globalPortletMenuMap.put(globalPortletNameMap.get(portletName), portletName);
        }

        // GROUP Portlets

        // Map portlet keys to portlet factories
        groupPortletFactoryMap = new HashMap<String, PortletViewFactory>();
        groupPortletFactoryMap.put(GroupAlertsPortlet.KEY, GroupAlertsPortlet.Factory.INSTANCE);
        groupPortletFactoryMap.put(GroupMetricsPortlet.KEY, GroupMetricsPortlet.Factory.INSTANCE);
        groupPortletFactoryMap.put(GroupOobsPortlet.KEY, GroupOobsPortlet.Factory.INSTANCE);
        groupPortletFactoryMap.put(GroupEventsPortlet.KEY, GroupEventsPortlet.Factory.INSTANCE);
        groupPortletFactoryMap.put(GroupOperationsPortlet.KEY, GroupOperationsPortlet.Factory.INSTANCE);
        groupPortletFactoryMap.put(GroupPkgHistoryPortlet.KEY, GroupPkgHistoryPortlet.Factory.INSTANCE);
        groupPortletFactoryMap.put(GroupBundleDeploymentsPortlet.KEY, GroupBundleDeploymentsPortlet.Factory.INSTANCE);
        groupPortletFactoryMap.put(GroupConfigurationUpdatesPortlet.KEY,
            GroupConfigurationUpdatesPortlet.Factory.INSTANCE);

        // sorted map of portlet names to portlet keys
        groupPortletNameMap = new TreeMap<String, String>();
        groupPortletNameMap.put(GroupAlertsPortlet.NAME, GroupAlertsPortlet.KEY);
        groupPortletNameMap.put(GroupMetricsPortlet.NAME, GroupMetricsPortlet.KEY);
        groupPortletNameMap.put(GroupOobsPortlet.NAME, GroupOobsPortlet.KEY);
        groupPortletNameMap.put(GroupEventsPortlet.NAME, GroupEventsPortlet.KEY);
        groupPortletNameMap.put(GroupOperationsPortlet.NAME, GroupOperationsPortlet.KEY);
        groupPortletNameMap.put(GroupPkgHistoryPortlet.NAME, GroupPkgHistoryPortlet.KEY);
        groupPortletNameMap.put(GroupBundleDeploymentsPortlet.NAME, GroupBundleDeploymentsPortlet.KEY);
        groupPortletNameMap.put(GroupConfigurationUpdatesPortlet.NAME, GroupConfigurationUpdatesPortlet.KEY);

        groupPortletMenuMap = new LinkedHashMap<String, String>(groupPortletNameMap.size());
        for (String portletName : groupPortletNameMap.keySet()) {
            groupPortletMenuMap.put(groupPortletNameMap.get(portletName), portletName);
        }

        // Resource Portlets

        // Map portlet keys to portlet factories
        resourcePortletFactoryMap = new HashMap<String, PortletViewFactory>();
        resourcePortletFactoryMap.put(ResourceMetricsPortlet.KEY, ResourceMetricsPortlet.Factory.INSTANCE);
        resourcePortletFactoryMap.put(ResourceEventsPortlet.KEY, ResourceEventsPortlet.Factory.INSTANCE);
        resourcePortletFactoryMap.put(ResourceOobsPortlet.KEY, ResourceOobsPortlet.Factory.INSTANCE);
        resourcePortletFactoryMap.put(ResourceAlertsPortlet.KEY, ResourceAlertsPortlet.Factory.INSTANCE);
        resourcePortletFactoryMap.put(ResourceOperationsPortlet.KEY, ResourceOperationsPortlet.Factory.INSTANCE);
        resourcePortletFactoryMap.put(ResourcePkgHistoryPortlet.KEY, ResourcePkgHistoryPortlet.Factory.INSTANCE);
        resourcePortletFactoryMap.put(ResourceBundleDeploymentsPortlet.KEY,
            ResourceBundleDeploymentsPortlet.Factory.INSTANCE);
        resourcePortletFactoryMap.put(ResourceConfigurationUpdatesPortlet.KEY,
            ResourceConfigurationUpdatesPortlet.Factory.INSTANCE);

        // sorted map of portlet names to portlet keys
        resourcePortletNameMap = new TreeMap<String, String>();
        resourcePortletNameMap.put(ResourceMetricsPortlet.NAME, ResourceMetricsPortlet.KEY);
        resourcePortletNameMap.put(ResourceEventsPortlet.NAME, ResourceEventsPortlet.KEY);
        resourcePortletNameMap.put(ResourceOobsPortlet.NAME, ResourceOobsPortlet.KEY);
        resourcePortletNameMap.put(ResourceOperationsPortlet.NAME, ResourceOperationsPortlet.KEY);
        resourcePortletNameMap.put(ResourcePkgHistoryPortlet.NAME, ResourcePkgHistoryPortlet.KEY);
        resourcePortletNameMap.put(ResourceAlertsPortlet.NAME, ResourceAlertsPortlet.KEY);
        resourcePortletNameMap.put(ResourceBundleDeploymentsPortlet.NAME, ResourceBundleDeploymentsPortlet.KEY);
        resourcePortletNameMap.put(ResourceConfigurationUpdatesPortlet.NAME, ResourceConfigurationUpdatesPortlet.KEY);

        resourcePortletMenuMap = new LinkedHashMap<String, String>(resourcePortletNameMap.size());
        for (String portletName : resourcePortletNameMap.keySet()) {
            resourcePortletMenuMap.put(resourcePortletNameMap.get(portletName), portletName);
        }

        //############## Portlet icon mappings  ############################################
        //register portlet names
        portletIconMap = new HashMap<String, String>(globalPortletFactoryMap.size());
        portletIconMap.put(GroupAlertsPortlet.KEY, ImageManager.getAlertIcon());
        portletIconMap.put(ResourceAlertsPortlet.KEY, ImageManager.getAlertIcon());
        portletIconMap.put(GroupMetricsPortlet.KEY, ImageManager.getMonitorIcon());
        portletIconMap.put(ResourceMetricsPortlet.KEY, ImageManager.getMonitorIcon());
        portletIconMap.put(GroupOobsPortlet.KEY, ImageManager.getMonitorFailedIcon());
        portletIconMap.put(ResourceOobsPortlet.KEY, ImageManager.getMonitorFailedIcon());
        portletIconMap.put(GroupEventsPortlet.KEY, ImageManager.getEventIcon());
        portletIconMap.put(ResourceEventsPortlet.KEY, ImageManager.getEventIcon());
        portletIconMap.put(GroupOperationsPortlet.KEY, ImageManager.getOperationIcon());
        portletIconMap.put(ResourceOperationsPortlet.KEY, ImageManager.getOperationIcon());
        portletIconMap.put(GroupPkgHistoryPortlet.KEY, ImageManager.getActivityPackageIcon());
        portletIconMap.put(ResourcePkgHistoryPortlet.KEY, ImageManager.getActivityPackageIcon());
        portletIconMap.put(GroupBundleDeploymentsPortlet.KEY, ImageManager.getBundleIcon());
        portletIconMap.put(ResourceBundleDeploymentsPortlet.KEY, ImageManager.getBundleIcon());
        portletIconMap.put(GroupConfigurationUpdatesPortlet.KEY, ImageManager.getConfigureIcon());
        portletIconMap.put(ResourceConfigurationUpdatesPortlet.KEY, ImageManager.getConfigureIcon());
    }

    private PortletFactory() {
        // static utility class
    }

    public static Portlet buildPortlet(PortletWindow portletWindow, DashboardPortlet storedPortlet,
        EntityContext context) {

        PortletViewFactory viewFactory = globalPortletFactoryMap.get(storedPortlet.getPortletKey());
        if (viewFactory == null) {//check group view factory
            viewFactory = groupPortletFactoryMap.get(storedPortlet.getPortletKey());

            if (viewFactory == null) {//check resource view factory
                viewFactory = resourcePortletFactoryMap.get(storedPortlet.getPortletKey());

                if (viewFactory == null) {
                    Message msg = new Message("Bad portlet: " + storedPortlet, Severity.Warning);
                    CoreGUI.getMessageCenter().notify(msg);
                    class InvalidPortlet extends Label implements Portlet {
                        InvalidPortlet() {
                            super(CoreGUI.getMessages().view_portlet_factory_invalidPortlet());
                        }

                        @Override
                        public Canvas getHelpCanvas() {
                            return new Label(getContents());
                        }

                        @Override
                        public void configure(PortletWindow portletWindow, DashboardPortlet storedPortlet) {
                        }
                    }
                    ;
                    return new InvalidPortlet();
                }
            }
        }

        Portlet view = viewFactory.getInstance(context);
        view.configure(portletWindow, storedPortlet);

        //add code to initiate refresh cycle for portlets
        if (view instanceof AutoRefreshPortlet) {
            ((AutoRefreshPortlet) view).startRefreshCycle();
        }

        return view;
    }

    public static boolean canBeAdded(String portletKey, Map<UiSubsystem, Boolean> showSubsystems) {
        if (!showSubsystems.get(UiSubsystem.ALERTS)) {
            if (ResourceAlertsPortlet.KEY.equals(portletKey) || GroupAlertsPortlet.KEY.equals(portletKey)
                || RecentAlertsPortlet.KEY.equals(portletKey)) {
                return false;
            }
        }
        if (!showSubsystems.get(UiSubsystem.CONFIG)) {
            if (ResourceConfigurationUpdatesPortlet.KEY.equals(portletKey)
                || GroupConfigurationUpdatesPortlet.KEY.equals(portletKey)) {
                return false;
            }
        }
        if (!showSubsystems.get(UiSubsystem.CONTENT)) {
            if (ResourcePkgHistoryPortlet.KEY.equals(portletKey) || GroupPkgHistoryPortlet.KEY.equals(portletKey)
                || ResourceBundleDeploymentsPortlet.KEY.equals(portletKey)
                || GroupBundleDeploymentsPortlet.KEY.equals(portletKey)) {
                return false;
            }
        }
        if (!showSubsystems.get(UiSubsystem.EVENTS)) {
            if (ResourceEventsPortlet.KEY.equals(portletKey) || GroupEventsPortlet.KEY.equals(portletKey)
                || RecentEventsPortlet.KEY.equals(portletKey)) {
                return false;
            }
        }
        if (!showSubsystems.get(UiSubsystem.OPERATIONS)) {
            if (ResourceOperationsPortlet.KEY.equals(portletKey) || GroupOperationsPortlet.KEY.equals(portletKey)
                || OperationHistoryPortlet.KEY.equals(portletKey) || OperationSchedulePortlet.KEY.equals(portletKey)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return Unprotected, make a copy if you need to alter the map entries.
     */
    public static LinkedHashMap<String, String> getGlobalPortletMenuMap() {
        return globalPortletMenuMap;
    }

    /**
     * @return Unprotected, make a copy if you need to alter the map entries.
     */
    public static LinkedHashMap<String, String> getGroupPortletMenuMap() {
        return groupPortletMenuMap;
    }

    /**
     * @return Unprotected, make a copy if you need to alter the map entries.
     */
    public static LinkedHashMap<String, String> getResourcePortletMenuMap() {
        return resourcePortletMenuMap;
    }

    public static String getRegisteredPortletIcon(String key) {

        return portletIconMap.get(key);
    }

}
