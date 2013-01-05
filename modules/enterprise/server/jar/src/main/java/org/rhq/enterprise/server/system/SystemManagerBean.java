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

import javax.ejb.EJB;
import javax.ejb.Stateless;
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
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.domain.server.PersistenceUtility;
import org.rhq.core.util.StopWatch;
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

@Stateless
public class SystemManagerBean implements SystemManagerLocal, SystemManagerRemote {
    private final String SQL_VACUUM = "VACUUM ANALYZE {0}";

    private final String SQL_ANALYZE = "ANALYZE";

    private final String SQL_REINDEX = "REINDEX TABLE {0}";

    private final String SQL_REBUILD = "ALTER INDEX {0} REBUILD UNRECOVERABLE";

    private final String[] TABLES_TO_VACUUM = { "RHQ_RESOURCE", "RHQ_CONFIG", "RHQ_CONFIG_PROPERTY", "RHQ_AGENT" };

    private final String[] TABLES_TO_REINDEX = { "RHQ_MEASUREMENT_DATA_NUM_1D", "RHQ_MEASUREMENT_DATA_NUM_6H",
        "RHQ_MEASUREMENT_DATA_NUM_1H", "RHQ_MEASUREMENT_DATA_TRAIT", "RHQ_CALLTIME_DATA_KEY",
        "RHQ_CALLTIME_DATA_VALUE", "RHQ_AVAILABILITY" };

    private final String[] ORA_INDEXES_TO_REBUILD = { "RHQ_MEAS_DATA_1H_ID_TIME_PK", "RHQ_MEAS_DATA_6H_ID_TIME_PK",
        "RHQ_MEAS_DATA_1D_ID_TIME_PK", "RHQ_MEAS_BASELINE_CTIME_IDX", "RHQ_MEAS_DATA_TRAIT_ID_TIME_PK" };

    private Log log = LogFactory.getLog(SystemManagerBean.class);

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

    private static SystemSettings cachedSystemSettings = null;

    public void scheduleConfigCacheReloader() {
        // each time the webapp is reloaded, we don't want to create duplicate jobs
        Collection<Timer> timers = timerService.getTimers();
        for (Timer existingTimer : timers) {
            log.debug("Found timer - attempting to cancel: " + existingTimer.toString());
            try {
                existingTimer.cancel();
            } catch (Exception e) {
                log.warn("Failed in attempting to cancel timer: " + existingTimer.toString());
            }
        }

        // timer that will trigger every 60 seconds
        timerService.createIntervalTimer(60000L, 60000L, new TimerConfig(null, false));
    }

    @Timeout
    public void reloadConfigCache(Timer timer) {
        try {
            // note: I could have added a timestamp column, looked at it and see if its newer than our
            //       currently cached config and if so, load in the config then. But that's still
            //       1 roundtrip to the database, and maybe 2 (if the config is different). So we
            //       still have the same amount of roundtrips, and to make the code easier, without
            //       a need for the timestamp column/checking, we just load in the full config now.
            //       We never need 2 round trips, and this table is small enough that selecting the
            //       all its rows is really not going to effect performance much.
            systemManager.loadSystemConfigurationCacheInNewTx();
        } catch (Throwable t) {
            log.error("Failed to reload the system config cache - will try again later. Cause: " + t);
        }
    }

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
                    log.warn("Failed to close temporary connection", e);
                }
            }
        }
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    @Deprecated
    public Properties getSystemConfiguration(Subject subject) {
        Properties copy = new Properties();

        SystemSettings settings = getSystemSettings(subject);
        for (Map.Entry<SystemSetting, String> e : settings.entrySet()) {
            //transform the value back to the database format, because that's
            //what this method always returned
            String value = transformSystemConfigurationProperty(e.getKey(), e.getValue(), false);

            copy.put(e.getKey().getInternalName(), value);
        }

        return copy;
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public SystemSettings getSystemSettings(Subject subject) {
        if (cachedSystemSettings == null) {
            loadSystemConfigurationCache();
        }

        return new SystemSettings(cachedSystemSettings);
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public void setSystemSettings(Subject subject, SystemSettings settings) {
        setSystemSettings(settings, false);
    }

    private void setSystemSettings(SystemSettings settings, boolean skipValidation) {
        // first, we need to get the current settings so we'll know if we need to persist or merge the new ones
        @SuppressWarnings("unchecked")
        List<SystemConfiguration> configs = entityManager.createNamedQuery(SystemConfiguration.QUERY_FIND_ALL)
            .getResultList();

        Map<String, SystemConfiguration> existingConfigMap = new HashMap<String, SystemConfiguration>();

        for (SystemConfiguration config : configs) {
            existingConfigMap.put(config.getPropertyKey(), config);
        }

        // verify each new setting and persist them to the database
        // note that if a new setting is the same as the old one, we do nothing - leave the old entity as is
        for (Map.Entry<SystemSetting, String> e : settings.entrySet()) {
            SystemSetting prop = e.getKey();
            String value = e.getValue();

            if (skipValidation == false) {
                verifyNewSystemConfigurationProperty(prop, value, settings);
            }

            SystemConfiguration existingConfig = existingConfigMap.get(prop.getInternalName());
            if (existingConfig == null) {
                value = transformSystemConfigurationProperty(prop, value, false);
                existingConfig = new SystemConfiguration(prop.getInternalName(), value);
                entityManager.persist(existingConfig);
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
                String existingValue = transformSystemConfigurationProperty(prop, existingConfig.getPropertyValue(),
                    true);

                if ((existingValue == null && value != null) || !existingValue.equals(value)) {
                    //SystemSetting#isReadOnly should be a superset of the "fReadOnly" field in the database
                    //but let's just be super paranoid here...
                    if (prop.isReadOnly()
                        || (existingConfig.getFreadOnly() != null && existingConfig.getFreadOnly().booleanValue())) {
                        throw new IllegalArgumentException("The setting [" + prop.getInternalName()
                            + "] is read-only - you cannot change its current value! Current value is ["
                            + existingConfig.getPropertyValue() + "] while the new value was [" + value + "].");
                    }

                    //transform to the database-specific format
                    value = transformSystemConfigurationProperty(prop, value, false);

                    existingConfig.setPropertyValue(value);
                    entityManager.merge(existingConfig);
                }
            }
        }

        //let the cache be reloaded once it's needed.
        //we can't assume that the caller provided the full set of properties we
        //have in the database, so let's just make sure we reinit the cache
        //from there...
        cachedSystemSettings = null;
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
                String value = transformSystemConfigurationProperty(prop, e.getValue(), true);
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
            log.warn(MasterServerPluginContainer.class.getSimpleName() + " is not started yet");
            return null;
        }

        DriftServerPluginContainer pc = masterPC.getPluginContainerByClass(DriftServerPluginContainer.class);
        if (pc == null) {
            log.warn(DriftServerPluginContainer.class + " has not been loaded by the " + masterPC.getClass() + " yet");
            return null;
        }

        return (DriftServerPluginManager) pc.getPluginManager();
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void loadSystemConfigurationCacheInNewTx() {
        // this is used by our timer, so any exceptions coming out here doesn't throw away our timer
        loadSystemConfigurationCache();
    }

    @SuppressWarnings("unchecked")
    public void loadSystemConfigurationCache() {
        // After this is done, the cachedSystemSettings contains the latest config.
        List<SystemConfiguration> configs = entityManager.createNamedQuery(SystemConfiguration.QUERY_FIND_ALL)
            .getResultList();

        SystemSettings settings = new SystemSettings();

        for (SystemConfiguration config : configs) {
            SystemSetting prop = SystemSetting.getByInternalName(config.getPropertyKey());
            if (prop == null) {
                log.warn("The database contains unknown system configuration setting [" + config.getPropertyKey()
                    + "].");
                continue;
            }

            if (config.getPropertyValue() == null) {
                // for some reason, the configuration is not found in the DB, so fallback to the persisted default.
                // if there isn't even a persisted default, just use an empty string.
                String defaultValue = config.getDefaultPropertyValue();
                defaultValue = transformSystemConfigurationProperty(prop, defaultValue, true);
                settings.put(prop, (defaultValue != null) ? defaultValue : "");
            } else {
                String value = config.getPropertyValue();
                value = transformSystemConfigurationProperty(prop, value, true);
                settings.put(prop, value);
            }
        }

        settings.setDriftPlugins(getDriftServerPlugins());

        cachedSystemSettings = settings;
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    @Deprecated
    public void setSystemConfiguration(Subject subject, Properties properties, boolean skipValidation) throws Exception {
        Map<String, String> map = toMap(properties);

        transformToSystemSettingsFormat(map);

        SystemSettings settings = SystemSettings.fromMap(map);

        setSystemSettings(settings, skipValidation);
    }

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
     */
    private String transformSystemConfigurationProperty(SystemSetting prop, String value, boolean fromDb) {
        if (fromDb) {
            // to support Oracle (whose booleans may be 1 or 0) transform the boolean settings properly
            switch (prop) {
            case LDAP_BASED_JAAS_PROVIDER:
                if (RHQConstants.JDBCJAASProvider.equals(value)) {
                    return Boolean.toString(false);
                } else if (RHQConstants.LDAPJAASProvider.equals(value)) {
                    return Boolean.toString(true);
                }
                break;
            case USE_SSL_FOR_LDAP:
                if (RHQConstants.LDAP_PROTOCOL_SECURED.equals(value)) {
                    return Boolean.toString(true);
                } else {
                    return Boolean.toString(false);
                }
            default:
                if (prop.getType() == PropertySimpleType.BOOLEAN) {
                    if ("0".equals(value)) {
                        return Boolean.FALSE.toString();
                    } else if ("1".equals(value)) {
                        return Boolean.TRUE.toString();
                    }
                }
            }
        } else {
            //toDB
            //the 0,1 -> true false scenario is no problem here, because the values are stored
            //as string anyway (so no conversion is done). I assume the above is a historical
            //code that could be safely eliminated.
            switch (prop) {
            case LDAP_BASED_JAAS_PROVIDER:
                if (Boolean.parseBoolean(value)) {
                    return RHQConstants.LDAPJAASProvider;
                } else {
                    return RHQConstants.JDBCJAASProvider;
                }
            case USE_SSL_FOR_LDAP:
                if (Boolean.parseBoolean(value)) {
                    return RHQConstants.LDAP_PROTOCOL_SECURED;
                } else {
                    return RHQConstants.LDAP_PROTOCOL_UNSECURED;
                }
            }
        }

        return value;
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

    public void enableHibernateStatistics() {
        PersistenceUtility.enableHibernateStatistics(this.entityManager, ManagementFactory.getPlatformMBeanServer());
    }

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
            log.error("Error analyzing database", e);
            throw new RuntimeException("Error analyzing database", e);
        } finally {
            if (dbtype != null) {
                dbtype.closeConnection(conn);
            }
        }
    }

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
            log.error("Error reindexing database", e);
            throw new RuntimeException("Error reindexing database", e);
        } finally {
            if (dbtype != null) {
                dbtype.closeConnection(conn);
            }
        }
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public long vacuum(Subject whoami) {
        return vacuum(whoami, null);
    }

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
            log.error("Error vacuuming database: " + e.getMessage(), e);
            return duration;
        } finally {
            if (dbtype != null) {
                dbtype.closeConnection(conn);
            }
        }
    }

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

        if (log.isDebugEnabled()) {
            log.debug("Execute command: " + command);
        }

        try {
            stmt = conn.createStatement();
            stmt.execute(command);
            return watch.getElapsed();
        } catch (SQLException e) {
            log.error("Error in command: " + command + ": " + e, e);
            return watch.getElapsed();
        } finally {
            dbtype.closeStatement(stmt);
        }
    }

    /**
     * Ensures the installer is no longer deployed.
     * @deprecated
     */
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

    public boolean isDebugModeEnabled() {
        try {
            Subject su = this.subjectManager.getOverlord();
            String setting = getSystemSettings(su).get(SystemSetting.DEBUG_MODE_ENABLED);
            if (setting == null) {
                setting = "false";
            }
            return Boolean.valueOf(setting);
        } catch (Throwable t) {
            return false; // paranoid catch-all
        }
    }

    public boolean isExperimentalFeaturesEnabled() {
        try {
            Subject su = this.subjectManager.getOverlord();
            String setting = getSystemSettings(su).get(SystemSetting.EXPERIMENTAL_FEATURES_ENABLED);
            if (setting == null) {
                setting = "false";
            }
            return Boolean.valueOf(setting);
        } catch (Throwable t) {
            return false; // paranoid catch-all
        }
    }

    public boolean isLdapAuthorizationEnabled() {
        Subject su = this.subjectManager.getOverlord();
        SystemSettings settings = getSystemSettings(su);

        String ldapAuthValue = settings.get(SystemSetting.LDAP_BASED_JAAS_PROVIDER);

        boolean ldapAuth = ldapAuthValue == null ? false : Boolean.valueOf(ldapAuthValue);

        String groupFilter = settings.get(SystemSetting.LDAP_GROUP_FILTER);
        String groupMember = settings.get(SystemSetting.LDAP_GROUP_MEMBER);

        return ldapAuth
            && (((groupFilter != null) && groupFilter.trim().length() > 0) || ((groupMember != null) && groupMember
                .trim().length() > 0));
    }

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

    public ProductInfo getProductInfo(Subject subject) {
        CoreServerMBean coreServer = LookupUtil.getCoreServer();
        ProductInfo productInfo = coreServer.getProductInfo();
        return productInfo;
    }

    @Override
    public void dumpSystemInfo(Subject subject) {
        systemInfoManager.dumpToLog(subject);
    }
}