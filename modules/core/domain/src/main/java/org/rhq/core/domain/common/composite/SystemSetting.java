/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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

package org.rhq.core.domain.common.composite;

import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;

/**
 * This is an enum - ADD NEW ENTRIES AT THE END TO PRESERVE ORDINAL VALUES!
 */
public enum SystemSetting {
    /** Base URL for the application - (i think this is only used for alert emails) */
    BASE_URL("CAM_BASE_URL", PropertySimpleType.STRING, false, true, true),

    /** how long can an agent be quiet (i.e. not send an avail report) before we consider it down and backfill it */
    AGENT_MAX_QUIET_TIME_ALLOWED("AGENT_MAX_QUIET_TIME_ALLOWED", PropertySimpleType.LONG, false, true, true),

    /** Are we allowing automatic AgentUpdate binary download */
    AGENT_AUTO_UPDATE_ENABLED("ENABLE_AGENT_AUTO_UPDATE", PropertySimpleType.BOOLEAN, false, true, true),

    /** Are we rendering a special "debug" menu for administrators? */
    DEBUG_MODE_ENABLED("ENABLE_DEBUG_MODE", PropertySimpleType.BOOLEAN, false, true, true),

    /** Are we rendering features which are marked as experimental for this installation? */
    EXPERIMENTAL_FEATURES_ENABLED("ENABLE_EXPERIMENTAL_FEATURES", PropertySimpleType.BOOLEAN, false, true, true),

    /** How often to perform database maintenance */
    DATA_MAINTENANCE_PERIOD("CAM_DATA_MAINTENANCE", PropertySimpleType.LONG, false, true, true),

    /** How long do we keep availability data */
    AVAILABILITY_PURGE_PERIOD("AVAILABILITY_PURGE", PropertySimpleType.LONG, false, true, true),

    /** How long do we keep alerts */
    ALERT_PURGE_PERIOD("ALERT_PURGE", PropertySimpleType.LONG, false, true, true),

    /** How long do we keep trait data */
    TRAIT_PURGE_PERIOD("TRAIT_PURGE", PropertySimpleType.LONG, false, true, true),

    /** How long do we keep rt data */
    RT_DATA_PURGE_PERIOD("RT_DATA_PURGE", PropertySimpleType.LONG, false, true, true),

    /** How long do we keep event data */
    EVENT_PURGE_PERIOD("EVENT_PURGE", PropertySimpleType.LONG, false, true, true),

    /** How long do we keep orphaned drift files */
    DRIFT_FILE_PURGE_PERIOD("DRIFT_FILE_PURGE", PropertySimpleType.LONG, false, true, true),

    /** Whether or not to reindex nightly */
    DATA_REINDEX_NIGHTLY("DATA_REINDEX_NIGHTLY", PropertySimpleType.BOOLEAN, false, true, true),

    /**
     * Baseline config options
     * The frequency to run auto-baselines, if 0, never auto-calculate baselines
     */
    BASE_LINE_FREQUENCY("CAM_BASELINE_FREQUENCY", PropertySimpleType.LONG, false, true, true),

    /** How much data to include */
    BASE_LINE_DATASET("CAM_BASELINE_DATASET", PropertySimpleType.LONG, false, true, true),

    LDAP_BASED_JAAS_PROVIDER("CAM_JAAS_PROVIDER", PropertySimpleType.BOOLEAN, false, true, true),

    LDAP_NAMING_PROVIDER_URL("CAM_LDAP_NAMING_PROVIDER_URL", PropertySimpleType.STRING, false, true, true),

    USE_SSL_FOR_LDAP("CAM_LDAP_PROTOCOL", PropertySimpleType.BOOLEAN, false, true, true),

    LDAP_LOGIN_PROPERTY("CAM_LDAP_LOGIN_PROPERTY", PropertySimpleType.STRING, false, false, true),

    LDAP_FILTER("CAM_LDAP_FILTER", PropertySimpleType.STRING, false, false, true),

    LDAP_GROUP_FILTER("CAM_LDAP_GROUP_FILTER", PropertySimpleType.STRING, false, false, true),

    LDAP_GROUP_MEMBER("CAM_LDAP_GROUP_MEMBER", PropertySimpleType.STRING, false, false, true),

    LDAP_GROUP_PAGING("CAM_LDAP_GROUP_PAGING", PropertySimpleType.BOOLEAN, false, true, true),

    LDAP_GROUP_QUERY_PAGE_SIZE("CAM_LDAP_GROUP_QUERY_PAGE_SIZE", PropertySimpleType.LONG, false, true, true),

    LDAP_BASE_DN("CAM_LDAP_BASE_DN", PropertySimpleType.STRING, false, false, true),

    LDAP_BIND_DN("CAM_LDAP_BIND_DN", PropertySimpleType.STRING, false, false, true),

    LDAP_BIND_PW("CAM_LDAP_BIND_PW", PropertySimpleType.PASSWORD, false, false, true),

    LDAP_NAMING_FACTORY("CAM_LDAP_NAMING_FACTORY_INITIAL", PropertySimpleType.STRING, true, true, true),

    LDAP_GROUP_USE_POSIX("CAM_LDAP_GROUP_USE_POSIX", PropertySimpleType.BOOLEAN, false, true, true),

    ACTIVE_DRIFT_PLUGIN("ACTIVE_DRIFT_PLUGIN", PropertySimpleType.STRING, false, true, true),

    /**
     * allow plugin initiated resource name & description upgrades (resource key is always upgradable)
     * making this readonly because it is not currently supported by UI code
     */
    ALLOW_RESOURCE_GENERIC_PROPERTIES_UPGRADE("RESOURCE_GENERIC_PROPERTIES_UPGRADE", PropertySimpleType.BOOLEAN, true,
        true, true),

    /**
     * @deprecated This attribute is no longer maintained and might not reflect the true version of the running server.
     * Use {@code SystemManagerRemote.getProductInfo()} method to get at an equivalent and always up-to-date
     * information.
     */
    @Deprecated
    SERVER_VERSION("SERVER_VERSION", PropertySimpleType.STRING, true, true, true),

    DB_SCHEMA_VERSION("DB_SCHEMA_VERSION", PropertySimpleType.STRING, true, true, true),

    /** @deprecated since RHQ 4.8.0. This is not used anymore */
    @Deprecated
    DATA_PURGE_1H_PERIOD("CAM_DATA_PURGE_1H", PropertySimpleType.LONG, true, true, true),

    /** @deprecated since RHQ 4.8.0. This is not used anymore */
    @Deprecated
    DATA_PURGE_6H_PERIOD("CAM_DATA_PURGE_6H", PropertySimpleType.LONG, true, true, true),

    /** @deprecated since RHQ 4.8.0. This is not used anymore */
    @Deprecated
    DATA_PURGE_1D_PERIOD("CAM_DATA_PURGE_1D", PropertySimpleType.LONG, true, true, true),

    /** The length of CoreGUI inactivity (no call to UserSessionManager.refresh()) before a CoreGUI session timeout, Default: 1 hour */
    RHQ_SESSION_TIMEOUT("RHQ_SESSION_TIMEOUT", PropertySimpleType.LONG, false, true, true),

    /**
     * The STORAGE_* settings are all read-only and private and deal with shared, cluster-wide settings
     * among storage nodes. They are read-only and private because they should only be updated through
     * the storage subsystem. The username and password should not be updated at all.
     */
    STORAGE_CQL_PORT("STORAGE_CQL_PORT", PropertySimpleType.INTEGER, true, true, false),

    /**
     * The STORAGE_* settings are all read-only and private and deal with shared, cluster-wide settings
     * among storage nodes. They are read-only and private because they should only be updated through
     * the storage subsystem. The username and password should not be updated at all.
     */
    STORAGE_GOSSIP_PORT("STORAGE_GOSSIP_PORT", PropertySimpleType.INTEGER, true, true, false),

    /**
     * The STORAGE_* settings are all read-only and private and deal with shared, cluster-wide settings
     * among storage nodes. They are read-only and private because they should only be updated through
     * the storage subsystem. The username and password should not be updated at all.
     */
    STORAGE_AUTOMATIC_DEPLOYMENT("STORAGE_AUTOMATIC_DEPLOYMENT", PropertySimpleType.BOOLEAN, true, true, false),

    /**
     * The STORAGE_* settings are all read-only and private and deal with shared, cluster-wide settings
     * among storage nodes. They are read-only and private because they should only be updated through
     * the storage subsystem. The username and password should not be updated at all.
     */
    STORAGE_USERNAME("STORAGE_USERNAME", PropertySimpleType.STRING, true, true, false),

    /**
     * The STORAGE_* settings are all read-only and private and deal with shared, cluster-wide settings
     * among storage nodes. They are read-only and private because they should only be updated through
     * the storage subsystem. The username and password should not be updated at all.
     */
    STORAGE_PASSWORD("STORAGE_PASSWORD", PropertySimpleType.PASSWORD, true, true, false),

    STORAGE_SCHEMA_REPLICATION_FACTOR("STORAGE_SCHEMA_REPLICATION_FACTOR", PropertySimpleType.INTEGER, true, true,
        false),

    //these seem to be unused yet still present in the database...
    @Deprecated
    HELP_USER("CAM_HELP_USER", PropertySimpleType.STRING, true, false, true),

    @Deprecated
    HELP_PASSWORD("CAM_HELP_PASSWORD", PropertySimpleType.PASSWORD, true, false, true),

    @Deprecated
    SYSLOG_ACTIONS_ENABLED("CAM_SYSLOG_ACTIONS_ENABLED", PropertySimpleType.STRING, true, false, true),

    @Deprecated
    GUIDE_ENABLED("CAM_GUIDE_ENABLED", PropertySimpleType.STRING, true, false, true),

    @Deprecated
    RT_COLLECT_IP_ADDRS("CAM_RT_COLLECT_IP_ADDRS", PropertySimpleType.STRING, true, false, true),

    /**
     * This is a default username that can be used to connect to a remote SSH box.
     * Right now, it's used when remotely controlling/installing/uninstalling agents.
     */
    REMOTE_SSH_USERNAME_DEFAULT("REMOTE_SSH_USERNAME_DEFAULT", PropertySimpleType.STRING, false, false, true),

    /**
     * This is a default password that can be used to connect to a remote SSH box.
     * Right now, it's used when remotely controlling/installing/uninstalling agents.
     */
    REMOTE_SSH_PASSWORD_DEFAULT("REMOTE_SSH_PASSWORD_DEFAULT", PropertySimpleType.PASSWORD, false, false, true),

    /** Is user without any assigned role allowed to see the web UI */
    LOGIN_WITHOUT_ROLES_ENABLED("ENABLE_LOGIN_WITHOUT_ROLES", PropertySimpleType.BOOLEAN, false, true, true),

    /**
     * Setting this property deactivates the metrics_cache table. Data will not be pulled from metrics_cache until
     * the time specified by this property has been reached. This applies to both past and current data. There are two
     * use cases for this - 1) data migration during upgrade and 2) cache partition resizing (i.e., number of schedules
     * stored per partition)
     */
    METRICS_CACHE_ACTIVATION_TIME("METRICS_CACHE_ACTIVATION_TIME", PropertySimpleType.LONG, true, false, false),

    LDAP_FOLLOW_REFERRALS("CAM_LDAP_FOLLOW_REFERRALS", PropertySimpleType.BOOLEAN, false, true, true),

    HTTP_PROXY_SERVER_HOST("HTTP_PROXY_SERVER_HOST", PropertySimpleType.STRING, false, false, true),

    HTTP_PROXY_SERVER_PORT("HTTP_PROXY_SERVER_PORT", PropertySimpleType.INTEGER, false, true, true),

    HTTP_PROXY_SERVER_USERNAME("HTTP_PROXY_SERVER_USERNAME", PropertySimpleType.STRING, false, false, true),

    HTTP_PROXY_SERVER_PASSWORD("HTTP_PROXY_SERVER_PASSWORD", PropertySimpleType.PASSWORD, false, false, true),

    /**
     * This is a bit magical, because the system itself updates it if any other property gets updated even though
     * this setting itself is readonly.
     */
    LAST_SYSTEM_CONFIG_UPDATE_TIME("LAST_SYSCONFIG_UPDATE_TIME", PropertySimpleType.LONG, true, false, false),

    /** How long do we keep operation history data */
    OPERATION_HISTORY_PURGE_PERIOD("OPERATION_HISTORY_PURGE", PropertySimpleType.LONG, false, true, true),

    /**
     * Enable/Disable snapshots management for storage cluster
     * 
     * The STORAGE_* settings are all read-only and private and deal with shared, cluster-wide settings
     * among storage nodes. They are read-only and private because they should only be updated through
     * the storage subsystem. The username and password should not be updated at all.
     */
    STORAGE_REGULAR_SNAPSHOTS("STORAGE_REGULAR_SNAPSHOTS", PropertySimpleType.BOOLEAN, true, true, false),

    /**
     * Cron Expression schedule for creating regular snapshots
     * 
     * The STORAGE_* settings are all read-only and private and deal with shared, cluster-wide settings
     * among storage nodes. They are read-only and private because they should only be updated through
     * the storage subsystem. The username and password should not be updated at all.
     */
    STORAGE_REGULAR_SNAPSHOTS_SCHEDULE("STORAGE_REGULAR_SNAPSHOTS_SCHEDULE", PropertySimpleType.STRING, true, true,
        false),

    /**
     * Retention strategy for creating regular snapshots
     * 
     * The STORAGE_* settings are all read-only and private and deal with shared, cluster-wide settings
     * among storage nodes. They are read-only and private because they should only be updated through
     * the storage subsystem. The username and password should not be updated at all.
     */
    STORAGE_REGULAR_SNAPSHOTS_RETENTION("STORAGE_REGULAR_SNAPSHOTS_RETENTION", PropertySimpleType.STRING, true, true,
        false),

    /**
     *  Count parameter for retention strategy for creating regular snapshots
     * 
     * The STORAGE_* settings are all read-only and private and deal with shared, cluster-wide settings
     * among storage nodes. They are read-only and private because they should only be updated through
     * the storage subsystem. The username and password should not be updated at all.
     */
    STORAGE_REGULAR_SNAPSHOTS_RETENTION_COUNT("STORAGE_REGULAR_SNAPSHOTS_RETENTION_COUNT", PropertySimpleType.INTEGER,
        true, true, false),

    /**
     * Deletion strategy for creating regular snapshots
     * 
     * The STORAGE_* settings are all read-only and private and deal with shared, cluster-wide settings
     * among storage nodes. They are read-only and private because they should only be updated through
     * the storage subsystem. The username and password should not be updated at all.
     */
    STORAGE_REGULAR_SNAPSHOTS_DELETION("STORAGE_REGULAR_SNAPSHOTS_DELETION", PropertySimpleType.STRING, true, false,
        false),

    /**
     * Location parameter for deletion strategy  for creating regular snapshots
     * 
     * The STORAGE_* settings are all read-only and private and deal with shared, cluster-wide settings
     * among storage nodes. They are read-only and private because they should only be updated through
     * the storage subsystem. The username and password should not be updated at all.
     */
    STORAGE_REGULAR_SNAPSHOTS_DELETION_LOCATION("STORAGE_REGULAR_SNAPSHOTS_DELETION_LOCATION",
        PropertySimpleType.STRING, true, true, false),

    /** How long do we keep partition events */
    PARTITION_EVENT_PURGE_PERIOD("PARTITION_EVENT_PURGE", PropertySimpleType.LONG, false, true, true),

    /** How long do we keep resource config history */
    RESOURCE_CONFIG_HISTORY_PURGE_PERIOD("RESOURCE_CONFIG_HISTORY_PURGE", PropertySimpleType.LONG, false, true, true),

    /** Hidden setting where we keep configuration id of storage cluster state configuration **/
    STORAGE_CLUSTER_STATE_CONFIGURATION_ID("STORAGE_CLUSTER_STATE_CONFIGURATION_ID", PropertySimpleType.LONG, true,
        false, false);

    private final String internalName;
    private final PropertySimpleType type;
    private final boolean readOnly;
    private final boolean requiringValue;
    private final boolean isPublic;

    private SystemSetting(String name, PropertySimpleType type, boolean readOnly, boolean valueRequired,
        boolean isPublic) {
        this.internalName = name;
        this.type = type;
        this.readOnly = readOnly;
        this.requiringValue = valueRequired;
        this.isPublic = isPublic;
    }

    public String getInternalName() {
        return internalName;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public boolean isRequiringValue() {
        return requiringValue;
    }

    /**
     * Whether or not is this setting present in the publicly accessible. If a property is NOT public, its value
     * cannot be obtained remotely and cannot be updated through the SystemManager. Other mechanism must exist in the
     * RHQ server to update such settings.
     * @return true if public, otherwise false
     */
    public boolean isPublic() {
        return isPublic;
    }

    public boolean validateValue(String value) {
        if (value == null) {
            //null values are not allowed
            return false;
        }

        switch (type) {
        case STRING:
        case PASSWORD:
            return true;
        case BOOLEAN:
            return isBoolean(value);
        case LONG:
            return isLong(value);
        case INTEGER:
            return isInteger(value);
        case FLOAT:
            return isFloat(value);
        case DOUBLE:
            return isDouble(value);
        default:
            throw new IllegalStateException("A system property '" + internalName
                + "' doesn't know how to validate its value which should have type '" + type + "'.");
        }
    }

    public PropertySimpleType getType() {
        return type;
    }

    public PropertyDefinitionSimple createPropertyDefinition() {
        return new PropertyDefinitionSimple(internalName, null, requiringValue, type);
    }

    public static SystemSetting getByInternalName(String internalName) {
        for (SystemSetting p : SystemSetting.values()) {
            if (p.internalName.equals(internalName)) {
                return p;
            }
        }

        return null;
    }

    private static boolean isLong(String value) {
        if (value == null) {
            return true;
        }

        try {
            Long.parseLong(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isInteger(String value) {
        if (value == null) {
            return true;
        }
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isFloat(String value) {
        if (value == null) {
            return true;
        }
        try {
            Float.parseFloat(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isDouble(String value) {
        if (value == null) {
            return true;
        }
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isBoolean(String value) {
        //be more strict about the values than Boolean.valueOf or Boolean.parseBoolean
        return value == null || Boolean.toString(true).equalsIgnoreCase(value)
            || Boolean.toString(false).equalsIgnoreCase(value);
    }
}
