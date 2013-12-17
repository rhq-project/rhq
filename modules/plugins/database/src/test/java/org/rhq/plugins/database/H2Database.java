package org.rhq.plugins.database;

import static org.rhq.core.domain.measurement.AvailabilityType.DOWN;
import static org.rhq.core.domain.measurement.AvailabilityType.UP;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;

/**
 * Tests using the H2Database.
 */
public class H2Database implements DatabaseComponent<ResourceComponent<?>>, ConnectionPoolingSupport {
    private static final Log LOG = LogFactory.getLog(H2Database.class);

    static final String DRIVER_CLASS_PROPERTY = "driverClass";
    static final String URL_PROPERTY = "url";
    static final String USERNAME_PROPERTY = "username";
    static final String PASSWORD_PROPERTY = "password";

    protected ResourceContext resourceContext;
    @Deprecated
    private Connection connection;
    private H2PooledConnectionProvider pooledConnectionProvider;

    public void start(ResourceContext resourceContext) throws InvalidPluginConfigurationException, Exception {
        this.resourceContext = resourceContext;
        buildSharedConnectionIfNeeded();
        pooledConnectionProvider = new H2PooledConnectionProvider(resourceContext.getPluginConfiguration());
    }

    public void stop() {
        resourceContext = null;
        DatabasePluginUtil.safeClose(connection);
        connection = null;
        pooledConnectionProvider.close();
        pooledConnectionProvider = null;
    }

    @Override
    public boolean supportsConnectionPooling() {
        return true;
    }

    @Override
    public PooledConnectionProvider getPooledConnectionProvider() {
        return pooledConnectionProvider;
    }

    public AvailabilityType getAvailability() {
        Connection jdbcConnection = null;
        try {
            jdbcConnection = getPooledConnectionProvider().getPooledConnection();
            return jdbcConnection.isValid(1) ? UP : DOWN;
        } catch (SQLException e) {
            return DOWN;
        } finally {
            DatabasePluginUtil.safeClose(jdbcConnection);
        }
    }

    public Connection getConnection() {
        buildSharedConnectionIfNeeded();
        return connection;
    }

    private void buildSharedConnectionIfNeeded() {
        try {
            if ((connection == null) || connection.isClosed()) {
                connection = buildConnection(resourceContext.getPluginConfiguration());
            }
        } catch (SQLException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Could not build shared connection", e);
            }
        }
    }

    public void removeConnection() {
        DatabasePluginUtil.safeClose(this.connection);
        this.connection = null;
    }

    static Connection buildConnection(Configuration pluginConfig) throws SQLException {
        String driverClass = pluginConfig.getSimpleValue(DRIVER_CLASS_PROPERTY, "org.h2.Driver");
        try {
            Class.forName(driverClass);
        } catch (ClassNotFoundException e) {
            throw new InvalidPluginConfigurationException("Specified JDBC driver class (" + driverClass
                + ") not found.");
        }

        String url = pluginConfig.getSimpleValue(URL_PROPERTY, "jdbc:h2:test");
        String username = pluginConfig.getSimpleValue(USERNAME_PROPERTY, "sa");
        String password = pluginConfig.getSimpleValue(PASSWORD_PROPERTY, "");
        if (LOG.isDebugEnabled()) {
            LOG.debug("Attempting JDBC connection to [" + url + "]");
        }
        return DriverManager.getConnection(url, username, password);
    }

}
