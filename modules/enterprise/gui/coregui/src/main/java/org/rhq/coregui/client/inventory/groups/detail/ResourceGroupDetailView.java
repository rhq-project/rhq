/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.coregui.client.inventory.groups.detail;

import static org.rhq.coregui.client.inventory.resource.detail.ResourceDetailView.Tab;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.Canvas;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.ResourceTypeFacet;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.IconEnum;
import org.rhq.coregui.client.ImageManager;
import org.rhq.coregui.client.UserSessionManager;
import org.rhq.coregui.client.ViewPath;
import org.rhq.coregui.client.alert.GroupAlertHistoryView;
import org.rhq.coregui.client.alert.definitions.GroupAlertDefinitionsView;
import org.rhq.coregui.client.components.tab.SubTab;
import org.rhq.coregui.client.components.tab.TwoLevelTab;
import org.rhq.coregui.client.components.tab.TwoLevelTabSelectedEvent;
import org.rhq.coregui.client.components.view.ViewFactory;
import org.rhq.coregui.client.components.view.ViewName;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.inventory.InventoryView;
import org.rhq.coregui.client.inventory.common.detail.AbstractTwoLevelTabSetView;
import org.rhq.coregui.client.inventory.common.event.EventCompositeHistoryView;
import org.rhq.coregui.client.inventory.groups.detail.configuration.GroupResourceConfigurationEditView;
import org.rhq.coregui.client.inventory.groups.detail.configuration.HistoryGroupResourceConfigurationView;
import org.rhq.coregui.client.inventory.groups.detail.inventory.GroupPluginConfigurationEditView;
import org.rhq.coregui.client.inventory.groups.detail.inventory.HistoryGroupPluginConfigurationView;
import org.rhq.coregui.client.inventory.groups.detail.inventory.MembersView;
import org.rhq.coregui.client.inventory.groups.detail.monitoring.metric.MetricsGroupView;
import org.rhq.coregui.client.inventory.groups.detail.monitoring.schedules.ResourceGroupSchedulesView;
import org.rhq.coregui.client.inventory.groups.detail.monitoring.table.GroupMonitoringTablesView;
import org.rhq.coregui.client.inventory.groups.detail.monitoring.traits.TraitsView;
import org.rhq.coregui.client.inventory.groups.detail.operation.history.GroupOperationHistoryListView;
import org.rhq.coregui.client.inventory.groups.detail.operation.schedule.GroupOperationScheduleListView;
import org.rhq.coregui.client.inventory.groups.detail.summary.ActivityView;
import org.rhq.coregui.client.inventory.resource.detail.monitoring.CalltimeView;
import org.rhq.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.coregui.client.util.BrowserUtility;
import org.rhq.coregui.client.util.Log;
import org.rhq.coregui.client.util.message.Message;

/**
 * The right panel of a Resource Group view (#ResourceGroup/* or #Resource/AutoGroup/*).
 *
 * @author Jay Shaughnessy
 * @author Ian Springer
 */
public class ResourceGroupDetailView extends
    AbstractTwoLevelTabSetView<ResourceGroupComposite, ResourceGroupTitleBar, D3GroupGraphListView> {

    public static final String AUTO_CLUSTER_VIEW = "ResourceGroup/AutoCluster";
    public static final String AUTO_GROUP_VIEW = "Resource/AutoGroup";

    private Integer groupId;
    private ResourceGroupComposite groupComposite;

    public static class GroupTab {
        public static class Inventory {
            public static class SubTab {
                public static final String MEMBERS = "Members";
            }
        }

        public static class Monitoring {
            public static class SubTab {
                public static final String GRAPHS = "Graphs";
            }
        }
    }

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
    private SubTab monitorMetrics;
    private SubTab monitorTraits;
    private SubTab monitorSched;
    private SubTab monitorCallTime;
    private SubTab inventoryMembers;
    private SubTab inventoryConn;
    private SubTab inventoryConnHistory;
    private SubTab operationsHistory;
    private SubTab operationsSchedules;
    private SubTab alertHistory;
    private SubTab alertDef;
    private SubTab configCurrent;
    private SubTab configHistory;
    private SubTab eventHistory;

    private String currentTabName;
    private String currentSubTabName;

    public ResourceGroupDetailView(String baseViewPath) {
        super(baseViewPath, createTitleBar(baseViewPath), createTabs());

        summaryTab = getTabSet().getTabByName(Tab.Summary.NAME);
        summaryActivity = summaryTab.getSubTabByName(Tab.Summary.SubTab.ACTIVITY);
        summaryTimeline = summaryTab.getSubTabByName(Tab.Summary.SubTab.TIMELINE);

        monitoringTab = getTabSet().getTabByName(Tab.Monitoring.NAME);
        monitorCallTime = monitoringTab.getSubTabByName(Tab.Monitoring.SubTab.CALL_TIME);
        monitorGraphs = monitoringTab.getSubTabByName(GroupTab.Monitoring.SubTab.GRAPHS);
        monitorMetrics = monitoringTab.getSubTabByName(Tab.Monitoring.SubTab.METRICS);
        monitorSched = monitoringTab.getSubTabByName(Tab.Monitoring.SubTab.SCHEDULES);
        monitorTraits = monitoringTab.getSubTabByName(Tab.Monitoring.SubTab.TRAITS);

        inventoryTab = getTabSet().getTabByName(Tab.Inventory.NAME);
        inventoryConn = inventoryTab.getSubTabByName(Tab.Inventory.SubTab.CONNECTION_SETTINGS);
        inventoryConnHistory = inventoryTab.getSubTabByName(Tab.Inventory.SubTab.CONNECTION_SETTINGS_HISTORY);
        inventoryMembers = inventoryTab.getSubTabByName(GroupTab.Inventory.SubTab.MEMBERS);

        operationsTab = getTabSet().getTabByName(Tab.Operations.NAME);
        operationsHistory = operationsTab.getSubTabByName(Tab.Operations.SubTab.HISTORY);
        operationsSchedules = operationsTab.getSubTabByName(Tab.Operations.SubTab.SCHEDULES);

        alertsTab = getTabSet().getTabByName(Tab.Alerts.NAME);
        alertDef = alertsTab.getSubTabByName(Tab.Alerts.SubTab.DEFINITIONS);
        alertHistory = alertsTab.getSubTabByName(Tab.Alerts.SubTab.HISTORY);

        configurationTab = getTabSet().getTabByName(Tab.Configuration.NAME);
        configCurrent = configurationTab.getSubTabByName(Tab.Configuration.SubTab.CURRENT);
        configHistory = configurationTab.getSubTabByName(Tab.Configuration.SubTab.HISTORY);

        eventsTab = getTabSet().getTabByName(Tab.Events.NAME);
        eventHistory = eventsTab.getSubTabByName(Tab.Events.SubTab.HISTORY);

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

    private static ResourceGroupTitleBar createTitleBar(String baseViewPath) {
        return new ResourceGroupTitleBar(isAutoGroup(baseViewPath), isAutoCluster(baseViewPath));
    }

    @Override
    protected D3GroupGraphListView createD3GraphListView() {

        graphListView = new D3GroupGraphListView(groupComposite.getResourceGroup(), true);

        // this listener handles the subtab navigation
        addViewRenderedListener(graphListView);

        return graphListView;
    }

    private static TwoLevelTab[] createTabs() {
        List<TwoLevelTab> tabs = new ArrayList<TwoLevelTab>();

        TwoLevelTab summaryTab = new TwoLevelTab(new ViewName(Tab.Summary.NAME, MSG.common_title_summary()), ImageManager.getResourceIcon(
            ResourceCategory.SERVICE, AvailabilityType.UP));

        SubTab summaryActivity = new SubTab(summaryTab, new ViewName(Tab.Summary.SubTab.ACTIVITY, MSG.view_tabs_common_activity()), null);
        SubTab summaryTimeline = new SubTab(summaryTab, new ViewName(Tab.Summary.SubTab.TIMELINE, MSG.view_tabs_common_timeline()), null);
        summaryTab.registerSubTabs(summaryActivity, summaryTimeline);
        tabs.add(summaryTab);

        TwoLevelTab inventoryTab = new TwoLevelTab(new ViewName(Tab.Inventory.NAME, MSG.view_tabs_common_inventory()),
            IconEnum.INVENTORY_SUMMARY);
        SubTab inventoryMembers = new SubTab(inventoryTab, new ViewName(GroupTab.Inventory.SubTab.MEMBERS, MSG.view_tabs_common_members()), null);
        SubTab inventoryConn = new SubTab(inventoryTab, new ViewName(Tab.Inventory.SubTab.CONNECTION_SETTINGS,
            MSG.view_tabs_common_connectionSettings()), null);
        SubTab inventoryConnHistory = new SubTab(inventoryTab, new ViewName(Tab.Inventory.SubTab.CONNECTION_SETTINGS_HISTORY,
            MSG.view_tabs_common_connectionSettingsHistory()), null);
        inventoryTab.registerSubTabs(inventoryMembers, inventoryConn, inventoryConnHistory);
        tabs.add(inventoryTab);

        TwoLevelTab alertsTab = new TwoLevelTab(new ViewName(Tab.Alerts.NAME, MSG.common_title_alerts()), IconEnum.ALERT_DEFINITIONS);
        SubTab alertHistory = new SubTab(alertsTab, new ViewName(Tab.Alerts.SubTab.HISTORY, MSG.view_tabs_common_history()), null);
        SubTab alertDef = new SubTab(alertsTab, new ViewName(Tab.Alerts.SubTab.DEFINITIONS, MSG.common_title_definitions()), null);
        alertsTab.registerSubTabs(alertHistory, alertDef);
        tabs.add(alertsTab);

        TwoLevelTab monitoringTab = new TwoLevelTab(new ViewName(Tab.Monitoring.NAME, MSG.view_tabs_common_monitoring()),
            IconEnum.SUSPECT_METRICS);
        SubTab monitorGraphs = new SubTab(monitoringTab, new ViewName(GroupTab.Monitoring.SubTab.GRAPHS, MSG.view_tabs_common_graphs()), null);
        SubTab monitorMetrics = new SubTab(monitoringTab, new ViewName(Tab.Monitoring.SubTab.METRICS, MSG.view_tabs_common_metrics()), null);
        SubTab monitorTraits = new SubTab(monitoringTab, new ViewName(Tab.Monitoring.SubTab.TRAITS, MSG.view_tabs_common_traits()), null);

        SubTab monitorSched = new SubTab(monitoringTab, new ViewName(Tab.Monitoring.SubTab.SCHEDULES, MSG.view_tabs_common_schedules()), null);
        SubTab monitorCallTime = new SubTab(monitoringTab, new ViewName(Tab.Monitoring.SubTab.CALL_TIME, MSG.view_tabs_common_calltime()), null);
        monitoringTab.registerSubTabs(monitorGraphs, monitorMetrics, monitorTraits, monitorSched,
            monitorCallTime);
        tabs.add(monitoringTab);

        TwoLevelTab eventsTab = new TwoLevelTab(new ViewName(Tab.Events.NAME, MSG.view_tabs_common_events()), IconEnum.EVENTS);
        SubTab eventHistory = new SubTab(eventsTab, new ViewName(Tab.Events.SubTab.HISTORY, MSG.view_tabs_common_history()), null);
        eventsTab.registerSubTabs(eventHistory);
        tabs.add(eventsTab);

        TwoLevelTab operationsTab = new TwoLevelTab(new ViewName(Tab.Operations.NAME, MSG.common_title_operations()),
            IconEnum.RECENT_OPERATIONS);
        SubTab operationsSchedules = new SubTab(operationsTab,
            new ViewName(Tab.Operations.SubTab.SCHEDULES, MSG.view_tabs_common_schedules()), null);
        SubTab operationsHistory = new SubTab(operationsTab, new ViewName(Tab.Operations.SubTab.HISTORY, MSG.view_tabs_common_history()),
            null);
        operationsTab.registerSubTabs(operationsSchedules, operationsHistory);
        tabs.add(operationsTab);

        TwoLevelTab configurationTab = new TwoLevelTab(new ViewName(Tab.Configuration.NAME, MSG.common_title_configuration()),
            IconEnum.CONFIGURATION_HISTORY);
        SubTab configCurrent = new SubTab(configurationTab, new ViewName(Tab.Configuration.SubTab.CURRENT, MSG.view_tabs_common_current()), null);
        SubTab configHistory = new SubTab(configurationTab, new ViewName(Tab.Configuration.SubTab.HISTORY, MSG.view_tabs_common_history()), null);
        configurationTab.registerSubTabs(configCurrent, configHistory);
        tabs.add(configurationTab);

        return tabs.toArray(new TwoLevelTab[tabs.size()]);
    }

    @Override
    protected void updateTabContent(ResourceGroupComposite groupComposite, boolean isRefresh) {
        super.updateTabContent(groupComposite, isRefresh);

        try {
            this.groupComposite = groupComposite;
            getTitleBar().setGroup(groupComposite, isRefresh);

            // wipe the canvas views for the current set of subtabs.
            this.getTabSet().destroyViews();

            GroupCategory groupCategory = groupComposite.getResourceGroup().getGroupCategory();
            Set<ResourceTypeFacet> facets = groupComposite.getResourceFacets().getFacets();

            updateSummaryTab();
            updateInventoryTab(facets);
            updateAlertsTab(groupCategory);
            updateMonitoringTab(facets);
            updateEventsTab(groupCategory, facets);
            updateOperationsTab(groupCategory, facets);
            updateConfigurationTab(groupCategory, facets);

            this.show();
            markForRedraw();
        } catch (Exception e) {
            CoreGUI.getErrorHandler().handleError("Failed to update tab content.", e);
        }
    }

    private void updateSummaryTab() {
        // Summary tab is always visible and enabled.
        updateSubTab(this.summaryTab, this.summaryActivity, true, true, new ViewFactory() {
            @Override
            public Canvas createView() {
                return new ActivityView(groupComposite, isAutoCluster(), isAutoGroup());
            }
        });
    }

    private void updateMonitoringTab(Set<ResourceTypeFacet> facets) {
        ViewFactory viewFactory;
        boolean visible = hasMetricsOfType(this.groupComposite, null);
        if (updateTab(this.monitoringTab, visible, true)) {
            visible = hasMetricsOfType(this.groupComposite, DataType.MEASUREMENT) ||
                hasMetricsOfType(this.groupComposite, DataType.AVAILABILITY);
            boolean showOnPage;

            if(BrowserUtility.isBrowserPreIE9()){
                showOnPage = false;
            }else{
                showOnPage = visible;
            }

            viewFactory = (!showOnPage) ? null : new ViewFactory() {
                @Override
                public Canvas createView() {
                    return MetricsGroupView.create(groupComposite.getResourceGroup());
                }
            };

            updateSubTab(this.monitoringTab, this.monitorGraphs, visible, true, viewFactory);

            // visible = same test as above           
            viewFactory = (!visible) ? null : new ViewFactory() {
                @Override
                public Canvas createView() {
                    //gwt version of group table view.
                    return new GroupMonitoringTablesView(groupComposite);
                }
            };
            updateSubTab(this.monitoringTab, this.monitorMetrics, visible, true, viewFactory);

            visible = hasMetricsOfType(this.groupComposite, DataType.TRAIT);
            viewFactory = (!visible) ? null : new ViewFactory() {
                @Override
                public Canvas createView() {
                    return new TraitsView(groupId);
                }
            };
            updateSubTab(this.monitoringTab, this.monitorTraits, visible, true, viewFactory);

            visible = hasMetricsOfType(this.groupComposite, null);
            viewFactory = (!visible) ? null : new ViewFactory() {
                @Override
                public Canvas createView() {
                    return new ResourceGroupSchedulesView(groupComposite);
                }
            };
            updateSubTab(this.monitoringTab, this.monitorSched, visible, true, viewFactory);

            visible = facets.contains(ResourceTypeFacet.CALL_TIME);
            viewFactory = (!visible) ? null : new ViewFactory() {
                @Override
                public Canvas createView() {
                    return new CalltimeView(EntityContext.forGroup(groupComposite.getResourceGroup()));
                }
            };
            updateSubTab(this.monitoringTab, this.monitorCallTime, visible, true, viewFactory);
        }
    }

    private void updateInventoryTab(Set<ResourceTypeFacet> facets) {
        // Inventory tab is always visible and enabled.
        final boolean canModifyMembers = (!isDynaGroup() && !isAutoGroup() && !isAutoCluster() && globalPermissions
            .contains(Permission.MANAGE_INVENTORY));

        updateSubTab(this.inventoryTab, this.inventoryMembers, true, true, new ViewFactory() {
            @Override
            public Canvas createView() {
                return new MembersView(groupComposite.getResourceGroup().getId(), canModifyMembers);
            }
        });
        updateSubTab(this.inventoryTab, this.inventoryConn, facets.contains(ResourceTypeFacet.PLUGIN_CONFIGURATION),
            true, new ViewFactory() {
                @Override
                public Canvas createView() {
                    return new GroupPluginConfigurationEditView(groupComposite);
                }
            });
        updateSubTab(this.inventoryTab, this.inventoryConnHistory,
            facets.contains(ResourceTypeFacet.PLUGIN_CONFIGURATION), true, new ViewFactory() {
                @Override
                public Canvas createView() {
                    return new HistoryGroupPluginConfigurationView(groupComposite);
                }
            });
    }

    private void updateOperationsTab(GroupCategory groupCategory, Set<ResourceTypeFacet> facets) {
        if (updateTab(this.operationsTab,
            groupCategory == GroupCategory.COMPATIBLE && facets.contains(ResourceTypeFacet.OPERATION), true)) {
            updateSubTab(this.operationsTab, this.operationsSchedules, true, true, new ViewFactory() {
                @Override
                public Canvas createView() {
                    return new GroupOperationScheduleListView(groupComposite);
                }
            });
            updateSubTab(this.operationsTab, this.operationsHistory, true, true, new ViewFactory() {
                @Override
                public Canvas createView() {
                    return new GroupOperationHistoryListView(groupComposite);
                }
            });
        }
    }

    private void updateAlertsTab(GroupCategory groupCategory) {
        // alerts tab is always visible, even for mixed groups
        if (updateTab(this.alertsTab, true, true)) {
            // alert history is always available
            updateSubTab(this.alertsTab, this.alertHistory, true, true, new ViewFactory() {
                @Override
                public Canvas createView() {
                    return GroupAlertHistoryView.get(groupComposite);
                }
            });
            // but alert definitions can only be created on compatible groups
            boolean visible = (groupCategory == GroupCategory.COMPATIBLE);
            ViewFactory viewFactory = (!visible) ? null : new ViewFactory() {
                @Override
                public Canvas createView() {
                    return new GroupAlertDefinitionsView(groupComposite);
                }
            };
            updateSubTab(this.alertsTab, this.alertDef, visible, true, viewFactory);
        }
    }

    private void updateConfigurationTab(GroupCategory groupCategory, Set<ResourceTypeFacet> facets) {
        boolean visible = (groupCategory == GroupCategory.COMPATIBLE && facets
            .contains(ResourceTypeFacet.CONFIGURATION));
        Set<Permission> groupPermissions = this.groupComposite.getResourcePermission().getPermissions();
        if (updateTab(this.configurationTab, visible, visible && groupPermissions.contains(Permission.CONFIGURE_READ))) {
            updateSubTab(this.configurationTab, this.configCurrent, true, true, new ViewFactory() {
                @Override
                public Canvas createView() {
                    return new GroupResourceConfigurationEditView(groupComposite);
                }
            });
            updateSubTab(this.configurationTab, this.configHistory, true, true, new ViewFactory() {
                @Override
                public Canvas createView() {
                    return new HistoryGroupResourceConfigurationView(groupComposite);
                }
            });
        }
    }

    private void updateEventsTab(GroupCategory groupCategory, Set<ResourceTypeFacet> facets) {
        // allow mixed groups to show events from supporting resources
        boolean visible = (groupCategory == GroupCategory.MIXED || (groupCategory == GroupCategory.COMPATIBLE && facets
            .contains(ResourceTypeFacet.EVENT)));
        if (updateTab(this.eventsTab, visible, true)) {
            updateSubTab(this.eventsTab, this.eventHistory, true, true, new ViewFactory() {
                @Override
                public Canvas createView() {
                    return EventCompositeHistoryView.get(groupComposite);
                }
            });
        }
    }


    @Override
    protected ResourceGroupComposite getSelectedItem() {
        return this.groupComposite;
    }

    @Override
    protected void loadSelectedItem(final int groupId, final ViewPath viewPath) {
        this.groupId = groupId;

        ResourceGroupCriteria criteria = new ResourceGroupCriteria();
        criteria.addFilterId(groupId);

        // for autoclusters and autogroups we need to add more criteria
        final boolean isAutoCluster = isAutoCluster();
        final boolean isAutoGroup = isAutoGroup();
        if (isAutoCluster) {
            criteria.addFilterVisible(false);
        } else if (isAutoGroup) {
            criteria.addFilterVisible(false);
            criteria.addFilterPrivate(true);
        }

        GWTServiceLookup.getResourceGroupService().findResourceGroupCompositesByCriteria(criteria,
            new AsyncCallback<PageList<ResourceGroupComposite>>() {
                @Override
                public void onFailure(Throwable caught) {
                    Message message = new Message(MSG.view_group_detail_failLoadComp(String.valueOf(groupId)),
                        Message.Severity.Warning);
                    CoreGUI.goToView(InventoryView.VIEW_ID.getName(), message);
                }

                @Override
                public void onSuccess(PageList<ResourceGroupComposite> result) {
                    if (result.isEmpty()) {
                        //noinspection ThrowableInstanceNeverThrown
                        onFailure(new Exception("Group with id [" + groupId + "] does not exist."));
                    } else {
                        groupComposite = result.get(0);
                        loadResourceType(groupComposite, viewPath);

                        // to avoid multiple queries limit recently visited to standard (visible) groups
                        if (!(isAutoCluster || isAutoGroup)) {
                            UserSessionManager.getUserPreferences().addRecentResourceGroup(groupId,
                                new AsyncCallback<Subject>() {

                                    @Override
                                    public void onFailure(Throwable caught) {
                                        Log.error("Unable to update recently viewed resource groups", caught);
                                    }

                                    @Override
                                    public void onSuccess(Subject result) {
                                        Log.debug("Updated recently viewed resource groups for " + result
                                                + " with resourceGroupId [" + groupId + "]");
                                    }
                                });
                        }
                    }
                }
            });
    }

    private boolean isDynaGroup() {
        return groupComposite.getResourceGroup().getGroupDefinition() != null;
    }

    private boolean isAutoGroup() {
        return isAutoGroup(getBaseViewPath());
    }

    private static boolean isAutoGroup(String baseViewPath) {
        return AUTO_GROUP_VIEW.equals(baseViewPath);
    }

    private boolean isAutoCluster() {
        return isAutoCluster(getBaseViewPath());
    }

    private static boolean isAutoCluster(String baseViewPath) {
        return AUTO_CLUSTER_VIEW.equals(baseViewPath);
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
                        // until we finish the following work we're susceptible to fast-click issues in
                        // tree navigation.  So, wait until after it's done to notify listeners that the view is
                        // safely rendered.  Make sure to notify even on failure.
                        try {
                            group.setResourceType(type);
                            updateTabContent(groupComposite, viewPath.isRefresh());
                            selectTab(getTabName(), getSubTabName(), viewPath);

                        } finally {
                            notifyViewRenderedListeners();
                        }
                    }
                });
        } else {
            try {
                updateTabContent(groupComposite, viewPath.isRefresh());
                selectTab(getTabName(), getSubTabName(), viewPath);

            } finally {
                notifyViewRenderedListeners();
            }
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
            // if we are traversing to the Members subtab assume this is happening after a save,
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
