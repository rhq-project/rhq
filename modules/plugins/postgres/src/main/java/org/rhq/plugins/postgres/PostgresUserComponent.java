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

package org.rhq.plugins.postgres;

import static org.rhq.plugins.database.DatabasePluginUtil.getNumericQueryValues;
import static org.rhq.plugins.database.DatabasePluginUtil.getSingleNumericQueryValue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Set;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.DeleteResourceFacet;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.plugins.database.ConnectionPoolingSupport;
import org.rhq.plugins.database.DatabaseComponent;
import org.rhq.plugins.database.DatabasePluginUtil;
import org.rhq.plugins.database.PooledConnectionProvider;

/**
 * @author Greg Hinkle
 */
public class PostgresUserComponent implements DatabaseComponent<PostgresServerComponent<?>>, ConnectionPoolingSupport,
    MeasurementFacet, ConfigurationFacet, DeleteResourceFacet {

    private ResourceContext<PostgresServerComponent<?>> resourceContext;

    public void start(ResourceContext<PostgresServerComponent<?>> resourceContext) {
        this.resourceContext = resourceContext;
    }

    public void stop() {
        this.resourceContext = null;
    }

    @Override
    public boolean supportsConnectionPooling() {
        return true;
    }

    @Override
    public PooledConnectionProvider getPooledConnectionProvider() {
        return resourceContext.getParentResourceComponent().getPooledConnectionProvider();
    }

    public String getUserName() {
        return this.resourceContext.getPluginConfiguration().getSimpleValue("userName", null);
    }

    public AvailabilityType getAvailability() {
        if (getSingleNumericQueryValue(this, "SELECT COUNT(*) FROM PG_ROLES WHERE rolname = ?", getUserName()) == 1) {
            return AvailabilityType.UP;
        } else {
            return AvailabilityType.DOWN;
        }
    }

    public Connection getConnection() {
        return this.resourceContext.getParentResourceComponent().getConnection();
    }

    public void removeConnection() {
        this.resourceContext.getParentResourceComponent().removeConnection();
    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
        Map<String, Double> values = getNumericQueryValues(this,
            "SELECT (SELECT COUNT(*) FROM pg_stat_activity where usename = ? AND current_query != '<IDLE>') AS active,\n"
                + "  (SELECT COUNT(*) FROM pg_stat_activity WHERE usename = ?) AS total", getUserName(), getUserName());

        for (MeasurementScheduleRequest request : metrics) {
            report.addData(new MeasurementDataNumeric(request, values.get(request.getName())));
        }
    }

    public Configuration loadResourceConfiguration() throws Exception {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = getPooledConnectionProvider().getPooledConnection();
            statement = connection.prepareStatement("SELECT * FROM PG_ROLES WHERE rolname = ?");
            statement.setString(1, getUserName());

            resultSet = statement.executeQuery();
            resultSet.next();
            Configuration config = new Configuration();
            config.put(new PropertySimple("user", resultSet.getString("rolname")));
            config.put(new PropertySimple("canLogin", resultSet.getBoolean("rolcanlogin")));
            config.put(new PropertySimple("inheritRights", resultSet.getBoolean("rolinherit")));
            config.put(new PropertySimple("superuser", resultSet.getBoolean("rolsuper")));
            config.put(new PropertySimple("canCreateDatabaseObjects", resultSet.getBoolean("rolcreatedb")));
            config.put(new PropertySimple("canCreateRoles", resultSet.getBoolean("rolcreaterole")));
            config.put(new PropertySimple("canModifyCatalogDirectly", resultSet.getBoolean("rolcatupdate")));
            config.put(new PropertySimple("connectionLimit", resultSet.getInt("rolconnlimit")));
            return config;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            DatabasePluginUtil.safeClose(connection, statement, resultSet);
        }
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        Configuration config = report.getConfiguration();
        String sql = getUserSQL(config, UpdateType.ALTER);

        Connection connection = null;
        Statement statement = null;
        try {
            connection = getPooledConnectionProvider().getPooledConnection();
            statement = getConnection().createStatement();
            statement.executeUpdate(sql);
            report.setStatus(ConfigurationUpdateStatus.SUCCESS);
        } catch (SQLException e) {
            report.setErrorMessageFromThrowable(e);
        } finally {
            DatabasePluginUtil.safeClose(connection, statement);
        }
    }

    public enum UpdateType {
        CREATE, ALTER, DROP
    }

    public static String getUserSQL(Configuration config, UpdateType type) {
        int connectionLimit = -1;
        PropertySimple connLimit = config.getSimple("connectionLimit");
        if ((connLimit != null) && (connLimit.getIntegerValue() != null)) {
            connectionLimit = connLimit.getIntegerValue();
        }

        String sql = type.name() + " USER " + config.getSimpleValue("user", null) + " ";

        if (type != UpdateType.DROP) {
            String password = config.getSimpleValue("password", null);
            if (password != null && password.length() != 0) {
                sql += " WITH PASSWORD '" + config.getSimpleValue("password", null) + "' ";
            }

            sql += (config.getSimple("canCreateDatabaseObjects").getBooleanValue() ? "CREATEDB " : "NOCREATEDB ");
            sql += (config.getSimple("canCreateRoles").getBooleanValue() ? "CREATEUSER " : "NOCREATEUSER ");
            sql += (connectionLimit > -1) ? ("CONNECTION LIMIT " + connectionLimit) : "";
        }

        return sql;
    }

    public void deleteResource() throws Exception {
        String sql = "DROP USER " + this.resourceContext.getResourceKey();
        Connection connection = null;
        Statement statement = null;
        try {
            connection = getPooledConnectionProvider().getPooledConnection();
            statement = getConnection().createStatement();
            statement.executeUpdate(sql);
        } finally {
            DatabasePluginUtil.safeClose(connection, statement);
        }
    }
}
