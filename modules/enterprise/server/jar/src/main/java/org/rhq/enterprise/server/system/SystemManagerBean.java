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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.mx.util.MBeanServerLocator;
import org.jboss.system.server.ServerConfig;

import org.rhq.core.db.DatabaseType;
import org.rhq.core.db.DatabaseTypeFactory;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.common.SystemConfiguration;
import org.rhq.core.domain.util.PersistenceUtility;
import org.rhq.core.util.ObjectNameFactory;
import org.rhq.core.util.StopWatch;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.core.CustomJaasDeploymentServiceMBean;
import org.rhq.enterprise.server.legacy.common.shared.HQConstants;
import org.rhq.enterprise.server.license.FeatureUnavailableException;
import org.rhq.enterprise.server.license.License;
import org.rhq.enterprise.server.license.LicenseManager;
import org.rhq.enterprise.server.license.LicenseStoreManager;

@Stateless
public class SystemManagerBean implements SystemManagerLocal {
    private final String SQL_VACUUM = "VACUUM ANALYZE {0}";

    private final String SQL_ANALYZE = "ANALYZE";

    private final String SQL_REINDEX = "REINDEX TABLE {0}";

    private final String SQL_REBUILD = "ALTER INDEX {0} REBUILD UNRECOVERABLE";

    private final String[] APPDEF_TABLES = { "RHQ_RESOURCE", "RHQ_CONFIG", "RHQ_CONFIG_PROPERTY", "RHQ_AGENT" };

    private final String[] DATA_TABLES = { "RHQ_MEASUREMENT_DATA_NUM_1D", "RHQ_MEASUREMENT_DATA_NUM_6H",
        "RHQ_MEASUREMENT_DATA_NUM_1H", "RHQ_MEASUREMENT_DATA_TRAIT", "RHQ_CALLTIME_DATA_KEY",
        "RHQ_CALLTIME_DATA_VALUE", "RHQ_AVAILABILITY" };

    private final String[] ORA_INDEXES = { "RHQ_MEAS_DATA_1H_ID_TIME_PK", "RHQ_MEAS_DATA_6H_ID_TIME_PK",
        "RHQ_MEAS_DATA_1D_ID_TIME_PK", "RHQ_MEAS_BASELINE_CTIME_IDX", "RHQ_MEAS_DATA_TRAIT_ID_TIME_PK" };

    protected Log log = LogFactory.getLog(SystemManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @javax.annotation.Resource(name = "RHQ_DS", mappedName = RHQConstants.DATASOURCE_JNDI_NAME)
    DataSource dataSource;

    LicenseManager licenseManager;

    @PostConstruct
    public void initialize() {
        licenseManager = LicenseManager.instance();
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

    @SuppressWarnings("unchecked")
    public Properties getSystemConfiguration() {
        Properties properties = new Properties();

        List<SystemConfiguration> configs = entityManager.createNamedQuery(SystemConfiguration.QUERY_FIND_ALL)
            .getResultList();
        for (SystemConfiguration config : configs) {
            if (config.getPropertyValue() == null) {
                properties.put(config.getPropertyKey(), (config.getDefaultPropertyValue() != null) ? config
                    .getDefaultPropertyValue() : "");
            } else {
                properties.put(config.getPropertyKey(), config.getPropertyValue());
            }
        }

        return properties;
    }

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public void setSystemConfiguration(Subject subject, Properties properties) {
        Map<String, SystemConfiguration> configMap = new HashMap<String, SystemConfiguration>();
        List<SystemConfiguration> configs = entityManager.createNamedQuery(SystemConfiguration.QUERY_FIND_ALL)
            .getResultList();

        for (SystemConfiguration config : configs) {
            configMap.put(config.getPropertyKey(), config);
        }

        for (Object key : properties.keySet()) {
            String name = (String) key;
            String value = properties.getProperty(name);

            verifyNewSystemConfigurationProperty(name, value, properties);

            SystemConfiguration existingConfig = configMap.get(name);
            if (existingConfig == null) {
                existingConfig = new SystemConfiguration(name, value);
                entityManager.persist(existingConfig);
            } else {
                if ((existingConfig.getPropertyValue() == null) || !existingConfig.getPropertyValue().equals(value)) {
                    existingConfig.setPropertyValue(value);
                    entityManager.merge(existingConfig);
                }
            }
        }
    }

    public Date getBootTime() {
        Date bootTime = null;
        Query q = entityManager.createNamedQuery(SystemConfiguration.FIND_PROPERTY_BY_KEY);
        q.setParameter("key", "LAST_BOOT_TIME");
        List<SystemConfiguration> configs = q.getResultList();
        if (configs.size() == 1) {
            SystemConfiguration dateConf = configs.get(0);
            String dateString = dateConf.getPropertyValue();
            try {
                bootTime = new SimpleDateFormat("yy-MM-dd hh:mm:ss").parse(dateString);
            } catch (ParseException e) {
                log.debug("Date was unparseable");
            }
        }
        if (bootTime == null)
            bootTime = new Date();

        return bootTime;
    }

    /**
     * This will make sure the given configuration has a valid value. An exception will be thrown if the given system
     * configuration value is invalid. This method returns if everything is OK.
     *
     * @param name       name for the property
     * @param value      value of the property
     * @param properties the full set of system configurations, in case the changed value needs to be compared against
     *                   other values
     */
    @SuppressWarnings("deprecation")
    private void verifyNewSystemConfigurationProperty(String name, String value, Properties properties) {
        if (HQConstants.BaselineDataSet.equals(name)) {
            // 1h table holds at most 14 days worth of data, make sure we don't set a dataset more than that
            long freq = Long.parseLong(value);
            if (freq > (1000L * 60 * 60 * 24 * 14)) {
                throw new IllegalArgumentException("Baseline dataset must be less than 14 days");
            }
        }

        return;
    }

    public void enableHibernateStatistics() {
        PersistenceUtility.enableHibernateStatistics(this.entityManager, ManagementFactory.getPlatformMBeanServer());
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
            log.error("Error creating database connection", e);
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
                for (int i = 0; i < DATA_TABLES.length; i++) {
                    duration += doCommand(dbtype, conn, SQL_REINDEX, DATA_TABLES[i]);
                }
            } else if (DatabaseTypeFactory.isOracle(dbtype)) {
                for (int i = 0; i < ORA_INDEXES.length; i++) {
                    duration += doCommand(dbtype, conn, SQL_REBUILD, ORA_INDEXES[i]);
                }
            } else {
                return -1;
            }

            return duration;
        } catch (Exception e) {
            log.error("Error creating database connection", e);
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
        return vacuum(whoami, APPDEF_TABLES);
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
     * See if monitoring feature is enabled for product
     */
    public boolean isMonitoringEnabled() {
        try {
            LicenseManager.instance().enforceFeatureLimit(LicenseManager.FEATURE_MONITOR);
            return true;
        } catch (FeatureUnavailableException e) {
            if (log.isDebugEnabled()) {
                log.debug("Monitoring feature is not enabled");
            }

            return false;
        }
    }

    /**
     * Retrieves the currently active license. Returns null if there is no active license. Otherwise it needs to
     * propogate the appropriate error back up to the caller, otherwise the error will be masked, and from a UI
     * perspective the user will think no license has been installed yet.
     *
     * @return The License object
     */
    public License getLicense() {
        // it's legal to return a null license, which then by-passes
        // the check to whether the expirationDate in the backing store
        // has been fiddled with
        License license = LicenseManager.instance().getLicense();
        if (license == null) {
            return license;
        }

        /*
         * but if the license exists, it can only be returned if the data in the backing store is consistent.  so call
         * the get method, which checks the backing store, and returns the license. if there are any errors, they will
         * bubble up as exceptions for the UI to handle appropriately.  otherwise, the license will be synced with the
         * backing store appropriately.
         */
        try {
            LicenseStoreManager.store(license);
        } catch (Exception ule) {
            log.error(ule.getMessage());
            throw new LicenseException(ule);
        }

        return license;
    }

    /**
     * Update the deployed license file by writing it to disk and reinitializing the LicenseManager static singleton.
     *
     * @param licenseData a byte array of the license data
     */
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public void updateLicense(Subject subject, byte[] licenseData) {
        try {
            MBeanServer mbs = MBeanServerLocator.locateJBoss();
            ObjectName name = ObjectNameFactory.create("jboss.system:type=ServerConfig");
            Object mbean = MBeanServerInvocationHandler.newProxyInstance(mbs, name, ServerConfig.class, false);

            File deployDirectory = new File(((ServerConfig) mbean).getServerHomeDir(), "deploy");

            String licenseFileName = LicenseManager.getLicenseFileName();

            if (deployDirectory.exists()) {
                String licenseDirectoryName = deployDirectory.getAbsolutePath() + File.separator
                    + RHQConstants.EAR_FILE_NAME + File.separator + "license";
                File licenseDir = new File(licenseDirectoryName);
                if (licenseDir.exists()) {
                    File licenseFile = saveLicenseFile(licenseDir, licenseFileName, licenseData);

                    // Now, we've updated the licenses on disk and we'll re-initialize the LicenseManager
                    LicenseManager.instance().doStartupCheck(licenseFile.getAbsolutePath());
                } else {
                    log.error("Could not update license file in non-existent directory: "
                        + licenseDir.getAbsolutePath());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private File saveLicenseFile(File dataDir, String licenseFileName, byte[] licenseData) throws IOException {
        log.debug("Updating license file in directory: " + dataDir.getAbsolutePath());

        // Copy file to data dir
        File licenseFile = new File(dataDir, licenseFileName);
        FileOutputStream fos = new FileOutputStream(licenseFile);

        // Writing in a single block since
        // a) we may be writing it twice, and
        // b) the license files are fairly small
        // c) this will be rare
        fos.write(licenseData);
        fos.close();
        return licenseFile;
    }

    /**
     * @return the expiration date, or null if the product never expires.
     */
    public Date getExpiration() {
        long exp;
        try {
            // TODO GH: Port over the LicenseManager
            exp = licenseManager.getExpiration();
        } catch (Exception e) {
            throw new LicenseException(e);
        }

        if (exp == -1) {
            return null;
        }

        return new Date(exp);
    }

    /**
     * Redeploy globally configured systems such as JAAS.
     */
    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public void reconfigureSystem(Subject whoami) {
        try {
            CustomJaasDeploymentServiceMBean mbean;

            mbean = (CustomJaasDeploymentServiceMBean)MBeanServerInvocationHandler.newProxyInstance(MBeanServerLocator.locateJBoss(),
                CustomJaasDeploymentServiceMBean.OBJECT_NAME, CustomJaasDeploymentServiceMBean.class, false);
            mbean.installJaasModules();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}