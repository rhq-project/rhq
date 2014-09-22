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
package org.rhq.enterprise.server.system;

import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.management.MBeanServerInvocationHandler;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.db.DatabaseType;
import org.rhq.core.db.DatabaseTypeFactory;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.common.ProductInfo;
import org.rhq.core.domain.common.ServerDetails;
import org.rhq.core.domain.common.ServerDetails.Detail;
import org.rhq.core.domain.common.SystemConfiguration;
import org.rhq.core.domain.common.composite.SystemSetting;
import org.rhq.core.domain.common.composite.SystemSettings;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.domain.server.PersistenceUtility;
import org.rhq.core.util.StopWatch;
import org.rhq.core.util.obfuscation.PicketBoxObfuscator;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.cloud.instance.ServerManagerLocal;
import org.rhq.enterprise.server.core.CoreServerMBean;
import org.rhq.enterprise.server.core.CustomJaasDeploymentServiceMBean;
import org.rhq.enterprise.server.plugin.pc.MasterServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.ServerPluginEnvironment;
import org.rhq.enterprise.server.plugin.pc.drift.DriftServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.drift.DriftServerPluginManager;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.util.SystemDatabaseInformation;

@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class SystemManagerBean implements SystemManagerLocal, SystemManagerRemote {
    private final String SQL_VACUUM = "VACUUM ANALYZE {0}";

    private final String SQL_ANALYZE = "ANALYZE";

    private final String SQL_REINDEX = "REINDEX TABLE {0}";

    private final String SQL_REBUILD = "ALTER INDEX {0} REBUILD";

    private final String[] TABLES_TO_VACUUM = { "RHQ_RESOURCE", "RHQ_CONFIG", "RHQ_CONFIG_PROPERTY", "RHQ_AGENT" };

    private final String[] TABLES_TO_REINDEX = { "RHQ_MEASUREMENT_DATA_TRAIT", "RHQ_CALLTIME_DATA_KEY",
        "RHQ_CALLTIME_DATA_VALUE", "RHQ_AVAILABILITY" };

    private final String[] ORA_INDEXES_TO_REBUILD = { "RHQ_MEAS_BASELINE_CTIME_IDX", "RHQ_MEAS_DATA_TRAIT_ID_TIME_PK" };

    private static final Log LOG = LogFactory.getLog(SystemManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @javax.annotation.Resource(name = "RHQ_DS", mappedName = RHQConstants.DATASOURCE_JNDI_NAME)
    private DataSource dataSource;

    @javax.annotation.Resource
    private TimerService timerService;

    @EJB
    private SystemManagerLocal systemManager;

    @EJB
    private ServerManagerLocal serverManager;

    @EJB
    //@IgnoreDependency
    private SubjectManagerLocal subjectManager;

    @EJB
    //@IgnoreDependency
    private SystemInfoManagerLocal systemInfoManager;

    private SystemSettings cachedSystemSettings = null;
    private SystemSettings cachedObfuscatedSystemSettings = null;

    @Override
    public void scheduleConfigCacheReloader() {
        // each time the webapp is reloaded, we don't want to create duplicate jobs
        Collection<Timer> timers = timerService.getTimers();
        for (Timer existingTimer : timers) {
            LOG.debug("Found timer - attempting to cancel: " + existingTimer.toString());
            try {
                existingTimer.cancel();
            } catch (Exception e) {
                LOG.warn("Failed in attempting to cancel timer: " + existingTimer.toString());
            }
        }

        // timer that will trigger every 60 seconds
        timerService.createIntervalTimer(60000L, 60000L, new TimerConfig(null, false));
    }

    @Timeout
    public void reloadConfigCache(Timer timer) {
        try {
            // reload the cache because it's fast and we'd need to make a db round-trip just to check if it's
            // stale.  if the cache was stale then after the reload also perform any system reconfiguration
            // that may be necessary given changes.
            String oldLastUpdate = getCachedSettings().get(SystemSetting.LAST_SYSTEM_CONFIG_UPDATE_TIME);

            systemManager.loadSystemConfigurationCacheInNewTx();

            String newLastUpdate = getCachedSettings().get(SystemSetting.LAST_SYSTEM_CONFIG_UPDATE_TIME);

            if (!safeEquals(oldLastUpdate, newLastUpdate)) {
                systemManager.reconfigureSystem(subjectManager.getOverlord());
            }
        } catch (Throwable t) {
            LOG.error("Failed to reload the system config cache - will try again later. Cause: " + t);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public DatabaseType getDatabaseType() {
        Connection conn = null;
        DatabaseType dbtype = null;
        try {
            conn = dataSource.getConnection();
            dbtype = DatabaseTypeFactory.getDatabaseType(conn);
            return dbtype;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    LOG.warn("Failed to close temporary connection", e);
                }
            }
        }
    }

    @Override
    @Deprecated
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public Properties getSystemConfiguration(Subject subject) {
        Properties copy = new Properties();

        SystemSettings settings = getUnmaskedSystemSettings(true);
        for (Map.Entry<SystemSetting, String> e : settings.entrySet()) {
            //transform the value back to the database format, because that's
            //what this method always returned.
            //Leave the password fields as they are though, because now (as of 4.10)
            //the passwords are stored obfuscated, but are kept clear in memory.
            //The legacy behavior was to store the values in clear text, too,
            //so the expected output of this method is to have passwords in clear.

            if (e.getKey().isPublic()) {
                String value = e.getValue();
                if (e.getKey().getType() != PropertySimpleType.PASSWORD) {
                    value = transformSystemConfigurationPropertyToDb(e.getKey(), e.getValue(), e.getValue());
                }

                copy.put(e.getKey().getInternalName(), value);
            }
        }

        return copy;
    }

    @Override
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public SystemSettings getSystemSettings(Subject subject) {
        SystemSettings ret = new SystemSettings();
        SystemSettings unmasked = getUnmaskedSystemSettings(true);

        for (Map.Entry<SystemSetting, String> entry : unmasked.entrySet()) {
            if (entry.getKey().isPublic()) {
                if (entry.getKey().getType() == PropertySimpleType.PASSWORD) {
                    entry.setValue(PropertySimple.MASKED_VALUE);
                }

                ret.put(entry.getKey(), entry.getValue());
            }
        }

        return ret;
    }

    @Override
    public SystemSettings getUnmaskedSystemSettings(boolean includePrivateSettings) {
        SystemSettings ret = getCachedSettings();

        return includePrivateSettings ? ret : removePrivateSettings(ret);
    }

    @Override
    public void deobfuscate(SystemSettings systemSettings) {
        for (Map.Entry<SystemSetting, String> entry : systemSettings.entrySet()) {
            String value = entry.getValue();
            if (value != null && entry.getKey().getType() == PropertySimpleType.PASSWORD) {
                entry.setValue(PicketBoxObfuscator.decode(value));
            }
        }
    }

    @Override
    public SystemSettings getObfuscatedSystemSettings(boolean includePrivateSettings) {
        SystemSettings ret = getCachedObfuscatedSettings();

        return includePrivateSettings ? ret : removePrivateSettings(ret);
    }

    @Override
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public void setSystemSettings(Subject subject, SystemSettings settings) {
        setAnySystemSettings(removePrivateSettings(settings), false, false);
    }

    @Override
    public void setAnySystemSetting(SystemSetting setting, String value) {
        if (SystemSetting.LAST_SYSTEM_CONFIG_UPDATE_TIME == setting) {
            return;
        }

        SystemSettings settings = getUnmaskedSystemSettings(true);
        settings.put(setting, value);
        setAnySystemSettings(settings, true, true);
    }

    private SystemSettings removePrivateSettings(SystemSettings settings) {
        SystemSettings cleansed = new SystemSettings(settings);
        for (SystemSetting s : SystemSetting.values()) {
            if (!s.isPublic()) {
                cleansed.remove(s);
            }
        }
        return cleansed;
    }

    @Override
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public void setStorageClusterSettings(Subject subject, SystemSettings settings) {
        for (SystemSetting setting : settings.keySet()) {
            if (!isStorageSetting(setting)) {
                throw new IllegalArgumentException(setting + " cannot be updated through this method. This method "
                    + "only allows updating of storage cluster settings.");
            }
        }
        setAnySystemSettings(settings, false, true);
    }

    @Override
    public void setAnySystemSettings(SystemSettings settings, boolean skipValidation, boolean ignoreReadOnly) {
        // first, we need to get the current settings so we'll know if we need to persist or merge the new ones
        @SuppressWarnings("unchecked")
        List<SystemConfiguration> configs = entityManager.createNamedQuery(SystemConfiguration.QUERY_FIND_ALL)
            .getResultList();

        Map<String, SystemConfiguration> existingConfigMap = new HashMap<String, SystemConfiguration>();

        for (SystemConfiguration config : configs) {
            existingConfigMap.put(config.getPropertyKey(), config);
        }

        boolean changed = false;

        SystemConfiguration lastUpdateTime = existingConfigMap.get(SystemSetting.LAST_SYSTEM_CONFIG_UPDATE_TIME
            .getInternalName());

        // verify each new setting and persist them to the database
        // note that if a new setting is the same as the old one, we do nothing - leave the old entity as is
        for (Map.Entry<SystemSetting, String> e : settings.entrySet()) {
            SystemSetting prop = e.getKey();

            String value = e.getValue();

            if (!skipValidation) {
                verifyNewSystemConfigurationProperty(prop, value, settings);
            }

            SystemConfiguration existingConfig = existingConfigMap.get(prop.getInternalName());
            if (e.getKey() == SystemSetting.LAST_SYSTEM_CONFIG_UPDATE_TIME) {
                //we don't let the user persist their own last system config update time
                //in any manner
                lastUpdateTime = existingConfig;
            } else if (existingConfig == null) {
                value = transformSystemConfigurationPropertyToDb(prop, value, null);
                existingConfig = new SystemConfiguration(prop.getInternalName(), value);
                entityManager.persist(existingConfig);
                changed = true;
                existingConfigMap.put(existingConfig.getPropertyKey(), existingConfig);
            } else {
                //make sure we compare the new value with a database-agnostic value
                //it is important to compare in the database-agnostic format instead
                //of database specific because the conversion isn't reflective.
                //Some legacy code somewhere is (or just used to) store booleans as "0"s and "1"s
                //even though they are stored as strings and thus don't suffer from Oracle's
                //lack of support for boolean data type. If we encounter such values, we convert
                //them to "false"/"true" and store that value to database. This is a one way operation
                //and therefore we need to compare the database-agnostic (i.e. "true"/"false") and
                //not database specific.
                //
                //More importantly though, we store the password fields obfuscated, so we need to compare apples with
                //apples here.
                String existingValue = transformSystemConfigurationPropertyFromDb(prop,
                    existingConfig.getPropertyValue(), true);
                //we need to unmask the new value so that we can compare for changes. only after that can we transform
                //it into the DB format.
                value = unmask(prop, value, existingValue);

                //also for oracle, treat null and empty string as the same.
                if ((isEmpty(existingValue) && !isEmpty(value))
                    || (null != existingValue && !existingValue.equals(value))) {
                    //SystemSetting#isReadOnly should be a superset of the "fReadOnly" field in the database
                    //but let's just be super paranoid here...
                    if ((prop.isReadOnly() || (existingConfig.getFreadOnly() != null && existingConfig.getFreadOnly()
                        .booleanValue())) && !(isStorageSetting(prop) || ignoreReadOnly)) {
                        throw new IllegalArgumentException("The setting [" + prop.getInternalName()
                            + "] is read-only - you cannot change its current value! Current value is ["
                            + existingConfig.getPropertyValue() + "] while the new value was [" + value + "].");
                    }

                    //transform to the database-specific format
                    value = transformSystemConfigurationPropertyToDb(prop, value, existingValue);

                    existingConfig.setPropertyValue(value);
                    entityManager.merge(existingConfig);
                    changed = true;
                }
            }
        }

        if (changed) {
            if (lastUpdateTime == null) {
                lastUpdateTime = new SystemConfiguration(
                    SystemSetting.LAST_SYSTEM_CONFIG_UPDATE_TIME.getInternalName(), Long.toString(System
                        .currentTimeMillis()));
                lastUpdateTime.setFreadOnly(SystemSetting.LAST_SYSTEM_CONFIG_UPDATE_TIME.isReadOnly());
                entityManager.persist(lastUpdateTime);
            } else {
                lastUpdateTime.setPropertyValue(Long.toString(System.currentTimeMillis()));

                entityManager.merge(lastUpdateTime);
            }

            existingConfigMap.put(SystemSetting.LAST_SYSTEM_CONFIG_UPDATE_TIME.getInternalName(), lastUpdateTime);

            fillCache(existingConfigMap.values());
        }
    }

    private static boolean isEmpty(String string) {
        return null == string || string.trim().isEmpty();
    }

    private boolean isStorageSetting(SystemSetting setting) {
        switch (setting) {
        case STORAGE_CQL_PORT:
        case STORAGE_GOSSIP_PORT:
        case STORAGE_AUTOMATIC_DEPLOYMENT:
        case STORAGE_PASSWORD:
        case STORAGE_USERNAME:
        case STORAGE_REGULAR_SNAPSHOTS:
        case STORAGE_REGULAR_SNAPSHOTS_SCHEDULE:
        case STORAGE_REGULAR_SNAPSHOTS_RETENTION:
        case STORAGE_REGULAR_SNAPSHOTS_RETENTION_COUNT:
        case STORAGE_REGULAR_SNAPSHOTS_DELETION:
        case STORAGE_REGULAR_SNAPSHOTS_DELETION_LOCATION:
            return true;
        default:
            return false;
        }
    }

    private Map<String, String> toMap(Properties props) {
        HashMap<String, String> map = new HashMap<String, String>(props.size());
        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            map.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
        }
        return map;
    }

    private void transformToSystemSettingsFormat(Map<String, String> map) {
        for (Map.Entry<String, String> e : map.entrySet()) {
            SystemSetting prop = SystemSetting.getByInternalName(e.getKey());
            if (prop != null) {
                //this is a legacy method that supplies values in the DB-specific format.
                //we therefore have to transform the values as if they came from the database.
                String value = transformSystemConfigurationPropertyFromDb(prop, e.getValue(), false);
                e.setValue(value);
            }
        }
    }

    private Map<String, String> getDriftServerPlugins() {
        DriftServerPluginManager pluginMgr = getDriftServerPluginManager();
        Map<String, String> plugins = new HashMap<String, String>();

        if (pluginMgr != null) {
            for (ServerPluginEnvironment env : pluginMgr.getPluginEnvironments()) {
                plugins.put(env.getPluginKey().getPluginName(), env.getPluginDescriptor().getDisplayName());
            }
        }

        return plugins;
    }

    private DriftServerPluginManager getDriftServerPluginManager() {
        MasterServerPluginContainer masterPC = LookupUtil.getServerPluginService().getMasterPluginContainer();
        if (masterPC == null) {
            LOG.warn(MasterServerPluginContainer.class.getSimpleName() + " is not started yet");
            return null;
        }

        DriftServerPluginContainer pc = masterPC.getPluginContainerByClass(DriftServerPluginContainer.class);
        if (pc == null) {
            LOG.warn(DriftServerPluginContainer.class + " has not been loaded by the " + masterPC.getClass() + " yet");
            return null;
        }

        return (DriftServerPluginManager) pc.getPluginManager();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void loadSystemConfigurationCacheInNewTx() {
        // this is used by our timer, so any exceptions coming out here doesn't throw away our timer
        loadSystemConfigurationCache();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void loadSystemConfigurationCache() {
        // After this is done, the cachedSystemSettings contains the latest config.
        List<SystemConfiguration> configs = entityManager.createNamedQuery(SystemConfiguration.QUERY_FIND_ALL)
            .getResultList();

        fillCache(configs);
    }

    @Override
    @Deprecated
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public void setSystemConfiguration(Subject subject, Properties properties, boolean skipValidation) throws Exception {
        Map<String, String> map = toMap(properties);

        transformToSystemSettingsFormat(map);

        SystemSettings settings = SystemSettings.fromMap(map);

        setAnySystemSettings(settings, skipValidation, false);
    }

    @Override
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public void validateSystemConfiguration(Subject subject, Properties properties)
        throws InvalidSystemConfigurationException {
        Map<String, String> map = toMap(properties);

        transformToSystemSettingsFormat(map);

        SystemSettings settings = SystemSettings.fromMap(map);
        for (Map.Entry<SystemSetting, String> e : settings.entrySet()) {
            SystemSetting prop = e.getKey();
            String value = e.getValue();

            verifyNewSystemConfigurationProperty(prop, value, settings);
        }
    }

    /**
     * Call this to transform a system setting to a more appropriate value.
     * Importantly, this (de)obfuscates the password fields as they go from and to the DB. We use the
     * @{link PicketBoxObfuscator} so that people are able encode their passwords in the system settings export files
     * using the "rhq-encode-value.{sh|bat}" script.
     */
    private String transformSystemConfigurationPropertyFromDb(SystemSetting prop, String value, boolean unobfuscate) {
        // to support Oracle (whose booleans may be 1 or 0) transform the boolean settings properly
        switch (prop) {
        case LDAP_BASED_JAAS_PROVIDER:
            if (RHQConstants.JDBCJAASProvider.equals(value)) {
                return Boolean.toString(false);
            } else if (RHQConstants.LDAPJAASProvider.equals(value)) {
                return Boolean.toString(true);
            } else {
                return value == null ? "" : value;
            }
        case USE_SSL_FOR_LDAP:
            if (RHQConstants.LDAP_PROTOCOL_SECURED.equals(value)) {
                return Boolean.toString(true);
            } else {
                return Boolean.toString(false);
            }
        default:
            switch (prop.getType()) {
            case BOOLEAN:
                if ("0".equals(value)) {
                    return Boolean.FALSE.toString();
                } else if ("1".equals(value)) {
                    return Boolean.TRUE.toString();
                } else {
                    return value == null ? Boolean.FALSE.toString() : value;
                }
            case PASSWORD:
                if (unobfuscate && value != null && value.trim().length() > 0) {
                    return PicketBoxObfuscator.decode(value);
                } else {
                    return value == null ? "" : value;
                }
            default:
                if (value == null) {
                    switch (prop.getType()) {
                    case DOUBLE:
                    case FLOAT:
                    case INTEGER:
                    case LONG:
                        value = "0";
                        break;
                    default:
                        value = "";
                    }
                }
                return value;
            }
        }
    }

    /**
     * Call this to transform a system setting to a more appropriate value.
     * Importantly, this (de)obfuscates the password fields as they go from and to the DB. We use the
     * @{link PicketBoxObfuscator} so that people are able encode their passwords in the system settings export files
     * using the "rhq-encode-value.{sh|bat}" script.
     */
    private String transformSystemConfigurationPropertyToDb(SystemSetting prop, String newValue, String oldValue) {
        //the 0,1 -> true false scenario is no problem here, because the values are stored
        //as string anyway (so no conversion is done). I assume the above is a historical
        //code that could be safely eliminated.
        switch (prop) {
        case LDAP_BASED_JAAS_PROVIDER:
            if (Boolean.parseBoolean(newValue)) {
                return RHQConstants.LDAPJAASProvider;
            } else {
                return RHQConstants.JDBCJAASProvider;
            }
        case USE_SSL_FOR_LDAP:
            if (Boolean.parseBoolean(newValue)) {
                return RHQConstants.LDAP_PROTOCOL_SECURED;
            } else {
                return RHQConstants.LDAP_PROTOCOL_UNSECURED;
            }
        default:
            if (prop.getType() == PropertySimpleType.PASSWORD && newValue != null) {
                if (PropertySimple.MASKED_VALUE.equals(newValue)) {
                    return oldValue;
                } else {
                    return PicketBoxObfuscator.encode(newValue);
                }
            } else {
                return newValue;
            }
        }
    }

    private String unmask(SystemSetting prop, String newValue, String currentValue) {
        if (prop.getType() == PropertySimpleType.PASSWORD && PropertySimple.MASKED_VALUE.equals(newValue)) {
            newValue = currentValue;
        }

        return newValue;
    }

    /**
     * This will make sure the given configuration has a valid value. An exception will be thrown if the given system
     * configuration value is invalid. This method returns if everything is OK.
     *
     * @param property   name for the property
     * @param value      value of the property
     * @param settings   the full set of system configurations, in case the changed value needs to be compared against
     *                   other values
     */
    private void verifyNewSystemConfigurationProperty(SystemSetting property, String value, SystemSettings settings) {
        if (property == SystemSetting.BASE_LINE_DATASET) {
            // 1h table holds at most 14 days worth of data, make sure we don't set a dataset more than that
            long baselineDataSet = Long.parseLong(value);
            if (baselineDataSet > (1000L * 60 * 60 * 24 * 14)) {
                throw new InvalidSystemConfigurationException("Baseline dataset must be less than 14 days");
            }
        } else if (property == SystemSetting.BASE_LINE_FREQUENCY) {
            long baselineFrequency = Long.parseLong(value);
            long baselineDataSet = Long.parseLong(settings.get(SystemSetting.BASE_LINE_DATASET));
            if (baselineFrequency > baselineDataSet) {
                throw new InvalidSystemConfigurationException(
                    "baseline computation frequency must not be larger than baseline data set");
            }
        } else if (property == SystemSetting.AGENT_MAX_QUIET_TIME_ALLOWED) {
            long time = Long.parseLong(value);
            // minimum should be 3 * the agent ping interval, any less risks unwanted backfilling
            if (time < 1000L * 60 * 3) {
                throw new InvalidSystemConfigurationException("Agent Max Quiet Time Allowed must be at least 3 minutes");
            }
        }

        return;
    }

    @Override
    public void enableHibernateStatistics() {
        PersistenceUtility.enableHibernateStatistics(this.entityManager, ManagementFactory.getPlatformMBeanServer());
    }

    @Override
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public void reconfigureSystem(Subject whoami) {
        try {
            Object mbean;

            mbean = MBeanServerInvocationHandler.newProxyInstance(ManagementFactory.getPlatformMBeanServer(),
                CustomJaasDeploymentServiceMBean.OBJECT_NAME, CustomJaasDeploymentServiceMBean.class, false);
            ((CustomJaasDeploymentServiceMBean) mbean).installJaasModules();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public long analyze(Subject whoami) {
        Connection conn = null;
        DatabaseType dbtype = null;
        try {
            conn = dataSource.getConnection();
            dbtype = DatabaseTypeFactory.getDatabaseType(conn);
            if (!DatabaseTypeFactory.isPostgres(dbtype)) {
                return -1;
            }

            long duration = doCommand(dbtype, conn, SQL_ANALYZE, null);
            return duration;
        } catch (Exception e) {
            LOG.error("Error analyzing database", e);
            throw new RuntimeException("Error analyzing database", e);
        } finally {
            if (dbtype != null) {
                dbtype.closeConnection(conn);
            }
        }
    }

    @Override
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public long reindex(Subject whoami) {
        Connection conn = null;
        DatabaseType dbtype = null;
        try {
            conn = dataSource.getConnection();
            dbtype = DatabaseTypeFactory.getDatabaseType(conn);
            long duration = 0;
            if (DatabaseTypeFactory.isPostgres(dbtype)) {
                for (String table : TABLES_TO_REINDEX) {
                    duration += doCommand(dbtype, conn, SQL_REINDEX, table);
                }
            } else if (DatabaseTypeFactory.isOracle(dbtype)) {
                for (String index : ORA_INDEXES_TO_REBUILD) {
                    duration += doCommand(dbtype, conn, SQL_REBUILD, index);
                }
            } else {
                return -1;
            }

            return duration;
        } catch (Exception e) {
            LOG.error("Error reindexing database", e);
            throw new RuntimeException("Error reindexing database", e);
        } finally {
            if (dbtype != null) {
                dbtype.closeConnection(conn);
            }
        }
    }

    @Override
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public long vacuum(Subject whoami) {
        return vacuum(whoami, null);
    }

    @Override
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public long vacuum(Subject whoami, String[] tableNames) {
        long duration = 0;
        Connection conn = null;
        DatabaseType dbtype = null;
        try {
            conn = dataSource.getConnection();
            dbtype = DatabaseTypeFactory.getDatabaseType(conn);
            if (!DatabaseTypeFactory.isPostgres(dbtype)) {
                return -1;
            }

            if (tableNames == null) // no names given -> operate on all tables.
            {
                tableNames = new String[1];
                tableNames[0] = null;
            }

            for (String tableName : tableNames) {
                duration += doCommand(dbtype, conn, SQL_VACUUM, tableName);
            }

            return duration;
        } catch (Exception e) {
            LOG.error("Error vacuuming database: " + e.getMessage(), e);
            return duration;
        } finally {
            if (dbtype != null) {
                dbtype.closeConnection(conn);
            }
        }
    }

    @Override
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public long vacuumAppdef(Subject whoami) {
        return vacuum(whoami, TABLES_TO_VACUUM);
    }

    private long doCommand(DatabaseType dbtype, Connection conn, String command, String table) {
        Statement stmt = null;
        StopWatch watch = new StopWatch();

        if (table == null) {
            table = "";
        }

        command = command.replace("{0}", table);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Execute command: " + command);
        }

        try {
            stmt = conn.createStatement();
            stmt.execute(command);
            return watch.getElapsed();
        } catch (SQLException e) {
            LOG.error("Error in command: " + command + ": " + e, e);
            return watch.getElapsed();
        } finally {
            dbtype.closeStatement(stmt);
        }
    }

    /**
     * Ensures the installer is no longer deployed.
     * @deprecated
     */
    @Override
    @Deprecated
    public void undeployInstaller() {
        // No need for this anymore - the new RHQ installer in AS7 detects it already installed things and will point the user to login screen
        // We leave this in here in case we want to re-introduce the ability to uninstall the installer.
        //        try {
        //            log.warn("!!!!!!!!TODO: UNDEPLOY THE INSTALLER HERE!!!!!");
        //        } catch (Exception e) {
        //            log.warn("Please manually remove installer war to secure your deployment: " + e);
        //            return;
        //        }
        //
        //        // we only get here if we are SURE we removed it!
        //        log.info("Confirmed that the installer has been undeployed");
        return;
    }

    @Override
    public boolean isDebugModeEnabled() {
        try {
            String setting = getUnmaskedSystemSettings(true).get(SystemSetting.DEBUG_MODE_ENABLED);
            if (setting == null) {
                setting = "false";
            }
            return Boolean.valueOf(setting);
        } catch (Throwable t) {
            return false; // paranoid catch-all
        }
    }

    @Override
    public boolean isLoginWithoutRolesEnabled() {
        try {
            String setting = getUnmaskedSystemSettings(true).get(SystemSetting.LOGIN_WITHOUT_ROLES_ENABLED);
            if (setting == null) {
                setting = "true";
            }
            return Boolean.valueOf(setting);
        } catch (Exception t) {
            return false; // paranoid catch-all
        }
    }

    @Override
    public boolean isExperimentalFeaturesEnabled() {
        try {
            String setting = getUnmaskedSystemSettings(true).get(SystemSetting.EXPERIMENTAL_FEATURES_ENABLED);
            if (setting == null) {
                setting = "false";
            }
            return Boolean.valueOf(setting);
        } catch (Throwable t) {
            return false; // paranoid catch-all
        }
    }

    @Override
    public boolean isLdapAuthorizationEnabled() {
        SystemSettings settings = getUnmaskedSystemSettings(true);

        String ldapAuthValue = settings.get(SystemSetting.LDAP_BASED_JAAS_PROVIDER);

        boolean ldapAuth = ldapAuthValue == null ? false : Boolean.valueOf(ldapAuthValue);

        String groupFilter = settings.get(SystemSetting.LDAP_GROUP_FILTER);
        String groupMember = settings.get(SystemSetting.LDAP_GROUP_MEMBER);

        return ldapAuth
            && (((groupFilter != null) && groupFilter.trim().length() > 0) || ((groupMember != null) && groupMember
                .trim().length() > 0));
    }

    @Override
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public ServerDetails getServerDetails(Subject subject) {
        CoreServerMBean coreServerMBean = LookupUtil.getCoreServer();

        ServerDetails serverDetails = new ServerDetails();

        serverDetails.setProductInfo(getProductInfo(subject));

        HashMap<Detail, String> details = serverDetails.getDetails();

        DateFormat localTimeFormatter = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.FULL);
        details.put(ServerDetails.Detail.SERVER_LOCAL_TIME, localTimeFormatter.format(new Date()));
        details.put(ServerDetails.Detail.SERVER_TIMEZONE, TimeZone.getDefault().getDisplayName());
        details.put(ServerDetails.Detail.SERVER_HOME_DIR, coreServerMBean.getJBossServerHomeDir().getAbsolutePath());
        details.put(ServerDetails.Detail.SERVER_INSTALL_DIR, coreServerMBean.getInstallDir().getAbsolutePath());

        SystemDatabaseInformation dbInfo = SystemDatabaseInformation.getInstance();
        details.put(ServerDetails.Detail.DATABASE_CONNECTION_URL, dbInfo.getDatabaseConnectionURL());
        details.put(ServerDetails.Detail.DATABASE_DRIVER_NAME, dbInfo.getDatabaseDriverName());
        details.put(ServerDetails.Detail.DATABASE_DRIVER_VERSION, dbInfo.getDatabaseDriverVersion());
        details.put(ServerDetails.Detail.DATABASE_PRODUCT_NAME, dbInfo.getDatabaseProductName());
        details.put(ServerDetails.Detail.DATABASE_PRODUCT_VERSION, dbInfo.getDatabaseProductVersion());

        details.put(ServerDetails.Detail.SERVER_IDENTITY, serverManager.getServer().getName());

        return serverDetails;
    }

    @Override
    public ProductInfo getProductInfo(Subject subject) {
        CoreServerMBean coreServer = LookupUtil.getCoreServer();
        ProductInfo productInfo = coreServer.getProductInfo();
        return productInfo;
    }

    @Override
    public void dumpSystemInfo(Subject subject) {
        systemInfoManager.dumpToLog(subject);
    }

    private synchronized SystemSettings getCachedSettings() {
        if (cachedSystemSettings == null) {
            loadSystemConfigurationCache();
        }

        return new SystemSettings(cachedSystemSettings);
    }

    private synchronized SystemSettings getCachedObfuscatedSettings() {
        if (cachedSystemSettings == null) {
            loadSystemConfigurationCache();
        }

        return new SystemSettings(cachedObfuscatedSystemSettings);
    }

    private void fillCache(Collection<SystemConfiguration> configs) {
        SystemSettings settings = new SystemSettings();

        for (SystemConfiguration config : configs) {
            SystemSetting prop = SystemSetting.getByInternalName(config.getPropertyKey());
            if (prop == null) {
                LOG.warn("The database contains unknown system configuration setting [" + config.getPropertyKey()
                    + "].");
                continue;
            }

            if (config.getPropertyValue() == null) {
                // for some reason, the configuration is not found in the DB, so fallback to the persisted default.
                // if there isn't even a persisted default, just use an empty string.
                String defaultValue = config.getDefaultPropertyValue();
                defaultValue = transformSystemConfigurationPropertyFromDb(prop, defaultValue, true);
                settings.put(prop, defaultValue);
            } else {
                String value = config.getPropertyValue();
                value = transformSystemConfigurationPropertyFromDb(prop, value, true);
                settings.put(prop, value);
            }
        }

        settings.setDriftPlugins(getDriftServerPlugins());

        synchronized (this) {
            //only update the caches if the settings were actually changed
            if (cachedSystemSettings == null
                || !safeEquals(cachedSystemSettings.get(SystemSetting.LAST_SYSTEM_CONFIG_UPDATE_TIME),
                    settings.get(SystemSetting.LAST_SYSTEM_CONFIG_UPDATE_TIME))) {
                cachedSystemSettings = settings;

                cachedObfuscatedSystemSettings = new SystemSettings(settings);
                for (Map.Entry<SystemSetting, String> entry : cachedObfuscatedSystemSettings.entrySet()) {
                    String value = entry.getValue();
                    if (value != null && entry.getKey().getType() == PropertySimpleType.PASSWORD) {
                        entry.setValue(PicketBoxObfuscator.encode(value));
                    }
                }
            }
        }
    }

    private static boolean safeEquals(Object a, Object b) {
        return a == null ? b == null : (b != null && a.equals(b));
    }
}
