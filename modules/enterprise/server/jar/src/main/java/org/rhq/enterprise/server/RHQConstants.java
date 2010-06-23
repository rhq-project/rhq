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
package org.rhq.enterprise.server;

/**
 * Global constants file to be used for config Properties, as well as any other constant used across subsystems
 *
 * NOTE: The defaults for these properties can be found in sysconfig-data.xml in the dbutils module.
 */
public class RHQConstants {
    public static final String PRODUCT_NAME = "RHQ";
    public static final String EAR_NAME = "rhq";
    public static final String EAR_FILE_NAME = EAR_NAME + ".ear";
    public static final String ENTITY_MANAGER_JNDI_NAME = "java:/RHQEntityManagerFactory";
    public static final String DATASOURCE_JNDI_NAME = "java:/RHQDS";
    public static final String PERSISTENCE_UNIT_NAME = "rhqpu";
    public static final String TRANSACTION_MANAGER_JNDI_NAME = "java:/TransactionManager";

    public static final String JAASProvider = "CAM_JAAS_PROVIDER";

    /**
     * Valid JAAS Providers *
     */
    public static final String JDBCJAASProvider = "JDBC";
    public static final String LDAPJAASProvider = "LDAP";

    // LDAP Configuration props
    public static final String LDAPFactory = "CAM_LDAP_NAMING_FACTORY_INITIAL";
    public static final String LDAPUrl = "CAM_LDAP_NAMING_PROVIDER_URL";
    public static final String LDAPProtocol = "CAM_LDAP_PROTOCOL";
    public static final String LDAPLoginProperty = "CAM_LDAP_LOGIN_PROPERTY";
    public static final String LDAPFilter = "CAM_LDAP_FILTER";
    public static final String LDAPGroupFilter = "CAM_LDAP_GROUP_FILTER";
    public static final String LDAPGroupMember = "CAM_LDAP_GROUP_MEMBER";
    public static final String LDAPBaseDN = "CAM_LDAP_BASE_DN";
    public static final String LDAPBindDN = "CAM_LDAP_BIND_DN";
    public static final String LDAPBindPW = "CAM_LDAP_BIND_PW";

    /**
     * Base URL for the application *
     */
    public static final String BaseURL = "CAM_BASE_URL";

    // how long can an agent be quiet (i.e. not send an avail report) before we consider it down and backfill it
    public static final String AgentMaxQuietTimeAllowed = "AGENT_MAX_QUIET_TIME_ALLOWED";

    // Are we allowing automatic AgentUpdate binary download
    public static final String EnableAgentAutoUpdate = "ENABLE_AGENT_AUTO_UPDATE";


   // Are we rendering a special "debug" menu for administrators?
    public static final String EnableDebugMode = "ENABLE_DEBUG_MODE";

    // How long do we keep data compressed in hourly intervals?
    public static final String DataPurge1Hour = "CAM_DATA_PURGE_1H";

    // How long do we keep data compressed in 6 hour intervals?
    public static final String DataPurge6Hour = "CAM_DATA_PURGE_6H";

    // How long do we keep data compressed in 1 day intervals?
    public static final String DataPurge1Day = "CAM_DATA_PURGE_1D";

    // How often to perform database maintainence
    public static final String DataMaintenance = "CAM_DATA_MAINTENANCE";

    // Whether or not to reindex nightly
    public static final String DataReindex = "DATA_REINDEX_NIGHTLY";

    // How long do we keep rt data
    public static final String RtDataPurge = "RT_DATA_PURGE";

    // How long do we keep alerts
    public static final String AlertPurge = "ALERT_PURGE";

    // How long do we keep event data
    public static final String EventPurge = "EVENT_PURGE";

    // How long do we keep trait data
    public static final String TraitPurge = "TRAIT_PURGE";

    // How long do we keep availability data
    public static final String AvailabilityPurge = "AVAILABILITY_PURGE";

    // Baseline config options
    // The frequency to run auto-baselines, if 0, never auto-calculate baselines
    public static final String BaselineFrequency = "CAM_BASELINE_FREQUENCY";

    // How much data to include
    public static final String BaselineDataSet = "CAM_BASELINE_DATASET";

    // Help related
    public static final String HelpUser = "CAM_HELP_USER";
    public static final String HelpUserPassword = "CAM_HELP_PASSWORD";

    // Syslog Actions enabled
    public static final String SyslogActionsEnabled = "CAM_SYSLOG_ACTIONS_ENABLED";
    
    //allow plugin initiated resource name & description upgrades (resource key is always upgradable)
    public static final String AllowResourceGenericPropertiesUpgrade = "RESOURCE_GENERIC_PROPERTIES_UPGRADE";

}