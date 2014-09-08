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

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.AggregateNumericMetricMapper;
import org.rhq.server.metrics.domain.Bucket;
import org.rhq.server.metrics.domain.IndexBucket;
import org.rhq.server.metrics.domain.IndexEntry;
import org.rhq.server.metrics.domain.ListPagedResult;
import org.rhq.server.metrics.domain.MetricsTable;
import org.rhq.server.metrics.domain.RawNumericMetric;
import org.rhq.server.metrics.domain.RawNumericMetricMapper;


/**
 * @author John Sanda
 */
public class MetricsDAO {

    private final Log log = LogFactory.getLog(MetricsDAO.class);

    private StorageSession storageSession;

    private MetricsConfiguration configuration;

    private PreparedStatement insertRawData;
    private PreparedStatement rawMetricsQuery;
    private PreparedStatement insertOneHourData;
    private PreparedStatement insertSixHourData;
    private PreparedStatement insertTwentyFourHourData;
    private PreparedStatement findLatestRawMetric;
    private PreparedStatement findRawMetrics;
    private PreparedStatement findAggregateMetricsByDateRange;
    private PreparedStatement insertIndexEntry;
    private PreparedStatement findIndexEntries;
    private PreparedStatement findIndexEntriesAfterScheduleId;
    private PreparedStatement deleteIndexEntry;
    private PreparedStatement deleteAggregate;

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

        insertOneHourData = storageSession.prepare(
            "INSERT INTO " + MetricsTable.AGGREGATE + "(schedule_id, bucket, time, avg, max, min) " +
            "VALUES (?, '" + Bucket.ONE_HOUR + "', ?, ?, ?, ?) " +
            "USING TTL " + configuration.getOneHourTTL());

        insertSixHourData = storageSession.prepare(
            "INSERT INTO " + MetricsTable.AGGREGATE + "(schedule_id, bucket, time, avg, max, min) " +
            "VALUES (?, '" + Bucket.SIX_HOUR + "', ?, ?, ?, ?) " +
            "USING TTL " + configuration.getSixHourTTL());

        insertTwentyFourHourData = storageSession.prepare(
            "INSERT INTO " + MetricsTable.AGGREGATE + "(schedule_id, bucket, time, avg, max, min) " +
            "VALUES (?, '" + Bucket.TWENTY_FOUR_HOUR + "', ?, ?, ?, ?) " +
            "USING TTL " + configuration.getTwentyFourHourTTL());

        findLatestRawMetric = storageSession.prepare("SELECT schedule_id, time, value FROM " + MetricsTable.RAW +
            " WHERE schedule_id = ? ORDER BY time DESC LIMIT 1");

        findRawMetrics = storageSession.prepare("SELECT schedule_id, time, value FROM " + MetricsTable.RAW +
            " WHERE schedule_id = ? AND time >= ? AND time <= ?");

        findAggregateMetricsByDateRange = storageSession.prepare(
            "SELECT schedule_id, bucket, time, avg, max, min " +
            "FROM " + MetricsTable.AGGREGATE + " " +
            "WHERE schedule_id = ? AND bucket = ? AND time >= ? AND time < ?");

        insertIndexEntry = storageSession.prepare(
            "INSERT INTO " + MetricsTable.INDEX + " (bucket, partition, time, schedule_id) " +
            "VALUES (?, ?, ?, ?) ");

        deleteIndexEntry = storageSession.prepare(
            "DELETE FROM " + MetricsTable.INDEX + " " +
            "WHERE bucket = ? AND partition = ? AND time = ? AND schedule_id = ?");

        findIndexEntries = storageSession.prepare(
            "SELECT schedule_id " +
            "FROM " + MetricsTable.INDEX + " " +
            "WHERE bucket = ? AND partition = ? AND time = ? " +
            "LIMIT " + configuration.getIndexPageSize());

        findIndexEntriesAfterScheduleId = storageSession.prepare(
            "SELECT schedule_id " +
            "FROM " + MetricsTable.INDEX + " " +
            "WHERE bucket = ? AND partition = ? AND time = ? AND schedule_id > ? " +
            "LIMIT " + configuration.getIndexPageSize());

        deleteAggregate = storageSession.prepare(
            "DELETE FROM " + MetricsTable.AGGREGATE + " " +
            "WHERE schedule_id = ? AND bucket = ? AND time = ?");

        long endTime = System.currentTimeMillis();
        log.info("Finished initializing prepared statements in " + (endTime - startTime) + " ms");
    }

    public StorageSession getStorageSession() {
        return storageSession;
    }

    public StorageResultSetFuture insertRawData(MeasurementDataNumeric data) {
        BoundStatement statement = insertRawData.bind(data.getScheduleId(), new Date(data.getTimestamp()),
            data.getValue());
        return storageSession.executeAsync(statement);
    }

    public StorageResultSetFuture insert1HourData(AggregateNumericMetric metric) {
        BoundStatement statement = insertOneHourData.bind(metric.getScheduleId(), new Date(metric.getTimestamp()),
            metric.getAvg(), metric.getMax(), metric.getMin());
        return storageSession.executeAsync(statement);
    }

    public StorageResultSetFuture insert6HourData(AggregateNumericMetric metric) {
        BoundStatement statement = insertSixHourData.bind(metric.getScheduleId(), new Date(metric.getTimestamp()),
            metric.getAvg(), metric.getMax(), metric.getMin());
        return storageSession.executeAsync(statement);
    }

    public StorageResultSetFuture insert24HourData(AggregateNumericMetric metric) {
        BoundStatement statement = insertTwentyFourHourData.bind(metric.getScheduleId(),
            new Date(metric.getTimestamp()), metric.getAvg(), metric.getMax(), metric.getMin());
        return storageSession.executeAsync(statement);
    }

    public List<RawNumericMetric> findRawMetrics(int scheduleId, long startTime, long endTime) {
        RawNumericMetricMapper mapper = new RawNumericMetricMapper();
        BoundStatement boundStatement = rawMetricsQuery.bind(scheduleId, new Date(startTime), new Date(endTime));
        ResultSet resultSet = storageSession.execute(boundStatement);

        return mapper.mapAll(resultSet);
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

    public List<AggregateNumericMetric> findAggregateMetrics(int scheduleId, Bucket bucket, long startTime,
        long endTime) {
        BoundStatement statement = findAggregateMetricsByDateRange.bind(scheduleId, bucket.toString(),
            new Date(startTime), new Date(endTime));
        ResultSet resultSet = storageSession.execute(statement);
        AggregateNumericMetricMapper mapper = new AggregateNumericMetricMapper();
        return mapper.mapAll(resultSet);
    }

    public StorageResultSetFuture findAggregateMetricsAsync(int scheduleId, Bucket bucket, long startTime,
        long endTime) {
        BoundStatement statement = findAggregateMetricsByDateRange.bind(scheduleId, bucket.toString(),
            new Date(startTime), new Date(endTime));
        return storageSession.executeAsync(statement);
    }

    public StorageResultSetFuture findIndexEntries(MetricsTable bucket, int partition, long timestamp) {
        BoundStatement statement = findIndexEntries.bind(bucket.toString(), partition, new Date(timestamp));
        return storageSession.executeAsync(statement);
    }

    public StorageResultSetFuture findIndexEntries(IndexBucket bucket, int partition, long timestamp, int scheduleId) {
        BoundStatement statement = findIndexEntriesAfterScheduleId.bind(bucket.toString(), partition,
            new Date(timestamp), scheduleId);
        return storageSession.executeAsync(statement);
    }

    public StorageResultSetFuture updateIndex(IndexBucket bucket, long timestamp, int scheduleId) {
        BoundStatement statement = insertIndexEntry.bind(bucket.toString(),
            (scheduleId % configuration.getIndexPartitions()), new Date(timestamp), scheduleId);
        return storageSession.executeAsync(statement);
    }

    public StorageResultSetFuture deleteIndexEntry(IndexEntry indexEntry) {
        BoundStatement statement = deleteIndexEntry.bind(indexEntry.getBucket().toString(), indexEntry.getPartition(),
            new Date(indexEntry.getTimestamp()), indexEntry.getScheduleId());
        return storageSession.executeAsync(statement);
    }

    public void deleteAggregate(AggregateNumericMetric metric) {
        BoundStatement statement = deleteAggregate.bind(metric.getScheduleId(), metric.getBucket().toString(),
            new Date(metric.getTimestamp()));
        storageSession.execute(statement);
    }

}
