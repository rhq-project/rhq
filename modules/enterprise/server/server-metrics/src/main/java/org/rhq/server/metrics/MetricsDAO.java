/*
 *
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package org.rhq.server.metrics;


import static com.datastax.driver.core.querybuilder.QueryBuilder.batch;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.ttl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.exceptions.NoHostAvailableException;

import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.AggregateNumericMetricMapper;
import org.rhq.server.metrics.domain.AggregateSimpleNumericMetric;
import org.rhq.server.metrics.domain.AggregateSimpleNumericMetricMapper;
import org.rhq.server.metrics.domain.AggregateType;
import org.rhq.server.metrics.domain.ListPagedResult;
import org.rhq.server.metrics.domain.MetricsIndexEntry;
import org.rhq.server.metrics.domain.MetricsIndexEntryMapper;
import org.rhq.server.metrics.domain.MetricsTable;
import org.rhq.server.metrics.domain.RawNumericMetric;
import org.rhq.server.metrics.domain.RawNumericMetricMapper;
import org.rhq.server.metrics.domain.SimplePagedResult;


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

    public List<AggregateNumericMetric> insertAggregates(MetricsTable table,
        List<AggregateNumericMetric> metrics, int ttl) {
        List<AggregateNumericMetric> updates = new ArrayList<AggregateNumericMetric>();

        if (metrics.isEmpty()) {
            return updates;
        }

        try {
            Statement[] statements = new Statement[metrics.size() * 3];
            int i = 0;

            for (AggregateNumericMetric metric : metrics) {
                statements[i++] = insertInto(table.getTableName())
                    .value("schedule_id", metric.getScheduleId())
                    .value("time", new Date(metric.getTimestamp()))
                    .value("type", AggregateType.MIN.ordinal())
                    .value("value", metric.getMin())
                    .using(ttl(ttl));

                statements[i++] = insertInto(table.getTableName())
                    .value("schedule_id", metric.getScheduleId())
                    .value("time", new Date(metric.getTimestamp()))
                    .value("type", AggregateType.MAX.ordinal())
                    .value("value", metric.getMax())
                    .using(ttl(ttl));

                statements[i++] = insertInto(table.getTableName())
                    .value("schedule_id", metric.getScheduleId())
                    .value("time", new Date(metric.getTimestamp()))
                    .value("type", AggregateType.AVG.ordinal())
                    .value("value", metric.getAvg())
                    .using(ttl(ttl));

                updates.add(metric);
            }
            session.execute(batch(statements));

            return updates;
        } catch (NoHostAvailableException e) {
            throw new CQLException(e);
        }
    }

    public List<MetricResultFuture<AggregateNumericMetric>> insertAggregatesAsync(MetricsTable table,
        List<AggregateNumericMetric> metrics, int ttl) {
        List<MetricResultFuture<AggregateNumericMetric>> updates = new ArrayList<MetricResultFuture<AggregateNumericMetric>>();

        if (metrics.isEmpty()) {
            return updates;
        }

        try {
            Statement statement = null;

            for (AggregateNumericMetric metric : metrics) {
                statement = insertInto(table.getTableName())
                    .value("schedule_id", metric.getScheduleId())
                    .value("time", new Date(metric.getTimestamp()))
                    .value("type", AggregateType.MIN.ordinal())
                    .value("value", metric.getMin());
                updates.add(new MetricResultFuture<AggregateNumericMetric>(session.executeAsync(statement), metric));

                statement = insertInto(table.getTableName())
                    .value("schedule_id", metric.getScheduleId())
                    .value("time", new Date(metric.getTimestamp()))
                    .value("type", AggregateType.MAX.ordinal())
                    .value("value", metric.getMax());
                updates.add(new MetricResultFuture<AggregateNumericMetric>(session.executeAsync(statement), metric));

                statement = insertInto(table.getTableName())
                    .value("schedule_id", metric.getScheduleId())
                    .value("time", new Date(metric.getTimestamp()))
                    .value("type", AggregateType.AVG.ordinal())
                    .value("value", metric.getAvg());
                updates.add(new MetricResultFuture<AggregateNumericMetric>(session.executeAsync(statement), metric));
            }

            return updates;
        } catch (Exception e) {
            throw new CQLException(e);
        }
    }

    public Iterable<RawNumericMetric> findRawMetrics(int scheduleId, long startTime, long endTime) {
        try {
            PreparedStatement statement = session.prepare(RAW_METRICS_QUERY);
            BoundStatement boundStatement = statement.bind(scheduleId, new Date(startTime), new Date(endTime));

            return new SimplePagedResult<RawNumericMetric>(boundStatement, new RawNumericMetricMapper(false), session);
        } catch (NoHostAvailableException e) {
            throw new CQLException(e);
        }
    }

    public Iterable<RawNumericMetric> findRawMetrics(int scheduleId, PageOrdering ordering, int limit) {
        try {
            String cql = RAW_METRICS_SIMPLE_QUERY + " " + ordering;
            if (limit > 0) {
                cql += " LIMIT " + limit;
            }
            PreparedStatement statement = session.prepare(cql);
            BoundStatement boundStatement = statement.bind(scheduleId);

            return new SimplePagedResult<RawNumericMetric>(boundStatement, new RawNumericMetricMapper(false), session);
        } catch (NoHostAvailableException e) {
            throw new CQLException(e);
        }
    }

    public Iterable<RawNumericMetric> findRawMetrics(int scheduleId, long startTime, long endTime,
        boolean includeMetadata) {

        if (!includeMetadata) {
            return findRawMetrics(scheduleId, startTime, endTime);
        }

        try {
            PreparedStatement statement = session.prepare(RAW_METRICS_WITH_METADATA_QUERY);
            BoundStatement boundStatement = statement.bind(scheduleId, new Date(startTime), new Date(endTime));

            return new SimplePagedResult<RawNumericMetric>(boundStatement, new RawNumericMetricMapper(true), session);
        } catch (NoHostAvailableException e) {
            throw new CQLException(e);
        }
    }

    public Iterable<RawNumericMetric> findRawMetrics(List<Integer> scheduleIds, long startTime, long endTime) {
        String cql = "SELECT schedule_id, time, value " +
                " FROM " + MetricsTable.RAW +
                " WHERE schedule_id = ? AND time >= " + startTime + " AND time <= " + endTime;

        return new ListPagedResult<RawNumericMetric>(cql, scheduleIds, new RawNumericMetricMapper(), session);
    }

    public Iterable<AggregateNumericMetric> findAggregateMetrics(MetricsTable table, int scheduleId) {
        try {
            String cql =
                "SELECT schedule_id, time, type, value " +
                "FROM " + table + " " +
                "WHERE schedule_id = ? " +
                "ORDER BY time, type";
            PreparedStatement statement = session.prepare(cql);
            BoundStatement boundStatement = statement.bind(scheduleId);

            return new SimplePagedResult<AggregateNumericMetric>(boundStatement, new AggregateNumericMetricMapper(), session);
        } catch (NoHostAvailableException e) {
            throw new CQLException(e);
        }
    }

    public Iterable<AggregateNumericMetric> findAggregateMetrics(MetricsTable table, int scheduleId, long startTime,
        long endTime) {

        try {
            String cql =
                "SELECT schedule_id, time, type, value " +
                "FROM " + table + " " +
                "WHERE schedule_id = ? AND time >= ? AND time < ?";
            PreparedStatement statement = session.prepare(cql);
            BoundStatement boundStatement = statement.bind(scheduleId, new Date(startTime), new Date(endTime));

            return new SimplePagedResult<AggregateNumericMetric>(boundStatement, new AggregateNumericMetricMapper(), session);
        } catch (NoHostAvailableException e) {
            throw new CQLException(e);
        }
    }

    public Iterable<AggregateSimpleNumericMetric> findAggregateSimpleMetrics(MetricsTable table, int scheduleId,
        long startTime, long endTime) {
        try {
            String cql = "SELECT schedule_id, type, value " + "FROM " + table
                + " WHERE schedule_id = ? AND time >= ? AND time < ?";
            PreparedStatement statement = session.prepare(cql);
            BoundStatement boundStatement = statement.bind(scheduleId, new Date(startTime), new Date(endTime));

            return new SimplePagedResult<AggregateSimpleNumericMetric>(boundStatement,
                new AggregateSimpleNumericMetricMapper(), session);
        } catch (NoHostAvailableException e) {
            throw new CQLException(e);
        }
    }

    public Iterable<AggregateNumericMetric> findAggregateMetrics(MetricsTable table, List<Integer> scheduleIds,
        long startTime, long endTime) {
        String cql =
                "SELECT schedule_id, time, type, value "+
                "FROM " + table + " " +
                "WHERE schedule_id = ? AND time >= " + startTime + " AND time < " + endTime;

        return new ListPagedResult<AggregateNumericMetric>(cql, scheduleIds, new AggregateNumericMetricMapper(), session);
    }

    public Iterable<AggregateNumericMetric> findAggregateMetricsWithMetadata(MetricsTable table, int scheduleId,
        long startTime, long endTime) {

        try {
            String cql =
                "SELECT schedule_id, time, type, value, ttl(value), writetime(value) " +
                "FROM " + table + " " +
                "WHERE schedule_id = ? AND time >= ? AND time < ?";
            PreparedStatement statement = session.prepare(cql);
            BoundStatement boundStatement = statement.bind(scheduleId, new Date(startTime), new Date(endTime));

            return new SimplePagedResult<AggregateNumericMetric>(boundStatement, new AggregateNumericMetricMapper(true),
                session);
        } catch (NoHostAvailableException e) {
            throw new CQLException(e);
        }
    }

    public Iterable<MetricsIndexEntry> findMetricsIndexEntries(final MetricsTable table) {
        /*
          String query = "SELECT time, schedule_id " + "FROM " + MetricsTable.INDEX + " " + "WHERE bucket =  " + table
            + " AND time >=0 ? AND time < ? " + "ORDER BY time";

        ResultSetMapper<MetricsIndexEntry> resultSetMapper = new MetricsIndexResultSetMapper(table);
        QueryCreator<MetricsIndexEntry> queryCreator = new QueryCreator<MetricsIndexEntry>() {

            @Override
            public String buildInitialQuery() {
                String query = "SELECT time, schedule_id " +
                               " FROM " + MetricsTable.INDEX + " " +
                               " WHERE bucket =  " + table +
                               " LIMIT 30000 ";
                return query;
            }

            @Override
            public String buildNextQuery(MetricsIndexEntry object) {
                String query = "SELECT time, schedule_id " +
                    " FROM " + MetricsTable.INDEX + " " +
                    " WHERE bucket =  " + table + " AND time >= " + object.getTime().getMillis() + " AND schedule_id >= " + object.getScheduleId()+
                    " LIMIT 30000 ";
                return query;
            }
        };
        SlicedPagedResult<MetricsIndexEntry> result = new SlicedPagedResult<MetricsIndexEntry>(queryCreator,
            resultSetMapper, session);
         */

        try {
            PreparedStatement statement = session.prepare(METRICS_INDEX_QUERY);
            BoundStatement boundStatement = statement.bind(table.toString());

            return new SimplePagedResult<MetricsIndexEntry>(boundStatement, new MetricsIndexEntryMapper(table), session);
        } catch (NoHostAvailableException e) {
            throw new CQLException(e);
        }
    }

    public void updateMetricsIndex(MetricsTable table, Map<Integer, Long> updates) {
        try {
            Statement[] statements = new Statement[updates.size()];
            int i = 0;
            for (Integer scheduleId : updates.keySet()) {
                statements[i++] = insertInto(MetricsTable.INDEX.toString())
                    .value("bucket", table.getTableName())
                    .value("time", new Date(updates.get(scheduleId)))
                    .value("schedule_id", scheduleId)
                    .value("null_col", false);
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
