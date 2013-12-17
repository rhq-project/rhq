/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.plugins.database;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;
import com.jolbox.bonecp.hooks.AbstractConnectionHook;
import com.jolbox.bonecp.hooks.AcquireFailConfig;
import com.jolbox.bonecp.hooks.ConnectionHook;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;

/**
 * Base implementation of a {@link PooledConnectionProvider}. Plugin authors <em>should</em>:
 * <ol>
 *     <li>create a concrete implementation which overrides the {@link #getDriverClass()} method</li>
 *     <li>adopt the configuration properties described here or override the corresponding #get method</li>
 * </ol>
 *
 * The first point is important if a concrete database plugin embeds the JDBC driver: the database-specific driver class
 * <strong>must</strong> be loaded by the child plugin classloader.
 *
 * @author Elias Ross
 */
public abstract class BasePooledConnectionProvider implements PooledConnectionProvider {
    private static final Log LOG = LogFactory.getLog(BasePooledConnectionProvider.class);

    /**
     * Driver class key.
     */
    public static final String DRIVER_CLASS = "driverClass";

    /**
     * JDBC URL config key.
     */
    public static final String URL = "url";

    /**
     * JDBC username config key.
     */
    public static final String USERNAME = "username";

    /**
     * JDBC password config key.
     */
    public static final String PASSWORD = "password";

    /**
     * If true, track connections and statements.
     */
    public static final String TRACK = "track";

    /**
     * Connection timeout setting.
     */
    public static final String TIMEOUT = "connectionTimeout";

    protected final Configuration pluginConfig;

    /**
     * Connection pool.
     * The connection pool implementation details should not be exposed as
     * the implementation may change.
     */
    private final BoneCP connectionPool;

    protected BasePooledConnectionProvider(Configuration pluginConfig) throws Exception {
        this.pluginConfig = pluginConfig;
        BoneCPConfig bconfig = new BoneCPConfig(getConnectionProperties());
        Class<Driver> driverClass;
        try {
            driverClass = getDriverClass();
        } catch (ClassNotFoundException e) {
            LOG.warn("Could not load driver class from: " + Thread.currentThread().getContextClassLoader());
            throw e;
        }
        if (driverClass != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Using driver class " + driverClass);
            }
            Driver driver = driverClass.newInstance();
            DataSource datasourceBean = new DriverDataSource(driver, getJdbcUrl(), getConnectionProperties());
            bconfig.setDatasourceBean(datasourceBean);
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Using driver manager for " + getJdbcUrl());
            }
            bconfig.setJdbcUrl(getJdbcUrl());
        }
        bconfig.setAcquireIncrement(1);
        bconfig.setAcquireRetryAttempts(0);
        bconfig.setAcquireRetryDelayInMs(0);
        bconfig.setPartitionCount(2);
        bconfig.setMaxConnectionsPerPartition(5);
        bconfig.setPassword(getPassword());
        bconfig.setUsername(getUsername());
        bconfig.setConnectionTimeoutInMs(getConnectionTimeout());
        if (isTrack()) {
            bconfig.setCloseConnectionWatch(true);
            bconfig.setCloseConnectionWatchTimeout(10, TimeUnit.MINUTES);
        }
        bconfig.setLazyInit(false);
        bconfig.setDisableJMX(true);
        // Do not manage retry
        ConnectionHook hook = new AbstractConnectionHook() {
            public boolean onAcquireFail(Throwable t, AcquireFailConfig acquireConfig) {
                LOG.error("Failed to obtain connection", t);
                return false;
            }
        };
        bconfig.setConnectionHook(hook);
        connectionPool = new BoneCP(bconfig);
    }

    private boolean isTrack() {
        return Boolean.valueOf(pluginConfig.getSimpleValue(TRACK, null));
    }

    @Override
    public Connection getPooledConnection() throws SQLException {
        if (LOG.isTraceEnabled()) {
            LOG.trace("get connection for " + getJdbcUrl() + " free " + connectionPool.getTotalFree() + "/"
                + connectionPool.getTotalCreatedConnections());
        }
        return connectionPool.getConnection();
    }

    /**
     * Return additional database connection pool properties.
     * By default, returns an empty properties object.
     */
    protected Properties getConnectionProperties() {
        return new Properties();
    }

    /**
     * Returns the driver class, by default the name from {@link #getDriverClassName()}.
     * If not configured, this assumes the plugin will load the appropriate driver.
     * @throws ClassNotFoundException if the classloader could not load this class
     */
    protected Class<Driver> getDriverClass() throws ClassNotFoundException {
        String cname = getDriverClassName();
        if (cname != null) {
            return (Class<Driver>) Thread.currentThread().getContextClassLoader().loadClass(cname);
        }
        return null;
    }

    /**
     * Returns the driver class, by default configuration item {@link #DRIVER_CLASS}.
     */
    protected String getDriverClassName() throws ClassNotFoundException {
        return pluginConfig.getSimpleValue(DRIVER_CLASS, null);
    }

    /**
     * Implemented by subclasses to return the JDBC connection URL.
     * By default, returns the configuration item {@link #URL}.
     */
    protected String getJdbcUrl() {
        return pluginConfig.getSimpleValue(URL);
    }

    /**
     * Return the JDBC username.
     * By default, returns the configuration item {@link #USERNAME}.
     */
    protected String getUsername() {
        return pluginConfig.getSimpleValue(USERNAME);
    }

    /**
     * Return the JDBC password.
     * By default, returns the configuration item {@link #PASSWORD}.
     */
    protected String getPassword() {
        return pluginConfig.getSimpleValue(PASSWORD);
    }

    /**
     * Return the connection timeout setting, in milliseconds.
     * By default, returns the configuration item {@link #TIMEOUT}.
     */
    protected long getConnectionTimeout() {
        String s = pluginConfig.getSimpleValue(TIMEOUT);
        if (s == null) {
            return 1000 * 10;
        }
        return Long.parseLong(s);
    }

    /**
     * Shutdown the connection pool.
     */
    public void close() {
        connectionPool.shutdown();
    }
}
