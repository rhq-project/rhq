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

import com.google.gwt.user.client.History;
import com.smartgwt.client.types.Side;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceTypeFacet;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.resource.composite.ResourcePermission;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.alert.definitions.ResourceAlertDefinitionsView;
import org.rhq.enterprise.gui.coregui.client.components.FullHTMLPane;
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
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.overview.ResourceOverviewView;

import java.util.Set;

/**
 * Right panel of the Resource view.
 *
 * @author Greg Hinkle
 */
public class ResourceDetailView extends VLayout implements BookmarkableView, ResourceSelectListener,
    TwoLevelTabSelectedHandler {

    private static final String DEFAULT_TAB_NAME = "Summary";

    private ResourceComposite resourceComposite;

    private TwoLevelTab summaryTab;
    private TwoLevelTab monitoringTab;
    private TwoLevelTab inventoryTab;
    private TwoLevelTab operationsTab;
    private TwoLevelTab alertsTab;
    private TwoLevelTab configurationTab;
    private TwoLevelTab eventsTab;
    private TwoLevelTab contentTab;

    private TwoLevelTabSet topTabSet;

    private ResourceTitleBar titleBar;


    @Override
    protected void onDraw() {
        super.onDraw();

        setWidth100();
        setHeight100();

        // The Tabs section

        topTabSet = new TwoLevelTabSet();
        topTabSet.setTabBarPosition(Side.TOP);
        topTabSet.setWidth100();
        topTabSet.setHeight100();
        topTabSet.setEdgeMarginSize(0);
        topTabSet.setEdgeSize(0);

        summaryTab = new TwoLevelTab("Summary", "/images/icons/Service_up_16.png");
        summaryTab.registerSubTabs("Overview", "Timeline");

        monitoringTab = new TwoLevelTab("Monitoring", "/images/icons/Monitor_grey_16.png");
        monitoringTab.registerSubTabs("Graphs", "Tables", "Traits", "Availability", "Schedules", "Call Time");

        inventoryTab = new TwoLevelTab("Inventory", "/images/icons/Inventory_grey_16.png");
        inventoryTab.registerSubTabs("Children", "Connection Settings");

        operationsTab = new TwoLevelTab("Operations", "/images/icons/Operation_grey_16.png");
        operationsTab.registerSubTabs("History", "Scheduled");

        alertsTab = new TwoLevelTab("Alerts", "/images/icons/Alert_grey_16.png");
        alertsTab.registerSubTabs("History", "Definitions", "DELETEME");

        configurationTab = new TwoLevelTab("Configuration", "/images/icons/Configure_grey_16.png");
        configurationTab.registerSubTabs("Current", "History");

        eventsTab = new TwoLevelTab("Events", "/images/icons/Events_grey_16.png");
        eventsTab.registerSubTabs("History");

        contentTab = new TwoLevelTab("Content", "/images/icons/Content_grey_16.png");
        contentTab.registerSubTabs("Deployed", "New", "Subscriptions", "History");

        topTabSet.setTabs(summaryTab, monitoringTab, inventoryTab, operationsTab, alertsTab, configurationTab,
            eventsTab, contentTab);

        topTabSet.addTwoLevelTabSelectedHandler(this);

        titleBar = new ResourceTitleBar();
        addMember(titleBar);

        addMember(topTabSet);

        //        CoreGUI.addBreadCrumb(getPlace());
    }

    public void onResourceSelected(ResourceComposite resourceComposite) {

        this.resourceComposite = resourceComposite;

        final Resource resource = this.resourceComposite.getResource();
        this.titleBar.setResource(resource);

        summaryTab.updateSubTab("Overview", new ResourceOverviewView(this.resourceComposite));
        summaryTab.updateSubTab("Timeline", new FullHTMLPane("/rhq/resource/summary/timeline-plain.xhtml?id="
            + resource.getId()));

        monitoringTab.updateSubTab("Graphs", new GraphListView(resource)); // new FullHTMLPane("/rhq/common/monitor/graphs.xhtml?id=" + resource.getId()));
        monitoringTab.updateSubTab("Tables", new FullHTMLPane("/rhq/common/monitor/tables-plain.xhtml?id="
            + resource.getId()));
        monitoringTab.updateSubTab("Traits", new FullHTMLPane("/rhq/resource/monitor/traits-plain.xhtml?id="
            + resource.getId()));
        monitoringTab.updateSubTab("Availability", new FullHTMLPane(
            "/rhq/resource/monitor/availabilityHistory-plain.xhtml?id=" + resource.getId()));
        monitoringTab.updateSubTab("Schedules", new FullHTMLPane("/rhq/resource/monitor/schedules-plain.xhtml?id="
            + resource.getId()));
        monitoringTab.updateSubTab("Call Time", new FullHTMLPane("/rhq/resource/monitor/response-plain.xhtml?id="
            + resource.getId()));

        inventoryTab.updateSubTab("Children", ResourceSearchView.getChildrenOf(resource.getId()));
        inventoryTab.updateSubTab("Connection Settings", new PluginConfigurationEditView(resource)); // new ConfigurationEditor(resource.getId(), resource.getResourceType().getId(), ConfigurationEditor.ConfigType.plugin));

        // comment out GWT-based operation history until...
        //     1) user can delete history if they possess the appropriate permissions
        //     2) user can see both operation arguments and results in the history details pop-up
        //     3) operation arguments/results become read-only configuration data in the history details pop-up
        //     4) user can navigate to the group operation that spawned this resource operation history, if appropriate 
        //operationsTab.updateSubTab("History", OperationHistoryView.getResourceHistoryView(resource));
        // note: enabled operation execution/schedules from left-nav, if it doesn't already exist
        operationsTab.updateSubTab("History", new FullHTMLPane(
            "/rhq/resource/operation/resourceOperationHistory-plain.xhtml?id=" + resource.getId()));
        operationsTab.updateSubTab("Scheduled", new FullHTMLPane(
            "/rhq/resource/operation/resourceOperationSchedules-plain.xhtml?id=" + resource.getId()));

        configurationTab.updateSubTab("Current", new ResourceConfigurationEditView(resource));
        configurationTab.updateSubTab("History", ConfigurationHistoryView.getHistoryOf(resource.getId()));

        // comment out GWT-based alert definitions/history views until...
        //     1) new workflow is implement for alert definition creation, with particular attention to interaction model for alert notifications
        //     2) user can delete/ack/purgeAll alerts if they possess the appropriate permissions
        //     3) user can enable/disable/delete alert definitions if they possess the appropriate permissions
        //     4) user can search alert history by: date alert was fired, alert priority, or alert definition 
        //alertsTab.updateSubTab("History", new ResourceAlertHistoryView(resource.getId()));
        alertsTab.updateSubTab("History", new FullHTMLPane("/rhq/resource/alert/listAlertHistory-plain.xhtml?id="
            + resource.getId()));
        alertsTab.updateSubTab("Definitions", new ResourceAlertDefinitionsView(resource));
        alertsTab.updateSubTab("DELETEME", new FullHTMLPane("/rhq/resource/alert/listAlertDefinitions-plain.xhtml?id="
            + resource.getId()));

        // comment out GWT-based view until...
        //     1) user can search event history by: metric display range, event source, event details, event severity
        //     2) user can delete events if they possess the appropriate permissions
        //eventsTab.updateSubTab("History", EventHistoryView.createResourceHistoryView(resource.getId()));
        eventsTab.updateSubTab("History", new FullHTMLPane("/rhq/resource/events/history-plain.xhtml?id="
            + resource.getId()));

        contentTab.updateSubTab("Deployed", new FullHTMLPane("/rhq/resource/content/view-plain.xhtml?id="
            + resource.getId()));
        contentTab.updateSubTab("New", new FullHTMLPane("/rhq/resource/content/deploy-plain.xhtml?id="
            + resource.getId()));
        contentTab.updateSubTab("Subscriptions", new FullHTMLPane("/rhq/resource/content/subscription-plain.xhtml?id="
            + resource.getId()));
        contentTab.updateSubTab("History", new FullHTMLPane("/rhq/resource/content/history-plain.xhtml?id="
            + resource.getId()));

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

        // TODO: This doesn't seem to actually be calling redraw(), draw(), or onDraw() on topTabSet, so subtab
        //       enablement isn't getting updated...
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
            History.newItem(path, false);
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
