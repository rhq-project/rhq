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

import java.util.EnumSet;
import java.util.Set;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Side;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.tab.Tab;

import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.ResourceTypeFacet;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.resource.composite.ResourcePermission;
import org.rhq.core.domain.util.PageList;
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
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.common.event.EventCompositeHistoryView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.InventoryView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceSearchView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.configuration.ConfigurationHistoryView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.configuration.ResourceConfigurationEditView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.inventory.PluginConfigurationEditView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.GraphListView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.schedules.SchedulesView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.traits.TraitsView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.operation.OperationHistoryView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.summary.DashboardView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.summary.OverviewView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * Right panel of the Resource view.
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class ResourceDetailView extends LocatableVLayout implements BookmarkableView, TwoLevelTabSelectedHandler {
    private int resourceId;

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

    private String tabName;
    private String subTabName;

    public ResourceDetailView(String locatorId) {
        super(locatorId);

        setWidth100();
        setHeight100();

        // The Tabs section

        topTabSet = new TwoLevelTabSet(extendLocatorId("TabSet"));
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

        titleBar = new ResourceTitleBar(extendLocatorId("TitleBar"));

        addMember(titleBar);
        addMember(topTabSet);

        //        CoreGUI.addBreadCrumb(getPlace());        
    }

    public void updateDetailViews(ResourceComposite resourceComposite) {

        this.resourceComposite = resourceComposite;

        final Resource resource = this.resourceComposite.getResource();
        this.titleBar.setResource(resource);

        for (Tab top : this.topTabSet.getTabs()) {
            ((TwoLevelTab) top).getLayout().destroyViews();
        }

        this.summaryOverview.setCanvas(new OverviewView(this.resourceComposite));
        this.summaryDashboard.setCanvas(new DashboardView(this.resourceComposite));
        this.summaryTimeline.setCanvas(new FullHTMLPane("/rhq/resource/summary/timeline-plain.xhtml?id="
            + resource.getId()));

        this.monitorGraphs.setCanvas(new GraphListView(this.monitoringTab.extendLocatorId("GraphListView"), resource));
        this.monitorTables.setCanvas(new FullHTMLPane("/rhq/common/monitor/tables-plain.xhtml?id=" + resource.getId()));
        this.monitorTraits.setCanvas(new TraitsView(this.monitoringTab.extendLocatorId("TraitsView"), resource.getId()));
        this.monitorAvail.setCanvas(new FullHTMLPane("/rhq/resource/monitor/availabilityHistory-plain.xhtml?id="
            + resource.getId()));
        this.monitorSched.setCanvas(new SchedulesView(monitoringTab.extendLocatorId("SchedulesView"), resource.getId()));
        this.monitorCallTime.setCanvas(new FullHTMLPane("/rhq/resource/monitor/response-plain.xhtml?id="
            + resource.getId()));

        this.inventoryChildren.setCanvas(ResourceSearchView.getChildrenOf(resource.getId()));
        this.inventoryConn.setCanvas(new PluginConfigurationEditView(this.extendLocatorId("PluginConfig"), resource));

        // comment out GWT-based operation history until...
        //     1) user can delete history if they possess the appropriate permissions
        //     2) user can see both operation arguments and results in the history details pop-up
        //     3) operation arguments/results become read-only configuration data in the history details pop-up
        //     4) user can navigate to the group operation that spawned this resource operation history, if appropriate
        // note: enabled operation execution/schedules from left-nav, if it doesn't already exist
        this.opHistory.setCanvas(OperationHistoryView.getResourceHistoryView(operationsTab.extendLocatorId("History"),
            resourceComposite));
        this.opSched.setCanvas(new FullHTMLPane("/rhq/resource/operation/resourceOperationSchedules-plain.xhtml?id="
            + resource.getId()));

        // comment out GWT-based alert definitions/history views until...
        //     1) new workflow is implement for alert definition creation, with particular attention to interaction model for alert notifications
        //     2) user can delete/ack/purgeAll alerts if they possess the appropriate permissions
        //     3) user can enable/disable/delete alert definitions if they possess the appropriate permissions
        //     4) user can search alert history by: date alert was fired, alert priority, or alert definition
        this.alertHistory.setCanvas(new FullHTMLPane("/rhq/resource/alert/listAlertHistory-plain.xhtml?id="
            + resource.getId()));
        this.alertDef.setCanvas(new ResourceAlertDefinitionsView(alertsTab.extendLocatorId("Def"), resource));
        this.alertDelete.setCanvas(new FullHTMLPane("/rhq/resource/alert/listAlertDefinitions-plain.xhtml?id="
            + resource.getId()));

        this.configCurrent
            .setCanvas(new ResourceConfigurationEditView(this.extendLocatorId("ResourceConfig"), resource));
        this.configHistory.setCanvas(ConfigurationHistoryView.getHistoryOf(configurationTab.extendLocatorId("Hist"),
            resource.getId()));

        this.eventHistory.setCanvas(EventCompositeHistoryView.get(resourceComposite));

        this.contentDeployed
            .setCanvas(new FullHTMLPane("/rhq/resource/content/view-plain.xhtml?id=" + resource.getId()));
        this.contentNew.setCanvas(new FullHTMLPane("/rhq/resource/content/deploy-plain.xhtml?id=" + resource.getId()));
        this.contentSubscrip.setCanvas(new FullHTMLPane("/rhq/resource/content/subscription-plain.xhtml?id="
            + resource.getId()));
        this.contentHistory.setCanvas(new FullHTMLPane("/rhq/resource/content/history-plain.xhtml?id="
            + resource.getId()));

        updateTabEnablement();
    }

    private void updateTabEnablement() {

        ResourcePermission permissions = this.resourceComposite.getResourcePermission();
        Set<ResourceTypeFacet> facets = this.resourceComposite.getResourceFacets().getFacets();

        // Summary, Monitoring, Inventory, and Alerts tabs are always enabled.

        monitoringTab.setSubTabEnabled(monitorTraits.getLocatorId(), hasTraits(this.resourceComposite));
        monitoringTab.setSubTabEnabled(monitorCallTime.getLocatorId(), facets.contains(ResourceTypeFacet.CALL_TIME));

        inventoryTab.setSubTabEnabled(inventoryConn.getLocatorId(), facets.contains(ResourceTypeFacet.PLUGIN_CONFIGURATION));
        ResourceType type = this.resourceComposite.getResource().getResourceType();
        inventoryTab.setSubTabEnabled(inventoryChildren.getLocatorId(), !type.getChildResourceTypes().isEmpty());

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
    }

    public void onTabSelected(TwoLevelTabSelectedEvent tabSelectedEvent) {
        //CoreGUI.printWidgetTree();

        if (this.resourceComposite == null) {
            //            History.fireCurrentHistoryState();
        } else {
            // Switch tabs directly, rather than letting the history framework do it, to avoid redrawing the outer views.
            //            selectTab(tabSelectedEvent.getId(), tabSelectedEvent.getSubTabId());
            String tabPath = "/" + tabSelectedEvent.getId() + "/" + tabSelectedEvent.getSubTabId();
            String path = "Resource/" + resourceId + tabPath;

            // If the tab that was selected is not already the current history item, the user clicked on the tab, rather
            // than going directly to the tab's URL. In this case, fire a history event to go to the tab and make it the
            // current history item.
            if (!History.getToken().equals(path)) {
                System.out.println("Firing History event [" + path + "]...");
                History.newItem(path, true);
            }
        }
    }

    public void renderView(ViewPath viewPath) {
        // e.g. #Resource/10010/Inventory/Overview
        //                ^ Current Path
        int resourceId = Integer.parseInt(viewPath.getCurrent().getPath());

        viewPath.next();

        tabName = (!viewPath.isEnd()) ? viewPath.getCurrent().getPath() : null; // e.g. "Inventory"
        subTabName = (viewPath.viewsLeft() >= 1) ? viewPath.getNext().getPath() : null; // e.g. "Overview"

        viewPath.next();
        viewPath.next();

        if (this.resourceId != resourceId) {
            // A different resource or first load, go get data
            loadSelectedResource(resourceId, viewPath);
        } else {
            // same resource just switch tabs
            selectTab(tabName, subTabName, viewPath);
        }

    }

    public void loadSelectedResource(final int resourceId, final ViewPath viewPath) {
        this.resourceId = resourceId;

        ResourceCriteria criteria = new ResourceCriteria();
        criteria.addFilterId(resourceId);
        criteria.fetchTags(true);
        //criteria.fetchParentResource(true);
        GWTServiceLookup.getResourceService().findResourceCompositesByCriteria(criteria,
            new AsyncCallback<PageList<ResourceComposite>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getMessageCenter().notify(
                        new Message("Resource with id [" + resourceId + "] does not exist or is not accessible.",
                            Message.Severity.Warning));

                    CoreGUI.goTo(InventoryView.VIEW_PATH);
                }

                public void onSuccess(PageList<ResourceComposite> result) {
                    if (result.isEmpty()) {
                        //noinspection ThrowableInstanceNeverThrown
                        onFailure(new Exception("Resource with id [" + resourceId + "] does not exist."));
                    } else {
                        final ResourceComposite resourceComposite = result.get(0);
                        loadResourceType(resourceComposite, viewPath);
                    }
                }
            });
    }

    private void loadResourceType(final ResourceComposite resourceComposite, final ViewPath viewPath) {
        final Resource resource = resourceComposite.getResource();
        ResourceTypeRepository.Cache.getInstance().getResourceTypes(
            resource.getResourceType().getId(),
            EnumSet.of(
                ResourceTypeRepository.MetadataType.children,
                ResourceTypeRepository.MetadataType.content,
                ResourceTypeRepository.MetadataType.operations,
                ResourceTypeRepository.MetadataType.measurements,
                ResourceTypeRepository.MetadataType.events,
                ResourceTypeRepository.MetadataType.resourceConfigurationDefinition),
            new ResourceTypeRepository.TypeLoadedCallback() {
                public void onTypesLoaded(ResourceType type) {
                    resourceComposite.getResource().setResourceType(type);
                    ResourceDetailView.this.resourceComposite = resourceComposite;
                    updateDetailViews(resourceComposite);
                    selectTab(tabName, subTabName, viewPath);
                }
            });
    }

    public void selectTab(String tabName, String subtabName, ViewPath viewPath) {
        try {
            TwoLevelTab tab = this.topTabSet.getTabByTitle(tabName);
            if (tab == null || tab.getDisabled()) {
                CoreGUI.getErrorHandler().handleError("Invalid tab name: " + tabName);
                // TODO: Should we fire a history event here to redirect to a valid bookmark?
                Tab defaultTab = this.topTabSet.getTab(0);
                tab = (TwoLevelTab) defaultTab;
            }
            SubTab subTab = null;
            if (subtabName != null) {
                if (!tab.getLayout().selectTab(subtabName)) {
                    CoreGUI.getErrorHandler().handleError("Invalid subtab name: " + subtabName);
                    // TODO: Should we fire a history event here to redirect to a valid bookmark?
                    SubTab defaultSubtab = tab.getLayout().getDefaultSubTab();
                    tab.getLayout().selectTab(defaultSubtab.getTitle());
                }
                subTab = tab.getLayout().getCurrentSubTab();
            }
            this.topTabSet.selectTab(tab);
            if (subTab != null) {
                tab.updateSubTab(subTab);
                Canvas subView = subTab.getCanvas();
                if (subView instanceof BookmarkableView) {
                    ((BookmarkableView) subView).renderView(viewPath);
                }
            }
            tab.getLayout().markForRedraw();
            this.topTabSet.markForRedraw();
        } catch (Exception e) {
            System.err.println("Failed to select tab " + tabName + "/" + subtabName + ": " + e);
        }
    }

    private static boolean hasTraits(ResourceComposite resourceComposite) {
        ResourceType type = resourceComposite.getResource().getResourceType();
        Set<MeasurementDefinition> metricDefs = type.getMetricDefinitions();
        for (MeasurementDefinition metricDef : metricDefs) {
            if (metricDef.getDataType() == DataType.TRAIT) {
                return true;
            }
        }
        return false;
    }
}
