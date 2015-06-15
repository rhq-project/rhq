/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.coregui.client;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.coregui.client.admin.roles.RolesView;
import org.rhq.coregui.client.admin.templates.DriftDefinitionTemplateTypeView;
import org.rhq.coregui.client.admin.users.UsersView;
import org.rhq.coregui.client.components.table.StringIDTableSection;
import org.rhq.coregui.client.drift.DriftDefinitionsView;

/**
 * @author Greg Hinkle
 */
public class LinkManager {

    private static boolean GWT = true;

    public static String getResourceLink(int resourceId) {
        if (GWT) {
            return "#Resource/" + resourceId;
        } else {
            return "/portal/rhq/resource/summary/overview.xhtml?id=" + resourceId;
        }
    }

    public static String getResourceTabLink(int resourceId, String tabName, String subTabName) {
        if (GWT) {
            return "#Resource/" + resourceId + "/" + tabName + ((null == subTabName) ? "" : ("/" + subTabName));
        } else {
            return "/portal/rhq/resource/summary/overview.xhtml?id=" + resourceId;
        }
    }

    public static String getResourceGroupLink(int groupId) {
        if (GWT) {
            return "#ResourceGroup/" + groupId;
        } else {
            return "/portal/rhq/group/inventory/view.xhtml?groupId=" + groupId;
        }
    }

    public static String getResourceGroupLink(ResourceGroup group) {
        return getResourceOrGroupLink(EntityContext.forGroup(group));
    }

    private static String getAutoGroupTabLink(int autoGroupId, String tabName, String subTabName) {
        if (GWT) {
            return "#Resource/AutoGroup/" + autoGroupId + "/" + tabName
                + ((null == subTabName) ? "" : ("/" + subTabName));
        } else {
            return "/portal/rhq/group/inventory/view.xhtml?groupId=" + autoGroupId;
        }
    }

    public static String getAutoGroupLink(int autoGroupId) {
        return "#Resource/AutoGroup/" + autoGroupId;
    }

    private static String getAutoClusterTabLink(int autoClusterGroupId, String tabName, String subTabName) {
        if (GWT) {
            return "#ResourceGroup/AutoCluster/" + autoClusterGroupId + "/" + tabName
                + ((null == subTabName) ? "" : ("/" + subTabName));
        } else {
            return "/portal/rhq/group/inventory/view.xhtml?groupId=" + autoClusterGroupId;
        }
    }

    private static String getAutoClusterLink(int autoClusterGroupId) {
        return "#ResourceGroup/AutoCluster/" + autoClusterGroupId;
    }

    private static String getResourceGroupTabLink(int resourceGroupId, String tabName, String subTabName) {
        if (GWT) {
            return "#ResourceGroup/" + resourceGroupId + "/" + tabName
                + ((null == subTabName) ? "" : ("/" + subTabName));
        } else {
            return "/portal/rhq/group/inventory/view.xhtml?groupId=" + resourceGroupId;
        }
    }

    public static String getEntityTabLink(EntityContext entityContext, String tabName, String subTabName) {
        String link;
        switch (entityContext.getType()) {
        case Resource:
            link = getResourceTabLink(entityContext.getResourceId(), tabName, subTabName);
            break;
        case ResourceGroup:
            if (entityContext.isAutoGroup()) {
                link = getAutoGroupTabLink(entityContext.getGroupId(), tabName, subTabName);
            } else if (entityContext.isAutoCluster()) {
                link = getAutoClusterTabLink(entityContext.getGroupId(), tabName, subTabName);
            } else {
                link = getResourceGroupTabLink(entityContext.getGroupId(), tabName, subTabName);
            }
            break;
        case SubsystemView:
            if (tabName.equals("Alerts") && subTabName.equals("Definitions")) {
                link = "#Reports/Subsystems/AlertDefinitions";
            } else if (tabName.equals("Alerts") && subTabName.equals("History")) {
                link = "#Reports/Subsystems/RecentAlerts";
            } else if (tabName.equals("Operations") && subTabName.equals("History")) {
                link = "#Reports/Subsystems/RecentOperations";
            } else if (tabName.equals("Configuration") && subTabName.equals("History")) {
                link = "#Reports/Subsystems/ConfigurationHistoryView";
            } else {
                throw new IllegalArgumentException("Subsystem link not supported for tab " + tabName + ">" + subTabName
                    + ".");
            }
            break;
        default:
            throw new IllegalArgumentException("Unsupported entity context type: " + entityContext.getType());
        }
        return link;
    }

    private static String getResourceOrGroupLink(EntityContext entityContext) {
        switch (entityContext.getType()) {
        case Resource:
            return getResourceLink(entityContext.getResourceId());
        case ResourceGroup:
            if (entityContext.isAutoGroup()) {
                return getAutoGroupLink(entityContext.getGroupId());
            } else if (entityContext.isAutoCluster()) {
                return getAutoClusterLink(entityContext.getGroupId());
            } else {
                return getResourceGroupLink(entityContext.getGroupId());
            }
        case SubsystemView:
            throw new IllegalArgumentException("Subsystems are not supported for simple entity context links.");
        default:
            throw new IllegalArgumentException("Unsupported entity context type: " + entityContext);
        }
    }

    public static String getGroupPluginConfigurationUpdateHistoryLink(EntityContext group, Integer groupUpdateHistoryId) {
        if (groupUpdateHistoryId != null) {
            return getEntityTabLink(group, "Inventory", "ConnectionSettingsHistory") + "/" + groupUpdateHistoryId;
        } else {
            return getEntityTabLink(group, "Inventory", "ConnectionSettingsHistory");
        }
    }

    public static String getGroupResourceConfigurationUpdateHistoryLink(EntityContext group,
        Integer groupUpdateHistoryId) {
        if (groupUpdateHistoryId != null) {
            return getEntityTabLink(group, "Configuration", "History") + "/" + groupUpdateHistoryId;
        } else {
            return getEntityTabLink(group, "Configuration", "History");
        }
    }

    public static String getGroupOperationHistoryLink(ResourceGroup group, int groupOperationHistoryId) {
        return getEntityTabLink(EntityContext.forGroup(group), "Operations", "History") + "/" + groupOperationHistoryId;
    }

    public static String getResourceEventHistoryListLink(int resourceId) {
        return "#Resource/" + resourceId + "/Events/History/";
    }

    public static String getGroupEventHistoryListLink(EntityContext group) {
        return getEntityTabLink(group, "Events", "History");
    }

    public static String getResourceMonitoringGraphsLink(int resourceId) {
        return "#Resource/" + resourceId + "/Monitoring/Graphs/";
    }

    public static String getGroupMonitoringGraphsLink(EntityContext group) {
        return getEntityTabLink(group, "Monitoring", "Graphs");
    }

    public static String getAllResourcesLink() {
        return "#Inventory/Resources/AllResources";
    }

    public static String getGroupDefinitionLink(int groupDefinitionId) {
        if (GWT) {
            return "#Inventory/Groups/DynagroupDefinitions/" + groupDefinitionId;
        } else {
            return "/portal/rhq/definition/group/view.xhtml?groupDefinitionId=" + groupDefinitionId;
        }
    }

    public static String getUserLink(int subjectId) {
        if (GWT) {
            return "#" + UsersView.VIEW_PATH + "/" + subjectId;
        } else {
            return "/portal/admin/user/UserAdmin.do?mode=view&u=" + subjectId;
        }
    }

    public static String getRoleLink(int roleId) {
        if (GWT) {
            return "#" + RolesView.VIEW_PATH + "/" + roleId;
        } else {
            return "/portal/admin/role/RoleAdmin.do?mode=view&r=" + roleId;
        }
    }

    public static String getSubsystemConfigurationLink() {
        return "/portal/rhq/subsystem/configurationUpdate.xhtml";
    }

    public static String getSubsystemSuspectMetricsLink() {
        return "/portal/rhq/subsystem/oobHistory.xhtml";
    }

    public static String getSubsystemResourceOperationHistoryLink(int resourceId, int opHistoryId) {
        String link;
        if (GWT) {
            link = "#Resource/" + resourceId + "/Operations/History/" + opHistoryId;
        } else {
            link = "/portal/rhq/resource/operation/resourceOperationHistoryDetails-plain.xhtml?id=" + resourceId + "&opId="
                + opHistoryId;
        }
        return link;
    }

    public static String getSubsystemResourceOperationScheduleLink(int resourceId, int opScheduleId) {
        String link;
        if (GWT) {
            link = "#Resource/" + resourceId + "/Operations/Schedules/" + opScheduleId;
        } else {
            link = "/portal/rhq/resource/operation/resourceOperationScheduleDetails-plain.xhtml?id=" + resourceId + "&opId="
                + opScheduleId;

        }
        return link;
    }

    public static String getSubsystemGroupOperationScheduleLink(int groupId, int opScheduleId) {
        return "#ResourceGroup/" + groupId + "/Operations/Schedules/" + opScheduleId;
    }

    public static String getAutoGroupOperationScheduleLink(int groupId, int opScheduleId) {
        return getAutoGroupLink(groupId) + "/Operations/Schedules/" + opScheduleId;
    }

    public static String getAlertDetailLink(EntityContext entityContext, int alertId) {
        String link;
        String baseLink = getEntityTabLink(entityContext, "Alerts", "History");
        link = baseLink + "/" + alertId;

        return link;
    }

    public static String getEventDetailLink(int resourceId, int eventId) {
        String link = "#Resource/" + resourceId + "/Events/History/" + eventId;
        return link;
    }

    public static String getSubsystemAlertDefinitionLink(int resourceId, int alertDefinitionId) {
        String link;
        if (GWT) {
            link = "#Resource/" + resourceId + "/Alerts/Definitions/" + alertDefinitionId;
        } else {
            link = "/portal/rhq/subsystem/alertDefinitions.xhtml";
        }

        return link;
    }
    
    public static String getStorageNodeLink(int storageNodeId) {
        return "#Administration/Topology/StorageNodes/" + storageNodeId;
    }

    public static String getAutodiscoveryQueueLink() {
        if (GWT) {
            return "#Inventory/Resources/AutodiscoveryQueue";
        } else {
            return "/portal/rhq/discovery/queue.xhtml";
        }
    }

    public static String getDashboardsLink() {
        if (GWT) {
            return "#Dashboards";
        } else {
            return "/portal/Dashboard.do";
        }
    }

    public static String getDashboardLink(int dashboardId) {
        if (GWT) {
            return "#Dashboards/" + dashboardId;
        } else {
            return "/portal/Dashboard.do";
        }
    }

    public static String getHubAllResourcesLink() {
        if (GWT) {
            return "#Inventory";
        } else {
            return "/portal/rhq/inventory/browseResources.xhtml?subtab=all";
        }
    }

    public static String getHubPlatformsLink() {
        if (GWT) {
            return "#Inventory/Resources/Platforms";
        } else {
            return "/portal/rhq/inventory/browseResources.xhtml?subtab=platform";
        }
    }

    public static String getHubServersLink() {
        if (GWT) {
            return "#Inventory/Resources/Servers";
        } else {
            return "/portal/rhq/inventory/browseResources.xhtml?subtab=server";
        }
    }
    public static String getHubServicesLink() {
        if (GWT) {
            return "#Inventory/Resources/Services";
        } else {
            return "/portal/rhq/inventory/browseResources.xhtml?subtab=service";
        }
    }

    public static String getSavedSearchLink(int searchId) {
        return "/portal/rhq/inventory/browseResources.xhtml?subtab=all&amp;searchId=" + searchId;
    }

    public static String getHubAllGroupsLink() {
        return "/portal/rhq/inventory/browseGroups.xhtml?subtab=all";
    }

    public static String getHubCompatibleGroupsLink() {
        return "#Inventory/Groups/CompatibleGroups";
    }

    public static String getHubMixedGroupsLink() {
        return "#Inventory/Groups/MixedGroups";
    }

    public static String getHubGroupDefinitionsLink() {
        if (GWT) {
            return "#Inventory/Groups/DynagroupDefinitions";
        } else {
            return "/portal/rhq/definition/group/list.xhtml";
        }
    }

    public static String getHubNewGroupLink() {
        return "/portal/resource/group/Inventory.do?mode=new";
    }

    public static String getHubNewGroupDefLink() {
        return "/portal/rhq/definition/group/new.xhtml";
    }

    public static String getAdminUsersLink() {
        if (GWT) {
            return "#Administration/Security/Users";
        } else {
            return "/portal/admin/user/UserAdmin.do?mode=list";
        }
    }

    public static String getAdminRolesLink() {
        if (GWT) {
            return "#Administration/Security/Roles";
        } else {
            return "/portal/admin/role/RoleAdmin.do?mode=list";
        }
    }

    public static String getAdminSysConfigLink() {
        if (GWT) {
            return "#Administration/Configuration/SystemSettings";
        } else {
            return "/portal/admin/config/Config.do?mode=edit";
        }
    }

    public static String getAdminPluginsLink() {
        if (GWT) {
            return "#Administration/Configuration/Plugins";
        } else {
            return "/portal/rhq/admin/plugin/plugin-list.xhtml";
        }
    }

    public static String getAdminTemplatesLink(String viewName) {
        if (GWT) {
            return "#Administration/Configuration/" + viewName;
        } else {
            return "/portal/admin/config/EditDefaults.do?mode=monitor&amp;viewMode=all";
        }
    }

    public static String getAdminTemplatesEditLink(String viewName, int typeId) {
        if (GWT) {
            return "#Administration/Configuration/" + viewName + "/" + typeId;
        } else {
            return "/portal/admin/config/EditDefaults.do?mode=monitor&amp;viewMode=all";
        }
    }

    public static String getAdminAlertNotifTemplatesLink() {
        return "/portal/rhq/admin/alert/template/notification/list.xhtml";
    }

    public static String getAdminContentProvidersLink() {
        return "/portal/rhq/content/listContentProviders.xhtml";
    }

    public static String getAdminContentReposLink() {
        return "/portal/rhq/content/listRepos.xhtml";
    }

    public static String getHAServersLink() {
        if (GWT) {
            return "#Administration/Cluster/Servers";
        } else {
            return "/portal/rhq/ha/listServers.xhtml";
        }
    }

    public static String getHAAgentsLink() {
        if (GWT) {
            return "#Administration/Cluster/Agents";
        } else {
            return "/portal/rhq/ha/listAgents.xhtml";
        }
    }

    public static String getHAAffinityGroupsLink() {
        if (GWT) {
            return "#Administration/Cluster/Affinity Groups";
        } else {
            return "/portal/rhq/ha/listAffinityGroups.xhtml";
        }
    }

    public static String getHAEventsLink() {
        if (GWT) {
            return "#Administration/Cluster/Partition Events";
        } else {
            return "/portal/rhq/ha/listPartitionEvents.xhtml";
        }
    }

    public static String getReportsInventoryLink() {
        if (GWT) {
            return "#Administration/Reports/Inventory Summary";
        } else {
            return "/portal/rhq/admin/report/resourceInstallReport.xhtml";
        }
    }

    public static String getAdminDownloadsLink() {
        if (GWT) {
            return "#Administration/Configuration/Downloads";
        } else {
            return "/portal/rhq/admin/downloads.xhtml";
        }
    }

    public static String getDebugSqlLink() {
        return "/portal/admin/test/sql.jsp";
    }

    public static String getDebugHibernateLink() {
        return "/portal/admin/test/hibernate.jsp";
    }

    public static String getDebugBrowserLink() {
        return "/portal/admin/test/browser.jsp";
    }

    public static String getUserPrefsLink(int subjectId) {
        return "/portal/admin/user/UserAdmin.do?mode=edit&amp;u=" + subjectId;
    }

    public static String getUserPasswordLink(int subjectId) {
        return "/portal/admin/user/UserAdmin.do?mode=editPass&amp;u=" + subjectId;
    }

    public static String getTagLink(String tag) {
        if (tag == null) {
            return "#Reports/Subsystems/Tags";
        } else {
            return "#Reports/Subsystems/Tags/" + tag;
        }
    }

    public static String getBundleLink(int bundleId) {
        return "#Bundles/Bundle/" + bundleId;
    }

    public static String getBundleGroupLink(int bundleGroupId) {
        return "#Bundles/BundleGroup/" + bundleGroupId;
    }

    public static String getBundleVersionLink(int bundleId, int bundleVersionId) {
        return "#Bundles/Bundle/" + bundleId + "/versions" + (bundleVersionId == 0 ? "" : ("/" + bundleVersionId));
    }

    public static String getBundleDestinationLink(int bundleId, int bundleDestinationId) {
        return "#Bundles/Bundle/" + bundleId + "/destinations"
            + (bundleDestinationId == 0 ? "" : ("/" + bundleDestinationId));
    }

    public static String getBundleDeploymentLink(int bundleId, int bundleDeploymentId) {
        return "#Bundles/Bundle/" + bundleId + "/deployments/" + bundleDeploymentId;
    }

    public static String getDriftDefinitionsLink(int resourceId) {
        return "#Resource/" + resourceId + "/Drift/Definitions";
    }

    public static String getDriftDefinitionCarouselLink(int resourceId, int driftDefId) {
        return "#Resource/" + resourceId + "/Drift/Definitions/" + driftDefId + "/"
            + DriftDefinitionsView.DetailView.Carousel.name();
    }

    public static String getDriftDefinitionEditLink(int resourceId, int driftDefId) {
        return "#Resource/" + resourceId + "/Drift/Definitions/" + driftDefId + "/"
            + DriftDefinitionsView.DetailView.Edit.name();
    }

    public static String getDriftDefinitionInitialSnapshotLink(int resourceId, int driftDefId) {
        return "#Resource/" + resourceId + "/Drift/Definitions/" + driftDefId + "/"
            + DriftDefinitionsView.DetailView.InitialSnapshot.name();
    }

    public static String getDriftCarouselDriftLink(int resourceId, int driftDefId, String driftId) {
        if (!driftId.startsWith(StringIDTableSection.ID_PREFIX)) {
            driftId = StringIDTableSection.ID_PREFIX + driftId;
        }
        return "#Resource/" + resourceId + "/Drift/Definitions/" + driftDefId + "/Drift/" + driftId;
    }

    public static String getDriftCarouselSnapshotLink(int resourceId, int driftDefId, int version) {
        return "#Resource/" + resourceId + "/Drift/Definitions/" + driftDefId + "/Snapshot/" + version;
    }

    public static String getDriftTemplateLink(int typeId, int templateId) {
        String result = getAdminTemplatesEditLink(DriftDefinitionTemplateTypeView.VIEW_ID.getName(), typeId);

        return result + "/" + templateId;
    }

    public static String getDriftTemplateSnapshotLink(int typeId, int templateId) {
        String result = getDriftTemplateLink(typeId, templateId);

        return result + "/Snapshot";
    }

    public static String getHelpLink() {
        return "#Help";
    }

    /** 
     * Return an href element for links
     * 
     * @param url the target url
     * @param value the display value for the link
     * @return
     */
    static public String getHref(String url, String value) {
        String result = "<a href=\"" + url + "\">" + value + "</a>";
        return result;
    }

}
