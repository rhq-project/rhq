/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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

import static java.lang.Boolean.TRUE;
import static org.rhq.core.domain.configuration.ConfigurationUpdateStatus.FAILURE;
import static org.rhq.core.domain.configuration.ConfigurationUpdateStatus.SUCCESS;
import static org.rhq.core.domain.measurement.AvailabilityType.DOWN;
import static org.rhq.core.domain.measurement.AvailabilityType.UP;
import static org.rhq.plugins.database.DatabasePluginUtil.getNumericQueryValues;
import static org.rhq.plugins.database.DatabasePluginUtil.getSingleNumericQueryValue;
import static org.rhq.plugins.database.DatabasePluginUtil.safeClose;
import static org.rhq.plugins.postgres.PostgresUserComponent.ResourceConfig.CAN_CREATE_DATABASE_OBJECTS;
import static org.rhq.plugins.postgres.PostgresUserComponent.ResourceConfig.CAN_CREATE_ROLES;
import static org.rhq.plugins.postgres.PostgresUserComponent.ResourceConfig.CAN_LOGIN;
import static org.rhq.plugins.postgres.PostgresUserComponent.ResourceConfig.CAN_UPDATE_SYSTEM_CATALOGS_DIRECTLY;
import static org.rhq.plugins.postgres.PostgresUserComponent.ResourceConfig.CONNECTION_LIMIT;
import static org.rhq.plugins.postgres.PostgresUserComponent.ResourceConfig.INHERIT_RIGHTS;
import static org.rhq.plugins.postgres.PostgresUserComponent.ResourceConfig.PASSWORD;
import static org.rhq.plugins.postgres.PostgresUserComponent.ResourceConfig.SUPERUSER;
import static org.rhq.plugins.postgres.PostgresUserComponent.ResourceConfig.USER;
import static org.rhq.plugins.postgres.PostgresUserDiscoveryComponent.OID_PREFIX;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Set;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.DeleteResourceFacet;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.plugins.database.ConnectionPoolingSupport;
import org.rhq.plugins.database.DatabaseComponent;
import org.rhq.plugins.database.PooledConnectionProvider;

/**
 * @author Greg Hinkle
 */
public class PostgresUserComponent implements DatabaseComponent<PostgresServerComponent<?>>, ConnectionPoolingSupport,
    MeasurementFacet, ConfigurationFacet, DeleteResourceFacet {

    /**
     * @deprecated as of RHQ 4.12. Shouldn't have been exposed.
     */
    @Deprecated
    public enum UpdateType {
        CREATE, ALTER, DROP
    }

    static class ResourceConfig {
        static final String USER = "user";
        static final String CAN_LOGIN = "canLogin";
        static final String PASSWORD = "password";
        static final String INHERIT_RIGHTS = "inheritRights";
        static final String SUPERUSER = "superuser";
        static final String CAN_CREATE_DATABASE_OBJECTS = "canCreateDatabaseObjects";
        static final String CAN_CREATE_ROLES = "canCreateRoles";
        static final String CAN_UPDATE_SYSTEM_CATALOGS_DIRECTLY = "canModifyCatalogDirectly";
        static final String CONNECTION_LIMIT = "connectionLimit";

        private ResourceConfig() {
            // Constants class
        }
    }

    static final String FIND_ROLE_BY_OID = "select * from pg_roles where oid = ?";
    static final String COUNT_ROLES_WITH_OID = "select count(*) from pg_roles where oid = ?";
    // See http://wiki.postgresql.org/wiki/What%27s_new_in_PostgreSQL_9.2#pg_stat_activity_and_pg_stat_replication.27s_definitions_have_changed
    static final String FIND_STAT_ACTIVITY_BY_OID_PRE_PG_9_2 = "select " //
        + "(select count(*) from pg_stat_activity where usesysid = ? and current_query != '<IDLE>') as active, " //
        + "(select count(*) from pg_stat_activity where usesysid = ?) as total";
    static final String FIND_STAT_ACTIVITY_BY_OID = "select " //
        + "(select count(*) from pg_stat_activity where usesysid = ? and state = 'active') as active, " //
        + "(select count(*) from pg_stat_activity where usesysid = ?) as total";
    static final String UPDATE_PG_AUTHID_SET_ROLCATUPDATE_WHERE_OID = "update pg_authid " //
        + "set rolcatupdate=? where oid=?";

    private ResourceContext<PostgresServerComponent<?>> resourceContext;
    private long pgRoleOid;

    @Override
    public void start(ResourceContext<PostgresServerComponent<?>> resourceContext) {
        this.resourceContext = resourceContext;
        try {
            pgRoleOid = Long.parseLong(resourceContext.getResourceKey().substring(OID_PREFIX.length()));
        } catch (Exception e) {
            throw new InvalidPluginConfigurationException("Invalid resource key [" + resourceContext.getResourceKey()
                + "]");
        }
    }

    @Override
    public void stop() {
        resourceContext = null;
    }

    @Override
    public boolean supportsConnectionPooling() {
        return true;
    }

    @Override
    public PooledConnectionProvider getPooledConnectionProvider() {
        return resourceContext.getParentResourceComponent().getPooledConnectionProvider();
    }

    /**
     * @deprecated as of RHQ 4.12. Shouldn't have been exposed.
     */
    @Deprecated
    public String getUserName() {
        try {
            String result = getUserNameInternal();
            if (result == null) {
                throw new RuntimeException("Found no user with oid [" + pgRoleOid + "]");
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private String getUserNameInternal() throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = getPooledConnectionProvider().getPooledConnection();
            statement = connection.prepareStatement(FIND_ROLE_BY_OID);
            statement.setLong(1, pgRoleOid);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString("rolname");
            }
            return null;
        } finally {
            safeClose(connection, statement, resultSet);
        }
    }

    @Override
    public AvailabilityType getAvailability() {
        if (getSingleNumericQueryValue(this, COUNT_ROLES_WITH_OID, pgRoleOid) == 1) {
            return UP;
        } else {
            return DOWN;
        }
    }

    @Override
    public Connection getConnection() {
        return this.resourceContext.getParentResourceComponent().getConnection();
    }

    @Override
    public void removeConnection() {
        this.resourceContext.getParentResourceComponent().removeConnection();
    }

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
        Map<String, Double> values;
        if (resourceContext.getParentResourceComponent().isVersionGreaterThan92()) {
            values = getNumericQueryValues(this, FIND_STAT_ACTIVITY_BY_OID, pgRoleOid, pgRoleOid);
        } else {
            values = getNumericQueryValues(this, FIND_STAT_ACTIVITY_BY_OID_PRE_PG_9_2, pgRoleOid, pgRoleOid);
        }
        for (MeasurementScheduleRequest request : metrics) {
            report.addData(new MeasurementDataNumeric(request, values.get(request.getName())));
        }
    }

    @Override
    public Configuration loadResourceConfiguration() throws Exception {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {

            connection = getPooledConnectionProvider().getPooledConnection();
            statement = connection.prepareStatement(FIND_ROLE_BY_OID);
            statement.setLong(1, pgRoleOid);

            resultSet = statement.executeQuery();
            resultSet.next();

            Configuration config = new Configuration();
            config.put(new PropertySimple(USER, resultSet.getString("rolname")));
            config.put(new PropertySimple(CAN_LOGIN, resultSet.getBoolean("rolcanlogin")));
            config.put(new PropertySimple(INHERIT_RIGHTS, resultSet.getBoolean("rolinherit")));
            boolean rolsuper = resultSet.getBoolean("rolsuper");
            config.put(new PropertySimple(SUPERUSER, rolsuper));
            if (rolsuper) {
                config.put(new PropertySimple(CAN_CREATE_DATABASE_OBJECTS, String.valueOf(true)));
                config.put(new PropertySimple(CAN_CREATE_ROLES, String.valueOf(true)));
            } else {
                config.put(new PropertySimple(CAN_CREATE_DATABASE_OBJECTS, resultSet.getBoolean("rolcreatedb")));
                config.put(new PropertySimple(CAN_CREATE_ROLES, resultSet.getBoolean("rolcreaterole")));
            }
            config.put(new PropertySimple(CAN_UPDATE_SYSTEM_CATALOGS_DIRECTLY, resultSet.getBoolean("rolcatupdate")));
            config.put(new PropertySimple(CONNECTION_LIMIT, resultSet.getInt("rolconnlimit")));

            return config;

        } finally {
            safeClose(connection, statement, resultSet);
        }
    }

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        Configuration config = report.getConfiguration();

        String userName = config.getSimpleValue(USER);
        if (userName == null || userName.trim().isEmpty()) {
            report.setStatus(FAILURE);
            report.setErrorMessage("User name is missing");
            return;
        }

        String currentUserName;
        try {
            currentUserName = getUserNameInternal();
        } catch (SQLException e) {
            report.setStatus(FAILURE);
            report.setErrorMessageFromThrowable(e);
            return;
        }

        if (!userName.equals(currentUserName)) {
            Connection connection = null;
            Statement statement = null;
            try {
                connection = getPooledConnectionProvider().getPooledConnection();
                statement = connection.createStatement();
                statement.executeUpdate("alter role " + currentUserName + " rename to " + userName);
            } catch (SQLException e) {
                report.setStatus(FAILURE);
                report.setErrorMessageFromThrowable(e);
                return;
            } finally {
                safeClose(connection, statement);
            }
        }

        Connection connection = null;
        PreparedStatement statement = null;

        try {
            connection = getPooledConnectionProvider().getPooledConnection();
            statement = connection.prepareStatement(buildUserSql(config, /*existing user*/false));
            statement.executeUpdate();
        } catch (SQLException e) {
            report.setStatus(FAILURE);
            report.setErrorMessageFromThrowable(e);
            return;
        } finally {
            safeClose(connection, statement);
        }

        try {
            connection = getPooledConnectionProvider().getPooledConnection();
            statement = connection.prepareStatement(UPDATE_PG_AUTHID_SET_ROLCATUPDATE_WHERE_OID);
            statement.setBoolean(
                1,
                Boolean.valueOf(config.getSimpleValue(SUPERUSER))
                    && Boolean.valueOf(config.getSimpleValue(CAN_UPDATE_SYSTEM_CATALOGS_DIRECTLY)));
            statement.setLong(2, pgRoleOid);
            statement.executeUpdate();
        } catch (SQLException e) {
            report.setStatus(FAILURE);
            report.setErrorMessageFromThrowable(e);
            return;
        } finally {
            safeClose(connection, statement);
        }

        report.setStatus(SUCCESS);
    }

    static String buildUserSql(Configuration config, boolean newUser) {
        StringBuilder sql = new StringBuilder();

        if (newUser) {
            sql.append("create");
        } else {
            sql.append("alter");
        }

        sql.append(" user ").append(config.getSimpleValue(USER));
        sql.append(Boolean.valueOf(config.getSimpleValue(CAN_LOGIN)) ? " LOGIN" : " NOLOGIN");

        String password = config.getSimpleValue(PASSWORD);
        if (password != null && !password.trim().isEmpty()) {
            sql.append(" password '").append(password).append("'");
        }

        sql.append(Boolean.valueOf(config.getSimpleValue(INHERIT_RIGHTS)) ? " INHERIT" : " NOINHERIT");

        if (Boolean.valueOf(config.getSimpleValue(SUPERUSER))) {
            // Do not add implied CREATEROLE and CREATEDB permissions otherwise Postgres rejects the query
            sql.append(" SUPERUSER");
        } else {
            sql.append(" NOSUPERUSER");
            sql.append(Boolean.valueOf(config.getSimpleValue(CAN_CREATE_DATABASE_OBJECTS)) ? " CREATEDB"
                : " NOCREATEDB");
            sql.append(Boolean.valueOf(config.getSimpleValue(CAN_CREATE_ROLES)) ? " CREATEROLE" : " NOCREATEROLE");
        }

        sql.append(" connection limit ");
        PropertySimple connectionLimit = config.getSimple(CONNECTION_LIMIT);
        if (connectionLimit != null) {
            sql.append(connectionLimit.getIntegerValue());
        } else {
            sql.append("-1");
        }

        return sql.toString();
    }

    /**
     * @deprecated as of RHQ 4.12. Shouldn't have been exposed.
     */
    public static String getUserSQL(Configuration config, UpdateType type) {
        String result;
        switch (type) {
        case CREATE:
            result = buildUserSql(config, /*new user*/true);
            break;
        case ALTER:
            result = buildUserSql(config, /*existing user*/false);
            break;
        case DROP:
            result = "drop user " + config.getSimpleValue(USER);
            break;
        default:
            result = null;
        }
        return result;
    }

    @Override
    public void deleteResource() throws Exception {
        String userName = getUserNameInternal();
        Connection connection = null;
        Statement statement = null;
        try {
            connection = getPooledConnectionProvider().getPooledConnection();
            statement = connection.createStatement();
            statement.executeUpdate("drop role " + userName);
        } finally {
            safeClose(connection, statement);
        }
    }
}
