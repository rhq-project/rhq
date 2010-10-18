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
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.alert.ResourceAlertHistoryView;
import org.rhq.enterprise.gui.coregui.client.alert.definitions.ResourceAlertDefinitionsView;
import org.rhq.enterprise.gui.coregui.client.components.FullHTMLPane;
import org.rhq.enterprise.gui.coregui.client.components.tab.SubTab;
import org.rhq.enterprise.gui.coregui.client.components.tab.TwoLevelTab;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.AbstractTwoLevelTabSetView;
import org.rhq.enterprise.gui.coregui.client.inventory.common.event.EventCompositeHistoryView;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.ResourceGroupListView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.InventoryView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceSearchView;
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
    private SubTab inventoryConn;
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

        summaryTab = new TwoLevelTab(getTabSet().extendLocatorId("Summary"), "Summary",
            "/images/icons/Service_up_16.png");
        summaryOverview = new SubTab(summaryTab.extendLocatorId("Overview"), "Overview", null);
        summaryDashboard = new SubTab(summaryTab.extendLocatorId("Dashboard"), "Dashboard", null);
        summaryTimeline = new SubTab(summaryTab.extendLocatorId("Timeline"), "Timeline", null);
        summaryTab.registerSubTabs(summaryOverview, summaryDashboard, summaryTimeline);
        tabs.add(summaryTab);

        monitoringTab = new TwoLevelTab(getTabSet().extendLocatorId("Monitoring"), "Monitoring",
            "/images/icons/Monitor_grey_16.png");
        monitorGraphs = new SubTab(monitoringTab.extendLocatorId("Graphs"), "Graphs", null);
        monitorTables = new SubTab(monitoringTab.extendLocatorId("Tables"), "Tables", null);
        monitorTraits = new SubTab(monitoringTab.extendLocatorId("Traits"), "Traits", null);
        monitorAvail = new SubTab(monitoringTab.extendLocatorId("Availability"), "Availability", null);
        monitorSched = new SubTab(monitoringTab.extendLocatorId("Schedules"), "Schedules", null);
        monitorCallTime = new SubTab(monitoringTab.extendLocatorId("CallTime"), "Call Time", null);
        monitoringTab.registerSubTabs(monitorGraphs, monitorTables, monitorTraits, monitorAvail, monitorSched,
            monitorCallTime);
        tabs.add(monitoringTab);

        inventoryTab = new TwoLevelTab(getTabSet().extendLocatorId("Inventory"), "Inventory",
            "/images/icons/Inventory_grey_16.png");
        inventoryChildren = new SubTab(inventoryTab.extendLocatorId("Children"), "Children", null);
        inventoryConn = new SubTab(inventoryTab.extendLocatorId("ConnectionSettings"), "Connection Settings", null);
        inventoryGroups = new SubTab(inventoryTab.extendLocatorId("Groups"), "Groups", null);
        inventoryGroupMembership = new SubTab(inventoryTab.extendLocatorId("GroupMembership"), "Group Membership", null);
        inventoryTab.registerSubTabs(this.inventoryChildren, this.inventoryConn, this.inventoryGroups,
            this.inventoryGroupMembership);
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

        contentTab = new TwoLevelTab(getTabSet().extendLocatorId("Content"), "Content",
            "/images/icons/Content_grey_16.png");
        this.contentDeployed = new SubTab(contentTab.extendLocatorId("Deployed"), "Deployed", null);
        this.contentNew = new SubTab(contentTab.extendLocatorId("New"), "New", null);
        this.contentSubscrip = new SubTab(contentTab.extendLocatorId("Subscriptions"), "Subscriptions", null);
        this.contentHistory = new SubTab(contentTab.extendLocatorId("History"), "History", null);
        contentTab.registerSubTabs(contentDeployed, contentNew, contentSubscrip, contentHistory);
        tabs.add(contentTab);

        return tabs;
    }

    protected ResourceTitleBar createTitleBar() {
        return new ResourceTitleBar(extendLocatorId("TitleBar"));
    }

    protected void updateTabContent(ResourceComposite resourceComposite, Set<Permission> globalPermissions) {
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

        updateSubTab(this.summaryTab, this.summaryOverview,
            new OverviewView(this.summaryTab.extendLocatorId("OverviewView"), this.resourceComposite), true, true);
        updateSubTab(this.summaryTab, this.summaryDashboard, new DashboardView(this.resourceComposite), true, true);
        updateSubTab(this.summaryTab, this.summaryTimeline, new FullHTMLPane(
            "/rhq/resource/summary/timeline-plain.xhtml?id=" + resource.getId()), true, true);

        visible = hasMetricsOfType(this.resourceComposite, DataType.MEASUREMENT);
        //canvas = (visible) ? new GraphListView(this.monitoringTab.extendLocatorId("GraphListView"), resource) : null;
        canvas = (visible) ? new FullHTMLPane("/rhq/resource/monitor/graphs-plain.xhtml?id=" + resource.getId()) : null;
        updateSubTab(this.monitoringTab, this.monitorGraphs, canvas, visible, true);
        // visible = same test as above
        canvas = (visible) ? new FullHTMLPane("/rhq/common/monitor/tables-plain.xhtml?id=" + resource.getId()) : null;
        updateSubTab(this.monitoringTab, this.monitorTables, canvas, visible, true);
        visible = hasMetricsOfType(this.resourceComposite, DataType.TRAIT);
        canvas = (visible) ? new TraitsView(this.monitoringTab.extendLocatorId("TraitsView"), resource.getId()) : null;
        updateSubTab(this.monitoringTab, this.monitorTraits, canvas, visible, true);

        updateSubTab(this.monitoringTab, this.monitorAvail, new FullHTMLPane(
            "/rhq/resource/monitor/availabilityHistory-plain.xhtml?id=" + resource.getId()), true, true);
        updateSubTab(this.monitoringTab, this.monitorSched,
            new SchedulesView(monitoringTab.extendLocatorId("SchedulesView"), resource.getId()),
            hasMetricsOfType(this.resourceComposite, null), true);
        visible = facets.contains(ResourceTypeFacet.CALL_TIME);
        canvas = (visible) ? new FullHTMLPane("/rhq/resource/monitor/response-plain.xhtml?id=" + resource.getId())
            : null;
        updateSubTab(this.monitoringTab, this.monitorCallTime, canvas, visible, true);

        ResourceType type = this.resourceComposite.getResource().getResourceType();
        visible = !type.getChildResourceTypes().isEmpty();
        canvas = (visible) ? ResourceSearchView.getChildrenOf(this.inventoryTab.extendLocatorId("ChildrenView"),
            resource.getId()) : null;
        updateSubTab(this.inventoryTab, this.inventoryChildren, canvas, visible, true);
        visible = facets.contains(ResourceTypeFacet.PLUGIN_CONFIGURATION);
        canvas = (visible) ? new PluginConfigurationEditView(this.inventoryTab.extendLocatorId("PluginConfigView"),
            resourceComposite) : null;
        updateSubTab(this.inventoryTab, this.inventoryConn, canvas, visible, true);
        updateSubTab(this.inventoryTab, this.inventoryGroups,
            ResourceGroupListView.getGroupsOf(this.inventoryTab.extendLocatorId("GroupsView"), resource.getId()), true,
            true);
        enabled = globalPermissions.contains(Permission.MANAGE_INVENTORY);
        canvas = (enabled) ? new ResourceResourceGroupsView(this.inventoryTab.extendLocatorId("GroupMembershipView"),
            resourceId) : null;
        updateSubTab(this.inventoryTab, this.inventoryGroupMembership, canvas, true, enabled);

        if (updateTab(this.operationsTab, facets.contains(ResourceTypeFacet.OPERATION), true)) {
            // comment out GWT-based operation history until...
            //     1) user can delete history if they possess the appropriate permissions
            //     2) user can see both operation arguments and results in the history details pop-up
            //     3) operation arguments/results become read-only configuration data in the history details pop-up
            //     4) user can navigate to the group operation that spawned this resource operation history, if appropriate
            // note: enabled operation execution/schedules from left-nav, if it doesn't already exist
            updateSubTab(this.operationsTab, this.opHistory, OperationHistoryView.getResourceHistoryView(
                operationsTab.extendLocatorId("HistoryView"), this.resourceComposite), true, true);
            updateSubTab(this.operationsTab, this.opSched, new FullHTMLPane(
                "/rhq/resource/operation/resourceOperationSchedules-plain.xhtml?id=" + resource.getId()), true, true);
        }

        updateSubTab(this.alertsTab, this.alertHistory, ResourceAlertHistoryView.get(resourceComposite), true, true);
        updateSubTab(this.alertsTab, this.alertDef,
            new ResourceAlertDefinitionsView(alertsTab.extendLocatorId("AlertDefView"), this.resourceComposite), true,
            true);

        if (updateTab(this.configurationTab, facets.contains(ResourceTypeFacet.CONFIGURATION),
            resourcePermissions.isConfigureRead())) {
            updateSubTab(this.configurationTab, this.configCurrent,
                new ResourceConfigurationEditView(this.extendLocatorId("ResourceConfigView"), resourceComposite), true,
                true);
            updateSubTab(this.configurationTab, this.configHistory,
                ConfigurationHistoryView.getHistoryOf(this.extendLocatorId("ConfigHistView"), resource.getId()), true,
                true);
        }

        if (updateTab(this.eventsTab, facets.contains(ResourceTypeFacet.EVENT), true)) {
            updateSubTab(this.eventsTab, this.eventHistory, EventCompositeHistoryView.get(
                this.eventsTab.extendLocatorId("CompositeHistoryView"), resourceComposite), true, true);
        }

        if (updateTab(this.contentTab, facets.contains(ResourceTypeFacet.CONTENT), true)) {
            updateSubTab(this.contentTab, this.contentDeployed, new FullHTMLPane(
                "/rhq/resource/content/view-plain.xhtml?id=" + resource.getId()), true, true);
            updateSubTab(this.contentTab, this.contentNew, new FullHTMLPane(
                "/rhq/resource/content/deploy-plain.xhtml?id=" + resource.getId()), true, true);
            updateSubTab(this.contentTab, this.contentSubscrip, new FullHTMLPane(
                "/rhq/resource/content/subscription-plain.xhtml?id=" + resource.getId()), true, true);
            updateSubTab(this.contentTab, this.contentHistory, new FullHTMLPane(
                "/rhq/resource/content/history-plain.xhtml?id=" + resource.getId()), true, true);
        }

        this.show();
        markForRedraw();
    }

    public Integer getSelectedItemId() {
        return this.resourceId;
    }

    protected void loadSelectedItem(final int resourceId, final ViewPath viewPath,
        final Set<Permission> globalPermissions) {
        this.resourceId = resourceId;

        ResourceCriteria criteria = new ResourceCriteria();
        criteria.addFilterId(resourceId);
        criteria.fetchTags(true);
        //criteria.fetchParentResource(true);
        GWTServiceLookup.getResourceService().findResourceCompositesByCriteria(criteria,
            new AsyncCallback<PageList<ResourceComposite>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getMessageCenter().notify(
                        new Message("Resource with id [" + resourceId + "] does not exist or is not accessible.",
                            Message.Severity.Warning));

                    CoreGUI.goToView(InventoryView.VIEW_ID);
                }

                public void onSuccess(PageList<ResourceComposite> result) {
                    if (result.isEmpty()) {
                        //noinspection ThrowableInstanceNeverThrown
                        onFailure(new Exception("Resource with id [" + resourceId + "] does not exist."));
                    } else {
                        final ResourceComposite resourceComposite = result.get(0);
                        loadResourceType(resourceComposite, viewPath, globalPermissions);
                    }
                }
            });
    }

    private void loadResourceType(final ResourceComposite resourceComposite, final ViewPath viewPath,
        final Set<Permission> globalPermissions) {
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
                    updateTabContent(resourceComposite, globalPermissions);
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
