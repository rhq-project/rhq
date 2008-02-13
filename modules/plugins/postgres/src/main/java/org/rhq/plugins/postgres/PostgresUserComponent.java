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
package org.rhq.plugins.postgres;

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
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.plugins.database.DatabaseComponent;
import org.rhq.plugins.database.DatabaseQueryUtility;

/**
 * @author Greg Hinkle
 */
public class PostgresUserComponent implements DatabaseComponent<PostgresServerComponent>, MeasurementFacet,
    ConfigurationFacet, DeleteResourceFacet {
    private ResourceContext<PostgresServerComponent> resourceContext;

    public void start(ResourceContext<PostgresServerComponent> resourceContext) {
        this.resourceContext = resourceContext;
    }

    public void stop() {
        this.resourceContext = null;
    }

    public String getUserName() {
        return this.resourceContext.getPluginConfiguration().getSimpleValue("userName", null);
    }

    public AvailabilityType getAvailability() {
        if (DatabaseQueryUtility.getSingleNumericQueryValue(this, "SELECT COUNT(*) FROM PG_ROLES WHERE rolname = ?",
            getUserName()) == 1) {
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
        Map<String, Double> values = DatabaseQueryUtility.getNumericQueryValues(this,
            "SELECT (SELECT COUNT(*) FROM pg_stat_activity where usename = ? AND current_query != '<IDLE>') AS active,\n"
                + "  (SELECT COUNT(*) FROM pg_stat_activity WHERE usename = ?) AS total", getUserName(), getUserName());

        for (MeasurementScheduleRequest request : metrics) {
            report.addData(new MeasurementDataNumeric(request, values.get(request.getName())));
        }
    }

    public Configuration loadResourceConfiguration() throws Exception {
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            statement = getConnection().prepareStatement("SELECT * FROM PG_ROLES WHERE rolname = ?");
            statement.setString(1, getUserName());

            rs = statement.executeQuery();
            rs.next();
            Configuration config = new Configuration();
            config.put(new PropertySimple("canLogin", rs.getBoolean("rolcanlogin")));
            config.put(new PropertySimple("inheritRights", rs.getBoolean("rolinherit")));
            config.put(new PropertySimple("superuser", rs.getBoolean("rolsuper")));
            config.put(new PropertySimple("canCreateDatabaseObjects", rs.getBoolean("rolcreatedb")));
            config.put(new PropertySimple("canCreateRoles", rs.getBoolean("rolcreaterol")));
            config.put(new PropertySimple("canModifyCatalogDirectly", rs.getBoolean("rolcatupdate")));
            config.put(new PropertySimple("connectionLimit", rs.getBoolean("rolconnlimit")));
            return config;
        } finally {
            JDBCUtil.safeClose(statement, rs);
        }
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        Configuration config = report.getConfiguration();

        Statement statement = null;
        String sql = getUserSQL(config, UpdateType.UPDATE);
        try {
            statement = getConnection().createStatement();
            int updates = statement.executeUpdate(sql);
            if (updates != 1) {
                report.setErrorMessage("Failed to update user " + config.getSimpleValue("user", null));
            } else {
                report.setStatus(ConfigurationUpdateStatus.SUCCESS);
            }
        } catch (SQLException e) {
            report.setErrorMessageFromThrowable(e);
        } finally {
            JDBCUtil.safeClose(statement);
        }
    }

    public enum UpdateType {
        CREATE, UPDATE, DROP
    }

    public static String getUserSQL(Configuration config, UpdateType type) {
        int connectionLimit = -1;
        PropertySimple connLimit = config.getSimple("connectionLimit");
        if ((connLimit != null) && (connLimit.getIntegerValue() != null)) {
            connectionLimit = connLimit.getIntegerValue();
        }

        return type.name()
            + " USER "
            + config.getSimpleValue("user", null)
            + " "
            + ((type != UpdateType.DROP) ? ("WITH PASSWORD '" + config.getSimpleValue("password", "") + "' "
                + (config.getSimple("canCreateDatabaseObjects").getBooleanValue() ? "CREATEDB " : "NOCREATEDB ")
                + (config.getSimple("canCreateRoles").getBooleanValue() ? "CREATEUSER " : "NOCREATEUSER ") + ((connectionLimit > -1) ? ("CONNECTION LIMIT " + connectionLimit)
                : " "))
                : "");
    }

    public void deleteResource() throws Exception {
        Statement statement = null;
        String sql = "DROP USER " + this.resourceContext.getResourceKey();
        try {
            statement = getConnection().createStatement();

            // Note: Postgres doesn't seem to return the expected update count of 1 here... but this does work
            // Note: executeUpdate()  returns 0 for statements that don't return rows like e.g. drop xxx
            statement.executeUpdate(sql);
        } finally {
            JDBCUtil.safeClose(statement);
        }
    }
}