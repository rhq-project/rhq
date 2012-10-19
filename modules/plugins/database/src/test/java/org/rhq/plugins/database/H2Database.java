package org.rhq.plugins.database;

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
import org.rhq.core.util.jdbc.JDBCUtil;

/**
 * Tests using the H2Database.
 */
public class H2Database implements DatabaseComponent<ResourceComponent<?>> {

    private Log log = LogFactory.getLog(this.getClass());
    protected ResourceContext resourceContext;
    private Connection connection;
    private Configuration configuration;

    public void start(ResourceContext resourceContext) throws InvalidPluginConfigurationException, Exception {
        this.resourceContext = resourceContext;
        this.configuration = resourceContext.getPluginConfiguration();
    }

    public void stop() {
        removeConnection();
    }

    public AvailabilityType getAvailability() {
        Connection conn = getConnection();
        AvailabilityType result = AvailabilityType.DOWN;
        if (conn != null) {
            result = AvailabilityType.UP;
        }
        return result;

    }

    public Connection getConnection() {
        try {
            if (this.connection == null || connection.isClosed()) {
                this.connection = buildConnection();
            }
        } catch (SQLException e) {
            log.info("Unable to create connection", e);
        }
        return this.connection;
    }

    @Override
    public void removeConnection() {
        JDBCUtil.safeClose(connection);
        this.connection = null;
    }

    private Connection buildConnection() throws SQLException {
        String driverClass = configuration.getSimpleValue("driverClass", "org.h2.Driver");
        try {
            Class.forName(driverClass);
        } catch (ClassNotFoundException e) {
            throw new InvalidPluginConfigurationException("Specified JDBC driver class (" + driverClass
                + ") not found.");
        }

        String url = configuration.getSimpleValue("url", "jdbc:h2:test");
        String username = configuration.getSimpleValue("username", "sa");
        String password = configuration.getSimpleValue("password", "");
        log.debug("Attempting JDBC connection to [" + url + "]");
        return DriverManager.getConnection(url, username, password);
    }


}
