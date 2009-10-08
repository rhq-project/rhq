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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.plugins.database.AbstractDatabaseComponent;
import org.rhq.plugins.database.DatabaseQueryUtility;

/**
 * @author Greg Hinkle
 */
public class OracleUserComponent extends AbstractDatabaseComponent implements MeasurementFacet {
    public AvailabilityType getAvailability() {
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            statement = getConnection().prepareStatement("SELECT COUNT(*) FROM DBA_USERS WHERE username = ?");
            statement.setString(1, this.resourceContext.getResourceKey());
            resultSet = statement.executeQuery();
            if (resultSet.next() && (resultSet.getInt(1) == 1)) {
                return AvailabilityType.UP;
            }
        } catch (SQLException e) {
            // Problems ? Mark the resource as down
        } finally {
            JDBCUtil.safeClose(statement, resultSet);
        }

        return AvailabilityType.DOWN;
    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
        for (MeasurementScheduleRequest request : metrics) {
            if (request.getName().equals("sessions")) {
                Map<String, Double> values = DatabaseQueryUtility
                    .getNumericQueryValues(this, "SELECT COUNT(*) as activeConnections FROM V$SESSION WHERE username = ?",
                        this.resourceContext.getResourceKey());
                Double sessions = values.get("count");
                if (sessions != null) {
                    report.addData(new MeasurementDataNumeric(request, sessions));
                }
            }
        }
    }
}