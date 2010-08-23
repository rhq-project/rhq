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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail;

import java.util.Set;

import com.google.gwt.user.client.History;
import com.smartgwt.client.types.Side;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceTypeFacet;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.resource.composite.ResourcePermission;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.alert.definitions.ResourceAlertDefinitionsView;
import org.rhq.enterprise.gui.coregui.client.components.FullHTMLPane;
import org.rhq.enterprise.gui.coregui.client.components.tab.SubTab;
import org.rhq.enterprise.gui.coregui.client.components.tab.TwoLevelTab;
import org.rhq.enterprise.gui.coregui.client.components.tab.TwoLevelTabSelectedEvent;
import org.rhq.enterprise.gui.coregui.client.components.tab.TwoLevelTabSelectedHandler;
import org.rhq.enterprise.gui.coregui.client.components.tab.TwoLevelTabSet;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceSearchView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceSelectListener;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.configuration.ConfigurationHistoryView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.configuration.ResourceConfigurationEditView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.inventory.PluginConfigurationEditView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.GraphListView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.schedules.SchedulesView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.summary.DashboardView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.summary.OverviewView;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * Right panel of the Resource view.
 *
 * @author Greg Hinkle
 */
public class ResourceDetailView extends LocatableVLayout implements BookmarkableView, ResourceSelectListener,
    TwoLevelTabSelectedHandler {

    private static final String DEFAULT_TAB_NAME = "Summary";

    private ResourceComposite resourceComposite;

    private TwoLevelTabSet topTabSet;

    private TwoLevelTab summaryTab;
    private TwoLevelTab monitoringTab;
    private TwoLevelTab inventoryTab;
    private TwoLevelTab operationsTab;
    private TwoLevelTab alertsTab;
    private TwoLevelTab configurationTab;
    private TwoLevelTab eventsTab;
    private TwoLevelTab contentTab;

    private SubTab summaryOverview;
    private SubTab summaryDashboard;
    private SubTab summaryTimeline;
    private SubTab monitorGraphs;
    private SubTab monitorTables;
    private SubTab monitorTraits;
    private SubTab monitorAvail;
    private SubTab monitorSched;
    private SubTab monitorCallTime;
    private SubTab inventoryChildren;
    private SubTab inventoryConn;
    private SubTab opHistory;
    private SubTab opSched;
    private SubTab alertHistory;
    private SubTab alertDef;
    private SubTab alertDelete;
    private SubTab configCurrent;
    private SubTab configHistory;
    private SubTab eventHistory;
    private SubTab contentDeployed;
    private SubTab contentNew;
    private SubTab contentSubscrip;
    private SubTab contentHistory;

    private ResourceTitleBar titleBar;

    public ResourceDetailView(String locatorId) {
        super(locatorId);
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        setWidth100();
        setHeight100();

        // The Tabs section

        topTabSet = new TwoLevelTabSet(getLocatorId());
        topTabSet.setTabBarPosition(Side.TOP);
        topTabSet.setWidth100();
        topTabSet.setHeight100();
        topTabSet.setEdgeMarginSize(0);
        topTabSet.setEdgeSize(0);

        summaryTab = new TwoLevelTab(topTabSet.extendLocatorId("Summary"), "Summary", "/images/icons/Service_up_16.png");
        summaryOverview = new SubTab(summaryTab.extendLocatorId("Overview"), "Overview", null);
        summaryDashboard = new SubTab(summaryTab.extendLocatorId("Dashboard"), "Dashboard", null);
        summaryTimeline = new SubTab(summaryTab.extendLocatorId("Timeline"), "Timeline", null);
        summaryTab.registerSubTabs(summaryOverview, summaryDashboard, summaryTimeline);

        monitoringTab = new TwoLevelTab(topTabSet.extendLocatorId("Monitoring"), "Monitoring",
            "/images/icons/Monitor_grey_16.png");
        monitorGraphs = new SubTab(monitoringTab.extendLocatorId("Graphs"), "Graphs", null);
        monitorTables = new SubTab(monitoringTab.extendLocatorId("Tables"), "Tables", null);
        monitorTraits = new SubTab(monitoringTab.extendLocatorId("Traits"), "Traits", null);
        monitorAvail = new SubTab(monitoringTab.extendLocatorId("Availability"), "Availability", null);
        monitorSched = new SubTab(monitoringTab.extendLocatorId("Schedules"), "Schedules", null);
        monitorCallTime = new SubTab(monitoringTab.extendLocatorId("CallTime"), "Call Time", null);
        monitoringTab.registerSubTabs(monitorGraphs, monitorTables, monitorTraits, monitorAvail, monitorSched,
            monitorCallTime);

        inventoryTab = new TwoLevelTab(topTabSet.extendLocatorId("Inventory"), "Inventory",
            "/images/icons/Inventory_grey_16.png");
        inventoryChildren = new SubTab(inventoryTab.extendLocatorId("Children"), "Children", null);
        inventoryConn = new SubTab(inventoryTab.extendLocatorId("ConnectionSettings"), "Connection Settings", null);
        inventoryTab.registerSubTabs(this.inventoryChildren, this.inventoryConn);

        operationsTab = new TwoLevelTab(topTabSet.extendLocatorId("Operations"), "Operations",
            "/images/icons/Operation_grey_16.png");
        this.opHistory = new SubTab(operationsTab.extendLocatorId("History"), "History", null);
        this.opSched = new SubTab(operationsTab.extendLocatorId("Scheduled"), "Scheduled", null);
        operationsTab.registerSubTabs(this.opHistory, this.opSched);

        alertsTab = new TwoLevelTab(topTabSet.extendLocatorId("Alerts"), "Alerts", "/images/icons/Alert_grey_16.png");
        this.alertHistory = new SubTab(alertsTab.extendLocatorId("History"), "History", null);
        this.alertDef = new SubTab(alertsTab.extendLocatorId("Definitions"), "Definitions", null);
        this.alertDelete = new SubTab(alertsTab.extendLocatorId("DELETEME"), "DELETEME", null);
        alertsTab.registerSubTabs(alertHistory, alertDef, alertDelete);

        configurationTab = new TwoLevelTab(topTabSet.extendLocatorId("Configuration"), "Configuration",
            "/images/icons/Configure_grey_16.png");
        this.configCurrent = new SubTab(configurationTab.extendLocatorId("Current"), "Current", null);
        this.configHistory = new SubTab(configurationTab.extendLocatorId("History"), "History", null);
        configurationTab.registerSubTabs(this.configCurrent, this.configHistory);

        eventsTab = new TwoLevelTab(topTabSet.extendLocatorId("Events"), "Events", "/images/icons/Events_grey_16.png");
        this.eventHistory = new SubTab(eventsTab.extendLocatorId("History"), "History", null);
        eventsTab.registerSubTabs(eventHistory);

        contentTab = new TwoLevelTab(topTabSet.extendLocatorId("Content"), "Content",
            "/images/icons/Content_grey_16.png");
        this.contentDeployed = new SubTab(contentTab.extendLocatorId("Deployed"), "Deployed", null);
        this.contentNew = new SubTab(contentTab.extendLocatorId("New"), "New", null);
        this.contentSubscrip = new SubTab(contentTab.extendLocatorId("Subscriptions"), "Subscriptions", null);
        this.contentHistory = new SubTab(contentTab.extendLocatorId("History"), "History", null);
        contentTab.registerSubTabs(contentDeployed, contentNew, contentSubscrip, contentHistory);

        topTabSet.setTabs(summaryTab, monitoringTab, inventoryTab, operationsTab, alertsTab, configurationTab,
            eventsTab, contentTab);

        topTabSet.addTwoLevelTabSelectedHandler(this);

        titleBar = new ResourceTitleBar(getLocatorId());
        addMember(titleBar);

        addMember(topTabSet);

        //        CoreGUI.addBreadCrumb(getPlace());
    }

    public void onResourceSelected(ResourceComposite resourceComposite) {

        this.resourceComposite = resourceComposite;

        final Resource resource = this.resourceComposite.getResource();
        this.titleBar.setResource(resource);

        this.summaryOverview.setCanvas(new OverviewView(this.resourceComposite));
        this.summaryDashboard.setCanvas(new DashboardView(this.resourceComposite));
        this.summaryTimeline.setCanvas(new FullHTMLPane("/rhq/resource/summary/timeline-plain.xhtml?id="
            + resource.getId()));
        summaryTab.updateSubTab(this.summaryOverview);
        summaryTab.updateSubTab(this.summaryDashboard);
        summaryTab.updateSubTab(this.summaryTimeline);

        this.monitorGraphs.setCanvas(new GraphListView(extendLocatorId(resource.getName()), resource));
        this.monitorTables.setCanvas(new FullHTMLPane("/rhq/common/monitor/tables-plain.xhtml?id=" + resource.getId()));
        this.monitorTraits
            .setCanvas(new FullHTMLPane("/rhq/resource/monitor/traits-plain.xhtml?id=" + resource.getId()));
        this.monitorAvail.setCanvas(new FullHTMLPane("/rhq/resource/monitor/availabilityHistory-plain.xhtml?id="
            + resource.getId()));
        this.monitorSched.setCanvas(new SchedulesView(monitoringTab.extendLocatorId("Schedules"), resource.getId()));
        this.monitorCallTime.setCanvas(new FullHTMLPane("/rhq/resource/monitor/response-plain.xhtml?id="
            + resource.getId()));
        monitoringTab.updateSubTab(this.monitorGraphs);
        monitoringTab.updateSubTab(this.monitorTables);
        monitoringTab.updateSubTab(this.monitorTraits);
        monitoringTab.updateSubTab(this.monitorAvail);
        monitoringTab.updateSubTab(this.monitorSched);
        monitoringTab.updateSubTab(this.monitorCallTime);

        this.inventoryChildren.setCanvas(ResourceSearchView.getChildrenOf(resource.getId()));
        this.inventoryConn.setCanvas(new PluginConfigurationEditView(resource));
        inventoryTab.updateSubTab(this.inventoryChildren);
        inventoryTab.updateSubTab(this.inventoryConn);

        // comment out GWT-based operation history until...
        //     1) user can delete history if they possess the appropriate permissions
        //     2) user can see both operation arguments and results in the history details pop-up
        //     3) operation arguments/results become read-only configuration data in the history details pop-up
        //     4) user can navigate to the group operation that spawned this resource operation history, if appropriate 
        //operationsTab.updateSubTab("History", OperationHistoryView.getResourceHistoryView(resource));
        // note: enabled operation execution/schedules from left-nav, if it doesn't already exist
        this.opHistory.setCanvas(new FullHTMLPane("/rhq/resource/operation/resourceOperationHistory-plain.xhtml?id="
            + resource.getId()));
        this.opSched.setCanvas(new FullHTMLPane("/rhq/resource/operation/resourceOperationSchedules-plain.xhtml?id="
            + resource.getId()));
        operationsTab.updateSubTab(this.opHistory);
        operationsTab.updateSubTab(this.opSched);

        // comment out GWT-based alert definitions/history views until...
        //     1) new workflow is implement for alert definition creation, with particular attention to interaction model for alert notifications
        //     2) user can delete/ack/purgeAll alerts if they possess the appropriate permissions
        //     3) user can enable/disable/delete alert definitions if they possess the appropriate permissions
        //     4) user can search alert history by: date alert was fired, alert priority, or alert definition 
        //alertsTab.updateSubTab("History", new ResourceAlertHistoryView(resource.getId()));
        this.alertHistory.setCanvas(new FullHTMLPane("/rhq/resource/alert/listAlertHistory-plain.xhtml?id="
            + resource.getId()));
        this.alertDef.setCanvas(new ResourceAlertDefinitionsView(alertsTab.getLocatorId(), resource));
        this.alertDelete.setCanvas(new FullHTMLPane("/rhq/resource/alert/listAlertDefinitions-plain.xhtml?id="
            + resource.getId()));
        alertsTab.updateSubTab(this.alertHistory);
        alertsTab.updateSubTab(this.alertDef);
        alertsTab.updateSubTab(this.alertDelete);

        this.configCurrent.setCanvas(new ResourceConfigurationEditView(resource));
        this.configHistory.setCanvas(ConfigurationHistoryView.getHistoryOf(configurationTab.getLocatorId(), resource
            .getId()));
        configurationTab.updateSubTab(this.configCurrent);
        configurationTab.updateSubTab(this.configHistory);

        // comment out GWT-based view until...
        //     1) user can search event history by: metric display range, event source, event details, event severity
        //     2) user can delete events if they possess the appropriate permissions
        //eventsTab.updateSubTab("History", EventHistoryView.createResourceHistoryView(resource.getId()));
        this.eventHistory
            .setCanvas(new FullHTMLPane("/rhq/resource/events/history-plain.xhtml?id=" + resource.getId()));
        eventsTab.updateSubTab(this.eventHistory);

        this.contentDeployed
            .setCanvas(new FullHTMLPane("/rhq/resource/content/view-plain.xhtml?id=" + resource.getId()));
        this.contentNew.setCanvas(new FullHTMLPane("/rhq/resource/content/deploy-plain.xhtml?id=" + resource.getId()));
        this.contentSubscrip.setCanvas(new FullHTMLPane("/rhq/resource/content/subscription-plain.xhtml?id="
            + resource.getId()));
        this.contentHistory.setCanvas(new FullHTMLPane("/rhq/resource/content/history-plain.xhtml?id="
            + resource.getId()));
        contentTab.updateSubTab(contentDeployed);
        contentTab.updateSubTab(contentNew);
        contentTab.updateSubTab(contentSubscrip);
        contentTab.updateSubTab(contentHistory);

        //        topTabSet.setSelectedTab(selectedTab);

        completeTabUpdate();
    }

    private void completeTabUpdate() {

        ResourcePermission permissions = this.resourceComposite.getResourcePermission();
        Set<ResourceTypeFacet> facets = this.resourceComposite.getResourceFacets().getFacets();

        // Summary, Monitoring, Inventory, and Alerts tabs are always enabled.

        monitoringTab.setSubTabEnabled("Call Time", facets.contains(ResourceTypeFacet.CALL_TIME));

        inventoryTab.setSubTabEnabled("Connection Settings", facets.contains(ResourceTypeFacet.PLUGIN_CONFIGURATION));

        if (facets.contains(ResourceTypeFacet.OPERATION)) {
            topTabSet.enableTab(operationsTab);
        } else {
            topTabSet.disableTab(operationsTab);
        }

        if (facets.contains(ResourceTypeFacet.CONFIGURATION) && permissions.isConfigureRead()) {
            topTabSet.enableTab(configurationTab);
        } else {
            topTabSet.disableTab(configurationTab);
        }

        if (facets.contains(ResourceTypeFacet.EVENT)) {
            topTabSet.enableTab(eventsTab);
        } else {
            topTabSet.disableTab(eventsTab);
        }

        if (facets.contains(ResourceTypeFacet.CONTENT)) {
            topTabSet.enableTab(contentTab);
        } else {
            topTabSet.disableTab(contentTab);
        }

        if (topTabSet.getSelectedTab().getDisabled()) {
            topTabSet.selectTab(0);
        }

        topTabSet.markForRedraw();
    }

    public void onTabSelected(TwoLevelTabSelectedEvent tabSelectedEvent) {
        if (this.resourceComposite == null) {
            History.fireCurrentHistoryState();
        } else {
            // Switch tabs directly, rather than letting the history framework do it, to avoid redrawing the outer views.
            selectTab(tabSelectedEvent.getId(), tabSelectedEvent.getSubTabId());
            String tabPath = "/" + tabSelectedEvent.getId() + "/" + tabSelectedEvent.getSubTabId();
            String path = "Resource/" + this.resourceComposite.getResource().getId() + tabPath;

            // But still add an item to the history, specifying false to tell it not to fire an event.
            History.newItem(path, true);
        }
    }

    public void renderView(ViewPath viewPath) {
        // e.g. #Resource/10010/Inventory/Overview
        String tabName = (!viewPath.isEnd()) ? viewPath.getCurrent().getPath() : null; // e.g. "Inventory"
        String subTabName = (viewPath.viewsLeft() >= 1) ? viewPath.getNext().getPath() : null; // e.g. "Overview"
        selectTab(tabName, subTabName);
    }

    public void selectTab(String tabName, String subtabName) {
        if (tabName == null) {
            tabName = DEFAULT_TAB_NAME;
        }
        TwoLevelTab tab = (TwoLevelTab) this.topTabSet.getTabByTitle(tabName);
        if (tab == null) {
            CoreGUI.getErrorHandler().handleError("Invalid tab name: " + tabName);
            // TODO: Should we fire a history event here to redirect to a valid bookmark?
            tab = (TwoLevelTab) this.topTabSet.getTabByTitle(DEFAULT_TAB_NAME);
        }
        this.topTabSet.selectTab(tab);
        if (subtabName != null) {
            if (!tab.getLayout().selectTab(subtabName)) {
                CoreGUI.getErrorHandler().handleError("Invalid subtab name: " + subtabName);
                // TODO: Should we fire a history event here to redirect to a valid bookmark?
                return;
            }
            tab.getLayout().selectTab(subtabName);
        }
    }

}
