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
import java.util.HashMap;
import java.util.List;

/**
 *  Bean dynamically provides menu information used by menu.xhtml.  This bean should be viewed 
 *  concurrently with menu.xhtml and it's designed to provide static or dynamic menu content 
 *  for menu.xhtml.
 * 
 * @author Simeon Pinder
 *
 */
public class PerspectivesMenuUIBean {
    /* Stores all mapped MenuItem information */
    private HashMap<String, MenuItem> menuDefinition = new HashMap<String, MenuItem>();

    /*===== LOGO TO HOME PAGE =====*/
    private MenuItem logo = new MenuItem("/images/JBossLogo_small.png", "", "");
    {
        logo.setIconHeight(21);
        logo.setIconWidth(37);
        menuDefinition.put(MenuItem.logo, logo);
    }

    /*===== OVERVIEW =====*/
    private MenuItem overview = new MenuItem("", "", "Overview");
    {
        menuDefinition.put(MenuItem.overview, overview);
    }
    /*===== Overview.BEFORE extension pt. ============= */
    private MenuItem overviewBefore = new MenuItem("", "", "");
    {
        overviewBefore.setRendered(false);//disable to start
        menuDefinition.put(MenuItem.overview_BEFORE, overviewBefore);
    }

    private List<MenuItem> overviewBeforeItems = new ArrayList<MenuItem>();

    /****** Overview > SYSTEM VIEWS ******/
    private MenuItem subsystemViews = new MenuItem("", "", "Subsystem Views");
    private List<MenuItem> subsystemMenuItems = new ArrayList<MenuItem>();
    {//initialization block 
        //SubsystemView menu items
        String[] subsystemCategories = { "Configure", "Monitor_failed", "Operation", "Flag_blue", "Flag_grey" };
        String[] subsystemPages = { "configurationUpdate", "oobHistory", "operationHistory", "alertHistory",
            "alertDefinitions" };
        String[] subsystemNames = { "Configuration Changes", "Suspect Metrics", "Operations", "Alerts",
            "Alert Definitions" };
        for (int i = 0; i < subsystemPages.length; i++) {
            subsystemMenuItems.add(new MenuItem("/images/icons/" + subsystemCategories[i] + "_16.png",//icon url 
                "/rhq/subsystem/" + subsystemPages[i] + ".xhtml", // menu item url 
                subsystemNames[i])); //menu item name
        }
    }
    /* ***** Overview > AUTO-DISCOVERY QUEUE ***** */
    private MenuItem autoDiscoveryQueue = new MenuItem("", "/rhq/discovery/queue.xhtml", "Auto Discovery Queue");;

    /* ***** Overview > DASHBOARD ***** */
    private MenuItem dashboard = new MenuItem("", "/Dashboard.do", "Dashboard");

    /* ===== RESOURCES ===== */
    private MenuItem resources = new MenuItem("", "", "Resources");

    /* Resources: Platform */
    private MenuItem resourcesPlatform = null;
    {
        resourcesPlatform = new MenuItem("/images/icons/Platform_up_16.png",
            "/ResourceHub.do?resourceCategory=PLATFORM", "");
    }
    /* Resources: Server */
    private MenuItem resourcesServer = null;
    {
        resourcesServer = new MenuItem("/images/icons/Server_up_16.png", "/ResourceHub.do?resourceCategory=SERVER", "");
    }

    /* Resources: Service */
    private MenuItem resourcesService = null;
    {
        resourcesService = new MenuItem("/images/icons/Service_up_16.png", "/ResourceHub.do?resourceCategory=SERVICE",
            "");
    }

    private MenuItem resourcesFavorite = new MenuItem("/images/icons/Favorite_16.png", "", "Favorite Resources");
    private MenuItem resourcesRecentlyViewed = new MenuItem("/images/icons/History_16.png", "",
        "Recently Viewed Resources");
    //TODO: enable?
    private MenuItem resourcesSearch = new MenuItem("/images/icons/History_16.png", "", "Search: ");

    /* ===== GROUPS ===== */
    private MenuItem groups = new MenuItem("", "", "Groups");
    private MenuItem groupsCompatible = new MenuItem("/images/icons/Cluster_up_16.png",
        "/GroupHub.do?groupCategory=COMPATIBLE", "");
    private MenuItem groupsMixed = new MenuItem("/images/icons/Group_up_16.png", "/GroupHub.do?groupCategory=MIXED", "");
    private MenuItem groupsDefinitions = new MenuItem("/images/GroupDefinition_16.png",
        "/rhq/definition/group/list.xhtml", "");
    private MenuItem groupsNewResource = new MenuItem("", "/resource/group/Inventory.do?mode=new", "");
    private MenuItem groupsNewDefinition = new MenuItem("", "/rhq/definition/group/new.xhtml", "");
    private MenuItem groupsFavorite = new MenuItem("/images/icons/Favorite_16.png", "", "Favorite Groups");
    private MenuItem groupsRecentlyViewed = new MenuItem("/images/icons/History_16.png", "", "Recently Viewed Groups");

    /* ===== ADMINISTRATION ===== */
    private MenuItem security = new MenuItem("", "", "Security");
    private MenuItem securityUsers = new MenuItem("", "/admin/user/UserAdmin.do?mode=list", "Users");
    private MenuItem securityRoles = new MenuItem("", "/admin/role/RoleAdmin.do?mode=list", "Roles");
    private MenuItem sysconfig = new MenuItem("", "", "System Configuration");
    private MenuItem sysconfigSettings = new MenuItem("", "/admin/config/Config.do?mode=edit", "Settings");
    private MenuItem sysconfigPlugins = new MenuItem("", "/rhq/admin/plugin/plugin-list.xhtml", "Plugins");
    private MenuItem sysconfigTemplates = new MenuItem("",
        "/admin/config/EditDefaults.do?mode=monitor&amp;viewMode=all", "Templates");
    private MenuItem content = new MenuItem("", "", "Content");
    private MenuItem contentContentProviders = new MenuItem("", "/rhq/content/listContentProviders.xhtml",
        "Content Providers");
    private MenuItem contentRepositories = new MenuItem("", "/rhq/content/listRepos.xhtml", "Repositories");
    private MenuItem ha = new MenuItem("", "", "High Availability");
    private MenuItem haServers = new MenuItem("", "/rhq/ha/listServers.xhtml", "Servers");
    private MenuItem haAgents = new MenuItem("", "/rhq/ha/listAgents.xhtml", "Agents");
    private MenuItem haAffinityGroups = new MenuItem("", "/rhq/ha/listAffinityGroups.xhtml", "Affinity Groups");
    private MenuItem haPartitionEvents = new MenuItem("", "/rhq/ha/listPartitionEvents.xhtml", "Partition Events");
    private MenuItem reports = new MenuItem("", "", "Reports");
    private MenuItem reportsResourceVersion = new MenuItem("", "/rhq/admin/report/resourceInstallReport.xhtml",
        "Resource Version Inventory Report");
    private MenuItem downloads = new MenuItem("/images/icons/Save_16.png", "/rhq/admin/downloads.xhtml", "Downloads");
    private MenuItem license = new MenuItem("", "/admin/license/LicenseAdmin.do?mode=view", "License");

    public List<MenuItem> getSubsystemMenuItems() {
        return subsystemMenuItems;
    }

    public void setSubsystemMenuItems(List<MenuItem> subsystemMenuItems) {
        this.subsystemMenuItems = subsystemMenuItems;
    }

    public MenuItem getAutoDiscoveryQueue() {
        return autoDiscoveryQueue;
    }

    public void setAutoDiscoveryQueue(MenuItem autoDiscoveryQueue) {
        this.autoDiscoveryQueue = autoDiscoveryQueue;
    }

    public MenuItem getDashboard() {
        return dashboard;
    }

    public void setDashboard(MenuItem dashboard) {
        this.dashboard = dashboard;
    }

    public MenuItem getResourcesPlatform() {
        return resourcesPlatform;
    }

    public void setResourcesPlatform(MenuItem resourcesPlatform) {
        this.resourcesPlatform = resourcesPlatform;
    }

    public MenuItem getResourcesServer() {
        return resourcesServer;
    }

    public void setResourcesServer(MenuItem resourcesServer) {
        this.resourcesServer = resourcesServer;
    }

    public MenuItem getResourcesService() {
        return resourcesService;
    }

    public void setResourcesService(MenuItem resourcesService) {
        this.resourcesService = resourcesService;
    }

    public MenuItem getGroups() {
        return groups;
    }

    public void setGroups(MenuItem groups) {
        this.groups = groups;
    }

    public MenuItem getGroupsCompatible() {
        return groupsCompatible;
    }

    public void setGroupsCompatible(MenuItem groupsCompatible) {
        this.groupsCompatible = groupsCompatible;
    }

    public MenuItem getGroupsMixed() {
        return groupsMixed;
    }

    public void setGroupsMixed(MenuItem groupsMixed) {
        this.groupsMixed = groupsMixed;
    }

    public MenuItem getGroupsDefinitions() {
        return groupsDefinitions;
    }

    public void setGroupsDefinitions(MenuItem groupsDefinitions) {
        this.groupsDefinitions = groupsDefinitions;
    }

    public MenuItem getGroupsNewResource() {
        return groupsNewResource;
    }

    public void setGroupsNewResource(MenuItem groupsNewResource) {
        this.groupsNewResource = groupsNewResource;
    }

    public MenuItem getGroupsNewDefinition() {
        return groupsNewDefinition;
    }

    public void setGroupsNewDefinition(MenuItem groupsNewDefinition) {
        this.groupsNewDefinition = groupsNewDefinition;
    }

    public MenuItem getLogo() {
        return logo;
    }

    public void setLogo(MenuItem logo) {
        this.logo = logo;
    }

    public MenuItem getOverview() {
        return overview;
    }

    public void setOverview(MenuItem overview) {
        this.overview = overview;
    }

    public MenuItem getSubsystemViews() {
        return subsystemViews;
    }

    public void setSubsystemViews(MenuItem subsystemViews) {
        this.subsystemViews = subsystemViews;
    }

    public MenuItem getResources() {
        return resources;
    }

    public void setResources(MenuItem resources) {
        this.resources = resources;
    }

    public MenuItem getResourcesFavorite() {
        return resourcesFavorite;
    }

    public void setResourcesFavorite(MenuItem resourcesFavorite) {
        this.resourcesFavorite = resourcesFavorite;
    }

    public MenuItem getResourcesRecentlyViewed() {
        return resourcesRecentlyViewed;
    }

    public void setResourcesRecentlyViewed(MenuItem resourcesRecentlyViewed) {
        this.resourcesRecentlyViewed = resourcesRecentlyViewed;
    }

    public MenuItem getGroupsFavorite() {
        return groupsFavorite;
    }

    public void setGroupsFavorite(MenuItem groupsFavorite) {
        this.groupsFavorite = groupsFavorite;
    }

    public MenuItem getGroupsRecentlyViewed() {
        return groupsRecentlyViewed;
    }

    public void setGroupsRecentlyViewed(MenuItem groupsRecentlyViewed) {
        this.groupsRecentlyViewed = groupsRecentlyViewed;
    }

    public MenuItem getResourcesSearch() {
        return resourcesSearch;
    }

    public void setResourcesSearch(MenuItem resourcesSearch) {
        this.resourcesSearch = resourcesSearch;
    }

    public MenuItem getSecurity() {
        return security;
    }

    public void setSecurity(MenuItem security) {
        this.security = security;
    }

    public MenuItem getSecurityUsers() {
        return securityUsers;
    }

    public void setSecurityUsers(MenuItem securityUsers) {
        this.securityUsers = securityUsers;
    }

    public MenuItem getSecurityRoles() {
        return securityRoles;
    }

    public void setSecurityRoles(MenuItem securityRoles) {
        this.securityRoles = securityRoles;
    }

    public MenuItem getSysconfig() {
        return sysconfig;
    }

    public void setSysconfig(MenuItem sysconfig) {
        this.sysconfig = sysconfig;
    }

    public MenuItem getSysconfigSettings() {
        return sysconfigSettings;
    }

    public void setSysconfigSettings(MenuItem sysconfigSettings) {
        this.sysconfigSettings = sysconfigSettings;
    }

    public MenuItem getSysconfigPlugins() {
        return sysconfigPlugins;
    }

    public void setSysconfigPlugins(MenuItem sysconfigPlugins) {
        this.sysconfigPlugins = sysconfigPlugins;
    }

    public MenuItem getSysconfigTemplates() {
        return sysconfigTemplates;
    }

    public void setSysconfigTemplates(MenuItem sysconfigTemplates) {
        this.sysconfigTemplates = sysconfigTemplates;
    }

    public MenuItem getContent() {
        return content;
    }

    public void setContent(MenuItem content) {
        this.content = content;
    }

    public MenuItem getContentContentProviders() {
        return contentContentProviders;
    }

    public void setContentContentProviders(MenuItem contentContentProviders) {
        this.contentContentProviders = contentContentProviders;
    }

    public MenuItem getContentRepositories() {
        return contentRepositories;
    }

    public void setContentRepositories(MenuItem contentRepositories) {
        this.contentRepositories = contentRepositories;
    }

    public MenuItem getHa() {
        return ha;
    }

    public void setHa(MenuItem ha) {
        this.ha = ha;
    }

    public MenuItem getHaServers() {
        return haServers;
    }

    public void setHaServers(MenuItem haServers) {
        this.haServers = haServers;
    }

    public MenuItem getHaAgents() {
        return haAgents;
    }

    public void setHaAgents(MenuItem haAgents) {
        this.haAgents = haAgents;
    }

    public MenuItem getHaAffinityGroups() {
        return haAffinityGroups;
    }

    public void setHaAffinityGroups(MenuItem haAffinityGroups) {
        this.haAffinityGroups = haAffinityGroups;
    }

    public MenuItem getHaPartitionEvents() {
        return haPartitionEvents;
    }

    public void setHaPartitionEvents(MenuItem haPartitionEvents) {
        this.haPartitionEvents = haPartitionEvents;
    }

    public MenuItem getReports() {
        return reports;
    }

    public void setReports(MenuItem reports) {
        this.reports = reports;
    }

    public MenuItem getReportsResourceVersion() {
        return reportsResourceVersion;
    }

    public void setReportsResourceVersion(MenuItem reportsResourceVersion) {
        this.reportsResourceVersion = reportsResourceVersion;
    }

    public MenuItem getDownloads() {
        return downloads;
    }

    public void setDownloads(MenuItem downloads) {
        this.downloads = downloads;
    }

    public MenuItem getLicense() {
        return license;
    }

    public void setLicense(MenuItem license) {
        this.license = license;
    }

    public HashMap<String, MenuItem> getMenuDefinition() {
        return menuDefinition;
    }

    public void setMenuDefinition(HashMap<String, MenuItem> menuDefinition) {
        this.menuDefinition = menuDefinition;
    }

    public MenuItem getOverviewBefore() {
        return overviewBefore;
    }

    public void setOverviewBefore(MenuItem overviewBefore) {
        this.overviewBefore = overviewBefore;
    }

    public List<MenuItem> getOverviewBeforeItems() {
        return overviewBeforeItems;
    }

    public void setOverviewBeforeItems(List<MenuItem> overviewBeforeItems) {
        this.overviewBeforeItems = overviewBeforeItems;
    }
}
