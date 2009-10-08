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
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.util.jdbc.JDBCUtil;

/**
 * The component for the {@link CustomTableDiscoveryComponent}.
 *
 * @author Greg Hinkle
 */
public class CustomTableComponent implements DatabaseComponent<DatabaseComponent>, MeasurementFacet {
    private ResourceContext<DatabaseComponent> context;

    public void start(ResourceContext<DatabaseComponent> resourceContext) throws InvalidPluginConfigurationException,
        Exception {
        this.context = resourceContext;
    }

    public void stop() {
    }

    public String getTable() {
        return this.context.getPluginConfiguration().getSimpleValue("table", null);
    }

    public AvailabilityType getAvailability() {
        Statement statement = null;
        try {
            statement = getConnection().createStatement();
            statement.executeQuery("SELECT COUNT(*) FROM " + getTable());
            return AvailabilityType.UP;
        } catch (SQLException e) {
            return AvailabilityType.DOWN;
        } finally {
            JDBCUtil.safeClose(statement);
        }
    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
        String query = this.context.getPluginConfiguration().getSimpleValue("metricQuery", null);

        Map<String, Double> values = DatabaseQueryUtility.getNumericQueryValueMap(this, query);
        for (MeasurementScheduleRequest request : metrics) {
            Double value = values.get(request.getName());
            if (value != null) {
                report.addData(new MeasurementDataNumeric(request, value));
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