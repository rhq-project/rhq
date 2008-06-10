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
package org.rhq.plugins.oracle;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.plugins.database.DatabaseComponent;
import org.rhq.plugins.database.DatabaseQueryUtility;

/**
 * @author Greg Hinkle
 */
public class OracleServerComponent implements DatabaseComponent, MeasurementFacet {
    private static final Log LOG = LogFactory.getLog(OracleServerComponent.class);

    private Connection connection;

    private ResourceContext resourceContext;

    public void start(ResourceContext resourceContext) throws InvalidPluginConfigurationException, Exception {
        this.resourceContext = resourceContext;
        this.connection = buildConnection(resourceContext.getPluginConfiguration());
    }

    public void stop() {
        if (this.connection != null) {
            try {
                this.connection.close();
            } catch (SQLException e) {
                LOG.debug("Unable to close oracle connection", e);
            }
        }
    }

    public AvailabilityType getAvailability() {
        if (getConnection() != null) {
            return AvailabilityType.UP;
        } else {
            return AvailabilityType.DOWN;
        }
    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
        Map<String, Double> values = DatabaseQueryUtility.getNumericQueryValueMap(this,
            "SELECT name, value FROM V$SYSSTAT");
        for (MeasurementScheduleRequest request : metrics) {
            if (request.getName().equals("totalSize")) {
                Double val = DatabaseQueryUtility.getSingleNumericQueryValue(this,
                    "SELECT SUM(bytes) FROM SYS.DBA_DATA_FILES");
                report.addData(new MeasurementDataNumeric(request, val));
            } else {
                Double value = values.get(request.getName());
                if (value != null) {
                    report.addData(new MeasurementDataNumeric(request, value));
                }
            }
        }
    }

    public Connection getConnection() {
        if (this.connection == null) {
            try {
                this.connection = buildConnection(this.resourceContext.getPluginConfiguration());
            } catch (SQLException e) {
                LOG.info("Unable to create oracle connection", e);
            }
        }

        return this.connection;
    }

    public void removeConnection() {
        this.connection = null;
    }

    public static Connection buildConnection(Configuration configuration) throws SQLException {
        String driverClass = configuration.getSimple("driverClass").getStringValue();
        try {
            Class.forName(driverClass);
        } catch (ClassNotFoundException e) {
            throw new InvalidPluginConfigurationException("Specified JDBC driver class (" + driverClass
                + ") not found.");
        }

        String url = buildUrl(configuration);
        LOG.debug("Attempting JDBC connection to [" + url + "]");

        String principal = configuration.getSimple("principal").getStringValue();
        String credentials = configuration.getSimple("credentials").getStringValue();

        Properties props = new Properties();
        props.put("user", principal);
        props.put("password", credentials);
        if (principal.equalsIgnoreCase("SYS")) {
            props.put("internal_logon", "sysdba");
        }

        return DriverManager.getConnection(url, props);
    }

    private static String buildUrl(Configuration configuration) {
        return "jdbc:oracle:thin:@" + configuration.getSimpleValue("host", "localhost") + ":"
            + configuration.getSimpleValue("port", "1521") + ":" + configuration.getSimpleValue("sid", "XE");
    }
}