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
import org.rhq.enterprise.gui.coregui.client.components.FullHTMLPane;
import org.rhq.enterprise.gui.coregui.client.components.tab.SubTab;
import org.rhq.enterprise.gui.coregui.client.components.tab.TwoLevelTab;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.AbstractTwoLevelTabSetView;
import org.rhq.enterprise.gui.coregui.client.inventory.common.event.EventCompositeHistoryView;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.schedules.SchedulesView;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.traits.TraitsView;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.summary.OverviewView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceSearchView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;

/**
 * Right panel of the group view.
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class ResourceGroupDetailView extends AbstractTwoLevelTabSetView<ResourceGroupComposite, ResourceGroupTitleBar> {
    private static final String BASE_VIEW_PATH = "ResourceGroup";

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
    private SubTab opHistory;
    private SubTab opSched;
    private SubTab alertHistory;
    private SubTab alertDef;
    private SubTab configCurrent;
    private SubTab configHistory;
    private SubTab eventHistory;

    public ResourceGroupDetailView(String locatorId) {
        super(locatorId, BASE_VIEW_PATH);
    }

    @Override
    public Integer getSelectedItemId() {
        return this.groupId;
    }

    @Override
    protected ResourceGroupTitleBar createTitleBar() {
        return new ResourceGroupTitleBar(getLocatorId());
    }

    protected List<TwoLevelTab> createTabs() {
        List<TwoLevelTab> tabs = new ArrayList<TwoLevelTab>();

        summaryTab = new TwoLevelTab(getTabSet().extendLocatorId("Summary"), "Summary", "/images/icons/Service_up_16.png");
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
        inventoryTab.registerSubTabs(this.inventoryMembers, this.inventoryConn);
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

    protected void updateTabContent(ResourceGroupComposite groupComposite) {
        this.groupComposite = groupComposite;

        getTitleBar().setGroup(groupComposite.getResourceGroup());

        for (Tab top : this.getTabSet().getTabs()) {
            ((TwoLevelTab) top).getLayout().destroyViews();
        }

        //        FullHTMLPane timelinePane = new FullHTMLPane("/rhq/resource/summary/timeline-plain.xhtml?id=" + resource.getId());
        //        summaryTab.updateSubTab("Overview", new DashboardView(resource));
        //        summaryTab.updateSubTab("Timeline", timelinePane);
        this.summaryOverview.setCanvas(new OverviewView(this.groupComposite));

        int groupId = this.groupComposite.getResourceGroup().getId();

        this.monitorGraphs.setCanvas(new FullHTMLPane("/rhq/group/monitor/graphs-plain.xhtml?groupId=" + groupId));
        this.monitorTables.setCanvas(new FullHTMLPane("/rhq/group/monitor/tables-plain.xhtml?groupId=" + groupId));
        this.monitorTraits.setCanvas(new TraitsView(this.monitoringTab.extendLocatorId("TraitsView"), groupId));
        this.monitorSched.setCanvas(new SchedulesView(this.monitoringTab.extendLocatorId("SchedulesView"), groupId));
        this.monitorCallTime.setCanvas(new FullHTMLPane("/rhq/group/monitor/response-plain.xhtml?groupId=" + groupId));

        this.inventoryMembers.setCanvas(ResourceSearchView.getMembersOf(groupId));
        // TODO: Uncomment this once the group config component is done.
        //this.inventoryConn.setCanvas(new GroupPluginConfigurationEditView(this.group.getId(), this.group.getResourceType().getId(), ConfigurationEditor.ConfigType.plugin));

        this.opHistory.setCanvas(new FullHTMLPane("/rhq/group/operation/groupOperationHistory-plain.xhtml?groupId="
                + groupId));
        this.opSched.setCanvas(new FullHTMLPane("/rhq/group/operation/groupOperationSchedules-plain.xhtml?groupId="
                + groupId));

        this.alertHistory.setCanvas(new FullHTMLPane("/rhq/group/alert/listGroupAlertHistory-plain.xhtml?groupId="
                + groupId));
        this.alertDef.setCanvas(new FullHTMLPane("/rhq/group/alert/listGroupAlertDefinitions-plain.xhtml?groupId="
                + groupId));

        this.configCurrent.setCanvas(new FullHTMLPane("/rhq/group/configuration/viewCurrent-plain.xhtml?groupId="
                + groupId));
        this.configHistory
                .setCanvas(new FullHTMLPane("/rhq/group/configuration/history-plain.xhtml?groupId=" + groupId));

        this.eventHistory.setCanvas(EventCompositeHistoryView.get(groupComposite));

        updateTabEnablement();
    }

    private void updateTabEnablement() {
        GroupCategory groupCategory = groupComposite.getResourceGroup().getGroupCategory();
        Set<ResourceTypeFacet> facets = groupComposite.getResourceFacets().getFacets();

        // Summary and Inventory tabs are always enabled.

        // Only enable "Call Time" and "Traits" subtabs for groups that implement them.
        this.monitoringTab.setSubTabEnabled(this.monitorTraits.getLocatorId(), hasTraits(this.groupComposite));
        this.monitoringTab.setSubTabEnabled(this.monitorCallTime.getLocatorId(), facets.contains(ResourceTypeFacet.CALL_TIME));

        // Inventory>Connection Settings subtab is only enabled for compat groups that define conn props.
        this.inventoryTab.setSubTabEnabled(this.inventoryConn.getLocatorId(), groupCategory == GroupCategory.COMPATIBLE
                && facets.contains(ResourceTypeFacet.PLUGIN_CONFIGURATION));

        // Monitoring and Alerts tabs are always enabled for compatible groups and always disabled for mixed groups.
        // TODO (ips): Break out mixed groups into a separate view.
        getTabSet().setTabEnabled(this.monitoringTab, groupCategory == GroupCategory.COMPATIBLE);
        getTabSet().setTabEnabled(this.alertsTab, groupCategory == GroupCategory.COMPATIBLE);

        // Operations tab is only enabled for compatible groups of a type that supports the Operations facet.
        getTabSet().setTabEnabled(this.operationsTab, facets.contains(ResourceTypeFacet.OPERATION));

        // Configuration tab is only enabled for compatible groups of a type that supports the Configuration facet
        // and when the current user has the CONFIGURE_READ permission.
        getTabSet().setTabEnabled(this.configurationTab, facets.contains(ResourceTypeFacet.CONFIGURATION)
                && this.permissions.isConfigureRead());

        // Events tab is only enabled for compatible groups of a type that supports the Events facet.
        getTabSet().setTabEnabled(this.eventsTab, facets.contains(ResourceTypeFacet.EVENT));
    }

    protected void loadSelectedItem(final int groupId, final ViewPath viewPath) {
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
                                            updateTabContent(groupComposite);
                                            selectTab(getTabName(), getSubTabName(), viewPath);
                                        }
                                    });
                        }
                    });
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
