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
package org.rhq.plugins.mysql;


import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.availability.AvailabilityFacet;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.database.DatabaseComponent;
import org.rhq.plugins.database.DatabaseQueryUtility;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;

/**
 *
 * @author Steve Millidge (C2B2 Consulting Limited)
 */
public class MySqlDatabaseComponent implements DatabaseComponent, AvailabilityFacet, OperationFacet {

    private ResourceContext resourceContext;
    private MySqlComponent parent;
    private String databaseName;
    private static Log log = LogFactory.getLog(MySqlDatabaseComponent.class);

    @Override
    public Connection getConnection() {
        return parent.getConnection();
    }

    @Override
    public void removeConnection() {
        parent.removeConnection();
    }

    @Override
    public void start(ResourceContext rc) throws InvalidPluginConfigurationException, Exception {
        resourceContext = rc;
        databaseName = rc.getResourceKey();
        parent = (MySqlComponent)resourceContext.getParentResourceComponent();
    }

    public String getName() { return databaseName; }

    @Override
    public void stop() {
    }

    @Override
    public AvailabilityType getAvailability() {
        AvailabilityType result = AvailabilityType.DOWN;

        if (log.isDebugEnabled()) {
            log.debug("Availability check for " + databaseName);
        }
        Connection conn = getConnection();
        if (conn != null) {
            Statement statement = null;
            ResultSet resultSet = null;
            try {
                statement = conn.createStatement();
                resultSet = statement.executeQuery("SHOW DATABASES LIKE '" + databaseName + "'");
                if (resultSet.next()) {
                    if (resultSet.getString(1).equalsIgnoreCase(databaseName)) {
                        result = AvailabilityType.UP;
                    }
                }
            }catch(SQLException e) {
                if (log.isDebugEnabled()) {
                    log.debug("Got Exception when determining database availability",e);
                }
            } finally {
                DatabaseQueryUtility.close(statement, resultSet);
            }
        }
        return result;
    }

    @Override
    public OperationResult invokeOperation(String name, Configuration parameters)
        throws InterruptedException, Exception {

        if ("invokeSql".equals(name)) {
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

