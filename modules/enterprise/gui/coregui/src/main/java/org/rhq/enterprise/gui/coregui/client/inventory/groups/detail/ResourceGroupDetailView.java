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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
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
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.alert.GroupAlertHistoryView;
import org.rhq.enterprise.gui.coregui.client.alert.definitions.GroupAlertDefinitionsView;
import org.rhq.enterprise.gui.coregui.client.components.FullHTMLPane;
import org.rhq.enterprise.gui.coregui.client.components.tab.SubTab;
import org.rhq.enterprise.gui.coregui.client.components.tab.TwoLevelTab;
import org.rhq.enterprise.gui.coregui.client.components.tab.TwoLevelTabSelectedEvent;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.AbstractTwoLevelTabSetView;
import org.rhq.enterprise.gui.coregui.client.inventory.common.event.EventCompositeHistoryView;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.configuration.GroupResourceConfigurationEditView;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.inventory.ResourceGroupMembershipView;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.schedules.SchedulesView;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.traits.TraitsView;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.summary.OverviewView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceSearchView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;

/**
 * Be able to view members as a resource list, or edit members via selector.  
 *
 * @author Jay Shaughnessy
 * @author Ian Springer
 */
public class ResourceGroupDetailView extends AbstractTwoLevelTabSetView<ResourceGroupComposite, ResourceGroupTitleBar> {
    public static final String AUTO_GROUP_VIEW_PATH = "Resource/AutoGroup";

    private Integer groupId;
    private ResourceGroupComposite groupComposite;
    private ResourcePermission permissions;

    // tabs
    private TwoLevelTab summaryTab;
    private TwoLevelTab monitoringTab;
    private TwoLevelTab inventoryTab;
    private TwoLevelTab operationsTab;
    private TwoLevelTab alertsTab;
    private TwoLevelTab configurationTab;
    private TwoLevelTab eventsTab;

    // subtabs
    private SubTab summaryOverview;
    private SubTab summaryTimeline;
    private SubTab monitorGraphs;
    private SubTab monitorTables;
    private SubTab monitorTraits;
    private SubTab monitorSched;
    private SubTab monitorCallTime;
    private SubTab inventoryMembers;
    private SubTab inventoryConn;
    private SubTab inventoryMembership;
    private SubTab opHistory;
    private SubTab opSched;
    private SubTab alertHistory;
    private SubTab alertDef;
    private SubTab configCurrent;
    private SubTab configHistory;
    private SubTab eventHistory;

    private String currentTab;
    private String currentSubTab;

    public ResourceGroupDetailView(String locatorId, String baseViewPath) {
        super(locatorId, baseViewPath);
        this.hide();
    }

    @Override
    public Integer getSelectedItemId() {
        return this.groupId;
    }

    @Override
    public void onTabSelected(TwoLevelTabSelectedEvent tabSelectedEvent) {
        // if moving from membership subtab then re-load the detail view as the membership and
        // group type may have changed.        
        if ((null != this.groupId) && this.inventoryTab.getTitle().equals(currentTab)
            && this.inventoryMembership.getTitle().equals(currentSubTab)) {

            String tabPath = "/" + tabSelectedEvent.getId() + "/" + tabSelectedEvent.getSubTabId();
            String path = this.getBaseViewPath() + "/" + getSelectedItemId() + tabPath;

            this.currentTab = null;
            this.currentSubTab = null;
            this.groupId = null;

            CoreGUI.goToView(path);

        } else {
            super.onTabSelected(tabSelectedEvent);
        }
    }

    @Override
    protected ResourceGroupTitleBar createTitleBar() {
        return new ResourceGroupTitleBar(getLocatorId());
    }

    protected List<TwoLevelTab> createTabs() {
        List<TwoLevelTab> tabs = new ArrayList<TwoLevelTab>();

        summaryTab = new TwoLevelTab(getTabSet().extendLocatorId("Summary"), "Summary",
            "/images/icons/Service_up_16.png");
        summaryOverview = new SubTab(summaryTab.extendLocatorId("Overview"), "Overview", null);
        summaryTimeline = new SubTab(summaryTab.extendLocatorId("Timeline"), "Timeline", null);
        summaryTab.registerSubTabs(summaryOverview, summaryTimeline);
        tabs.add(summaryTab);

        monitoringTab = new TwoLevelTab(getTabSet().extendLocatorId("Monitoring"), "Monitoring",
            "/images/icons/Monitor_grey_16.png");
        monitorGraphs = new SubTab(monitoringTab.extendLocatorId("Graphs"), "Graphs", null);
        monitorTables = new SubTab(monitoringTab.extendLocatorId("Tables"), "Tables", null);
        monitorTraits = new SubTab(monitoringTab.extendLocatorId("Traits"), "Traits", null);

        monitorSched = new SubTab(monitoringTab.extendLocatorId("Schedules"), "Schedules", null);
        monitorCallTime = new SubTab(monitoringTab.extendLocatorId("CallTime"), "Call Time", null);
        monitoringTab.registerSubTabs(monitorGraphs, monitorTables, monitorTraits, monitorSched, monitorCallTime);
        tabs.add(monitoringTab);

        inventoryTab = new TwoLevelTab(getTabSet().extendLocatorId("Inventory"), "Inventory",
            "/images/icons/Inventory_grey_16.png");
        inventoryMembers = new SubTab(inventoryTab.extendLocatorId("Members"), "Members", null);
        inventoryConn = new SubTab(inventoryTab.extendLocatorId("ConnectionSettings"), "Connection Settings", null);
        inventoryMembership = new SubTab(inventoryTab.extendLocatorId("Membership"), "Membership", null);
        inventoryTab.registerSubTabs(this.inventoryMembers, this.inventoryConn, this.inventoryMembership);
        tabs.add(inventoryTab);

        operationsTab = new TwoLevelTab(getTabSet().extendLocatorId("Operations"), "Operations",
            "/images/icons/Operation_grey_16.png");
        this.opHistory = new SubTab(operationsTab.extendLocatorId("History"), "History", null);
        this.opSched = new SubTab(operationsTab.extendLocatorId("Scheduled"), "Scheduled", null);
        operationsTab.registerSubTabs(this.opHistory, this.opSched);
        tabs.add(operationsTab);

        alertsTab = new TwoLevelTab(getTabSet().extendLocatorId("Alerts"), "Alerts", "/images/icons/Alert_grey_16.png");
        this.alertHistory = new SubTab(alertsTab.extendLocatorId("History"), "History", null);
        this.alertDef = new SubTab(alertsTab.extendLocatorId("Definitions"), "Definitions", null);
        alertsTab.registerSubTabs(alertHistory, alertDef);
        tabs.add(alertsTab);

        configurationTab = new TwoLevelTab(getTabSet().extendLocatorId("Configuration"), "Configuration",
            "/images/icons/Configure_grey_16.png");
        this.configCurrent = new SubTab(configurationTab.extendLocatorId("Current"), "Current", null);
        this.configHistory = new SubTab(configurationTab.extendLocatorId("History"), "History", null);
        configurationTab.registerSubTabs(this.configCurrent, this.configHistory);
        tabs.add(configurationTab);

        eventsTab = new TwoLevelTab(getTabSet().extendLocatorId("Events"), "Events", "/images/icons/Events_grey_16.png");
        this.eventHistory = new SubTab(eventsTab.extendLocatorId("History"), "History", null);
        eventsTab.registerSubTabs(eventHistory);
        tabs.add(eventsTab);

        return tabs;
    }

    protected void updateTabContent(ResourceGroupComposite groupComposite, Set<Permission> globalPermissions) {
        boolean enabled;
        boolean visible;
        Canvas canvas;

        this.groupComposite = groupComposite;
        ResourceGroup group = groupComposite.getResourceGroup();
        int groupId = group.getId();
        getTitleBar().setGroup(group);

        for (Tab top : this.getTabSet().getTabs()) {
            ((TwoLevelTab) top).getLayout().destroyViews();
        }

        GroupCategory groupCategory = groupComposite.getResourceGroup().getGroupCategory();
        Set<ResourceTypeFacet> facets = groupComposite.getResourceFacets().getFacets();

        //        FullHTMLPane timelinePane = new FullHTMLPane("/rhq/resource/summary/timeline-plain.xhtml?id=" + resource.getId());
        //        summaryTab.updateSubTab("Overview", new DashboardView(resource));
        //        summaryTab.updateSubTab("Timeline", timelinePane);
        // Summary tab is always visible and enabled.
        updateSubTab(this.summaryTab, this.summaryOverview, new OverviewView(this.summaryTab
            .extendLocatorId("OverviewView"), this.groupComposite), true, true);

        if (updateTab(this.monitoringTab, groupCategory == GroupCategory.COMPATIBLE, true)) {
            visible = hasMetricsOfType(this.groupComposite, DataType.MEASUREMENT);
            canvas = (visible) ? new FullHTMLPane("/rhq/group/monitor/graphs-plain.xhtml?groupId=" + groupId) : null;
            updateSubTab(this.monitoringTab, this.monitorGraphs, canvas, visible, true);
            // visible = same test as above
            canvas = (visible) ? new FullHTMLPane("/rhq/group/monitor/tables-plain.xhtml?groupId=" + groupId) : null;
            updateSubTab(this.monitoringTab, this.monitorTables, canvas, visible, true);
            visible = hasMetricsOfType(this.groupComposite, DataType.TRAIT);
            canvas = (visible) ? new TraitsView(this.monitoringTab.extendLocatorId("TraitsView"), groupId) : null;
            updateSubTab(this.monitoringTab, this.monitorTraits, canvas, visible, true);
            visible = hasMetricsOfType(this.groupComposite, null);
            canvas = (visible) ? new SchedulesView(this.monitoringTab.extendLocatorId("SchedulesView"), groupId) : null;
            updateSubTab(this.monitoringTab, this.monitorSched, canvas, visible, true);
            visible = facets.contains(ResourceTypeFacet.CALL_TIME);
            canvas = (visible) ? new FullHTMLPane("/rhq/group/monitor/response-plain.xhtml?groupId=" + groupId) : null;
            updateSubTab(this.monitoringTab, this.monitorCallTime, canvas, visible, true);
            // TODO (ips): Add Availability subtab.
        }

        // Inventory tab is always visible and enabled.
        updateSubTab(this.inventoryTab, this.inventoryMembers, ResourceSearchView.getMembersOf(this.inventoryTab
            .extendLocatorId("MembersView"), groupId), true, true);
        // TODO: Uncomment this once the group config component is done.
        //updateSubTab(this.inventoryTab, this.inventoryConn,
        //     new GroupPluginConfigurationEditView(this.group.getId(), this.group.getResourceType().getId(), ConfigurationEditor.ConfigType.plugin),
        //     facets.contains(ResourceTypeFacet.PLUGIN_CONFIGURATION), true);
        enabled = globalPermissions.contains(Permission.MANAGE_INVENTORY);
        canvas = (enabled) ? new ResourceGroupMembershipView(this.inventoryTab.extendLocatorId("MembershipView"),
            groupId) : null;
        updateSubTab(this.inventoryTab, this.inventoryMembership, canvas, true, enabled);

        if (updateTab(this.operationsTab, groupCategory == GroupCategory.COMPATIBLE
            && facets.contains(ResourceTypeFacet.OPERATION), true)) {
            updateSubTab(this.operationsTab, this.opHistory, new FullHTMLPane(
                "/rhq/group/operation/groupOperationHistory-plain.xhtml?groupId=" + groupId), true, true);
            updateSubTab(this.operationsTab, this.opSched, new FullHTMLPane(
                "/rhq/group/operation/groupOperationSchedules-plain.xhtml?groupId=" + groupId), true, true);
        }

        // alerts tab is always visible, even for mixed groups
        if (updateTab(this.alertsTab, true, true)) {
            // alert history is always available
            updateSubTab(this.alertsTab, this.alertHistory, GroupAlertHistoryView.get(groupComposite), true, true);
            // but alert definitions can only be created on compatible groups
            visible = (groupCategory == GroupCategory.COMPATIBLE);
            canvas = (visible) ? new GroupAlertDefinitionsView(alertsTab.extendLocatorId("AlertDefView"),
                this.groupComposite) : null;
            updateSubTab(this.alertsTab, this.alertDef, canvas, visible, true);
        }

        visible = groupCategory == GroupCategory.COMPATIBLE && facets.contains(ResourceTypeFacet.CONFIGURATION);
        if (updateTab(this.configurationTab, visible, visible && this.permissions.isConfigureRead())) {
            //updateSubTab(this.configurationTab, this.configCurrent, new FullHTMLPane(
            //    "/rhq/group/configuration/viewCurrent-plain.xhtml?groupId=" + groupId), true, true);
            updateSubTab(this.configurationTab, this.configCurrent, new GroupResourceConfigurationEditView(
                this.configCurrent.extendLocatorId("View"), this.groupComposite), true, true);
            updateSubTab(this.configurationTab, this.configHistory, new FullHTMLPane(
                "/rhq/group/configuration/history-plain.xhtml?groupId=" + groupId), true, true);
        }
        // allow mixed groups to show events from supporting resources
        visible = groupCategory == GroupCategory.MIXED
            || (groupCategory == GroupCategory.COMPATIBLE && facets.contains(ResourceTypeFacet.EVENT));
        if (updateTab(this.eventsTab, visible, true)) {
            updateSubTab(this.eventsTab, this.eventHistory, EventCompositeHistoryView.get(this.eventsTab
                .extendLocatorId("CompositeHistView"), groupComposite), true, true);
        }

        this.show();
        markForRedraw();
    }

    protected void loadSelectedItem(final int groupId, final ViewPath viewPath, final Set<Permission> globalPermissions) {
        this.groupId = groupId;

        ResourceGroupCriteria criteria = new ResourceGroupCriteria();
        criteria.addFilterId(groupId);

        // for autogroups we need to add more criteria
        if (AUTO_GROUP_VIEW_PATH.equals(getBaseViewPath())) {
            criteria.addFilterVisible(null);
            criteria.addFilterPrivate(true);
        }

        GWTServiceLookup.getResourceGroupService().findResourceGroupCompositesByCriteria(criteria,
            new AsyncCallback<PageList<ResourceGroupComposite>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(
                        "Failed to load group composite for group with id " + groupId, caught);
                }

                public void onSuccess(PageList<ResourceGroupComposite> result) {
                    if (result.isEmpty()) {
                        CoreGUI.getErrorHandler().handleError(
                            "Failed to load group composite for group with id " + groupId);
                    }

                    groupComposite = result.get(0);
                    loadResourceType(groupComposite, viewPath, globalPermissions);
                }
            });
    }

    private void loadResourceType(final ResourceGroupComposite groupComposite, final ViewPath viewPath,
        final Set<Permission> globalPermissions) {
        final ResourceGroup group = this.groupComposite.getResourceGroup();

        if (group.getGroupCategory() == GroupCategory.COMPATIBLE) {

            // Load the fully fetched ResourceType.
            ResourceType groupType = group.getResourceType();
            ResourceTypeRepository.Cache.getInstance().getResourceTypes(
                groupType.getId(),
                EnumSet.of(ResourceTypeRepository.MetadataType.content, ResourceTypeRepository.MetadataType.operations,
                    ResourceTypeRepository.MetadataType.measurements, ResourceTypeRepository.MetadataType.events,
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
                                    updateTabContent(groupComposite, globalPermissions);
                                    selectTab(getTabName(), getSubTabName(), viewPath);
                                }
                            });
                    }
                });
        } else {
            updateTabContent(groupComposite, globalPermissions);
            selectTab(getTabName(), getSubTabName(), viewPath);
        }
    }

    private static boolean hasMetricsOfType(ResourceGroupComposite groupComposite, DataType dataType) {
        ResourceType type = groupComposite.getResourceGroup().getResourceType();
        if (type != null) {
            Set<MeasurementDefinition> metricDefs = type.getMetricDefinitions();
            for (MeasurementDefinition metricDef : metricDefs) {
                if (dataType == null || metricDef.getDataType() == dataType) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void selectTab(String tabTitle, String subtabTitle, ViewPath viewPath) {
        currentTab = tabTitle;
        currentSubTab = subtabTitle;
        super.selectTab(tabTitle, subtabTitle, viewPath);
    }
}
