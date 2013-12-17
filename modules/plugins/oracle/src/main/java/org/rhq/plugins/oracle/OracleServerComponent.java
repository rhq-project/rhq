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

package org.rhq.plugins.oracle;

import static org.rhq.core.domain.measurement.AvailabilityType.DOWN;
import static org.rhq.core.domain.measurement.AvailabilityType.UP;
import static org.rhq.plugins.database.DatabasePluginUtil.getNumericQueryValueMap;
import static org.rhq.plugins.database.DatabasePluginUtil.getSingleNumericQueryValue;
import static org.rhq.plugins.oracle.OraclePooledConnectionProvider.CREDENTIALS_PROPERTY;
import static org.rhq.plugins.oracle.OraclePooledConnectionProvider.DRIVER_CLASS_PROPERTY;
import static org.rhq.plugins.oracle.OraclePooledConnectionProvider.PRINCIPAL_PROPERTY;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.plugins.database.ConnectionPoolingSupport;
import org.rhq.plugins.database.DatabaseComponent;
import org.rhq.plugins.database.DatabasePluginUtil;
import org.rhq.plugins.database.PooledConnectionProvider;

/**
 * @author Greg Hinkle
 */
public class OracleServerComponent implements DatabaseComponent, ConnectionPoolingSupport, MeasurementFacet {
    private static final Log LOG = LogFactory.getLog(OracleServerComponent.class);

    private ResourceContext resourceContext;
    @Deprecated
    private Connection connection;
    private OraclePooledConnectionProvider pooledConnectionProvider;

    public void start(ResourceContext resourceContext) throws InvalidPluginConfigurationException, Exception {
        this.resourceContext = resourceContext;
        buildSharedConnectionIfNeeded();
        pooledConnectionProvider = new OraclePooledConnectionProvider(resourceContext.getPluginConfiguration());
    }

    public void stop() {
        removeConnection();
    }

    @Override
    public boolean supportsConnectionPooling() {
        return true;
    }

    @Override
    public PooledConnectionProvider getPooledConnectionProvider() {
        return pooledConnectionProvider;
    }

    private void buildSharedConnectionIfNeeded() {
        try {
            if (this.connection == null || connection.isClosed()) {
                this.connection = buildConnection(this.resourceContext.getPluginConfiguration());
            }
        } catch (SQLException e) {
            LOG.debug("Unable to create oracle connection", e);
        }
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

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
        Map<String, Double> values = getNumericQueryValueMap(this, "SELECT name, value FROM V$SYSSTAT");
        for (MeasurementScheduleRequest request : metrics) {
            if (request.getName().equals("totalSize")) {
                Double val = getSingleNumericQueryValue(this, "SELECT SUM(bytes) FROM SYS.DBA_DATA_FILES");
                report.addData(new MeasurementDataNumeric(request, val));
            } else {
                Double value = values.get(request.getName());
                if (value != null) {
                    report.addData(new MeasurementDataNumeric(request, value));
                }
            }
        }
    }

    public Connection getConnection() {
        buildSharedConnectionIfNeeded();
        return this.connection;
    }

    public void removeConnection() {
        DatabasePluginUtil.safeClose(this.connection);
        this.connection = null;
    }

    public static Connection buildConnection(Configuration configuration) throws SQLException {
        String driverClass = configuration.getSimple(DRIVER_CLASS_PROPERTY).getStringValue();
        try {
            Class.forName(driverClass);
        } catch (ClassNotFoundException e) {
            throw new InvalidPluginConfigurationException("Specified JDBC driver class (" + driverClass
                + ") not found.");
        }

        String url = buildUrl(configuration);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Attempting JDBC connection to [" + url + "]");
        }

        String principal = configuration.getSimple(PRINCIPAL_PROPERTY).getStringValue();
        String credentials = configuration.getSimple(CREDENTIALS_PROPERTY).getStringValue();

        Properties props = new Properties();
        props.put("user", principal);
        props.put("password", credentials);
        if (principal.equalsIgnoreCase("SYS")) {
            props.put("internal_logon", "sysdba");
        }

        return DriverManager.getConnection(url, props);
    }

    static String buildUrl(Configuration configuration) {
        String connMethod = configuration.getSimpleValue("connectionMethod", "SID");
        if (connMethod.equalsIgnoreCase("SID")) {
            return "jdbc:oracle:thin:@" + configuration.getSimpleValue("host", "localhost") + ":"
                + configuration.getSimpleValue("port", "1521") + ":" + configuration.getSimpleValue("sid", "XE");
        } else {
            return "jdbc:oracle:thin:@//" + configuration.getSimpleValue("host", "localhost") + ":"
                + configuration.getSimpleValue("port", "1521") + "/" + configuration.getSimpleValue("sid", "XE");
        }
    }
}
