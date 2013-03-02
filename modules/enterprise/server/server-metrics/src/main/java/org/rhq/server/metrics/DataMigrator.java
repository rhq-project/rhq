/*
 * RHQ Management Platform
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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

package org.rhq.server.metrics;

import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.ttl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.measurement.MeasurementDataNumeric1D;
import org.rhq.core.domain.measurement.MeasurementDataNumeric1H;
import org.rhq.core.domain.measurement.MeasurementDataNumeric6H;
import org.rhq.core.domain.measurement.MeasurementDataNumericAggregateInterface;
import org.rhq.server.metrics.domain.AggregateType;
import org.rhq.server.metrics.domain.MetricsTable;

/**
 * @author Stefan Negrea
 *
 */
public class DataMigrator {

    private final Log log = LogFactory.getLog(DataMigrator.class);

    private static final int MAX_RECORDS_TO_LOAD_FROM_SQL = 30000;
    private static final int MAX_RECORDS_TO_BATCH_TO_CASSANDRA = 500;
    private static final int MAX_NUMBER_OF_FAILURES = 5;

    private final EntityManager entityManager;

    private final Session session;

    private boolean telemetry;

    private boolean deleteDataImmediatelyAfterMigration;
    private boolean deleteAllDataAtEndOfMigration;

    private boolean runRawDataMigration;
    private boolean run1HAggregateDataMigration;
    private boolean run6HAggregateDataMigration;
    private boolean run1DAggregateDataMigration;

    public DataMigrator(EntityManager entityManager, Session session) {
        this.entityManager = entityManager;
        this.session = session;

        this.deleteDataImmediatelyAfterMigration = true;
        this.deleteAllDataAtEndOfMigration = false;
        this.runRawDataMigration = true;
        this.run1HAggregateDataMigration = true;
        this.run6HAggregateDataMigration = true;
        this.run1DAggregateDataMigration = true;

        this.telemetry = false;
    }

    public void run1HAggregateDataMigration(boolean value) {
        this.run1HAggregateDataMigration = value;
    }

    public void run6HAggregateDataMigration(boolean value) {
        this.run6HAggregateDataMigration = value;
    }

    public void run1DAggregateDataMigration(boolean value) {
        this.run1DAggregateDataMigration = value;
    }


    public void deleteDataImmediatelyAfterMigration() {
        this.deleteDataImmediatelyAfterMigration = true;
        this.deleteAllDataAtEndOfMigration = false;
    }

    public void deleteAllDataAtEndOfMigration() {
        this.deleteAllDataAtEndOfMigration = true;
        this.deleteDataImmediatelyAfterMigration = false;
    }

    public void preserveData() {
        this.deleteAllDataAtEndOfMigration = false;
        this.deleteDataImmediatelyAfterMigration = false;
    }

    public void enableTelemetry() {
        this.telemetry = true;
    }

    public void disableTelemetry() {
        this.telemetry = false;
    }

    public void migrateData() throws Exception {
        if (runRawDataMigration) {
            retryOnFailure(new RawDataMigrator());
        }

        if (run1HAggregateDataMigration) {
            retryOnFailure(new AggregateDataMigrator(MetricsTable.ONE_HOUR));
        }

        if (run6HAggregateDataMigration) {
            retryOnFailure(new AggregateDataMigrator(MetricsTable.SIX_HOUR));
        }

        if (run1DAggregateDataMigration) {
            retryOnFailure(new AggregateDataMigrator(MetricsTable.TWENTY_FOUR_HOUR));
        }

        if (deleteAllDataAtEndOfMigration) {
            retryOnFailure(new DeleteAllData());
        }
    }

    /**
     * Retries the migration {@link #MAX_NUMBER_OF_FAILURES} times before
     * failing the migration operation.
     *
     * @param migrator
     * @throws Exception
     */
    private void retryOnFailure(CallableMigrationWorker migrator) throws Exception {
        int numberOfFailures = 0;
        Exception caughtException = null;

        log.info(migrator.getClass());

        while (numberOfFailures < MAX_NUMBER_OF_FAILURES) {
            try {
                migrator.work();
                return;
            } catch (Exception e) {
                log.error("Migrator " + migrator.getClass() + " failed. Retrying!", e);

                caughtException = e;
                numberOfFailures++;
            }
        }

        throw caughtException;
    }

    /**
     * Returns a list of all the raw SQL metric tables.
     * There is no equivalent in Cassandra, all raw data is stored in a single column family.
     *
     * @return SQL raw metric tables
     */
    private String[] getRawDataTables() {
        int tableCount = 15;
        String tablePrefix = "RHQ_MEAS_DATA_NUM_R";

        String[] tables = new String[tableCount];
        for (int i = 0; i < tableCount; i++) {
            if (i < 10) {
                tables[i] = tablePrefix + "0" + i;
            } else {
                tables[i] = tablePrefix + i;
            }
        }

        return tables;
    }

    private interface CallableMigrationWorker {
        void work() throws Exception;
    }


    private class AggregateDataMigrator implements CallableMigrationWorker {

        private final String query;
        private final MetricsTable metricsTable;

        /**
         * @param query
         * @param metricsTable
         */
        public AggregateDataMigrator(MetricsTable metricsTable) {
            this.metricsTable = metricsTable;

            if (MetricsTable.ONE_HOUR.equals(this.metricsTable)) {
                this.query = MeasurementDataNumeric1H.QUERY_FIND_ALL;
            } else if (MetricsTable.ONE_HOUR.equals(this.metricsTable)) {
                this.query = MeasurementDataNumeric6H.QUERY_FIND_ALL;
            } else if (MetricsTable.TWENTY_FOUR_HOUR.equals(this.metricsTable)) {
                this.query = MeasurementDataNumeric1D.QUERY_FIND_ALL;
            } else {
                this.query = null;
            }
        }

        public void work() throws Exception {
            if (deleteDataImmediatelyAfterMigration) {
                performedBatchedMigration();
            } else {
                performFullMigration();
            }
        }

        @SuppressWarnings("unchecked")
        private void performedBatchedMigration() throws Exception {
            List<MeasurementDataNumericAggregateInterface> existingData;

            while (true) {
                Query q = entityManager.createNamedQuery(query);
                q.setMaxResults(MAX_RECORDS_TO_LOAD_FROM_SQL);
                existingData = (List<MeasurementDataNumericAggregateInterface>) q.getResultList();

                if (existingData.size() == 0) {
                    break;
                }

                try {
                    insertDataToCassandra(existingData);
                } catch (Exception e) {
                    log.error("Failed to insert " + metricsTable.toString()
                        + " data. Attempting to insert the current batch of data one more time");
                    insertDataToCassandra(existingData);
                }

                for (Object entity : existingData) {
                    entityManager.remove(entity);
                }
                entityManager.flush();
            }
        }

        @SuppressWarnings("unchecked")
        private void performFullMigration() throws Exception {
            List<MeasurementDataNumericAggregateInterface> existingData = null;
            int lastMigratedRecord = 0;

            while (true) {
                Query q = entityManager.createNamedQuery(query);
                q.setFirstResult(lastMigratedRecord + 1);
                q.setMaxResults(MAX_RECORDS_TO_LOAD_FROM_SQL);

                existingData = (List<MeasurementDataNumericAggregateInterface>) q.getResultList();

                if (existingData.size() == 0) {
                    break;
                }

                lastMigratedRecord += existingData.size();

                try{
                    insertDataToCassandra(existingData);
                } catch (Exception e) {
                    log.error("Failed to insert " + metricsTable.toString()
                        + " data. Attempting to insert the current batch of data one more time");
                    insertDataToCassandra(existingData);
                }
            }
        }

        private void insertDataToCassandra(List<MeasurementDataNumericAggregateInterface> existingData)
            throws Exception {
            Statement statement = null;

            List<ResultSetFuture> resultSetFutures = new ArrayList<ResultSetFuture>();
            List<Statement> statementsAccumulator = new ArrayList<Statement>();

            //only need approximate TTL to speed up processing
            //given that each batch is processed within seconds, getting the
            //system time once per batch has minimal impact on the record retention
            long currentTimeMillis = System.currentTimeMillis();
            long expectedTTLMillis = metricsTable.getTTLinMilliseconds() * 10;
            long itemTTLSeconds = 0;

            for (MeasurementDataNumericAggregateInterface measurement : existingData) {
                itemTTLSeconds = (expectedTTLMillis - currentTimeMillis + measurement.getTimestamp()) / 1000l;

                statement = QueryBuilder.insertInto(metricsTable.toString())
                    .value("schedule_id", measurement.getScheduleId())
                    .value("time", new Date(measurement.getTimestamp()))
                    .value("type", AggregateType.MIN.ordinal())
                    .value("value", measurement.getMin())
                    .using(ttl((int) itemTTLSeconds));;
                statementsAccumulator.add(statement);

                statement = insertInto(metricsTable.toString())
                    .value("schedule_id", measurement.getScheduleId())
                    .value("time", new Date(measurement.getTimestamp()))
                    .value("type", AggregateType.MAX.ordinal())
                    .value("value", measurement.getMax())
                    .using(ttl((int) itemTTLSeconds));
                statementsAccumulator.add(statement);

                statement = insertInto(metricsTable.toString()).value("schedule_id", measurement.getScheduleId())
                    .value("time", new Date(measurement.getTimestamp()))
                    .value("type", AggregateType.AVG.ordinal())
                    .value("value", Double.parseDouble(measurement.getValue().toString()))
                    .using(ttl((int) itemTTLSeconds));
                statementsAccumulator.add(statement);

                if (statementsAccumulator.size() >= MAX_RECORDS_TO_BATCH_TO_CASSANDRA) {
                    resultSetFutures.add(session.executeAsync(QueryBuilder.batch((Statement[]) statementsAccumulator
                        .toArray(new Statement[statementsAccumulator.size()]))));
                    statementsAccumulator.clear();
                }
            }

            if (statementsAccumulator.size() != 0) {
                resultSetFutures.add(session.executeAsync(QueryBuilder.batch((Statement[]) statementsAccumulator
                    .toArray(new Statement[statementsAccumulator.size()]))));
            }

            for (ResultSetFuture future : resultSetFutures) {
                future.get();
            }
        }
    }


    private class RawDataMigrator implements CallableMigrationWorker {

        Queue<String> tablesNotProcessed = new LinkedList<String>(Arrays.asList(getRawDataTables()));

        public void work() throws Exception {
            if (deleteDataImmediatelyAfterMigration) {
                performBatchedMigration();
            } else {
                performFullMigration();
            }
        }

        @SuppressWarnings("unchecked")
        private void performBatchedMigration() throws Exception {
            List<Object[]> existingData = null;

            while (!tablesNotProcessed.isEmpty()) {
                String table = tablesNotProcessed.peek();

                String selectQuery = "SELECT schedule_id, time_stamp, value FROM " + table;
                String deleteQuery = "DELETE FROM " + table + " WHERE schedule_id = ?";

                while (true) {
                    Query query = entityManager.createNativeQuery(selectQuery);
                    query.setMaxResults(MAX_RECORDS_TO_LOAD_FROM_SQL);
                    existingData = query.getResultList();

                    if (existingData.size() == 0) {
                        break;
                    }

                    try {
                        insertDataToCassandra(existingData);
                    } catch (Exception e) {
                        log.error("Failed to insert " + MetricsTable.RAW.toString()
                            + " data. Attempting to insert the current batch of data one more time");
                        insertDataToCassandra(existingData);
                    }

                    query = entityManager.createNativeQuery(deleteQuery);

                    for (Object[] rawDataPoint : existingData) {
                        query.setParameter(0, Integer.parseInt(rawDataPoint[0].toString()));
                        query.executeUpdate();
                    }
                }

                tablesNotProcessed.poll();
            }
        }

        @SuppressWarnings("unchecked")
        private void performFullMigration() throws Exception {
            List<Object[]> existingData = null;

            while (!tablesNotProcessed.isEmpty()) {
                String table = tablesNotProcessed.peek();

                log.info("Start migrating raw table: " + table);

                int lastMigratedRecord = 0;

                while (true) {
                    String selectQuery = "SELECT schedule_id, time_stamp, value FROM " + table;
                    Query query = entityManager.createNativeQuery(selectQuery);
                    query.setFirstResult(lastMigratedRecord + 1);
                    query.setMaxResults(MAX_RECORDS_TO_LOAD_FROM_SQL);

                    existingData = query.getResultList();

                    if (existingData.size() == 0) {
                        break;
                    }

                    lastMigratedRecord += existingData.size();

                    try {
                        insertDataToCassandra(existingData);
                    } catch (Exception e) {
                        log.error("Failed to insert " + MetricsTable.RAW.toString()
                            + " data. Attempting to insert the current batch of data one more time");
                        insertDataToCassandra(existingData);
                    }

                    if (lastMigratedRecord % MAX_RECORDS_TO_LOAD_FROM_SQL == 0) {
                        log.info("------------" + lastMigratedRecord + "---------------------");
                    }
                }

                log.info("Done migrating raw table" + table + "---------------------");
                tablesNotProcessed.poll();
            }
        }

        private void insertDataToCassandra(List<Object[]> existingData) throws Exception {
            List<ResultSetFuture> resultSetFutures = new ArrayList<ResultSetFuture>();
            List<Statement> statementsAccumulator = new ArrayList<Statement>();

            //only need approximate TTL to speed up processing
            //given that each batch is processed within seconds, getting the
            //system time once per batch has minimal impact on the record retention
            long currentTimeMillis = System.currentTimeMillis();
            long expectedTTLMillis = MetricsTable.RAW.getTTLinMilliseconds() * 10;
            long creationTimeMillis = 0;
            long itemTTLSeconds = 0;

            for (Object[] rawDataPoint : existingData) {
                creationTimeMillis = Long.parseLong(rawDataPoint[1].toString());
                itemTTLSeconds = (expectedTTLMillis - currentTimeMillis + creationTimeMillis) / 1000l;

                if (itemTTLSeconds > 0) {
                    Statement boundStatement = QueryBuilder.insertInto(MetricsTable.RAW.toString())
                        .value("schedule_id", Integer.parseInt(rawDataPoint[0].toString()))
                        .value("time", new Date(creationTimeMillis))
                        .value("value", Double.parseDouble(rawDataPoint[2].toString()))
                        .using(ttl((int) itemTTLSeconds));

                    statementsAccumulator.add(boundStatement);
                }

                if (statementsAccumulator.size() >= MAX_RECORDS_TO_BATCH_TO_CASSANDRA) {
                    resultSetFutures.add(session.executeAsync(QueryBuilder.batch((Statement[]) statementsAccumulator
                        .toArray(new Statement[statementsAccumulator.size()]))));
                    statementsAccumulator.clear();
                }
            }

            if (statementsAccumulator.size() != 0) {
                resultSetFutures.add(session.executeAsync(QueryBuilder.batch((Statement[]) statementsAccumulator
                    .toArray(new Statement[statementsAccumulator.size()]))));
            }

            for (ResultSetFuture future : resultSetFutures) {
                future.get();
            }
        }
    }


    private class DeleteAllData implements CallableMigrationWorker {

        public void work() {
            Query q = entityManager.createNamedQuery(MeasurementDataNumeric1H.QUERY_DELETE_ALL);
            q.executeUpdate();

            q = entityManager.createNamedQuery(MeasurementDataNumeric6H.QUERY_DELETE_ALL);
            q.executeUpdate();

            q = entityManager.createNamedQuery(MeasurementDataNumeric1D.QUERY_DELETE_ALL);
            q.executeUpdate();

            for (String table : getRawDataTables()) {
                String deleteAllData = "DELETE FROM " + table;
                q = entityManager.createNativeQuery(deleteAllData);
                q.executeUpdate();
            }
        }
    }
}
