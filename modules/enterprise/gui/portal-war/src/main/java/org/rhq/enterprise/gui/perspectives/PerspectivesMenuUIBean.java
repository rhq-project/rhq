/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.gui.perspectives;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 *  Bean dynamically provides menu information used by menu.xhtml.  This bean should be viewed 
 *  concurrently with menu.xhtml and it's designed to provide static or dynamic menu content 
 *  for menu.xhtml.
 * 
 * @author Simeon Pinder
 *
 */
public class PerspectivesMenuUIBean {

    //Stores reference to the root menu item
    private MenuItem CoreMenuBar = null;

    /* Stores all mapped MenuItem information */
    private HashMap<String, MenuItem> menuDefinition = new HashMap<String, MenuItem>();

    // provides MenuItem mapping lookup via bean accessor
    private MenuItem extensionPoint = null;

    {//initialization block
        //Root of core menu
        MenuItem menu = new MenuItem();
        menu.setExtensionKey(ExtensionPoints.menu);
        CoreMenuBar = menu;
        menuDefinition.put(ExtensionPoints.menu, menu);

        List<MenuItem> attachPoints = menu.getChildMenuItems();

        /*===== LOGO TO HOME PAGE =====*/
        MenuItem logoBefore = new MenuItem("", "", "", menu.getChildMenuItems(), ExtensionPoints.logo_BEFORE,
            menuDefinition);
        logoBefore.setRendered(false);
        MenuItem logo = new MenuItem("/images/JBossLogo_small.png", "", "", menu.getChildMenuItems(),
            ExtensionPoints.logo, menuDefinition);
        logo.setIconHeight(21);
        logo.setIconWidth(37);
        MenuItem logoAfter = new MenuItem("", "", "", menu.getChildMenuItems(), ExtensionPoints.logo_AFTER,
            menuDefinition);
        logoAfter.setRendered(false);
        /*===== OVERVIEW =====*/
        MenuItem overview = new MenuItem("", "", "Overview", menu.getChildMenuItems(), ExtensionPoints.overview,
            menuDefinition);
        /*===== Overview.BEFORE extension pt. Ignore same as Logo.after ============= */
        /*===== OVERVIEW.AFTER =====*/
        MenuItem overviewAfter = new MenuItem("", "", "", menu.getChildMenuItems(), ExtensionPoints.overview_AFTER,
            menuDefinition);
        overviewAfter.setRendered(false);

        /****** Overview > SYSTEM VIEWS ******/
        MenuItem subsystemViews = new MenuItem("", "", "Subsystem Views", overview.getChildMenuItems(),
            ExtensionPoints.subsystemViews, menuDefinition);
        List<MenuItem> subsystemMenuItems = new ArrayList<MenuItem>();
        {
            //SubsystemView menu items
            String[] imageNames = { "Configure", "Monitor_failed", "Operation", "Flag_blue", "Flag_grey" };
            String[] pageNames = { "configurationUpdate", "oobHistory", "operationHistory", "alertHistory",
                "alertDefinitions" };
            String[] menuNames = { "Configuration Changes", "Suspect Metrics", "Operations", "Alerts",
                "Alert Definitions" };
            for (int i = 0; i < imageNames.length; i++) {
                String extKey = null;
                String name = menuNames[i];
                if (name.indexOf("Configuration") > -1) {
                    extKey = ExtensionPoints.configurationChanges;
                }
                if (name.indexOf("Suspect") > -1) {
                    extKey = ExtensionPoints.suspectMetrics;
                }
                if (name.indexOf("Operations") > -1) {
                    extKey = ExtensionPoints.operations;
                }
                if (name.indexOf("Alerts") > -1) {
                    extKey = ExtensionPoints.alerts;
                }
                if (name.indexOf("Alert") > -1) {
                    extKey = ExtensionPoints.alertDefinitions;
                }
                MenuItem m = new MenuItem("/images/icons/" + imageNames[i] + "_16.png",//icon url 
                    "/rhq/subsystem/" + pageNames[i] + ".xhtml", // menu item url 
                    menuNames[i], subsystemViews.getChildMenuItems(), extKey, menuDefinition); //menu item name
            }
        }
        /* ***** Overview > AUTO-DISCOVERY QUEUE ***** */
        MenuItem autoDiscoveryQueue = new MenuItem("", "/rhq/discovery/queue.xhtml", "Auto Discovery Queue", overview
            .getChildMenuItems(), ExtensionPoints.autoDiscoveryQueue, menuDefinition);

        /* ***** Overview > DASHBOARD ***** */
        MenuItem dashboard = new MenuItem("", "/Dashboard.do", "Dashboard", overview.getChildMenuItems(),
            ExtensionPoints.dashboard, menuDefinition);

        /* ===== RESOURCES ===== */
        MenuItem resources = new MenuItem("", "", "Resources", menu.getChildMenuItems(), ExtensionPoints.resources,
            menuDefinition);
        /*===== RESOURCES.BEFORE extension pt. Ignore same as Overvie.after ============= */
        /*===== Resources.AFTER =====*/
        MenuItem resourcesAfter = new MenuItem("", "", "", menu.getChildMenuItems(), ExtensionPoints.resources_AFTER,
            menuDefinition);
        resourcesAfter.setRendered(false);

        /* Resources: Platform */
        MenuItem resourcesPlatform = new MenuItem("/images/icons/Platform_up_16.png",
            "/ResourceHub.do?resourceCategory=PLATFORM", "", resources.getChildMenuItems(), ExtensionPoints.platforms,
            menuDefinition);
        /* Resources: Server */
        MenuItem resourcesServer = new MenuItem("/images/icons/Server_up_16.png",
            "/ResourceHub.do?resourceCategory=SERVER", "", resources.getChildMenuItems(), ExtensionPoints.servers,
            menuDefinition);
        /* Resources: Service */
        MenuItem resourcesService = new MenuItem("/images/icons/Service_up_16.png",
            "/ResourceHub.do?resourceCategory=SERVICE", "", resources.getChildMenuItems(), ExtensionPoints.services,
            menuDefinition);
        // Menu bar separator
        MenuItem separator = new MenuItem("", "", "");
        separator.setMenuSeparator(true);
        resources.getChildMenuItems().add(separator);
        /* Resources: Favorites */
        MenuItem resourcesFavorite = new MenuItem("/images/icons/Favorite_16.png", "", "Favorite Resources", resources
            .getChildMenuItems(), ExtensionPoints.favoriteResources, menuDefinition);
        /* Resources: Recently Viewed */
        MenuItem resourcesRecentlyViewed = new MenuItem("/images/icons/History_16.png", "",
            "Recently Viewed Resources", resources.getChildMenuItems(), ExtensionPoints.recentlyViewedResources,
            menuDefinition);
        //ADD Menu bar separator
        resources.getChildMenuItems().add(separator);
        /* Resources: Search */
        MenuItem resourcesSearch = new MenuItem("", "", "Search: ", resources.getChildMenuItems(),
            ExtensionPoints.searchResources, menuDefinition);

        /* ===== GROUPS ===== */
        MenuItem groups = new MenuItem("", "", "Groups", menu.getChildMenuItems(), ExtensionPoints.groups,
            menuDefinition);
        /*===== Groups.BEFORE extension pt. Ignore same as Overvie.after ============= */
        /*===== Groups.AFTER =====*/
        MenuItem groupsAfter = new MenuItem("", "", "", menu.getChildMenuItems(), ExtensionPoints.groups_AFTER,
            menuDefinition);
        groupsAfter.setRendered(false);
        /* GROUPS: Compatible */
        MenuItem groupsCompatible = new MenuItem("/images/icons/Cluster_up_16.png",
            "/GroupHub.do?groupCategory=COMPATIBLE", "", groups.getChildMenuItems(), ExtensionPoints.compatible,
            menuDefinition);
        /* GROUPS: Mixed */
        MenuItem groupsMixed = new MenuItem("/images/icons/Group_up_16.png", "/GroupHub.do?groupCategory=MIXED", "",
            groups.getChildMenuItems(), ExtensionPoints.mixed, menuDefinition);
        /* GROUPS: Group Definitions */
        MenuItem groupsDefinitions = new MenuItem("/images/GroupDefinition_16.png", "/rhq/definition/group/list.xhtml",
            "", groups.getChildMenuItems(), ExtensionPoints.definitions, menuDefinition);
        /* GROUPS: New Group */
        MenuItem groupsNewResource = new MenuItem("", "/resource/group/Inventory.do?mode=new", "", groups
            .getChildMenuItems(), ExtensionPoints.newGroup, menuDefinition);
        /* GROUPS: New Group Definition */
        MenuItem groupsNewDefinition = new MenuItem("", "/rhq/definition/group/new.xhtml", "", groups
            .getChildMenuItems(), ExtensionPoints.newGroupDefinition, menuDefinition);
        // Menu bar separator
        groups.getChildMenuItems().add(separator);

        /* GROUPS: Favorites */
        MenuItem groupsFavorite = new MenuItem("/images/icons/Favorite_16.png", "", "Favorite Groups", groups
            .getChildMenuItems(), ExtensionPoints.favoriteGroups, menuDefinition);
        /* GROUPS: Recently Viewed */
        MenuItem groupsRecentlyViewed = new MenuItem("/images/icons/History_16.png", "", "Recently Viewed Groups",
            groups.getChildMenuItems(), ExtensionPoints.recentlyViewedGroups, menuDefinition);
        // Menu bar separator
        groups.getChildMenuItems().add(separator);
        /* GROUPS: Recently Viewed */
        MenuItem groupsSearch = new MenuItem("", "", "Search", groups.getChildMenuItems(),
            ExtensionPoints.searchGroups, menuDefinition);

        /* ===== ADMINISTRATION ===== */
        MenuItem administration = new MenuItem("", "", "Administration", menu.getChildMenuItems(),
            ExtensionPoints.administration, menuDefinition);
        /*===== ADMINISTRATION.BEFORE extension pt. Ignore same as Groups.after ============= */
        /*===== ADMINISTRATION.AFTER =====*/
        MenuItem administrationAfter = new MenuItem("", "", "", menu.getChildMenuItems(),
            ExtensionPoints.administration_AFTER, menuDefinition);
        administrationAfter.setRendered(false);
        MenuItem security = new MenuItem("", "", "Security", administration.getChildMenuItems(),
            ExtensionPoints.security, menuDefinition);
        MenuItem securityUsers = new MenuItem("", "/admin/user/UserAdmin.do?mode=list", "Users", security
            .getChildMenuItems(), ExtensionPoints.users, menuDefinition);
        MenuItem securityRoles = new MenuItem("", "/admin/role/RoleAdmin.do?mode=list", "Roles", security
            .getChildMenuItems(), ExtensionPoints.roles, menuDefinition);
        MenuItem sysconfig = new MenuItem("", "", "System Configuration", administration.getChildMenuItems(),
            ExtensionPoints.systemConfiguration, menuDefinition);
        MenuItem sysconfigSettings = new MenuItem("", "/admin/config/Config.do?mode=edit", "Settings", sysconfig
            .getChildMenuItems(), ExtensionPoints.settings, menuDefinition);
        MenuItem sysconfigPlugins = new MenuItem("", "/rhq/admin/plugin/plugin-list.xhtml", "Plugins", sysconfig
            .getChildMenuItems(), ExtensionPoints.plugins, menuDefinition);
        MenuItem sysconfigTemplates = new MenuItem("", "/admin/config/EditDefaults.do?mode=monitor&amp;viewMode=all",
            "Templates", sysconfig.getChildMenuItems(), ExtensionPoints.templates, menuDefinition);
        MenuItem content = new MenuItem("", "", "Content", administration.getChildMenuItems(), ExtensionPoints.content,
            menuDefinition);
        MenuItem contentContentProviders = new MenuItem("", "/rhq/content/listContentProviders.xhtml",
            "Content Providers", content.getChildMenuItems(), ExtensionPoints.contentProviders, menuDefinition);
        MenuItem contentRepositories = new MenuItem("", "/rhq/content/listRepos.xhtml", "Repositories", content
            .getChildMenuItems(), ExtensionPoints.repositories, menuDefinition);
        MenuItem ha = new MenuItem("", "", "High Availability", administration.getChildMenuItems(),
            ExtensionPoints.highAvailability, menuDefinition);
        MenuItem haServers = new MenuItem("", "/rhq/ha/listServers.xhtml", "Servers", ha.getChildMenuItems(),
            ExtensionPoints.haServers, menuDefinition);
        MenuItem haAgents = new MenuItem("", "/rhq/ha/listAgents.xhtml", "Agents", ha.getChildMenuItems(),
            ExtensionPoints.agents, menuDefinition);
        MenuItem haAffinityGroups = new MenuItem("", "/rhq/ha/listAffinityGroups.xhtml", "Affinity Groups", ha
            .getChildMenuItems(), ExtensionPoints.affinityGroups, menuDefinition);
        MenuItem haPartitionEvents = new MenuItem("", "/rhq/ha/listPartitionEvents.xhtml", "Partition Events", ha
            .getChildMenuItems(), ExtensionPoints.partitionEvents, menuDefinition);
        MenuItem reports = new MenuItem("", "", "Reports", administration.getChildMenuItems(), ExtensionPoints.reports,
            menuDefinition);
        MenuItem reportsResourceVersion = new MenuItem("", "/rhq/admin/report/resourceInstallReport.xhtml",
            "Resource Version Inventory Report", reports.getChildMenuItems(),
            ExtensionPoints.resourceVersionInventoryReport, menuDefinition);
        MenuItem downloads = new MenuItem("/images/icons/Save_16.png", "/rhq/admin/downloads.xhtml", "Downloads",
            administration.getChildMenuItems(), ExtensionPoints.downloads, menuDefinition);
        MenuItem license = new MenuItem("", "/admin/license/LicenseAdmin.do?mode=view", "License", administration
            .getChildMenuItems(), ExtensionPoints.license, menuDefinition);

        //* ------------- DEBUG ------------------- *//
        /*===== DEBUG.BEFORE extension pt. Ignore same as Administration.after ============= */
        MenuItem debug = new MenuItem("", "", "Debug", menu.getChildMenuItems(), ExtensionPoints.debug, menuDefinition);
        MenuItem sqlInterface = new MenuItem("", "/admin/test/sql.jsp", "SQL Interface", debug.getChildMenuItems(),
            ExtensionPoints.sqlInterface, menuDefinition);
        MenuItem jpqlHibernateInterface = new MenuItem("", "/admin/test/hibernate.jsp", "JPQL/HQL Interface", debug
            .getChildMenuItems(), ExtensionPoints.jpqlHqlInterface, menuDefinition);
        MenuItem hibernate = new MenuItem("", "/admin/test/browser.jsp", "Hibernate Entity Browser", debug
            .getChildMenuItems(), ExtensionPoints.hibernate, menuDefinition);
        MenuItem administrativeControl = new MenuItem("", "/admin/test/control.jsp", "Administrative Control", debug
            .getChildMenuItems(), ExtensionPoints.administrativeControl, menuDefinition);
        MenuItem testEmail = new MenuItem("", "/admin/test/email.jsp", "Test Email Configuration", debug
            .getChildMenuItems(), ExtensionPoints.testEmail, menuDefinition);
        MenuItem testAgent = new MenuItem("", "/admin/test/agent.jsp", "Test Agent Communications", debug
            .getChildMenuItems(), ExtensionPoints.testAgent, menuDefinition);
        /*===== DEBUG.AFTER =====*/
        MenuItem debugAfter = new MenuItem("", "", "", menu.getChildMenuItems(), ExtensionPoints.debug_AFTER,
            menuDefinition);
        debugAfter.setRendered(false);

        //* ------------- HELP ------------------- *//
        /*===== HELP.BEFORE extension pt. Ignore same as Administration.after ============= */
        MenuItem help = new MenuItem("", "", "Help", attachPoints, ExtensionPoints.help, menuDefinition);
        /*===== HELP.AFTER =====*/
        MenuItem helpAfter = new MenuItem("", "", "", attachPoints, ExtensionPoints.help_AFTER, menuDefinition);
        helpAfter.setRendered(false);
        MenuItem onlineDoc = new MenuItem("", "http://www.redhat.com/docs/en-US/JBoss_ON/", "Online Documentation",
            help.getChildMenuItems(), ExtensionPoints.help, menuDefinition);
        MenuItem support = new MenuItem("", "https://support.redhat.com/jbossnetwork/restricted/addCase.html",
            "Open a Support Case", help.getChildMenuItems(), ExtensionPoints.openASupportCase, menuDefinition);
        // Menu bar separator
        help.getChildMenuItems().add(separator);
        MenuItem about = new MenuItem("/images/icons/availability_grey_16.png", "", "", help.getChildMenuItems(),
            ExtensionPoints.about, menuDefinition);

        //* ------------- REFRESH ------------------- *//
        /*===== REFRESH.BEFORE extension pt. ============= */
        MenuItem refreshBefore = new MenuItem("", "", "", menu.getChildMenuItems(), ExtensionPoints.refresh_BEFORE,
            menuDefinition);
        refreshBefore.setRendered(false);
        refreshBefore.setAlignLeft(false);
        MenuItem refresh = new MenuItem("", "", "", attachPoints, ExtensionPoints.refresh, menuDefinition);
        refresh.setAlignLeft(false);
        /*===== HELP.AFTER =====*/
        MenuItem refreshAfter = new MenuItem("", "", "", attachPoints, ExtensionPoints.refresh_AFTER, menuDefinition);
        refreshAfter.setRendered(false);
        refreshAfter.setAlignLeft(false);
        MenuItem stop = new MenuItem("", "", "Stop", refresh.getChildMenuItems(), ExtensionPoints.stop, menuDefinition);
        MenuItem refresh1 = new MenuItem("", "", "Refresh every 1 minute", refresh.getChildMenuItems(),
            ExtensionPoints.refreshEvery1Minute, menuDefinition);
        MenuItem refresh5 = new MenuItem("", "", "Refresh every 5 minutes", refresh.getChildMenuItems(),
            ExtensionPoints.refreshEvery5Minutes, menuDefinition);
        MenuItem refresh10 = new MenuItem("", "", "Refresh every 10 minutes", refresh.getChildMenuItems(),
            ExtensionPoints.refreshEvery10Minutes, menuDefinition);

        //* ------------- IS LOGGED IN ------------------- *//
        /*===== IS LOGGED IN.BEFORE extension pt. Ignore same as refresh.after ============= */
        MenuItem loggedIn = new MenuItem("", "", "", attachPoints, ExtensionPoints.loggedInAs, menuDefinition);
        loggedIn.setAlignLeft(false);
        /*===== IS LOGGED IN.AFTER =====*/
        MenuItem loggedInAfter = new MenuItem("", "", "", attachPoints, ExtensionPoints.loggedInAs_AFTER,
            menuDefinition);
        loggedInAfter.setRendered(false);
        loggedInAfter.setAlignLeft(false);
        MenuItem edit = new MenuItem("", "", "Edit Details", loggedIn.getChildMenuItems(), ExtensionPoints.stop,
            menuDefinition);
        MenuItem change = new MenuItem("", "", "Change Password", loggedIn.getChildMenuItems(),
            ExtensionPoints.refreshEvery1Minute, menuDefinition);
        // Menu bar separator
        loggedIn.getChildMenuItems().add(separator);
        MenuItem logout = new MenuItem("", "/Logout.do", "Logout", loggedIn.getChildMenuItems(),
            ExtensionPoints.refreshEvery5Minutes, menuDefinition);

    }

    public MenuItem getCoreMenuBar() {
        return CoreMenuBar;
    }

    public void setCoreMenuBar(MenuItem coreMenuBar) {
        CoreMenuBar = coreMenuBar;
    }

    public HashMap<String, MenuItem> getMenuDefinition() {
        return menuDefinition;
    }

    public void setMenuDefinition(HashMap<String, MenuItem> menuDefinition) {
        this.menuDefinition = menuDefinition;
    }

    public static void main(String[] args) {
        PerspectivesMenuUIBean bean = new PerspectivesMenuUIBean();
        //        System.out.println("Bean:" + bean);
        MenuItem coreMenu = bean.getCoreMenuBar();
        HashMap<String, MenuItem> definition = bean.getMenuDefinition();
        //iterate over the coreMenu recursively
        recurse(coreMenu, "(TOP)");
        //iterate over the map and display the values
        Set<String> keys = definition.keySet();
        ArrayList<String> list = new ArrayList<String>(keys);
        Collections.sort(list);
        System.out.println("\n Key listing [" + keys.size() + " item(s)] \n");
        for (String key : list) {
            System.out.println("KEY:" + key + "=" + definition.get(key));
        }
    }

    private static void recurse(MenuItem menu, String prefix) {
        if ((menu != null)) {
            //base case: leaf
            if (menu.getChildMenuItems().size() == 0) {
                System.out.println(prefix + "#" + menu.getName() + ":" + menu.getExtensionKey() + ":");
            } else {
                System.out.println(prefix + "[DIR-(" + menu.getChildMenuItems().size() + ")]" + menu.getName() + ":"
                    + menu.getExtensionKey() + ":");
                for (MenuItem child : menu.getChildMenuItems()) {
                    recurse(child, prefix + "\t");
                }
            }
        }
    }
}
