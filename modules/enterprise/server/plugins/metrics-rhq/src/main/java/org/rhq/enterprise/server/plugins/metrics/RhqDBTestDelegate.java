/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation version 2 of the License.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program; if not, write to the Free Software
 *  * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

package org.rhq.enterprise.server.plugins.metrics;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.rhq.enterprise.server.plugin.pc.metrics.AggregateTestData;
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.enterprise.server.measurement.util.MeasurementDataManagerUtility;
import org.rhq.enterprise.server.plugin.pc.metrics.MetricsServerPluginException;
import org.rhq.enterprise.server.plugin.pc.metrics.MetricsServerPluginTestDelegate;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author John Sanda
 */
public class RhqDBTestDelegate implements MetricsServerPluginTestDelegate {

    @Override
    public void purgeRawData() {
        purgeTables(MeasurementDataManagerUtility.getAllRawTables());
    }

    @Override
    public void purge1HourData() {
        purgeTables("rhq_measurement_data_num_1h");
    }

    @Override
    public void purge6HourData() {
        purgeTables("rhq_measurement_data_num_6h");
    }

    @Override
    public void purge24HourData() {
        purgeTables("rhq_measurement_data_num_24h");
    }

    @Override
    public void insert1HourData(List<AggregateTestData> data) {
        Connection connection = getConnection();

        try {
            connection.setAutoCommit(false);
            String sql = "insert into rhq_measurement_data_num_1h(time_stamp, schedule_id, value, minvalue, maxvalue) values(?, ?, ?, ?, ?)";
            PreparedStatement statement = connection.prepareStatement(sql);

            for (AggregateTestData datum : data) {
                statement.setLong(1, datum.getTimestamp());
                statement.setInt(2, datum.getScheduleId());
                statement.setDouble(3, datum.getAvg());
                statement.setDouble(4, datum.getMin());
                statement.setDouble(5, datum.getMax());

                statement.addBatch();
            }

            statement.executeBatch();
            connection.commit();
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException e1) {
                throw new MetricsServerPluginException("Failed to rollback transaction", e1);
            }
            throw new MetricsServerPluginException("Failed to insert 1 hour data", e);
        } finally {
            JDBCUtil.safeClose(connection);
        }
    }

    private void purgeTables(String... tables) {
        // This method was previous implemented using EntityManager.createNativeQuery
        // and called from within a TransactionCallback. It was causing a
        // TransactionRequiredException, and I am not clear why. I suspect it is a
        // configuration issue in our testing environment, but I haven't figured it out
        // yet. For now,  raw tables are purges in their own separate JDBC transaction.
        //
        // jsanda
        Connection connection = getConnection();

        try {
            connection.setAutoCommit(false);
            for (String table : tables) {
                Statement statement = connection.createStatement();
                try {
                    statement.execute("delete from " + table);
                } finally {
                    JDBCUtil.safeClose(statement);
                }
            }
            connection.commit();
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException e1) {
                throw new MetricsServerPluginException("Failed to rollback transaction", e1);
            }
            throw new MetricsServerPluginException("Failed to purge data from " + tables, e);
        } finally {
            JDBCUtil.safeClose(connection);
        }
    }

    private Connection getConnection() {
        try {
            return LookupUtil.getDataSource().getConnection();
        } catch (SQLException e) {
            throw new MetricsServerPluginException("Failed to get DataSource connection", e);
        }
    }

}
