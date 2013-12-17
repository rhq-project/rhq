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

package org.rhq.plugins.oracle;

import static org.rhq.plugins.database.DatabasePluginUtil.getConnectionFromComponent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.plugins.database.AbstractDatabaseComponent;
import org.rhq.plugins.database.DatabasePluginUtil;

/**
 * Oracle ASM Disk Group Component.
 * 
 * @author Richard Hensman
 */
@SuppressWarnings("rawtypes")
public class OracleAsmDiskGroupComponent extends AbstractDatabaseComponent implements MeasurementFacet {
    private static final Log LOG = LogFactory.getLog(OracleAsmDiskGroupComponent.class);

    private static final String SQL_AVAILABLE = "SELECT COUNT(*) FROM v$asm_diskgroup WHERE group_number = ? and STATE <> 'BROKEN'";
    private static final String SQL_VALUES = "SELECT GROUP_NUMBER, " + "NAME, " + "SECTOR_SIZE sectorSize, "
        + "BLOCK_SIZE blockSize, " + "ALLOCATION_UNIT_SIZE allocationUnitSize, " + "STATE state, " + "TYPE type, "
        + "TOTAL_MB totalMb, " + "FREE_MB freeMb, " + "((TOTAL_MB-FREE_MB)/TOTAL_MB) usedPercent, "
        + "REQUIRED_MIRROR_FREE_MB requiredMirrorFreeMb, " + "USABLE_FILE_MB usableFileMb, "
        + "OFFLINE_DISKS offlineDisks, " + "COMPATIBILITY compatibility, "
        + "DATABASE_COMPATIBILITY databaseCompatibility " + "FROM v$asm_diskgroup WHERE group_number = ?";

    public AvailabilityType getAvailability() {
        Connection jdbcConnection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            jdbcConnection = getConnectionFromComponent(this);
            statement = jdbcConnection.prepareStatement(SQL_AVAILABLE);
            statement.setString(1, this.resourceContext.getResourceKey());
            resultSet = statement.executeQuery();
            if (resultSet.next() && (resultSet.getInt(1) == 1)) {
                return AvailabilityType.UP;
            }
        } catch (SQLException e) {
            LOG.debug("unable to query", e);
        } finally {
            DatabasePluginUtil.safeClose(null, statement, resultSet);
            if (supportsConnectionPooling()) {
                DatabasePluginUtil.safeClose(jdbcConnection);
            }
        }

        return AvailabilityType.DOWN;
    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
        Connection jdbcConnection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            jdbcConnection = getConnectionFromComponent(this);
            statement = jdbcConnection.prepareStatement(SQL_VALUES);
            statement.setString(1, this.resourceContext.getResourceKey());
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                for (MeasurementScheduleRequest request : metrics) {
                    String name = request.getName().toUpperCase(Locale.US);
                    if (request.getDataType().equals(DataType.TRAIT)) {
                        report.addData(new MeasurementDataTrait(request, resultSet.getString(name)));
                    } else {
                        try {
                            report.addData(new MeasurementDataNumeric(request, resultSet.getDouble(name)));
                        } catch (SQLException e) {
                            // Ignoring metrics that cannot be read as a double
                            LOG.warn("Ignoring metric " + name + " as it cannot be read as a double");
                        }
                    }
                }
            }
        } catch (SQLException e) {
            LOG.debug("Unable to read value", e);
        } finally {
            DatabasePluginUtil.safeClose(null, statement, resultSet);
            if (supportsConnectionPooling()) {
                DatabasePluginUtil.safeClose(jdbcConnection);
            }
        }
    }
}
