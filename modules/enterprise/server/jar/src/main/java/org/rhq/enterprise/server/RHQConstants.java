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

import org.rhq.core.domain.common.composite.SystemProperty;

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

    // JAAS settings      
    @Deprecated public static final String JAASProvider = SystemProperty.LDAP_BASED_JAAS_PROVIDER.getInternalName();
    //these are values that correspond to the booleans of the JAASProvider that actually go into the database..
    public static final String JDBCJAASProvider = "JDBC";
    public static final String LDAPJAASProvider = "LDAP";

    // LDAP JAAS properties
    @Deprecated public static final String LDAPFactory = SystemProperty.LDAP_NAMING_FACTORY.getInternalName();
    @Deprecated public static final String LDAPUrl = SystemProperty.LDAP_NAMING_PROVIDER_URL.getInternalName();
    @Deprecated public static final String LDAPProtocol = SystemProperty.USE_SSL_FOR_LDAP.getInternalName();
    //these are the two values in the database that correspond to the booleans of "USE_SSL_FOR_LDAP"
    public static final String LDAP_PROTOCOL_UNSECURED = "";
    public static final String LDAP_PROTOCOL_SECURED = "ssl";
    
    @Deprecated public static final String LDAPLoginProperty = SystemProperty.LDAP_LOGIN_PROPERTY.getInternalName();
    @Deprecated public static final String LDAPFilter = SystemProperty.LDAP_FILTER.getInternalName();
    @Deprecated public static final String LDAPGroupFilter = SystemProperty.LDAP_GROUP_FILTER.getInternalName();
    @Deprecated public static final String LDAPGroupMember = SystemProperty.LDAP_GROUP_MEMBER.getInternalName();
    @Deprecated public static final String LDAPBaseDN = SystemProperty.LDAP_BASE_DN.getInternalName();
    @Deprecated public static final String LDAPBindDN = SystemProperty.LDAP_BIND_DN.getInternalName();
    @Deprecated public static final String LDAPBindPW = SystemProperty.LDAP_BIND_PW.getInternalName();

    // DRIFT Server properties
    @Deprecated public static final String ACTIVE_DRIFT_PLUGIN = SystemProperty.ACTIVE_DRIFT_PLUGIN.getInternalName();

    // Base URL for the application - (i think this is only used for alert emails)
    @Deprecated public static final String BaseURL = SystemProperty.BASE_URL.getInternalName();

    // how long can an agent be quiet (i.e. not send an avail report) before we consider it down and backfill it
    @Deprecated public static final String AgentMaxQuietTimeAllowed = SystemProperty.AGENT_MAX_QUIET_TIME_ALLOWED.getInternalName();

    // Are we allowing automatic AgentUpdate binary download
    @Deprecated public static final String EnableAgentAutoUpdate = SystemProperty.AGENT_AUTO_UPDATE_ENABLED.getInternalName();

    // Are we rendering a special "debug" menu for administrators?
    @Deprecated public static final String EnableDebugMode = SystemProperty.DEBUG_MODE_ENABLED.getInternalName();

    // Are we rendering features which are marked as experimental for this installation?
    @Deprecated public static final String EnableExperimentalFeatures = SystemProperty.EXPERIMENTAL_FEATURES_ENABLED.getInternalName();

    // How long do we keep data compressed in hourly intervals?
    @Deprecated public static final String DataPurge1Hour = SystemProperty.DATA_PURGE_1H_PERIOD.getInternalName();

    // How long do we keep data compressed in 6 hour intervals?
    @Deprecated public static final String DataPurge6Hour = SystemProperty.DATA_PURGE_6H_PERIOD.getInternalName();

    // How long do we keep data compressed in 1 day intervals?
    @Deprecated public static final String DataPurge1Day = SystemProperty.DATA_PURGE_1D_PERIOD.getInternalName();

    // How often to perform database maintenance
    @Deprecated public static final String DataMaintenance = SystemProperty.DATA_MAINTENANCE_PERIOD.getInternalName();

    // Whether or not to reindex nightly
    @Deprecated public static final String DataReindex = SystemProperty.DATA_REINDEX_NIGHTLY.getInternalName();

    // How long do we keep rt data
    @Deprecated public static final String RtDataPurge = SystemProperty.RT_DATA_PURGE_PERIOD.getInternalName();

    // How long do we keep alerts
    @Deprecated public static final String AlertPurge = SystemProperty.ALERT_PURGE_PERIOD.getInternalName();

    // How long do we keep event data
    @Deprecated public static final String EventPurge = SystemProperty.EVENT_PURGE_PERIOD.getInternalName();

    // How long do we keep orphaned drift files
    @Deprecated public static final String DriftFilePurge = SystemProperty.DRIFT_FILE_PURGE_PERIOD.getInternalName();

    // How long do we keep trait data
    @Deprecated public static final String TraitPurge = SystemProperty.TRAIT_PURGE_PERIOD.getInternalName();

    // How long do we keep availability data
    @Deprecated public static final String AvailabilityPurge = SystemProperty.AVAILABILITY_PURGE_PERIOD.getInternalName();

    // Baseline config options
    // The frequency to run auto-baselines, if 0, never auto-calculate baselines
    @Deprecated public static final String BaselineFrequency = SystemProperty.BASE_LINE_FREQUENCY.getInternalName();

    // How much data to include
    @Deprecated public static final String BaselineDataSet = SystemProperty.BASE_LINE_DATASET.getInternalName();

    //allow plugin initiated resource name & description upgrades (resource key is always upgradable)
    @Deprecated public static final String AllowResourceGenericPropertiesUpgrade = SystemProperty.ALLOW_RESOURCE_GENERIC_PROPERTIES_UPGRADE.getInternalName();

    /////////////////////////////////////////////////////
    // the settings below are not used today and can probably be removed at some point

    // Help related
    public static final String HelpUser = "CAM_HELP_USER";
    public static final String HelpUserPassword = "CAM_HELP_PASSWORD";

    // Syslog Actions enabled
    public static final String SyslogActionsEnabled = "CAM_SYSLOG_ACTIONS_ENABLED";

}