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


import java.util.Date;
import java.util.List;
import java.util.Map;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
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
import org.rhq.server.metrics.domain.MetricsTable;
import org.rhq.server.metrics.domain.RawNumericMetric;
import org.rhq.server.metrics.domain.RawNumericMetricMapper;
import org.rhq.server.metrics.domain.SimplePagedResult;


/**
 * @author John Sanda
 */
public class MetricsDAO {

    private final Log log = LogFactory.getLog(MetricsDAO.class);

    private StorageSession storageSession;

    private MetricsConfiguration configuration;

    private PreparedStatement insertRawData;
    private PreparedStatement rawMetricsQuery;
    private PreparedStatement insertCacheEntry;
    private PreparedStatement insertOneHourData;
    private PreparedStatement insertSixHourData;
    private PreparedStatement insertTwentyFourHourData;
    private PreparedStatement findLatestRawMetric;
    private PreparedStatement findRawMetrics;
    private PreparedStatement findOneHourMetricsByDateRange;
    private PreparedStatement findSixHourMetricsByDateRange;
    private PreparedStatement findTwentyFourHourMetricsByDateRange;
    private PreparedStatement findCacheEntries;
    private PreparedStatement findCacheTimeSlice;
    private PreparedStatement deleteCacheEntries;

    public MetricsDAO(StorageSession session, MetricsConfiguration configuration) {
        this.storageSession = session;
        this.configuration = configuration;
        initPreparedStatements();
    }

    public void initPreparedStatements() {
        log.info("Initializing prepared statements");
        long startTime = System.currentTimeMillis();

        // If we at any point decide to support configurable TTLs then some of the
        // statements below will have to be updated and prepared again with the new TTL.
        // So let's say the user triggers an API call to use new TTLs. Assuming this is
        // done without requiring an RHQ server restart, then any MetricsDAO instances will
        // have to get updated with the new TTLs. PreparedStatement object will have to be
        // re-initialized and re-prepared with the new TTLs. None of this would be necessary
        // if the TTL value could be a bound value.

        insertRawData = storageSession.prepare(
            "INSERT INTO " + MetricsTable.RAW + " (schedule_id, time, value) VALUES (?, ?, ?) USING TTL " +
                configuration.getRawTTL());

        rawMetricsQuery = storageSession.prepare("SELECT schedule_id, time, value FROM " + MetricsTable.RAW +
            " WHERE schedule_id = ? AND time >= ? AND time < ? ORDER BY time");

        insertOneHourData = storageSession.prepare("INSERT INTO " + MetricsTable.ONE_HOUR + "(schedule_id, time, " +
            "type, value) VALUES (?, ?, ?, ?) USING TTL " + configuration.getOneHourTTL());

        insertSixHourData = storageSession.prepare("INSERT INTO " + MetricsTable.SIX_HOUR + "(schedule_id, time, " +
            "type, value) VALUES (?, ?, ?, ?) USING TTL " + configuration.getSixHourTTL());

        insertTwentyFourHourData = storageSession.prepare("INSERT INTO " + MetricsTable.TWENTY_FOUR_HOUR + "(schedule_id, " +
            "time, type, value) VALUES (?, ?, ?, ?) USING TTL " + configuration.getTwentyFourHourTTL());

        insertCacheEntry = storageSession.prepare("INSERT INTO " + MetricsTable.METRICS_CACHE +
            " (bucket, time_slice, start_schedule_id, schedule_id, time, value) VALUES (?, ?, ?, ?, ?, ?)");

        findLatestRawMetric = storageSession.prepare("SELECT schedule_id, time, value FROM " + MetricsTable.RAW +
            " WHERE schedule_id = ? ORDER BY time DESC LIMIT 1");

        findRawMetrics = storageSession.prepare("SELECT schedule_id, time, value FROM " + MetricsTable.RAW +
            " WHERE schedule_id = ? AND time >= ? AND time <= ?");

        findOneHourMetricsByDateRange = storageSession.prepare("SELECT schedule_id, time, type, value FROM " +
            MetricsTable.ONE_HOUR + " WHERE schedule_id = ? AND time >= ? AND time < ?");

        findSixHourMetricsByDateRange = storageSession.prepare("SELECT schedule_id, time, type, value FROM "
            + MetricsTable.SIX_HOUR + " WHERE schedule_id = ? AND time >= ? AND time < ?");

        findTwentyFourHourMetricsByDateRange = storageSession.prepare("SELECT schedule_id, time, type, value FROM " +
            MetricsTable.TWENTY_FOUR_HOUR + " WHERE schedule_id = ? AND time >= ? AND time < ?");

        findCacheEntries = storageSession.prepare("SELECT schedule_id, time, value FROM " + MetricsTable.METRICS_CACHE +
            " WHERE bucket = ? AND time_slice = ? AND start_schedule_id = ?");

        findCacheTimeSlice = storageSession.prepare("SELECT time_slice FROM " + MetricsTable.METRICS_CACHE +
            " WHERE bucket = ? AND time_slice = ? AND start_schedule_id = ?");

        deleteCacheEntries = storageSession.prepare("DELETE FROM " + MetricsTable.METRICS_CACHE +
            " WHERE bucket = ? AND time_slice = ? AND start_schedule_id = ?");

        long endTime = System.currentTimeMillis();
        log.info("Finished initializing prepared statements in " + (endTime - startTime) + " ms");
    }

    public StorageResultSetFuture insertRawData(MeasurementDataNumeric data) {
        BoundStatement statement = insertRawData.bind(data.getScheduleId(), new Date(data.getTimestamp()),
            data.getValue());
        return storageSession.executeAsync(statement);
    }

    public ResultSet insertOneHourData(int scheduleId, long timestamp, AggregateType type, double value) {
        BoundStatement statement = insertOneHourData.bind(scheduleId, new Date(timestamp), type.ordinal(), value);
        return storageSession.execute(statement);
    }

    public StorageResultSetFuture insertOneHourDataAsync(int scheduleId, long timestamp, AggregateType type,
        double value) {
        BoundStatement statement = insertOneHourData.bind(scheduleId, new Date(timestamp), type.ordinal(), value);
        return storageSession.executeAsync(statement);
    }

    public ResultSet insertSixHourData(int scheduleId, long timestamp, AggregateType type, double value) {
        BoundStatement statement = insertSixHourData.bind(scheduleId, new Date(timestamp), type.ordinal(), value);
        return storageSession.execute(statement);
    }

    public StorageResultSetFuture insertSixHourDataAsync(int scheduleId, long timestamp, AggregateType type,
        double value) {
        BoundStatement statement = insertSixHourData.bind(scheduleId, new Date(timestamp), type.ordinal(), value);
        return storageSession.executeAsync(statement);
    }

    public ResultSet insertTwentyFourHourData(int scheduleId, long timestamp, AggregateType type, double value) {
        BoundStatement statement = insertTwentyFourHourData.bind(scheduleId, new Date(timestamp), type.ordinal(),
            value);
        return storageSession.execute(statement);
    }

    public StorageResultSetFuture insertTwentyFourHourDataAsync(int scheduleId, long timestamp, AggregateType type,
        double value) {
        BoundStatement statement = insertTwentyFourHourData.bind(scheduleId, new Date(timestamp), type.ordinal(),
            value);
        return storageSession.executeAsync(statement);
    }

    public Iterable<RawNumericMetric> findRawMetrics(int scheduleId, long startTime, long endTime) {
        try {
            BoundStatement boundStatement = rawMetricsQuery.bind(scheduleId, new Date(startTime), new Date(endTime));
            return new SimplePagedResult<RawNumericMetric>(boundStatement, new RawNumericMetricMapper(false),
                storageSession);
        } catch (NoHostAvailableException e) {
            throw new CQLException(e);
        }
    }

    public ResultSet findRawMetricsSync(int scheduleId, long startTime, long endTime) {
        BoundStatement boundStatement = rawMetricsQuery.bind(scheduleId, new Date(startTime), new Date(endTime));
        return storageSession.execute(boundStatement);
    }

    public StorageResultSetFuture findRawMetricsAsync(int scheduleId, long startTime, long endTime) {
        BoundStatement boundStatement = rawMetricsQuery.bind(scheduleId, new Date(startTime), new Date(endTime));
        return storageSession.executeAsync(boundStatement);
    }

    public RawNumericMetric findLatestRawMetric(int scheduleId) {
        RawNumericMetricMapper mapper = new RawNumericMetricMapper(false);
        BoundStatement boundStatement = findLatestRawMetric.bind(scheduleId);
        ResultSet resultSet = storageSession.execute(boundStatement);

        return mapper.mapOne(resultSet);
    }

    public Iterable<RawNumericMetric> findRawMetrics(List<Integer> scheduleIds, long startTime, long endTime) {
        return new ListPagedResult<RawNumericMetric>(findRawMetrics, scheduleIds, startTime, endTime,
            new RawNumericMetricMapper(), storageSession);
    }

    public Iterable<AggregateNumericMetric> findOneHourMetrics(int scheduleId, long startTime, long endTime) {
        BoundStatement statement = findOneHourMetricsByDateRange.bind(scheduleId, new Date(startTime), new Date(endTime));
        return new SimplePagedResult<AggregateNumericMetric>(statement, new AggregateNumericMetricMapper(),
            storageSession);
    }

    public StorageResultSetFuture findOneHourMetricsAsync(int scheduleId, long startTime, long endTime) {
        BoundStatement statement = findOneHourMetricsByDateRange.bind(scheduleId, new Date(startTime),
            new Date(endTime));
        return storageSession.executeAsync(statement);
    }

    public Iterable<AggregateNumericMetric> findSixHourMetrics(int scheduleId, long startTime, long endTime) {
        BoundStatement statement = findSixHourMetricsByDateRange.bind(scheduleId, new Date(startTime), new Date(endTime));
        return new SimplePagedResult<AggregateNumericMetric>(statement, new AggregateNumericMetricMapper(),
            storageSession);
    }

    public StorageResultSetFuture findSixHourMetricsAsync(int scheduleId, long startTime, long endTime) {
        BoundStatement statement = findSixHourMetricsByDateRange.bind(scheduleId, new Date(startTime), new Date(endTime));
        return storageSession.executeAsync(statement);
    }

    public Iterable<AggregateNumericMetric> findTwentyFourHourMetrics(int scheduleId, long startTime, long endTime) {
        BoundStatement statement = findTwentyFourHourMetricsByDateRange.bind(scheduleId, new Date(startTime), new Date(endTime));
        return new SimplePagedResult<AggregateNumericMetric>(statement, new AggregateNumericMetricMapper(),
            storageSession);
    }

    public StorageResultSetFuture findTwentyFourHourMetricsAsync(int scheduleId, long startTime, long endTime) {
        BoundStatement statement = findTwentyFourHourMetricsByDateRange.bind(scheduleId, new Date(startTime),
            new Date(endTime));
        return storageSession.executeAsync(statement);
    }

    public Iterable<AggregateSimpleNumericMetric> findAggregatedSimpleOneHourMetric(int scheduleId, long startTime,
        long endTime) {
        BoundStatement statement = findOneHourMetricsByDateRange.bind(scheduleId, new Date(startTime),
            new Date(endTime));
        return new SimplePagedResult<AggregateSimpleNumericMetric>(statement, new AggregateSimpleNumericMetricMapper(),
            storageSession);
    }

    public Iterable<AggregateNumericMetric> findOneHourMetrics(List<Integer> scheduleIds, long startTime,
        long endTime) {
        return new ListPagedResult<AggregateNumericMetric>(findOneHourMetricsByDateRange, scheduleIds, startTime, endTime,
            new AggregateNumericMetricMapper(), storageSession);
    }

    public Iterable<AggregateNumericMetric> findSixHourMetrics(List<Integer> scheduleIds, long startTime,
        long endTime) {
        return new ListPagedResult<AggregateNumericMetric>(findSixHourMetricsByDateRange, scheduleIds, startTime, endTime,
            new AggregateNumericMetricMapper(), storageSession);
    }

    public Iterable<AggregateNumericMetric> findTwentyFourHourMetrics(List<Integer> scheduleIds, long startTime,
        long endTime) {
        return new ListPagedResult<AggregateNumericMetric>(findTwentyFourHourMetricsByDateRange, scheduleIds, startTime, endTime,
            new AggregateNumericMetricMapper(), storageSession);
    }

    public StorageResultSetFuture findCacheEntriesAsync(MetricsTable table, long timeSlice,
        int startScheduleId) {
        BoundStatement statement = findCacheEntries.bind(table.toString(), new Date(timeSlice), startScheduleId);
        return storageSession.executeAsync(statement);
    }

    public ResultSet findCacheTimeSlice(MetricsTable table, long timestamp, int startScheduleId) {
        BoundStatement statement = findCacheTimeSlice.bind(table.toString(), new Date(timestamp), startScheduleId);
        return storageSession.execute(statement);
    }

    public StorageResultSetFuture updateMetricsCache(MetricsTable table, long timeSlice, int startScheduleId,
        int scheduleId, long timestamp, Map<Integer, Double> values) {
        BoundStatement statement = insertCacheEntry.bind(table.getTableName(), new Date(timeSlice), startScheduleId,
            scheduleId, new Date(timestamp), values);
        return storageSession.executeAsync(statement);
    }

    public StorageResultSetFuture deleteCacheEntries(MetricsTable table, long timestamp,
        int startScheduleId) {
        BoundStatement statement = deleteCacheEntries.bind(table.getTableName(), new Date(timestamp), startScheduleId);
        return storageSession.executeAsync(statement);
    }
}
