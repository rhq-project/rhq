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

package org.rhq.plugins.mysql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.availability.AvailabilityFacet;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.database.ConnectionPoolingSupport;
import org.rhq.plugins.database.DatabaseComponent;
import org.rhq.plugins.database.DatabasePluginUtil;
import org.rhq.plugins.database.PooledConnectionProvider;

/**
 * @author Steve Millidge (C2B2 Consulting Limited)
 */
public class MySqlDatabaseComponent implements DatabaseComponent, ConnectionPoolingSupport, AvailabilityFacet,
    OperationFacet {

    private static final Log LOG = LogFactory.getLog(MySqlDatabaseComponent.class);

    private ResourceContext resourceContext;
    private MySqlComponent parent;
    private String databaseName;

    @Override
    public Connection getConnection() {
        return parent.getConnection();
    }

    @Override
    public void removeConnection() {
        parent.removeConnection();
    }

    @Override
    public boolean supportsConnectionPooling() {
        return true;
    }

    @Override
    public PooledConnectionProvider getPooledConnectionProvider() {
        return parent.getPooledConnectionProvider();
    }

    @Override
    public void start(ResourceContext rc) throws InvalidPluginConfigurationException, Exception {
        resourceContext = rc;
        databaseName = rc.getResourceKey();
        parent = (MySqlComponent) resourceContext.getParentResourceComponent();
    }

    public String getName() {
        return databaseName;
    }

    @Override
    public void stop() {
        resourceContext = null;
        databaseName = null;
        parent = null;
    }

    @Override
    public AvailabilityType getAvailability() {
        AvailabilityType result = AvailabilityType.DOWN;

        if (LOG.isDebugEnabled()) {
            LOG.debug("Availability check for " + databaseName);
        }
        Connection jdbcConnection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            jdbcConnection = getPooledConnectionProvider().getPooledConnection();
            statement = jdbcConnection.createStatement();
            resultSet = statement.executeQuery("SHOW DATABASES LIKE '" + databaseName + "'");
            if (resultSet.next()) {
                if (resultSet.getString(1).equalsIgnoreCase(databaseName)) {
                    result = AvailabilityType.UP;
                }
            }
        } catch (SQLException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Got Exception when determining database availability", e);
            }
        } finally {
            DatabasePluginUtil.safeClose(jdbcConnection, statement, resultSet);
        }
        return result;
    }

    @Override
    public OperationResult invokeOperation(String name, Configuration parameters) throws InterruptedException,
        Exception {
        if ("invokeSql".equals(name)) {
            return invokeSql(parameters);
        } else {
            throw new UnsupportedOperationException("Operation [" + name + "] is not supported yet.");
        }
    }

    private OperationResult invokeSql(Configuration parameters) throws SQLException {
        Connection jdbcConnection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            jdbcConnection = getPooledConnectionProvider().getPooledConnection();
            statement = getConnection().createStatement();
            String sql = parameters.getSimple("sql").getStringValue();
            OperationResult result = new OperationResult();

            if (parameters.getSimple("type").getStringValue().equals("update")) {
                int updateCount = statement.executeUpdate(sql);
                result.getComplexResults().put(new PropertySimple("result", "Query updated " + updateCount + " rows"));

            } else {
                resultSet = statement.executeQuery(parameters.getSimple("sql").getStringValue());

                ResultSetMetaData md = resultSet.getMetaData();
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

                while (resultSet.next()) {
                    rowCount++;
                    buf.append("<tr>");
                    for (int i = 1; i <= md.getColumnCount(); i++) {
                        buf.append("<td>");
                        buf.append(resultSet.getString(i));
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
            DatabasePluginUtil.safeClose(jdbcConnection, statement, resultSet);
        }
    }
}
