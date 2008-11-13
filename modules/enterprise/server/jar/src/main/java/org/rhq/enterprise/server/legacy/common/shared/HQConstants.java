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
package org.rhq.enterprise.server.legacy.common.shared;

import org.rhq.enterprise.server.core.jaas.JDBCLoginModule;
import org.rhq.enterprise.server.core.jaas.LdapLoginModule;

/**
 * Global constants file to be used for Config Properties, as well as any other constant used across subsystems
 *
 * @deprecated
 */
@Deprecated
public class HQConstants {
    public static final String JAASProvider = "CAM_JAAS_PROVIDER";

    /**
     * Valid JAAS Providers *
     */
    public static final String JDBCJAASProvider = "JDBC";
    public static final String LDAPJAASProvider = "LDAP";

    /**
     * JAAS Provider class names *
     */
    public static final String JDBCJAASProviderClass = JDBCLoginModule.class.getName();
    public static final String LDAPJAASProviderClass = LdapLoginModule.class.getName();

    // LDAP Configuration props
    public static final String LDAPFactory = "CAM_LDAP_NAMING_FACTORY_INITIAL";
    public static final String LDAPUrl = "CAM_LDAP_NAMING_PROVIDER_URL";
    public static final String LDAPProtocol = "CAM_LDAP_PROTOCOL";
    public static final String LDAPLoginProperty = "CAM_LDAP_LOGIN_PROPERTY";
    public static final String LDAPFilter = "CAM_LDAP_FILTER";
    public static final String LDAPBaseDN = "CAM_LDAP_BASE_DN";
    public static final String LDAPBindDN = "CAM_LDAP_BIND_DN";
    public static final String LDAPBindPW = "CAM_LDAP_BIND_PW";

    /**
     * Base URL for the application *
     */
    public static final String BaseURL = "CAM_BASE_URL";

    // how long can an agent be quiet (i.e. not send an avail report) before we consider it down and backfill it
    public static final String AgentMaxQuietTimeAllowed = "AGENT_MAX_QUIET_TIME_ALLOWED";

    // how long can an agent be quiet (i.e. not send an avail report) before we consider it down and backfill it
    public static final String EnableAgentAutoUpdate = "ENABLE_AGENT_AUTO_UPDATE";

    // Data storage options (All in ms)
    // How long do we keep raw metric data?
    /**
     * @deprecated
     */
    @Deprecated
    public static final String DataPurgeRaw = "CAM_DATA_PURGE_RAW";

    // How long do we keep data compressed in hourly intervals?
    public static final String DataPurge1Hour = "CAM_DATA_PURGE_1H";

    // How long do we keep data compressed in 6 hour intervals?
    public static final String DataPurge6Hour = "CAM_DATA_PURGE_6H";

    // How long do we keep data compressed in 1 day intervals?
    public static final String DataPurge1Day = "CAM_DATA_PURGE_1D";

    // How often to perform database maintainence
    public static final String DataMaintenance = "CAM_DATA_MAINTENANCE";

    // Whether or not to store every data point
    public static final String DataStoreAll = "DATA_STORE_ALL";

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

    // When was the last time the baselines were auto-calculated (in epoch millis)
    public static final String BaselineLastCalculationTime = "CAM_BASELINE_LASTTIME";

    // Help related
    public static final String HelpUser = "CAM_HELP_USER";
    public static final String HelpUserPassword = "CAM_HELP_PASSWORD";

    // High Availability related
    public static final String HighAvailAddress = "CAM_MULTICAST_ADDRESS";
    public static final String HighAvailPort = "CAM_MULTICAST_PORT";

    // Syslog Actions enabled
    public static final String SyslogActionsEnabled = "CAM_SYSLOG_ACTIONS_ENABLED";

    // SNMP configurations
    public static final String SNMPAuthProtocol = "SNMP_AUTH_PROTOCOL";
    public static final String SNMPAuthPassphrase = "SNMP_AUTH_PASSPHRASE";
    public static final String SNMPPrivacyPassphrase = "SNMP_PRIV_PASSPHRASE";
    public static final String SNMPCommunity = "SNMP_COMMUNITY";
    public static final String SNMPEngineID = "SNMP_ENGINE_ID";
    public static final String SNMPContextName = "SNMP_CONTEXT_NAME";
    public static final String SNMPSecurityName = "SNMP_SECURITY_NAME";
    public static final String SNMPTrapOID = "SNMP_TRAP_OID";
    public static final String SNMPEnterpriseOID = "SNMP_ENTERPRISE_OID";
    public static final String SNMPGenericID = "SNMP_GENERIC_ID";
    public static final String SNMPSpecificID = "SNMP_SPECIFIC_ID";
    public static final String SNMPAgentAddress = "SNMP_AGENT_ADDRESS";
    public static final String SNMPVersion = "SNMP_VERSION";
    public static final String SNMPPrivacyProtocol = "SNMP_PRIVACY_PROTOCOL";

}