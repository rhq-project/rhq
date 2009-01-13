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
package org.rhq.enterprise.gui.legacy;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.configuration.AbstractResourceConfigurationUpdate;

/**
 * Constant values for parameter names and their values passed in through the request
 */
public interface ParamConstants {
    /**
     * The request parameter that specifies the id of the ai platform to fetch.
     */
    public static final String AI_PLATFORM_PARAM = "aiPid";

    /**
     * The request parameter that specifies the id of the ai resource to fetch.
     */
    public static final String AI_RESOURCE_PARAM = "aiRid";

    /**
     * Alert definition enable action units for time period: days.
     */
    public static final int ALERT_ACTION_ENABLE_UNITS_DAYS = 3;

    /**
     * Alert definition enable action units for time period: hours.
     */
    public static final int ALERT_ACTION_ENABLE_UNITS_HOURS = 2;

    /**
     * Alert definition enable action units for time period: minutes.
     */
    public static final int ALERT_ACTION_ENABLE_UNITS_MINUTES = 1;

    /**
     * Alert definition enable action units for time period: seconds.
     */
    public static final int ALERT_ACTION_ENABLE_UNITS_SECONDS = 0;

    /**
     * Alert definition enable action units for time period: weeks.
     */
    public static final int ALERT_ACTION_ENABLE_UNITS_WEEKS = 4;

    /**
     * Constant to indicate that no conditions were deleted.
     */
    public static final int ALERT_CONDITION_NONE_DELETED = -1;

    public static final String ALERT_DEFINITION_PARAM = "ad";

    public static final String ALERT_STATE_PARAM = "alertState";

    /**
     * Alert threshold comparator for equal-to (=).
     */
    public static final String ALERT_THRESHOLD_COMPARATOR_EQ = "=";

    /**
     * Alert threshold comparator for greater-than (>).
     */
    public static final String ALERT_THRESHOLD_COMPARATOR_GT = ">";

    /**
     * Alert threshold comparator for less-than (<).
     */
    public static final String ALERT_THRESHOLD_COMPARATOR_LT = "<";

    /**
     * The request parameter that specifies the resource type of the autogroup resources of the currently viewed
     * resource.
     */
    public static final String AUTOGROUP_TYPE_ID_PARAM = "autogrouptype";

    /**
     * The request parameter name that declares the "cancel" form submit button was clicked.
     */
    public static final String CANCEL_PARAM = "cancel";

    /**
     * The request parameter name that declares the "cancel" form image button was clicked.
     */
    public static final String CANCEL_X_PARAM = CANCEL_PARAM + ".x";

    /**
     * The request parameter name that declares the "okAssign" form submit button was clicked.
     */
    public static final String OK_ASSIGN_PARAM = "okassign";

    /**
     * The request parameter name that declares the "okassign" form image button was clicked.
     */
    public static final String OK_ASSIGN_X_PARAM = OK_ASSIGN_PARAM + ".x";

    /**
     * The request parameter name that declares the "ok" form submit button was clicked.
     */
    public static final String OK_PARAM = "ok";

    /**
     * The request parameter name that declares the "ok" form image button was clicked.
     */
    public static final String OK_X_PARAM = OK_PARAM + ".x";

    /**
     * The request parameter name that declares the "add" form submit button was clicked.
     */
    public static final String ADD_PARAM = "add";

    /**
     * The request parameter name that declares the "add" form image button was clicked.
     */
    public static final String ADD_X_PARAM = ADD_PARAM + ".x";

    /**
     * The request parameter name that declares the "edit" form submit button was clicked.
     */
    public static final String EDIT_PARAM = "edit";

    /**
     * The request parameter name that declares the "edit" form image button was clicked.
     */
    public static final String EDIT_X_PARAM = EDIT_PARAM + ".x";

    /**
     * The request parameter name that declares the "new" form submit button was clicked.
     */
    public static final String NEW_PARAM = "create";

    /**
     * The request parameter name that declares the "new" form image button was clicked.
     */
    public static final String NEW_X_PARAM = NEW_PARAM + ".x";

    /**
     * The request parameter name that declares the "remove" form submit button was clicked.
     */
    public static final String REMOVE_PARAM = "remove";

    /**
     * The request parameter name that declares the "remove" form image button was clicked.
     */
    public static final String REMOVE_X_PARAM = REMOVE_PARAM + ".x";

    /**
     * The request parameter name that declares the "reset" form submit button was clicked.
     */
    public static final String RESET_PARAM = "reset";

    /**
     * The request parameter name that declares the "reset" form image button was clicked.
     */
    public static final String RESET_X_PARAM = RESET_PARAM + ".x";

    public static final String CHILD_RESOURCE_APPDEF_TYPE_ID_PARAM = "appdefTypeId";

    /**
     * The request parameter that specifies the resource type of the child resources of the currently viewed resource.
     */
    public static final String CHILD_RESOURCE_TYPE_ID_PARAM = "ctype";

    /* Request parameter of the batch id name.
     */
    public static final String CONTROL_BATCH_ID_PARAM = "bid";

    /**
     * Request param of the current control action.
     */
    public static final String CONTROL_STATUS_PARAM = "controlStatus";

    /**
     * The request parameter that specifies the ids of the entities to fetch.
     */
    @Deprecated
    public static final String ENTITY_ID_PARAM = "eid";

    /**
     * The request parameter that specifies the ids of the {@link org.rhq.core.domain.resource.Resource}s to fetch.
     */
    public static final String RESOURCE_ID_PARAM = "id";

    /**
     * The request parameter that specifies the ids of the {@link org.rhq.core.domain.resource.group.Group}s to fetch.
     */
    public static final String GROUP_ID_PARAM = "groupId";

    /**
     * The request parameter that specifies the ids of the {@link org.rhq.core.domain.resource.group.GroupDefinition}s
     * to fetch.
     */
    public static final String GROUP_DEFINITION_ID_PARAM = "groupDefinitionId";

    /**
     * The request parameter name that declares the 'filter by' param
     */
    public static final String FILTER_BY_PARAM = "filterBy";

    /**
     * a value for a request paramater that holds the message key that is the reason the user is bieng presented with
     * login page.<br>
     */
    public static final String LOGON_STATUS = "loginStatus";

    /**
     * The request parameters passed for the page requested to be called when the user is authenticated.
     */
    public static final String LOGON_URL_PARAMETERS = "forwardParams";

    public static final String METRIC_ID_PARAM = "m";
    public static final String METRIC_NAME_PARAM = "mname";

    /**
     * Parameter for metric range.
     */
    public static final String METRIC_RANGE = "metricRange";

    /**
     * The request parameter that specifies which page of a list table to display.
     */
    public static final String PAGENUM_PARAM = "pn";

    /**
     * The request parameter that specifies the number of elements in a page of a list table.
     */
    public static final String PAGESIZE_PARAM = "ps";

    /**
     * Parameter for end of range.
     */
    public static final String RANGE_END_DATE = "rangeEndDate";

    /**
     * Parameter for beginning of range.
     */
    public static final String RANGE_START_DATE = "rangeStartDate";

    /**
     * The paramater used to remove a portlet from the dashboard.
     */
    public static final String REM_PORTLET_PARAM = "portletName";

    /**
     * The request parameter that specifies the id of the resource to fetch.
     *
     * @deprecated use {@link #RESOURCE_ID_PARAM} instead
     */
    @Deprecated
    public static final String RESOURCE_PARAM = "rid";

    /**
     * The request parameter that specifies the type of the resource to fetch.
     */
    public static final String RESOURCE_TYPE_ID_PARAM = "type";

    /**
     * The request parameter that specifies the relevant parent resource id.
     */
    public static final String PARENT_RESOURCE_ID_PARAM = "parent";

    /**
     * Request paramater for the map of {@link Permission} names the role has (map values are TRUE)
     */
    public static final String ROLE_PERMISSIONS_PARAM = "rolePermissions";

    //---------------------------------------request parameters

    /**
     * The request parameter that specifies the id of the role to fetch.
     */
    public static final String ROLE_PARAM = "r";

    /**
     * The request parameter that specifies the id of the resource to fetch.
     */
    public static final String SCHEDULE_PARAM = "sid";

    /**
     * The request parameter name that specifies the column on which to sort a list table.
     */
    public static final String SORTCOL_PARAM = "sc";

    /**
     * The request parameter name that specifies the order in which to sort a list table.
     */
    public static final String SORTORDER_PARAM = "so";

    /**
     * The request parameter that specifies the currently viewed url.
     */
    public static final String URL_PARAM = "url";

    /**
     * The request parameter that specifies the id of the user to fetch.
     */
    public static final String USER_PARAM = "u";

    //---------------------- MODE param and its possible values

    /**
     * a standard request paramater.<br>
     */
    public static final String MODE_PARAM = "mode";

    /**
     * a value for a standard request paramater mode.<br>
     * signifies viewing the result of an action.
     */
    public static final String MODE_VIEW_RESULTS = "viewResults";

    /**
     * a value for a standard request paramater mode.<br>
     * signifies viewing an item.
     */
    public static final String MODE_VIEW = "view";

    /**
     * a value for a standard request paramater mode.<br>
     * signifies viewing the result of an action.
     */
    public static final String MODE_RESULTS = "results";

    /**
     * a value for a standard request paramater mode.<br>
     * signifies deleting an item.
     */
    public static final String MODE_REMOVE = "remove";

    /**
     * a value for a standard request paramater mode.<br>
     * signify editing a users password
     */
    public static final String MODE_REGISTER = "register";

    /**
     * a value for a standard request paramater mode.<br>
     * signifies creating a new item
     */
    public static final String MODE_NEW = "new";

    /**
     * a value for a standard request paramater mode.<br>
     * signify displaying a list of items
     */
    public static final String MODE_LIST = "list";

    /**
     * a value for a standard request paramater mode.<br>
     * signify displaying a list of scheduled control items
     */
    public static final String MODE_HST_DETAIL = "hstDetail";

    /**
     * A value for a standard request paramater mode.<br>
     * Signifies the mode screen where it is displaying a history of actions
     */
    public static final String MODE_HST = "history";

    /**
     * a value for a standard request paramater mode.<br>
     * signifies editing the type-specific properties of a resource.
     */
    public static final String MODE_EDIT_TYPE = "editType";

    /**
     * a value for a standard request paramater mode.<br>
     * signifies editing a resource item's resource-specific properties
     */
    public static final String MODE_EDIT_RESOURCE = "editResource";

    /**
     * a value for a standard request paramater mode.<br>
     * signify editing a users password
     */
    public static final String MODE_EDIT_PASS = "editPass";

    /**
     * a value for a standard request paramater mode.<br>
     * signifies editing the type-specific properties of a resource.
     */
    public static final String MODE_EDIT_CONFIG = "editConfig";

    /**
     * a value for a standard request parameter mode.<br>
     * signifies editing an item
     */
    public static final String MODE_EDIT = "edit";

    /**
     * a value for a standard request paramater mode.<br>
     * signify displaying a list of control actions that have been perfomed
     */
    public static final String MODE_CRNT_DETAIL = "crntDetail";

    /**
     * a constant for a standard request paramater mode.<br>
     * signifies configuring the resource.
     */
    public static final String MODE_CONFIGURE = "configure";

    /**
     * a value for a standard request paramater mode.<br>
     * signifies changing the owner of a resource.
     */
    public static final String MODE_CHANGE_OWNER = "changeOwner";

    /**
     * a value for a standard request paramater mode.<br>
     * signify adding users for a resource.
     */
    public static final String MODE_ADD_USERS = "addUsers";

    public static final String MODE_ADD_USER = "addUser";

    /**
     * a value for a standard request paramater mode.<br>
     * signify adding services to a resource (like, an applicaiton).
     */
    public static final String MODE_ADD_SERVICES = "addServices";

    /**
     * a value for a standard request paramater mode.<br>
     * signify displaying a list of items
     */
    public static final String MODE_ADD_ROLES = "addRoles";

    public static final String MODE_ADD_ROLE = "addRole";

    /**
     * a value for a standard request paramater mode.<br>
     * signify adding resources to a group.
     */
    public static final String MODE_ADD_RESOURCES = "addResources";

    /**
     * a value for a standard request paramater mode.<br>
     * signify adding groups for a resource.
     */
    public static final String MODE_ADD_GROUPS = "addGroups";

    /**
     * a value for a standard request paramater mode.<br>
     * signify displaying a list of items
     */
    public static final String MODE_ADD = "add";

    /**
     * Chart a single metric for a single resource.
     */
    public static final String MODE_MON_CHART_SMSR = "chartSingleMetricSingleResource";

    /**
     * Chart a single metric for multiple resources.
     */
    public static final String MODE_MON_CHART_SMMR = "chartSingleMetricMultiResource";

    /**
     * Chart multiple metrics for a single resource.
     */
    public static final String MODE_MON_CHART_MMSR = "chartMultiMetricSingleResource";

    /**
     * Compare metrics of the same MeasurementTemplate for multiple resources.
     */
    public static final String MODE_MON_COMPARE_METRICS = "compareMetrics";

    /**
     * Display expanded metric view with metadata
     */
    public static final String MODE_MON_METRIC_METADATA = "metricMetadata";

    /**
     * Set mode of something to 'enabled
     */
    public static final String MODE_ENABLED = "enabled";
    public static final String MODE_DISABLED = "disabled";

    /**
     * Set the mode as install
     */
    public static final String MODE_INSTALL = "install";

    /**
     * Set the mode as uninstall
     */
    public static final String MODE_MANUAL_UNINSTALL = "manualUninstall";

    /**
     * Constant for removing a control action from an alert definition.
     */
    public static final String CONTROL_ACTION_NONE = "none";

    /*
     * Request parameter for specifying the availability check timeout
     */
    public static final String AVAILABILITY_TIMEOUT_PARAM = "timeout";

    /**
     * Constants for indicator views
     */
    public static final String PARAM_VIEW = "view";
    public static final String PARAM_VAL_DELIMITOR = ",";

    /**
     * @deprecated use {@link #RESOURCE_TYPE_ID_PARAM} instead
     */
    @Deprecated
    public static final String APPDEF_RES_TYPE_ID = "aetid";

    /**
     * A request parameter that specifies the {@link AbstractResourceConfigurationUpdate} id for a Resource
     * Configuration to be viewed or edited.
     */
    String CONFIG_ID_PARAM = "configId";

    String LIST_NAME_PARAM = "listName";

    String LIST_INDEX_PARAM = "listIndex";
}