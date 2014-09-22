/*
 * RHQ Management Platform
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.rhq.server.metrics.migrator.workers;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSetFuture;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hibernate.StatelessSession;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import org.rhq.server.metrics.DateTimeService;
import org.rhq.server.metrics.MetricsConfiguration;
import org.rhq.server.metrics.domain.MetricsTable;
import org.rhq.server.metrics.migrator.DataMigrator.DataMigratorConfiguration;

/**
 * @author Stefan Negrea
 *
 */
public class MetricsIndexMigrator extends AbstractMigrationWorker {
    private static final int MAX_SIZE = 3000;

    private final Log log = LogFactory.getLog(MetricsIndexMigrator.class);

    private final DateTimeService dateTimeService = new DateTimeService();
    private final MetricsConfiguration metricsConfiguration = new MetricsConfiguration();
    private final Map<Integer, Set<Long>> accumulator = new HashMap<Integer, Set<Long>>();

    private final MigrationTable table;
    private final DataMigratorConfiguration migratorConfiguration;

    private final long timeLimit;
    private final PreparedStatement updateMetricsIndex;
    private final Duration sliceDuration;
    private final boolean validAccumulatorTable;

    private int currentCount = 0;

    public MetricsIndexMigrator(MigrationTable table, DataMigratorConfiguration config) {
        this.table = table;
        this.migratorConfiguration = config;

        if (MigrationTable.RAW.equals(table) || MigrationTable.ONE_HOUR.equals(table)
            || MigrationTable.SIX_HOUR.equals(table)) {
            this.sliceDuration = this.getTimeSliceDuration(table);
            this.timeLimit = this.getLastAggregationTime(table) - this.sliceDuration.getMillis();
            this.updateMetricsIndex = config.getSession().prepare(
                "INSERT INTO " + MetricsTable.INDEX + " " +
                "(bucket, partition, time, schedule_id) " +
                "VALUES (?, ?, ?, ?) ");
            this.validAccumulatorTable = true;
        } else {
            this.timeLimit = Integer.MAX_VALUE;
            this.updateMetricsIndex = null;
            this.sliceDuration = null;
            this.validAccumulatorTable = false;
        }
    }

    public void add(int scheduleId, long timestamp) throws Exception {
        if (validAccumulatorTable && timeLimit <= timestamp) {
            long alignedTimeSlice = dateTimeService.getTimeSlice(timestamp, sliceDuration).getMillis();

            if (accumulator.containsKey(scheduleId)) {
                Set<Long> timestamps = accumulator.get(scheduleId);
                if (!timestamps.contains(alignedTimeSlice)) {
                    timestamps.add(alignedTimeSlice);

                    currentCount++;
                }
            } else {
                Set<Long> timestamps = new HashSet<Long>();
                timestamps.add(timestamp);
                accumulator.put(scheduleId, timestamps);

                currentCount++;
            }
        }

        if (currentCount > MAX_SIZE) {
            drain();
        }
    }

    public void drain() throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("Draining metrics index accumulator with " + currentCount + " entries");
        }

        List<ResultSetFuture> resultSetFutures = new ArrayList<ResultSetFuture>();

        for (Map.Entry<Integer, Set<Long>> entry : accumulator.entrySet()) {
            for (Long timestamp : entry.getValue()) {
                Integer scheduleId = entry.getKey();

                BoundStatement statement = updateMetricsIndex.bind(table.getAggregationBucket().toString(),
                    (scheduleId % metricsConfiguration.getIndexPartitions()), new Date(timestamp), scheduleId);

                resultSetFutures.add(migratorConfiguration.getSession().executeAsync(statement));
            }
        }

        for (ResultSetFuture future : resultSetFutures) {
            future.get();
        }

        accumulator.clear();
        currentCount = 0;
    }

    private long getLastAggregationTime(MigrationTable migratedTable) {
        StatelessSession session = getSQLSession(migratorConfiguration);

        long aggregationSlice = Integer.MAX_VALUE;
        Duration duration = null;
        String queryString = null;

        if (MigrationTable.RAW.equals(migratedTable)) {
            duration = metricsConfiguration.getRawTimeSliceDuration();
            queryString = MigrationQuery.MAX_TIMESTAMP_1H_DATA.toString();
        } else if (MigrationTable.ONE_HOUR.equals(migratedTable)) {
            duration = metricsConfiguration.getOneHourTimeSliceDuration();
            queryString = MigrationQuery.MAX_TIMESTAMP_6H_DATA.toString();
        } else if (MigrationTable.SIX_HOUR.equals(migratedTable)) {
            duration = metricsConfiguration.getSixHourTimeSliceDuration();
            queryString = MigrationQuery.MAX_TIMESTAMP_1D_DATA.toString();
        }

        if (duration != null && queryString != null) {
            Query query = session.createSQLQuery(queryString);

            Long timeStamp;
            Object result = query.uniqueResult();
            if(result != null){
                String queryResult = query.uniqueResult().toString();
                Long timestamp = Long.parseLong(queryResult);
                aggregationSlice = dateTimeService.getTimeSlice(new DateTime(timestamp), duration).getMillis();
            }
        }

        closeSQLSession(session);

        return aggregationSlice;
    }

    public Duration getTimeSliceDuration(MigrationTable table) {
        if (MigrationTable.RAW.equals(table)) {
            return metricsConfiguration.getRawTimeSliceDuration();
        } else if (MigrationTable.ONE_HOUR.equals(table)) {
            return metricsConfiguration.getOneHourTimeSliceDuration();
        } else if (MigrationTable.SIX_HOUR.equals(table)) {
            return metricsConfiguration.getSixHourTimeSliceDuration();
        }

        throw new IllegalArgumentException("Time slice duration for " + table.getTableName()
            + " table is not supported");
    }
}
