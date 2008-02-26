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

/**
 * Constant values used as keys in maps
 */
public interface KeyConstants {
    /**
     * Keys in the WebUser.preferences (i.e., in the userprefs ConfigResponse) XXX NOTE: These are mostly hardcoded all
     * over the place (LAME). I'm starting to move these over one-by-one whenver I work with one of them. And even the
     * ones defined here are still hardcoded in the JSPs, because it's often difficult to reference constants in the
     * contexts in which they are used.
     */
    public static final String USERPREF_KEY_FAVORITE_RESOURCES = ".dashContent.resourcehealth.resources";

    /**
     * The key that holds the user's chart queries
     */
    public static final String USER_DASHBOARD_CHARTS = ".dashContent.charts";

    /**
     * the the user preferences key that holds the users portal second column choices.
     */
    public static final String USER_PORTLETS_SECOND = ".dashcontent.portal.portlets.second";

    /**
     * the the user preferences key that holds the users portal first column portlet choices.
     */
    public static final String USER_PORTLETS_FIRST = ".dashcontent.portal.portlets.first";

    public static final String HELP_BASE_URL_KEY = "helpBaseURL";

    /**
     * key that will contain the cam specific page title context.
     */
    public static final String PAGE_TITLE_KEY = "camTitle";

    public static final String LOGON_URL_KEY = "forwardURL";

    /*
     * The top level tab controls for the monitoring screen link to the "Current Health" view
     */
    public static final String MODE_MON_CUR = "currentHealth";

    /*
     * The top level tab controls for the monitoring screen link to the "Resource Metrics" view
     */
    public static final String MODE_MON_RES_METS = "resourceMetrics";

    /*
     * The top level tab controls for the monitoring screen link to the "Deployed Services" view
     */
    public static final String MODE_MON_DEPL_SVRS = "deployedServers";

    /*
     * The top level tab controls for the monitoring screen link to the "Deployed Services" view
     */
    public static final String MODE_MON_DEPL_SVCS = "deployedServices";

    /*
     * The top level tab controls for the monitoring screen link to the "Internal Services" view
     */
    public static final String MODE_MON_INTERN_SVCS = "internalServices";

    /*
     * The top level tab controls for the monitoring screen link to the "Performance" view
     */
    public static final String MODE_MON_PERF = "performance";

    /*
     * The top level tab controls for the monitoring screen link to the "Events" view
     */
    public static final String MODE_MON_EVENT = "events";

    /*
     * The top level tab controls for the monitoring screen link to the "URL Detail" view
     */
    public static final String MODE_MON_URL = "url";

    /*
     * The current health page links to the deployed child resource types
     */
    public static final String INTERN_CHILD_MODE_ATTR = "internal";

    /*
     * The current health page links to the internal child resource types
     */
    public static final String DEPL_CHILD_MODE_ATTR = "deployed";

    /*
     * The top level tab controls for the monitoring screen link to the "Internal Services" or "deployed" view
     */
    public static final String MON_SVC_TYPE = "serviceType";

    /*
     * The top level tab controls for the monitoring screen link to the "Edit Metric Value Range" view
     */
    public static final String MODE_MON_EDIT_RANGE = "editRange";

    /*
     * Mode for adding metrics to a resource.
     */
    public static final String MODE_ADD_METRICS = "addMetrics";

    /**
     * key value for testMode string stored in System.properties
     */
    public static final String MOCK_TEST_MODE = "net.hyperic.hq.system.mockTestMode";

    /**
     * key value for testMode string stored in System.properties
     */
    public static final String MOCK_AUTH_BOSS = "net.hyperic.hq.system.mockAuthBoss";

    /**
     * key value for testMode string stored in System.properties
     */
    public static final String MOCK_AUTHZ_BOSS = "net.hyperic.hq.system.mockAuthzBoss";

    /**
     * key value for testMode string stored in System.properties
     */
    public static final String MOCK_APPDEF_BOSS = "net.hyperic.hq.system.mockAppdefBoss";

    /**
     * key value for testMode string stored in System.properties
     */
    public static final String MOCK_CONTROL_BOSS = "net.hyperic.hq.system.mockControlBoss";

    /**
     * key value for testMode string stored in System.properties
     */
    public static final String MOCK_AI_BOSS = "net.hyperic.hq.system.mockAIBoss";

    /**
     * key value for testMode string stored in System.properties
     */
    public static final String MOCK_PRODUCT_BOSS = "net.hyperic.hq.system.mockProductBoss";

    /**
     * key value for testMode string stored in System.properties
     */
    public static final String MOCK_MEASUREMENT_BOSS = "net.hyperic.hq.system.mockMeasurementBoss";

    /**
     * key values for indicator views
     */
    public static final String INDICATOR_VIEWS = "monitor.visibility.indicator.views.";

    public static final String DEFAULT_INDICATOR_VIEW = "resource.common.monitor.visibility.defaultview";
}