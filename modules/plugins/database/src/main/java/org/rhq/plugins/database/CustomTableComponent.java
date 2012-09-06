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
package org.rhq.plugins.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.util.jdbc.JDBCUtil;

/**
 * The component for the {@link CustomTableDiscoveryComponent}.
 *
 * @author Greg Hinkle
 */
public class CustomTableComponent implements DatabaseComponent<DatabaseComponent<?>>, MeasurementFacet {
    private ResourceContext<DatabaseComponent<?>> context;

    private static final Log log = LogFactory.getLog(CustomTableComponent.class);

    public void start(ResourceContext<DatabaseComponent<?>> resourceContext)
        throws InvalidPluginConfigurationException, Exception {
        this.context = resourceContext;
    }

    public void stop() {
    }

    public String getTable() {
        return this.context.getPluginConfiguration().getSimpleValue("table", "");
    }

    public AvailabilityType getAvailability() {
        if (getTable().isEmpty()) {
            // not set
            return AvailabilityType.UP;
        }
        Statement statement = null;
        try {
            statement = getConnection().createStatement();
            statement.setMaxRows(1);
            statement.setFetchSize(1);
            statement.executeQuery("SELECT * FROM " + getTable()).close();
            return AvailabilityType.UP;
        } catch (SQLException e) {
            return AvailabilityType.DOWN;
        } finally {
            JDBCUtil.safeClose(statement);
        }
    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {

        Configuration config = this.context.getPluginConfiguration();
        String query = config.getSimpleValue("metricQuery", null);

        if (query == null) {
            if (log.isTraceEnabled()) {
                ResourceType type = this.context.getResourceType();
                String resourceKey = this.context.getResourceKey();
                log.trace("Resource "
                    + resourceKey
                    + " ("
                    + type.getName()
                    + ", plugin "
                    + type.getPlugin()
                    + "): The plugin configuration doesn't specify 'metricQuery' property. Ignoring the measurement request.");
            }
            return;
        }

        query = CustomTableRowDiscoveryComponent.formatMessage(query, config.getSimpleValue("key", null));
        String column = config.getSimpleValue("column", "");
        Map<String, Double> values;
        if (Boolean.parseBoolean(column)) {
            // data in column format
            values = DatabaseQueryUtility.getNumericQueryValues(this, query);
        } else {
            // data in row format
            values = DatabaseQueryUtility.getNumericQueryValueMap(this, query);
        }
        if (log.isDebugEnabled())
            log.debug("returned values " + values);

        // this is a for loop because the name of each column can be the name of the metric
        for (MeasurementScheduleRequest request : metrics) {
            String columnName = request.getName();

            Double value = values.get(columnName);

            // the db might be returning the column name in a different case than we expect, try all lower and all upper just in case
            if (value == null) {
                value = values.get(columnName.toLowerCase());
                if (value == null) {
                    value = values.get(columnName.toUpperCase());
                }
            }

            if (value != null) {
                report.addData(new MeasurementDataNumeric(request, value));
            } else {
                log.debug("Missing column in query results - metric not collected: " + columnName);
            }
        }
    }

    public Connection getConnection() {
        return this.context.getParentResourceComponent().getConnection();
    }

    public void removeConnection() {
        this.context.getParentResourceComponent().removeConnection();
    }
}
