package org.rhq.enterprise.gui.perspectives;

public class ExtensionPoints {

    //constants:begin Extension point map
    //MENU:ExtensionPoints BEGIN
    //root menu item container
    public static final String menu = "menu";
    //logo 
    public static final String logo = menu + ".logo";
    public static final String logo_BEFORE = logo + "_BEFORE";
    public static final String logo_AFTER = logo + "_AFTER";
    //overview 
    public static final String overview_BEFORE = logo_AFTER;//same spot
    public static final String overview = menu + ".overview";
    public static final String subsystemViews = overview + ".subsystemViews";
    public static final String configurationChanges = subsystemViews + ".configurationChanges";
    public static final String suspectMetrics = subsystemViews + ".suspectMetrics";
    public static final String operations = subsystemViews + ".operations";
    public static final String alerts = subsystemViews + ".alerts";
    public static final String alertDefinitions = subsystemViews + ".alertDefinitions";
    public static final String autoDiscoveryQueue = overview + ".autoDiscoveryQueue";
    public static final String dashboard = overview + ".dashboard";
    public static final String overview_AFTER = overview + "_AFTER";
    //resources
    public static final String resources_BEFORE = overview_AFTER;//same spot
    public static final String resources = menu + ".resources";
    public static final String platforms = resources + ".platforms";
    public static final String servers = resources + ".servers";
    public static final String services = resources + ".services";
    public static final String favoriteResources = resources + ".favoriteResources";
    public static final String recentlyViewedResources = resources + ".recentlyViewedResources";
    public static final String searchResources = resources + ".search";
    public static final String resources_AFTER = resources + "_AFTER";
    //groups
    public static final String groups_BEFORE = resources_AFTER;//same spot
    public static final String groups = menu + ".groups";
    public static final String compatible = groups + ".compatibleGroups";
    public static final String mixed = groups + ".mixedGroups";
    public static final String definitions = groups + ".groupDefinitions";
    public static final String newGroup = groups + ".newGroup";
    public static final String newGroupDefinition = groups + ".newGroupDefinition";
    public static final String favoriteGroups = groups + ".favoriteGroups";
    public static final String recentlyViewedGroups = groups + ".recentlyViewedGroups";
    public static final String searchGroups = groups + ".search";
    public static final String groups_AFTER = groups + "_AFTER";
    //administration
    public static final String administration_BEFORE = groups_AFTER;//same spot
    public static final String administration = menu + ".administration";
    public static final String security = administration + ".security";
    public static final String users = security + ".users";
    public static final String roles = security + ".roles";
    public static final String systemConfiguration = administration + ".systemConfiguration";
    public static final String settings = systemConfiguration + ".settings";
    public static final String plugins = systemConfiguration + ".plugins";
    public static final String templates = systemConfiguration + ".templates";
    public static final String content = administration + ".content";
    public static final String contentProviders = content + ".contentProviders";
    public static final String repositories = content + ".repositories";
    public static final String highAvailability = administration + ".highAvailability";
    public static final String haServers = highAvailability + ".servers";
    public static final String agents = highAvailability + ".agents";
    public static final String affinityGroups = highAvailability + ".affinityGroups";
    public static final String partitionEvents = highAvailability + ".partitionEvents";
    public static final String reports = administration + ".reports";
    public static final String resourceVersionInventoryReport = reports + ".resourceVersionInventoryReport";
    public static final String downloads = administration + ".downloads";
    public static final String license = administration + ".license";
    public static final String administration_AFTER = administration + "_AFTER";
    //debug
    public static final String debug_BEFORE = administration_AFTER;//same spot
    public static final String debug = menu + ".debug";
    public static final String sqlInterface = debug + ".sqlInterface";
    public static final String jpqlHqlInterface = debug + ".jpqlHqlInterface";
    public static final String hibernate = debug + ".hibernate";
    public static final String administrativeControl = debug + ".administrativeControl";
    public static final String testEmail = debug + ".testEmail";
    public static final String testAgent = debug + ".testAgent";
    public static final String debug_AFTER = debug + "_AFTER";
    //help
    public static final String help_BEFORE = debug_AFTER;//same spot
    public static final String help = menu + ".help";
    public static final String onlineDocumentation = help + ".onlineDocumentation";
    public static final String openASupportCase = help + ".openASupportCase";
    public static final String about = help + ".about";
    public static final String help_AFTER = help + "_AFTER";
    //refresh
    public static final String refresh = menu + ".refresh";
    public static final String refresh_BEFORE = refresh + "_BEFORE";
    public static final String stop = refresh + ".stop";
    public static final String refreshEvery1Minute = refresh + ".refreshEvery1Minute";
    public static final String refreshEvery5Minutes = refresh + ".refreshEvery5Minutes";
    public static final String refreshEvery10Minutes = refresh + ".refreshEvery10Minutes";
    public static final String refresh_AFTER = refresh + "_AFTER";
    //loggedInAs
    public static final String loggedInAs_BEFORE = refresh_AFTER;//same spot
    public static final String loggedInAs = menu + ".loggedInAs";
    public static final String editDetails = loggedInAs + ".editDetails";
    public static final String changePassword = loggedInAs + ".changePassword";
    public static final String logout = loggedInAs + ".logout";
    public static final String loggedInAs_AFTER = loggedInAs + "_AFTER";

    public ExtensionPoints() {
    }

    public String getMenu() {
        return menu;
    }

    public String getLogo() {
        return logo;
    }

    public String getLogoBefore() {
        return logo_BEFORE;
    }

    public String getLogoAfter() {
        return logo_AFTER;
    }

    public String getOverviewBefore() {
        return overview_BEFORE;
    }

    public String getOverview() {
        return overview;
    }

    public String getSubsystemviews() {
        return subsystemViews;
    }

    public String getConfigurationchanges() {
        return configurationChanges;
    }

    public String getSuspectmetrics() {
        return suspectMetrics;
    }

    public String getOperations() {
        return operations;
    }

    public String getAlerts() {
        return alerts;
    }

    public String getAlertdefinitions() {
        return alertDefinitions;
    }

    public String getAutodiscoveryqueue() {
        return autoDiscoveryQueue;
    }

    public String getDashboard() {
        return dashboard;
    }

    public String getOverviewAfter() {
        return overview_AFTER;
    }

    public String getDebugBefore() {
        return debug_BEFORE;
    }

    public String getDebug() {
        return debug;
    }

    public String getSqlinterface() {
        return sqlInterface;
    }

    public String getJpqlhqlinterface() {
        return jpqlHqlInterface;
    }

    public String getHibernate() {
        return hibernate;
    }

    public String getAdministrativecontrol() {
        return administrativeControl;
    }

    public String getTestemail() {
        return testEmail;
    }

    public String getTestagent() {
        return testAgent;
    }

    public String getDebugAfter() {
        return debug_AFTER;
    }

    public String getResourcesBefore() {
        return resources_BEFORE;
    }

    public String getResources() {
        return resources;
    }

    public String getPlatforms() {
        return platforms;
    }

    public String getServers() {
        return servers;
    }

    public String getServices() {
        return services;
    }

    public String getFavoriteresources() {
        return favoriteResources;
    }

    public String getRecentlyviewedresources() {
        return recentlyViewedResources;
    }

    public String getSearchresources() {
        return searchResources;
    }

    public String getResourcesAfter() {
        return resources_AFTER;
    }

    public String getGroupsBefore() {
        return groups_BEFORE;
    }

    public String getGroups() {
        return groups;
    }

    public String getCompatible() {
        return compatible;
    }

    public String getMixed() {
        return mixed;
    }

    public String getDefinitions() {
        return definitions;
    }

    public String getNewgroup() {
        return newGroup;
    }

    public String getNewgroupdefinition() {
        return newGroupDefinition;
    }

    public String getFavoritegroups() {
        return favoriteGroups;
    }

    public String getRecentlyviewedgroups() {
        return recentlyViewedGroups;
    }

    public String getSearchgroups() {
        return searchGroups;
    }

    public String getGroupsAfter() {
        return groups_AFTER;
    }

    public String getAdministrationBefore() {
        return administration_BEFORE;
    }

    public String getAdministration() {
        return administration;
    }

    public String getSecurity() {
        return security;
    }

    public String getUsers() {
        return users;
    }

    public String getRoles() {
        return roles;
    }

    public String getSystemconfiguration() {
        return systemConfiguration;
    }

    public String getSettings() {
        return settings;
    }

    public String getPlugins() {
        return plugins;
    }

    public String getTemplates() {
        return templates;
    }

    public String getContent() {
        return content;
    }

    public String getContentproviders() {
        return contentProviders;
    }

    public String getRepositories() {
        return repositories;
    }

    public String getHighavailability() {
        return highAvailability;
    }

    public String getHaservers() {
        return haServers;
    }

    public String getAgents() {
        return agents;
    }

    public String getAffinitygroups() {
        return affinityGroups;
    }

    public String getPartitionevents() {
        return partitionEvents;
    }

    public String getReports() {
        return reports;
    }

    public String getResourceversioninventoryreport() {
        return resourceVersionInventoryReport;
    }

    public String getDownloads() {
        return downloads;
    }

    public String getLicense() {
        return license;
    }

    public String getAdministrationAfter() {
        return administration_AFTER;
    }

    public String getHelpBefore() {
        return help_BEFORE;
    }

    public String getHelp() {
        return help;
    }

    public String getOnlinedocumentation() {
        return onlineDocumentation;
    }

    public String getOpenasupportcase() {
        return openASupportCase;
    }

    public String getAbout() {
        return about;
    }

    public String getHelpAfter() {
        return help_AFTER;
    }

    public String getRefreshBefore() {
        return refresh_BEFORE;
    }

    public String getRefresh() {
        return refresh;
    }

    public String getStop() {
        return stop;
    }

    public String getRefreshevery1minute() {
        return refreshEvery1Minute;
    }

    public String getRefreshevery5minutes() {
        return refreshEvery5Minutes;
    }

    public String getRefreshevery10minutes() {
        return refreshEvery10Minutes;
    }

    public String getRefreshAfter() {
        return refresh_AFTER;
    }

    public String getLoggedinasBefore() {
        return loggedInAs_BEFORE;
    }

    public String getLoggedinas() {
        return loggedInAs;
    }

    public String getEditdetails() {
        return editDetails;
    }

    public String getChangepassword() {
        return changePassword;
    }

    public String getLogout() {
        return logout;
    }

    public String getLoggedinasAfter() {
        return loggedInAs_AFTER;
    }

    //MENU:ExtensionPoints END

    //constants:end
}
