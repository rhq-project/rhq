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
package org.rhq.enterprise.gui.coregui.client.inventory.groups.detail;

import java.util.EnumSet;
import java.util.Set;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Side;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.ResourceTypeFacet;
import org.rhq.core.domain.resource.composite.ResourcePermission;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.FullHTMLPane;
import org.rhq.enterprise.gui.coregui.client.components.tab.TwoLevelTab;
import org.rhq.enterprise.gui.coregui.client.components.tab.TwoLevelTabSelectedEvent;
import org.rhq.enterprise.gui.coregui.client.components.tab.TwoLevelTabSelectedHandler;
import org.rhq.enterprise.gui.coregui.client.components.tab.TwoLevelTabSet;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.schedules.SchedulesView;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.traits.TraitsView;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.summary.OverviewView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceSearchView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;

/**
 * Right panel of the group view.
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class ResourceGroupDetailView extends VLayout implements BookmarkableView, TwoLevelTabSelectedHandler {
    private static final String DEFAULT_TAB_NAME = "Inventory";

    private ResourceGroupComposite groupComposite;
    private ResourcePermission permissions;

    private TwoLevelTab summaryTab;
    private TwoLevelTab monitoringTab;
    private TwoLevelTab inventoryTab;
    private TwoLevelTab operationsTab;
    private TwoLevelTab alertsTab;
    private TwoLevelTab configurationTab;
    private TwoLevelTab eventsTab;

    private TwoLevelTabSet topTabSet;

    private ResourceGroupTitleBar titleBar;

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
        monitoringTab.registerSubTabs("Graphs", "Tables", "Traits", "Schedules", "Call Time");

        inventoryTab = new TwoLevelTab("Inventory", "/images/icons/Inventory_grey_16.png");
        inventoryTab.registerSubTabs("Members", "Connection Settings");

        operationsTab = new TwoLevelTab("Operations", "/images/icons/Operation_grey_16.png");
        operationsTab.registerSubTabs("History", "Scheduled");

        alertsTab = new TwoLevelTab("Alerts", "/images/icons/Alert_grey_16.png");
        alertsTab.registerSubTabs("History", "Definitions");

        configurationTab = new TwoLevelTab("Configuration", "/images/icons/Configure_grey_16.png");
        configurationTab.registerSubTabs("Current", "History");

        eventsTab = new TwoLevelTab("Events", "/images/icons/Events_grey_16.png");
        eventsTab.registerSubTabs("History");

        topTabSet.setTabs(summaryTab, monitoringTab, inventoryTab, operationsTab, alertsTab, configurationTab,
            eventsTab);

        topTabSet.addTwoLevelTabSelectedHandler(this);

        titleBar = new ResourceGroupTitleBar();
        addMember(titleBar);

        addMember(topTabSet);

        //        CoreGUI.addBreadCrumb(getPlace());
    }

    public void onGroupSelected(ResourceGroupComposite groupComposite) {

        this.groupComposite = groupComposite;

        this.titleBar.setGroup(groupComposite.getResourceGroup());

        //        FullHTMLPane timelinePane = new FullHTMLPane("/rhq/resource/summary/timeline-plain.xhtml?id=" + resource.getId());
        //        summaryTab.updateSubTab("Overview", new DashboardView(resource));
        //        summaryTab.updateSubTab("Timeline", timelinePane);
        summaryTab.updateSubTab("Overview", new OverviewView(this.groupComposite));

        int groupId = this.groupComposite.getResourceGroup().getId();

        monitoringTab.updateSubTab("Graphs", new FullHTMLPane("/rhq/group/monitor/graphs-plain.xhtml?groupId="
            + groupId));
        monitoringTab.updateSubTab("Tables", new FullHTMLPane("/rhq/group/monitor/tables-plain.xhtml?groupId="
            + groupId));
        monitoringTab.updateSubTab("Traits", new TraitsView(groupId));
        monitoringTab.updateSubTab("Schedules", new SchedulesView(groupId));
                
        //new FullHTMLPane("/rhq/group/monitor/schedules-plain.xhtml?groupId=" + groupId));
        monitoringTab.updateSubTab("Call Time", new FullHTMLPane("/rhq/group/monitor/response-plain.xhtml?groupId="
            + groupId));

        inventoryTab.updateSubTab("Members", ResourceSearchView.getMembersOf(groupId));
        //        inventoryTab.updateSubTab("Connection Settings", new GroupPluginConfigurationEditView(this.group.getId(), this.group.getResourceType().getId(), ConfigurationEditor.ConfigType.plugin));

        operationsTab.updateSubTab("History", new FullHTMLPane(
            "/rhq/group/operation/groupOperationHistory-plain.xhtml?groupId=" + groupId));
        operationsTab.updateSubTab("Scheduled", new FullHTMLPane(
            "/rhq/group/operation/groupOperationSchedules-plain.xhtml?groupId=" + groupId));

        configurationTab.updateSubTab("Current", new FullHTMLPane(
            "/rhq/group/configuration/viewCurrent-plain.xhtml?groupId=" + groupId));
        configurationTab.updateSubTab("History", new FullHTMLPane(
            "/rhq/group/configuration/history-plain.xhtml?groupId=" + groupId));

        alertsTab.updateSubTab("History", new FullHTMLPane(
            "/rhq/group/alert/listGroupAlertHistory-plain.xhtml?groupId=" + groupId));
        alertsTab.updateSubTab("Definitions", new FullHTMLPane(
            "/rhq/group/alert/listGroupAlertDefinitions-plain.xhtml?groupId=" + groupId));

        eventsTab.updateSubTab("History", new FullHTMLPane("/rhq/group/events/history-plain.xhtml?groupId=" + groupId));

        //        topTabSet.setSelectedTab(selectedTab);

        updateTabStatus();

        topTabSet.markForRedraw();
    }

    private void updateTabStatus() {
        final ResourceGroup group = this.groupComposite.getResourceGroup();

        if (group.getGroupCategory() == GroupCategory.COMPATIBLE) {

            // Load the fully fetched ResourceType.
            ResourceType groupType = group.getResourceType();
            ResourceTypeRepository.Cache.getInstance().getResourceTypes(
                groupType.getId(),
                EnumSet.of(ResourceTypeRepository.MetadataType.content, ResourceTypeRepository.MetadataType.operations,
                    ResourceTypeRepository.MetadataType.events,
                    ResourceTypeRepository.MetadataType.resourceConfigurationDefinition),
                new ResourceTypeRepository.TypeLoadedCallback() {
                    public void onTypesLoaded(ResourceType type) {
                        group.setResourceType(type);
                        GWTServiceLookup.getAuthorizationService().getImplicitGroupPermissions(group.getId(),
                            new AsyncCallback<Set<Permission>>() {
                                public void onFailure(Throwable caught) {
                                    CoreGUI.getErrorHandler().handleError("Failed to load group permissions.", caught);
                                }

                                public void onSuccess(Set<Permission> result) {
                                    ResourceGroupDetailView.this.permissions = new ResourcePermission(result);
                                    completeTabUpdate();
                                }
                            });
                    }
                });
        }
    }

    private void completeTabUpdate() {

        GroupCategory groupCategory = groupComposite.getResourceGroup().getGroupCategory();
        Set<ResourceTypeFacet> facets = groupComposite.getResourceFacets().getFacets();

        // Summary and Inventory tabs are always enabled.
        topTabSet.enableTab(summaryTab);
        topTabSet.enableTab(inventoryTab);

        // Inventory>Connection Settings subtab is only enabled for compat groups that define conn props.
        inventoryTab.setSubTabEnabled("Connection Settings",
                groupCategory == GroupCategory.COMPATIBLE && facets.contains(ResourceTypeFacet.PLUGIN_CONFIGURATION));

        // Monitoring and Alerts tabs are always enabled for compatible groups and always disabled for mixed groups.
        if (groupCategory == GroupCategory.COMPATIBLE) {
            topTabSet.enableTab(monitoringTab);
            topTabSet.enableTab(alertsTab);
        } else {
            topTabSet.disableTab(monitoringTab);
            topTabSet.disableTab(alertsTab);
        }

        // Operations tab is only enabled for compatible groups of a type that supports the Operations facet.
        if (facets.contains(ResourceTypeFacet.OPERATION)) {
            topTabSet.enableTab(operationsTab);
        } else {
            topTabSet.disableTab(operationsTab);
        }

        // Configuration tab is only enabled for compatible groups of a type that supports the Configuration facet
        // and when the current user has the CONFIGURE_READ permission.
        if (facets.contains(ResourceTypeFacet.CONFIGURATION) && permissions.isConfigureRead()) {
            topTabSet.enableTab(configurationTab);
        } else {
            topTabSet.disableTab(configurationTab);
        }

        // Events tab is only enabled for compatible groups of a type that supports the Events facet.
        if (facets.contains(ResourceTypeFacet.EVENT)) {
            topTabSet.enableTab(eventsTab);
        } else {
            topTabSet.disableTab(eventsTab);
        }

        // only enable "Call Time" sub-tab for those that implement it
        monitoringTab.setSubTabEnabled("Call Time", facets.contains(ResourceTypeFacet.CALL_TIME));
    }

    public void onTabSelected(TwoLevelTabSelectedEvent tabSelectedEvent) {
        if (this.groupComposite == null) {
            History.fireCurrentHistoryState();
        } else {
            // Switch tabs directly, rather than letting the history framework do it, to avoid redrawing the outer views.
            selectTab(tabSelectedEvent.getId(), tabSelectedEvent.getSubTabId());
            String tabPath = "/" + tabSelectedEvent.getId() + "/" + tabSelectedEvent.getSubTabId();
            String path = "ResourceGroup/" + this.groupComposite.getResourceGroup().getId() + tabPath;

            // But still add an item to the history, specifying false to tell it not to fire an event.
            History.newItem(path, false);
        }
    }

    public void renderView(ViewPath viewPath) {
        // e.g. #ResourceGroup/10010/Inventory/Overview
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

