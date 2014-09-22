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

import static com.datastax.driver.core.querybuilder.QueryBuilder.ttl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.querybuilder.Batch;
import com.datastax.driver.core.querybuilder.QueryBuilder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.StatelessSession;

import org.rhq.server.metrics.domain.AggregateType;
import org.rhq.server.metrics.domain.MetricsTable;
import org.rhq.server.metrics.migrator.DataMigrator;
import org.rhq.server.metrics.migrator.DataMigrator.DataMigratorConfiguration;
import org.rhq.server.metrics.migrator.DataMigrator.Task;
import org.rhq.server.metrics.migrator.datasources.ExistingDataSource;

/**
 * @author Stefan Negrea
 *
 */
public class AggregateDataMigrator extends AbstractMigrationWorker implements CallableMigrationWorker {

    private final Log log = LogFactory.getLog(AggregateDataMigrator.class);

    private final DataMigratorConfiguration config;
    private final String selectQuery;
    private final String deleteQuery;
    private final String countQuery;
    private final MigrationTable migrationTable;
    private final MetricsIndexMigrator metricsIndexAccumulator;

    /**
     * @param query
     * @param migrationTable
     */
    public AggregateDataMigrator(MigrationTable migrationTable, DataMigratorConfiguration config)
        throws Exception {

        this.migrationTable = migrationTable;
        this.config = config;

        if (MigrationTable.ONE_HOUR.equals(this.migrationTable)) {
            this.selectQuery = MigrationQuery.SELECT_1H_DATA.toString();
            this.deleteQuery = MigrationQuery.DELETE_1H_DATA.toString();
            this.countQuery = MigrationQuery.COUNT_1H_DATA.toString();
        } else if (MigrationTable.SIX_HOUR.equals(this.migrationTable)) {
            this.selectQuery = MigrationQuery.SELECT_6H_DATA.toString();
            this.deleteQuery = MigrationQuery.DELETE_6H_DATA.toString();
            this.countQuery = MigrationQuery.COUNT_6H_DATA.toString();
        } else if (MigrationTable.TWENTY_FOUR_HOUR.equals(this.migrationTable)) {
            this.selectQuery = MigrationQuery.SELECT_1D_DATA.toString();
            this.deleteQuery = MigrationQuery.DELETE_1D_DATA.toString();
            this.countQuery = MigrationQuery.COUNT_1D_DATA.toString();
        } else {
            throw new Exception("MigrationTable " + migrationTable.toString() + " not supported by this migrator.");
        }

        metricsIndexAccumulator = new MetricsIndexMigrator(migrationTable, config);
    }

    @Override
    public long estimate() throws Exception {
        long recordCount = this.getRowCount(this.countQuery);
        log.debug("Retrieved record count for table " + migrationTable.toString() + " -- " + recordCount);

        Telemetry telemetry = this.performMigration(Task.Estimate);
        long estimatedTimeToMigrate = telemetry.getMigrationTime();

        long estimation = (recordCount / MAX_RECORDS_TO_LOAD_FROM_SQL / NUMBER_OF_BATCHES_FOR_ESTIMATION)
            * estimatedTimeToMigrate;

        estimation += telemetry.getNonMigrationTime();

        return estimation;
    }

    public void migrate() throws Exception {
        performMigration(Task.Migrate);
        if (config.isDeleteDataImmediatelyAfterMigration()) {
            deleteTableData();
        }
    }

    private long getRowCount(String countQuery) {
        StatelessSession session = getSQLSession(config);

        org.hibernate.Query query = session.createSQLQuery(countQuery);
        query.setReadOnly(true);
        query.setTimeout(DataMigrator.SQL_TIMEOUT);
        long count = Long.parseLong(query.uniqueResult().toString());

        closeSQLSession(session);

        return count;
    }

    private void deleteTableData() throws Exception {
        int failureCount = 0;
        while (failureCount < MAX_NUMBER_OF_FAILURES) {
            try {
                StatelessSession session = getSQLSession(config);
                session.getTransaction().begin();
                org.hibernate.Query nativeQuery = session.createSQLQuery(this.deleteQuery);
                nativeQuery.executeUpdate();
                session.getTransaction().commit();
                closeSQLSession(session);
                log.info("- " + migrationTable.toString() + " - Cleaned -");
            } catch (Exception e) {
                log.error("Failed to delete " + migrationTable.toString()
                    + " data. Attempting to delete data one more time...");

                failureCount++;
                if (failureCount == MAX_NUMBER_OF_FAILURES) {
                    throw e;
                }
            }
        }
    }

    private Telemetry performMigration(Task task) throws Exception {
        Telemetry telemetry = new Telemetry();
        telemetry.getGeneralTimer().start();

        long numberOfBatchesMigrated = 0;

        List<Object[]> existingData;
        int failureCount;

        int lastMigratedRecord = 0;
        ExistingDataSource dataSource = getExistingDataSource(selectQuery, task, config);
        dataSource.initialize();

        telemetry.getMigrationTimer().start();
        while (true) {
            existingData = dataSource.getData(lastMigratedRecord, MAX_RECORDS_TO_LOAD_FROM_SQL);

            if (existingData.size() == 0) {
                break;
            }

            lastMigratedRecord += existingData.size();

            failureCount = 0;
            while (failureCount < MAX_NUMBER_OF_FAILURES) {
                try {
                    insertDataToCassandra(existingData);
                    break;
                } catch (Exception e) {
                    log.error("Failed to insert " + migrationTable.toString()
                        + " data. Attempting to insert the current batch of data one more time");
                    log.error(e);

                    failureCount++;
                    if (failureCount == MAX_NUMBER_OF_FAILURES) {
                        throw e;
                    }
                }
            }

            log.info("- " + migrationTable + " - " + lastMigratedRecord + " -");

            numberOfBatchesMigrated++;
            if (Task.Estimate.equals(task) && numberOfBatchesMigrated >= NUMBER_OF_BATCHES_FOR_ESTIMATION) {
                break;
            }
        }

        metricsIndexAccumulator.drain();

        telemetry.getMigrationTimer().stop();

        dataSource.close();
        telemetry.getGeneralTimer().stop();

        return telemetry;
    }

    private void insertDataToCassandra(List<Object[]> existingData) throws Exception {
        List<ResultSetFuture> resultSetFutures = new ArrayList<ResultSetFuture>();
        Batch batch = QueryBuilder.batch();
        int batchSize = 0;

        //only need approximate TTL to speed up processing
        //given that each batch is processed within seconds, getting the
        //system time once per batch has minimal impact on the record retention
        long creationTimeMillis;
        long itemTTLSeconds;
        long currentTimeMillis = System.currentTimeMillis();
        long expectedTTLMillis = migrationTable.getTTLinMilliseconds();

        for (Object[] rawMeasurement : existingData) {
            creationTimeMillis = Long.parseLong(rawMeasurement[MigrationQuery.TIMESTAMP_INDEX].toString());
            itemTTLSeconds = (expectedTTLMillis - currentTimeMillis + creationTimeMillis) / 1000l;

            if (itemTTLSeconds > 0) {
                int scheduleId = Integer.parseInt(rawMeasurement[MigrationQuery.SCHEDULE_INDEX].toString());
                Date time = new Date(creationTimeMillis);

                batch.add(QueryBuilder.insertInto(MetricsTable.AGGREGATE.toString())
                    .value("schedule_id", scheduleId)
                    .value("bucket", migrationTable.getMigrationBucket().toString())
                    .value("time", time)
                    .value("avg", Double.parseDouble(rawMeasurement[MigrationQuery.VALUE_INDEX].toString()))
                    .value("min", Double.parseDouble(rawMeasurement[MigrationQuery.MIN_VALUE_INDEX].toString()))
                    .value("max", Double.parseDouble(rawMeasurement[MigrationQuery.MAX_VALUE_INDEX].toString()))
                    .using(ttl((int) itemTTLSeconds)));

                batchSize++;

                metricsIndexAccumulator.add(scheduleId, creationTimeMillis);
            }

            if (batchSize >= MAX_AGGREGATE_BATCH_TO_CASSANDRA) {
                resultSetFutures.add(config.getSession().executeAsync(batch));
                batch = QueryBuilder.batch();
                batchSize = 0;
            }
        }

        if (batchSize != 0) {
            resultSetFutures.add(config.getSession().executeAsync(batch));
        }

        for (ResultSetFuture future : resultSetFutures) {
            future.get();
        }
    }
}
