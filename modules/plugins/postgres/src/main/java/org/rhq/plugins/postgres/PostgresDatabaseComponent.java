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
import java.sql.ResultSetMetaData;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.pluginapi.inventory.CreateChildResourceFacet;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.plugins.database.DatabaseComponent;

public class PostgresDatabaseComponent implements DatabaseComponent<PostgresServerComponent>, MeasurementFacet,
    CreateChildResourceFacet, OperationFacet {
    private Log log = LogFactory.getLog(PostgresDatabaseComponent.class);

    private ResourceContext<PostgresServerComponent> resourceContext;

    private Connection databaseConnection;

    private String databaseName;

    public Connection getConnection() {
        this.databaseName = resourceContext.getPluginConfiguration().getSimple("databaseName").getStringValue();
        if (this.databaseName.equals(resourceContext.getParentResourceComponent().getResourceContext()
            .getPluginConfiguration().getSimple("db").getStringValue())) {
            return resourceContext.getParentResourceComponent().getConnection();
        } else {
            // ??? Need to use a different connection to talk to a different db?
            if (this.databaseConnection == null) {
                Configuration config = resourceContext.getParentResourceComponent().getResourceContext()
                    .getPluginConfiguration();
                config = config.deepCopy();
                config.put(new PropertySimple("db", databaseName));
                log.debug("Getting db specific connection to postgres for [" + databaseName + "] database");
                this.databaseConnection = PostgresDiscoveryComponent.buildConnection(config, true);
            }

            // TODO GH: Attempt to load other db connections? or only monitor this dbs stuff? Weird situation.
            return this.databaseConnection;
        }
    }

    public void removeConnection() {
        try {
            if ((this.databaseConnection != null) && !this.databaseConnection.isClosed()) {
                this.databaseConnection.close();
            }
        } catch (SQLException se) {
            log.debug("Closing and removing postgres connection");
        }

        this.databaseConnection = null;
    }

    public void start(ResourceContext<PostgresServerComponent> context) {
        this.resourceContext = context;
    }

    public void stop() {
        this.resourceContext = null;
    }

    public AvailabilityType getAvailability() {
        return resourceContext.getParentResourceComponent().getAvailability();
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) {
        PreparedStatement statement = null;
        try {
            statement = this.resourceContext.getParentResourceComponent().getConnection().prepareStatement(
                "SELECT *, pg_database_size(datname) AS size FROM pg_stat_database where datname = ?");
            statement.setString(1, this.resourceContext.getPluginConfiguration().getSimple("databaseName")
                .getStringValue());
            ResultSet results = statement.executeQuery();

            if (!results.next()) {
                throw new RuntimeException("Couldn't get the data"); // TODO Error handling system
            }

            for (MeasurementScheduleRequest request : metrics) {
                // Only size expected
                double val = results.getDouble(request.getName());
                report.addData(new MeasurementDataNumeric(request, val));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
            }
        }
    }

    public CreateResourceReport createResource(CreateResourceReport report) {
        StringBuilder buf = new StringBuilder();
        Configuration configuration = report.getResourceConfiguration();

        String tableName = configuration.getSimple("tableName").getStringValue();
        String owner = configuration.getSimpleValue("owner", null);
        String tablespace = configuration.getSimpleValue("tablespace", null);
        PropertyList columnList = configuration.getList("columns");

        buf.append("CREATE TABLE ").append(tableName).append("(\n");

        boolean first = true;
        for (Property c : columnList.getList()) {
            if (!first) {
                buf.append(",\n");
            }

            PropertyMap column = (PropertyMap) c;
            String colName = column.getSimple("columnName").getStringValue();
            String colType = column.getSimple("columnType").getStringValue();
            PropertySimple length = column.getSimple("columnLength");
            PropertySimple precision = column.getSimple("columnPrecision");
            PropertySimple colDefault = column.getSimple("columnDefault");
            PropertySimple colNullable = column.getSimple("columnNullable");

            if ((colName != null) && !colName.equals("")) {
                buf.append(colName).append(" ").append(colType);
                if ((length != null) && (length.getIntegerValue() != null)) {
                    buf.append("(" + length.getIntegerValue() + ")");
                }

                if ((precision != null) && (precision.getIntegerValue() != null)) {
                    buf.append("(" + precision.getIntegerValue() + ")");
                }

                if ((colDefault != null) && (colDefault.getStringValue() != null)) {
                    buf.append(" DEFAULT " + colDefault.getStringValue());
                }

                if ((colNullable != null) && (colNullable.getBooleanValue() != null)
                    && colNullable.getBooleanValue().equals(Boolean.FALSE)) {
                    buf.append(" NOT NULL");
                }

                first = false;
            }
        }

        buf.append("\n)");

        log.info("Creating table with: " + buf.toString());
        PropertyList constraintList = configuration.getList("constraints");
        if (constraintList != null) {
            for (Property c : constraintList.getList()) {
                PropertyMap constraint = (PropertyMap) c;
                // TODO
            }
        }

        Statement statement = null;
        try {
            report.setResourceKey(tableName);
            statement = getConnection().createStatement();
            statement.executeUpdate(buf.toString());
            report.setStatus(CreateResourceStatus.SUCCESS);
            report.setResourceName(tableName);
        } catch (SQLException e) {
            report.setException(e);
            report.setStatus(CreateResourceStatus.FAILURE);
        } finally {
            JDBCUtil.safeClose(statement);
        }

        return report;
    }

    public OperationResult invokeOperation(String name, Configuration parameters) 
        throws InterruptedException, Exception {
        
        if ("resetStatistics".equals(name)) {
            Statement stmt = null;
            ResultSet rs = null;
            try {
                stmt = getConnection().createStatement();
                rs = stmt.executeQuery("select * from pg_stat_reset()");
                
            } finally {
                if (rs != null) {
                    rs.close();
                }

                if (stmt != null) {
                    stmt.close();
                }
            }
            return null;
        } else if ("invokeSql".equals(name)) {
            Statement stmt = null;
            ResultSet rs = null;
            try {
                stmt = getConnection().createStatement();
                String sql = parameters.getSimple("sql").getStringValue();
                OperationResult result = new OperationResult();

                if (parameters.getSimple("type").getStringValue().equals("update")) {
                    int updateCount = stmt.executeUpdate(sql);
                    result.getComplexResults().put(new PropertySimple("result", "Query updated " + updateCount + " rows"));

                } else {
                    rs = stmt.executeQuery(parameters.getSimple("sql").getStringValue());

                    ResultSetMetaData md = rs.getMetaData();
                    StringBuilder buf = new StringBuilder();
                    int rowCount = 0;

                    buf.append("<table>");
                    buf.append("<th>");
                    for (int i = 1; i <= md.getColumnCount(); i++) {
                        buf.append("<td>");
                        buf.append(md.getColumnName(i) + " (" + md.getColumnTypeName(i) + ")");
                        buf.append("</td>");
                    }
                    buf.append("</th>");


                    while (rs.next()) {
                        rowCount++;
                        buf.append("<tr>");
                        for (int i = 1; i <= md.getColumnCount(); i++) {
                            buf.append("<td>");
                            buf.append(rs.getString(i));
                            buf.append("</td>");
                        }
                        buf.append("</tr>");
                    }

                    buf.append("</table>");
                    result.getComplexResults().put(new PropertySimple("result", "Query returned " + rowCount + " rows"));
                    result.getComplexResults().put(new PropertySimple("contents", buf.toString()));
                }
                return result;
            } finally {
                if (rs != null) {
                    rs.close();
                }

                if (stmt != null) {
                    stmt.close();
                }
            }
        } else {
            throw new UnsupportedOperationException("Operation [" + name + "] is not supported yet.");
        }
    }
}