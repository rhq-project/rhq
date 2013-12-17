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
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.database.ConnectionPoolingSupport;
import org.rhq.plugins.database.DatabaseComponent;
import org.rhq.plugins.database.DatabasePluginUtil;
import org.rhq.plugins.database.DatabaseQueryUtility;
import org.rhq.plugins.database.PooledConnectionProvider;

/**
 * Represents a postgres table
 *
 * @author Greg Hinkle
 */
public class PostgresTableComponent implements DatabaseComponent<PostgresDatabaseComponent>, ConnectionPoolingSupport,
    MeasurementFacet, ConfigurationFacet, DeleteResourceFacet, OperationFacet {

    private static final List<String> PG_STAT_USER_TABLE_STATS = Arrays.asList("seq_scan", "seq_tup_read", "idx_scan",
        "idx_tup_fetch", "n_tup_ins", "n_tup_upd", "n_tup_del", "table_size", "total_size");

    public static final String PG_STAT_USER_TABLES_QUERY = "SELECT ts.*,  pg_relation_size(ts.relid) AS table_size, pg_total_relation_size(ts.relid) AS total_size, \n"
        + "  ios.heap_blks_read, ios.heap_blks_hit, ios.idx_blks_read, ios.idx_blks_hit, \n"
        + "  ios.toast_blks_read, ios.toast_blks_hit, ios.tidx_blks_read, ios.tidx_blks_hit \n"
        + "FROM pg_stat_user_tables ts LEFT JOIN pg_statio_user_tables ios on ts.relid = ios.relid \n"
        + "WHERE ts.relname = ?";

    // NOTE: You can't bind table names as parameters
    public static final String PG_COUNT_ROWS = "SELECT COUNT(*) FROM ";
    public static final String PG_COUNT_ROWS_APPROX = "SELECT reltuples FROM pg_class WHERE relname = ? ";

    private ResourceContext<PostgresDatabaseComponent> resourceContext;

    public void start(ResourceContext<PostgresDatabaseComponent> context) {
        this.resourceContext = context;
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

    public String getTableName() {
        return this.resourceContext.getPluginConfiguration().getSimple("tableName").getStringValue();
    }

    public AvailabilityType getAvailability() {
        return resourceContext.getParentResourceComponent().getAvailability();
    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) {
        this.resourceContext.getParentResourceComponent().getConnection();

        Map<String, Double> results = getNumericQueryValues(this, PG_STAT_USER_TABLES_QUERY, getTableName());
        for (MeasurementScheduleRequest request : requests) {
            String metricName = request.getName();
            Double value;
            if (metricName.equals("rows")) {
                value = getSingleNumericQueryValue(this, PG_COUNT_ROWS + getTableName());
            } else if (metricName.equals("rows_approx")) {
                value = getSingleNumericQueryValue(this, PG_COUNT_ROWS_APPROX, getTableName());
            } else {
                value = results.get(metricName);
            }

            if (value != null) {
                MeasurementDataNumeric mdn = new MeasurementDataNumeric(request, value);
                report.addData(mdn);
            }
        }
    }

    public void deleteResource() throws Exception {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = getPooledConnectionProvider().getPooledConnection();
            statement = connection.prepareStatement("DROP TABLE " + getTableName());
            statement.executeUpdate();
        } finally {
            DatabasePluginUtil.safeClose(connection, statement);
        }
    }

    public Configuration loadResourceConfiguration() throws Exception {
        Configuration config = new Configuration();
        config.put(new PropertySimple("tableName", resourceContext.getPluginConfiguration().getSimple("tableName")
            .getStringValue()));

        Connection connection = null;
        ResultSet columns = null;
        try {
            connection = this.resourceContext.getParentResourceComponent().getConnection();
            DatabaseMetaData databaseMetaData = connection.getMetaData();
            columns = databaseMetaData.getColumns("", "", getTableName(), "");

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
            DatabasePluginUtil.safeClose(connection);
        }

        return config;
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        try {
            Configuration updatedConfiguration = report.getConfiguration();
            PropertyList updatedColumns = updatedConfiguration.getList("columns");

            Connection connection = this.resourceContext.getParentResourceComponent().getConnection();

            DatabaseMetaData dmd = connection.getMetaData();
            ResultSet rs = dmd.getColumns("", "", getTableName(), "");
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
                    String sql = "ALTER TABLE " + getTableName() + " ADD COLUMN " + newDef.getColumnSql();
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
                        String sql = "ALTER TABLE " + getTableName() + " ALTER COLUMN " + newDef.columnName + " TYPE "
                            + newDef.columnType;
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
                        String sql = "ALTER TABLE " + getTableName() + " ALTER COLUMN " + newDef.columnName;
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
                DatabaseQueryUtility.executeUpdate(this, "ALTER TABLE " + getTableName() + " DROP COLUMN "
                    + def.columnName);
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
            DatabaseQueryUtility.executeUpdate(this, "vacuum " + getTableName());
        }
        return null;
    }

    private static class ColumnDefinition {
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

            // TODO this is called collumnNullable in other places - that is meant here?
            columnNullable = (column.getSimple("columnNotNull") == null) ? false : column.getSimple("columnNotNull")
                .getBooleanValue();
        }

        public String getColumnSql() {
            StringBuilder buf = new StringBuilder();
            buf.append(columnName).append(" ").append(columnType);
            if (columnLength != null) {
                buf.append("(" + columnLength + ")");
            }

            if (columnPrecision != null) {
                buf.append("(" + columnPrecision + ")");
            }

            if (columnDefault != null) {
                buf.append(" DEFAULT " + columnDefault);
            }

            if (columnNullable) {
                buf.append(" NOT NULL");
            }

            return buf.toString();
        }
    }
}
