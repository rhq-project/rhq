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
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.querybuilder.Batch;
import com.datastax.driver.core.querybuilder.QueryBuilder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.StatelessSession;

import org.rhq.server.metrics.domain.MetricsTable;
import org.rhq.server.metrics.migrator.DataMigrator;
import org.rhq.server.metrics.migrator.DataMigrator.DataMigratorConfiguration;
import org.rhq.server.metrics.migrator.DataMigrator.Task;
import org.rhq.server.metrics.migrator.datasources.ExistingDataSource;

/**
 * @author Stefan Negrea
 *
 */
public class RawDataMigrator extends AbstractMigrationWorker implements CallableMigrationWorker {
    private final Log log = LogFactory.getLog(RawDataMigrator.class);

    private final Queue<String> tablesNotProcessed = new LinkedList<String>(Arrays.asList(getRawDataTables()));
    private final MetricsIndexMigrator metricsIndexAccumulator;
    private final DataMigratorConfiguration config;

    public RawDataMigrator(DataMigratorConfiguration config) {
        this.config = config;
        this.metricsIndexAccumulator = new MetricsIndexMigrator(MigrationTable.RAW, config);
    }

    public long estimate() throws Exception {
        long recordCount = 0;
        for (String table : getRawDataTables()) {
            String countQuery = String.format(MigrationQuery.COUNT_RAW.toString(), table);
            long tableRecordCount = this.getRowCount(countQuery);

            log.debug("Retrieved record count for table " + table + " -- " + tableRecordCount);

            recordCount += tableRecordCount;
        }

        Telemetry telemetry = this.performMigration(Task.Estimate);
        long estimatedTimeToMigrate = telemetry.getMigrationTime();
        long estimation = (recordCount / MAX_RECORDS_TO_LOAD_FROM_SQL / NUMBER_OF_BATCHES_FOR_ESTIMATION)
            * estimatedTimeToMigrate;
        estimation += telemetry.getNonMigrationTime();

        return estimation;
    }

    public void migrate() throws Exception {
        performMigration(Task.Migrate);
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

    private Telemetry performMigration(Task task) throws Exception {
        Telemetry telemetry = new Telemetry();
        telemetry.getGeneralTimer().start();

        long numberOfBatchesMigrated = 0;

        List<Object[]> existingData;
        int failureCount;

        telemetry.getMigrationTimer().start();
        telemetry.getMigrationTimer().suspend();

        while (!tablesNotProcessed.isEmpty()) {
            String table = tablesNotProcessed.peek();

            String selectQuery = String.format(MigrationQuery.SELECT_RAW_DATA.toString(), table);

            ExistingDataSource dataSource = getExistingDataSource(selectQuery, task, config);
            dataSource.initialize();

            log.info("Start migrating raw table: " + table);

            telemetry.getMigrationTimer().resume();
            int lastMigratedRecord = 0;
            while (true) {
                existingData = dataSource.getData(lastMigratedRecord, MAX_RECORDS_TO_LOAD_FROM_SQL);

                if (existingData == null || existingData.size() == 0) {
                    break;
                }

                lastMigratedRecord += existingData.size();

                failureCount = 0;
                while (failureCount < MAX_NUMBER_OF_FAILURES) {
                    try {
                        insertDataToCassandra(existingData);
                        break;
                    } catch (Exception e) {
                        log.error("Failed to insert " + MetricsTable.RAW.toString()
                            + " data. Attempting to insert the current batch of data one more time");
                        log.error(e);

                        failureCount++;
                        if (failureCount == MAX_AGGREGATE_BATCH_TO_CASSANDRA) {
                            throw e;
                        }
                    }
                }

                log.info("- " + table + " - " + lastMigratedRecord + " -");

                numberOfBatchesMigrated++;
                if (Task.Estimate.equals(task) && numberOfBatchesMigrated >= NUMBER_OF_BATCHES_FOR_ESTIMATION) {
                    break;
                }
            }
            telemetry.getMigrationTimer().suspend();

            if (Task.Migrate.equals(task)) {
                log.info("Done migrating raw table" + table + "---------------------");

                if (config.isDeleteDataImmediatelyAfterMigration()) {
                    deleteTableData(table);
                }
            } else if (numberOfBatchesMigrated >= NUMBER_OF_BATCHES_FOR_ESTIMATION) {
                break;
            }

            dataSource.close();
            tablesNotProcessed.poll();
        }

        telemetry.getMigrationTimer().resume();
        metricsIndexAccumulator.drain();
        telemetry.getMigrationTimer().suspend();

        telemetry.getGeneralTimer().stop();
        return telemetry;
    }

    private void deleteTableData(String table) throws Exception {
        String deleteQuery = String.format(MigrationQuery.DELETE_RAW_ENTRY.toString(), table);
        int failureCount = 0;
        while (failureCount < MAX_NUMBER_OF_FAILURES) {
            try {
                StatelessSession session = getSQLSession(config);
                session.getTransaction().begin();
                org.hibernate.Query nativeQuery = session.createSQLQuery(deleteQuery);
                nativeQuery.executeUpdate();
                session.getTransaction().commit();
                closeSQLSession(session);
                log.info("- " + table + " - Cleaned -");
            } catch (Exception e) {
                log.error("Failed to delete " + table + " data. Attempting to delete data one more time...");

                failureCount++;
                if (failureCount == MAX_NUMBER_OF_FAILURES) {
                    throw e;
                }
            }
        }
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
        long expectedTTLMillis = MigrationTable.RAW.getTTLinMilliseconds();

        for (Object[] rawDataPoint : existingData) {
            creationTimeMillis = Long.parseLong(rawDataPoint[MigrationQuery.TIMESTAMP_INDEX].toString());
            itemTTLSeconds = (expectedTTLMillis - currentTimeMillis + creationTimeMillis) / 1000l;

            if (itemTTLSeconds > 0) {
                int scheduleId = Integer.parseInt(rawDataPoint[MigrationQuery.SCHEDULE_INDEX].toString());
                Date creationTime = new Date(creationTimeMillis);

                batch.add(QueryBuilder.insertInto(MetricsTable.RAW.toString()).value("schedule_id", scheduleId)
                    .value("time", creationTime)
                    .value("value", Double.parseDouble(rawDataPoint[MigrationQuery.VALUE_INDEX].toString()))
                    .using(ttl((int) itemTTLSeconds)));
                batchSize++;

                metricsIndexAccumulator.add(scheduleId, creationTimeMillis);
            }

            if (batchSize >= MAX_RAW_BATCH_TO_CASSANDRA) {
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
