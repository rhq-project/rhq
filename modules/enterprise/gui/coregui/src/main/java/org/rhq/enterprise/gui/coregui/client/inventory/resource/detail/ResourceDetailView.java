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
import com.smartgwt.client.widgets.tab.Tab;

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
import org.rhq.enterprise.gui.coregui.client.alert.definitions.ResourceAlertDefinitionsView;
import org.rhq.enterprise.gui.coregui.client.components.FullHTMLPane;
import org.rhq.enterprise.gui.coregui.client.components.tab.SubTab;
import org.rhq.enterprise.gui.coregui.client.components.tab.TwoLevelTab;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.AbstractTwoLevelTabSetView;
import org.rhq.enterprise.gui.coregui.client.inventory.common.event.EventCompositeHistoryView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.InventoryView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceSearchView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.configuration.ConfigurationHistoryView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.configuration.ResourceConfigurationEditView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.inventory.PluginConfigurationEditView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.GraphListView;
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
    private SubTab opHistory;
    private SubTab opSched;
    private SubTab alertHistory;
    private SubTab alertDef;
    private SubTab alertDelete;
    private SubTab configCurrent;
    private SubTab configHistory;
    private SubTab eventHistory;
    private SubTab contentDeployed;
    private SubTab contentNew;
    private SubTab contentSubscrip;
    private SubTab contentHistory;

    public ResourceDetailView(String locatorId) {
        super(locatorId, BASE_VIEW_PATH);
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
        inventoryTab.registerSubTabs(this.inventoryChildren, this.inventoryConn);
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
        this.alertDelete = new SubTab(alertsTab.extendLocatorId("DELETEME"), "DELETEME", null);
        alertsTab.registerSubTabs(alertHistory, alertDef, alertDelete);
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

    protected void updateTabContent(ResourceComposite resourceComposite) {
        this.resourceComposite = resourceComposite;

        final Resource resource = this.resourceComposite.getResource();
        getTitleBar().setResource(resource);

        for (Tab top : this.getTabSet().getTabs()) {
            ((TwoLevelTab) top).getLayout().destroyViews();
        }

        this.summaryOverview.setCanvas(new OverviewView(this.summaryTab.extendLocatorId("OverviewView"),
            this.resourceComposite));
        this.summaryDashboard.setCanvas(new DashboardView(this.resourceComposite));
        this.summaryTimeline.setCanvas(new FullHTMLPane("/rhq/resource/summary/timeline-plain.xhtml?id="
            + resource.getId()));

        this.monitorGraphs.setCanvas(new GraphListView(this.monitoringTab.extendLocatorId("GraphListView"), resource));
        this.monitorTables.setCanvas(new FullHTMLPane("/rhq/common/monitor/tables-plain.xhtml?id=" + resource.getId()));
        this.monitorTraits
            .setCanvas(new TraitsView(this.monitoringTab.extendLocatorId("TraitsView"), resource.getId()));
        this.monitorAvail.setCanvas(new FullHTMLPane("/rhq/resource/monitor/availabilityHistory-plain.xhtml?id="
            + resource.getId()));
        this.monitorSched
            .setCanvas(new SchedulesView(monitoringTab.extendLocatorId("SchedulesView"), resource.getId()));
        this.monitorCallTime.setCanvas(new FullHTMLPane("/rhq/resource/monitor/response-plain.xhtml?id="
            + resource.getId()));

        this.inventoryChildren.setCanvas(ResourceSearchView.getChildrenOf(resource.getId()));
        this.inventoryConn.setCanvas(new PluginConfigurationEditView(this.extendLocatorId("PluginConfig"), resource));

        // comment out GWT-based operation history until...
        //     1) user can delete history if they possess the appropriate permissions
        //     2) user can see both operation arguments and results in the history details pop-up
        //     3) operation arguments/results become read-only configuration data in the history details pop-up
        //     4) user can navigate to the group operation that spawned this resource operation history, if appropriate
        // note: enabled operation execution/schedules from left-nav, if it doesn't already exist
        this.opHistory.setCanvas(OperationHistoryView.getResourceHistoryView(operationsTab.extendLocatorId("History"),
            resourceComposite));
        this.opSched.setCanvas(new FullHTMLPane("/rhq/resource/operation/resourceOperationSchedules-plain.xhtml?id="
            + resource.getId()));

        // comment out GWT-based alert definitions/history views until...
        //     1) new workflow is implement for alert definition creation, with particular attention to interaction model for alert notifications
        //     2) user can delete/ack/purgeAll alerts if they possess the appropriate permissions
        //     3) user can enable/disable/delete alert definitions if they possess the appropriate permissions
        //     4) user can search alert history by: date alert was fired, alert priority, or alert definition
        this.alertHistory.setCanvas(new FullHTMLPane("/rhq/resource/alert/listAlertHistory-plain.xhtml?id="
            + resource.getId()));
        this.alertDef.setCanvas(new ResourceAlertDefinitionsView(alertsTab.extendLocatorId("Def"), resource));
        this.alertDelete.setCanvas(new FullHTMLPane("/rhq/resource/alert/listAlertDefinitions-plain.xhtml?id="
            + resource.getId()));

        this.configCurrent
            .setCanvas(new ResourceConfigurationEditView(this.extendLocatorId("ResourceConfig"), resource));
        this.configHistory.setCanvas(ConfigurationHistoryView.getHistoryOf(configurationTab.extendLocatorId("Hist"),
            resource.getId()));

        this.eventHistory.setCanvas(EventCompositeHistoryView.get(resourceComposite));

        this.contentDeployed
            .setCanvas(new FullHTMLPane("/rhq/resource/content/view-plain.xhtml?id=" + resource.getId()));
        this.contentNew.setCanvas(new FullHTMLPane("/rhq/resource/content/deploy-plain.xhtml?id=" + resource.getId()));
        this.contentSubscrip.setCanvas(new FullHTMLPane("/rhq/resource/content/subscription-plain.xhtml?id="
            + resource.getId()));
        this.contentHistory.setCanvas(new FullHTMLPane("/rhq/resource/content/history-plain.xhtml?id="
            + resource.getId()));

        updateTabEnablement();
    }

    private void updateTabEnablement() {
        ResourcePermission permissions = this.resourceComposite.getResourcePermission();
        Set<ResourceTypeFacet> facets = this.resourceComposite.getResourceFacets().getFacets();

        // Summary, Monitoring, Inventory, and Alerts tabs are always enabled.

        monitoringTab.setSubTabEnabled(monitorTraits.getLocatorId(), hasTraits(this.resourceComposite));
        monitoringTab.setSubTabEnabled(monitorCallTime.getLocatorId(), facets.contains(ResourceTypeFacet.CALL_TIME));

        inventoryTab.setSubTabEnabled(inventoryConn.getLocatorId(), facets
            .contains(ResourceTypeFacet.PLUGIN_CONFIGURATION));
        ResourceType type = this.resourceComposite.getResource().getResourceType();
        inventoryTab.setSubTabEnabled(inventoryChildren.getLocatorId(), !type.getChildResourceTypes().isEmpty());

        getTabSet().setTabEnabled(this.operationsTab, facets.contains(ResourceTypeFacet.OPERATION));
        getTabSet().setTabEnabled(this.configurationTab,
            facets.contains(ResourceTypeFacet.CONFIGURATION) && permissions.isConfigureRead());
        getTabSet().setTabEnabled(this.eventsTab, facets.contains(ResourceTypeFacet.EVENT));
        getTabSet().setTabEnabled(this.contentTab, facets.contains(ResourceTypeFacet.CONTENT));
    }

    public Integer getSelectedItemId() {
        return this.resourceId;
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
                        new Message("Resource with id [" + resourceId + "] does not exist or is not accessible.",
                            Message.Severity.Warning));

                    CoreGUI.goToView(InventoryView.VIEW_PATH);
                }

                public void onSuccess(PageList<ResourceComposite> result) {
                    if (result.isEmpty()) {
                        //noinspection ThrowableInstanceNeverThrown
                        onFailure(new Exception("Resource with id [" + resourceId + "] does not exist."));
                    } else {
                        final ResourceComposite resourceComposite = result.get(0);
                        loadResourceType(resourceComposite, viewPath);
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

    private static boolean hasTraits(ResourceComposite resourceComposite) {
        ResourceType type = resourceComposite.getResource().getResourceType();
        Set<MeasurementDefinition> metricDefs = type.getMetricDefinitions();
        for (MeasurementDefinition metricDef : metricDefs) {
            if (metricDef.getDataType() == DataType.TRAIT) {
                return true;
            }
        }
        return false;
    }
}
