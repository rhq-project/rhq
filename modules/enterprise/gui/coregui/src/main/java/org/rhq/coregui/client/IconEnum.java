/*
 * RHQ Management Platform
 * Copyright (C) 2012 Red Hat, Inc.
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
package org.rhq.coregui.client;


/**
 * Adding type information around Icons instead of having plain Strings.
 * Icons imply more than just a String type. There are attributes associated with icons
 * that should be grouped together.
 * Manages Centralized Icon definitions.
 * This provides more type safety than the ImageManager class that deals only with String type.
 * IDE's can quickly query on Icon Types and quickly see exactly where that image was used
 * and understand the full ramifications of changing an image.
 * Helps provide consistency of images throughout the app.
 * By having the IconEnum represent the icon, we can defer the determination of the
 * size of the icon to the implementation while still defining an icon.
 * It is also very easy for a method signature to become ambiguous if the string is null.
 *
 * @author  Mike Thompson
 */
public enum IconEnum {

    /////////////////////////////
    // Inventory Tab
    /////////////////////////////

    INVENTORY("global/Inventory_16.png","global/Inventory_24.png"),
    DISCOVERY_QUEUE("global/AutoDiscovery_16.png","global/AutoDiscovery_24.png"),
    ALL_RESOURCES("resources/all_resources.png","resources/all_resources.png"),
    PLATFORMS("types/Platform_type_16.png","types/Platform_type_24.png"),
    SERVERS("types/Server_type_16.png", "types/Server_type_24.png"),
    SERVICES("types/Service_type_16.png", "types/Service_type_24.png"),
    UNAVAILABLE_SERVERS("types/Server_down_16.png", "types/Server_down_24.png"),
    // Groups Section
    DYNAGROUPS("types/GroupDefinition_16.png", "types/GroupDefinition_24.png"),
    ALL_GROUPS("types/Group_mixed_type_16.png", "types/Group_mixed_type_24.png"),
    COMPATIBLE_GROUPS("types/Cluster_type_16.png", "types/Cluster_type_24.png"),
    MIXED_GROUPS("types/Group_mixed_type_16.png", "types/Group_mixed_type_24.png"),
    PROBLEM_GROUPS("types/Group_down_16.png", "types/Group_down_24.png"),

    /////////////////////////////
    // Reports Tab
    /////////////////////////////
    TAGS("global/Tag_16.png","global/Tag_24.png" ),
    REPORT("subsystems/report/Document_16.png","subsystems/report/Document_24.png" ),
    SUSPECT_METRICS("subsystems/monitor/Monitor_failed_16.png","subsystems/monitor/Monitor_failed_24.png","subsystems/monitor/Monitor_grey_16.png",null),
    RECENT_MEASUREMENTS("subsystems/monitor/Monitor_16.png","subsystems/monitor/Monitor_24.png"),
    CONFIGURATION_HISTORY("subsystems/configure/Configure_16.png", "subsystems/configure/Configure_24.png","subsystems/configure/Configure_grey_16.png",null),
    RECENT_OPERATIONS("subsystems/control/Operation_16.png","subsystems/control/Operation_24.png", "subsystems/control/Operation_grey_16.png",null),
    RECENT_ALERTS("subsystems/alert/Alert_LOW_16.png"),
    ALERT_DEFINITIONS("subsystems/alert/Alerts_16.png",null, "subsystems/alert/Alerts_16.png",null),
    RECENT_DRIFT("subsystems/drift/Drift_16.png", "subsystems/drift/Drift_24.png", "subsystems/drift/Drift_16.png", null ),
    // Inventory Section
    INVENTORY_SUMMARY("subsystems/inventory/Inventory_16.png", "subsystems/inventory/Inventory_24.png", "subsystems/inventory/Inventory_grey_16.png",null),
    PLATFORM_UTILIZATION("types/Platform_up_16.png", "types/Platform_up_24.png"),
    DRIFT_COMPLIANCE("subsystems/drift/Drift_16.png", "subsystems/drift/Drift_24.png", "subsystems/drift/Drift_16.png",null),

    /////////////////////////////
    //  HELP Tab
    /////////////////////////////
    HELP("global/Help_16.png", "global/Help_24.png"),

    /////////////////////////////
    //  Bundle Tab
    /////////////////////////////
    BUNDLE("subsystems/bundle/Bundle_16.png", "subsystems/bundle/Bundle_24.png"),
    BUNDLE_GROUP("subsystems/bundle/BundleGroup_16.png", "subsystems/bundle/BundleGroup_24.png"),
    BUNDLE_DELETE("subsystems/bundle/BundleAction_Delete_16.png", "subsystems/bundle/BundleAction_Delete_24.png"),
    BUNDLE_DEPLOY("subsystems/bundle/BundleAction_Deploy_16.png", "subsystems/bundle/BundleAction_Deploy_24.png"),
    BUNDLE_VERSION("subsystems/bundle/BundleVersion_16.png", "subsystems/bundle/BundleVersion_24.png"),
    BUNDLE_DEPLOYMENT("subsystems/bundle/BundleDeployment_16.png", "subsystems/bundle/BundleDeployment_24.png"),
    BUNDLE_DESTINATION("subsystems/bundle/BundleDestination_16.png", "subsystems/bundle/BundleDestination_24.png"),
    BUNDLE_DESTINATION_DELETE("subsystems/bundle/BundleDestinationAction_Delete_16.png", "subsystems/bundle/BundleDestinationAction_Delete_24.png"),
    BUNDLE_DESTINATION_PURGE("subsystems/bundle/BundleDestinationAction_Purge_16.png", "subsystems/bundle/BundleDestinationAction_Purge_24.png"),
    BUNDLE_REVERT("subsystems/bundle/BundleAction_Revert_16.png", "subsystems/bundle/BundleAction_Revert_24.png"),

    /////////////////////////////
    // Search
    /////////////////////////////
    STAR_OFF("search/star1.png"),
    STAR_ACTIVE("search/star2.png"),
    STAR_ON("search/star3.png"),
    ARROW_WHITE("search/menu_arrow.png"),
    ARROW_GRAY("search/menu_arrow_down.png"),
    TRASH("search/trash.png"),
    QUERY("search/glass.png"),

    /////////////////////////////
    //  Administration Tab
    /////////////////////////////
    ADMIN("global/Admin_16.png", "global/Admin_24.png"),
    USERS("global/User_16.png", "global/User_24.png"),
    ROLES("global/Role_16.png", "global/Role_24.png"),
    CONFIGURE("subsystems/configure/Configure_16.png", "subsystems/configure/Configure_24.png", "subsystems/configure/Configure_grey_16.png", null),
    DOWNLOAD("global/Download_16.png", "global/Download_24.png"),
    EVENTS("subsystems/event/Events_16.png", "subsystems/event/Events_24.png", "subsystems/event/Events_grey_16.png", null),
    AGENT("global/Agent_16.png", "global/Agent_24.png"),
    METRIC("subsystems/monitor/Edit_Metric.png", "subsystems/monitor/Edit_Metric.png"),
    PLUGIN("global/Plugin_16.png", "global/Plugin_24.png"),
    ALERT_FLAG_BLUE("subsystems/alert/Flag_blue_16.png", "subsystems/alert/Flag_blue_24.png"),
    CONTENT("subsystems/content/Content_16.png", "subsystems/content/Content_24.png", "subsystems/content/Content_grey_16.png",null),
    STORAGE_NODE("global/StorageNode_16.png", "global/StorageNode_24.png"),

    /////////////////////////////
    //  General
    /////////////////////////////
    EXPANDED_ICON("[SKIN]/ListGrid/row_expanded.png"),
    COLLAPSED_ICON("[SKIN]/ListGrid/row_collapsed.png"),

    /////////////////////////////
    //  Resource Specific Tabs
    /////////////////////////////
    CALLTIME("global/Recent_16.png", "global/Recent_24.png");

    IconEnum(String icon16x16Path ){
        this.icon16x16Path = icon16x16Path;
    }

    IconEnum(String icon16x16Path, String icon24x24Path){
        this.icon16x16Path = icon16x16Path;
        this.icon24x24Path = icon24x24Path;
    }

    IconEnum(String icon16x16Path,  String icon24x24Path, String icon16x16DisabledPath, String icon24x24DisabledPath) {
        this.icon16x16Path = icon16x16Path;
        this.icon16x16DisabledPath = icon16x16DisabledPath;
        this.icon24x24Path = icon24x24Path;
        this.icon24x24DisabledPath = icon24x24DisabledPath;
    }

    //private String icon11x11Path;
    private String icon16x16Path;
    private String icon16x16DisabledPath;
    private String icon24x24Path;
    private String icon24x24DisabledPath;
    //private String hoverText;

    public String getIcon16x16Path() {
        return icon16x16Path;
    }

    public String getIcon16x16DisabledPath() {
        return icon16x16DisabledPath;
    }


    public String getIcon24x24Path() {
        return icon24x24Path;
    }

    public String getIcon24x24DisabledPath() {
        return icon24x24DisabledPath;
    }

}
