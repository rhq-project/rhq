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

import static com.datastax.driver.core.utils.querybuilder.Clause.eq;
import static com.datastax.driver.core.utils.querybuilder.Clause.gte;
import static com.datastax.driver.core.utils.querybuilder.Clause.lt;
import static com.datastax.driver.core.utils.querybuilder.QueryBuilder.select;
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
import com.datastax.driver.core.exceptions.NoHostAvailableException;

import org.joda.time.DateTime;

import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.util.PageOrdering;

/**
 * @author John Sanda
 */
public class MetricsDAO {

    public static final String METRICS_INDEX_TABLE = "metrics_index";

    public static final String RAW_METRICS_TABLE = "raw_metrics";

    public static final String ONE_HOUR_METRICS_TABLE = "one_hour_metrics";

    public static final String SIX_HOUR_METRICS_TABLE = "six_hour_metrics";

    public static final String TWENTY_FOUR_HOUR_METRICS_TABLE = "twenty_four_hour_metrics";

    // It looks like that there might be a bug in the DataStax driver that will prevent our
    // using prepared statements for range queries. See https://github.com/datastax/java-driver/issues/3
    // for details.
    //
    // jsanda

    private static final String RAW_METRICS_QUERY =
        "SELECT schedule_id, time, value " +
        "FROM " + RAW_METRICS_TABLE + " " +
        "WHERE schedule_id = ? AND time >= ? AND time < ?";

    private static final String RAW_METRICS_WITH_METADATA_QUERY =
        "SELECT schedule_id, time, value, ttl(value), writetime(value) " +
            "FROM " + RAW_METRICS_TABLE + " " +
            "WHERE schedule_id = ? AND time >= ? AND time < ?";

    private static final String INSERT_RAW_METRICS =
        "INSERT INTO raw_metrics (schedule_id, time, value) " +
        "VALUES (?, ?, ?) USING TTL ? AND TIMESTAMP ?";

    private static final String METRICS_INDEX_QUERY =
        "SELECT time, schedule_id " +
        "FROM " + METRICS_INDEX_TABLE + " " +
        "WHERE bucket = ? " +
        "ORDER BY time";

    private static final String UPDATE_METRICS_INDEX =
        "INSERT INTO " + METRICS_INDEX_TABLE + " (bucket, time, schedule_id, null_col) VALUES (?, ?, ?, ?)";

    private Session session;

    public MetricsDAO(Session session) {
        this.session = session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public Set<MeasurementDataNumeric> insertRawMetrics(Set<MeasurementDataNumeric> dataSet, int ttl) {
        Set<MeasurementDataNumeric> insertedMetrics = new HashSet<MeasurementDataNumeric>();
        String sql = "INSERT INTO raw_metrics (schedule_id, time, value) VALUES (?, ?, ?) " +
            "USING TTL " + ttl;
        try {
            PreparedStatement statement = session.prepare(sql);
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

    public List<AggregatedNumericMetric> insertAggregates(String bucket, List<AggregatedNumericMetric> metrics,
        int ttl) {
        List<AggregatedNumericMetric> updates = new ArrayList<AggregatedNumericMetric>();
        try {
            String cql = "INSERT INTO " + bucket + " (schedule_id, time, type, value) VALUES (?, ?, ?, ?) USING TTL " +
                ttl;
            PreparedStatement statement = session.prepare(cql);

            for (AggregatedNumericMetric metric : metrics) {
                BoundStatement boundStatement = statement.bind(metric.getScheduleId(), new Date(metric.getTimestamp()),
                    AggregateType.MIN.ordinal(), metric.getMin());
                session.execute(boundStatement);

                boundStatement = statement.bind(metric.getScheduleId(), new Date(metric.getTimestamp()),
                    AggregateType.MAX.ordinal(), metric.getMax());
                session.execute(boundStatement);

                boundStatement = statement.bind(metric.getScheduleId(), new Date(metric.getTimestamp()),
                    AggregateType.AVG.ordinal(), metric.getAvg());
                session.execute(boundStatement);

                updates.add(metric);
            }
            return updates;
        } catch (NoHostAvailableException e) {
            throw new CQLException(e);
        }
    }

    public List<RawNumericMetric> findRawMetrics(int scheduleId, DateTime startTime, DateTime endTime) {
        try {
            List<RawNumericMetric> metrics = new ArrayList<RawNumericMetric>();

            String cql = "SELECT schedule_id, time, value FROM " + RAW_METRICS_TABLE + " WHERE schedule_id = " +
                scheduleId + " AND time >= " + startTime.getMillis() + " AND time < " + endTime.getMillis() +
                " ORDER BY time";

            ResultSet resultSet = session.execute(cql);
            ResultSetMapper<RawNumericMetric> resultSetMapper = new RawNumericMetricMapper();
            for (Row row : resultSet) {
                metrics.add(resultSetMapper.map(row));
            }
            return metrics;
        } catch (NoHostAvailableException e) {
            throw new CQLException(e);
        }
    }

    public List<RawNumericMetric> findRawMetrics(int scheduleId,  PageOrdering ordering, int limit) {
        try {
            List<RawNumericMetric> metrics = new ArrayList<RawNumericMetric>();

            String cql = "SELECT schedule_id, time, value FROM " + RAW_METRICS_TABLE + " WHERE schedule_id = " +
                scheduleId + " ORDER BY time " + ordering;

            if (limit > 0) {
                cql += " LIMIT " + limit;
            }

            ResultSet resultSet = session.execute(cql);
            ResultSetMapper<RawNumericMetric> resultSetMapper = new RawNumericMetricMapper();
            for (Row row : resultSet) {
                metrics.add(resultSetMapper.map(row));
            }
            return metrics;
        } catch (NoHostAvailableException e) {
            throw new CQLException(e);
        }
    }

    public List<RawNumericMetric> findRawMetrics(int scheduleId, DateTime startTime, DateTime endTime,
        boolean includeMetadata) {

        if (!includeMetadata) {
            return findRawMetrics(scheduleId, startTime, endTime);
        }

        try {
            ResultSet resultSet = session.execute(
                select("schedule_id", "time", "value", "ttl(value), writetime(value)")
                .from(RAW_METRICS_TABLE)
                .where(
                    eq("schedule_id", scheduleId),
                    gte("time", startTime.toDate()),
                    lt("time", endTime.toDate()))
               .getQueryString()
            );
            List<RawNumericMetric> metrics = new ArrayList<RawNumericMetric>();
            ResultSetMapper<RawNumericMetric> resultSetMapper = new RawNumericMetricMapper(true);
            for (Row row : resultSet) {
                metrics.add(resultSetMapper.map(row));
            }
            return metrics;
        } catch (NoHostAvailableException e) {
            throw new CQLException(e);
        }
    }

    public List<RawNumericMetric> findRawMetrics(List<Integer> scheduleIds, DateTime startTime, DateTime endTime) {
        try {
            // TODO investigate possible bug in datastax driver for binding lists as parameters
            // I was not able to get the below query working by directly binding the List
            // object. From a quick glance at the driver code, it looks like it might not
            // yet be properly supported in which case we need to report a bug.
            //
            // jsanda

//            String cql = "SELECT schedule_id, time, value FROM " + RAW_METRICS_TABLE +
//                " WHERE schedule_id IN (?) AND time >= ? AND time < ? ORDER BY time";
//            PreparedStatement statement = session.prepare(cql);
//            BoundStatement boundStatement = statement.bind(scheduleIds, startTime.toDate(), endTime.toDate());
//            ResultSet resultSet = session.execute(boundStatement);
//            String cql =
//                select("schedule_id", "time", "value")
//                .from(RAW_METRICS_TABLE)
//                .where(
//                    in("schedule_id", listToString(scheduleIds)),
//                    gte("time", startTime.toDate()),
//                    lt("time", endTime.toDate()))
//                .getQueryString();

            String cql = "SELECT schedule_id, time, value FROM " + RAW_METRICS_TABLE + " WHERE schedule_id IN (" +
                listToString(scheduleIds) + ") AND time >= " + startTime.getMillis() + " AND time <= " +
                endTime.getMillis();
            ResultSet resultSet = session.execute(cql);

            List<RawNumericMetric> metrics = new ArrayList<RawNumericMetric>();
            ResultSetMapper<RawNumericMetric> resultSetMapper = new RawNumericMetricMapper(false);
            for (Row row : resultSet) {
                metrics.add(resultSetMapper.map(row));
            }
            return metrics;
        } catch (NoHostAvailableException e) {
            throw new CQLException(e);
        }
    }

    public List<AggregatedNumericMetric> findAggregateMetrics(String bucket, int scheduleId) {
        try {
            String cql =
                "SELECT schedule_id, time, type, value " +
                "FROM " + bucket + " " +
                "WHERE schedule_id = " + scheduleId + " " +
                "ORDER BY time, type";
            List<AggregatedNumericMetric> metrics = new ArrayList<AggregatedNumericMetric>();
            ResultSetMapper<AggregatedNumericMetric> resultSetMapper = new AggregateMetricMapper();
            ResultSet resultSet = session.execute(cql);

            while (!resultSet.isExhausted()) {
                metrics.add(resultSetMapper.map(resultSet.fetchOne(), resultSet.fetchOne(), resultSet.fetchOne()));
            }

            return metrics;
        } catch (NoHostAvailableException e) {
            throw new CQLException(e);
        }
    }

    public List<AggregatedNumericMetric> findAggregateMetrics(String bucket, int scheduleId, DateTime startTime,
        DateTime endTime) {

        try {
            String cql =
                "SELECT schedule_id, time, type, value " +
                "FROM " + bucket + " " +
                "WHERE schedule_id = " + scheduleId + " AND time >= " + startTime.getMillis() +
                    " AND time < " + endTime.getMillis();
            List<AggregatedNumericMetric> metrics = new ArrayList<AggregatedNumericMetric>();
            ResultSetMapper<AggregatedNumericMetric> resultSetMapper = new AggregateMetricMapper();
            ResultSet resultSet = session.execute(cql);

            while (!resultSet.isExhausted()) {
                metrics.add(resultSetMapper.map(resultSet.fetchOne(), resultSet.fetchOne(), resultSet.fetchOne()));
            }

            return metrics;
        } catch (NoHostAvailableException e) {
            throw new CQLException(e);
        }
    }

    List<AggregatedNumericMetric> findAggregateMetricsWithMetadata(String bucket, int scheduleId, DateTime startTime,
        DateTime endTime) {

        try {
            String cql =
                "SELECT schedule_id, time, type, value, ttl(value), writetime(value) " +
                    "FROM " + bucket + " " +
                    "WHERE schedule_id = " + scheduleId + " AND time >= " + startTime.getMillis() +
                    " AND time < " + endTime.getMillis();
            List<AggregatedNumericMetric> metrics = new ArrayList<AggregatedNumericMetric>();
            ResultSetMapper<AggregatedNumericMetric> resultSetMapper = new AggregateMetricMapper(true);
            ResultSet resultSet = session.execute(cql);

            while (!resultSet.isExhausted()) {
                metrics.add(resultSetMapper.map(resultSet.fetchOne(), resultSet.fetchOne(), resultSet.fetchOne()));
            }

            return metrics;
        } catch (NoHostAvailableException e) {
            throw new CQLException(e);
        }
    }

    public List<MetricsIndexEntry> findMetricsIndexEntries(String bucket) {
        try {
            PreparedStatement statement = session.prepare(METRICS_INDEX_QUERY);
            BoundStatement boundStatement = statement.bind(bucket);
            ResultSet resultSet = session.execute(boundStatement);
            List<MetricsIndexEntry> indexEntries = new ArrayList<MetricsIndexEntry>();
            ResultSetMapper<MetricsIndexEntry> resultSetMapper = new MetricsIndexResultSetMapper(bucket);

            for (Row row : resultSet) {
                indexEntries.add(resultSetMapper.map(row));
            }

            return indexEntries;

        } catch (NoHostAvailableException e) {
            throw new CQLException(e);
        }
    }

    public void updateMetricsIndex(String bucket, Map<Integer, DateTime> updates) {
        try {
            PreparedStatement statement = session.prepare(UPDATE_METRICS_INDEX);
            for (Integer scheduleId : updates.keySet()) {
                BoundStatement boundStatement = statement.bind(bucket, updates.get(scheduleId).toDate(), scheduleId,
                    false);
                session.execute(boundStatement);
            }
        } catch (NoHostAvailableException e) {
            throw new CQLException(e);
        }
    }

    public void deleteMetricsIndexEntries(String table) {
        try {
            String cql = "DELETE FROM " + METRICS_INDEX_TABLE + " WHERE bucket = '" + table + "'";
            session.execute(cql);
        } catch (NoHostAvailableException e) {
            throw new CQLException(e);
        }
    }
}
