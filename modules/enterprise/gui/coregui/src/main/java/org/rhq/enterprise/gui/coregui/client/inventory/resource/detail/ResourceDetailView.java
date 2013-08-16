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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.Canvas;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.ResourceTypeFacet;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.resource.composite.ResourcePermission;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.IconEnum;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.UserSessionManager;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.alert.ResourceAlertHistoryView;
import org.rhq.enterprise.gui.coregui.client.alert.definitions.ResourceAlertDefinitionsView;
import org.rhq.enterprise.gui.coregui.client.components.FullHTMLPane;
import org.rhq.enterprise.gui.coregui.client.components.tab.SubTab;
import org.rhq.enterprise.gui.coregui.client.components.tab.TwoLevelTab;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewFactory;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;
import org.rhq.enterprise.gui.coregui.client.drift.ResourceDriftDefinitionsView;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.InventoryView;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.AbstractTwoLevelTabSetView;
import org.rhq.enterprise.gui.coregui.client.inventory.common.event.EventCompositeHistoryView;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.ResourceGroupListView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceCompositeSearchView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.configuration.ResourceConfigurationEditView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.configuration.ResourceConfigurationHistoryListView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.inventory.PluginConfigurationEditView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.inventory.PluginConfigurationHistoryListView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.inventory.ResourceAgentView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.CalltimeView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.D3GraphListView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.schedules.ResourceSchedulesView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.table.MetricsResourceView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.traits.TraitsView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.operation.history.ResourceOperationHistoryListView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.operation.schedule.ResourceOperationScheduleListView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.summary.ActivityView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.summary.TimelineView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.util.BrowserUtility;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

/**
 * Right panel of the Resource view (#Resource/*).
 *
 * @author Ian Springer
 * @author Jay Shaughnessy
 * @author Greg Hinkle
 */
public class ResourceDetailView extends
    AbstractTwoLevelTabSetView<ResourceComposite, ResourceTitleBar, D3GraphListView> {

    private static final String BASE_VIEW_PATH = "Resource";

    public static class Tab {
        public static final String CONFIGURATION = "Configuration";
        public static final String DRIFT = "Drift";
        public static final String OPERATIONS = "Operations";
    }

    public static class ConfigurationSubTab {
        public static final String CURRENT = "Current";
        public static final String HISTORY = "History";
    }

    public static class DriftSubTab {
        public static final String DEFINITIONS = "Definitions";
    }

    public static class OperationsSubTab {
        public static final String SCHEDULES = "Schedules";
        public static final String HISTORY = "History";
    }

    private Integer resourceId;

    private ResourceComposite resourceComposite;

    private MetricsResourceView metricsResourceView;

    //private List<ResourceSelectListener> selectListeners = new ArrayList<ResourceSelectListener>();

    private TwoLevelTab summaryTab;
    private TwoLevelTab monitoringTab;
    private TwoLevelTab inventoryTab;
    private TwoLevelTab operationsTab;
    private TwoLevelTab alertsTab;
    private TwoLevelTab configurationTab;
    private TwoLevelTab eventsTab;
    private TwoLevelTab driftTab;
    private TwoLevelTab contentTab;

    private SubTab summaryActivity;
    private SubTab summaryTimeline;
    private SubTab monitorMetrics;
    private SubTab monitorTraits;
    private SubTab monitorSched;
    private SubTab monitorCallTime;
    private SubTab inventoryChildren;
    private SubTab inventoryChildHistory;
    private SubTab inventoryConn;
    private SubTab inventoryConnHistory;
    private SubTab inventoryGroups;
    private SubTab inventoryAgent;
    private SubTab operationsHistory;
    private SubTab operationsSchedules;
    private SubTab alertHistory;
    private SubTab alertDef;
    private SubTab configCurrent;
    private SubTab configHistory;
    private SubTab eventHistory;
    private SubTab driftDefinitions;
    private SubTab contentDeployed;
    private SubTab contentNew;
    private SubTab contentSubscrip;
    private SubTab contentHistory;

    public ResourceDetailView() {
        super(BASE_VIEW_PATH);

        // hide until we have our tabs in place
        this.hide();
    }

    @Override
    protected List<TwoLevelTab> createTabs() {
        List<TwoLevelTab> tabs = new ArrayList<TwoLevelTab>();

        summaryTab = new TwoLevelTab(new ViewName("Summary", MSG.common_title_summary()), ImageManager.getResourceIcon(
            ResourceCategory.SERVICE, AvailabilityType.UP));
        summaryActivity = new SubTab(summaryTab, new ViewName("Activity", MSG.view_tabs_common_activity()), null);
        summaryTimeline = new SubTab(summaryTab, new ViewName("Timeline", MSG.view_tabs_common_timeline()), null);
        summaryTab.registerSubTabs(summaryActivity, summaryTimeline);
        tabs.add(summaryTab);

        inventoryTab = new TwoLevelTab(new ViewName("Inventory", MSG.view_tabs_common_inventory()),
            IconEnum.INVENTORY_SUMMARY);
        inventoryChildren = new SubTab(inventoryTab, new ViewName("Children", MSG.view_tabs_common_child_resources()),
            null);
        inventoryChildHistory = new SubTab(inventoryTab, new ViewName("ChildHistory",
            MSG.view_tabs_common_child_history()), null);
        inventoryConn = new SubTab(inventoryTab, new ViewName("ConnectionSettings",
            MSG.view_tabs_common_connectionSettings()), null);
        inventoryConnHistory = new SubTab(inventoryTab, PluginConfigurationHistoryListView.VIEW_ID, null);
        inventoryGroups = new SubTab(inventoryTab, new ViewName("Groups", MSG.view_tabs_common_groups()), null);
        inventoryAgent = new SubTab(inventoryTab, new ViewName("Agent", MSG.view_tabs_common_agent()), null);
        inventoryTab.registerSubTabs(this.inventoryChildren, this.inventoryChildHistory, this.inventoryConn,
            this.inventoryConnHistory, this.inventoryGroups, this.inventoryAgent);
        tabs.add(inventoryTab);

        alertsTab = new TwoLevelTab(new ViewName("Alerts", MSG.common_title_alerts()), IconEnum.ALERT_DEFINITIONS);
        this.alertHistory = new SubTab(alertsTab, new ViewName("History", MSG.view_tabs_common_history()), null);
        this.alertDef = new SubTab(alertsTab, new ViewName("Definitions", MSG.common_title_definitions()), null);
        alertsTab.registerSubTabs(alertHistory, alertDef);
        tabs.add(alertsTab);

        monitoringTab = new TwoLevelTab(new ViewName("Monitoring", MSG.view_tabs_common_monitoring()),
            IconEnum.SUSPECT_METRICS);

        monitorMetrics = new SubTab(monitoringTab, new ViewName("Metrics", MSG.view_tabs_common_metrics()), null);
        monitorTraits = new SubTab(monitoringTab, new ViewName("Traits", MSG.view_tabs_common_traits()), null);
        monitorSched = new SubTab(monitoringTab, new ViewName("Schedules", MSG.view_tabs_common_schedules()), null);
        monitorCallTime = new SubTab(monitoringTab, new ViewName("CallTime", MSG.view_tabs_common_calltime()), null);
        monitoringTab.registerSubTabs( monitorMetrics, monitorTraits,  monitorSched, monitorCallTime);
        tabs.add(monitoringTab);

        eventsTab = new TwoLevelTab(new ViewName("Events", MSG.view_tabs_common_events()), IconEnum.EVENTS);
        this.eventHistory = new SubTab(eventsTab, new ViewName("History", MSG.view_tabs_common_history()), null);
        eventsTab.registerSubTabs(eventHistory);
        tabs.add(eventsTab);

        operationsTab = new TwoLevelTab(new ViewName(Tab.OPERATIONS, MSG.common_title_operations()),
            IconEnum.RECENT_OPERATIONS);
        this.operationsSchedules = new SubTab(operationsTab, new ViewName(OperationsSubTab.SCHEDULES,
            MSG.view_tabs_common_schedules()), null);
        this.operationsHistory = new SubTab(operationsTab, new ViewName(OperationsSubTab.HISTORY,
            MSG.view_tabs_common_history()), null);
        operationsTab.registerSubTabs(this.operationsSchedules, this.operationsHistory);
        tabs.add(operationsTab);

        configurationTab = new TwoLevelTab(new ViewName(Tab.CONFIGURATION, MSG.common_title_configuration()),
            IconEnum.CONFIGURATION_HISTORY);
        this.configCurrent = new SubTab(configurationTab, new ViewName(ConfigurationSubTab.CURRENT,
            MSG.view_tabs_common_current()), null);
        this.configHistory = new SubTab(configurationTab, new ViewName(ConfigurationSubTab.HISTORY,
            MSG.view_tabs_common_history()), null);
        configurationTab.registerSubTabs(this.configCurrent, this.configHistory);
        tabs.add(configurationTab);

        driftTab = new TwoLevelTab(new ViewName(Tab.DRIFT, MSG.view_tabs_common_drift()), IconEnum.DRIFT_COMPLIANCE);
        this.driftDefinitions = new SubTab(driftTab, new ViewName(DriftSubTab.DEFINITIONS,
            MSG.common_title_definitions()), null);
        driftTab.registerSubTabs(driftDefinitions);
        tabs.add(driftTab);

        contentTab = new TwoLevelTab(new ViewName("Content", MSG.view_tabs_common_content()), IconEnum.CONTENT);
        this.contentDeployed = new SubTab(contentTab, new ViewName("Deployed", MSG.view_tabs_common_deployed()), null);
        this.contentNew = new SubTab(contentTab, new ViewName("New", MSG.common_button_new()), null);
        this.contentSubscrip = new SubTab(contentTab, new ViewName("Subscriptions",
            MSG.view_tabs_common_subscriptions()), null);
        this.contentHistory = new SubTab(contentTab, new ViewName("History", MSG.view_tabs_common_history()), null);
        contentTab.registerSubTabs(contentDeployed, contentNew, contentSubscrip, contentHistory);
        tabs.add(contentTab);

        return tabs;
    }

    @Override
    protected ResourceTitleBar createTitleBar() {
        return new ResourceTitleBar();
    }

    @Override
    protected D3GraphListView createD3GraphListView() {
        graphListView = D3GraphListView.createSummaryMultipleGraphs(resourceComposite.getResource(), true);
        return graphListView;
    }



    @Override
    protected void updateTabContent(ResourceComposite resourceComposite, boolean isRefresh) {
        super.updateTabContent(resourceComposite, isRefresh);

        try {
            this.resourceComposite = resourceComposite;
            //            for (ResourceSelectListener selectListener : this.selectListeners) {
            //                selectListener.onResourceSelected(this.resourceComposite);
            //            }
            Resource resource = this.resourceComposite.getResource();
            getTitleBar().setResource(this.resourceComposite, isRefresh);

            // wipe the canvas views for the current set of subtabs.
            this.getTabSet().destroyViews();

            ResourcePermission resourcePermissions = this.resourceComposite.getResourcePermission();
            Set<ResourceTypeFacet> facets = this.resourceComposite.getResourceFacets().getFacets();

            updateSummaryTabContent();
            updateInventoryTabContent(resourceComposite, resource, facets);
            updateAlertsTabContent(resourceComposite);
            updateMonitoringTabContent(resource, facets);
            updateEventsTabContent(resourceComposite, facets);
            updateOperationsTabContent(facets);
            updateConfigurationTabContent(resourceComposite, resource, resourcePermissions, facets);
            updateDriftTabContent(resourceComposite, facets);
            updateContentTabContent(resource, facets);

            this.show();
            markForRedraw();
        } catch (Exception e) {
            CoreGUI.getErrorHandler().handleError("Failed to update tab content.", e);
        }
    }

    private void updateSummaryTabContent() {
        updateSubTab(this.summaryTab, this.summaryActivity, true, true, new ViewFactory() {
            @Override
            public Canvas createView() {
                return new ActivityView(resourceComposite);
            }
        });

        updateSubTab(this.summaryTab, this.summaryTimeline, true, true, new ViewFactory() {
            @Override
            public Canvas createView() {
                return new TimelineView(resourceComposite);
            }
        });
    }

    // Inventory Tab (always enabled and visible)
    private void updateInventoryTabContent(final ResourceComposite resourceComposite, final Resource resource,
        Set<ResourceTypeFacet> facets) {

        ResourceType type = this.resourceComposite.getResource().getResourceType();
        boolean visible = !type.getChildResourceTypes().isEmpty();
        ViewFactory viewFactory = (!visible) ? null : new ViewFactory() {
            @Override
            public Canvas createView() {
                return ResourceCompositeSearchView.getChildrenOf(resourceComposite);
            }
        };
        updateSubTab(this.inventoryTab, this.inventoryChildren, visible, true, viewFactory);

        visible = !type.getChildResourceTypes().isEmpty();
        viewFactory = (!visible) ? null : new ViewFactory() {
            @Override
            public Canvas createView() {
                return new ChildHistoryView(resourceComposite);
            }
        };
        updateSubTab(this.inventoryTab, this.inventoryChildHistory, visible, true, viewFactory);

        visible = facets.contains(ResourceTypeFacet.PLUGIN_CONFIGURATION);
        viewFactory = (!visible) ? null : new ViewFactory() {
            @Override
            public Canvas createView() {
                return new PluginConfigurationEditView(resourceComposite);
            }
        };
        updateSubTab(this.inventoryTab, this.inventoryConn, visible, true, viewFactory);

        // same test, use above setting for 'visible'
        viewFactory = (!visible) ? null : new ViewFactory() {
            @Override
            public Canvas createView() {
                return new PluginConfigurationHistoryListView(resourceComposite.getResourcePermission().isInventory(),
                    resourceComposite.getResource().getId());
            }
        };
        updateSubTab(this.inventoryTab, this.inventoryConnHistory, visible, true, viewFactory);

        final boolean canModifyMembership = globalPermissions.contains(Permission.MANAGE_INVENTORY);
        updateSubTab(this.inventoryTab, this.inventoryGroups, true, true, new ViewFactory() {
            @Override
            public Canvas createView() {
                return ResourceGroupListView.getGroupsOf(resource.getId(), canModifyMembership);
            }
        });

        boolean enabled = globalPermissions.contains(Permission.MANAGE_SETTINGS);
        viewFactory = (!enabled) ? null : new ViewFactory() {
            @Override
            public Canvas createView() {
                return new ResourceAgentView(resourceId);
            }
        };
        updateSubTab(this.inventoryTab, this.inventoryAgent, true, enabled, viewFactory);
    }

    private void updateAlertsTabContent(final ResourceComposite resourceComposite) {
        updateSubTab(this.alertsTab, this.alertHistory, true, true, new ViewFactory() {
            @Override
            public Canvas createView() {
                return ResourceAlertHistoryView.get(resourceComposite);
            }
        });

        updateSubTab(this.alertsTab, this.alertDef, true, true, new ViewFactory() {
            @Override
            public Canvas createView() {
                return new ResourceAlertDefinitionsView(resourceComposite);
            }
        });
    }

    private void updateMonitoringTabContent(final Resource resource, Set<ResourceTypeFacet> facets) {
        boolean visible = hasMetricsOfType(this.resourceComposite, DataType.MEASUREMENT);
        ViewFactory viewFactory;

        boolean visibleToIE8 = !BrowserUtility.isBrowserPreIE9();

        // visible = same test as above
        viewFactory = (!visible) ? null : new ViewFactory() {
            @Override
            public Canvas createView() {
                if(null == metricsResourceView){
                    metricsResourceView = new MetricsResourceView(resource);
                }
                return  metricsResourceView;
            }
        };
        updateSubTab(this.monitoringTab, this.monitorMetrics, visible, visibleToIE8, viewFactory);

        visible = hasMetricsOfType(this.resourceComposite, DataType.TRAIT);
        viewFactory = (!visible) ? null : new ViewFactory() {
            @Override
            public Canvas createView() {
                return new TraitsView(resource.getId());
            }
        };
        updateSubTab(this.monitoringTab, this.monitorTraits, visible, true, viewFactory);

        updateSubTab(this.monitoringTab, this.monitorSched, hasMetricsOfType(this.resourceComposite, null), true,
            new ViewFactory() {
                @Override
                public Canvas createView() {
                    return new ResourceSchedulesView(resourceComposite);
                }
            });

        visible = facets.contains(ResourceTypeFacet.CALL_TIME);
        viewFactory = (!visible) ? null : new ViewFactory() {
            @Override
            public Canvas createView() {
                return new CalltimeView(EntityContext.forResource(resource.getId()));
            }
        };
        updateSubTab(this.monitoringTab, this.monitorCallTime, visible, true, viewFactory);
    }

    private void updateEventsTabContent(final ResourceComposite resourceComposite, Set<ResourceTypeFacet> facets) {
        if (updateTab(this.eventsTab, facets.contains(ResourceTypeFacet.EVENT), true)) {

            updateSubTab(this.eventsTab, this.eventHistory, true, true, new ViewFactory() {
                @Override
                public Canvas createView() {
                    return EventCompositeHistoryView.get(resourceComposite);
                }
            });
        }
    }

    private void updateOperationsTabContent(Set<ResourceTypeFacet> facets) {
        if (updateTab(this.operationsTab, facets.contains(ResourceTypeFacet.OPERATION), true)) {
            // comment out GWT-based operation history until...
            //     1) user can delete history if they possess the appropriate permissions
            //     2) user can see both operation arguments and results in the history details pop-up
            //     3) operation arguments/results become read-only configuration data in the history details pop-up
            //     4) user can navigate to the group operation that spawned this resource operation history, if appropriate
            // note: enabled operation execution/schedules from left-nav, if it doesn't already exist

            updateSubTab(this.operationsTab, this.operationsSchedules, true, true, new ViewFactory() {
                @Override
                public Canvas createView() {
                    return new ResourceOperationScheduleListView(resourceComposite);
                }
            });

            updateSubTab(this.operationsTab, this.operationsHistory, true, true, new ViewFactory() {
                @Override
                public Canvas createView() {
                    return new ResourceOperationHistoryListView(resourceComposite);
                }
            });
        }
    }

    private void updateConfigurationTabContent(final ResourceComposite resourceComposite, final Resource resource,
        ResourcePermission resourcePermissions, Set<ResourceTypeFacet> facets) {
        if (updateTab(this.configurationTab, facets.contains(ResourceTypeFacet.CONFIGURATION),
            resourcePermissions.isConfigureRead())) {

            updateSubTab(this.configurationTab, this.configCurrent, true, true, new ViewFactory() {
                @Override
                public Canvas createView() {
                    return new ResourceConfigurationEditView(resourceComposite);
                }
            });

            updateSubTab(this.configurationTab, this.configHistory, true, true, new ViewFactory() {
                @Override
                public Canvas createView() {
                    return new ResourceConfigurationHistoryListView(resourceComposite.getResourcePermission()
                        .isConfigureWrite(), resource.getId());
                }
            });
        }
    }

    private void updateDriftTabContent(final ResourceComposite resourceComposite, Set<ResourceTypeFacet> facets) {
        if (updateTab(this.driftTab, facets.contains(ResourceTypeFacet.DRIFT), true)) {

            updateSubTab(this.driftTab, this.driftDefinitions, true, true, new ViewFactory() {
                @Override
                public Canvas createView() {
                    return ResourceDriftDefinitionsView.get(resourceComposite);
                }
            });
        }
    }

    private void updateContentTabContent(final Resource resource, Set<ResourceTypeFacet> facets) {
        if (updateTab(this.contentTab, facets.contains(ResourceTypeFacet.CONTENT), true)) {

            updateSubTab(this.contentTab, this.contentDeployed, true, true, new ViewFactory() {
                @Override
                public Canvas createView() {
                    return new FullHTMLPane("/rhq/resource/content/view-plain.xhtml?id=" + resource.getId());
                }
            });

            updateSubTab(this.contentTab, this.contentNew, true, true, new ViewFactory() {
                @Override
                public Canvas createView() {
                    return new FullHTMLPane("/rhq/resource/content/deploy-plain.xhtml?id=" + resource.getId());
                }
            });

            updateSubTab(this.contentTab, this.contentSubscrip, true, true, new ViewFactory() {
                @Override
                public Canvas createView() {
                    return new FullHTMLPane("/rhq/resource/content/subscription-plain.xhtml?id=" + resource.getId());
                }
            });

            updateSubTab(this.contentTab, this.contentHistory, true, true, new ViewFactory() {
                @Override
                public Canvas createView() {
                    return new FullHTMLPane("/rhq/resource/content/history-plain.xhtml?id=" + resource.getId());
                }
            });
        }
    }

    public Integer getSelectedItemId() {
        return this.resourceId;
    }

    @Override
    protected ResourceComposite getSelectedItem() {
        return this.resourceComposite;
    }

    protected void loadSelectedItem(final int resourceId, final ViewPath viewPath) {
        this.resourceId = resourceId;

        ResourceCriteria criteria = new ResourceCriteria();
        criteria.addFilterId(resourceId);
        criteria.fetchTags(true);
        GWTServiceLookup.getResourceService().findResourceCompositesByCriteria(criteria,
            new AsyncCallback<PageList<ResourceComposite>>() {
                @Override
                public void onFailure(Throwable caught) {
                    Message message = new Message(MSG.view_inventory_resource_loadFailed(String.valueOf(resourceId)),
                        Message.Severity.Warning);
                    CoreGUI.goToView(InventoryView.VIEW_ID.getName(), message);
                }

                @Override
                public void onSuccess(PageList<ResourceComposite> result) {
                    if (result.isEmpty()) {
                        //noinspection ThrowableInstanceNeverThrown
                        onFailure(new Exception("Resource with id [" + resourceId + "] does not exist."));
                    } else {
                        final ResourceComposite resourceComposite = result.get(0);
                        loadResourceType(resourceComposite, viewPath);

                        // add this resouce to the user's recently visited list
                        UserSessionManager.getUserPreferences().addRecentResource(resourceId,
                            new AsyncCallback<Subject>() {

                                @Override
                                public void onFailure(Throwable caught) {
                                    Log.error("Unable to update recently viewed resources", caught);
                                }

                                @Override
                                public void onSuccess(Subject result) {
                                    Log.debug("Updated recently viewed resources for " + result + " with resourceId ["
                                        + resourceId + "]");
                                }
                            });
                    }
                }
            });
    }

    private void loadResourceType(final ResourceComposite resourceComposite, final ViewPath viewPath) {
        final Resource resource = resourceComposite.getResource();
        ResourceTypeRepository.Cache.getInstance().getResourceTypes(
            resource.getResourceType().getId(),
            EnumSet.of(ResourceTypeRepository.MetadataType.children, ResourceTypeRepository.MetadataType.content,
                ResourceTypeRepository.MetadataType.operations, ResourceTypeRepository.MetadataType.measurements,
                ResourceTypeRepository.MetadataType.events,
                ResourceTypeRepository.MetadataType.resourceConfigurationDefinition),
            new ResourceTypeRepository.TypeLoadedCallback() {
                public void onTypesLoaded(ResourceType type) {
                    // until we finish the following work we're susceptible to fast-click issues in
                    // tree navigation.  So, wait until after it's done to notify listeners that the view is
                    // safely rendered.  Make sure to notify even on failure.
                    try {
                        resourceComposite.getResource().setResourceType(type);
                        updateTabContent(resourceComposite, viewPath.isRefresh());
                        selectTab(getTabName(), getSubTabName(), viewPath);
                    } finally {
                        notifyViewRenderedListeners();
                    }
                }
            });
    }

    private static boolean hasMetricsOfType(ResourceComposite resourceComposite, DataType dataType) {
        ResourceType type = resourceComposite.getResource().getResourceType();
        Set<MeasurementDefinition> metricDefs = type.getMetricDefinitions();
        for (MeasurementDefinition metricDef : metricDefs) {
            if (dataType == null || metricDef.getDataType() == dataType) {
                return true;
            }
        }
        return false;
    }
}
