/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client;

import org.rhq.enterprise.gui.coregui.client.admin.roles.RolesView;
import org.rhq.enterprise.gui.coregui.client.admin.users.UsersView;

/**
 * @author Greg Hinkle
 */
public class LinkManager {

    private static boolean GWT = true;

    public static String getResourceLink(int resourceId) {
        if (GWT) {
            return "#Resource/" + resourceId + "/Summary/Overview";
        } else {
            return "/rhq/resource/summary/overview.xhtml?id=" + resourceId;
        }
    }

    public static String getResourceGroupLink(int groupId) {
        if (GWT) {
            return "#ResourceGroup/" + groupId;
        } else {
            return "/rhq/group/inventory/view.xhtml?groupId=" + groupId;
        }
    }

    public static String getGroupPluginConfigurationUpdateHistoryLink(int groupId) {
        return getResourceGroupLink(groupId) + "/Inventory/ConnectionSettingsHistory";
    }

    public static String getGroupDefinitionLink(int groupDefinitionId) {
        if (GWT) {
            return "#Inventory/Groups/DynaGroupDefinitions/" + groupDefinitionId;
        } else {
            return "/rhq/definition/group/view.xhtml?groupDefinitionId=" + groupDefinitionId;
        }
    }

    public static String getUserLink(int subjectId) {
        if (GWT) {
            return "#" + UsersView.VIEW_PATH + "/" + subjectId;
        } else {
            return "/admin/user/UserAdmin.do?mode=view&u=" + subjectId;
        }
    }

    public static String getRoleLink(int roleId) {
        if (GWT) {
            return "#" + RolesView.VIEW_PATH + "/" + roleId;
        } else {
            return "/admin/role/RoleAdmin.do?mode=view&r=" + roleId;
        }
    }

    public static String getSubsystemConfigurationLink() {
        return "/rhq/subsystem/configurationUpdate.xhtml";
    }

    public static String getSubsystemSuspectMetricsLink() {
        return "/rhq/subsystem/oobHistory.xhtml";
    }

    public static String getSubsystemOperationHistoryLink() {
        return "/rhq/subsystem/operationHistory.xhtml";
    }

    public static String getSubsystemAlertHistoryLink() {
        return "/rhq/subsystem/alertHistory.xhtml";
    }

    public static String getSubsystemAlertDefsLink() {
        return "/rhq/subsystem/alertDefinitions.xhtml";
    }

    public static String getAutodiscoveryQueueLink() {
        if (GWT) {
            return "#Administration/Security/Auto%20Discovery%20Queue";
        } else {
            return "/rhq/discovery/queue.xhtml";
        }
    }

    public static String getDashboardLink() {
        if (GWT) {
            return "#Dashboard";
        } else {
            return "/Dashboard.do";
        }
    }

    public static String getHubAllResourcesLink() {
        if (GWT) {
            return "#Inventory";
        } else {
            return "/rhq/inventory/browseResources.xhtml?subtab=all";
        }
    }

    public static String getHubPlatformsLink() {
        if (GWT) {
            return "#Inventory/Platforms";
        } else {
            return "/rhq/inventory/browseResources.xhtml?subtab=platform";
        }
    }

    public static String getHubServerssLink() {
        if (GWT) {
            return "#Inventory/Serers";
        } else {
            return "/rhq/inventory/browseResources.xhtml?subtab=server";
        }
    }

    public static String getHubServicesLink() {
        if (GWT) {
            return "#Inventory/Services";
        } else {
            return "/rhq/inventory/browseResources.xhtml?subtab=service";
        }
    }

    public static String getSavedSearchLink(int searchId) {
        return "/rhq/inventory/browseResources.xhtml?subtab=all&amp;searchId=" + searchId;
    }

    public static String getHubAllGroupsLink() {
        return "/rhq/inventory/browseGroups.xhtml?subtab=all";
    }

    public static String getHubCompatibleGroupsLink() {
        return "/rhq/inventory/browseGroups.xhtml?subtab=compatible";
    }

    public static String getHubMixedGroupsLink() {
        return "/rhq/inventory/browseGroups.xhtml?subtab=mixed";
    }

    public static String getHubGroupDefinitionsLink() {
        if (GWT) {
            return "#Inventory/Groups/DynaGroupDefinitions";
        } else {
            return "/rhq/definition/group/list.xhtml";
        }
    }

    public static String getHubNewGroupLink() {
        return "/resource/group/Inventory.do?mode=new";
    }

    public static String getHubNewGroupDefLink() {
        return "/rhq/definition/group/new.xhtml";
    }

    public static String getAdminUsersLink() {
        if (GWT) {
            return "#Administration/Security/Manage Users";
        } else {
            return "/admin/user/UserAdmin.do?mode=list";
        }
    }

    public static String getAdminRolesLink() {
        if (GWT) {
            return "#Administration/Security/Manage Roles";
        } else {
            return "/admin/role/RoleAdmin.do?mode=list";
        }
    }

    public static String getAdminSysConfigLink() {
        if (GWT) {
            return "#Administration/Configuration/System Settings";
        } else {
            return "/admin/config/Config.do?mode=edit";
        }
    }

    public static String getAdminPluginsLink() {
        if (GWT) {
            return "#Administration/Configuration/Plugins";
        } else {
            return "/rhq/admin/plugin/plugin-list.xhtml";
        }
    }

    public static String getAdminTemplatesLink() {
        if (GWT) {
            return "#Administration/Configuration/Templates";
        } else {
            return "/admin/config/EditDefaults.do?mode=monitor&amp;viewMode=all";
        }
    }

    public static String getAdminAlertNotifTemplatesLink() {
        return "/rhq/admin/alert/template/notification/list.xhtml";
    }

    public static String getAdminContentProvidersLink() {
        return "/rhq/content/listContentProviders.xhtml";
    }

    public static String getAdminContentReposLink() {
        return "/rhq/content/listRepos.xhtml";
    }

    public static String getHAServersLink() {
        if (GWT) {
            return "#Administration/Cluster/Servers";
        } else {
            return "/rhq/ha/listServers.xhtml";
        }
    }

    public static String getHAAgentsLink() {
        if (GWT) {
            return "#Administration/Cluster/Agents";
        } else {
            return "/rhq/ha/listAgents.xhtml";
        }
    }

    public static String getHAAffinityGroupsLink() {
        if (GWT) {
            return "#Administration/Cluster/Affinity Groups";
        } else {
            return "/rhq/ha/listAffinityGroups.xhtml";
        }
    }

    public static String getHAEventsLink() {
        if (GWT) {
            return "#Administration/Cluster/Partition Events";
        } else {
            return "/rhq/ha/listPartitionEvents.xhtml";
        }
    }

    public static String getReportsInventoryLink() {
        if (GWT) {
            return "#Administration/Reports/Inventory Summary";
        } else {
            return "/rhq/admin/report/resourceInstallReport.xhtml";
        }
    }

    public static String getAdminDownloadsLink() {
        return "/rhq/admin/downloads.xhtml";
    }

    public static String getAdminLicenseLink() {
        if (GWT) {
            return "#Administration/Configuration/License";
        } else {
            return "/admin/license/LicenseAdmin.do?mode=view";
        }
    }

    public static String getDebugSqlLink() {
        return "/admin/test/sql.jsp";
    }

    public static String getDebugHibernateLink() {
        return "/admin/test/hibernate.jsp";
    }

    public static String getDebugBrowserLink() {
        return "/admin/test/browser.jsp";
    }

    public static String getUserPrefsLink(int subjectId) {
        return "/admin/user/UserAdmin.do?mode=edit&amp;u=" + subjectId;
    }

    public static String getUserPasswordLink(int subjectId) {
        return "/admin/user/UserAdmin.do?mode=editPass&amp;u=" + subjectId;
    }

    public static String getTagLink(String tag) {
        return "#Reports/Subsystems/Tags/" + tag;
    }
}
