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

import static org.rhq.core.domain.measurement.AvailabilityType.DOWN;
import static org.rhq.core.domain.measurement.AvailabilityType.UP;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
 * @author Steve Millidge (C2B2 Consulting Limited)
 */
public class MySqlUserComponent implements MeasurementFacet, DatabaseComponent, ConnectionPoolingSupport {
    private static final Log LOG = LogFactory.getLog(MySqlUserComponent.class);

    private String userName;
    private String host;
    private MySqlComponent parent;
    private ResourceContext resourceContext;

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
    public void start(ResourceContext resourceContext) throws InvalidPluginConfigurationException, Exception {
        parent = (MySqlComponent) resourceContext.getParentResourceComponent();
        this.resourceContext = resourceContext;
        userName = this.resourceContext.getPluginConfiguration().getSimple("userName").getStringValue();
        host = this.resourceContext.getPluginConfiguration().getSimple("host").getStringValue();
    }

    @Override
    public void stop() {
        parent = null;
        resourceContext = null;
        userName = null;
        host = null;
    }

    public void getValues(MeasurementReport mr, Set<MeasurementScheduleRequest> requests) throws Exception {
        Connection jdbcConnection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        int activeConnections = 0;
        int totalConnections = 0;
        try {
            jdbcConnection = getPooledConnectionProvider().getPooledConnection();
            statement = jdbcConnection.createStatement();
            resultSet = statement.executeQuery("select User,Host,State from information_schema.processlist where User='" + userName
                + "'");
            while (resultSet.next()) {
                String hostVal = resultSet.getString(2);
                String state = resultSet.getString(3);
                if (hostVal.startsWith(host)) {
                    if (state.length() > 1) {
                        activeConnections++;
                    }
                    totalConnections++;
                }
            }
        } catch (SQLException ignore) {
        } finally {
            DatabasePluginUtil.safeClose(jdbcConnection, statement, resultSet);
        }

        for (MeasurementScheduleRequest request : requests) {
            if (request.getName().equals("TotalConnections")) {
                mr.addData(new MeasurementDataNumeric(request, new Double((double) totalConnections)));
            } else if (request.getName().equals("ActiveConnections")) {
                mr.addData(new MeasurementDataNumeric(request, new Double((double) activeConnections)));
            }
        }
    }

    public AvailabilityType getAvailability() {
        Connection jdbcConnection = null;
        ResultSet resultSet = null;
        Statement statement = null;
        try {
            jdbcConnection = getPooledConnectionProvider().getPooledConnection();
            statement = jdbcConnection.createStatement();
            resultSet = statement.executeQuery("select User from mysql.user where User='" + userName + "' and Host='"
                + host + "'");
            if (resultSet.first()) {
                return UP;
            }
        } catch (SQLException sqle) {
            // Will return DOWN
            System.out.println("sqle = " + sqle);
        } finally {
            DatabasePluginUtil.safeClose(jdbcConnection, statement, resultSet);
        }
        return DOWN;
    }

}
