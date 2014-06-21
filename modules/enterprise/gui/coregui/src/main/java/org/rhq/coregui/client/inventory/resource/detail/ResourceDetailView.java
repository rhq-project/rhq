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
package org.rhq.coregui.client.inventory.resource.detail;

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
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.IconEnum;
import org.rhq.coregui.client.ImageManager;
import org.rhq.coregui.client.UserSessionManager;
import org.rhq.coregui.client.ViewPath;
import org.rhq.coregui.client.alert.ResourceAlertHistoryView;
import org.rhq.coregui.client.alert.definitions.ResourceAlertDefinitionsView;
import org.rhq.coregui.client.components.FullHTMLPane;
import org.rhq.coregui.client.components.tab.SubTab;
import org.rhq.coregui.client.components.tab.TwoLevelTab;
import org.rhq.coregui.client.components.view.ViewFactory;
import org.rhq.coregui.client.components.view.ViewName;
import org.rhq.coregui.client.drift.ResourceDriftDefinitionsView;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.inventory.InventoryView;
import org.rhq.coregui.client.inventory.common.detail.AbstractTwoLevelTabSetView;
import org.rhq.coregui.client.inventory.common.event.EventCompositeHistoryView;
import org.rhq.coregui.client.inventory.groups.ResourceGroupListView;
import org.rhq.coregui.client.inventory.resource.ResourceCompositeSearchView;
import org.rhq.coregui.client.inventory.resource.detail.configuration.ResourceConfigurationEditView;
import org.rhq.coregui.client.inventory.resource.detail.configuration.ResourceConfigurationHistoryListView;
import org.rhq.coregui.client.inventory.resource.detail.inventory.PluginConfigurationEditView;
import org.rhq.coregui.client.inventory.resource.detail.inventory.PluginConfigurationHistoryListView;
import org.rhq.coregui.client.inventory.resource.detail.inventory.ResourceAgentView;
import org.rhq.coregui.client.inventory.resource.detail.monitoring.CalltimeView;
import org.rhq.coregui.client.inventory.resource.detail.monitoring.D3GraphListView;
import org.rhq.coregui.client.inventory.resource.detail.monitoring.schedules.ResourceSchedulesView;
import org.rhq.coregui.client.inventory.resource.detail.monitoring.table.MetricsResourceView;
import org.rhq.coregui.client.inventory.resource.detail.monitoring.traits.TraitsView;
import org.rhq.coregui.client.inventory.resource.detail.operation.history.ResourceOperationHistoryListView;
import org.rhq.coregui.client.inventory.resource.detail.operation.schedule.ResourceOperationScheduleListView;
import org.rhq.coregui.client.inventory.resource.detail.summary.ActivityView;
import org.rhq.coregui.client.inventory.resource.detail.summary.TimelineView;
import org.rhq.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.coregui.client.util.BrowserUtility;
import org.rhq.coregui.client.util.Log;
import org.rhq.coregui.client.util.message.Message;

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
    private Integer resourceId;
    private ResourceComposite resourceComposite;
    private MetricsResourceView metricsResourceView;
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

    public ResourceDetailView(ResourceTreeView platformTree) {
        super(BASE_VIEW_PATH, createTitleBar(platformTree), createTabs());
        addStyleName("resourceDetail");
        summaryTab = getTabSet().getTabByName(Tab.Summary.NAME);
        summaryActivity = summaryTab.getSubTabByName(Tab.Summary.SubTab.ACTIVITY);
        summaryTimeline = summaryTab.getSubTabByName(Tab.Summary.SubTab.TIMELINE);

        inventoryTab = getTabSet().getTabByName(Tab.Inventory.NAME);
        inventoryChildren = inventoryTab.getSubTabByName(Tab.Inventory.SubTab.CHILDREN);
        inventoryChildHistory = inventoryTab.getSubTabByName(Tab.Inventory.SubTab.CHILD_HISTORY);
        inventoryConn = inventoryTab.getSubTabByName(Tab.Inventory.SubTab.CONNECTION_SETTINGS);
        inventoryConnHistory = inventoryTab.getSubTabByName(Tab.Inventory.SubTab.CONNECTION_SETTINGS_HISTORY);
        inventoryGroups = inventoryTab.getSubTabByName(Tab.Inventory.SubTab.GROUPS);
        inventoryAgent = inventoryTab.getSubTabByName(Tab.Inventory.SubTab.AGENT);

        alertsTab = getTabSet().getTabByName(Tab.Alerts.NAME);
        alertHistory = alertsTab.getSubTabByName(Tab.Alerts.SubTab.HISTORY);
        alertDef = alertsTab.getSubTabByName(Tab.Alerts.SubTab.DEFINITIONS);

        monitoringTab = getTabSet().getTabByName(Tab.Monitoring.NAME);
        monitorMetrics = monitoringTab.getSubTabByName(Tab.Monitoring.SubTab.METRICS);
        monitorTraits = monitoringTab.getSubTabByName(Tab.Monitoring.SubTab.TRAITS);
        monitorSched = monitoringTab.getSubTabByName(Tab.Monitoring.SubTab.SCHEDULES);
        monitorCallTime = monitoringTab.getSubTabByName(Tab.Monitoring.SubTab.CALL_TIME);

        eventsTab = getTabSet().getTabByName(Tab.Events.NAME);
        eventHistory = eventsTab.getSubTabByName(Tab.Events.SubTab.HISTORY);

        operationsTab = getTabSet().getTabByName(Tab.Operations.NAME);
        operationsSchedules = operationsTab.getSubTabByName(Tab.Operations.SubTab.SCHEDULES);
        operationsHistory = operationsTab.getSubTabByName(Tab.Operations.SubTab.HISTORY);

        configurationTab = getTabSet().getTabByName(Tab.Configuration.NAME);
        configCurrent = configurationTab.getSubTabByName(Tab.Configuration.SubTab.CURRENT);
        configHistory = configurationTab.getSubTabByName(Tab.Configuration.SubTab.HISTORY);

        driftTab = getTabSet().getTabByName(Tab.Drift.NAME);
        driftDefinitions = driftTab.getSubTabByName(Tab.Drift.SubTab.DEFINITIONS);

        contentTab = getTabSet().getTabByName(Tab.Content.NAME);
        contentDeployed = contentTab.getSubTabByName(Tab.Content.SubTab.DEPLOYED);
        contentNew = contentTab.getSubTabByName(Tab.Content.SubTab.NEW);
        contentSubscrip = contentTab.getSubTabByName(Tab.Content.SubTab.SUBSCRIPTIONS);
        contentHistory = contentTab.getSubTabByName(Tab.Content.SubTab.HISTORY);

        // hide until we have our tabs in place
        this.hide();
    }

    private static TwoLevelTab[] createTabs() {
        List<TwoLevelTab> tabs = new ArrayList<TwoLevelTab>();

        TwoLevelTab summaryTab = new TwoLevelTab(new ViewName(Tab.Summary.NAME, MSG.common_title_summary()),
            ImageManager.getResourceIcon(ResourceCategory.SERVICE, AvailabilityType.UP));
        SubTab summaryActivity = new SubTab(summaryTab, new ViewName(Tab.Summary.SubTab.ACTIVITY,
            MSG.view_tabs_common_activity()), null);
        SubTab summaryTimeline = new SubTab(summaryTab, new ViewName(Tab.Summary.SubTab.TIMELINE,
            MSG.view_tabs_common_timeline()), null);
        summaryTab.registerSubTabs(summaryActivity, summaryTimeline);
        tabs.add(summaryTab);

        TwoLevelTab inventoryTab = new TwoLevelTab(new ViewName(Tab.Inventory.NAME, MSG.view_tabs_common_inventory()),
            IconEnum.INVENTORY_SUMMARY);
        SubTab inventoryChildren = new SubTab(inventoryTab, new ViewName(Tab.Inventory.SubTab.CHILDREN,
            MSG.view_tabs_common_child_resources()), null);
        SubTab inventoryChildHistory = new SubTab(inventoryTab, new ViewName(Tab.Inventory.SubTab.CHILD_HISTORY,
            MSG.view_tabs_common_child_history()), null);
        SubTab inventoryConn = new SubTab(inventoryTab, new ViewName(Tab.Inventory.SubTab.CONNECTION_SETTINGS,
            MSG.view_tabs_common_connectionSettings()), null);
        SubTab inventoryConnHistory = new SubTab(inventoryTab, PluginConfigurationHistoryListView.VIEW_ID, null);
        SubTab inventoryGroups = new SubTab(inventoryTab, new ViewName(Tab.Inventory.SubTab.GROUPS,
            MSG.view_tabs_common_groups()), null);
        SubTab inventoryAgent = new SubTab(inventoryTab, new ViewName(Tab.Inventory.SubTab.AGENT,
            MSG.view_tabs_common_agent()), null);
        inventoryTab.registerSubTabs(inventoryChildren, inventoryChildHistory, inventoryConn, inventoryConnHistory,
            inventoryGroups, inventoryAgent);
        tabs.add(inventoryTab);

        TwoLevelTab alertsTab = new TwoLevelTab(new ViewName(Tab.Alerts.NAME, MSG.common_title_alerts()),
            IconEnum.ALERT_DEFINITIONS);
        SubTab alertHistory = new SubTab(alertsTab, new ViewName(Tab.Alerts.SubTab.HISTORY,
            MSG.view_tabs_common_history()), null);
        SubTab alertDef = new SubTab(alertsTab, new ViewName(Tab.Alerts.SubTab.DEFINITIONS,
            MSG.common_title_definitions()), null);
        alertsTab.registerSubTabs(alertDef, alertHistory);
        tabs.add(alertsTab);

        TwoLevelTab monitoringTab = new TwoLevelTab(
            new ViewName(Tab.Monitoring.NAME, MSG.view_tabs_common_monitoring()), IconEnum.SUSPECT_METRICS);

        SubTab monitorMetrics = new SubTab(monitoringTab, new ViewName(Tab.Monitoring.SubTab.METRICS,
            MSG.view_tabs_common_metrics()), null);
        SubTab monitorTraits = new SubTab(monitoringTab, new ViewName(Tab.Monitoring.SubTab.TRAITS,
            MSG.view_tabs_common_traits()), null);
        SubTab monitorSched = new SubTab(monitoringTab, new ViewName(Tab.Monitoring.SubTab.SCHEDULES,
            MSG.view_tabs_common_schedules()), null);
        SubTab monitorCallTime = new SubTab(monitoringTab, new ViewName(Tab.Monitoring.SubTab.CALL_TIME,
            MSG.view_tabs_common_calltime()), null);
        monitoringTab.registerSubTabs(monitorMetrics, monitorTraits, monitorSched, monitorCallTime);
        tabs.add(monitoringTab);

        TwoLevelTab eventsTab = new TwoLevelTab(new ViewName(Tab.Events.NAME, MSG.view_tabs_common_events()),
            IconEnum.EVENTS);
        SubTab eventHistory = new SubTab(eventsTab, new ViewName(Tab.Events.SubTab.HISTORY,
            MSG.view_tabs_common_history()), null);
        eventsTab.registerSubTabs(eventHistory);
        tabs.add(eventsTab);

        TwoLevelTab operationsTab = new TwoLevelTab(new ViewName(Tab.Operations.NAME, MSG.common_title_operations()),
            IconEnum.RECENT_OPERATIONS);
        SubTab operationsSchedules = new SubTab(operationsTab, new ViewName(Tab.Operations.SubTab.SCHEDULES,
            MSG.view_tabs_common_schedules()), null);
        SubTab operationsHistory = new SubTab(operationsTab, new ViewName(Tab.Operations.SubTab.HISTORY,
            MSG.view_tabs_common_history()), null);
        operationsTab.registerSubTabs(operationsSchedules, operationsHistory);
        tabs.add(operationsTab);

        TwoLevelTab configurationTab = new TwoLevelTab(new ViewName(Tab.Configuration.NAME,
            MSG.common_title_configuration()), IconEnum.CONFIGURATION_HISTORY);
        SubTab configCurrent = new SubTab(configurationTab, new ViewName(Tab.Configuration.SubTab.CURRENT,
            MSG.view_tabs_common_current()), null);
        SubTab configHistory = new SubTab(configurationTab, new ViewName(Tab.Configuration.SubTab.HISTORY,
            MSG.view_tabs_common_history()), null);
        configurationTab.registerSubTabs(configCurrent, configHistory);
        tabs.add(configurationTab);

        TwoLevelTab driftTab = new TwoLevelTab(new ViewName(Tab.Drift.NAME, MSG.view_tabs_common_drift()),
            IconEnum.DRIFT_COMPLIANCE);
        SubTab driftDefinitions = new SubTab(driftTab, new ViewName(Tab.Drift.SubTab.DEFINITIONS,
            MSG.common_title_definitions()), null);
        driftTab.registerSubTabs(driftDefinitions);
        tabs.add(driftTab);

        TwoLevelTab contentTab = new TwoLevelTab(new ViewName(Tab.Content.NAME, MSG.view_tabs_common_content()),
            IconEnum.CONTENT);
        SubTab contentDeployed = new SubTab(contentTab, new ViewName(Tab.Content.SubTab.DEPLOYED,
            MSG.view_tabs_common_deployed()), null);
        SubTab contentNew = new SubTab(contentTab, new ViewName(Tab.Content.SubTab.NEW, MSG.common_button_new()), null);
        SubTab contentSubscrip = new SubTab(contentTab, new ViewName(Tab.Content.SubTab.SUBSCRIPTIONS,
            MSG.view_tabs_common_subscriptions()), null);
        SubTab contentHistory = new SubTab(contentTab, new ViewName(Tab.Content.SubTab.HISTORY,
            MSG.view_tabs_common_history()), null);
        contentTab.registerSubTabs(contentDeployed, contentNew, contentSubscrip, contentHistory);
        tabs.add(contentTab);

        return tabs.toArray(new TwoLevelTab[tabs.size()]);
    }

    private static ResourceTitleBar createTitleBar(ResourceTreeView platformTree) {
        return new ResourceTitleBar(platformTree);
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
        boolean visible = hasMetricsOfType(this.resourceComposite, DataType.MEASUREMENT)
            || hasMetricsOfType(this.resourceComposite, DataType.AVAILABILITY);
        ViewFactory viewFactory;

        boolean visibleToIE8 = !BrowserUtility.isBrowserPreIE9();

        // visible = same test as above
        viewFactory = (!visible) ? null : new ViewFactory() {
            @Override
            public Canvas createView() {
                // metricsResourceView contains state of opened graphs (unlike other stateless views)
                metricsResourceView = MetricsResourceView.create(resourceComposite.getResource() );

                // this listener handles the subtab navigation
                addViewRenderedListener(metricsResourceView);

                return metricsResourceView;
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
                    ResourceConfigurationHistoryListView view = new ResourceConfigurationHistoryListView(resourceComposite.getResourcePermission()
                        .isConfigureWrite(), resource.getId());
                    view.setShowHeader(false);
                    return view;
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
                    return new FullHTMLPane("/portal/rhq/resource/content/view-plain.xhtml?id=" + resource.getId());
                }
            });

            updateSubTab(this.contentTab, this.contentNew, true, true, new ViewFactory() {
                @Override
                public Canvas createView() {
                    return new FullHTMLPane("/portal/rhq/resource/content/deploy-plain.xhtml?id=" + resource.getId());
                }
            });

            updateSubTab(this.contentTab, this.contentSubscrip, true, true, new ViewFactory() {
                @Override
                public Canvas createView() {
                    return new FullHTMLPane("/portal/rhq/resource/content/subscription-plain.xhtml?id=" + resource.getId());
                }
            });

            updateSubTab(this.contentTab, this.contentHistory, true, true, new ViewFactory() {
                @Override
                public Canvas createView() {
                    return new FullHTMLPane("/portal/rhq/resource/content/history-plain.xhtml?id=" + resource.getId());
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
                        resourceComposite = result.get(0);
                        loadResourceType(viewPath);

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

    private void loadResourceType(final ViewPath viewPath) {
        ResourceTypeRepository.Cache.getInstance().getResourceTypes(
            resourceComposite.getResource().getResourceType().getId(),
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

    public static class Tab {
        public static class Summary {
            public static final String NAME = "Summary";

            public static class SubTab {
                public static final String ACTIVITY = "Activity";
                public static final String TIMELINE = "Timeline";
            }
        }

        public static class Inventory {
            public static final String NAME = "Inventory";

            public static class SubTab {
                public static final String CHILDREN = "Children";
                public static final String CHILD_HISTORY = "ChildHistory";
                public static final String CONNECTION_SETTINGS = "ConnectionSettings";
                public static final String CONNECTION_SETTINGS_HISTORY = PluginConfigurationHistoryListView.VIEW_ID
                    .getName();
                public static final String GROUPS = "Groups";
                public static final String AGENT = "Agent";
            }
        }

        public static class Alerts {
            public static final String NAME = "Alerts";

            public static class SubTab {
                public static final String HISTORY = "History";
                public static final String DEFINITIONS = "Definitions";
            }
        }

        public static class Monitoring {
            public static final String NAME = "Monitoring";

            public static class SubTab {
                public static final String METRICS = "Metrics";
                public static final String TRAITS = "Traits";
                public static final String SCHEDULES = "Schedules";
                public static final String CALL_TIME = "CallTime";
            }
        }

        public static class Events {
            public static final String NAME = "Events";

            public static class SubTab {
                public static final String HISTORY = "History";
            }
        }

        public static class Operations {
            public static final String NAME = "Operations";

            public static class SubTab {
                public static final String SCHEDULES = "Schedules";
                public static final String HISTORY = "History";
            }
        }

        public static class Configuration {
            public static final String NAME = "Configuration";

            public static class SubTab {
                public static final String CURRENT = "Current";
                public static final String HISTORY = "History";
            }
        }

        public static class Drift {
            public static final String NAME = "Drift";

            public static class SubTab {
                public static final String DEFINITIONS = "Definitions";
            }
        }

        public static class Content {
            public static final String NAME = "Content";

            public static class SubTab {
                public static final String DEPLOYED = "Deployed";
                public static final String NEW = "New";
                public static final String SUBSCRIPTIONS = "Subscriptions";
                public static final String HISTORY = "History";
            }
        }
    }
}
