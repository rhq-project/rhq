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
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourcePermission;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.FullHTMLPane;
import org.rhq.enterprise.gui.coregui.client.components.tab.TwoLevelTab;
import org.rhq.enterprise.gui.coregui.client.components.tab.TwoLevelTabSelectedEvent;
import org.rhq.enterprise.gui.coregui.client.components.tab.TwoLevelTabSelectedHandler;
import org.rhq.enterprise.gui.coregui.client.components.tab.TwoLevelTabSet;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceSearchView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceSelectListener;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.configuration.ConfigurationHistoryView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.configuration.ResourceConfigurationEditView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.inventory.PluginConfigurationEditView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.GraphListView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.overview.ResourceOverviewView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;

/**
 * Right panel of the resource view.
 *
 * @author Greg Hinkle
 */
public class ResourceDetailView extends VLayout implements BookmarkableView, ResourceSelectListener,
    TwoLevelTabSelectedHandler {

    private static final String DEFAULT_TAB_NAME = "Summary";

    private Resource resource;
    private ResourcePermission permissions;
    private ResourceType type;

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

    public void setResource(Resource resource) {
        this.resource = resource;
    }

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
        alertsTab.registerSubTabs("History", "Definitions");

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

    public void onResourceSelected(Resource resource) {

        this.resource = resource;

        titleBar.setResource(resource);

        summaryTab.updateSubTab("Overview", new ResourceOverviewView(resource));
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

        operationsTab.updateSubTab("History", OperationHistoryView.getResourceHistoryView(resource));

        configurationTab.updateSubTab("Current", new ResourceConfigurationEditView(resource));
        configurationTab.updateSubTab("History", ConfigurationHistoryView.getHistoryOf(resource.getId()));

        alertsTab.updateSubTab("History", new ResourceAlertHistoryView(resource.getId()));
        alertsTab.updateSubTab("Definitions", AlertDefinitionsView.getResourceView(resource));


        contentTab.updateSubTab("Deployed", new FullHTMLPane("/rhq/resource/content/view-plain.xhtml?id=" + resource.getId()));
        contentTab.updateSubTab("New", new FullHTMLPane("/rhq/resource/content/deploy-plain.xhtml?id=" + resource.getId()));
        contentTab.updateSubTab("Subscriptions", new FullHTMLPane("/rhq/resource/content/subscription-plain.xhtml?id=" + resource.getId()));
        contentTab.updateSubTab("History", new FullHTMLPane("/rhq/resource/content/history-plain.xhtml?id=" + resource.getId()));
        // comment out GWT-based view until...
        //     1) user can search event history by: metric display range, event source, event details, event severity
        //     2) user can delete events if they possess the appropriate permissions
        //eventsTab.updateSubTab("History", EventHistoryView.createResourceHistoryView(resource.getId()));
        eventsTab.updateSubTab("History", new FullHTMLPane("/rhq/resource/events/history-plain.xhtml?id="
            + resource.getId()));


        //        topTabSet.setSelectedTab(selectedTab);

        updateTabStatus();

        topTabSet.markForRedraw();
    }

    private void updateTabStatus() {
        // Go and get the type with all needed metadata
        // and then get the permissions for this resource

        ResourceTypeRepository.Cache.getInstance().getResourceTypes(
            resource.getResourceType().getId(),
            EnumSet.of(ResourceTypeRepository.MetadataType.content, ResourceTypeRepository.MetadataType.operations,
                ResourceTypeRepository.MetadataType.events,
                ResourceTypeRepository.MetadataType.resourceConfigurationDefinition),
            new ResourceTypeRepository.TypeLoadedCallback() {
                public void onTypesLoaded(ResourceType type) {

                    ResourceDetailView.this.type = type;

                    GWTServiceLookup.getAuthorizationService().getImplicitResourcePermissions(
                        ResourceDetailView.this.resource.getId(), new AsyncCallback<Set<Permission>>() {
                            public void onFailure(Throwable caught) {
                                CoreGUI.getErrorHandler().handleError("Failed to load resource permissions", caught);
                            }

                            public void onSuccess(Set<Permission> result) {
                                ResourceDetailView.this.permissions = new ResourcePermission(result);
                                completeTabUpdate();
                            }
                        });
                }
            });
    }

    private void completeTabUpdate() {

        if (!permissions.isMeasure()) {
            topTabSet.disableTab(monitoringTab);
        } else {
            topTabSet.enableTab(monitoringTab);
        }

        if (type.getOperationDefinitions() == null || type.getOperationDefinitions().isEmpty()
            || !permissions.isControl()) {
            topTabSet.disableTab(operationsTab);
        } else {
            topTabSet.enableTab(operationsTab);
        }

        if (!permissions.isAlert()) {
            topTabSet.disableTab(alertsTab);
        } else {
            topTabSet.enableTab(alertsTab);
        }

        if (type.getResourceConfigurationDefinition() == null || !permissions.isConfigureRead()) {
            topTabSet.disableTab(configurationTab);
        } else {
            topTabSet.enableTab(configurationTab);
        }

        if (type.getEventDefinitions() == null || type.getEventDefinitions().isEmpty() || !permissions.isMeasure()) {
            topTabSet.enableTab(eventsTab);
        } else {
            topTabSet.enableTab(eventsTab);
        }

        if (type.getPackageTypes() == null || type.getPackageTypes().isEmpty() || !permissions.isContent()) {
            topTabSet.disableTab(contentTab);
        } else {
            topTabSet.enableTab(contentTab);
        }

        if (topTabSet.getSelectedTab().getDisabled()) {
            topTabSet.selectTab(0);
        }

    }

    public void onTabSelected(TwoLevelTabSelectedEvent tabSelectedEvent) {
        // Switch tabs directly, rather than letting the history framework do it, to avoid redrawing the outer views.
        selectTab(tabSelectedEvent.getId(), tabSelectedEvent.getSubTabId());
        String tabPath = "/" + tabSelectedEvent.getId() + "/" + tabSelectedEvent.getSubTabId();
        String path = "Resource/" + this.resource.getId() + tabPath;

        // But still add an item to the history, specifying false to tell it not to fire an event.
        History.newItem(path, false);
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
