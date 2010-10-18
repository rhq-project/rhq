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
import java.sql.PreparedStatement;
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
import org.rhq.plugins.database.DatabaseComponent;
import org.rhq.plugins.database.DatabaseQueryUtility;

/**
 *
 * @author Steve Millidge (C2B2 Consulting Limited)
 */
public class MySqlUserComponent implements MeasurementFacet, DatabaseComponent {

    private String userName;
    private String host;
    private MySqlComponent parent;
    private Log log = LogFactory.getLog(this.getClass());
    private ResourceContext context;

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
        parent = (MySqlComponent)rc.getParentResourceComponent();
        context = rc;
        userName = context.getPluginConfiguration().getSimple("userName").getStringValue();
        host = context.getPluginConfiguration().getSimple("host").getStringValue();
    }

    @Override
    public void stop() {
    }


    public void getValues(MeasurementReport mr, Set<MeasurementScheduleRequest> requests) throws Exception {
        Connection conn = getConnection();
        ResultSet rs = null;
        Statement stmt = null;
        int activeConnections = 0;
        int totalConnections = 0;
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery("select User,Host,State from information_schema.processlist where User='"+userName+"'");
            while(rs.next()) {
                String hostVal = rs.getString(2);
                String state = rs.getString(3);
                if (hostVal.startsWith(host)) {
                    if (state.length() > 1) {
                        activeConnections ++;
                    }
                    totalConnections++;
                }
            }
        }catch(SQLException sqle) {

        } finally {
           DatabaseQueryUtility.close(stmt, rs);
        }

        for (MeasurementScheduleRequest request : requests) {
            if (request.getName().equals("TotalConnections")) {
                mr.addData(new MeasurementDataNumeric(request, new Double((double)totalConnections)));
            } else if (request.getName().equals("ActiveConnections")) {
                 mr.addData(new MeasurementDataNumeric(request, new Double((double)activeConnections)));
            }
        }
    }

    public AvailabilityType getAvailability() {
        AvailabilityType result = AvailabilityType.DOWN;
        Connection conn = getConnection();
        ResultSet rs = null;
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery("select User from mysql.user where User='"+userName+"' and Host='" + host +"'");
            if (rs.first()) {
                result = AvailabilityType.UP;
            }
        }catch(SQLException sqle) {

        } finally {
           DatabaseQueryUtility.close(stmt, rs);
        }
        return result;
    }

}
