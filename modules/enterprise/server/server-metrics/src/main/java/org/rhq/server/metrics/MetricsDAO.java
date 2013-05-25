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


import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.exceptions.NoHostAvailableException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.measurement.MeasurementDataNumeric;
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

    private final Log log = LogFactory.getLog(MetricsDAO.class);

    private Session session;

    private MetricsConfiguration configuration;

    private PreparedStatement insertRawData;
    private PreparedStatement rawMetricsQuery;
    private PreparedStatement updateMetricsIndex;
    private PreparedStatement insertOneHourData;
    private PreparedStatement insertSixHourData;
    private PreparedStatement insertTwentyFourHourData;
    private PreparedStatement findLatestRawMetric;
    private PreparedStatement findRawMetrics;
    private PreparedStatement findOneHourMetricsByDateRange;
    private PreparedStatement findSixHourMetricsByDateRange;
    private PreparedStatement findTwentyFourHourMetricsByDateRange;
    private PreparedStatement findIndexEntries;
    private PreparedStatement deleteIndexEntries;

    public MetricsDAO(Session session, MetricsConfiguration configuration) {
        this.session = session;
        this.configuration = configuration;
        initPreparedStatements();
    }

    public void initPreparedStatements() {
        log.info("Initializing prepared statements");

        // If we at any point decide to support configurable TTLs then some of the
        // statements below will have to be updated and prepared again with the new TTL.
        // So let's say the user triggers an API call to use new TTLs. Assuming this is
        // done without requiring an RHQ server restart, then any MetricsDAO instances will
        // have to get updated with the new TTLs. PreparedStatement object will have to be
        // re-initialized and re-prepared with the new TTLs. None of this would be necessary
        // if the TTL value could be a bound value.

        insertRawData = session.prepare(
            "INSERT INTO " + MetricsTable.RAW + " (schedule_id, time, value) VALUES (?, ?, ?) USING TTL " +
                configuration.getRawTTL());

        rawMetricsQuery = session.prepare("SELECT schedule_id, time, value FROM " + MetricsTable.RAW +
            " WHERE schedule_id = ? AND time >= ? AND time < ? ORDER BY time");

        insertOneHourData = session.prepare("INSERT INTO " + MetricsTable.ONE_HOUR + "(schedule_id, time, " +
            "type, value) VALUES (?, ?, ?, ?) USING TTL " + configuration.getOneHourTTL());

        insertSixHourData = session.prepare("INSERT INTO " + MetricsTable.SIX_HOUR + "(schedule_id, time, " +
            "type, value) VALUES (?, ?, ?, ?) USING TTL " + configuration.getOneHourTTL());

        insertTwentyFourHourData = session.prepare("INSERT INTO " + MetricsTable.TWENTY_FOUR_HOUR + "(schedule_id, " +
            "time, type, value) VALUES (?, ?, ?, ?) USING TTL " + configuration.getOneHourTTL());

        updateMetricsIndex = session.prepare("INSERT INTO " + MetricsTable.INDEX + " (bucket, time, schedule_id) " +
            "VALUES (?, ?, ?)");

        findLatestRawMetric = session.prepare("SELECT schedule_id, time, value FROM " + MetricsTable.RAW +
            " WHERE schedule_id = ? ORDER BY time DESC LIMIT 1");

        findRawMetrics = session.prepare("SELECT schedule_id, time, value FROM " + MetricsTable.RAW +
            " WHERE schedule_id = ? AND time >= ? AND time <= ?");

        findOneHourMetricsByDateRange = session.prepare("SELECT schedule_id, time, type, value FROM " +
            MetricsTable.ONE_HOUR + " WHERE schedule_id = ? AND time >= ? AND time < ?");

        findSixHourMetricsByDateRange = session.prepare("SELECT schedule_id, time, type, value FROM "
            + MetricsTable.SIX_HOUR + " WHERE schedule_id = ? AND time >= ? AND time < ?");

        findTwentyFourHourMetricsByDateRange = session.prepare("SELECT schedule_id, time, type, value FROM " +
            MetricsTable.TWENTY_FOUR_HOUR + " WHERE schedule_id = ? AND time >= ? AND time < ?");

        findIndexEntries = session.prepare("SELECT time, schedule_id FROM " + MetricsTable.INDEX +
            " WHERE bucket = ? AND time = ?");

        deleteIndexEntries = session.prepare("DELETE FROM " + MetricsTable.INDEX + " WHERE bucket = ? AND time = ?");
    }

    public ResultSetFuture insertRawData(MeasurementDataNumeric data) {
        BoundStatement statement = insertRawData.bind(data.getScheduleId(), new Date(data.getTimestamp()),
            data.getValue());
        return session.executeAsync(statement);
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

    public ResultSet insertOneHourData(int scheduleId, long timestamp, AggregateType type, double value) {
        BoundStatement statement = insertOneHourData.bind(scheduleId, new Date(timestamp), type.ordinal(), value);
        return session.execute(statement);
    }

    public ResultSet insertSixHourData(int scheduleId, long timestamp, AggregateType type, double value) {
        BoundStatement statement = insertSixHourData.bind(scheduleId, new Date(timestamp), type.ordinal(), value);
        return session.execute(statement);
    }

    public ResultSet insertTwentyFourHourData(int scheduleId, long timestamp, AggregateType type, double value) {
        BoundStatement statement = insertTwentyFourHourData.bind(scheduleId, new Date(timestamp), type.ordinal(),
            value);
        return session.execute(statement);
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
            BoundStatement boundStatement = rawMetricsQuery.bind(scheduleId, new Date(startTime), new Date(endTime));
            return new SimplePagedResult<RawNumericMetric>(boundStatement, new RawNumericMetricMapper(false), session);
        } catch (NoHostAvailableException e) {
            throw new CQLException(e);
        }
    }

    public RawNumericMetric findLatestRawMetric(int scheduleId) {
        RawNumericMetricMapper mapper = new RawNumericMetricMapper(false);
        BoundStatement boundStatement = findLatestRawMetric.bind(scheduleId);
        ResultSet resultSet = session.execute(boundStatement);

        return mapper.mapOne(resultSet);
    }

    public Iterable<RawNumericMetric> findRawMetrics(List<Integer> scheduleIds, long startTime, long endTime) {
        return new ListPagedResult<RawNumericMetric>(findRawMetrics, scheduleIds, startTime, endTime,
            new RawNumericMetricMapper(), session);
    }

    public Iterable<AggregateNumericMetric> findOneHourMetrics(int scheduleId, long startTime, long endTime) {
        BoundStatement statement = findOneHourMetricsByDateRange.bind(scheduleId, new Date(startTime), new Date(endTime));
        return new SimplePagedResult<AggregateNumericMetric>(statement, new AggregateNumericMetricMapper(), session);
    }

    public Iterable<AggregateNumericMetric> findSixHourMetrics(int scheduleId, long startTime, long endTime) {
        BoundStatement statement = findSixHourMetricsByDateRange.bind(scheduleId, new Date(startTime), new Date(endTime));
        return new SimplePagedResult<AggregateNumericMetric>(statement, new AggregateNumericMetricMapper(), session);
    }

    public Iterable<AggregateNumericMetric> findTwentyFourHourMetrics(int scheduleId, long startTime, long endTime) {
        BoundStatement statement = findTwentyFourHourMetricsByDateRange.bind(scheduleId, new Date(startTime), new Date(endTime));
        return new SimplePagedResult<AggregateNumericMetric>(statement, new AggregateNumericMetricMapper(), session);
    }

    public Iterable<AggregateSimpleNumericMetric> findAggregatedSimpleOneHourMetric(int scheduleId, long startTime,
        long endTime) {
        BoundStatement statement = findOneHourMetricsByDateRange.bind(scheduleId, new Date(startTime),
            new Date(endTime));
        return new SimplePagedResult<AggregateSimpleNumericMetric>(statement, new AggregateSimpleNumericMetricMapper(),
            session);
    }

    public Iterable<AggregateNumericMetric> findOneHourMetrics(List<Integer> scheduleIds, long startTime,
        long endTime) {
        return new ListPagedResult<AggregateNumericMetric>(findOneHourMetricsByDateRange, scheduleIds, startTime, endTime,
            new AggregateNumericMetricMapper(), session);
    }

    public Iterable<AggregateNumericMetric> findSixHourMetrics(List<Integer> scheduleIds, long startTime,
        long endTime) {
        return new ListPagedResult<AggregateNumericMetric>(findSixHourMetricsByDateRange, scheduleIds, startTime, endTime,
            new AggregateNumericMetricMapper(), session);
    }

    public Iterable<AggregateNumericMetric> findTwentyFourHourMetrics(List<Integer> scheduleIds, long startTime,
        long endTime) {
        return new ListPagedResult<AggregateNumericMetric>(findTwentyFourHourMetricsByDateRange, scheduleIds, startTime, endTime,
            new AggregateNumericMetricMapper(), session);
    }

    public Iterable<MetricsIndexEntry> findMetricsIndexEntries(final MetricsTable table, long timestamp) {
        BoundStatement statement = findIndexEntries.bind(table.toString(), new Date(timestamp));
        return new SimplePagedResult<MetricsIndexEntry>(statement, new MetricsIndexEntryMapper(table), session);
    }

    public void updateMetricsIndex(MetricsTable table, Map<Integer, Long> updates) {
            for (Integer scheduleId : updates.keySet()) {
                BoundStatement statement = updateMetricsIndex.bind(table.getTableName(),
                    new Date(updates.get(scheduleId)), scheduleId);
                session.execute(statement);
            }
    }

    public ResultSetFuture updateMetricsIndex(MetricsTable table, int scheduleId, long timestamp) {
        BoundStatement statement = updateMetricsIndex.bind(table.getTableName(), new Date(timestamp), scheduleId);
        return session.executeAsync(statement);
    }

    public void deleteMetricsIndexEntries(MetricsTable table, long timestamp) {
        BoundStatement statement = deleteIndexEntries.bind(table.getTableName(), new Date(timestamp));
        session.execute(statement);
    }
}
