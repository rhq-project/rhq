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
import org.joda.time.DateTime;

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

    private PreparedStatement rawMetricsQuery;
    private PreparedStatement findLatestRawMetric;
    private PreparedStatement findRawMetrics;
    private PreparedStatement findAggregateMetricsByDateRange;
    private PreparedStatement insertIndexEntry;
    private PreparedStatement findIndexEntries;
    private PreparedStatement findIndexEntriesAfterScheduleId;
    private PreparedStatement deleteIndexEntry;
    private PreparedStatement deleteAggregate;

    private InsertStatements insertStatements;
    private DateTimeService dateTimeService;

    public MetricsDAO(StorageSession session, MetricsConfiguration configuration) {
        this.storageSession = session;
        this.configuration = configuration;
        initPreparedStatements();
    }

    public MetricsDAO(StorageSession session, MetricsConfiguration configuration, InsertStatements insertStatements,
        DateTimeService dateTimeService) {
        this.storageSession = session;
        this.configuration = configuration;
        this.insertStatements = insertStatements;
        this.dateTimeService = dateTimeService;
        initPreparedStatements();
    }

    public void initPreparedStatements() {
        log.info("Initializing prepared statements");
        long startTime = System.currentTimeMillis();

        rawMetricsQuery = storageSession.prepare("SELECT schedule_id, time, value FROM " + MetricsTable.RAW +
            " WHERE schedule_id = ? AND time >= ? AND time < ? ORDER BY time");

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
        DateTime day = dateTimeService.get24HourTimeSlice(data.getTimestamp());
        PreparedStatement preparedStatement = insertStatements.raw.get(day);
        if (preparedStatement == null) {
            throw new IllegalArgumentException("No raw insert prepared statement found for {day: " + day +
                ", data: " + data + "}");
        }
        BoundStatement statement = preparedStatement.bind(data.getScheduleId(), new Date(data.getTimestamp()),
            data.getValue());
        return storageSession.executeAsync(statement);
    }

    public StorageResultSetFuture insert1HourData(AggregateNumericMetric metric) {
        DateTime day = dateTimeService.get24HourTimeSlice(metric.getTimestamp());
        PreparedStatement preparedStatement = insertStatements.oneHour.get(day);
        if (preparedStatement == null) {
            throw new IllegalArgumentException("No 1 hour insert prepared statement found for {day: " + day +
                ", metric: " + metric + "}");
        }
        BoundStatement statement = preparedStatement.bind(metric.getScheduleId(), new Date(metric.getTimestamp()),
            metric.getAvg(), metric.getMax(), metric.getMin());
        return storageSession.executeAsync(statement);
    }

    public StorageResultSetFuture insert6HourData(AggregateNumericMetric metric) {
        DateTime day = dateTimeService.get24HourTimeSlice(metric.getTimestamp());
        PreparedStatement preparedStatement = insertStatements.sixHour.get(day);
        if (preparedStatement == null) {
            throw new IllegalArgumentException("No 6 hour insert prepared statement found for {day: " + day +
                ", metric: " + metric + "}");
        }
        BoundStatement statement = preparedStatement.bind(metric.getScheduleId(), new Date(metric.getTimestamp()),
            metric.getAvg(), metric.getMax(), metric.getMin());
        return storageSession.executeAsync(statement);
    }

    public StorageResultSetFuture insert24HourData(AggregateNumericMetric metric) {
        DateTime day = dateTimeService.get24HourTimeSlice(metric.getTimestamp());
        PreparedStatement preparedStatement = insertStatements.twentyFourHour.get(day);
        if (preparedStatement == null) {
            throw new IllegalArgumentException("No 24 hour insert prepared statement found for {day: " + day +
                ", metric: " + metric + "}");
        }
        BoundStatement statement = preparedStatement.bind(metric.getScheduleId(),
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
        RawNumericMetricMapper mapper = new RawNumericMetricMapper();
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

    public StorageResultSetFuture findIndexEntries(IndexBucket bucket, int partition, long timestamp) {
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
