/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License, version 2, as
 *  * published by the Free Software Foundation, and/or the GNU Lesser
 *  * General Public License, version 2.1, also as published by the Free
 *  * Software Foundation.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License and the GNU Lesser General Public License
 *  * for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * and the GNU Lesser General Public License along with this program;
 *  * if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package org.rhq.server.metrics;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.sql.DataSource;

import org.joda.time.DateTime;

import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.util.jdbc.JDBCUtil;

/**
 * @author John Sanda
 */
public class MetricsDAO {

    public static final String METRICS_INDEX_TABLE = "metrics_index";

    public static final String RAW_METRICS_TABLE = "raw_metrics";

    public static final String ONE_HOUR_METRICS_TABLE = "one_hour_metrics";

    public static final String SIX_HOUR_METRICS_TABLE = "six_hour_metrics";

    private static final String RAW_METRICS_QUERY =
        "SELECT schedule_id, time, value " +
        "FROM " + RAW_METRICS_TABLE + " " +
        "WHERE schedule_id = ? AND time >= ? AND time < ?";

    private static final String INSERT_RAW_METRICS =
        "INSERT INTO raw_metrics (schedule_id, time, value) VALUES (?, ?, ?)";

    private static final String METRICS_INDEX_QUERY =
        "SELECT time, schedule_id " +
        "FROM " + METRICS_INDEX_TABLE + " " +
        "WHERE bucket = ? " +
        "ORDER BY time";

    private static final String UPDATE_METRICS_INDEX =
        "INSERT INTO " + METRICS_INDEX_TABLE + " (bucket, time, schedule_id, null_col) VALUES (?, ?, ?, ?)";

    private static interface ConnectionCallback {
        void invoke(Connection connection);
    }

    private DataSource dataSource;

    public MetricsDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Map<Integer, DateTime> insertRawMetrics(Set<MeasurementDataNumeric> dataSet) {
        Map<Integer, DateTime> updates = new TreeMap<Integer, DateTime>();
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = dataSource.getConnection();
            statement = connection.prepareStatement(INSERT_RAW_METRICS);

            for (MeasurementDataNumeric data : dataSet) {
                statement.setInt(1, data.getScheduleId());
                statement.setDate(2, new java.sql.Date(data.getTimestamp()));
                statement.setDouble(3, data.getValue());
                statement.executeUpdate();

                updates.put(data.getScheduleId(), new DateTime(data.getTimestamp()).hourOfDay().roundFloorCopy());
            }

            return updates;
        } catch (SQLException e) {
            throw new CQLException(e);
        } finally {
            JDBCUtil.safeClose(statement);
            JDBCUtil.safeClose(connection);
        }
    }

    public Map<Integer, DateTime> insertAggregates(String bucket, List<AggregatedNumericMetric> metrics) {
        Map<Integer, DateTime> updates = new TreeMap<Integer, DateTime>();
        Connection connection = null;
        PreparedStatement statement = null;
        String sql = "INSERT INTO " + bucket + " (schedule_id, time, type, value) VALUES (?, ?, ?, ?)";
        try {
            connection = dataSource.getConnection();
            statement = connection.prepareStatement(sql);

            for (AggregatedNumericMetric metric : metrics) {
                statement.setInt(1, metric.getScheduleId());
                statement.setDate(2, new java.sql.Date(metric.getTimestamp()));
                statement.setInt(3, AggregateType.MIN.ordinal());
                statement.setDouble(4, metric.getMin());
                statement.executeUpdate();

                statement.setInt(1, metric.getScheduleId());
                statement.setDate(2, new java.sql.Date(metric.getTimestamp()));
                statement.setInt(3, AggregateType.MAX.ordinal());
                statement.setDouble(4, metric.getMax());
                statement.executeUpdate();

                statement.setInt(1, metric.getScheduleId());
                statement.setDate(2, new java.sql.Date(metric.getTimestamp()));
                statement.setInt(3, AggregateType.AVG.ordinal());
                statement.setDouble(4, metric.getAvg());
                statement.executeUpdate();

                updates.put(metric.getScheduleId(), new DateTime(metric.getTimestamp()));
            }

            return updates;
        } catch (SQLException e) {
            throw new CQLException(e);
        } finally {
            JDBCUtil.safeClose(statement);
            JDBCUtil.safeClose(connection);
        }
    }

    public List<RawNumericMetric> findRawMetrics(int scheduleId, DateTime startTime, DateTime endTime) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = dataSource.getConnection();
            statement = connection.prepareStatement(RAW_METRICS_QUERY);
            statement.setInt(1, scheduleId);
            statement.setDate(2, new java.sql.Date(startTime.getMillis()));
            statement.setDate(3, new java.sql.Date(endTime.getMillis()));

            resultSet = statement.executeQuery();
            List<RawNumericMetric> metrics = new ArrayList<RawNumericMetric>();
            ResultSetMapper<RawNumericMetric> resultSetMapper = new RawNumericMetricMapper();

            while (resultSet.next()) {
                metrics.add(resultSetMapper.map(resultSet));
            }
            return metrics;
        } catch (SQLException e) {
            throw new CQLException(e);
        } finally {
            JDBCUtil.safeClose(resultSet);
            JDBCUtil.safeClose(statement);
            JDBCUtil.safeClose(connection);
        }
    }

    public List<AggregatedNumericMetric> findAggregateMetrics(String bucket, int scheduleId) {
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;

        String sql =
            "SELECT schedule_id, time, type, value, ttl(value), writetime(value)" +
            "FROM " + bucket + " " +
            "WHERE schedule_id = " + scheduleId + " " +
            "ORDER BY time, type";

        try {
            connection = dataSource.getConnection();
            statement = connection.createStatement();
            resultSet = statement.executeQuery(sql);

            List<AggregatedNumericMetric> metrics = new ArrayList<AggregatedNumericMetric>();
            ResultSetMapper<AggregatedNumericMetric> resultSetMapper = new AggregateMetricMapper();

            while (resultSet.next()) {
                metrics.add(resultSetMapper.map(resultSet));
            }

            return metrics;
        } catch (SQLException e) {
            throw new CQLException(e);
        } finally {
            JDBCUtil.safeClose(resultSet);
            JDBCUtil.safeClose(statement);
            JDBCUtil.safeClose(connection);
        }
    }

    public List<MetricsIndexEntry> findMetricsIndexEntries(String bucket) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = dataSource.getConnection();
            statement = connection.prepareStatement(METRICS_INDEX_QUERY);
            statement.setString(1, bucket);
            resultSet = statement.executeQuery();
            List<MetricsIndexEntry> indexEntries = new ArrayList<MetricsIndexEntry>();
            ResultSetMapper<MetricsIndexEntry> resultSetMapper = new MetricsIndexResultSetMapper(bucket);

            while (resultSet.next()) {
                indexEntries.add(resultSetMapper.map(resultSet));
            }
            return indexEntries;
        } catch (SQLException e) {
            throw new CQLException(e);
        } finally {
            JDBCUtil.safeClose(resultSet);
            JDBCUtil.safeClose(statement);
            JDBCUtil.safeClose(connection);
        }
    }

    public void updateMetricsIndex(String bucket, Map<Integer, DateTime> updates) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = dataSource.getConnection();
            statement = connection.prepareStatement(UPDATE_METRICS_INDEX);
            for (Integer scheduleId : updates.keySet()) {
                try {
                    statement.setString(1, bucket);
                    statement.setDate(2, new java.sql.Date(updates.get(scheduleId).getMillis()));
                    statement.setInt(3, scheduleId);
                    statement.setBoolean(4, false);

                    statement.executeUpdate();
                } catch (SQLException e) {
//                    log.warn("Failed to update " + columnFamily + " index for " + scheduleId + " at time slice " +
//                        updates.get(scheduleId));
                }
            }
        } catch (SQLException e) {
            throw new CQLException(e);
        } finally {
            JDBCUtil.safeClose(statement);
            JDBCUtil.safeClose(connection);
        }
    }

    private void execute(ConnectionCallback callback) {
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            callback.invoke(connection);
        } catch(SQLException e) {
            throw new CQLException(e);
        } finally {
            JDBCUtil.safeClose(connection);
        }
    }
}
