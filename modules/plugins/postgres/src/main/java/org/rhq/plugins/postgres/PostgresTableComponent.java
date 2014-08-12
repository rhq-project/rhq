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

import static org.rhq.core.domain.measurement.AvailabilityType.DOWN;
import static org.rhq.core.domain.measurement.AvailabilityType.UNKNOWN;
import static org.rhq.core.domain.measurement.AvailabilityType.UP;
import static org.rhq.core.util.StringUtil.isBlank;
import static org.rhq.plugins.database.DatabasePluginUtil.getNumericQueryValues;
import static org.rhq.plugins.database.DatabasePluginUtil.getSingleNumericQueryValue;
import static org.rhq.plugins.database.DatabasePluginUtil.safeClose;
import static org.rhq.plugins.postgres.PostgresTableDiscoveryComponent.SCHEMA_SEPARATOR;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
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
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.database.ConnectionPoolingSupport;
import org.rhq.plugins.database.DatabaseComponent;
import org.rhq.plugins.database.DatabaseQueryUtility;
import org.rhq.plugins.database.PooledConnectionProvider;

/**
 * Represents a postgres table
 *
 * @author Greg Hinkle
 */
public class PostgresTableComponent implements DatabaseComponent<PostgresDatabaseComponent>, ConnectionPoolingSupport,
    MeasurementFacet, ConfigurationFacet, DeleteResourceFacet, OperationFacet {

    private static final Log LOG = LogFactory.getLog(PostgresTableComponent.class);

    private static final String TABLE_EXISTS_QUERY = "select 1 from pg_stat_user_tables "
        + "where schemaname = ? and relname = ?";

    private static final String TABLE_STATS_QUERY = "select ts.*,  "
        + "pg_relation_size(ts.relid) AS table_size, pg_total_relation_size(ts.relid) AS total_size, "
        + "ios.heap_blks_read, ios.heap_blks_hit, ios.idx_blks_read, ios.idx_blks_hit, "
        + "ios.toast_blks_read, ios.toast_blks_hit, ios.tidx_blks_read, ios.tidx_blks_hit "
        + "from pg_stat_user_tables ts left join pg_statio_user_tables ios on ts.relid = ios.relid "
        + "where ts.schemaname = ? and ts.relname = ?";

    private static final String TABLE_ROW_COUNT_APPROX_QUERY = "select pgc.reltuples "
        + "from pg_class pgc, pg_namespace pgn "
        + "where pgn.nspname = ? and pgc.relname = ? and pgc.relnamespace = pgn.oid";

    /**
     * @deprecated as of RHQ4.11. No longer used (and shouldn't have been exposed anyway).
     */
    @Deprecated
    public static final String PG_STAT_USER_TABLES_QUERY = "SELECT ts.*,  pg_relation_size(ts.relid) AS table_size, pg_total_relation_size(ts.relid) AS total_size, \n"
        + "  ios.heap_blks_read, ios.heap_blks_hit, ios.idx_blks_read, ios.idx_blks_hit, \n"
        + "  ios.toast_blks_read, ios.toast_blks_hit, ios.tidx_blks_read, ios.tidx_blks_hit \n"
        + "FROM pg_stat_user_tables ts LEFT JOIN pg_statio_user_tables ios on ts.relid = ios.relid \n"
        + "WHERE ts.relname = ?";

    /**
     * @deprecated as of RHQ4.11. No longer used (and shouldn't have been exposed anyway).
     */
    @Deprecated
    public static final String PG_COUNT_ROWS = "SELECT COUNT(*) FROM ";

    /**
     * @deprecated as of RHQ4.11. No longer used (and shouldn't have been exposed anyway).
     */
    @Deprecated
    public static final String PG_COUNT_ROWS_APPROX = "SELECT reltuples FROM pg_class WHERE relname = ? ";

    private ResourceContext<PostgresDatabaseComponent> resourceContext;

    public void start(ResourceContext<PostgresDatabaseComponent> context) {
        if (!context.getResourceKey().contains(SCHEMA_SEPARATOR)) {
            throw new InvalidPluginConfigurationException("Resource key in old format (missing schema name)");
        }
        if (isBlank(getSchemaNameFromContext(context))) {
            throw new InvalidPluginConfigurationException("schemaName is not defined");
        }
        if (isBlank(getTableNameFromContext(context))) {
            throw new InvalidPluginConfigurationException("tableName is not defined");
        }
        try {
            if (!tableExists(context)) {
                throw new InvalidPluginConfigurationException("table does not exist");
            }
        } catch (SQLException e) {
            throw new InvalidPluginConfigurationException("Exception while checking table existence", e);
        }
        this.resourceContext = context;
    }

    private boolean tableExists(ResourceContext<PostgresDatabaseComponent> context) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = context.getParentResourceComponent().getPooledConnectionProvider().getPooledConnection();
            statement = connection.prepareStatement(TABLE_EXISTS_QUERY);
            statement.setString(1, getSchemaNameFromContext(context));
            statement.setString(2, getTableNameFromContext(context)); // Do not use quoted name here
            resultSet = statement.executeQuery();
            return resultSet.next();
        } finally {
            safeClose(connection, statement, resultSet);
        }
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

    /**
     * May be useful for child components.
     *
     * @return the name of the schema the table belongs to
     */
    public String getSchemaName() {
        return getSchemaNameFromContext(resourceContext);
    }

    /**
     * May be useful for child components.
     *
     * @return the name of the table
     */
    public String getTableName() {
        return getTableNameFromContext(resourceContext);
    }

    public AvailabilityType getAvailability() {
        try {
            return tableExists(resourceContext) ? UP : DOWN;
        } catch (SQLException e) {
            LOG.debug("Exception while checking table existence", e);
            return UNKNOWN;
        }
    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) {
        String tableName = getTableNameFromContext(resourceContext);
        String schemaName = getSchemaNameFromContext(resourceContext);
        Map<String, Double> results = getNumericQueryValues(this, TABLE_STATS_QUERY, schemaName, tableName);
        for (MeasurementScheduleRequest request : requests) {
            String metricName = request.getName();
            Double value;
            if (metricName.equals("rows")) {
                value = getSingleNumericQueryValue(this, getCountQuery(schemaName, tableName));
            } else if (metricName.equals("rows_approx")) {
                value = getSingleNumericQueryValue(this, TABLE_ROW_COUNT_APPROX_QUERY, schemaName, tableName);
            } else {
                value = results.get(metricName);
            }
            if (value != null) {
                MeasurementDataNumeric mdn = new MeasurementDataNumeric(request, value);
                report.addData(mdn);
            }
        }
    }

    private String getCountQuery(String schemaName, String tableName) {
        return "select count(1) from " + getFullyQualifiedTableName(schemaName, getQuoted(tableName));
    }

    private String getFullyQualifiedTableName(String schemaName, String tableName) {
        return schemaName + SCHEMA_SEPARATOR + tableName;
    }

    public void deleteResource() throws Exception {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = getPooledConnectionProvider().getPooledConnection();
            statement = connection.prepareStatement("drop table "
                + getFullyQualifiedTableName(getSchemaNameFromContext(resourceContext),
                    getQuoted(getTableNameFromContext(resourceContext))));
            statement.executeUpdate();
        } finally {
            safeClose(connection, statement);
        }
    }

    public Configuration loadResourceConfiguration() throws Exception {
        Configuration config = new Configuration();
        config.put(new PropertySimple("schemaName", resourceContext.getPluginConfiguration().getSimple("schemaName")
            .getStringValue()));
        config.put(new PropertySimple("tableName", resourceContext.getPluginConfiguration().getSimple("tableName")
            .getStringValue()));

        Connection connection = null;
        ResultSet columns;
        try {
            connection = this.resourceContext.getParentResourceComponent().getConnection();
            DatabaseMetaData databaseMetaData = connection.getMetaData();
            columns = databaseMetaData.getColumns("", getSchemaNameFromContext(resourceContext),
                getTableNameFromContext(resourceContext), "");
            PropertyList columnList = new PropertyList("columns");

            while (columns.next()) {
                PropertyMap col = new PropertyMap("columnDefinition");

                col.put(new PropertySimple("columnName", columns.getString("COLUMN_NAME")));
                col.put(new PropertySimple("columnType", columns.getString("TYPE_NAME")));
                col.put(new PropertySimple("columnLength", columns.getInt("COLUMN_SIZE")));
                col.put(new PropertySimple("columnPrecision", columns.getInt("DECIMAL_DIGITS")));
                col.put(new PropertySimple("columnDefault", columns.getString("COLUMN_DEF")));
                col.put(new PropertySimple("columnNullable", columns.getBoolean("IS_NULLABLE")));

                columnList.add(col);
            }

            config.put(columnList);
        } finally {
            safeClose(connection);
        }

        return config;
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        try {
            Configuration updatedConfiguration = report.getConfiguration();
            PropertyList updatedColumns = updatedConfiguration.getList("columns");

            Connection connection = this.resourceContext.getParentResourceComponent().getConnection();

            DatabaseMetaData dmd = connection.getMetaData();
            ResultSet rs = dmd.getColumns("", "", getTableNameFromContext(resourceContext), "");
            Map<String, ColumnDefinition> existingDefs = new HashMap<String, ColumnDefinition>();
            try {
                while (rs.next()) {
                    ColumnDefinition def = new ColumnDefinition(rs);
                    existingDefs.put(def.columnName, def);
                }
            } finally {
                rs.close();
            }

            for (Property newColumnDefinition : updatedColumns.getList()) {
                PropertyMap colDef = (PropertyMap) newColumnDefinition;

                ColumnDefinition existingDef = existingDefs.get(colDef.getSimple("columnName").getStringValue());
                ColumnDefinition newDef = new ColumnDefinition(colDef);
                if (existingDef == null) {
                    // This is a new column to add
                    String sql = "ALTER TABLE " + getQuoted(getTableNameFromContext(resourceContext)) + " ADD COLUMN "
                        + newDef.getColumnSql();
                    if (DatabaseQueryUtility.executeUpdate(this, sql) != 0) {
                        throw new RuntimeException("Couldn't add column using SQL: " + sql);
                    }
                } else {
                    existingDefs.remove(existingDef.columnName);
                    boolean columnLengthChanged = ((existingDef.columnLength != null && !existingDef.columnLength
                        .equals(newDef.columnLength)) || (existingDef.columnLength == null && existingDef.columnLength != null));
                    boolean columnPrecisionChanged = ((existingDef.columnPrecision != null && !existingDef.columnPrecision
                        .equals(newDef.columnPrecision)) || (existingDef.columnPrecision == null && existingDef.columnPrecision != null));
                    if (!existingDef.columnType.equals(newDef.columnType) || columnLengthChanged
                        || columnPrecisionChanged) {
                        String sql = "ALTER TABLE " + getQuoted(getTableNameFromContext(resourceContext)) + " ALTER COLUMN "
                            + getQuoted(newDef.columnName) + " TYPE " + newDef.columnType;
                        if (newDef.columnLength != null) {
                            sql += " ( " + newDef.columnLength;
                            // TODO: Implement a more robust check to figure out if this column has a numeric type.
                            if (newDef.columnPrecision != null && !newDef.columnType.startsWith("varchar"))
                                sql += ", " + newDef.columnPrecision;
                            sql += " ) ";
                        }

                        if (DatabaseQueryUtility.executeUpdate(this, sql) != 1) {
                            throw new RuntimeException("Couldn't alter column type using SQL: " + sql);
                        }
                    }

                    // Set default separately.
                    boolean columnDefaultChanged = ((existingDef.columnDefault != null && !existingDef.columnDefault
                        .equals(newDef.columnDefault)) || (existingDef.columnDefault == null && newDef.columnDefault != null));
                    if (columnDefaultChanged) {
                        String sql = "ALTER TABLE " + getQuoted(getTableNameFromContext(resourceContext)) + " ALTER COLUMN "
                            + getQuoted(newDef.columnName);
                        if (newDef.columnDefault == null) {
                            sql += " DROP DEFAULT";
                        } else {
                            sql += " SET DEFAULT " + newDef.columnDefault;
                        }

                        if (DatabaseQueryUtility.executeUpdate(this, sql) != 1) {
                            throw new RuntimeException("Couldn't update column default using SQL: " + sql);
                        }
                    }
                }
            }

            // Cols left in existdef map have been removed and need to be dropped
            for (ColumnDefinition def : existingDefs.values()) {
                DatabaseQueryUtility.executeUpdate(this, "ALTER TABLE " + getQuoted(getTableNameFromContext(resourceContext))
                    + " DROP COLUMN " + getQuoted(def.columnName));
            }

            report.setStatus(ConfigurationUpdateStatus.SUCCESS);
        } catch (SQLException e) {
            report.setErrorMessageFromThrowable(e);
            report.setStatus(ConfigurationUpdateStatus.FAILURE);
        }
    }

    public Connection getConnection() {
        return this.resourceContext.getParentResourceComponent().getConnection();
    }

    public void removeConnection() {
        this.resourceContext.getParentResourceComponent().removeConnection();
    }

    public OperationResult invokeOperation(String name, Configuration parameters) throws InterruptedException,
        Exception {

        if ("vacuum".equals(name)) {
            Connection connection = null;
            PreparedStatement statement = null;
            try {
                connection = getPooledConnectionProvider().getPooledConnection();
                statement = connection.prepareStatement("vacuum "
                    + getFullyQualifiedTableName(getSchemaNameFromContext(resourceContext),
                        getQuoted(getTableNameFromContext(resourceContext))));
                statement.executeUpdate();
            } finally {
                safeClose(connection, statement);
            }
        }
        return null;
    }

    static class ColumnDefinition {
        String columnName;
        String columnType;
        Integer columnLength;
        Integer columnPrecision;
        String columnDefault;
        boolean columnNullable;

        public ColumnDefinition(ResultSet rs) throws SQLException {
            columnName = rs.getString("COLUMN_NAME");
            columnType = rs.getString("TYPE_NAME");
            columnLength = rs.getInt("COLUMN_SIZE");
            columnPrecision = rs.getInt("DECIMAL_DIGITS");
            columnDefault = rs.getString("COLUMN_DEF");
            columnNullable = rs.getBoolean("IS_NULLABLE");
        }

        public ColumnDefinition(PropertyMap column) {
            columnName = column.getSimple("columnName").getStringValue();
            columnType = column.getSimple("columnType").getStringValue();
            columnLength = (column.getSimple("columnLength") == null) ? null : column.getSimple("columnLength")
                .getIntegerValue();
            columnPrecision = (column.getSimple("columnPrecision") == null) ? null : column
                .getSimple("columnPrecision").getIntegerValue();
            columnDefault = (column.getSimple("columnDefault") == null) ? null : column.getSimple("columnDefault")
                .getStringValue();
            columnNullable = !(column.getSimple("columnNullable") == null || column.getSimple("columnNullable").getBooleanValue() == null)
                    && column.getSimple("columnNullable").getBooleanValue().booleanValue();
        }

        public String getColumnSql() {
            StringBuilder buf = new StringBuilder();
            buf.append(getQuoted(columnName)).append(" ").append(columnType);
            if(!isArrayColumnType(columnType)) {
                if (columnLength != null) {
                    buf.append("(").append(columnLength).append(")");
                }

                if (columnPrecision != null) {
                    buf.append("(").append(columnPrecision).append(")");
                }
            }
            if (columnDefault != null) {
                buf.append(" DEFAULT ").append(columnDefault);
            }

            if (!columnNullable) {
                buf.append(" NOT NULL");
            }

            return buf.toString();
        }

        private boolean isArrayColumnType(String columnType) {
            return columnType != null && columnType.trim().endsWith("[]");
        }
    }

    private static String getSchemaNameFromContext(ResourceContext<PostgresDatabaseComponent> resourceContext) {
        return resourceContext.getPluginConfiguration().getSimpleValue("schemaName");
    }

    private static String getTableNameFromContext(ResourceContext<PostgresDatabaseComponent> resourceContext) {
        return resourceContext.getPluginConfiguration().getSimpleValue("tableName");
    }

    private static String getQuoted(String s) {
        return "\"" + s + "\"";
    }
}
