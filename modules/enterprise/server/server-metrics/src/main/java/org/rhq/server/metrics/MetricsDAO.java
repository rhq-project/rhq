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

import static com.datastax.driver.core.utils.querybuilder.QueryBuilder.batch;
import static com.datastax.driver.core.utils.querybuilder.QueryBuilder.insert;
import static org.rhq.core.util.StringUtil.listToString;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.datastax.driver.core.utils.querybuilder.Using;

import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.util.PageOrdering;

/**
 * @author John Sanda
 */
public class MetricsDAO {

    // It looks like that there might be a bug in the DataStax driver that will prevent our
    // using prepared statements for range queries. See https://github.com/datastax/java-driver/issues/3
    // for details.
    //
    // jsanda

    private static final String RAW_METRICS_SIMPLE_QUERY =
        "SELECT schedule_id, time, value " +
        "FROM " + MetricsTable.RAW + " " +
        "WHERE schedule_id = ? ORDER by time";

    private static final String RAW_METRICS_QUERY =
        "SELECT schedule_id, time, value " +
        "FROM " + MetricsTable.RAW + " " +
        "WHERE schedule_id = ? AND time >= ? AND time < ? ORDER BY time";

    private static final String RAW_METRICS_SCHEDULE_LIST_QUERY =
        "SELECT schedule_id, time, value " +
        "FROM " + MetricsTable.RAW + " " +
        "WHERE schedule_id IN (?) AND time >= ? AND time < ? ORDER BY time";

    private static final String RAW_METRICS_WITH_METADATA_QUERY =
        "SELECT schedule_id, time, value, ttl(value), writetime(value) " +
            "FROM " + MetricsTable.RAW + " " +
            "WHERE schedule_id = ? AND time >= ? AND time < ?";

    private static final String INSERT_RAW_METRICS =
        "INSERT INTO "+ MetricsTable.RAW +" (schedule_id, time, value) " +
        "VALUES (?, ?, ?) USING TTL ? AND TIMESTAMP ?";

    private static final String METRICS_INDEX_QUERY =
        "SELECT time, schedule_id " +
        "FROM " + MetricsTable.INDEX + " " +
        "WHERE bucket = ? " +
        "ORDER BY time";

    private static final String UPDATE_METRICS_INDEX =
        "INSERT INTO " + MetricsTable.INDEX + " (bucket, time, schedule_id, null_col) VALUES (?, ?, ?, ?)";

    private Session session;

    public MetricsDAO(Session session) {
        this.session = session;
    }

    public Set<MeasurementDataNumeric> insertRawMetrics(Set<MeasurementDataNumeric> dataSet, int ttl) {
        // TODO Determine if batch inserts will be faster that prepared statements for raw data
        try {
            String cql = "INSERT INTO raw_metrics (schedule_id, time, value) VALUES (?, ?, ?) " + "USING TTL " + ttl;
            PreparedStatement statement = session.prepare(cql);

            Set<MeasurementDataNumeric> insertedMetrics = new HashSet<MeasurementDataNumeric>();
            for (MeasurementDataNumeric data : dataSet) {
                BoundStatement boundStatement = statement.bind(data.getScheduleId(), new Date(data.getTimestamp()),
                    data.getValue());
                session.execute(boundStatement);
                insertedMetrics.add(data);
            }

            return insertedMetrics;
        } catch (NoHostAvailableException e) {
            throw new CQLException(e);
        }
    }

    public List<MetricResultFuture<MeasurementDataNumeric>> insertRawMetricsAsync(Set<MeasurementDataNumeric> dataSet,
        int ttl) {
        try {
            List<MetricResultFuture<MeasurementDataNumeric>> resultFutures = new ArrayList<MetricResultFuture<MeasurementDataNumeric>>();

            String cql = "INSERT INTO raw_metrics (schedule_id, time, value) VALUES (?, ?, ?) " + "USING TTL " + ttl;
            PreparedStatement statement = session.prepare(cql);

            for (MeasurementDataNumeric data : dataSet) {
                BoundStatement boundStatement = statement.bind(data.getScheduleId(), new Date(data.getTimestamp()),
                    data.getValue());

                resultFutures.add(new MetricResultFuture<MeasurementDataNumeric>(session.executeAsync(boundStatement),
                    data));
            }

            return resultFutures;
        } catch (NoHostAvailableException e) {
            throw new CQLException(e);
        }
    }

    public List<AggregatedNumericMetric> insertAggregates(MetricsTable table,
        List<AggregatedNumericMetric> metrics, int ttl) {
        List<AggregatedNumericMetric> updates = new ArrayList<AggregatedNumericMetric>();

        if (metrics.isEmpty()) {
            return updates;
        }

        try {
            Statement[] statements = new Statement[metrics.size() * 3];
            int i = 0;

            for (AggregatedNumericMetric metric : metrics) {
                statements[i++] = insert("schedule_id", "time", "type", "value")
                    .into(table.getTableName())
                    .values(metric.getScheduleId(), new Date(metric.getTimestamp()), AggregateType.MIN.ordinal(),
                        metric.getMin()).using(Using.ttl(ttl));

                statements[i++] = insert("schedule_id", "time", "type", "value")
                    .into(table.getTableName())
                    .values(metric.getScheduleId(), new Date(metric.getTimestamp()), AggregateType.MAX.ordinal(),
                        metric.getMax()).using(Using.ttl(ttl));

                statements[i++] = insert("schedule_id", "time", "type", "value")
                    .into(table.getTableName())
                    .values(metric.getScheduleId(), new Date(metric.getTimestamp()), AggregateType.AVG.ordinal(),
                        metric.getAvg()).using(Using.ttl(ttl));

                updates.add(metric);
            }
            session.execute(batch(statements));

            return updates;
        } catch (NoHostAvailableException e) {
            throw new CQLException(e);
        }
    }

    public List<MetricResultFuture<AggregatedNumericMetric>> insertAggregatesAsync(MetricsTable table,
        List<AggregatedNumericMetric> metrics, int ttl) {
        List<MetricResultFuture<AggregatedNumericMetric>> updates = new ArrayList<MetricResultFuture<AggregatedNumericMetric>>();

        if (metrics.isEmpty()) {
            return updates;
        }

        try {
            Statement statement = null;

            for (AggregatedNumericMetric metric : metrics) {
                statement = insert("schedule_id", "time", "type", "value")
                    .into(table.getTableName())
                    .values(metric.getScheduleId(), new Date(metric.getTimestamp()), AggregateType.MIN.ordinal(),
                        metric.getMin()).using(Using.ttl(ttl));
                updates.add(new MetricResultFuture<AggregatedNumericMetric>(session.executeAsync(statement), metric));

                statement = insert("schedule_id", "time", "type", "value")
                    .into(table.getTableName())
                    .values(metric.getScheduleId(), new Date(metric.getTimestamp()), AggregateType.MAX.ordinal(),
                        metric.getMax()).using(Using.ttl(ttl));
                updates.add(new MetricResultFuture<AggregatedNumericMetric>(session.executeAsync(statement), metric));

                statement = insert("schedule_id", "time", "type", "value")
                    .into(table.getTableName())
                    .values(metric.getScheduleId(), new Date(metric.getTimestamp()), AggregateType.AVG.ordinal(),
                        metric.getAvg()).using(Using.ttl(ttl));
                updates.add(new MetricResultFuture<AggregatedNumericMetric>(session.executeAsync(statement), metric));
            }

            return updates;
        } catch (Exception e) {
            throw new CQLException(e);
        }
    }

    public List<RawNumericMetric> findRawMetrics(int scheduleId, long startTime, long endTime) {
        try {
            PreparedStatement statement = session.prepare(RAW_METRICS_QUERY);
            BoundStatement boundStatement = statement.bind(scheduleId, new Date(startTime), new Date(endTime));
            ResultSet resultSet = session.execute(boundStatement);

            ResultSetMapper<RawNumericMetric> resultSetMapper = new RawNumericMetricMapper();
            return resultSetMapper.mapAll(resultSet);
        } catch (NoHostAvailableException e) {
            throw new CQLException(e);
        }
    }

    public PagedResultSet<RawNumericMetric> findRawMetricsPaged(int scheduleId, long startTime, long endTime) {
        try {
            PreparedStatement statement = session.prepare(RAW_METRICS_QUERY);
            BoundStatement boundStatement = statement.bind(scheduleId, new Date(startTime), new Date(endTime));
            ResultSet resultSet = session.execute(boundStatement);

            PagedResultSet<RawNumericMetric> pagedResultSet = new PagedResultSet<RawNumericMetric>(resultSet,
                new RawNumericMetricMapper());
            return pagedResultSet;
        } catch (NoHostAvailableException e) {
            throw new CQLException(e);
        }
    }

    public List<RawNumericMetric> findRawMetrics(int scheduleId,  PageOrdering ordering, int limit) {
        try {
            String cql = RAW_METRICS_SIMPLE_QUERY + " " + ordering;
            if (limit > 0) {
                cql += " LIMIT " + limit;
            }
            PreparedStatement statement = session.prepare(cql);
            BoundStatement boundStatement = statement.bind(scheduleId);
            ResultSet resultSet = session.execute(boundStatement);

            ResultSetMapper<RawNumericMetric> resultSetMapper = new RawNumericMetricMapper();
            return resultSetMapper.mapAll(resultSet);
        } catch (NoHostAvailableException e) {
            throw new CQLException(e);
        }
    }

    public List<RawNumericMetric> findRawMetrics(int scheduleId, long startTime, long endTime,
        boolean includeMetadata) {

        if (!includeMetadata) {
            return findRawMetrics(scheduleId, startTime, endTime);
        }

        try {
            PreparedStatement statement = session.prepare(RAW_METRICS_WITH_METADATA_QUERY);
            BoundStatement boundStatement = statement.bind(scheduleId, new Date(startTime), new Date(endTime));
            ResultSet resultSet = session.execute(boundStatement);

            ResultSetMapper<RawNumericMetric> resultSetMapper = new RawNumericMetricMapper(true);
            return resultSetMapper.mapAll(resultSet);
        } catch (NoHostAvailableException e) {
            throw new CQLException(e);
        }
    }

    public List<RawNumericMetric> findRawMetrics(List<Integer> scheduleIds, long startTime, long endTime) {
        try {
            // TODO investigate possible bug in datastax driver for binding lists as parameters
            // I was not able to get the below query working by directly binding the List
            // object. From a quick glance at the driver code, it looks like it might not
            // yet be properly supported in which case we need to report a bug.
            // jsanda

            //PreparedStatement statement = session.prepare(RAW_METRICS_SCHEDULE_LIST_QUERY);
            //BoundStatement boundStatement = statement.bind(scheduleIds, startTime, endTime);
            //ResultSet resultSet = session.execute(boundStatement);
            String cql = "SELECT schedule_id, time, value FROM " + MetricsTable.RAW + " WHERE schedule_id IN ("
                + listToString(scheduleIds) + ") AND time >= " + startTime + " AND time <= " + endTime;
            ResultSet resultSet = session.execute(cql);

            ResultSetMapper<RawNumericMetric> resultSetMapper = new RawNumericMetricMapper(false);
            return resultSetMapper.mapAll(resultSet);
        } catch (NoHostAvailableException e) {
            throw new CQLException(e);
        }
    }

    public List<AggregatedNumericMetric> findAggregateMetrics(MetricsTable table, int scheduleId) {
        try {
            String cql =
                "SELECT schedule_id, time, type, value " +
                "FROM " + table + " " +
                "WHERE schedule_id = ? " +
                "ORDER BY time, type";
            PreparedStatement statement = session.prepare(cql);
            BoundStatement boundStatement = statement.bind(scheduleId);
            ResultSet resultSet = session.execute(boundStatement);

            ResultSetMapper<AggregatedNumericMetric> resultSetMapper = new AggregateMetricMapper();
            return resultSetMapper.mapAll(resultSet);
        } catch (NoHostAvailableException e) {
            throw new CQLException(e);
        }
    }

    public List<AggregatedNumericMetric> findAggregateMetrics(MetricsTable table, int scheduleId, long startTime,
        long endTime) {

        try {
            String cql =
                "SELECT schedule_id, time, type, value " +
                "FROM " + table + " " +
                "WHERE schedule_id = ? AND time >= ? AND time < ?";
            PreparedStatement statement = session.prepare(cql);
            BoundStatement boundStatement = statement.bind(scheduleId, new Date(startTime), new Date(endTime));
            ResultSet resultSet = session.execute(boundStatement);

            ResultSetMapper<AggregatedNumericMetric> resultSetMapper = new AggregateMetricMapper();
            return resultSetMapper.mapAll(resultSet);
        } catch (NoHostAvailableException e) {
            throw new CQLException(e);
        }
    }

    public List<AggregatedSimpleNumericMetric> findAggregateSimpleMetrics(MetricsTable table, int scheduleId,
        long startTime, long endTime) {
        try {
            String cql = "SELECT type, value " + "FROM " + table + " WHERE schedule_id = ? AND time >= ? AND time < ?";
            PreparedStatement statement = session.prepare(cql);
            BoundStatement boundStatement = statement.bind(scheduleId, new Date(startTime), new Date(endTime));
            ResultSet resultSet = session.execute(boundStatement);

            List<AggregatedSimpleNumericMetric> metrics = new ArrayList<AggregatedSimpleNumericMetric>();
            while (!resultSet.isExhausted()) {
                Row row = resultSet.fetchOne();
                metrics.add(new AggregatedSimpleNumericMetric(row.getDouble(1), AggregateType.valueOf(row.getInt(0))));
            }

            return metrics;
        } catch (NoHostAvailableException e) {
            throw new CQLException(e);
        }
    }

    public List<AggregatedNumericMetric> findAggregateMetrics(MetricsTable table, List<Integer> scheduleIds,
        long startTime, long endTime) {
        try {
            String cql =
                "SELECT schedule_id, time, type, value FROM " + table + " " +
                    "WHERE schedule_id IN (" + listToString(scheduleIds) + ") AND time >= " + startTime + " AND time < " + endTime;
            ResultSet resultSet = session.execute(cql);

            ResultSetMapper<AggregatedNumericMetric> resultSetMapper = new AggregateMetricMapper();
            return resultSetMapper.mapAll(resultSet);
        } catch (NoHostAvailableException e) {
            throw new CQLException(e);
        }
    }

    public List<AggregatedNumericMetric> findAggregateMetricsWithMetadata(MetricsTable table, int scheduleId,
        long startTime, long endTime) {

        try {
            String cql =
                "SELECT schedule_id, time, type, value, ttl(value), writetime(value) " +
                "FROM " + table + " " +
                "WHERE schedule_id = ? AND time >= ? AND time < ?";
            PreparedStatement statement = session.prepare(cql);
            BoundStatement boundStatement = statement.bind(scheduleId, new Date(startTime), new Date(endTime));
            ResultSet resultSet = session.execute(boundStatement);

            ResultSetMapper<AggregatedNumericMetric> resultSetMapper = new AggregateMetricMapper(true);
            return resultSetMapper.mapAll(resultSet);
        } catch (NoHostAvailableException e) {
            throw new CQLException(e);
        }
    }

    public List<MetricsIndexEntry> findMetricsIndexEntries(MetricsTable table) {
        try {
            PreparedStatement statement = session.prepare(METRICS_INDEX_QUERY);
            BoundStatement boundStatement = statement.bind(table.toString());
            ResultSet resultSet = session.execute(boundStatement);

            ResultSetMapper<MetricsIndexEntry> resultSetMapper = new MetricsIndexResultSetMapper(table);
            return resultSetMapper.mapAll(resultSet);
        } catch (NoHostAvailableException e) {
            throw new CQLException(e);
        }
    }

    public void updateMetricsIndex(MetricsTable table, Map<Integer, Long> updates) {
        try {
            Statement[] statements = new Statement[updates.size()];
            int i = 0;
            for (Integer scheduleId : updates.keySet()) {
                statements[i++] =
                    insert("bucket", "time", "schedule_id", "null_col")
                    .into(MetricsTable.INDEX.getTableName())
                     .values(table.toString(), new Date(updates.get(scheduleId)), scheduleId, false);
            }
            session.execute(batch(statements));
        } catch (NoHostAvailableException e) {
            throw new CQLException(e);
        }
    }

    public void deleteMetricsIndexEntries(MetricsTable table) {
        try {
            String cql = "DELETE FROM " + MetricsTable.INDEX + " WHERE bucket = '" + table + "'";
            session.execute(cql);
        } catch (NoHostAvailableException e) {
            throw new CQLException(e);
        }
    }
}
