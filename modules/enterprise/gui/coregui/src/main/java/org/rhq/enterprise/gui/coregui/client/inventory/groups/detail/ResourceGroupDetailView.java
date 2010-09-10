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
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.tab.Tab;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.ResourceTypeFacet;
import org.rhq.core.domain.resource.composite.ResourcePermission;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.FullHTMLPane;
import org.rhq.enterprise.gui.coregui.client.components.tab.SubTab;
import org.rhq.enterprise.gui.coregui.client.components.tab.TwoLevelTab;
import org.rhq.enterprise.gui.coregui.client.components.tab.TwoLevelTabSelectedEvent;
import org.rhq.enterprise.gui.coregui.client.components.tab.TwoLevelTabSelectedHandler;
import org.rhq.enterprise.gui.coregui.client.components.tab.TwoLevelTabSet;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.common.event.EventCompositeHistoryView;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.schedules.SchedulesView;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.traits.TraitsView;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.summary.OverviewView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceSearchView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * Right panel of the group view.
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class ResourceGroupDetailView extends LocatableVLayout implements BookmarkableView, TwoLevelTabSelectedHandler {
    private static final String DEFAULT_TAB_NAME = "Inventory";

    private int groupId;
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

    private SubTab summaryOverview;
    private SubTab summaryTimeline;
    private SubTab monitorGraphs;
    private SubTab monitorTables;
    private SubTab monitorTraits;
    private SubTab monitorSched;
    private SubTab monitorCallTime;
    private SubTab inventoryMembers;
    private SubTab inventoryConn;
    private SubTab opHistory;
    private SubTab opSched;
    private SubTab alertHistory;
    private SubTab alertDef;
    private SubTab configCurrent;
    private SubTab configHistory;
    private SubTab eventHistory;

    private ResourceGroupTitleBar titleBar;

    private String tabName;
    private String subTabName;

    public ResourceGroupDetailView(String locatorId) {
        super(locatorId);

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
        summaryTimeline = new SubTab(summaryTab.extendLocatorId("Timeline"), "Timeline", null);
        summaryTab.registerSubTabs(summaryOverview, summaryTimeline);

        monitoringTab = new TwoLevelTab(topTabSet.extendLocatorId("Monitoring"), "Monitoring",
                "/images/icons/Monitor_grey_16.png");
        monitorGraphs = new SubTab(monitoringTab.extendLocatorId("Graphs"), "Graphs", null);
        monitorTables = new SubTab(monitoringTab.extendLocatorId("Tables"), "Tables", null);
        monitorTraits = new SubTab(monitoringTab.extendLocatorId("Traits"), "Traits", null);

        monitorSched = new SubTab(monitoringTab.extendLocatorId("Schedules"), "Schedules", null);
        monitorCallTime = new SubTab(monitoringTab.extendLocatorId("CallTime"), "Call Time", null);
        monitoringTab.registerSubTabs(monitorGraphs, monitorTables, monitorTraits, monitorSched, monitorCallTime);

        inventoryTab = new TwoLevelTab(topTabSet.extendLocatorId("Inventory"), "Inventory",
                "/images/icons/Inventory_grey_16.png");
        inventoryMembers = new SubTab(inventoryTab.extendLocatorId("Members"), "Members", null);
        inventoryConn = new SubTab(inventoryTab.extendLocatorId("ConnectionSettings"), "Connection Settings", null);
        inventoryTab.registerSubTabs(this.inventoryMembers, this.inventoryConn);

        operationsTab = new TwoLevelTab(topTabSet.extendLocatorId("Operations"), "Operations",
                "/images/icons/Operation_grey_16.png");
        this.opHistory = new SubTab(operationsTab.extendLocatorId("History"), "History", null);
        this.opSched = new SubTab(operationsTab.extendLocatorId("Scheduled"), "Scheduled", null);
        operationsTab.registerSubTabs(this.opHistory, this.opSched);

        alertsTab = new TwoLevelTab(topTabSet.extendLocatorId("Alerts"), "Alerts", "/images/icons/Alert_grey_16.png");
        this.alertHistory = new SubTab(alertsTab.extendLocatorId("History"), "History", null);
        this.alertDef = new SubTab(alertsTab.extendLocatorId("Definitions"), "Definitions", null);
        alertsTab.registerSubTabs(alertHistory, alertDef);

        configurationTab = new TwoLevelTab(topTabSet.extendLocatorId("Configuration"), "Configuration",
                "/images/icons/Configure_grey_16.png");
        this.configCurrent = new SubTab(configurationTab.extendLocatorId("Current"), "Current", null);
        this.configHistory = new SubTab(configurationTab.extendLocatorId("History"), "History", null);
        configurationTab.registerSubTabs(this.configCurrent, this.configHistory);

        eventsTab = new TwoLevelTab(topTabSet.extendLocatorId("Events"), "Events", "/images/icons/Events_grey_16.png");
        this.eventHistory = new SubTab(eventsTab.extendLocatorId("History"), "History", null);
        eventsTab.registerSubTabs(eventHistory);

        topTabSet.setTabs(summaryTab, monitoringTab, inventoryTab, operationsTab, alertsTab, configurationTab,
                eventsTab);

        topTabSet.addTwoLevelTabSelectedHandler(this);

        titleBar = new ResourceGroupTitleBar(getLocatorId());
        addMember(titleBar);

        addMember(topTabSet);

        //        CoreGUI.addBreadCrumb(getPlace());
    }

    public void updateDetailViews(ResourceGroupComposite groupComposite) {

        this.groupComposite = groupComposite;

        this.titleBar.setGroup(groupComposite.getResourceGroup());

        for (Tab top : topTabSet.getTabs()) {
            ((TwoLevelTab) top).getLayout().destroyViews();
        }

        //        FullHTMLPane timelinePane = new FullHTMLPane("/rhq/resource/summary/timeline-plain.xhtml?id=" + resource.getId());
        //        summaryTab.updateSubTab("Overview", new DashboardView(resource));
        //        summaryTab.updateSubTab("Timeline", timelinePane);
        this.summaryOverview.setCanvas(new OverviewView(this.groupComposite));
        summaryTab.updateSubTab(this.summaryOverview);

        int groupId = this.groupComposite.getResourceGroup().getId();

        this.monitorGraphs.setCanvas(new FullHTMLPane("/rhq/group/monitor/graphs-plain.xhtml?groupId=" + groupId));
        this.monitorTables.setCanvas(new FullHTMLPane("/rhq/group/monitor/tables-plain.xhtml?groupId=" + groupId));
        this.monitorTraits.setCanvas(new TraitsView(this.monitoringTab.extendLocatorId("TraitsView"), groupId));
        this.monitorSched.setCanvas(new SchedulesView(this.monitoringTab.extendLocatorId("SchedulesView"), groupId));
        this.monitorCallTime.setCanvas(new FullHTMLPane("/rhq/group/monitor/response-plain.xhtml?groupId=" + groupId));
        monitoringTab.updateSubTab(this.monitorGraphs);
        monitoringTab.updateSubTab(this.monitorTables);
        monitoringTab.updateSubTab(this.monitorSched);
        monitoringTab.updateSubTab(this.monitorCallTime);

        this.inventoryMembers.setCanvas(ResourceSearchView.getMembersOf(groupId));
        inventoryTab.updateSubTab(this.inventoryMembers);

        //        inventoryTab.updateSubTab("Connection Settings", new GroupPluginConfigurationEditView(this.group.getId(), this.group.getResourceType().getId(), ConfigurationEditor.ConfigType.plugin));

        this.opHistory.setCanvas(new FullHTMLPane("/rhq/group/operation/groupOperationHistory-plain.xhtml?groupId="
                + groupId));
        this.opSched.setCanvas(new FullHTMLPane("/rhq/group/operation/groupOperationSchedules-plain.xhtml?groupId="
                + groupId));
        operationsTab.updateSubTab(this.opHistory);
        operationsTab.updateSubTab(this.opSched);

        this.alertHistory.setCanvas(new FullHTMLPane("/rhq/group/alert/listGroupAlertHistory-plain.xhtml?groupId="
                + groupId));
        this.alertDef.setCanvas(new FullHTMLPane("/rhq/group/alert/listGroupAlertDefinitions-plain.xhtml?groupId="
                + groupId));
        alertsTab.updateSubTab(this.alertHistory);
        alertsTab.updateSubTab(this.alertDef);

        this.configCurrent.setCanvas(new FullHTMLPane("/rhq/group/configuration/viewCurrent-plain.xhtml?groupId="
                + groupId));
        this.configHistory
                .setCanvas(new FullHTMLPane("/rhq/group/configuration/history-plain.xhtml?groupId=" + groupId));
        configurationTab.updateSubTab(this.configCurrent);
        configurationTab.updateSubTab(this.configHistory);

        this.eventHistory.setCanvas(EventCompositeHistoryView.get(groupComposite));
        eventsTab.updateSubTab(this.eventHistory);

        updateTabEnablement();
    }


    private void updateTabEnablement() {

        GroupCategory groupCategory = groupComposite.getResourceGroup().getGroupCategory();
        Set<ResourceTypeFacet> facets = groupComposite.getResourceFacets().getFacets();

        // Summary and Inventory tabs are always enabled.
        topTabSet.enableTab(summaryTab);
        topTabSet.enableTab(inventoryTab);

        // Inventory>Connection Settings subtab is only enabled for compat groups that define conn props.
        inventoryTab.setSubTabEnabled(this.inventoryConn.getLocatorId(), groupCategory == GroupCategory.COMPATIBLE
                && facets.contains(ResourceTypeFacet.PLUGIN_CONFIGURATION));

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

        // only enable "Call Time" and "Traits" subtabs for groups that implement them.
        monitoringTab.setSubTabEnabled(monitorTraits.getLocatorId(), hasTraits(this.groupComposite));
        monitoringTab.setSubTabEnabled(monitorCallTime.getLocatorId(), facets.contains(ResourceTypeFacet.CALL_TIME));
    }

    public void onTabSelected(TwoLevelTabSelectedEvent tabSelectedEvent) {
        if (this.groupComposite == null) {
//            History.fireCurrentHistoryState();
        } else {
            // Switch tabs directly, rather than letting the history framework do it, to avoid redrawing the outer views.
//            selectTab(tabSelectedEvent.getId(), tabSelectedEvent.getSubTabId());
            String tabPath = "/" + tabSelectedEvent.getId() + "/" + tabSelectedEvent.getSubTabId();
            String path = "ResourceGroup/" + this.groupComposite.getResourceGroup().getId() + tabPath;

            // But still add an item to the history, specifying false to tell it not to fire an event.
            History.newItem(path, true);
        }
    }

    public void renderView(ViewPath viewPath) {
        // e.g. #ResourceGroup/10010/Inventory/Overview
        int groupId = Integer.parseInt(viewPath.getCurrent().getPath());

        viewPath.next();

        tabName = (!viewPath.isEnd()) ? viewPath.getCurrent().getPath() : null; // e.g. "Inventory"
        subTabName = (viewPath.viewsLeft() >= 1) ? viewPath.getNext().getPath() : null; // e.g. "Overview"

        viewPath.next();
        viewPath.next();


        if (this.groupId != groupId) {
            loadSelectedGroup(groupId, viewPath);
        } else {
            // Same group just switching tabs
            selectTab(tabName, subTabName, viewPath);
        }
    }

    public void loadSelectedGroup(final int groupId, final ViewPath viewPath) {
        this.groupId = groupId;

        ResourceGroupCriteria criteria = new ResourceGroupCriteria();
        criteria.addFilterId(groupId);
        criteria.addFilterVisible(null);

        GWTServiceLookup.getResourceGroupService().findResourceGroupCompositesByCriteria(criteria,
                new AsyncCallback<PageList<ResourceGroupComposite>>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError("Failed to load group composite for group with id "
                                + groupId, caught);
                    }

                    public void onSuccess(PageList<ResourceGroupComposite> result) {
                        groupComposite = result.get(0);
                        loadResourceType(groupComposite, viewPath);
                    }
                });
    }


    private void loadResourceType(final ResourceGroupComposite groupComposite, final ViewPath viewPath) {
        final ResourceGroup group = this.groupComposite.getResourceGroup();

        if (group.getGroupCategory() == GroupCategory.COMPATIBLE) {

            // Load the fully fetched ResourceType.
            ResourceType groupType = group.getResourceType();
            ResourceTypeRepository.Cache.getInstance().getResourceTypes(
                    groupType.getId(),
                    EnumSet.of(ResourceTypeRepository.MetadataType.content,
                            ResourceTypeRepository.MetadataType.operations,
                            ResourceTypeRepository.MetadataType.measurements,
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
                                            updateDetailViews(groupComposite);
                                            selectTab(tabName, subTabName, viewPath);
                                        }
                                    });
                        }
                    });
        }
    }


    public void selectTab(String tabName, String subtabName, ViewPath viewPath) {
        if (tabName == null) {
            tabName = DEFAULT_TAB_NAME;
        }
        TwoLevelTab tab = this.topTabSet.getTabByTitle(tabName);
        if (tab == null) {
            CoreGUI.getErrorHandler().handleError("Invalid tab name: " + tabName);
            // TODO: Should we fire a history event here to redirect to a valid bookmark?
            tab = this.topTabSet.getTabByTitle(DEFAULT_TAB_NAME);
        }
        this.topTabSet.selectTab(tab);
        if (subtabName != null) {
            if (!tab.getLayout().selectTab(subtabName)) {
                CoreGUI.getErrorHandler().handleError("Invalid subtab name: " + subtabName);                
                // TODO: Should we fire a history event here to redirect to a valid bookmark?
                return;
            } else {
                Canvas subView = tab.getLayout().getCurrentCanvas();
                if (subView instanceof BookmarkableView) {
                    ((BookmarkableView) subView).renderView(viewPath);
                }
            }
        }
    }


    private static boolean hasTraits(ResourceGroupComposite groupComposite) {
        ResourceType type = groupComposite.getResourceGroup().getResourceType();
        if (type != null) {
            Set<MeasurementDefinition> metricDefs = type.getMetricDefinitions();
            for (MeasurementDefinition metricDef : metricDefs) {
                if (metricDef.getDataType() == DataType.TRAIT) {
                    return true;
                }
            }
        }
        return false;
    }
}
