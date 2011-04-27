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

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.Canvas;

import org.rhq.core.domain.auth.Subject;
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
import org.rhq.enterprise.gui.coregui.client.UserSessionManager;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.alert.GroupAlertHistoryView;
import org.rhq.enterprise.gui.coregui.client.alert.definitions.GroupAlertDefinitionsView;
import org.rhq.enterprise.gui.coregui.client.components.FullHTMLPane;
import org.rhq.enterprise.gui.coregui.client.components.tab.SubTab;
import org.rhq.enterprise.gui.coregui.client.components.tab.TwoLevelTab;
import org.rhq.enterprise.gui.coregui.client.components.tab.TwoLevelTabSelectedEvent;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewFactory;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.AbstractTwoLevelTabSetView;
import org.rhq.enterprise.gui.coregui.client.inventory.common.event.EventCompositeHistoryView;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.configuration.GroupResourceConfigurationEditView;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.configuration.HistoryGroupResourceConfigurationView;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.inventory.GroupPluginConfigurationEditView;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.inventory.HistoryGroupPluginConfigurationView;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.inventory.MembersView;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.schedules.SchedulesView;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.table.GroupMeasurementTableView;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.table.GroupMembersHealthView;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.traits.TraitsView;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.operation.history.GroupOperationHistoryListView;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.operation.schedule.GroupOperationScheduleListView;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.summary.ActivityView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * Right panel of the Resource Group view (#ResourceGroup/*).
 *
 * @author Jay Shaughnessy
 * @author Ian Springer
 */
public class ResourceGroupDetailView extends AbstractTwoLevelTabSetView<ResourceGroupComposite, ResourceGroupTitleBar> {
    public static final String AUTO_CLUSTER_VIEW = "ResourceGroup/AutoCluster";
    public static final String AUTO_GROUP_VIEW = "Resource/AutoGroup";

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
    private SubTab operationsSchedules;
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
        this.operationsSchedules = new SubTab(operationsTab.extendLocatorId("Schedules"), new ViewName("Schedules", MSG
            .view_tabs_common_schedules()), null);
        this.operationsHistory = new SubTab(operationsTab.extendLocatorId("History"), new ViewName("History", MSG
            .view_tabs_common_history()), null);
        operationsTab.registerSubTabs(this.operationsSchedules, this.operationsHistory);
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

        // wipe the canvas views for the current set of subtabs.
        this.getTabSet().destroyViews();

        GroupCategory groupCategory = groupComposite.getResourceGroup().getGroupCategory();
        Set<ResourceTypeFacet> facets = groupComposite.getResourceFacets().getFacets();

        updateSummaryTab();
        updateMonitoringTab(groupId, groupCategory, facets);
        updateInventoryTab(groupId, facets);
        updateOperationsTab(groupCategory, facets);
        updateAlertsTab(groupComposite, groupCategory);
        updateConfigurationTab(groupId, groupCategory, facets);
        updateEventsTab(groupComposite, groupCategory, facets);

        this.show();
        markForRedraw();
    }

    private void updateSummaryTab() {
        // Summary tab is always visible and enabled.
        updateSubTab(this.summaryTab, this.summaryActivity, true, true, new ViewFactory() {
            @Override
            public Canvas createView() {
                return new ActivityView(summaryActivity.extendLocatorId("View"), groupComposite);
            }
        });
        // TODO (ips): Add Timeline subtab?
    }

    private void updateMonitoringTab(final int groupId, GroupCategory groupCategory, Set<ResourceTypeFacet> facets) {
        boolean visible;
        ViewFactory viewFactory;
        if (updateTab(this.monitoringTab, groupCategory == GroupCategory.COMPATIBLE, true)) {
            visible = hasMetricsOfType(this.groupComposite, DataType.MEASUREMENT);
            viewFactory = (!visible) ? null : new ViewFactory() {
                @Override
                public Canvas createView() {
                    return new FullHTMLPane(monitorGraphs.extendLocatorId("View"),
                        "/rhq/group/monitor/graphs-plain.xhtml?groupId=" + groupId);
                }
            };
            updateSubTab(this.monitoringTab, this.monitorGraphs, visible, true, viewFactory);

            // visible = same test as above           
            viewFactory = (!visible) ? null : new ViewFactory() {
                @Override
                public Canvas createView() {
                    //                    return new FullHTMLPane(monitorTables.extendLocatorId("View"),
                    //                        "/rhq/group/monitor/tables-plain.xhtml?groupId=" + groupId);
                    //gwt version of group table view.
                    LocatableVLayout groupTableView = new LocatableVLayout(monitorTables
                        .extendLocatorId("monitorTable"));
                    GroupMeasurementTableView metrics = new GroupMeasurementTableView(monitorTables
                        .extendLocatorId("ViewMetrics"), groupComposite, groupId);
                    GroupMembersHealthView memberHealth = new GroupMembersHealthView(monitorTables
                        .extendLocatorId("ViewHealth"), groupId, false);
                    groupTableView.addMember(metrics);
                    groupTableView.addMember(memberHealth);
                    return groupTableView;
                }
            };
            updateSubTab(this.monitoringTab, this.monitorTables, visible, true, viewFactory);

            visible = hasMetricsOfType(this.groupComposite, DataType.TRAIT);
            viewFactory = (!visible) ? null : new ViewFactory() {
                @Override
                public Canvas createView() {
                    return new TraitsView(monitorTraits.extendLocatorId("View"), groupId);
                }
            };
            updateSubTab(this.monitoringTab, this.monitorTraits, visible, true, viewFactory);

            visible = hasMetricsOfType(this.groupComposite, null);
            viewFactory = (!visible) ? null : new ViewFactory() {
                @Override
                public Canvas createView() {
                    return new SchedulesView(monitorSched.extendLocatorId("View"), groupComposite);
                }
            };
            updateSubTab(this.monitoringTab, this.monitorSched, visible, true, viewFactory);

            visible = facets.contains(ResourceTypeFacet.CALL_TIME);
            viewFactory = (!visible) ? null : new ViewFactory() {
                @Override
                public Canvas createView() {
                    return new FullHTMLPane(monitorCallTime.extendLocatorId("View"),
                        "/rhq/group/monitor/response-plain.xhtml?groupId=" + groupId);
                }
            };
            updateSubTab(this.monitoringTab, this.monitorCallTime, visible, true, viewFactory);
            // TODO (ips): Add Availability subtab.
        }
    }

    private void updateInventoryTab(final int groupId, Set<ResourceTypeFacet> facets) {
        // Inventory tab is always visible and enabled.
        final boolean canModifyMembers = (!isAutoGroup() && !isAutoCluster() && globalPermissions
            .contains(Permission.MANAGE_INVENTORY));
        updateSubTab(this.inventoryTab, this.inventoryMembers, true, true, new ViewFactory() {
            @Override
            public Canvas createView() {
                return new MembersView(inventoryMembers.extendLocatorId("View"), groupId, canModifyMembers);
            }
        });
        updateSubTab(this.inventoryTab, this.inventoryConn, facets.contains(ResourceTypeFacet.PLUGIN_CONFIGURATION),
            true, new ViewFactory() {
                @Override
                public Canvas createView() {
                    return new GroupPluginConfigurationEditView(inventoryConn.extendLocatorId("View"), groupComposite);
                }
            });
        updateSubTab(this.inventoryTab, this.inventoryConnHistory, facets
            .contains(ResourceTypeFacet.PLUGIN_CONFIGURATION), true, new ViewFactory() {
            @Override
            public Canvas createView() {
                return new HistoryGroupPluginConfigurationView(inventoryConnHistory.extendLocatorId("View"),
                    groupComposite);
            }
        });
    }

    private void updateOperationsTab(GroupCategory groupCategory, Set<ResourceTypeFacet> facets) {
        if (updateTab(this.operationsTab, groupCategory == GroupCategory.COMPATIBLE
            && facets.contains(ResourceTypeFacet.OPERATION), true)) {
            updateSubTab(this.operationsTab, this.operationsSchedules, true, true, new ViewFactory() {
                @Override
                public Canvas createView() {
                    return new GroupOperationScheduleListView(operationsSchedules.extendLocatorId("View"),
                        groupComposite);
                }
            });
            updateSubTab(this.operationsTab, this.operationsHistory, true, true, new ViewFactory() {
                @Override
                public Canvas createView() {
                    return new GroupOperationHistoryListView(operationsHistory.extendLocatorId("View"), groupComposite);
                }
            });
        }
    }

    private void updateAlertsTab(final ResourceGroupComposite groupComposite, GroupCategory groupCategory) {
        // alerts tab is always visible, even for mixed groups
        if (updateTab(this.alertsTab, true, true)) {
            // alert history is always available
            updateSubTab(this.alertsTab, this.alertHistory, true, true, new ViewFactory() {
                @Override
                public Canvas createView() {
                    return GroupAlertHistoryView.get(alertHistory.extendLocatorId("View"), groupComposite);
                }
            });
            // but alert definitions can only be created on compatible groups
            boolean visible = (groupCategory == GroupCategory.COMPATIBLE);
            ViewFactory viewFactory = (!visible) ? null : new ViewFactory() {
                @Override
                public Canvas createView() {
                    return new GroupAlertDefinitionsView(alertDef.extendLocatorId("View"), groupComposite);
                }
            };
            updateSubTab(this.alertsTab, this.alertDef, visible, true, viewFactory);
        }
    }

    private void updateConfigurationTab(final int groupId, GroupCategory groupCategory, Set<ResourceTypeFacet> facets) {
        boolean visible = (groupCategory == GroupCategory.COMPATIBLE && facets
            .contains(ResourceTypeFacet.CONFIGURATION));
        Set<Permission> groupPermissions = this.groupComposite.getResourcePermission().getPermissions();
        if (updateTab(this.configurationTab, visible, visible && groupPermissions.contains(Permission.CONFIGURE_READ))) {
            updateSubTab(this.configurationTab, this.configCurrent, true, true, new ViewFactory() {
                @Override
                public Canvas createView() {
                    return new GroupResourceConfigurationEditView(configCurrent.extendLocatorId("View"), groupComposite);
                }
            });
            updateSubTab(this.configurationTab, this.configHistory, true, true, new ViewFactory() {
                @Override
                public Canvas createView() {
                    return new HistoryGroupResourceConfigurationView(inventoryConnHistory.extendLocatorId("View"),
                        groupComposite);
                }
            });
        }
    }

    private void updateEventsTab(final ResourceGroupComposite groupComposite, GroupCategory groupCategory,
        Set<ResourceTypeFacet> facets) {
        // allow mixed groups to show events from supporting resources
        boolean visible = (groupCategory == GroupCategory.MIXED || (groupCategory == GroupCategory.COMPATIBLE && facets
            .contains(ResourceTypeFacet.EVENT)));
        if (updateTab(this.eventsTab, visible, true)) {
            updateSubTab(this.eventsTab, this.eventHistory, true, true, new ViewFactory() {
                @Override
                public Canvas createView() {
                    return EventCompositeHistoryView.get(eventHistory.extendLocatorId("View"), groupComposite);
                }
            });
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

                        // to avoid multiple queries limit recently visited to standard (visible) groups
                        if (!(isAutoCluster || isAutoGroup)) {
                            UserSessionManager.getUserPreferences().addRecentResourceGroup(groupId,
                                new AsyncCallback<Subject>() {

                                    public void onFailure(Throwable caught) {
                                        Log.error("Unable to update recently viewed resource groups", caught);
                                    }

                                    public void onSuccess(Subject result) {
                                        if (Log.isDebugEnabled()) {
                                            Log.debug("Updated recently viewed resource groups for " + result
                                                + " with resourceGroupId [" + groupId + "]");
                                        }
                                    }
                                });
                        }
                    }
                }
            });
    }

    private boolean isAutoGroup() {
        return AUTO_GROUP_VIEW.equals(getBaseViewPath());
    }

    private boolean isAutoCluster() {
        return AUTO_CLUSTER_VIEW.equals(getBaseViewPath());
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
