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
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.ResourceTypeFacet;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.alert.GroupAlertHistoryView;
import org.rhq.enterprise.gui.coregui.client.alert.definitions.GroupAlertDefinitionsView;
import org.rhq.enterprise.gui.coregui.client.components.FullHTMLPane;
import org.rhq.enterprise.gui.coregui.client.components.tab.SubTab;
import org.rhq.enterprise.gui.coregui.client.components.tab.TwoLevelTab;
import org.rhq.enterprise.gui.coregui.client.components.tab.TwoLevelTabSelectedEvent;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.AbstractTwoLevelTabSetView;
import org.rhq.enterprise.gui.coregui.client.inventory.common.event.EventCompositeHistoryView;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.configuration.GroupResourceConfigurationEditView;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.inventory.CurrentGroupPluginConfigurationView;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.inventory.HistoryGroupPluginConfigurationView;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.inventory.MembersView;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.schedules.SchedulesView;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.traits.TraitsView;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.summary.ActivityView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;

/**
 * Right panel of the Resource Group view (#ResourceGroup/*).
 *
 * @author Jay Shaughnessy
 * @author Ian Springer
 */
public class ResourceGroupDetailView extends AbstractTwoLevelTabSetView<ResourceGroupComposite, ResourceGroupTitleBar> {
    public static final String AUTO_CLUSTER_VIEW_PATH = "ResourceGroup/AutoCluster";
    public static final String AUTO_GROUP_VIEW_PATH = "Resource/AutoGroup";

    private Integer groupId;
    private ResourceGroupComposite groupComposite;

    // tabs
    private TwoLevelTab summaryTab;
    private TwoLevelTab monitoringTab;
    private TwoLevelTab inventoryTab;
    private TwoLevelTab operationsTab;
    private TwoLevelTab alertsTab;
    private TwoLevelTab configurationTab;
    private TwoLevelTab eventsTab;

    // subtabs
    private SubTab summaryActivity;
    private SubTab summaryTimeline;
    private SubTab monitorGraphs;
    private SubTab monitorTables;
    private SubTab monitorTraits;
    private SubTab monitorSched;
    private SubTab monitorCallTime;
    private SubTab inventoryMembers;
    private SubTab inventoryConn;
    private SubTab inventoryConnHistory;
    private SubTab operationsHistory;
    private SubTab operationsSchedule;
    private SubTab alertHistory;
    private SubTab alertDef;
    private SubTab configCurrent;
    private SubTab configHistory;
    private SubTab eventHistory;

    private String currentTabName;
    private String currentSubTabName;

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
        // if moving from members subtab then re-load the detail view as the membership and
        // group type may have changed.        
        if ((null != this.groupId) && this.inventoryTab.getName().equals(currentTabName)
            && this.inventoryMembers.getName().equals(currentSubTabName)) {

            String tabPath = "/" + tabSelectedEvent.getId() + "/" + tabSelectedEvent.getSubTabId();
            String path = this.getBaseViewPath() + "/" + getSelectedItemId() + tabPath;

            this.currentTabName = null;
            this.currentSubTabName = null;
            this.groupId = null;

            CoreGUI.goToView(path);

        } else {
            super.onTabSelected(tabSelectedEvent);
        }
    }

    @Override
    protected ResourceGroupTitleBar createTitleBar() {
        return new ResourceGroupTitleBar(getLocatorId(), isAutoGroup(), isAutoCluster());
    }

    protected List<TwoLevelTab> createTabs() {
        List<TwoLevelTab> tabs = new ArrayList<TwoLevelTab>();

        summaryTab = new TwoLevelTab(getTabSet().extendLocatorId("Summary"), new ViewName("Summary", MSG
            .view_tabs_common_summary()), ImageManager.getResourceIcon(ResourceCategory.SERVICE, Boolean.TRUE));
        summaryActivity = new SubTab(summaryTab.extendLocatorId("Activity"), new ViewName("Activity", MSG
            .view_tabs_common_activity()), null);
        summaryTimeline = new SubTab(summaryTab.extendLocatorId("Timeline"), new ViewName("Timeline", MSG
            .view_tabs_common_timeline()), null);
        summaryTab.registerSubTabs(summaryActivity, summaryTimeline);
        tabs.add(summaryTab);

        inventoryTab = new TwoLevelTab(getTabSet().extendLocatorId("Inventory"), new ViewName("Inventory", MSG
            .view_tabs_common_inventory()), "/images/icons/Inventory_grey_16.png");
        inventoryMembers = new SubTab(inventoryTab.extendLocatorId("Members"), new ViewName("Members", MSG
            .view_tabs_common_members()), null);
        inventoryConn = new SubTab(inventoryTab.extendLocatorId("ConnectionSettings"), new ViewName(
            "ConnectionSettings", MSG.view_tabs_common_connectionSettings()), null);
        inventoryConnHistory = new SubTab(inventoryTab.extendLocatorId("ConnectionSettingsHistory"), new ViewName(
            "ConnectionSettingsHistory", MSG.view_tabs_common_connectionSettingsHistory()), null);
        inventoryTab.registerSubTabs(this.inventoryMembers, this.inventoryConn, this.inventoryConnHistory);
        tabs.add(inventoryTab);

        alertsTab = new TwoLevelTab(getTabSet().extendLocatorId("Alerts"), new ViewName("Alerts", MSG
            .view_tabs_common_alerts()), "/images/icons/Alert_grey_16.png");
        this.alertHistory = new SubTab(alertsTab.extendLocatorId("History"), new ViewName("History", MSG
            .view_tabs_common_history()), null);
        this.alertDef = new SubTab(alertsTab.extendLocatorId("Definitions"), new ViewName("Definitions", MSG
            .view_tabs_common_definitions()), null);
        alertsTab.registerSubTabs(alertHistory, alertDef);
        tabs.add(alertsTab);

        monitoringTab = new TwoLevelTab(getTabSet().extendLocatorId("Monitoring"), new ViewName("Monitoring", MSG
            .view_tabs_common_monitoring()), "/images/icons/Monitor_grey_16.png");
        monitorGraphs = new SubTab(monitoringTab.extendLocatorId("Graphs"), new ViewName("Graphs", MSG
            .view_tabs_common_graphs()), null);
        monitorTables = new SubTab(monitoringTab.extendLocatorId("Tables"), new ViewName("Tables", MSG
            .view_tabs_common_tables()), null);
        monitorTraits = new SubTab(monitoringTab.extendLocatorId("Traits"), new ViewName("Traits", MSG
            .view_tabs_common_traits()), null);

        monitorSched = new SubTab(monitoringTab.extendLocatorId("Schedules"), new ViewName("Schedules", MSG
            .view_tabs_common_schedules()), null);
        monitorCallTime = new SubTab(monitoringTab.extendLocatorId("CallTime"), new ViewName("CallTime", MSG
            .view_tabs_common_calltime()), null);
        monitoringTab.registerSubTabs(monitorGraphs, monitorTables, monitorTraits, monitorSched, monitorCallTime);
        tabs.add(monitoringTab);

        eventsTab = new TwoLevelTab(getTabSet().extendLocatorId("Events"), new ViewName("Events", MSG
            .view_tabs_common_events()), "/images/icons/Events_grey_16.png");
        this.eventHistory = new SubTab(eventsTab.extendLocatorId("History"), new ViewName("History", MSG
            .view_tabs_common_history()), null);
        eventsTab.registerSubTabs(eventHistory);
        tabs.add(eventsTab);

        operationsTab = new TwoLevelTab(getTabSet().extendLocatorId("Operations"), new ViewName("Operations", MSG
            .view_tabs_common_operations()), "/images/icons/Operation_grey_16.png");
        this.operationsSchedule = new SubTab(operationsTab.extendLocatorId("Schedule"), new ViewName("Schedule", MSG
            .view_tabs_common_schedule()), null);
        this.operationsHistory = new SubTab(operationsTab.extendLocatorId("History"), new ViewName("History", MSG
            .view_tabs_common_history()), null);
        operationsTab.registerSubTabs(this.operationsHistory, this.operationsSchedule);
        tabs.add(operationsTab);

        configurationTab = new TwoLevelTab(getTabSet().extendLocatorId("Configuration"), new ViewName("Configuration",
            MSG.view_tabs_common_configuration()), "/images/icons/Configure_grey_16.png");
        this.configCurrent = new SubTab(configurationTab.extendLocatorId("Current"), new ViewName("Current", MSG
            .view_tabs_common_current()), null);
        this.configHistory = new SubTab(configurationTab.extendLocatorId("History"), new ViewName("History", MSG
            .view_tabs_common_history()), null);
        configurationTab.registerSubTabs(this.configCurrent, this.configHistory);
        tabs.add(configurationTab);

        return tabs;
    }

    protected void updateTabContent(ResourceGroupComposite groupComposite) {
        this.groupComposite = groupComposite;
        ResourceGroup group = groupComposite.getResourceGroup();
        int groupId = group.getId();
        getTitleBar().setGroup(groupComposite);

        for (Tab top : this.getTabSet().getTabs()) {
            ((TwoLevelTab) top).getLayout().destroyViews();
        }

        GroupCategory groupCategory = groupComposite.getResourceGroup().getGroupCategory();
        Set<ResourceTypeFacet> facets = groupComposite.getResourceFacets().getFacets();

        updateSummaryTab();
        updateMonitoringTab(groupId, groupCategory, facets);
        updateInventoryTab(groupId, facets);
        updateOperationsTab(groupId, groupCategory, facets);
        updateAlertsTab(groupComposite, groupCategory);
        updateConfigurationTab(groupId, groupCategory, facets);
        updateEventsTab(groupComposite, groupCategory, facets);

        this.show();
        markForRedraw();
    }

    private void updateSummaryTab() {
        // Summary tab is always visible and enabled.
        updateSubTab(this.summaryTab, this.summaryActivity, new ActivityView(this.summaryActivity
            .extendLocatorId("View"), this.groupComposite), true, true);
        // TODO (ips): Add Timeline subtab?
    }

    private void updateMonitoringTab(int groupId, GroupCategory groupCategory, Set<ResourceTypeFacet> facets) {
        boolean visible;
        Canvas canvas;
        if (updateTab(this.monitoringTab, groupCategory == GroupCategory.COMPATIBLE, true)) {
            visible = hasMetricsOfType(this.groupComposite, DataType.MEASUREMENT);
            canvas = (visible) ? new FullHTMLPane(this.monitorGraphs.extendLocatorId("View"),
                "/rhq/group/monitor/graphs-plain.xhtml?groupId=" + groupId) : null;
            updateSubTab(this.monitoringTab, this.monitorGraphs, canvas, visible, true);

            // visible = same test as above
            canvas = (visible) ? new FullHTMLPane(this.monitorTables.extendLocatorId("View"),
                "/rhq/group/monitor/tables-plain.xhtml?groupId=" + groupId) : null;
            updateSubTab(this.monitoringTab, this.monitorTables, canvas, visible, true);

            visible = hasMetricsOfType(this.groupComposite, DataType.TRAIT);
            canvas = (visible) ? new TraitsView(this.monitorTraits.extendLocatorId("View"), groupId) : null;
            updateSubTab(this.monitoringTab, this.monitorTraits, canvas, visible, true);

            visible = hasMetricsOfType(this.groupComposite, null);
            canvas = (visible) ? new SchedulesView(this.monitorSched.extendLocatorId("View"), groupId) : null;
            updateSubTab(this.monitoringTab, this.monitorSched, canvas, visible, true);

            visible = facets.contains(ResourceTypeFacet.CALL_TIME);
            canvas = (visible) ? new FullHTMLPane(this.monitorCallTime.extendLocatorId("View"),
                "/rhq/group/monitor/response-plain.xhtml?groupId=" + groupId) : null;
            updateSubTab(this.monitoringTab, this.monitorCallTime, canvas, visible, true);
            // TODO (ips): Add Availability subtab.
        }
    }

    private void updateInventoryTab(int groupId, Set<ResourceTypeFacet> facets) {
        // Inventory tab is always visible and enabled.
        boolean canModifyMembers = (!isAutoGroup() && !isAutoCluster()
            && globalPermissions.contains(Permission.MANAGE_INVENTORY));
        updateSubTab(this.inventoryTab, this.inventoryMembers, new MembersView(this.inventoryMembers
            .extendLocatorId("View"), groupId, canModifyMembers), true, true);
        updateSubTab(this.inventoryTab, this.inventoryConn, new CurrentGroupPluginConfigurationView(this.inventoryConn
            .extendLocatorId("View"), this.groupComposite), facets.contains(ResourceTypeFacet.PLUGIN_CONFIGURATION),
            true);
        updateSubTab(this.inventoryTab, this.inventoryConnHistory, new HistoryGroupPluginConfigurationView(
            this.inventoryConnHistory.extendLocatorId("View"), this.groupComposite), facets
            .contains(ResourceTypeFacet.PLUGIN_CONFIGURATION), true);
    }

    private void updateOperationsTab(int groupId, GroupCategory groupCategory, Set<ResourceTypeFacet> facets) {
        if (updateTab(this.operationsTab, groupCategory == GroupCategory.COMPATIBLE
            && facets.contains(ResourceTypeFacet.OPERATION), true)) {
            updateSubTab(this.operationsTab, this.operationsSchedule, new FullHTMLPane(this.operationsSchedule.extendLocatorId("View"),
                "/rhq/group/operation/groupOperationSchedules-plain.xhtml?groupId=" + groupId), true, true);
            updateSubTab(this.operationsTab, this.operationsHistory, new FullHTMLPane(this.operationsHistory.extendLocatorId("View"),
                "/rhq/group/operation/groupOperationHistory-plain.xhtml?groupId=" + groupId), true, true);
        }
    }

    private void updateAlertsTab(ResourceGroupComposite groupComposite, GroupCategory groupCategory) {
        // alerts tab is always visible, even for mixed groups
        if (updateTab(this.alertsTab, true, true)) {
            // alert history is always available
            updateSubTab(this.alertsTab, this.alertHistory, GroupAlertHistoryView.get(this.alertHistory
                .extendLocatorId("View"), groupComposite), true, true);
            // but alert definitions can only be created on compatible groups
            boolean visible = (groupCategory == GroupCategory.COMPATIBLE);
            Canvas canvas = (visible) ? new GroupAlertDefinitionsView(alertDef.extendLocatorId("View"), this.groupComposite)
                : null;
            updateSubTab(this.alertsTab, this.alertDef, canvas, visible, true);
        }
    }

    private void updateConfigurationTab(int groupId, GroupCategory groupCategory, Set<ResourceTypeFacet> facets) {
        boolean visible = (groupCategory == GroupCategory.COMPATIBLE && facets.contains(ResourceTypeFacet.CONFIGURATION));
        Set<Permission> groupPermissions = this.groupComposite.getResourcePermission().getPermissions();
        if (updateTab(this.configurationTab, visible, visible && groupPermissions.contains(Permission.CONFIGURE_READ))) {
            //updateSubTab(this.configurationTab, this.configCurrent, new FullHTMLPane(
            //    "/rhq/group/configuration/viewCurrent-plain.xhtml?groupId=" + groupId), true, true);
            updateSubTab(this.configurationTab, this.configCurrent, new GroupResourceConfigurationEditView(
                this.configCurrent.extendLocatorId("View"), this.groupComposite), true, true);
            updateSubTab(this.configurationTab, this.configHistory, new FullHTMLPane(this.configHistory
                .extendLocatorId("View"), "/rhq/group/configuration/history-plain.xhtml?groupId=" + groupId), true,
                true);
        }
    }

    private void updateEventsTab(ResourceGroupComposite groupComposite, GroupCategory groupCategory,
                                 Set<ResourceTypeFacet> facets) {
        // allow mixed groups to show events from supporting resources
        boolean visible = (groupCategory == GroupCategory.MIXED
            || (groupCategory == GroupCategory.COMPATIBLE && facets.contains(ResourceTypeFacet.EVENT)));
        if (updateTab(this.eventsTab, visible, true)) {
            updateSubTab(this.eventsTab, this.eventHistory, EventCompositeHistoryView.get(this.eventHistory
                .extendLocatorId("View"), groupComposite), true, true);
        }
    }

    @Override
    protected ResourceGroupComposite getSelectedItem() {
        return this.groupComposite;
    }

    protected void loadSelectedItem(final int groupId, final ViewPath viewPath) {
        this.groupId = groupId;

        ResourceGroupCriteria criteria = new ResourceGroupCriteria();
        criteria.addFilterId(groupId);

        // for autoclusters and autogroups we need to add more criteria
        if (isAutoCluster()) {
            criteria.addFilterVisible(null);

        } else if (isAutoGroup()) {
            criteria.addFilterVisible(null);
            criteria.addFilterPrivate(true);
        }

        GWTServiceLookup.getResourceGroupService().findResourceGroupCompositesByCriteria(criteria,
            new AsyncCallback<PageList<ResourceGroupComposite>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_group_detail_failLoadComp(String.valueOf(groupId)),
                        caught);
                }

                public void onSuccess(PageList<ResourceGroupComposite> result) {
                    if (result.isEmpty()) {
                        CoreGUI.getErrorHandler().handleError(
                            MSG.view_group_detail_failLoadComp(String.valueOf(groupId)));
                    } else {
                        groupComposite = result.get(0);
                        loadResourceType(groupComposite, viewPath);
                    }
                }
            });
    }

    private boolean isAutoGroup() {
        return AUTO_GROUP_VIEW_PATH.equals(getBaseViewPath());
    }

    private boolean isAutoCluster() {
        return AUTO_CLUSTER_VIEW_PATH.equals(getBaseViewPath());
    }

    private void loadResourceType(final ResourceGroupComposite groupComposite, final ViewPath viewPath) {
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
                        updateTabContent(groupComposite);
                        selectTab(getTabName(), getSubTabName(), viewPath);
                    }
                });
        } else {
            updateTabContent(groupComposite);
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
    public void selectTab(String tabName, String subtabName, ViewPath viewPath) {
        currentTabName = tabName;
        currentSubTabName = subtabName;
        super.selectTab(tabName, subtabName, viewPath);
    }

    @Override
    public void renderView(ViewPath viewPath) {
        if ("AutoCluster".equals(viewPath.getCurrent().getPath())) {
            super.renderView(viewPath.next());
        } else {
            // we are traversing to to the Members subtab. Assume this is happening after a save 
            // which means the group type and membership may have changed - get it so we refresh everything.
            if ((null != this.groupId) && this.inventoryTab.getName().equals(currentTabName)
                && this.inventoryMembers.getName().equals(currentSubTabName)) {
                this.currentTabName = null;
                this.currentSubTabName = null;
                this.groupId = null;
            }
            super.renderView(viewPath);
        }
    }

}
