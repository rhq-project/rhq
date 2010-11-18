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
import com.smartgwt.client.widgets.tab.Tab;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.ResourceTypeFacet;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.resource.composite.ResourcePermission;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.PermissionsLoadedListener;
import org.rhq.enterprise.gui.coregui.client.UserPermissionsManager;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.alert.ResourceAlertHistoryView;
import org.rhq.enterprise.gui.coregui.client.alert.definitions.ResourceAlertDefinitionsView;
import org.rhq.enterprise.gui.coregui.client.components.FullHTMLPane;
import org.rhq.enterprise.gui.coregui.client.components.tab.SubTab;
import org.rhq.enterprise.gui.coregui.client.components.tab.TwoLevelTab;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.InventoryView;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.AbstractTwoLevelTabSetView;
import org.rhq.enterprise.gui.coregui.client.inventory.common.event.EventCompositeHistoryView;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.ResourceGroupListView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceCompositeSearchView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.configuration.ConfigurationHistoryView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.configuration.ResourceConfigurationEditView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.inventory.PluginConfigurationEditView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.inventory.ResourceResourceGroupsView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.schedules.SchedulesView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.traits.TraitsView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.operation.OperationHistoryView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.summary.DashboardView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.summary.OverviewView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

/**
 * Right panel of the Resource view.
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class ResourceDetailView extends AbstractTwoLevelTabSetView<ResourceComposite, ResourceTitleBar> {
    private static final String BASE_VIEW_PATH = "Resource";

    private Integer resourceId;

    private ResourceComposite resourceComposite;

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
    private SubTab inventoryChildHistory;
    private SubTab inventoryConn;
    private SubTab inventoryConnHistory;
    private SubTab inventoryGroups;
    private SubTab inventoryGroupMembership;
    private SubTab opHistory;
    private SubTab opSched;
    private SubTab alertHistory;
    private SubTab alertDef;
    private SubTab configCurrent;
    private SubTab configHistory;
    private SubTab eventHistory;
    private SubTab contentDeployed;
    private SubTab contentNew;
    private SubTab contentSubscrip;
    private SubTab contentHistory;

    public ResourceDetailView(String locatorId) {
        super(locatorId, BASE_VIEW_PATH);
        this.hide();
    }

    protected List<TwoLevelTab> createTabs() {
        List<TwoLevelTab> tabs = new ArrayList<TwoLevelTab>();

        summaryTab = new TwoLevelTab(getTabSet().extendLocatorId("Summary"), MSG.view_tabs_common_summary(),
            "/images/icons/Service_up_16.png");
        summaryOverview = new SubTab(summaryTab.extendLocatorId("Overview"), MSG.view_tabs_common_overview(), null);
        summaryDashboard = new SubTab(summaryTab.extendLocatorId("Dashboard"), MSG.view_tabs_common_dashboard(), null);
        summaryTimeline = new SubTab(summaryTab.extendLocatorId("Timeline"), MSG.view_tabs_common_timeline(), null);
        summaryTab.registerSubTabs(summaryOverview, summaryDashboard, summaryTimeline);
        tabs.add(summaryTab);

        monitoringTab = new TwoLevelTab(getTabSet().extendLocatorId("Monitoring"), MSG.view_tabs_common_monitoring(),
            "/images/icons/Monitor_grey_16.png");
        monitorGraphs = new SubTab(monitoringTab.extendLocatorId("Graphs"), MSG.view_tabs_common_graphs(), null);
        monitorTables = new SubTab(monitoringTab.extendLocatorId("Tables"), MSG.view_tabs_common_tables(), null);
        monitorTraits = new SubTab(monitoringTab.extendLocatorId("Traits"), MSG.view_tabs_common_traits(), null);
        monitorAvail = new SubTab(monitoringTab.extendLocatorId("Availability"), MSG.view_tabs_common_availability(),
            null);
        monitorSched = new SubTab(monitoringTab.extendLocatorId("Schedules"), MSG.view_tabs_common_schedules(), null);
        monitorCallTime = new SubTab(monitoringTab.extendLocatorId("CallTime"), MSG.view_tabs_common_calltime(), null);
        monitoringTab.registerSubTabs(monitorGraphs, monitorTables, monitorTraits, monitorAvail, monitorSched,
            monitorCallTime);
        tabs.add(monitoringTab);

        inventoryTab = new TwoLevelTab(getTabSet().extendLocatorId("Inventory"), MSG.view_tabs_common_inventory(),
            "/images/icons/Inventory_grey_16.png");
        inventoryChildren = new SubTab(inventoryTab.extendLocatorId("Children"),
            MSG.view_tabs_common_child_resources(), null);
        inventoryChildHistory = new SubTab(inventoryTab.extendLocatorId("ChildHist"), MSG
            .view_tabs_common_child_history(), null);
        inventoryConn = new SubTab(inventoryTab.extendLocatorId("ConnectionSettings"), MSG
            .view_tabs_common_connectionSettings(), null);
        inventoryConnHistory = new SubTab(inventoryTab.extendLocatorId("ConnSetHist"), MSG
            .view_tabs_common_connectionSettingsHistory(), null);
        inventoryGroups = new SubTab(inventoryTab.extendLocatorId("Groups"), MSG.view_tabs_common_groups(), null);
        inventoryGroupMembership = new SubTab(inventoryTab.extendLocatorId("GroupMembership"), MSG
            .view_tabs_common_group_membership(), null);
        inventoryTab.registerSubTabs(this.inventoryChildren, this.inventoryChildHistory, this.inventoryConn,
            this.inventoryConnHistory, this.inventoryGroups, this.inventoryGroupMembership);
        tabs.add(inventoryTab);

        operationsTab = new TwoLevelTab(getTabSet().extendLocatorId("Operations"), MSG.view_tabs_common_operations(),
            "/images/icons/Operation_grey_16.png");
        this.opHistory = new SubTab(operationsTab.extendLocatorId("History"), MSG.view_tabs_common_history(), null);
        this.opSched = new SubTab(operationsTab.extendLocatorId("Scheduled"), MSG.view_tabs_common_scheduled(), null);
        operationsTab.registerSubTabs(this.opHistory, this.opSched);
        tabs.add(operationsTab);

        alertsTab = new TwoLevelTab(getTabSet().extendLocatorId("Alerts"), MSG.view_tabs_common_alerts(),
            "/images/icons/Alert_grey_16.png");
        this.alertHistory = new SubTab(alertsTab.extendLocatorId("History"), MSG.view_tabs_common_history(), null);
        this.alertDef = new SubTab(alertsTab.extendLocatorId("Definitions"), MSG.view_tabs_common_definitions(), null);
        alertsTab.registerSubTabs(alertHistory, alertDef);
        tabs.add(alertsTab);

        configurationTab = new TwoLevelTab(getTabSet().extendLocatorId("Configuration"), MSG
            .view_tabs_common_configuration(), "/images/icons/Configure_grey_16.png");
        this.configCurrent = new SubTab(configurationTab.extendLocatorId("Current"), MSG.view_tabs_common_current(),
            null);
        this.configHistory = new SubTab(configurationTab.extendLocatorId("History"), MSG.view_tabs_common_history(),
            null);
        configurationTab.registerSubTabs(this.configCurrent, this.configHistory);
        tabs.add(configurationTab);

        eventsTab = new TwoLevelTab(getTabSet().extendLocatorId("Events"), MSG.view_tabs_common_events(),
            "/images/icons/Events_grey_16.png");
        this.eventHistory = new SubTab(eventsTab.extendLocatorId("History"), MSG.view_tabs_common_history(), null);
        eventsTab.registerSubTabs(eventHistory);
        tabs.add(eventsTab);

        contentTab = new TwoLevelTab(getTabSet().extendLocatorId("Content"), MSG.view_tabs_common_content(),
            "/images/icons/Content_grey_16.png");
        this.contentDeployed = new SubTab(contentTab.extendLocatorId("Deployed"), MSG.view_tabs_common_deployed(), null);
        this.contentNew = new SubTab(contentTab.extendLocatorId("New"), MSG.view_tabs_common_new(), null);
        this.contentSubscrip = new SubTab(contentTab.extendLocatorId("Subscriptions"), MSG
            .view_tabs_common_subscriptions(), null);
        this.contentHistory = new SubTab(contentTab.extendLocatorId("History"), MSG.view_tabs_common_history(), null);
        contentTab.registerSubTabs(contentDeployed, contentNew, contentSubscrip, contentHistory);
        tabs.add(contentTab);

        return tabs;
    }

    protected ResourceTitleBar createTitleBar() {
        return new ResourceTitleBar(extendLocatorId("TitleBar"));
    }

    protected void updateTabContent(ResourceComposite resourceComposite) {
        boolean enabled;
        boolean visible;
        Canvas canvas;

        this.resourceComposite = resourceComposite;
        Resource resource = this.resourceComposite.getResource();
        getTitleBar().setResource(resource);

        for (Tab top : this.getTabSet().getTabs()) {
            ((TwoLevelTab) top).getLayout().destroyViews();
        }

        ResourcePermission resourcePermissions = this.resourceComposite.getResourcePermission();
        Set<ResourceTypeFacet> facets = this.resourceComposite.getResourceFacets().getFacets();

        // Summary Tab
        updateSubTab(this.summaryTab, this.summaryOverview, new OverviewView(this.summaryTab
            .extendLocatorId("OverviewView"), this.resourceComposite), true, true);

        updateSubTab(this.summaryTab, this.summaryDashboard, new DashboardView(this.summaryDashboard
            .extendLocatorId("View"), this.resourceComposite), true, true);

        updateSubTab(this.summaryTab, this.summaryTimeline, new FullHTMLPane(this.summaryTimeline
            .extendLocatorId("View"), "/rhq/resource/summary/timeline-plain.xhtml?id=" + resource.getId()), true, true);

        // Monitoring Tab
        visible = hasMetricsOfType(this.resourceComposite, DataType.MEASUREMENT);
        //canvas = (visible) ? new GraphListView(this.monitoringTab.extendLocatorId("GraphListView"), resource) : null;
        canvas = (visible) ? new FullHTMLPane(this.monitorGraphs.extendLocatorId("View"),
            "/rhq/resource/monitor/graphs-plain.xhtml?id=" + resource.getId()) : null;
        updateSubTab(this.monitoringTab, this.monitorGraphs, canvas, visible, true);

        // visible = same test as above
        canvas = (visible) ? new FullHTMLPane(this.monitorTables.extendLocatorId("View"),
            "/rhq/common/monitor/tables-plain.xhtml?id=" + resource.getId()) : null;
        updateSubTab(this.monitoringTab, this.monitorTables, canvas, visible, true);

        visible = hasMetricsOfType(this.resourceComposite, DataType.TRAIT);
        canvas = (visible) ? new TraitsView(this.monitoringTab.extendLocatorId("TraitsView"), resource.getId()) : null;
        updateSubTab(this.monitoringTab, this.monitorTraits, canvas, visible, true);

        updateSubTab(this.monitoringTab, this.monitorAvail, new FullHTMLPane(this.monitorAvail.extendLocatorId("View"),
            "/rhq/resource/monitor/availabilityHistory-plain.xhtml?id=" + resource.getId()), true, true);

        updateSubTab(this.monitoringTab, this.monitorSched, new SchedulesView(monitoringTab
            .extendLocatorId("SchedulesView"), resource.getId()), hasMetricsOfType(this.resourceComposite, null), true);

        visible = facets.contains(ResourceTypeFacet.CALL_TIME);
        canvas = (visible) ? new FullHTMLPane(this.monitorCallTime.extendLocatorId("View"),
            "/rhq/resource/monitor/response-plain.xhtml?id=" + resource.getId()) : null;
        updateSubTab(this.monitoringTab, this.monitorCallTime, canvas, visible, true);

        // Inventory Tab
        ResourceType type = this.resourceComposite.getResource().getResourceType();
        visible = !type.getChildResourceTypes().isEmpty();
        canvas = (visible) ? ResourceCompositeSearchView.getChildrenOf(this.inventoryTab
            .extendLocatorId("ChildrenView"), resourceComposite) : null;
        updateSubTab(this.inventoryTab, this.inventoryChildren, canvas, visible, true);

        updateSubTab(this.inventoryTab, this.inventoryChildHistory, new Canvas(), visible, true);

        visible = facets.contains(ResourceTypeFacet.PLUGIN_CONFIGURATION);
        canvas = (visible) ? new PluginConfigurationEditView(this.inventoryTab.extendLocatorId("PluginConfigView"),
            resourceComposite) : null;
        updateSubTab(this.inventoryTab, this.inventoryConn, canvas, visible, true);

        // same test, use above setting for 'visible'
        canvas = (visible) ? new Canvas() : null; // TODO: Add real canvas when visible        
        updateSubTab(this.inventoryTab, this.inventoryConnHistory, canvas, visible, true);

        updateSubTab(this.inventoryTab, this.inventoryGroups, ResourceGroupListView.getGroupsOf(this.inventoryTab
            .extendLocatorId("GroupsView"), resource.getId()), true, true);

        Set<Permission> globalPermissions = this.resourceComposite.getResourcePermission().getPermissions();
        enabled = globalPermissions.contains(Permission.MANAGE_INVENTORY);
        canvas = (enabled) ? new ResourceResourceGroupsView(this.inventoryTab.extendLocatorId("GroupMembershipView"),
            resourceId) : null;
        updateSubTab(this.inventoryTab, this.inventoryGroupMembership, canvas, true, enabled);

        // Operations Tab
        if (updateTab(this.operationsTab, facets.contains(ResourceTypeFacet.OPERATION), true)) {
            // comment out GWT-based operation history until...
            //     1) user can delete history if they possess the appropriate permissions
            //     2) user can see both operation arguments and results in the history details pop-up
            //     3) operation arguments/results become read-only configuration data in the history details pop-up
            //     4) user can navigate to the group operation that spawned this resource operation history, if appropriate
            // note: enabled operation execution/schedules from left-nav, if it doesn't already exist
            updateSubTab(this.operationsTab, this.opHistory, OperationHistoryView.getResourceHistoryView(operationsTab
                .extendLocatorId("HistoryView"), this.resourceComposite), true, true);

            updateSubTab(this.operationsTab, this.opSched, new FullHTMLPane(this.opSched.extendLocatorId("View"),
                "/rhq/resource/operation/resourceOperationSchedules-plain.xhtml?id=" + resource.getId()), true, true);
        }

        // Alerts Tab
        updateSubTab(this.alertsTab, this.alertHistory, ResourceAlertHistoryView.get(this.alertHistory
            .extendLocatorId("View"), resourceComposite), true, true);

        updateSubTab(this.alertsTab, this.alertDef, new ResourceAlertDefinitionsView(alertsTab
            .extendLocatorId("AlertDefView"), this.resourceComposite), true, true);

        // Configuration Tab
        if (updateTab(this.configurationTab, facets.contains(ResourceTypeFacet.CONFIGURATION), resourcePermissions
            .isConfigureRead())) {

            updateSubTab(this.configurationTab, this.configCurrent, new ResourceConfigurationEditView(this
                .extendLocatorId("ResourceConfigView"), resourceComposite), true, true);

            updateSubTab(this.configurationTab, this.configHistory, ConfigurationHistoryView.getHistoryOf(this
                .extendLocatorId("ConfigHistView"), resource.getId()), true, true);
        }

        // Events Tab
        if (updateTab(this.eventsTab, facets.contains(ResourceTypeFacet.EVENT), true)) {

            updateSubTab(this.eventsTab, this.eventHistory, EventCompositeHistoryView.get(this.eventsTab
                .extendLocatorId("CompositeHistoryView"), resourceComposite), true, true);
        }

        // Content Tab
        if (updateTab(this.contentTab, facets.contains(ResourceTypeFacet.CONTENT), true)) {

            updateSubTab(this.contentTab, this.contentDeployed, new FullHTMLPane(this.contentDeployed
                .extendLocatorId("View"), "/rhq/resource/content/view-plain.xhtml?id=" + resource.getId()), true, true);

            updateSubTab(this.contentTab, this.contentNew, new FullHTMLPane(this.contentNew.extendLocatorId("View"),
                "/rhq/resource/content/deploy-plain.xhtml?id=" + resource.getId()), true, true);

            updateSubTab(this.contentTab, this.contentSubscrip, new FullHTMLPane(this.contentSubscrip
                .extendLocatorId("View"), "/rhq/resource/content/subscription-plain.xhtml?id=" + resource.getId()),
                true, true);

            updateSubTab(this.contentTab, this.contentHistory, new FullHTMLPane(this.configHistory
                .extendLocatorId("View"), "/rhq/resource/content/history-plain.xhtml?id=" + resource.getId()), true,
                true);
        }

        this.show();
        markForRedraw();
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
        //criteria.fetchParentResource(true);
        GWTServiceLookup.getResourceService().findResourceCompositesByCriteria(criteria,
            new AsyncCallback<PageList<ResourceComposite>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getMessageCenter().notify(
                        new Message(MSG.view_inventory_resource_loadFailed(String.valueOf(resourceId)),
                            Message.Severity.Warning));

                    CoreGUI.goToView(InventoryView.VIEW_ID.getName());
                }

                public void onSuccess(PageList<ResourceComposite> result) {
                    if (result.isEmpty()) {
                        //noinspection ThrowableInstanceNeverThrown
                        onFailure(new Exception(MSG.view_inventory_resource_loadFailed(String.valueOf(resourceId))));
                    } else {
                        final ResourceComposite resourceComposite = result.get(0);
                        // First load the user's permissions for the Resource.
                        UserPermissionsManager.getInstance().loadResourcePermissions(resourceComposite,
                            new PermissionsLoadedListener() {
                                public void onPermissionsLoaded(Set<Permission> permissions) {
                                    // Next load the Resource's type.
                                    loadResourceType(resourceComposite, viewPath);
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
                    resourceComposite.getResource().setResourceType(type);
                    ResourceDetailView.this.resourceComposite = resourceComposite;
                    updateTabContent(resourceComposite);
                    selectTab(getTabName(), getSubTabName(), viewPath);
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
