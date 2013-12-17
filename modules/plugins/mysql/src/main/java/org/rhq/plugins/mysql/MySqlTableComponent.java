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
import org.rhq.core.domain.measurement.MeasurementDataTrait;
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
public class MySqlTableComponent implements DatabaseComponent, ConnectionPoolingSupport, MeasurementFacet {
    private static final Log LOG = LogFactory.getLog(MySqlTableComponent.class);

    private String tableName;
    private MySqlDatabaseComponent parent;
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
        tableName = rc.getResourceKey();
        parent = (MySqlDatabaseComponent) rc.getParentResourceComponent();
        databaseName = parent.getName();
    }

    @Override
    public void stop() {
        tableName = null;
        parent = null;
        databaseName = null;
    }

    @Override
    public AvailabilityType getAvailability() {
        Connection jdbcConnection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            jdbcConnection = getPooledConnectionProvider().getPooledConnection();
            statement = jdbcConnection.createStatement();
            resultSet = statement.executeQuery("show tables from " + databaseName + " like '" + tableName + "'");
            if (resultSet.first()) {
                return UP;
            }
        } catch (SQLException se) {
            // Will return down
        } finally {
            DatabasePluginUtil.safeClose(jdbcConnection, statement, resultSet);
        }
        return DOWN;
    }

    @Override
    public void getValues(MeasurementReport mr, Set<MeasurementScheduleRequest> set) throws Exception {
        Connection jdbcConnection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            jdbcConnection = getPooledConnectionProvider().getPooledConnection();
            statement = jdbcConnection.createStatement();
            resultSet = statement.executeQuery("show table status from " + databaseName + " like '" + tableName + "'");
            if (resultSet.next()) {
                for (MeasurementScheduleRequest request : set) {
                    String value = resultSet.getString(request.getName());
                    if (value == null) {
                        value = "0";
                    }
                    switch (request.getDataType()) {
                    case MEASUREMENT: {
                        mr.addData(new MeasurementDataNumeric(request, Double.valueOf(value)));
                        break;
                    }
                    case TRAIT: {
                        mr.addData(new MeasurementDataTrait(request, value));
                        break;
                    }
                    default: {
                        break;
                    }
                    }
                }
            }
        } catch (Exception se) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Unable to measure table statistics", se);
            }
        } finally {
            DatabasePluginUtil.safeClose(jdbcConnection, statement, resultSet);
        }
    }
}
